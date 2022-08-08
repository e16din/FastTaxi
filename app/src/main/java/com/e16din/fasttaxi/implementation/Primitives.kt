package com.e16din.fasttaxi.implementation

import android.os.Handler
import android.os.Looper
import com.e16din.fasttaxi.BuildConfig
import com.e16din.fasttaxi.implementation.utils.redshadow.RedShadow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicInteger


val stepsHistory = mutableListOf<Step>()

class Event(
  val desc: String,
) {
  private val listeners = mutableListOf<() -> Unit>()

  fun call(data: Any? = null) {
    listeners.forEach {
      data(data)
      RedShadow.onEvent(desc, Event::class.java)
      stepsHistory.add(EventStep(desc))

      it.invoke()
    }
  }

  fun listen(onEvent: () -> Unit) {
    listeners.add(onEvent)
  }
}

inline fun Any.doAction(
  desc: String? = null, // todo: сделать ссылками как в стектрейсе
  events: List<Event>,
  crossinline onAction: () -> Unit,
) {

  events.forEach { event ->
    event.listen {
      val name = desc ?: "doAction()"
      RedShadow.onActionStart(name, this.javaClass)
      onAction.invoke()
      stepsHistory.add(ActionStep(name))
      RedShadow.onActionEnd(desc, this.javaClass)
    }
  }
}

inline fun feature(desc: String, onEvent: () -> Unit) {
  onEvent.invoke()
}

inline fun <T> Any.data(value: T): T {
  RedShadow.onEvent("data: $value", this.javaClass)
  return value
}

inline fun Any.desc(text: String) {
  RedShadow.onEvent("data: $text", this.javaClass)
}

fun Handler.doLast(delay: Long = 350L, call: () -> Unit) {
  this.removeCallbacksAndMessages(null)
  this.postDelayed({
    call.invoke()
  }, delay)
}

val coroutineScopeFailActionName = "CoroutineScopeFail"
fun Any.makeScope() =
  CoroutineScope(Dispatchers.Main + CoroutineExceptionHandler { _, t ->
    RedShadow.onError(coroutineScopeFailActionName, t.stackTraceToString(), this.javaClass)
  })

class Condition<T : Any?>(
  val desc: String,
  val value: T,
  val falseValues: List<T> = emptyList(),
  val trueValues: List<T> = emptyList(),
  val checkFunction: (data: T) -> Boolean,
)

inline fun <T> Any.checkConditionsNot(
  conditions: List<Condition<T>>,
  crossinline onOk: () -> Unit,
  crossinline onNotOk: (falseConditions: List<Condition<T>>) -> Unit,
): Boolean {
  return checkConditions(conditions, onOk, onNotOk, invert = true)
}

inline fun <T> Any.checkConditions(
  conditions: List<Condition<T>>,
  crossinline onOk: () -> Unit,
  crossinline onNotOk: (falseConditions: List<Condition<T>>) -> Unit,
  invert: Boolean = false, // NOTE: Нужен чтобы перевернуть все объекты Condition
): Boolean {
  if (BuildConfig.DEBUG) {
    // Тестировать falseValues и trueValues,
    // если в логике условий ошибка, пусть она вскроется сразу и бросит исключение
    conditions.forEach { condition ->
      condition.falseValues.forEach { falseValue ->
        val result = !condition.checkFunction.invoke(falseValue)
        if (!result) {
          RedShadow.onError("[Test False Value] Not Ok", condition.desc, this.javaClass)
          RedShadow.onError("[Test False Value] Not Ok", falseValue, this.javaClass)
        }
        check(result)
      }

      condition.trueValues.forEach { trueValue ->
        val result = condition.checkFunction.invoke(trueValue)
        if (!result) {
          RedShadow.onError("[Test True Value] Not Ok", condition.desc, this.javaClass)
          RedShadow.onError("[Test True Value] Not Ok", trueValue, this.javaClass)
        }
        check(result)
      }
    }
  }

  // Проверяем условия, выдаем результат
  val successConditions = conditions.filter {
    if (invert)
      !it.checkFunction.invoke(it.value)
    else
      it.checkFunction.invoke(it.value)
  }
  val failConditions = conditions - successConditions.toSet()

  if (failConditions.isEmpty()) {
    RedShadow.onEvent("[Check] Ok", this.javaClass)
    onOk.invoke()
    return true

  } else {
    failConditions.forEach {
      RedShadow.onEvent("[Check] Not Ok | ${it.desc}", this.javaClass)
    }
    onNotOk.invoke(failConditions)
    return false
  }
}

private val uniqueStepsCount = AtomicInteger()
fun generateStepId() = uniqueStepsCount.incrementAndGet()

class ActionStep(desc: String, id: Int = generateStepId()) : Step(id, desc, Type.Action)
class EventStep(desc: String, id: Int = generateStepId()) : Step(id, desc, Type.Event)
abstract class Step(
  var id: Int,
  var desc: String,
  var type: Type,
) {
  enum class Type {
    Action,
    Event
  }
}

val clicksHandler = Handler(Looper.getMainLooper())
val fieldsHandler = Handler(Looper.getMainLooper())
