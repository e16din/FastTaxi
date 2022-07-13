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
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.databinding.ItemAddressPointBinding
import com.e16din.fasttaxi.databinding.ScreenSelectAddressBinding
import com.e16din.fasttaxi.implementation.FastTaxiApp
import com.e16din.fasttaxi.implementation.data.AddressPointData
import com.e16din.fasttaxi.implementation.doAction
import com.e16din.fasttaxi.implementation.makeScope
import com.e16din.fasttaxi.implementation.onEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectPointsFragment : BottomSheetDialogFragment() {

  class SelectPointsScreenState(
    var selectionMode: SelectionMode = SelectionMode.Start,
  ) : ScreenState {
    enum class SelectionMode { Start, Finish }
  }

  private var screenState = SelectPointsScreenState()


  private lateinit var binding: ScreenSelectAddressBinding

  private val scope = makeScope()

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
      ?: SelectPointsScreenState()

    FastTaxiApp.addScreenState(screen)
    binding = ScreenSelectAddressBinding.inflate(layoutInflater)

    binding.startPointField.addTextChangedListener { query ->
      onEvent("Пользователь вводит точку старта") {
        doAction("Запросить адреса с сервера 1", query) {
          doGetAddresses(query.toString())
        }
      }
    }

    binding.finishPointField.addTextChangedListener { query ->
      screenState.selectionMode = SelectPointsScreenState.SelectionMode.Finish

      onEvent("Пользователь вводит точку финиша") {
        doAction("Запросить адреса с сервера 2", query) {
          doGetAddresses(query.toString())
        }
      }
    }

    binding.addressesList.layoutManager =
      LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
    binding.addressesList.adapter =
      AddressPointsAdapter(onPointSelected = { selectedPoint ->
        when (screenState.selectionMode) {
          SelectPointsScreenState.SelectionMode.Start -> {
            onEvent("Пользователь выбирает точку (старта)") {
              FastTaxiApp.orderFruit.startPoint = selectedPoint // "Обновить фрукт заказа"
              doAction("Показать выбранный адрес в активном поле ввода", FastTaxiApp.orderFruit) {
                doUpdateSelectedPoints()
              }
            }
          }
          SelectPointsScreenState.SelectionMode.Finish -> {
            onEvent("Пользователь выбирает точку (финиша)") {
              FastTaxiApp.orderFruit.finishPoint = selectedPoint // "Обновить фрукт заказа"
              doAction("Показать выбранный адрес в активном поле ввода", FastTaxiApp.orderFruit) {
                doUpdateSelectedPoints()
              }
            }
          }
        }
      })

    onEvent("ОС создает экран выбора точек (старт/финиш)") {
      doAction("Показать пользователю уже выбранные точки (старт/финиш)", FastTaxiApp.orderFruit) {
        doUpdateSelectedPoints()
      }
    }

    return binding.root
  }

  private fun doGetAddresses(query: String) {
    scope.launch(Dispatchers.Main) {
      val result = withContext(Dispatchers.IO) { FastTaxiServer.getAddresses(query) }
      when (result.success) {
        true -> {
          onEvent(
            "Сервер вернул запрошенные адреса"
          ) {
            doAction("Показать список адресов по введенному тексту из поля финиша") {
              (binding.addressesList.adapter as AddressPointsAdapter)
                .fill(result.data)
            }
          }
        }
        false -> {
          onEvent("Сервер вернул ошибку на запрос адресов") {
            val message = "Не удалось получить адреса"
            doAction("Показать пользователю сообщение об ошибке загрузки данных",
              message
            ) {
              Toast.makeText(binding.root.context, message, Toast.LENGTH_SHORT)
                .show()
            }
          }
        }
      }
    }
  }

  private fun doUpdateSelectedPoints(
  ) {
    val startPoint = FastTaxiApp.orderFruit.startPoint
    val finishPoint = FastTaxiApp.orderFruit.finishPoint

    binding.startPointField.setText(startPoint?.getAddress() ?: "")
    binding.finishPointField.setText(finishPoint?.getAddress() ?: "")
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