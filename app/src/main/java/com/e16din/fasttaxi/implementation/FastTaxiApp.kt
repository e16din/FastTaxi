package com.e16din.fasttaxi.implementation

import android.app.Application
import com.e16din.fasttaxi.architecture.App
import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.implementation.fruits.OrderFruit
import com.e16din.fasttaxi.implementation.fruits.ProfileFruit
import com.e16din.redshadow.BuildConfig
import com.e16din.redshadow.RedShadow
import com.github.terrakok.cicerone.Cicerone
import com.yandex.mapkit.MapKitFactory
import kotlin.reflect.KClass

class FastTaxiApp : Application(), App {

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      val shadowActions = listOf(
        RedShadow.makePrintLogShadowAction()
      )
      RedShadow.shadowActions.addAll(shadowActions)
    }

    MapKitFactory.setApiKey("f601226d-e33a-453f-b2a5-2835e85fa373")
    MapKitFactory.initialize(this)
  }

  companion object {
    // todo: remove cicerone
    private val cicerone = Cicerone.create()
    val router get() = cicerone.router
    val navigatorHolder get() = cicerone.getNavigatorHolder()

    // NOTE: to save Screen objects on configuration changes (rotation and etc.)
    private val activeScreensMap = mutableMapOf<String, Screen>()

    fun <T : Screen?> getScreen(key: KClass<*>): T {
      return activeScreensMap[key.simpleName] as T
    }

    fun addScreen(screen: Screen) {
      val key = screen.javaClass.simpleName
      activeScreensMap[key] = screen
      RedShadow.onEvent("addScreen: $key", FastTaxiApp::class.java)
    }

    fun removeScreen(screenClass: KClass<*>) {
      val key = screenClass.simpleName
      activeScreensMap.remove(key)
      RedShadow.onEvent("removeScreen: $key", FastTaxiApp::class.java)
    }

    val orderFruit = OrderFruit()
    val profileFruit = ProfileFruit()
  }
}