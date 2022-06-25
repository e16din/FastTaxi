package com.e16din.fasttaxi.implementation.utils.base

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BottomSheetFragmentSystemAgent() : BottomSheetDialogFragment() {

  val events = SystemAgentEvents()

  fun onBackPressed() {
    events.onBackPressed?.invoke()
    // NOTE: activity?.onBackPressed() not called here, if you need it call it on your SystemAgent(ore use finish() or etc)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

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