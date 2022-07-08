package com.e16din.fasttaxi.implementation.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.architecture.subjects.SystemActor
import com.e16din.fasttaxi.architecture.subjects.UserActor
import com.e16din.fasttaxi.databinding.ScreenMainBinding
import com.e16din.fasttaxi.implementation.*
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ScreenMainBinding

  class MainScreenState() : ScreenState
  var screenState = MainScreenState()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ScreenMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    screenState = FastTaxiApp.getScreenState()
      ?: MainScreenState()
    FastTaxiApp.addScreenState(screenState)

    fun SystemActor.doShowSelectPointsScreen(desc: String) = SystemActor.doAction(desc) {
      SelectPointsFragment().show(
        supportFragmentManager,
        SelectPointsFragment::class.java.simpleName
      )
    }

    binding.defaultStartButton.setOnClickListener {
      UserActor.onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ СТАРТА") {
        SystemActor.doShowSelectPointsScreen("ОС открыла экран выбора адресов (default)")
      }
    }

    fun onSelectStartPointClick() {
      UserActor.onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ ФИНИША") {
        SystemActor.doShowSelectPointsScreen("ОС открыла экран выбора адресов")
      }
    }

    binding.startButton.setOnClickListener {
      onSelectStartPointClick()
    }
    binding.finishButton.setOnClickListener {
      onSelectStartPointClick()
    }
    binding.orderButton.setOnClickListener {
      UserActor.onEvent("Пользователь нажал ЗАКАЗАТЬ") {
        // todo:
      }
    }

    SystemActor.onEvent("ОС открыла главный экран") {
      if (!FastTaxiApp.profileFruit.isAuthorized()) {
        SystemActor.doAction("Открыть экран авторизации") {
          val intent = Intent(this, AuthActivity::class.java)
          startActivity(intent)
        }
        return@onEvent
      }

      val falseValues = listOf(
        null,
        "",
        "    "
      )

      val hasPointsConditions = listOf(
        Condition(
          desc = "Выбрана точка старта",
          falseValues = falseValues,
          value = FastTaxiApp.orderFruit.startPoint?.getAddress(),
          checkFunction = { !it.isNullOrBlank() }
        ),
        Condition(
          desc = "Выбрана точка финиша",
          falseValues = falseValues,
          value = FastTaxiApp.orderFruit.finishPoint?.getAddress(),
          checkFunction = { !it.isNullOrBlank() }
        )
      )
      val hasNoAnyPoints = checkConditionsNot(hasPointsConditions, {}, {})

      UserActor.doAction("Показываем пользователю детали заказа (в начальном/развернутом формате)") {
        binding.defaultStartButton.isVisible = hasNoAnyPoints
        binding.filledPointsContainer.isVisible = !hasNoAnyPoints

        binding.startButton.text = FastTaxiApp.orderFruit.startPoint?.getAddress()
        binding.finishButton.text = FastTaxiApp.orderFruit.finishPoint?.getAddress()
      }

      val hasBothPoints = checkConditions(hasPointsConditions, {}, {})
      UserActor.doAction("Показываем пользователю кнопку ЗАКАЗАТЬ (активную/неактивную)") {
        binding.orderButton.isVisible = hasBothPoints
      }
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

  override fun onBackPressed() {
    SystemActor.onEvent("Пользователь нажал/свайпнул НАЗАД") {
      finish()
    }
  }
}