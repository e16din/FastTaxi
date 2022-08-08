package com.e16din.fasttaxi.implementation.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.e16din.fasttaxi.R
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.databinding.ScreenMainBinding
import com.e16din.fasttaxi.implementation.*
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ScreenMainBinding

  class MainScreenState : ScreenState

  private var screenState = MainScreenState()

  private val events = Events()
  private val conditions = Conditions()

  private fun main() {
    doAction(
      desc = "Открыть экран авторизации",
      events = listOf(
        events.onCreate
      )
    ) {
      if (!FastTaxiApp.profileFruit.isAuthorized()) {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
      }
    }

    doAction( // Система
      desc = "Открыть экран выбора адресов",
      events = listOf(
        events.onSelectDefaultStartPointClick,
        events.onSelectStartPointClick,
        events.onSelectFinishPointClick,
      )
    ) {
      val alreadyStarted =
        FastTaxiApp.getScreenState<SelectPointsFragment.SelectPointsScreenFruit>() != null
      if (!alreadyStarted) {
        val selectPointsFragment = SelectPointsFragment()
        selectPointsFragment.callbacks.onExit.listen {
          events.onSelectPointsScreenExit.call()
        }
        selectPointsFragment.show(
          supportFragmentManager,
          SelectPointsFragment::class.java.simpleName
        )
      }
    }

    feature("Показать блок деталей заказа")
    {
      doAction(
        desc = "Показываем пользователю точки старта/финиша (в деталях заказа)",
        events = listOf(
          events.onCreate,
          events.onSelectPointsScreenExit
        )
      ) {
        binding.defaultStartButton.isVisible = conditions.hasNoAnyPoints()
        binding.filledPointsContainer.isVisible = !conditions.hasNoAnyPoints()

        with(FastTaxiApp.orderFruit) {
          binding.startButton.text =
            "Старт: " + (startPoint?.getAddress() // todo: сделать иконки старта/финиша, удалить текст
              ?: getString(R.string.where_to_go))
          binding.finishButton.text = "Финиш: " + (finishPoint?.getAddress()
            ?: getString(R.string.where_to_go))
        }
      }

      doAction(
        desc = "Показываем пользователю кнопку ЗАКАЗАТЬ (активную/неактивную)",
        events = listOf(
          events.onCreate,
          events.onSelectPointsScreenExit
        )
      ) {
        binding.orderButton.isVisible = conditions.hasBothPoints()
      }

      doAction(
        desc = "Показать экран пойска машины",
        events = listOf(events.onOrderClick)
      ) {
        //TODO:
      }
    }

    doAction(
      desc = "Система закрывает экран",
      events = listOf(
        events.onBackPressed,
      )
    ) {
      finish()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ScreenMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    screenState = FastTaxiApp.getScreenState()
      ?: MainScreenState()
    FastTaxiApp.addScreenState(screenState)

    binding.defaultStartButton.setOnClickListener {
      events.onSelectDefaultStartPointClick.call()
    }
    binding.startButton.setOnClickListener {
      events.onSelectStartPointClick.call()
    }
    binding.finishButton.setOnClickListener {
      events.onSelectFinishPointClick.call()
    }
    binding.orderButton.setOnClickListener {
      events.onOrderClick.call()
    }

    main()

    events.onCreate.call()
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
    events.onBackPressed.call()
  }

  class Events {
    val onCreate = Event("onCreate: ОС открыла главный экран")
    var onSelectPointsScreenExit = Event("Скрыто окно выбора точек адресов")
    val onBackPressed = Event("Чел нажал/свайпнул НАЗАД")

    val onSelectDefaultStartPointClick = Event("Чел нажал ВЫБРАТЬ ТОЧКУ СТАРТА (default)")
    val onSelectStartPointClick = Event("Чел нажал ВЫБРАТЬ ТОЧКУ СТАРТА")
    val onSelectFinishPointClick = Event("Чел нажал ВЫБРАТЬ ТОЧКУ ФИНИША")
    val onOrderClick = Event("Чел нажал ЗАКАЗАТЬ")
  }

  inner class Conditions {
    private val falseValues = listOf(
      null,
      "",
      "    "
    )
    private val hasPointsConditions = listOf(
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

    fun hasNoAnyPoints() = checkConditionsNot(hasPointsConditions, {}, {})
    fun hasBothPoints() = checkConditions(hasPointsConditions, {}, {})
  }
}