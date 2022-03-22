package org.ergoplatform.explorer.db.queries

import doobie.LogHandler
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import doobie.util.query.Query0
import doobie.util.update.Update0
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.BlockStats
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedBlockInfo, MinerStats, TimePoint}

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
    "total_coins_in_txs",
    "max_tx_gix",
    "max_box_gix",
    "main_chain"
  )

  def getBlockInfo(headerId: BlockId)(implicit lh: LogHandler): Query0[BlockStats] =
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
         |  bi.max_tx_gix,
         |  bi.max_box_gix,
         |  bi.main_chain
         |from blocks_info bi where bi.header_id = $headerId
         |""".stripMargin.query[BlockStats]

  def getManyExtendedMain(
    offset: Long,
    limit: Int,
    ordering: OrderingString,
    orderBy: String
  )(implicit lh: LogHandler): Query0[ExtendedBlockInfo] = {
    val ord =
      Fragment.const(s"order by bi.$orderBy $ordering")
    val lim =
      Fragment.const(s"offset $offset limit $limit")
    val q =
      sql"""
         |select
         |  nh.version,
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
         |  bi.max_tx_gix,
         |  bi.max_box_gix,
         |  bi.main_chain,
         |  mi.miner_name
         |from blocks_info bi
         |left join known_miners mi on bi.miner_address = mi.miner_address
         |left join node_headers nh on bi.header_id = nh.id
         |where bi.main_chain = true
         |""".stripMargin
    (q ++ ord ++ lim).query[ExtendedBlockInfo]
  }

  def getManySince(ts: Long)(implicit lh: LogHandler): Query0[BlockStats] =
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
         |  bi.max_tx_gix,
         |  bi.max_box_gix,
         |  bi.main_chain
         |from blocks_info bi where bi.timestamp >= $ts
         |""".stripMargin.query[BlockStats]

  def getLastStats(implicit lh: LogHandler): Query0[BlockStats] =
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
         |  bi.max_tx_gix,
         |  bi.max_box_gix,
         |  bi.main_chain
         |from blocks_info bi where bi.main_chain = true
         |order by bi.height desc limit 1
         |""".stripMargin.query[BlockStats]

  def getManyExtendedByIdLike(q: String)(implicit lh: LogHandler): Query0[ExtendedBlockInfo] =
    sql"""
         |select
         |  nh.version,
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
         |  bi.max_tx_gix,
         |  bi.max_box_gix,
         |  bi.main_chain,
         |  mi.miner_name
         |from blocks_info bi
         |left join known_miners mi on bi.miner_address = mi.miner_address
         |left join node_headers nh on bi.header_id = nh.id
         |where bi.header_id like ${s"%$q%"}
         |""".stripMargin.query[ExtendedBlockInfo]

  def getBlockSize(id: BlockId)(implicit lh: LogHandler): Query0[Int] =
    sql"select block_size from blocks_info where header_id = $id".query[Int]

  def totalDifficultySince(ts: Long)(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(difficulty) as bigint), 0) from blocks_info
         |where timestamp >= $ts
         |""".stripMargin.query[Long]

  def circulatingSupplySince(ts: Long)(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select coalesce(cast(sum(o.value) as bigint), 0) from node_transactions t
         |right join node_outputs o on t.id = o.tx_id
         |where t.timestamp >= $ts
         |""".stripMargin.query[Long]

  def totalCoinsSince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(max(total_coins_issued) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def avgBlockSizeSince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(avg(block_size) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def avgTxsQtySince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(avg(txs_count) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def totalTxsQtySince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(sum(txs_count) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def totalBlockChainSizeSince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(max(block_chain_total_size) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def avgDifficultiesSince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(avg(difficulty) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def totalDifficultiesSince(ts: Long): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(sum(difficulty) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def totalMinerRevenueSince(ts: Long)(implicit lh: LogHandler): Query0[TimePoint[Long]] =
    sql"""
         |select
         |  min(timestamp) as t,
         |  cast(sum(miner_revenue) as bigint),
         |  to_char(to_timestamp(timestamp / 1000), 'DD/MM/YYYY') as date
         |from blocks_info
         |where (timestamp >= $ts and exists(select 1 from node_headers h where h.main_chain = true))
         |group by date order by t asc
         |""".stripMargin.query[TimePoint[Long]]

  def minerStatsSince(ts: Long)(implicit lh: LogHandler): Query0[MinerStats] =
    sql"""
         |select
         |  bi.miner_address,
         |  coalesce(cast(sum(bi.difficulty) as bigint), 0),
         |  coalesce(cast(sum(bi.block_mining_time) as bigint), 0),
         |  count(*) as count,
         |  m.miner_name
         |from blocks_info bi left join known_miners m on (bi.miner_address = m.miner_address)
         |where timestamp >= $ts
         |group by bi.miner_address, m.miner_name
         |order by count desc
         |""".stripMargin.query[MinerStats]

  def updateChainStatusByHeaderId(headerId: BlockId, newChainStatus: Boolean)(implicit lh: LogHandler): Update0 =
    sql"""
         |update blocks_info set main_chain = $newChainStatus
         |where header_id = $headerId
         |""".stripMargin.update

  def updateTotalParametersByHeaderId(
    headerId: BlockId,
    newSize: Long,
    newTxsCount: Long,
    newMiningTime: Long,
    newFees: Long,
    newReward: Long,
    newCoins: Long
  )(implicit lh: LogHandler): Update0 =
    sql"""
         |update blocks_info set block_chain_total_size = $newSize, total_txs_count = $newTxsCount, total_mining_time = $newMiningTime, total_fees = $newFees, total_miners_reward = $newReward, total_coins_in_txs = $newCoins
         |where header_id = $headerId
         |""".stripMargin.update
}
