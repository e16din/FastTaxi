package com.e16din.fasttaxi.implementation.navigation

import android.app.Activity
import android.content.Intent
import com.e16din.fasttaxi.implementation.screens.AuthSystemAgent
import com.github.terrakok.cicerone.androidx.ActivityScreen

object NavigationScreens {
  fun Auth(activity: Activity) = ActivityScreen {
    Intent(activity, AuthSystemAgent::class.java)
  }
}