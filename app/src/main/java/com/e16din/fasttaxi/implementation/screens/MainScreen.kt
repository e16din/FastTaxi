package com.e16din.fasttaxi.implementation.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.databinding.ScreenMainBinding
import com.e16din.fasttaxi.implementation.*
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ScreenMainBinding

  class MainScreenState() : ScreenState

  var screenState = MainScreenState()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onEvent("ОС открыла главный экран") {
      feature("При открытии главного экрана") {
        binding = ScreenMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        screenState = FastTaxiApp.getScreenState()
          ?: MainScreenState()
        FastTaxiApp.addScreenState(screenState)

        if (!FastTaxiApp.profileFruit.isAuthorized()) {
          doAction("Открыть экран авторизации") {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
          }
          return@onEvent
        }
      }

      fun doShowSelectPointsScreen() = feature("Показать экран выбора адресов(только один)") {
        val alreadyStarted =
          FastTaxiApp.getScreenState<SelectPointsFragment.SelectPointsScreenState>() != null
        if (!alreadyStarted) {
          SelectPointsFragment().show(
            supportFragmentManager,
            SelectPointsFragment::class.java.simpleName
          )
        }
      }

      binding.defaultStartButton.setOnClickListener {
        onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ СТАРТА") {
          doAction("ОС открыла экран выбора адресов (default)") {
            doShowSelectPointsScreen()
          }
        }
      }

      fun onSelectStartPointClick() {
        onEvent("Пользователь нажал ВЫБРАТЬ ТОЧКУ ФИНИША") {
          doAction("ОС открыла экран выбора адресов") {
            doShowSelectPointsScreen()
          }
        }
      }

      binding.startButton.setOnClickListener {
        onSelectStartPointClick()
      }
      binding.finishButton.setOnClickListener {
        onSelectStartPointClick()
      }
      binding.orderButton.setOnClickListener {
        onEvent("Пользователь нажал ЗАКАЗАТЬ") {
          // todo:
        }
      }

      doShowOrderDetails()
    }
  }

  private fun doShowOrderDetails() = feature("Показать блок деталей заказа") {
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

    doAction("Показываем пользователю точки старта/финиша (в деталях заказа)", FastTaxiApp.orderFruit) {
      val hasNoAnyPoints = checkConditionsNot(hasPointsConditions, {}, {})
      binding.defaultStartButton.isVisible = hasNoAnyPoints
      binding.filledPointsContainer.isVisible = !hasNoAnyPoints

      binding.startButton.text = FastTaxiApp.orderFruit.startPoint?.getAddress()
      binding.finishButton.text = FastTaxiApp.orderFruit.finishPoint?.getAddress()
    }

    doAction("Показываем пользователю кнопку ЗАКАЗАТЬ (активную/неактивную)") {
      val hasBothPoints = checkConditions(hasPointsConditions, {}, {})
      binding.orderButton.isVisible = hasBothPoints
    }
  }

  override fun onResume() {
    super.onResume()
    onEvent("Система сказала что экран стал активным (после перекрытия или на старте)") {
      doShowOrderDetails()

      doAction("Показать выбранные точки на карте") {
        //TODO:
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
    onEvent("Пользователь нажал/свайпнул НАЗАД") {
      finish()
    }
  }
}