package com.e16din.fasttaxi.implementation.fruits

import com.e16din.fasttaxi.implementation.data.AddressPointData
import kotlinx.serialization.Serializable

@Serializable
data class OrderFruit(
  var startPoint: AddressPointData? = null,
  var finishPoint: AddressPointData? = null,
)