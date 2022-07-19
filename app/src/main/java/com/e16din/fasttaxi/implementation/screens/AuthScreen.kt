package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.LocalDataSource
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.databinding.ScreenAuthBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.data.AuthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthActivity : AppCompatActivity() {

  class AuthScreenState(
    // User
    var login: String? = null,
    var password: String? = null,
    // Data Source
    var authResult: FastTaxiServer.Result<AuthData?>? = null,
    var authJob: Job? = null,
  ) : ScreenState {
    fun getToken(): String? {
      return authResult?.data?.token
    }
  }

  private lateinit var binding: ScreenAuthBinding

  private val screenFruit = AuthScreenState()

  private val scope = makeScope()

  private val events = Events()
  private val conditions = Conditions()

  private fun main() {
    doAction(
      desc = "Показать пользователю кнопку ВОЙТИ (активную/неактивную)",
      events = listOf(
        events.onCreate,
        events.onLoginChanged,
        events.onPasswordChanged
      )
    ) {
      binding.signInButton.isEnabled = data(conditions.isEnterButtonEnabled)
    }

    doAction(
      desc = "Приложение запрашивает авторизацию на сервере",
      events = listOf(events.onEnterClick)
    ) {
      if (screenFruit.authJob?.isActive == true) {
        return@doAction
      }

      screenFruit.authJob = scope.launch(Dispatchers.Main) {
        val result = withContext(Dispatchers.IO) {
          FastTaxiServer.auth(
            login = requireNotNull(screenFruit.login),
            password = requireNotNull(screenFruit.password)
          )
        }

        screenFruit.authResult = result
        when (result.success) {
          true -> {
            events.onAuthSuccess.call()
          }
          false -> {
            events.onAuthFail.call()
          }
        }
      }
    }

    doAction(
      desc = "Приложение показало ошибку авторизации пользователю",
      events = listOf(events.onAuthFail)
    ) {
      Toast.makeText(binding.root.context, "Fail!", Toast.LENGTH_SHORT)
        .show()
    }

    doAction(
      desc = "Приложение сохранило токен авторизации",
      events = listOf(events.onAuthSuccess)
    ) {
      val profileFruit = FastTaxiApp.profileFruit
      profileFruit.token = screenFruit.getToken()
      LocalDataSource.saveLocalData(profileFruit)
    }
    doAction(
      desc = "Система закрыла экран авторизации",
      events = listOf(events.onAuthSuccess)
    ) {
      FastTaxiApp.removeScreenState(AuthScreenState::class)
      finish()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ScreenAuthBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val screen = FastTaxiApp.getScreenState()
      ?: AuthScreenState()
    FastTaxiApp.addScreenState(screen)

    binding.loginField.addTextChangedListener { login ->
      fieldsHandler.doLast {
        screenFruit.login = login.toString()
        events.onLoginChanged.call()
      }
    }
    binding.passwordField.addTextChangedListener { password ->
      fieldsHandler.doLast {
        screenFruit.password = password.toString()
        events.onPasswordChanged.call()
      }
    }
    binding.signInButton.setOnClickListener {
      events.onEnterClick.call()
    }

    main()

    events.onCreate.call()
  }

  class Events {
    val onCreate = Event("onCreate: ОС открыла экран авторизации")

    val onLoginChanged = Event("Чел ввел логин")
    val onPasswordChanged = Event("Чел ввел пароль")
    val onEnterClick = Event("Чел нажал кнопку ВОЙТИ")

    val onAuthSuccess = Event("Сервер прислал ответ - авторизация успешна")
    val onAuthFail = Event("Сервер прислал ответ - отказ в авторизации")
  }

  inner class Conditions {
    val isEnterButtonEnabled = checkConditions(
      conditions = listOf(
        Condition(
          desc = "Логин от 4-х до 15-ти сиволов",
          falseValues = listOf(
            "",
            "abc",
            "a123456789012345"
          ),
          trueValues = listOf(
            "a123",
            "a123456",
            "a12345678901234",
          ),
          value = screenFruit.login,
          checkFunction = { it?.length in 4..15 }
        ),
        // todo: написать регулярку для проверки логина
        //      Condition(
        //        // todo:        (узнать какие знаки разрешены для логина)
        //        desc = "Логин только латиницей, числами, и разрешенными знаками",
        //        falseValues = listOf(
        //          "олдоолддоододо",
        //          "олдоолддоододо12",
        //          "олдоолддоододо12-sdas_were",
        //          "олд-_оолддоододо12",
        //        ),
        //        value = screenState.login,
        //        checkFunction = { value -> value?.all { it.isLatinChar() } == true }
        //      ),
        Condition(
          desc = "Логин без пробелов",
          falseValues = listOf(
            "             ",
            "             q455",
            " wqrdgsnvdjlk",
            "wqrdgsnvdjlk ",
            "wqrdgs nvdjlk"
          ),
          value = screenFruit.login,
          checkFunction = { it?.contains(" ") == false }
        ),
        Condition(
          desc = "Пароль от 6-ти символов",
          falseValues = listOf(
            "",
            "1",
            "12345"
          ),
          trueValues = listOf(
            "123456",
            "123457"
          ),
          value = screenFruit.password,
          checkFunction = { it?.length in 6..Int.MAX_VALUE }
        )
      ), onOk = {}, onNotOk = {})
  }
}
