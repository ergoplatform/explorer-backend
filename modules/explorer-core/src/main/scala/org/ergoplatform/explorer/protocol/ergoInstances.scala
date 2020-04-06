package org.ergoplatform.explorer.protocol

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.wallet.serialization.JsonCodecsWrapper.{ergoLikeTransactionDecoder, ergoLikeTransactionEncoder}
import sttp.tapir.{Schema, SchemaType}

object ergoInstances {

  implicit val eltCodec: io.circe.Codec[ErgoLikeTransaction] =
    io.circe.Codec.from(ergoLikeTransactionDecoder, ergoLikeTransactionEncoder)

  implicit val schemaBlockInfo: Schema[ErgoLikeTransaction] =
    Schema(SchemaType.SProduct(SchemaType.SObjectInfo("ErgoLikeTransaction"), Iterable.empty)) // todo: derive schema for the whole ErgoLikeTransaction.
}
