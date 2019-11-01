package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.{HexString, Id}

final case class AdProof(
  headerId: Id,
  proofBytes: HexString,
  digest: HexString
)
