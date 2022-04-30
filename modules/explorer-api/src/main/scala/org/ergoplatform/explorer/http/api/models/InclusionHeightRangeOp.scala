package org.ergoplatform.explorer.http.api.models

final case class InclusionHeightRangeOp(fromHeight: Option[Int], toHeight: Option[Int])
final case class InclusionHeightRange(fromHeight: Int, toHeight: Int)
