package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.architecture.subjects.DataActor
import com.e16din.fasttaxi.architecture.subjects.SystemActor
import com.e16din.fasttaxi.architecture.subjects.UserActor
import com.e16din.fasttaxi.databinding.ScreenAuthBinding
import com.e16din.fasttaxi.implementation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthActivity : AppCompatActivity() {

  class AuthScreenState(
    var login: String? = null,
    var password: String? = null,
  ) : ScreenState

  private val screenState = AuthScreenState()

  private val binding = ScreenAuthBinding.inflate(layoutInflater)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(binding.root)

    val screen = FastTaxiApp.getScreenState()
      ?: AuthScreenState()
    FastTaxiApp.addScreenState(screen)

    SystemActor.onEvent("ОС открыла экран авторизации") {
      val isEnterButtonEnabled = checkLoginPasswordValues()
      UserActor.doAction(
        desc = "Показать пользователю кнопку ВОЙТИ (активную/неактивную)",
        data = isEnterButtonEnabled
      ) {
        binding.signInButton.isEnabled = isEnterButtonEnabled
      }
    }

    binding.loginField.addTextChangedListener { login ->
      fieldsHandler.doLast {
        UserActor.onEvent("Пользователь ввел логин") {
          screenState.login = login.toString()
          val isEnterButtonEnabled = checkLoginPasswordValues()
          UserActor.doAction("Приложение сделало кнопку доступной/недоступной",
            isEnterButtonEnabled) {
            binding.signInButton.isEnabled = isEnterButtonEnabled
          }
        }
      }
    }
    binding.passwordField.addTextChangedListener { password ->
      fieldsHandler.doLast {
        UserActor.onEvent("Пользователь ввел логин") {
          screenState.password = password.toString()
          val isEnterButtonEnabled = checkLoginPasswordValues()
          UserActor.doAction("Приложение сделало кнопку доступной/недоступной",
            isEnterButtonEnabled) {
            binding.signInButton.isEnabled = isEnterButtonEnabled
          }
        }
      }
    }
    binding.signInButton.setOnClickListener {
      clicksHandler.doLast {
        UserActor.onEvent(desc = "Пользователь нажал кнопку ВОЙТИ") {
          DataActor.doAction(desc = "Приложение запрашивает авторизацию на сервере") {
            DataActor.scope.launch(Dispatchers.Main) {
              val result = withContext(Dispatchers.IO) {
                FastTaxiServer.auth(
                  login = requireNotNull(screenState.login),
                  password = requireNotNull(screenState.password)
                )
              }

              when (result.success) {
                true -> {
                  DataActor.onEvent("Сервер прислал ответ - авторизация успешна") {
                    SystemActor.doAction("ОС закрыла экран авторизации") {
                      FastTaxiApp.profileFruit.token = result.data?.token
                      FastTaxiApp.removeScreenState(AuthScreenState::class)
                      finish()
                    }
                  }
                }
                false -> {
                  DataActor.onEvent("Сервер прислал ответ - отказ в авторизации") {
                    UserActor.doAction("Приложение показало ошибку авторизации пользователю") {
                      Toast.makeText(binding.root.context, "Fail!", Toast.LENGTH_SHORT)
                        .show()
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private fun checkLoginPasswordValues(): Boolean {
    return checkConditions(listOf(
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
        value = screenState.login,
        checkFunction = { it?.length in 4..15 }
      ),
      Condition(
        desc = "Логин без пробелов",
        falseValues = listOf(
          "             ",
          "             q455",
          " wqrdgsnvdjlk",
          "wqrdgsnvdjlk ",
          "wqrdgs nvdjlk"
        ),
        value = screenState.login,
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
        value = screenState.password,
        checkFunction = { it?.length in 6..Int.MAX_VALUE }
      )
    ), {}, {})
  }
}