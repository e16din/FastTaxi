package com.e16din.fasttaxi.implementation.fruits

import com.e16din.fasttaxi.implementation.data.AddressPointData

data class OrderFruit(
  var startPoint: AddressPointData? = null,
  var finishPoint: AddressPointData? = null,
)