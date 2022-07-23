package com.mattzq.migrate

import scala.util.{ Try, Failure, Success }

extension [T](value: T | Null)
  inline def toOption: Option[T] =
    if (value == null) None else Some(value)

  inline def toFailure(throwable: Throwable): Try[T] =
    if (value == null) Failure(throwable) else Success(value)

  // https://dotty.epfl.ch/3.0.0/docs/reference/other-new-features/explicit-nulls.html
  inline def nn: T =
    assert(value != null)
    value.asInstanceOf[T]

def formatTable[T](
    values: List[T],
    columns: List[String],
    widths: List[Int],
    row: T => List[String],
  ): String =
  val delim = " | "
  val padding = ' '
  columns.zipWithIndex.map((v, index) => v.padTo(widths(index), padding)).mkString(delim)
    + "\n"
    + widths.indices.map(index => "-" * widths(index)).mkString("-|-")
    + "\n"
    + values
      .map(value =>
        row(value)
          .zipWithIndex
          .map((v, index) => v.padTo(widths(index), padding))
          .mkString(delim)
      )
      .mkString("\n")
