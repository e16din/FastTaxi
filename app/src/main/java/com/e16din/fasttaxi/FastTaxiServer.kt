package com.e16din.fasttaxi

import com.e16din.fasttaxi.implementation.data.AuthData

object FastTaxiServer {

  class Result<T>(
    val success: Boolean,
    val data: T,
  )

//  fun getMapPoints(addressQuery: String): Result<List<TaxiPointData>> {
//    // todo: load data from yandex geocoder
//    val allPoints = listOf(
//      TaxiPointData("Address1", "Name1",
//        TaxiPointData.Location(1.0, 2.0))
//    )
//
//    val searchedPoints = allPoints.filter {
//      it.address.contains(addressQuery) || it.name.contains(addressQuery)
//    }
//    return Result(
//      success = true,
//      data = searchedPoints.ifEmpty { allPoints }
//    )
//  }

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