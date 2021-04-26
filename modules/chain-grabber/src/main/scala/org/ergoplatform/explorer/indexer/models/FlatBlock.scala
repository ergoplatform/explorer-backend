package org.ergoplatform.explorer.indexer.models

import org.ergoplatform.explorer.db.models._

/** Flattened representation of a full block from
  * Ergo protocol enriched with statistics.
  */
final case class FlatBlock(
  header: Header,
  info: BlockStats,
  extension: BlockExtension,
  adProofOpt: Option[AdProof],
  txs: List[Transaction],
  inputs: List[Input],
  dataInputs: List[DataInput],
  outputs: List[Output],
  assets: List[Asset],
  registers: List[BoxRegister],
  tokens: List[Token],
  constants: List[ScriptConstant]
)
