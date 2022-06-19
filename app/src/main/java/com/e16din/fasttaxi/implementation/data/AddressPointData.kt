package com.e16din.fasttaxi.implementation.data

class AddressPointData(
  val address: String?,
  val location: PointLocation?,
) {
  class PointLocation(val latitude: Long, val longitude: Long)
}