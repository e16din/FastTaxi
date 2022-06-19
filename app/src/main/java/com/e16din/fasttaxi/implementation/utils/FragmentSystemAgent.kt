package com.e16din.fasttaxi.implementation.utils

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

abstract class FragmentSystemAgent() : Fragment() {

  val events = SystemAgentEvents()

  fun onBackPressed() {
    events.onBackPressed?.invoke()
    // NOTE: activity?.onBackPressed() not called here, if you need it call it on your SystemAgent(ore use finish() or etc)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    activity?.onBackPressedDispatcher?.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          onBackPressed()
        }
      })
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