package com.e16din.fasttaxi

import android.content.Context
import android.content.SharedPreferences
import com.e16din.fasttaxi.implementation.FastTaxiApp
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object LocalDataSource {

  lateinit var sharedPreferences: SharedPreferences

  fun init(context: Context) {
    sharedPreferences = context.getSharedPreferences(
      FastTaxiApp::class.qualifiedName,
      Context.MODE_PRIVATE
    )
  }

  inline fun <reified T> saveLocalData(data: T) {
    val json = Json.encodeToString(data)
    with(sharedPreferences.edit()) {
      putString(T::class.simpleName, json)
      apply()
    }
  }

  @OptIn(InternalSerializationApi::class)
  inline fun <reified T : Any> loadLocalData(cls: KClass<T>): T? {
    val json = sharedPreferences.getString(cls.simpleName, null)
    return if (json == null) {
      null
    } else {
      Json.decodeFromString(cls.serializer(), json)
    }
  }
}