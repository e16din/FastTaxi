package com.e16din.fasttaxi.implementation.utils.base

import com.e16din.fasttaxi.implementation.JustEventBlock

class SystemAgentEvents {
  var onCreate: JustEventBlock? = null
  var onPause: JustEventBlock? = null
  var onResume: JustEventBlock? = null
  var onStop: JustEventBlock? = null
  var onStart: JustEventBlock? = null
  var onBackPressed: JustEventBlock? = null
}