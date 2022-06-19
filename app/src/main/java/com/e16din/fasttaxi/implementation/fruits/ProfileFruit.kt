package com.e16din.fasttaxi.implementation.fruits

import com.e16din.fasttaxi.architecture.Fruit

class ProfileFruit() : Fruit {
  private val token: String? = null

  fun isAuthorized() = token != null
}