package com.e16din.fasttaxi.implementation.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.e16din.fasttaxi.FastTaxiServer
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.subjects.ServerAgent
import com.e16din.fasttaxi.architecture.subjects.SystemAgent
import com.e16din.fasttaxi.architecture.subjects.UserAgent
import com.e16din.fasttaxi.databinding.ItemAddressPointBinding
import com.e16din.fasttaxi.databinding.ScreenSelectAddressBinding
import com.e16din.fasttaxi.implementation.FastTaxiApp
import com.e16din.fasttaxi.implementation.data.AddressPointData
import com.e16din.fasttaxi.implementation.doAction
import com.e16din.fasttaxi.implementation.fruits.OrderFruit
import com.e16din.fasttaxi.implementation.makeScope
import com.e16din.fasttaxi.implementation.onEvent
import com.e16din.fasttaxi.implementation.utils.base.BottomSheetFragmentSystemAgent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectPointsScreen(val orderFruit: OrderFruit) : Screen {
  lateinit var systemAgent: SelectPointsSystemAgent
  lateinit var serverAgent: SelectPointsServerAgent
  lateinit var userAgent: SelectPointsUserAgent

  fun main() {
    systemAgent.events.onCreate =
      systemAgent.onEvent("ОС создает экран выбора точек (старт/финиш)") {
        userAgent.doUpdateSelectedPoints("Показать пользователю уже выбранные точки (старт/финиш)",
          orderFruit.startPoint,
          orderFruit.finishPoint
        )
      }

    userAgent.onStartAddressQueryChanged = userAgent.onEvent(
      "Пользователь вводит точку старта"
    ) { query ->
      serverAgent.doGetAddresses("Запросить адреса с сервера", query)
    }

    userAgent.onFinishAddressQueryChanged = userAgent.onEvent(
      "Пользователь вводит точку финиша"
    ) { query ->
      serverAgent.doGetAddresses("Запросить адреса с сервера", query)
    }

    serverAgent.onAddressesResult = serverAgent.onEvent(
      "Сервер вернул запрошенные адреса"
    ) { addresses ->
      userAgent.doShowAddresses(
        "Показать список адресов по введенному тексту из поля финиша",
        addresses
      )
    }

    serverAgent.onFailResult = serverAgent.onEvent(
      "Сервер вернул ошибку на запрос адресов"
    ) { message ->
      userAgent.doShowLoadingFailed(
        "Показать пользователю сообщение об ошибке загрузки данных",
        message
      )
    }

    userAgent.onStartAddressPointSelected = userAgent.onEvent<AddressPointData>(
      "Пользователь выбирает точку (старта)"
    ) { startPoint ->
      orderFruit.startPoint = startPoint // "Обновить фрукт заказа"
      userAgent.doUpdateSelectedPoints("Показать выбранный адрес в активном поле ввода",
        orderFruit.startPoint,
        orderFruit.finishPoint
      )
    }

    userAgent.onFinishAddressPointSelected = userAgent.onEvent<AddressPointData>(
      "Пользователь выбирает точку (финиша)"
    ) { finishPoint ->
      orderFruit.finishPoint = finishPoint // "Обновить фрукт заказа"
      userAgent.doUpdateSelectedPoints("Показать выбранный адрес в активном поле ввода",
        orderFruit.startPoint,
        orderFruit.finishPoint
      )
    }
  }
}

class SelectPointsSystemAgent : BottomSheetFragmentSystemAgent(), SystemAgent {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    dialog?.let {
      val sheet = it as BottomSheetDialog
      sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    val binding = ScreenSelectAddressBinding.inflate(layoutInflater)

    val screen = FastTaxiApp.getScreen()
      ?: SelectPointsScreen(FastTaxiApp.orderFruit)
    screen.apply {
      serverAgent = SelectPointsServerAgent()
      systemAgent = this@SelectPointsSystemAgent
      userAgent = SelectPointsUserAgent(binding)
    }
    FastTaxiApp.addScreen(screen)

    screen.main()
    events.onCreate?.invoke()

    return binding.root
  }
}

class SelectPointsUserAgent(val binding: ScreenSelectAddressBinding) : UserAgent {

  lateinit var onStartAddressQueryChanged: (query: String) -> Unit
  lateinit var onFinishAddressQueryChanged: (query: String) -> Unit

  lateinit var onStartAddressPointSelected: (point: AddressPointData) -> Unit
  lateinit var onFinishAddressPointSelected: (point: AddressPointData) -> Unit

  enum class SelectionMode { Start, Finish }

  private var selectionMode = SelectionMode.Start

  init {
    binding.startPointField.addTextChangedListener {
      selectionMode = SelectionMode.Start
      onStartAddressQueryChanged.invoke(it.toString())
    }
    binding.finishPointField.addTextChangedListener {
      selectionMode = SelectionMode.Finish
      onFinishAddressQueryChanged.invoke(it.toString())
    }

    binding.addressesList.layoutManager =
      LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
    binding.addressesList.adapter = AddressPointsAdapter(onPointSelected = { point ->
      when (selectionMode) {
        SelectionMode.Start -> onStartAddressPointSelected.invoke(point)
        SelectionMode.Finish -> onFinishAddressPointSelected.invoke(point)
      }
    })
  }

  fun doUpdateSelectedPoints(
    desc: String,
    startPoint: AddressPointData?,
    finishPoint: AddressPointData?,
  ) = doAction(desc, listOf(startPoint, finishPoint)) {
    binding.startPointField.setText(startPoint?.getAddress() ?: "")
    binding.finishPointField.setText(finishPoint?.getAddress() ?: "")
  }

  fun doShowLoadingFailed(desc: String, message: String) = doAction(desc, message) {
    Toast.makeText(binding.root.context, message, Toast.LENGTH_SHORT)
      .show()
  }

  fun doShowAddresses(desc: String, addresses: List<AddressPointData>) = doAction(desc) {
    (binding.addressesList.adapter as AddressPointsAdapter)
      .fill(addresses)
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

    val items = mutableListOf<AddressPointData>()

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

class SelectPointsServerAgent : ServerAgent {

  val scope = makeScope()

  lateinit var onAddressesResult: (addresses: List<AddressPointData>) -> Unit
  lateinit var onFailResult: (message: String) -> Unit


  fun doGetAddresses(
    desc: String,
    query: String,
  ) = doAction(desc, query) {
    scope.launch(Dispatchers.Main) {
      val result = withContext(Dispatchers.IO) { FastTaxiServer.getAddresses(query) }
      when (result.success) {
        true -> onAddressesResult.invoke(result.data)
        false -> onFailResult.invoke("Не удалось получить адреса")
      }
    }
  }
}