package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.query.Query0
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.db.models.BlockInfo
import org.ergoplatform.explorer.db.models.aggregates.{ChartPoint, ExtendedBlockInfo}

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

  def getManyExtended(offset: Int, limit: Int): Query0[ExtendedBlockInfo] =
    sql"""
         |select
         |  bi.header_id,
         |  bi.timestamp,
         |  bi.height,
         |  bi.difficulty,
         |  bi.block_size,
         |  bi.block_coins,
         |  bi.block_mining_time,
         |  bi.txs_count,
         |  bi.txs_size,
         |  bi.miner_address,
         |  bi.miner_reward,
         |  bi.miner_revenue,
         |  bi.block_fee,
         |  bi.block_chain_total_size,
         |  bi.total_txs_count,
         |  bi.total_coins_issued,
         |  bi.total_mining_time,
         |  bi.total_fees,
         |  bi.total_miners_reward,
         |  bi.total_coins_in_txs,
         |  mi.miner_name
         |from blocks_info bi
         |left join known_miners mi on bi.miner_address = mi.miner_address
         |offset $offset limit $limit
         |"""
      .query[ExtendedBlockInfo]

  def getManySince(ts: Long): Query0[BlockInfo] =
    sql"select * from blocks_info where timestamp >= $ts"
      .query[BlockInfo]

  def getBlockSize(id: Id): Query0[Int] =
    sql"select block_size from blocks_info where id = $id"
      .query[Int]

  def totalDifficultySince(ts: Long): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(difficulty) as bigint), 0) from blocks_info
         |where timestamp >= $ts
         |""".stripMargin.query[Long]

  def circulatingSupplySince(ts: Long): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_transactions t
         |right join node_outputs o on t.id = o.tx_id
         |where t.timestamp >= $ts
         |""".stripMargin.query[Long]

  def totalCoinsSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(max(total_coins_issued) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def avgBlockSizeSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(avg(block_size) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def avgTxsQtySince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(avg(txs_count) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def totalTxsQtySince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(sum(txs_count) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def totalBlockChainSizeSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(max(block_chain_total_size) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def avgDifficultiesSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(avg(difficulty) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def totalDifficultiesSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(sum(difficulty) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]

  def totalMinerRevenueSince(ts: Long): Query0[ChartPoint] =
    sql"""
         |select min(timestamp) as t, cast(sum(miner_revenue) as bigint) from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[ChartPoint]
}
