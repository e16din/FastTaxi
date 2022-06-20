package com.e16din.fasttaxi.implementation.screens

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.e16din.fasttaxi.R
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.subjects.ServerAgent
import com.e16din.fasttaxi.architecture.subjects.SystemAgent
import com.e16din.fasttaxi.databinding.ScreenMainBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.data.AddressPointData
import com.e16din.fasttaxi.implementation.utils.ActivitySystemAgent
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.CoroutineScope

class MainScreen : Screen {

  lateinit var systemAgent: MainSystemAgent
  lateinit var serverAgent: MainServerAgent
  lateinit var userAgent: MainUserAgent

  fun main() {
    systemAgent.events.onCreate = systemAgent.event("onCreate()") {
      if (!FastTaxiApp.profileFruit.isAuthorized()) {
        systemAgent.showAuthScreen()
        return@event
      }

      userAgent.updateOrderDetails(
        startPoint = FastTaxiApp.orderFruit.startPoint,
        finishPoint = FastTaxiApp.orderFruit.finishPoint
      )
    }
    userAgent.onSelectStartPointClick = userAgent.event("onSelectStartPointClick()") {
      systemAgent.showSelectPointsScreen()
    }
    userAgent.onSelectFinishPointClick = userAgent.event("onSelectFinishPointClick()") {
      systemAgent.showSelectPointsScreen()
    }
    userAgent.onOrderClick = userAgent.event("onOrderClick()") {
    }

    systemAgent.events.onBackPressed = systemAgent.event("onBackPressed()") {
      systemAgent.finish()
    }
  }
}

class MainSystemAgent() : ActivitySystemAgent(), SystemAgent {

  private val screenScope = makeScreenScope()

  private lateinit var binding: ScreenMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ScreenMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val screen = FastTaxiApp.getScreen(MainScreen::class)
      ?: MainScreen()
    screen.apply {
      serverAgent = MainServerAgent(screenScope)
      systemAgent = this@MainSystemAgent
      userAgent = MainUserAgent(binding)
    }
    FastTaxiApp.addScreen(screen)

    screen.main()
    events.onCreate?.invoke()
  }

  private val navigator = AppNavigator(this, R.id.container)

  override fun onResume() {
    super.onResume()
    FastTaxiApp.navigatorHolder.setNavigator(navigator)
  }

  override fun onPause() {
    FastTaxiApp.navigatorHolder.removeNavigator()
    super.onPause()
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

  fun showAuthScreen() = doAction() {
    val intent = Intent(this, AuthSystemAgent::class.java)
    startActivity(intent)
  }

  fun showSelectPointsScreen() = doAction {
    // todo: show SelectPointScreen
  }
}

class MainUserAgent(val binding: ScreenMainBinding) : ServerAgent {

  fun updateOrderDetails(
    startPoint: AddressPointData?,
    finishPoint: AddressPointData?,
  ) = doAction("updateOrderDetails()") {
    val hasStartPoint = startPoint != null
    val hasFinishPoint = finishPoint != null
    val isAnyPointFilled = hasStartPoint || hasFinishPoint
    binding.defaultStartButton.isVisible = !isAnyPointFilled
    binding.filledPointsContainer.isVisible = isAnyPointFilled

    binding.startButton.text = startPoint?.address
    binding.finishButton.text = finishPoint?.address

    binding.orderButton.isVisible = hasStartPoint && hasFinishPoint
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

class MainServerAgent(val screenScope: CoroutineScope) : ServerAgent {
    //
}