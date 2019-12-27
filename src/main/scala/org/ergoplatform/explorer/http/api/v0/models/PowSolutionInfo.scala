package org.ergoplatform.explorer.http.api.v0.models

import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class PowSolutionInfo(pk: String, w: String, n: String, d: String)

object PowSolutionInfo {

  implicit val schema: Schema[PowSolutionInfo] =
    implicitly[Derived[Schema[PowSolutionInfo]]].value
}
