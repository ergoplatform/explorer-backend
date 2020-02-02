package org.ergoplatform.explorer.db.models

import io.getquill.{idiom => _, _}

object schema {

  import doobie.quill.DoobieContext

  val ctx = new DoobieContext.Postgres(Literal) // Literal naming scheme
  import ctx._

  val Assets = quote {
    querySchema[Asset](
      "node_assets",
      _.tokenId  -> "token_id",
      _.boxId    -> "box_id",
      _.headerId -> "header_id",
      _.amount   -> "value"
    )
  }

  val Outputs = quote {
    querySchema[Output](
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

}
