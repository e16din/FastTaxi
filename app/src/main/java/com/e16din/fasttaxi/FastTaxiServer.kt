package com.e16din.fasttaxi

import com.e16din.fasttaxi.implementation.data.AddressPointData
import com.e16din.fasttaxi.implementation.data.AuthData

object FastTaxiServer {

  class Result<T>(
    val success: Boolean,
    val data: T,
  )

  fun getAddresses(addressQuery: String): Result<List<AddressPointData>> {
    // todo: load data from yandex geocoder
    val allPoints = listOf(
      AddressPointData(
        "Улица 1",
        "12",
        "Москва",
        AddressPointData.PointLocation(1.0, 2.0)),
      AddressPointData(
        "Улица 2",
        "12/2",
        "Москва",
        AddressPointData.PointLocation(3.0, 2.0)),
      AddressPointData(
        "Новая улица",
        "1",
        "Ростов-на-Дону",
        AddressPointData.PointLocation(1.1, 2.1))
    )

    val searchedPoints = allPoints.filter {
      it.getAddress().contains(addressQuery)
    }
    return Result(
      success = true,
      data = searchedPoints.ifEmpty { allPoints }
    )
  }

  fun auth(login: String, password: String): Result<AuthData?> {
    if (login.isNotBlank() && password.isNotBlank()) {
      return Result(
        success = true,
        data = AuthData("1", "Александр Кундрюков", "image_url", "token7657874")
      )
    } else {
      return Result(
        success = false,
        data = null
      )
    }
  }
}