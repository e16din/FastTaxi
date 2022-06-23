package com.e16din.fasttaxi.implementation.utils.handlytester

import com.e16din.fasttaxi.implementation.FastTaxiApp
import com.e16din.fasttaxi.implementation.coroutineScopeFailActionName
import com.e16din.fasttaxi.implementation.screens.AuthScreen
import com.e16din.fasttaxi.implementation.screens.MainScreen
import com.e16din.fasttaxi.implementation.screens.MainSystemAgent
import com.e16din.fasttaxi.implementation.utils.redshadow.RedShadow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HandlyTester {

  private val scope = CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, t ->
    RedShadow.onError(coroutineScopeFailActionName, t.stackTraceToString(), this.javaClass)
  })

  var isScenaryModeEnabled = false

  val testActionName = "[Test]"

  fun testAuthScreen(screen: AuthScreen) {
    screen.state
    FastTaxiApp.profileFruit.token = null
  }

  fun testMainScreen(screen: MainScreen) {
    scope.launch(Dispatchers.Main) {
      RedShadow.onEvent(
        name = testActionName,
        data = "Start: Если пользователь не авторизован, то показать экран логина",
        subject = HandlyTester::class.java
      )
      // settings
      FastTaxiApp.profileFruit.token = null

      // events
      screen.main()
      screen.systemAgent.events.onCreate?.invoke()

//      delay(500)

      // check
      check(
        RedShadow.actionsHistory.contains(MainSystemAgent.ActionTypes.ShowAuthScreen.text)
      )
      RedShadow.onEvent(testActionName, "Ok!", HandlyTester::class.java)
    }
  }

  fun runSmokeTests() {
    val mainScreen = MainScreen().apply {
//      systemAgent = //todo:
//      serverAgent = //todo:
//      userAgent = //todo:
    }
    testMainScreen(mainScreen)
  }
}