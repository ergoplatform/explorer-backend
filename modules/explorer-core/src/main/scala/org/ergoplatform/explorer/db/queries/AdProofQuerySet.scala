package org.ergoplatform.explorer.db.queries

import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.models.AdProof

/** A set of queries for doobie implementation of [AdProofRepo].
  */
object AdProofQuerySet extends QuerySet {

  val tableName: String = "node_ad_proofs"

  val fields: List[String] = List(
    "header_id",
    "proof_bytes",
    "digest"
  )

  def getByHeaderId(headerId: BlockId)(implicit lh: LogHandler): Query0[AdProof] =
    sql"select header_id, proof_bytes, digest from node_ad_proofs where header_id = $headerId"
      .query[AdProof]
}
