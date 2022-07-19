package com.e16din.fasttaxi.implementation

import android.app.Application
import com.e16din.fasttaxi.BuildConfig
import com.e16din.fasttaxi.LocalDataSource
import com.e16din.fasttaxi.architecture.App
import com.e16din.fasttaxi.architecture.ScreenState
import com.e16din.fasttaxi.implementation.fruits.OrderFruit
import com.e16din.fasttaxi.implementation.fruits.ProfileFruit
import com.e16din.fasttaxi.implementation.utils.redshadow.RedShadow
import com.yandex.mapkit.MapKitFactory
import kotlin.reflect.KClass

class FastTaxiApp : Application(), App {

  override fun onCreate() {
    super.onCreate()

    LocalDataSource.init(this)

    profileFruit = LocalDataSource.loadLocalData(ProfileFruit::class)
      ?: ProfileFruit()
    orderFruit = LocalDataSource.loadLocalData(OrderFruit::class)
      ?: OrderFruit()

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
    val activeScreensStatesMap = mutableMapOf<String, ScreenState>()

    inline fun <reified T : ScreenState?> getScreenState(): T? {
      return activeScreensStatesMap[T::class.java.simpleName] as T?
    }

    fun addScreenState(screenState: ScreenState) {
      val key = screenState.javaClass.simpleName
      activeScreensStatesMap[key] = screenState
      RedShadow.onEvent("addScreen: $key", FastTaxiApp::class.java)
    }

    fun removeScreenState(cls: KClass<*>) {
      val key = cls.simpleName
      activeScreensStatesMap.remove(key)
      RedShadow.onEvent("removeScreen: $key",  FastTaxiApp::class.java)
    }

    lateinit var profileFruit: ProfileFruit
    lateinit var orderFruit: OrderFruit
  }
}