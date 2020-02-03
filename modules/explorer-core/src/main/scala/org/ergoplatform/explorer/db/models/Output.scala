package org.ergoplatform.explorer.db.models

import io.circe.Json
import io.getquill.Embedded
import org.ergoplatform.explorer.{Address, BoxId, HexString, TxId}

/** Entity representing `node_outputs` table.
  */
final case class Output(
  boxId: BoxId,
  txId: TxId,
  value: Long, // amount of nanoERG in thee corresponding box
  creationHeight: Int, // the height this output was created
  index: Int, // index of the output in the transaction
  ergoTree: HexString, // serialized and hex-encoded ErgoTree
  addressOpt: Option[Address], // an address derived from ergoTree (if possible)
  additionalRegisters: Json, // arbitrary key-value dictionary
  timestamp: Long, // approx time output appeared in the blockchain
  mainChain: Boolean // chain status, `true` if this output resides in main chain
) extends Embedded

object Output {

  import schema.ctx._

  val quillSchemaMeta = schemaMeta[Output](
    "node_outputs",
    _.boxId               -> "box_id",
    _.txId                -> "tx_id",
    _.value               -> "value",
    _.creationHeight      -> "creation_height",
    _.index               -> "index",
    _.ergoTree            -> "ergo_tree",
    _.addressOpt          -> "address",
    _.additionalRegisters -> "additional_registers",
    _.timestamp           -> "timestamp",
    _.mainChain           -> "main_chain"
  )
}
