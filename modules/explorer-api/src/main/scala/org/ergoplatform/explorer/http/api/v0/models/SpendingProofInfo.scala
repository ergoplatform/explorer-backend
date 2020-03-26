package org.ergoplatform.explorer.http.api.v0.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer.HexString
import sttp.tapir.generic.Derived
import sttp.tapir.{Schema, SchemaType}

// in particular format of `extension` (what is inside it)
final case class SpendingProofInfo(proofBytes: Option[HexString], extension: Json)

object SpendingProofInfo {

  implicit val codec: Codec[SpendingProofInfo] = deriveCodec

  implicit val schema: Schema[SpendingProofInfo] =
    implicitly[Derived[Schema[SpendingProofInfo]]].value
      .modify(_.proofBytes)(_.description("Hex-encoded serialized sigma proof"))
      .modify(_.extension)(_.description("Proof extension (key->value dictionary)"))

  implicit private def extensionSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        SchemaType.SObjectInfo("ProofExtension"),
        Schema(SchemaType.SString)
      )
    )
}
