package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
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
import com.e16din.fasttaxi.implementation.utils.ActivitySystemAgent
import kotlinx.coroutines.CoroutineScope
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

  private val state = State()

  fun main() {
    systemAgent.events.onCreate = systemAgent.event {
      //todo: call quit onStop() here, reset screenScope onStart()

      fun updateSignInButtonState() {
        val conditions = listOf(
          Condition(
            "Логин от 4-х до 15-ти сиволов",
            state.login?.length in 4..15),
          Condition("Пароль от 6-ти символов",
            state.password?.length in 6..Int.MAX_VALUE)
        )
        checkConditions( // todo: вызывать на каждое действие субъекта
          conditions,
          onOk = {
            userAgent.doUpdateSignInButtonState(true)
          },
          onNotOk = {
            // todo: handle conditions
            userAgent.doUpdateSignInButtonState(false)
          }
        )
      }

      updateSignInButtonState()

      userAgent.onLoginChanged = userAgent.event("Ввод логина") { login ->
        state.login = login
        updateSignInButtonState()
      }

      userAgent.onPasswordChanged = userAgent.event("onPasswordChanged()") { password ->
        state.password = password
        updateSignInButtonState()
      }

      userAgent.onSignInClick = userAgent.event("onSignInClick()") {
        serverAgent.signIn(
          login = requireNotNull(state.login),
          password = requireNotNull(state.password),
          onResult = { result ->
            if (result.success) {
              systemAgent.hideScreen()
            } else {
              userAgent.showFailMessage()
            }
          })
      }
    }
  }
}

class AuthSystemAgent : ActivitySystemAgent(), SystemAgent {

  private val screenScope = makeScreenScope()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = ScreenAuthBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val screen = FastTaxiApp.getScreen(AuthScreen::class)
      ?: AuthScreen()
    screen.apply {
      serverAgent = AuthServerAgent(screenScope)
      systemAgent = this@AuthSystemAgent
      userAgent = AuthUserAgent(binding)
    }
    FastTaxiApp.addScreen(screen)

    screen.main()
    events.onCreate?.invoke()
  }

  fun hideScreen() = doAction("AuthSystemAgent.hideScreen()") {
    FastTaxiApp.removeScreen(AuthScreen::class)
    finish()
  }
}

class AuthServerAgent(private val screenScope: CoroutineScope) : ServerAgent {

  fun signIn(
    login: String,
    password: String,
    onResult: (FastTaxiServer.Result<AuthData?>) -> Unit,
  ) = doAction(name = "signIn()", isAsync = true) {
    screenScope.launch {
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

class AuthUserAgent(private val binding: ScreenAuthBinding) : UserAgent {

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