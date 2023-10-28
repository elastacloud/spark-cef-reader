package com.elastacloud.spark.cef

import CefParserOptions._
import org.apache.commons.codec.language.DoubleMetaphone
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.FileSourceOptions
import org.apache.spark.sql.catalyst.util.{CaseInsensitiveMap, ParseMode}
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import scala.collection.convert.ImplicitConversions.`map AsScala`
import scala.collection.mutable.ArrayBuffer

private[cef] class CefParserOptions(
  @transient val parameters: CaseInsensitiveMap[String])
  extends FileSourceOptions(parameters) with Logging {

  def this(parameters: Map[String, String]) = {
    this(CaseInsensitiveMap(parameters))
  }

  def this() = {
    this(Map[String, String]())
  }

  /**
   * Is the required date format epoch milliseconds
   *
   * @return true if the date format is for epoch milliseconds
   */
  def isDateFormatInMillis: Boolean = {
    dateFormat.compareToIgnoreCase("millis") == 0
  }

  checkInvalidOptions(parameters.keySet) match {
    case Some(errors) => throw new CefParserOptionsException(s"Unable to parse options:\n${errors.mkString("\n")}")
    case _ =>
  }

  val maxRecords: Int = parameters.getOrElse("maxRecords", "10000").toInt
  val pivotFields: Boolean = parameters.getOrElse("pivotFields", "false").toBoolean
  val mode: ParseMode = ParseMode.fromString(parameters.getOrElse("mode", "permissive"))
  val corruptColumnName: String = parameters.getOrElse("corruptRecordColumnName", null)
  val defensiveMode: Boolean = parameters.getOrElse("defensiveMode", "false").toBoolean
  val nullValue: String = parameters.getOrElse("nullValue", "-")
  val dateFormat: String = validateDateFormat(parameters.getOrElse("dateFormat", "MMM dd yyyy HH:mm:ss.SSS zzz"))
}

private[cef] object CefParserOptions {
  private val encoder = new DoubleMetaphone()

  private val validDateFormats = Vector[String](
    "millis",
    "MMM dd yyyy HH:mm:ss",
    "MMM dd yyyy HH:mm:ss.SSS zzz",
    "MMM dd yyyy HH:mm:ss.SSS",
    "MMM dd yyyy HH:mm:ss zzz",
    "MMM dd HH:mm:ss",
    "MMM dd HH:mm:ss.SSS zzz",
    "MMM dd HH:mm:ss.SSS",
    "MMM dd HH:mm:ss zzz"
  )

  private val validOptions = Map[String, String](
    encoder.encode("maxRecords") -> "maxRecords",
    encoder.encode("pivotFields") -> "pivotFields",
    encoder.encode("corruptRecordColumnName") -> "corruptRecordColumnName",
    encoder.encode("defensiveMode") -> "defensiveMode",
    encoder.encode("nullValue") -> "nullValue",
    encoder.encode("dateFormat") -> "dateFormat"
  )

  private def isValidDateFormat(userFormat: String): Boolean = {
    userFormat.compareToIgnoreCase("millis") == 0 || validDateFormats.exists(_.compareTo(userFormat) == 0)
  }

  private def validateDateFormat(userFormat: String): String = {
    if (!isValidDateFormat(userFormat)) {
      throw new CefParserOptionsException(s"Unable to parse date format '$userFormat', valid options are: ${validDateFormats.mkString(", ")}")
    }
    userFormat
  }

  private def checkInvalidOptions(keys: Set[String]): Option[Seq[String]] = {
    val buffer = new ArrayBuffer[String]()

    keys.foreach { key =>
      val encodedKey = encoder.encode(key)
      if (!validOptions.values.exists(_.compareToIgnoreCase(key) == 0) && validOptions.contains(encodedKey)) {
        val errorString = s"Unable to find option '$key', did you mean '${validOptions(encodedKey)}'"
        buffer.append(errorString)
      }
    }

    if (buffer.isEmpty) {
      None
    } else {
      Some(buffer)
    }
  }

  /**
   * Create a new set of parser options from a [[CaseInsensitiveStringMap]]
   *
   * @param options a map of options to create from
   * @return a new [[CefParserOptions]] instance
   */
  def from(options: CaseInsensitiveStringMap): CefParserOptions = {
    new CefParserOptions(options.toMap)
  }

  /**
   * Create a new set of parser options from a [[Map]]
   *
   * @param options a map of options to create from
   * @return a new [[CefParserOptions]] instance
   */
  def from(options: Map[String, String]): CefParserOptions = {
    new CefParserOptions(options)
  }
}
