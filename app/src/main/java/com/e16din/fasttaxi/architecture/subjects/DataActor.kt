package com.e16din.fasttaxi.architecture.subjects

import com.e16din.fasttaxi.architecture.Subject
import com.e16din.fasttaxi.implementation.makeScope

object DataActor : Subject {
  val scope = makeScope() // todo: exclude to BaseActor
}
