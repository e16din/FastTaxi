package com.e16din.fasttaxi.implementation.data

import kotlinx.serialization.Serializable

@Serializable
data class AddressPointData(
  val street: String?,
  val houseNumber: String?,
  val city: String?,
  val location: PointLocation?,
) {
  @Serializable
  class PointLocation(var latitude: Double, var longitude: Double)

  fun getAddress(): String {
    return "$street, $houseNumber"
  }
}