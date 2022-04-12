package org.ergoplatform.explorer.http.api.models

import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import sttp.tapir.{Codec, DecodeResult}
import eu.timepit.refined.refineMV
import org.ergoplatform.explorer.constraints.OrderingString

final case class Sorting(sortBy: String, order: SortOrder)

object Sorting {

  sealed trait SortOrder { def value: OrderingString }

  object SortOrder {

    implicit val codec: Codec.PlainCodec[SortOrder] = Codec.string
      .mapDecode(fromString)(_.toString)

    private def fromString(s: String): DecodeResult[SortOrder] =
      s.trim.toLowerCase match {
        case "asc"  => DecodeResult.Value(Asc)
        case "desc" => DecodeResult.Value(Desc)
        case other  => DecodeResult.Mismatch("`asc` or `desc`", other)
      }
  }

  case object Asc extends SortOrder {
    override def toString: String = value.value
    def value: OrderingString = refineMV("asc")
  }

  case object Desc extends SortOrder {
    override def toString: String = value.value
    def value: OrderingString = refineMV("desc")
  }
}
