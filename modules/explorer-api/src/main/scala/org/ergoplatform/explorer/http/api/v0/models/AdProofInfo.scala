package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.ergoplatform.explorer.db.models.AdProof
import org.ergoplatform.explorer.{HexString, Id}
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class AdProofInfo(
  headerId: Id,
  proofBytes: HexString,
  digest: HexString
)

object AdProofInfo {

  implicit val codec: Codec[AdProofInfo] = deriveCodec

  implicit val schema: Schema[AdProofInfo] =
    implicitly[Derived[Schema[AdProofInfo]]].value
      .modify(_.headerId)(_.description("Id of the corresponding header"))
      .modify(_.proofBytes)(_.description("Hex-encoded serialized AD proof"))
      .modify(_.digest)(_.description("Hex-encoded AD proof digest"))

  def apply(adProof: AdProof): AdProofInfo =
    AdProofInfo(adProof.headerId, adProof.proofBytes, adProof.digest)
}
