package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.subjects.ServerAgent
import com.e16din.fasttaxi.architecture.subjects.SystemAgent
import com.e16din.fasttaxi.architecture.subjects.UserAgent
import com.e16din.fasttaxi.databinding.ScreenAuthBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.data.AuthData
import com.e16din.fasttaxi.implementation.utils.base.ActivitySystemAgent
import com.e16din.fasttaxi.implementation.utils.base.ScreenState
import com.e16din.fasttaxi.implementation.utils.handlytester.HandlyTester
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
  ) : ScreenState()

  val state = State()

  fun main() {
    fun checkLoginPassword(): Boolean {
      return checkConditions(listOf(
        Condition(
          "Логин от 4-х до 15-ти сиволов",
          state.login?.length in 4..15
        ),
        Condition(
          "Пароль от 6-ти символов",
          state.password?.length in 6..Int.MAX_VALUE
        )
      ), {}, {})
    }

    systemAgent.events.onCreate = systemAgent.event {
      val isEnterButtonEnabled = checkLoginPassword()
      userAgent.doUpdateSignInButtonState(isEnterButtonEnabled)
    }

    systemAgent.events.onStop = systemAgent.event {
      userAgent.doStopUiUpdates()
    }

    systemAgent.events.onStart = systemAgent.event {
      userAgent.doStartUiUpdates()
    }

    scenario(
      script = {
        // Given
        val login = "MyLogin"
        val password = "12345678jjiO"

        // When
        userAgent.onLoginChanged?.invoke(login)
        userAgent.onPasswordChanged?.invoke(password)

        // Then
        check(userAgent.binding.signInButton.isEnabled)
      },
      body = {
        userAgent.onLoginChanged = userAgent.event("onLoginChanged") { login ->
          state.login = login
          val isEnterButtonEnabled = checkLoginPassword()
          userAgent.doUpdateSignInButtonState(isEnterButtonEnabled)
        }

        userAgent.onPasswordChanged = userAgent.event("onPasswordChanged") { password ->
          state.password = password
          val isEnterButtonEnabled = checkLoginPassword()
          userAgent.doUpdateSignInButtonState(isEnterButtonEnabled)
        }
      }).pasteBody()
    // todo: call startScript to test scenario

    scenario(
      script = {
        // todo: run it in the scope
        // Given
        val isEnabled = true

        // When
        userAgent.onSignInClick?.invoke()

        // Then

        var stopped = false
        systemAgent.events.onStop = {
          stopped = true
        }
        while(!stopped) {
          // delay(1000)
          // todo: break on long check
        }
        check(stopped)

      },
      body = {
        userAgent.onSignInClick = userAgent.event("onSignInClick") {
          serverAgent.signIn(
            login = requireNotNull(state.login),
            password = requireNotNull(state.password),
            onResult = { result ->
              if (result.success) {
                state.isTopmost = false // todo: remove it
                systemAgent.hideScreen()
              } else {
                userAgent.showFailMessage()
              }
            })
        }
      }
    ).pasteBody()
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

    if (HandlyTester.isScenaryModeEnabled) {
      HandlyTester.testAuthScreen(screen)
    } else {
      screen.main()
      events.onCreate?.invoke()
    }
  }

  fun hideScreen() = doAction("AuthSystemAgent.hideScreen()") {
    FastTaxiApp.removeScreen(AuthScreen::class)
    finish()
  }
}

class AuthServerAgent : ServerAgent {

  private val scope = makeScope()

  fun signIn(
    login: String,
    password: String,
    onResult: (FastTaxiServer.Result<AuthData?>) -> Unit,
  ) = doAction(name = "signIn()", isAsync = true) {
    scope.launch {
      val result = withContext(Dispatchers.IO) {
        FastTaxiServer.auth(
          login = login,
          password = password
        )
      }

      onResult.invoke(result)
    }
  }
}

class AuthUserAgent(val binding: ScreenAuthBinding) : UserAgent {
  private lateinit var handler: Handler

  fun doStopUiUpdates() = doAction("Прекратить действия с UI") {
    handler.removeCallbacksAndMessages(null)
  }

  fun doStartUiUpdates() = doAction("Возобновить действия с UI") {
    handler = Handler()
  }

  fun doUpdateSignInButtonState(enabled: Boolean) = doAction("updateSignInButtonState()", enabled) {
    binding.signInButton.isEnabled = enabled
  }

  fun showFailMessage() = doAction("showFailMessage()") {
    Toast.makeText(binding.root.context, "Fail!", Toast.LENGTH_SHORT)
      .show()
  }

  var onLoginChanged: Event<String>? = null
  var onPasswordChanged: Event<String>? = null
  var onSignInClick: JustEvent? = null

  init {
    binding.loginField.addTextChangedListener { login ->
      // todo: add debounce
      onLoginChanged?.invoke(login.toString())
    }

    binding.passwordField.addTextChangedListener { password ->
      // todo: add debounce
      onPasswordChanged?.invoke(password.toString())
    }

    binding.signInButton.setOnClickListener {
      onSignInClick?.invoke()
    }
  }
}