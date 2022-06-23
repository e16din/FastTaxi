package com.e16din.fasttaxi.implementation

import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.Subject
import com.e16din.fasttaxi.implementation.utils.redshadow.RedShadow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

typealias Event<T> = (data: T) -> Unit
typealias JustEvent = () -> Unit

inline fun <T> Subject.event(
  name: String? = null,
  crossinline onEvent: Event<T>,
): Event<T> =
  { data: T ->
    RedShadow.onEvent(name, data, this.javaClass)
    onEvent.invoke(data)
  }

inline fun Subject.event(
  name: String? = null,
  crossinline onEvent: JustEvent,
): JustEvent = {
  RedShadow.onEvent("$name", null, this.javaClass)
  onEvent.invoke()
}

typealias Action = () -> Unit

fun Subject.doAction(
  name: String? = null,
  data: Any? = null,
  isAsync: Boolean = false,
  action: Action,
) {
  RedShadow.onActionStart(name, data, isAsync, this.javaClass)
  action.invoke()
  RedShadow.onActionEnd(name, data, isAsync, this.javaClass)
}

val coroutineScopeFailActionName = "CoroutineScopeFail"
fun Subject.makeScope() =
  CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, t ->
    RedShadow.onError(coroutineScopeFailActionName, t.stackTraceToString(), this.javaClass)
  })

class Condition(val desc: String, val value: Boolean)

val checkOkActionName = "[Check] Ok"
val checkNotOkActionName = "[Check] Not Ok"
inline fun Screen.checkConditions(
  conditions: List<Condition>,
  crossinline onOk: () -> Unit,
  crossinline onNotOk: (falseConditions: List<Condition>) -> Unit,
): Boolean {

  if (conditions.all { it.value }) {
    RedShadow.onEvent(checkOkActionName, null, this.javaClass)
    onOk.invoke()
    return true

  } else {
    val falseConditions = conditions.filter { !it.value }
    falseConditions.forEach {
      RedShadow.onEvent(checkNotOkActionName, it.desc, this.javaClass)
    }
    onNotOk.invoke(falseConditions)
    return false
  }
}

