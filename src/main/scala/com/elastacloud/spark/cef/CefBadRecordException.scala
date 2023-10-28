package com.elastacloud.spark.cef

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.unsafe.types.UTF8String

case class CefBadRecordException(
                                  @transient record: () => UTF8String,
                                  @transient partialResult: () => Option[InternalRow],
                                  cause: Throwable) extends Exception(cause)
