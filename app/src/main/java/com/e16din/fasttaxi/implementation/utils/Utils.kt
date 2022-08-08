package com.e16din.fasttaxi.implementation.utils

import java.nio.charset.Charset

fun Char.isLatinChar(): Boolean {
  val encoder = Charset.forName("US-ASCII").newEncoder()
  return encoder.canEncode(this)
}