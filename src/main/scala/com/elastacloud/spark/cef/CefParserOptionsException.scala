package com.elastacloud.spark.cef

case class CefParserOptionsException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) {
    this(message, null)
  }
}
