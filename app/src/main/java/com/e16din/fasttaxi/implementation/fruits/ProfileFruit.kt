package com.e16din.fasttaxi.implementation.fruits

import com.e16din.fasttaxi.architecture.Fruit
import kotlinx.serialization.Serializable

@Serializable
data class ProfileFruit(
  var token: String? = null,
) : Fruit {

  fun isAuthorized() = token != null
}