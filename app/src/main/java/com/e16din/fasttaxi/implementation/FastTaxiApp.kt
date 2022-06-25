package com.e16din.fasttaxi.implementation

import android.app.Application
import com.e16din.fasttaxi.BuildConfig
import com.e16din.fasttaxi.architecture.App
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.implementation.fruits.OrderFruit
import com.e16din.fasttaxi.implementation.fruits.ProfileFruit
import com.e16din.fasttaxi.implementation.utils.redshadow.RedShadow
import com.yandex.mapkit.MapKitFactory
import kotlin.reflect.KClass

class FastTaxiApp : Application(), App {

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      RedShadow.init()
      val shadowActions = listOf(
        RedShadow.makePrintLogShadowAction()
      )
      RedShadow.shadowActions.addAll(shadowActions)
    }

    MapKitFactory.setApiKey("f601226d-e33a-453f-b2a5-2835e85fa373")
    MapKitFactory.initialize(this)
  }

  companion object {
    // NOTE: to save Screen objects on configuration changes (rotation and etc.)
    val activeScreensMap = mutableMapOf<String, Screen>()

    inline fun <reified T : Screen?> getScreen(): T? {
      return activeScreensMap[T::class.java.simpleName] as T
    }

    fun addScreen(screen: Screen) {
      val key = screen.javaClass.simpleName
      activeScreensMap[key] = screen
      RedShadow.onEvent("addScreen: $key", null, FastTaxiApp::class.java)
    }

    fun removeScreen(screenClass: KClass<*>) {
      val key = screenClass.simpleName
      activeScreensMap.remove(key)
      RedShadow.onEvent("removeScreen: $key", null, FastTaxiApp::class.java)
    }

    val profileFruit = ProfileFruit()
    val orderFruit = OrderFruit()
  }
}