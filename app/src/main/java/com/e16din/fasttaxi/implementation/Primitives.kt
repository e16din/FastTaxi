package com.e16din.fasttaxi.implementation

import com.e16din.fasttaxi.architecture.Screen
import com.e16din.fasttaxi.architecture.Subject
import com.e16din.redshadow.RedShadow
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
    RedShadow.onEvent("$name | data = $data", this.javaClass)
    onEvent.invoke(data)
  }

inline fun Subject.event(
  name: String? = null,
  crossinline onEvent: JustEvent,
): JustEvent = {
  RedShadow.onEvent("$name", this.javaClass)
  onEvent.invoke()
}

typealias Action = () -> Unit

fun Subject.doAction(
  name: String? = null,
  data: Any? = null,
  isAsync: Boolean = false,
  action: Action,
) {
  val actionName = if (data == null) name else "$name | data = $data"
  RedShadow.onActionStart(actionName, isAsync, this.javaClass)
  action.invoke()
  RedShadow.onActionEnd(actionName, isAsync, this.javaClass)
}

fun Subject.makeScreenScope() =
  CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, t ->
    RedShadow.onError("[Fail!] ${t.stackTraceToString()}", this.javaClass)
  })

class Condition(val desc: String, val value: Boolean)

// можно делать TDD без автотестов
inline fun Screen.checkConditions(
  conditions: List<Condition>,
  crossinline onOk: () -> Unit,
  crossinline onNotOk: (falseConditions: List<Condition>) -> Unit,
) {

  if (conditions.all { it.value }) {
    RedShadow.onEvent("[Check] Ok", this.javaClass)
    onOk.invoke()
  } else {
    val falseConditions = conditions.filter { !it.value }
    falseConditions.forEach {
      RedShadow.onEvent("[Check] Not Ok | ${it.desc}", this.javaClass)
    }
    onNotOk.invoke(falseConditions)
  }
}

class DisposablePackage<T> {
  var value: T? = null
    get() {
      val result = field
      field = null
      return result
    }
}

