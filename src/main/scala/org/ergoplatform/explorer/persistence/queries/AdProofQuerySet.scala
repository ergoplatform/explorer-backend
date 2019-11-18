package org.ergoplatform.explorer.persistence.queries

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.refined.implicits._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.persistence.models.AdProof

/** A set of queries required to implement functionality of production [AdProofRepo].
  */
object AdProofQuerySet extends QuerySet {

  val tableName: String = "node_ad_proofs"

  val fields: List[String] = List(
    "header_id",
    "proof_bytes",
    "digest"
  )

  def getByHeaderId(headerId: Id): ConnectionIO[Option[AdProof]] =
    sql"select header_id, proof_bytes, digest from node_ad_proofs where header_id = $headerId"
      .query[AdProof]
      .option
}
