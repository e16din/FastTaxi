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
import com.e16din.fasttaxi.implementation.utils.handlytester.HandlyTester
import com.yandex.mapkit.MapKitFactory

class MainScreen : Screen {

  lateinit var systemAgent: MainSystemAgent
  lateinit var serverAgent: MainServerAgent
  lateinit var userAgent: MainUserAgent

  val orderFruit = OrderFruit()

  fun main() {
    systemAgent.events.onCreate = systemAgent.onEvent("ОС открыла главный экран") {
      if (!FastTaxiApp.profileFruit.isAuthorized()) {
        systemAgent.doShowAuthScreen()
        return@onEvent
      }

      val hasStartPointCond = Condition(
        desc = "Выбрана точка старта",
        value = orderFruit.startPoint?.address,
        checkFunction = { !it.isNullOrBlank() }
      )
      val hasFinishPointCond = Condition(
        desc = "Выбрана точка финиша",
        value = orderFruit.finishPoint?.address,
        checkFunction = { !it.isNullOrBlank() }
      )

      val isDefaultModeEnabled = checkConditions(listOf(hasStartPointCond), {}, {})
      userAgent.doUpdateOrderDetails(
        desc = "Показываем пользователю детали заказа (в начальном/развернутом формате)",
        isDefaultModeEnabled = isDefaultModeEnabled,
        startPointText = orderFruit.startPoint?.address,
        finishPointText = orderFruit.finishPoint?.address
      )

      val isOrderButtonEnabled = checkConditions(
        listOf(
          hasStartPointCond,
          hasFinishPointCond
        ), {}, {})
      userAgent.doUpdateOrderButton(
        desc = "Показываем пользователю кнопку ЗАКАЗАТЬ (активную/неактивную)",
        enabled = isOrderButtonEnabled
      )
    }

    userAgent.onSelectStartPointClick = userAgent.onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ СТАРТА") {
      systemAgent.doShowSelectPointsScreen("ОС открыла экран выбора адресов")
    }
    userAgent.onSelectFinishPointClick = userAgent.onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ ФИНИША") {
      systemAgent.doShowSelectPointsScreen("ОС открыла экран выбора адресов")
    }
    userAgent.onOrderClick = userAgent.onEvent("Пользователь нажал ЗАКАЗАТЬ") {
    }

    systemAgent.events.onBackPressed = systemAgent.onEvent("Пользователь нажал/свайпнул НАЗАД") {
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

    if (HandlyTester.isTestModeEnabled) {
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

  fun doShowSelectPointsScreen(desc: String) = doAction(desc) {
    // todo: show SelectPointScreen
  }
}

class MainUserAgent(val binding: ScreenMainBinding) : ServerAgent {

  fun doUpdateOrderDetails(
    desc: String,
    isDefaultModeEnabled: Boolean,
    startPointText: String?,
    finishPointText: String?
  ) = doAction(desc) {
    binding.defaultStartButton.isVisible = isDefaultModeEnabled
    binding.filledPointsContainer.isVisible = !isDefaultModeEnabled

    binding.startButton.text = startPointText
    binding.finishButton.text = finishPointText
  }

  fun doUpdateOrderButton(desc: String, enabled: Boolean) = doAction(desc) {
    binding.orderButton.isVisible = enabled
  }

  var onSelectStartPointClick: JustEventBlock? = null
  var onSelectFinishPointClick: JustEventBlock? = null
  var onOrderClick: JustEventBlock? = null

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