package com.e16din.fasttaxi.implementation.screens

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.subjects.ServerAgent
import com.e16din.fasttaxi.architecture.subjects.SystemAgent
import com.e16din.fasttaxi.databinding.ScreenMainBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.fruits.OrderFruit
import com.e16din.fasttaxi.implementation.utils.base.ActivitySystemAgent
import com.e16din.fasttaxi.implementation.utils.base.ScreenState
import com.e16din.fasttaxi.implementation.utils.handlytester.HandlyTester
import com.yandex.mapkit.MapKitFactory

class MainScreen : Screen {

  lateinit var systemAgent: MainSystemAgent
  lateinit var serverAgent: MainServerAgent
  lateinit var userAgent: MainUserAgent

  val orderFruit = OrderFruit()

  class State : ScreenState()
  val state = State()

  fun main() {
    systemAgent.events.onCreate = systemAgent.event("onCreate()") {
      if (!FastTaxiApp.profileFruit.isAuthorized()) {
        systemAgent.doShowAuthScreen()
        return@event
      }

      val hasStartPointCond = Condition(
        "Выбрана точка старта",
        orderFruit.startPoint?.address != null
      )
      val hasFinishPointCond = Condition(
        "Выбрана точка финиша",
        orderFruit.finishPoint?.address != null
      )

      val isDefaultModeEnabled = checkConditions(listOf(hasStartPointCond), {}, {})
      userAgent.doUpdateOrderDetails(
        isDefaultModeEnabled = isDefaultModeEnabled,
        startPointText = orderFruit.startPoint?.address,
        finishPointText = orderFruit.finishPoint?.address
      )

      val isOrderButtonEnabled = checkConditions(
        listOf(
          hasStartPointCond,
          hasFinishPointCond
        ), {}, {})
      userAgent.doUpdateOrderButton(isOrderButtonEnabled)
    }

    systemAgent.events.onStart = systemAgent.event("onStart") {
      state.isTopmost = true
    }

    userAgent.onSelectStartPointClick = userAgent.event("onSelectStartPointClick") {
      systemAgent.showSelectPointsScreen()
    }
    userAgent.onSelectFinishPointClick = userAgent.event("onSelectFinishPointClick") {
      systemAgent.showSelectPointsScreen()
    }
    userAgent.onOrderClick = userAgent.event("onOrderClick") {
    }

    systemAgent.events.onBackPressed = systemAgent.event("onBackPressed") {
      systemAgent.finish()
    }
  }
}

class MainSystemAgent : ActivitySystemAgent(), SystemAgent {

  private lateinit var binding: ScreenMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ScreenMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val screen = FastTaxiApp.getScreen()
      ?: MainScreen()
    screen.apply {
      serverAgent = MainServerAgent()
      systemAgent = this@MainSystemAgent
      userAgent = MainUserAgent(binding)
    }
    FastTaxiApp.addScreen(screen)

    if (HandlyTester.isScenaryModeEnabled) {
      HandlyTester.testMainScreen(screen)
    } else {
      screen.main()
      events.onCreate?.invoke()
    }
  }

  override fun onStop() {
    binding.mapView.onStop()
    MapKitFactory.getInstance().onStop()
    super.onStop()
  }

  override fun onStart() {
    super.onStart()
    MapKitFactory.getInstance().onStart()
    binding.mapView.onStart()
  }

  /// ACTIONS
  enum class ActionTypes(val text: String) {
    ShowAuthScreen("Открыть экран авторизации")
  }

  fun doShowAuthScreen() = doAction(ActionTypes.ShowAuthScreen.text) {
    val intent = Intent(this, AuthSystemAgent::class.java)
    startActivity(intent)
  }

  fun showSelectPointsScreen() = doAction {
    // todo: show SelectPointScreen
  }
}

class MainUserAgent(val binding: ScreenMainBinding) : ServerAgent {

  fun doUpdateOrderDetails(
    isDefaultModeEnabled: Boolean,
    startPointText: String?,
    finishPointText: String?,
  ) = doAction("updateOrderDetails()") {
    binding.defaultStartButton.isVisible = isDefaultModeEnabled
    binding.filledPointsContainer.isVisible = !isDefaultModeEnabled

    binding.startButton.text = startPointText
    binding.finishButton.text = finishPointText
  }

  fun doUpdateOrderButton(enabled: Boolean) = doAction("doUpdateOrderButton()") {
    binding.orderButton.isVisible = enabled
  }

  var onSelectStartPointClick: JustEvent? = null
  var onSelectFinishPointClick: JustEvent? = null
  var onOrderClick: JustEvent? = null

  init {
    binding.defaultStartButton.setOnClickListener {
      onSelectStartPointClick?.invoke()
    }
    binding.startButton.setOnClickListener {
      onSelectStartPointClick?.invoke()
    }
    binding.finishButton.setOnClickListener {
      onSelectFinishPointClick?.invoke()
    }
    binding.orderButton.setOnClickListener {
      onOrderClick?.invoke()
    }
  }
}

class MainServerAgent : ServerAgent {
  //
}