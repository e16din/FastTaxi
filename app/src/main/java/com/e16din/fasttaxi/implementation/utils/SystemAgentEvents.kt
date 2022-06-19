package com.e16din.fasttaxi.implementation.utils

import com.e16din.fasttaxi.implementation.JustEvent

class SystemAgentEvents {
  var onCreate: JustEvent? = null
  var onPause: JustEvent? = null
  var onResume: JustEvent? = null
  var onStop: JustEvent? = null
  var onStart: JustEvent? = null
  var onBackPressed: JustEvent? = null
}