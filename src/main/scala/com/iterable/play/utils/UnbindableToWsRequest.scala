package com.iterable.play.utils

trait UnbindableToWsRequest[T <: UnbindableToWsRequest[T]] { self: T =>
  def unbindToWsRequest(implicit caseClassMapping: CaseClassMapping[T]): Map[String, Seq[String]] = implicitly[CaseClassMapping[T]].unbindToWsRequest(this)
}
