package org.ergoplatform.explorer.http.api

import sttp.tapir.Schema

object tapirInstances {

  implicit val bigIntSchema: Schema[BigInt] =
    Schema.schemaForBigDecimal.map(x => Some(x.toBigInt()))(x => BigDecimal(x.bigInteger))
}
