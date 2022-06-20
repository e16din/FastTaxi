package com.e16din.redshadow

import android.util.Log

typealias RedShadowAction = (
  name: String,
  subject: Class<*>,
  isEvent: Boolean,
  isStart: Boolean,
  isAsync: Boolean,
  isFail: Boolean
) -> Unit

// todo: create annotation @RedShadow to get function name and arguments automatically
object RedShadow {
  val shadowActions = mutableListOf<RedShadowAction>()

  // todo: add action to save log to file
  // todo: add action to send data to analytics
  fun makePrintLogShadowAction(): RedShadowAction =
    { name: String?, subject: Class<*>, isEvent: Boolean, isStart: Boolean, isAsync: Boolean, isFail:Boolean ->
      val subject = "${subject.simpleName}"
      val startOrEnd =
        if (isEvent) "[Event]" else if (isStart) "[Action][Start]" else "[Action][End]"
      val asyncOrNot = if (isAsync) "[Async]" else ""
      val message = "$subject:$startOrEnd$asyncOrNot $name"
      if(isFail) {
        Log.e("RedShadow", message)
      } else {
        Log.i("RedShadow", message)
      }
    }

  fun onError(name: String, subject: Class<*>) {
    shadowActions.forEach { it.invoke(name, subject, true, false, false, true) }
  }

  fun onEvent(name: String, subject: Class<*>) {
    shadowActions.forEach { it.invoke(name, subject, true, false, false, false) }
  }

  fun onActionStart(name: String?, isAsync: Boolean = false, subject: Class<*>) {
    shadowActions.forEach { it.invoke("$name", subject, false, true, isAsync, false) }
  }

  fun onActionEnd(name: String?, isAsync: Boolean = false, subject: Class<*>) {
    shadowActions.forEach { it.invoke("$name", subject, false, false, isAsync, false) }
  }
}