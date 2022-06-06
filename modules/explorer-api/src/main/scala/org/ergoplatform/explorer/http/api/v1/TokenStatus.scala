package org.ergoplatform.explorer.http.api.v1

import cats.syntax.either._
import enumeratum.values._
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.syntax._
import sttp.tapir.Schema

import scala.collection.immutable

sealed abstract class TokenStatus(val value: Int) extends IntEnumEntry

object TokenStatus {

  object TokenStatus extends IntEnum[TokenStatus] {
    val values: immutable.IndexedSeq[TokenStatus] = findValues

    case object Unknown extends TokenStatus(0)
    case object Verified extends TokenStatus(1)
    case object Suspicious extends TokenStatus(2)
    case object Blocked extends TokenStatus(3)
    case object Null extends TokenStatus(-1)
  }

  implicit val tokenOrdering: Ordering[TokenStatus] =
    Ordering.fromLessThan((a, b) => a.value < b.value)

  implicit def encoder: Encoder[TokenStatus] = render(_).asJson

  implicit def decoder: Decoder[TokenStatus] =
    c =>
      c.as[Int].flatMap { i =>
        TokenStatus
          .withValueOpt(i)
          .fold(DecodingFailure(s"Unknown token status: [$i]", c.history).asLeft[TokenStatus])(_.asRight)
      }

  def parse(i: Int): TokenStatus = TokenStatus.withValue(i)

  implicit val schema: Schema[TokenStatus] = Schema
    .derived[TokenStatus]
    .modify(_.value)(_.description("Flag with 0 unknown, \n 1 verified, \n 2 suspicious, \n 3 blocked (see EIP-21)"))

  private def render(t: TokenStatus): Int =
    t match {
      case TokenStatus.Unknown    => 0
      case TokenStatus.Verified   => 1
      case TokenStatus.Suspicious => 2
      case TokenStatus.Blocked    => 3
    }

}
