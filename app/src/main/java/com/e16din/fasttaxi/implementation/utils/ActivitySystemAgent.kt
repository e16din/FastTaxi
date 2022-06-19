package com.e16din.fasttaxi.implementation.utils

import androidx.appcompat.app.AppCompatActivity

abstract class ActivitySystemAgent() : AppCompatActivity() {

  val events = SystemAgentEvents()

  override fun onBackPressed() {
    events.onBackPressed?.invoke()
    // NOTE: activity?.onBackPressed() not called here, if you need it call it on your SystemAgent(ore use finish() or etc)
  }

  override fun onPause() {
    events.onPause?.invoke()
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    events.onResume?.invoke()
  }

  override fun onStop() {
    events.onStop?.invoke()
    super.onStop()
  }

  override fun onStart() {
    super.onStart()
    events.onStart?.invoke()
  }
}