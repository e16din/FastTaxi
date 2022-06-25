package com.e16din.fasttaxi.implementation.utils.redshadow

import android.util.Log

typealias RedShadowAction = (
  name: String,
  data: Any?,
  subject: Class<*>,
  isEvent: Boolean,
  isStart: Boolean,
  isFail: Boolean,
) -> Unit

// todo: create annotation @RedShadow to get function name and arguments automatically
object RedShadow {

  val actionsHistory = mutableListOf<String>()
  val shadowActions = mutableListOf<RedShadowAction>()

  fun init() {
    shadowActions.add { name, _, _, _, _, _ ->
      actionsHistory.add(name)
    }
  }

  // todo: add action to save log to file
  // todo: add action to send data to analytics
  fun makePrintLogShadowAction(): RedShadowAction =
    { name: String?, data: Any?, subject: Class<*>, isEvent: Boolean, isStart: Boolean, isFail: Boolean ->
      val startOrEnd =
        if (isEvent) "[Event]" else if (isStart) "[Action][Start]" else "[Action][End]"
      val nameAndData = if (data == null) name else "$name | data = $data"
      val subject = subject.simpleName
      val message = "$subject:$startOrEnd $nameAndData"
      if (isFail) {
        Log.e("RedShadow", message)
      } else {
        Log.i("RedShadow", message)
      }
    }

  fun onError(name: String, data: Any?, subject: Class<*>) {
    actionsHistory.add(name)
    shadowActions.forEach { it.invoke(name, data, subject, true, false,  true) }
  }

  fun onEvent(name: String?, data: Any?, subject: Class<*>) {
    shadowActions.forEach { it.invoke("$name", data, subject, true, false,  false) }
  }

  fun onActionStart(name: String?, data: Any?, subject: Class<*>) {
    shadowActions.forEach { it.invoke("$name", data, subject, false, true, false) }
  }

  fun onActionEnd(name: String?, data: Any?, subject: Class<*>) {
    shadowActions.forEach { it.invoke("$name", data, subject, false, false, false) }
  }
}