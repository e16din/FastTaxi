package com.e16din.fasttaxi.architecture.subjects

import com.e16din.fasttaxi.architecture.Subject
import com.e16din.fasttaxi.implementation.makeScope

object SystemActor: Subject {
  val scope = makeScope()
}