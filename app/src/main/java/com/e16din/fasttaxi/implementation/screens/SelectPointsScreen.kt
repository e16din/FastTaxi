package com.e16din.fasttaxi.implementation.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.e16din.fasttaxi.FastTaxiServer
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

  class SelectPointsScreenState(
    var selectionMode: SelectionMode = SelectionMode.Start,
  ) : ScreenState {
    enum class SelectionMode {
      Start,
      Finish
    }
  }

  private var screenState = SelectPointsScreenState()


  private lateinit var binding: ScreenSelectAddressBinding

  private val scope = makeScope()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    feature("При открытии экрана выбора адресов") {
      dialog?.let {
        val sheet = it as BottomSheetDialog
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
      }

      val screen = FastTaxiApp.getScreenState()
        ?: SelectPointsScreenState()

      FastTaxiApp.addScreenState(screen)
      binding = ScreenSelectAddressBinding.inflate(layoutInflater)

      onEvent("ОС создает экран выбора точек (старт/финиш)") {
        doAction("Показать пользователю уже выбранные точки (старт/финиш)",
          FastTaxiApp.orderFruit) {
          doUpdateSelectedPoints()
        }
      }
    }

    fun doGetAddresses(query: String) = feature("Запрос адресов и обработка ответа") {
      scope.launch(Dispatchers.Main) {
        val addressesResult = withContext(Dispatchers.IO) {
          FastTaxiServer.getAddresses(query)
        }
        when (addressesResult.success) {
          true -> {
            onEvent("Сервер вернул запрошенные адреса") {
              doAction("Показать список адресов по введенному тексту из поля финиша") {
                (binding.addressesList.adapter as AddressPointsAdapter)
                  .fill(addressesResult.data)
              }
            }
          }
          false -> {
            onEvent("Сервер вернул ошибку на запрос адресов") {
              val message = "Не удалось получить адреса"
              doAction("Показать пользователю сообщение об ошибке загрузки данных", message) {
                Toast.makeText(binding.root.context, message, Toast.LENGTH_SHORT)
                  .show()
              }
            }
          }
        }
      }
    }

    fun initQueryChangedFeature(
      field: EditText,
      selectionMode: SelectPointsScreenState.SelectionMode,
    ) = feature("Обработка ввода текста в поля поиска") {
      field.addTextChangedListener { query ->
        onEvent("Пользователь вводит точку старта") {
          if (query.isNullOrBlank()) {
            doAction("Обновить фрукт деталей заказа, точку старта/финиша", selectionMode) {
              when (selectionMode) {
                SelectPointsScreenState.SelectionMode.Start -> {
                  FastTaxiApp.orderFruit.startPoint = null
                }
                SelectPointsScreenState.SelectionMode.Finish -> {
                  FastTaxiApp.orderFruit.finishPoint = null
                }
              }
            }
          }
          doAction("Запросить адреса с сервера 2", query) {
            doGetAddresses(query.toString())
          }
        }
      }
    }
    initQueryChangedFeature(
      field = binding.startPointField,
      selectionMode = SelectPointsScreenState.SelectionMode.Start
    )
    initQueryChangedFeature(
      field = binding.finishPointField,
      selectionMode = SelectPointsScreenState.SelectionMode.Finish
    )

    fun initChangeFieldsFeature(
      field: EditText,
      selectionMode: SelectPointsScreenState.SelectionMode,
    ) = feature("Переключение между полями Старт/Финиш") {
      field.setOnFocusChangeListener { v, hasFocus ->
        if (hasFocus) {
          doAction("Обновить стейт, режим показа адресов", selectionMode) {
            screenState.selectionMode = selectionMode
          }

          val query = field.text.toString()
          doAction("Запросить адреса с сервера 1", query) {
            doGetAddresses(query)
          }
        }
      }
    }
    initChangeFieldsFeature(
      field = binding.startPointField,
      selectionMode = SelectPointsScreenState.SelectionMode.Start
    )
    initChangeFieldsFeature(
      field = binding.finishPointField,
      selectionMode = SelectPointsScreenState.SelectionMode.Finish
    )

    feature("Показ найденых адресов") {
      binding.addressesList.layoutManager =
        LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
      binding.addressesList.adapter = AddressPointsAdapter(onPointSelected = { selectedPoint ->
        feature("выбор адреса из списка") {
          when (screenState.selectionMode) {
            SelectPointsScreenState.SelectionMode.Start -> {
              onEvent("Пользователь выбирает точку (старта)", selectedPoint) {
                doAction("Обновить фрукт заказа (Start)", selectedPoint) {
                  FastTaxiApp.orderFruit.startPoint = selectedPoint
                }
                doAction("Показать выбранный адрес в активном поле ввода", FastTaxiApp.orderFruit) {
                  doUpdateSelectedPoints()
                }
              }
            }
            SelectPointsScreenState.SelectionMode.Finish -> {
              onEvent("Пользователь выбирает точку (финиша)", selectedPoint) {
                doAction("Обновить фрукт заказа (Finish)", selectedPoint) {
                  FastTaxiApp.orderFruit.finishPoint = selectedPoint
                }
                doAction("Показать выбранный адрес в активном поле ввода", FastTaxiApp.orderFruit) {
                  doUpdateSelectedPoints()
                }
              }
            }
          }
        }
      })
    }

    return binding.root
  }

  override fun onDismiss(dialog: DialogInterface) {
    FastTaxiApp.removeScreenState(SelectPointsScreenState::class)
    super.onDismiss(dialog)
  }

  private fun doUpdateSelectedPoints() = feature("Показать выбранные адреса") {
    val startAddress = FastTaxiApp.orderFruit.startPoint?.getAddress() ?: ""
    binding.startPointField.setText(startAddress)

    val finishAddress = FastTaxiApp.orderFruit.finishPoint?.getAddress() ?: ""
    binding.finishPointField.setText(finishAddress)
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