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
