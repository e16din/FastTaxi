package com.e16din.fasttaxi.implementation.fruits

import com.e16din.fasttaxi.architecture.Fruit

class ProfileFruit(
  var token: String? = null,
) : Fruit {

  fun isAuthorized() = token != null
}