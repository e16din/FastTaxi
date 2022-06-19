package com.e16din.redshadow

import android.util.Log

typealias RedShadowAction =
      (name: String, actor: Class<*>, isEvent: Boolean, isStart: Boolean, isAsync: Boolean) -> Unit

// todo: create annotation @RedShadow to get function name and arguments automatically
object RedShadow {
  val shadowActions = mutableListOf<RedShadowAction>()

  // todo: add action to save log to file
  // todo: add action to send data to analytics
  fun makePrintLogShadowAction(): RedShadowAction =
    { name: String?, subject: Class<*>, isEvent: Boolean, isStart: Boolean, isAsync: Boolean ->
      val subject = "${subject.simpleName}"
      val startOrEnd = if (isEvent) "[Event]" else if (isStart) "[Action][Start]" else "[Action][End]"
      val asyncOrNot = if (isAsync) "[Async]" else ""
      val message = "$subject:$startOrEnd$asyncOrNot $name"
      Log.i("RedShadow", message)
    }

  fun onEvent(name: String, actor: Class<*>) {
    shadowActions.forEach { it.invoke(name, actor, true, false, false) }
  }

  fun onActionStart(name: String?, isAsync: Boolean = false, actor: Class<*>) {
    shadowActions.forEach { it.invoke("$name", actor, false, true, isAsync) }
  }

  fun onActionEnd(name: String?, isAsync: Boolean = false, actor: Class<*>) {
    shadowActions.forEach { it.invoke("$name", actor, false, false, isAsync) }
  }
}