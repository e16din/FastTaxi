package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.subjects.ServerAgent
import com.e16din.fasttaxi.architecture.subjects.SystemAgent
import com.e16din.fasttaxi.architecture.subjects.UserAgent
import com.e16din.fasttaxi.databinding.ScreenAuthBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.utils.base.ActivitySystemAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AuthScreen : Screen {
  lateinit var systemAgent: AuthSystemAgent
  lateinit var serverAgent: AuthServerAgent
  lateinit var userAgent: AuthUserAgent

  class State(
    var login: String? = null,
    var password: String? = null,
  )

  val state = State()

  fun main() {
    fun checkLoginPasswordValues(): Boolean {
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
          value = state.login,
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
          value = state.login,
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
          value = state.password,
          checkFunction = { it?.length in 6..Int.MAX_VALUE }
        )
      ), {}, {})
    }

    systemAgent.events.onCreate = systemAgent.onEvent("ОС открыла экран авторизации") {
      val isEnterButtonEnabled = checkLoginPasswordValues()
      userAgent.doUpdateSignInButtonState(
        desc = "Показать пользователю кнопку ВОЙТИ (активную/неактивную)",
        enabled = isEnterButtonEnabled
      )
    }

    userAgent.onLoginChanged = userAgent.onEvent("Пользователь ввел логин") { login ->
      state.login = login
      val isEnterButtonEnabled = checkLoginPasswordValues()
      userAgent.doUpdateSignInButtonState("Приложение сделало кнопку доступной/недоступной",
        isEnterButtonEnabled)
    }

    userAgent.onPasswordChanged = userAgent.onEvent("Пользователь ввел логин") { password ->
      state.password = password
      val isEnterButtonEnabled = checkLoginPasswordValues()
      userAgent.doUpdateSignInButtonState("Приложение сделало кнопку доступной/недоступной",
        isEnterButtonEnabled)
    }

    userAgent.onSignInClick = userAgent.onEvent(desc = "Пользователь нажал кнопку ВОЙТИ") {
      serverAgent.doSignIn(
        desc = "Приложение запрашивает авторизацию на сервере",
        login = requireNotNull(state.login),
        password = requireNotNull(state.password)
      )
    }

    serverAgent.onAuthSuccess =
      serverAgent.onEvent("Сервер прислал ответ - авторизация успешна") {
        systemAgent.doHideScreen("ОС закрыла экран авторизации")
      }

    serverAgent.onAuthFail = serverAgent.onEvent("Сервер прислал ответ - отказ в авторизации") {
      userAgent.doShowFailMessage("Приложение показало ошибку авторизации пользователю")
    }
  }
}

class AuthSystemAgent : ActivitySystemAgent(), SystemAgent {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = ScreenAuthBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val screen = FastTaxiApp.getScreen()
      ?: AuthScreen()
    screen.apply {
      serverAgent = AuthServerAgent()
      systemAgent = this@AuthSystemAgent
      userAgent = AuthUserAgent(binding)
    }
    FastTaxiApp.addScreen(screen)

    screen.main()
    events.onCreate?.invoke()
  }

  fun doHideScreen(desc: String) = doAction(desc) {
    FastTaxiApp.removeScreen(AuthScreen::class)
    finish()
  }
}

class AuthServerAgent : ServerAgent {

  private val scope = makeScope()

  lateinit var onAuthSuccess: JustEventBlock
  lateinit var onAuthFail: JustEventBlock

  fun doSignIn(desc: String, login: String, password: String) = doAction(desc) {
    scope.launch(Dispatchers.Main) {
      val result = withContext(Dispatchers.IO) {
        FastTaxiServer.auth(
          login = login,
          password = password
        )
      }

      when (result.success) {
        true -> onAuthSuccess.invoke()
        false -> onAuthFail.invoke()
      }
    }
  }
}

class AuthUserAgent(
  private val binding: ScreenAuthBinding,
) : UserAgent {

  private val handler = Handler(Looper.getMainLooper())

  var onLoginChanged: EventBlock<String>? = null
  var onPasswordChanged: EventBlock<String>? = null
  var onSignInClick: JustEventBlock? = null

  init {
    binding.loginField.addTextChangedListener { login ->
      handler.doLast {
        onLoginChanged?.invoke(login.toString())
      }
    }

    binding.passwordField.addTextChangedListener { password ->
      handler.doLast {
        onPasswordChanged?.invoke(password.toString())
      }
    }

    binding.signInButton.setOnClickListener {
      handler.doLast {
        onSignInClick?.invoke()
      }
    }
  }

  fun doUpdateSignInButtonState(desc: String, enabled: Boolean) = doAction(desc, enabled) {
    binding.signInButton.isEnabled = enabled
  }

  fun doShowFailMessage(desc: String) = doAction(desc) {
    Toast.makeText(binding.root.context, "Fail!", Toast.LENGTH_SHORT)
      .show()
  }
}