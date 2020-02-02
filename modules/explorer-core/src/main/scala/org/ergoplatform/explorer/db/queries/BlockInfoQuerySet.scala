package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.models.BlockInfo

object BlockInfoQuerySet extends QuerySet {

  val tableName: String = "blocks_info"

  val fields: List[String] = List(
    "header_id",
    "timestamp",
    "height",
    "difficulty",
    "block_size",
    "block_coins",
    "block_mining_time",
    "txs_count",
    "txs_size",
    "miner_address",
    "miner_reward",
    "miner_revenue",
    "block_fee",
    "block_chain_total_size",
    "total_txs_count",
    "total_coins_issued",
    "total_mining_time",
    "total_fees",
    "total_miners_reward",
    "total_coins_in_txs"
  )

  def getBlockInfo(headerId: Id): Query0[BlockInfo] =
    sql"select * from blocks_info where header_id = $headerId"
      .query[BlockInfo]

  def getMany(offset: Int, limit: Int): Query0[BlockInfo] =
    sql"select * from blocks_info offset $offset limit $limit"
      .query[BlockInfo]

  def getBlockSize(id: Id): Query0[Int] =
    sql"select block_size from blocks_info where id = $id"
      .query[Int]
}
