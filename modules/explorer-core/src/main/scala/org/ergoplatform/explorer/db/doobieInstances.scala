package org.ergoplatform.explorer.db

import doobie.Meta
import doobie.util.{Read, Write}
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

object doobieInstances {

  implicit val bigIntRead: Read[BigInt] =
    Read[BigDecimal].map(_.toBigInt())

  implicit val bigIntWrite: Write[BigInt] =
    Write[BigDecimal].contramap(x => BigDecimal(x.bigInteger))

  implicit val JsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .imap[Json](a => parse(a.getValue).right.getOrElse(Json.Null))(mkPgJson)

  private def mkPgJson(a: Json) = {
    val o = new PGobject
    o.setType("json")
    o.setValue(a.noSpaces)
    o
  }
}
