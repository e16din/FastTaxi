package com.e16din.fasttaxi.implementation.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.LocalDataSource
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.databinding.ItemAddressPointBinding
import com.e16din.fasttaxi.databinding.ScreenSelectAddressBinding
import com.e16din.fasttaxi.implementation.*
import com.e16din.fasttaxi.implementation.data.AddressPointData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectPointsFragment : BottomSheetDialogFragment() {

  data class SelectPointsScreenFruit(
    var selectionMode: SelectionMode = SelectionMode.Start,
    var startPointQuery: String = "",
    var finishPointQuery: String = "",
    var addressesResult: FastTaxiServer.Result<List<AddressPointData>>? = null,
  ) : ScreenState {

    fun getPoints(): List<AddressPointData> {
      return addressesResult?.data ?: emptyList()
    }

    enum class SelectionMode {
      Start,
      Finish
    }
  }

  private var screenFruit = SelectPointsScreenFruit()


  private lateinit var binding: ScreenSelectAddressBinding

  private val scope = makeScope()

  class Events {
    val onCreate = Event("onCreate: Система создает экран выбора точек")

    val onGetAddressesSuccess = Event("Сервер вернул запрошенные адреса")
    val onGetAddressesFail = Event("Сервер вернул ошибку на запрос адресов")

    val onStartPointQueryChanged = Event("Пользователь вводит точку старта")
    val onStartPointSelected = Event("Пользователь выбирает точку (старта)")

    val onFinishPointQueryChanged = Event("Пользователь вводит точку финиша")
    val onFinishPointSelected = Event("Пользователь выбирает точку (финиша)")

    val onStartPointModeSelected = Event("Чел активировал режим поиска точки старта")
    val onFinishPointModeSelected = Event("Чел активировал режим поиска точки финиша")
  }

  private val events = Events()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    dialog?.let {
      val sheet = it as BottomSheetDialog
      sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    val screen = FastTaxiApp.getScreenState()
      ?: SelectPointsScreenFruit()

    FastTaxiApp.addScreenState(screen)
    binding = ScreenSelectAddressBinding.inflate(layoutInflater)

    binding.startPointField.addTextChangedListener { query ->
      fieldsHandler.doLast {
        screenFruit.startPointQuery = query.toString()
        if (query.isNullOrEmpty()) {
          FastTaxiApp.orderFruit.startPoint = null
        }
        events.onStartPointQueryChanged.call()
      }
    }
    binding.finishPointField.addTextChangedListener { query ->
      fieldsHandler.doLast {
        screenFruit.finishPointQuery = query.toString()
        if (query.isNullOrEmpty()) {
          FastTaxiApp.orderFruit.finishPoint = null
        }
        events.onFinishPointQueryChanged.call()
      }
    }

    binding.startPointField.setOnFocusChangeListener { v, hasFocus ->
      if (hasFocus) {
        screenFruit.selectionMode = SelectPointsScreenFruit.SelectionMode.Start
        events.onStartPointModeSelected.call()
      }
    }
    binding.finishPointField.setOnFocusChangeListener { v, hasFocus ->
      if (hasFocus) {
        screenFruit.selectionMode = SelectPointsScreenFruit.SelectionMode.Finish
        events.onFinishPointModeSelected.call()
      }
    }

    binding.addressesList.layoutManager =
      LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
    binding.addressesList.adapter = AddressPointsAdapter(onPointSelected = { selectedPoint ->
      when (screenFruit.selectionMode) {
        SelectPointsScreenFruit.SelectionMode.Start -> {
          FastTaxiApp.orderFruit.startPoint = selectedPoint
          events.onStartPointSelected.call()
        }
        SelectPointsScreenFruit.SelectionMode.Finish -> {
          FastTaxiApp.orderFruit.finishPoint = selectedPoint
          events.onFinishPointSelected.call()
        }
      }
    })

    main()

    events.onCreate.call()

    return binding.root
  }

  private fun main() {
    doAction(
      desc = "Показать пользователю уже выбранные точки (старт/финиш)",
      events = listOf(
        events.onCreate,
        events.onStartPointSelected,
        events.onFinishPointSelected
      )
    ) {
      val startAddress = FastTaxiApp.orderFruit.startPoint?.getAddress() ?: ""
      binding.startPointField.setText(startAddress)

      val finishAddress = FastTaxiApp.orderFruit.finishPoint?.getAddress() ?: ""
      binding.finishPointField.setText(finishAddress)
    }

    doAction(
      desc = "Сохранить выбранные адреса 1",
      events = listOf(events.onStartPointQueryChanged)
    ) {
      LocalDataSource.saveLocalData(data(FastTaxiApp.orderFruit))
    }

    doAction(
      desc = "Показать список адресов по введенному тексту из поля финиша",
      events = listOf(events.onGetAddressesSuccess)
    ) {
      (binding.addressesList.adapter as AddressPointsAdapter)
        .fill(screenFruit.getPoints())
    }

    doAction(
      desc = "Показать пользователю сообщение об ошибке загрузки данных",
      events = listOf(events.onGetAddressesFail)
    ) {
      val message = "Не удалось получить адреса"
      Toast.makeText(binding.root.context, data(message), Toast.LENGTH_SHORT)
        .show()
    }

    doAction(
      desc = "Запросить адреса с сервера",
      events = listOf(
        events.onStartPointQueryChanged,
        events.onFinishPointQueryChanged
      )
    ) {
      val query = when (screenFruit.selectionMode) {
        SelectPointsScreenFruit.SelectionMode.Start -> screenFruit.startPointQuery
        SelectPointsScreenFruit.SelectionMode.Finish -> screenFruit.finishPointQuery
      }

      scope.launch(Dispatchers.Main) {
        val addressesResult = withContext(Dispatchers.IO) {
          FastTaxiServer.getAddresses(data(query))
        }
        screenFruit.addressesResult = addressesResult
        when (addressesResult.success) {
          true -> {
            events.onGetAddressesSuccess.call()
          }
          false -> {
            events.onGetAddressesFail.call()
          }
        }
      }
    }

    doAction(
      desc = "Сохранить выбранные адреса",
      events = listOf(
        events.onStartPointSelected,
        events.onFinishPointSelected
      )
    ) {
      LocalDataSource.saveLocalData(FastTaxiApp.orderFruit)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    FastTaxiApp.removeScreenState(SelectPointsScreenFruit::class)
    super.onDismiss(dialog)
  }

  class AddressPointsAdapter(
    val onPointSelected: (point: AddressPointData) -> Unit,
  ) : RecyclerView.Adapter<AddressPointsAdapter.AddressPointViewHolder>() {

    class AddressPointViewHolder(
      val binding: ItemAddressPointBinding,
    ) :
      RecyclerView.ViewHolder(binding.root) {

      fun bind(item: AddressPointData, onPointSelected: (point: AddressPointData) -> Unit) {
        binding.addressLabel.text = item.getAddress()
        binding.additionLabel.text = item.city
        itemView.setOnClickListener {
          onPointSelected.invoke(item)
        }
      }
    }

    private val items = mutableListOf<AddressPointData>()

    fun fill(items: List<AddressPointData>) {
      this.items.clear()
      this.items.addAll(items)
      notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressPointViewHolder {
      val layoutInflater = LayoutInflater.from(parent.context)
      val binding = ItemAddressPointBinding.inflate(layoutInflater)
      return AddressPointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressPointViewHolder, position: Int) {
      holder.bind(items[position], onPointSelected)
    }

    override fun getItemCount(): Int {
      return items.size
    }
  }
}