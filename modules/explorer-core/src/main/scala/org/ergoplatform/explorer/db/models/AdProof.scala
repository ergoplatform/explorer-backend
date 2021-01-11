package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.{HexString, Id}

/** Represents `node_ad_proofs` table.
  */
final case class AdProof(
  headerId: Id,
  proofBytes: HexString, // serialized and hex-encoded AVL+ tree path
  digest: HexString      // hex-encoded tree root hash
)
