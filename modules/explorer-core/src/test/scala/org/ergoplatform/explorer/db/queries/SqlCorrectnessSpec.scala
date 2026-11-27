package org.ergoplatform.explorer.db.queries

import doobie.util.log.LogHandler
import eu.timepit.refined.refineMV
import org.ergoplatform.explorer.constraints.OrderingSpec
import org.ergoplatform.explorer.{Address, ErgoTreeTemplateHash}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class SqlCorrectnessSpec extends AnyFlatSpec with should.Matchers {

  implicit private val lh: LogHandler = LogHandler.nop

  private val templateHash = ErgoTreeTemplateHash.fromStringUnsafe("aa" * 32)
  private val genesisAddr  = Address.fromStringUnsafe("9f4QF8AD1nQ3nJahQVkMj8hFSVVzVom77b52JU7EW71Zexg6N8v")

  "OutputQuerySet template-hash epoch queries" should "match DISTINCT ON with initial ORDER BY and join node_headers" in {
    val allSql     = OutputQuerySet.getAllByErgoTreeTemplateHashByEpochs(templateHash, 0, 100).sql.toLowerCase
    val unspentSql = OutputQuerySet.getUnspentByErgoTreeTemplateHashByEpochs(templateHash, 0, 100).sql.toLowerCase

    allSql should include("select distinct on (h.height, o.box_id, o.creation_height)")
    allSql should include("o.main_chain = true")
    allSql should include("order by h.height asc")

    unspentSql should include("select distinct on (h.height, o.box_id, o.creation_height)")
    unspentSql should include("left join node_headers h on h.id = o.header_id")
    unspentSql should include("order by h.height asc")
  }

  "OutputQuerySet stats queries" should "use LEFT JOIN for estimatedOutputsSince and filter main_chain" in {
    val estSql = OutputQuerySet.estimatedOutputsSince(0L)(genesisAddr).sql.toLowerCase
    estSql should include("left join node_inputs i on o.box_id = i.box_id and i.main_chain = true")
    estSql should include("o.main_chain = true")
    estSql should include("o.address <> ?")

    val sumSql = OutputQuerySet.sumOfAllUnspentOutputsSince(0L).sql.toLowerCase
    sumSql should include("o.main_chain = true")

    val balSql = OutputQuerySet.balanceStatsMain(0, 10).sql.toLowerCase
    balSql should include("tx.main_chain = true")
  }

  "BlockInfoQuerySet chart and miner queries" should "correlate node_headers EXISTS to blocks_info.header_id and filter main_chain" in {
    val chartQueries = List(
      BlockInfoQuerySet.totalCoinsSince(0L).sql,
      BlockInfoQuerySet.avgBlockSizeSince(0L).sql,
      BlockInfoQuerySet.avgTxsQtySince(0L).sql,
      BlockInfoQuerySet.totalTxsQtySince(0L).sql,
      BlockInfoQuerySet.totalBlockChainSizeSince(0L).sql,
      BlockInfoQuerySet.avgDifficultiesSince(0L).sql,
      BlockInfoQuerySet.totalDifficultiesSince(0L).sql,
      BlockInfoQuerySet.totalMinerRevenueSince(0L).sql
    )
    chartQueries.foreach { q =>
      q.toLowerCase should include("h.id = blocks_info.header_id and h.main_chain = true")
    }

    BlockInfoQuerySet.minerStatsSince(0L).sql.toLowerCase should include("bi.main_chain = true")
    val circSql = BlockInfoQuerySet.circulatingSupplySince(0L).sql.toLowerCase
    circSql should include("t.header_id = o.header_id and t.main_chain = true")
    circSql should include("o.main_chain = true")
  }

  "HeaderQuerySet.getMany" should "filter main_chain = true" in {
    val hdrSql = HeaderQuerySet.getMany(0, 10, refineMV[OrderingSpec]("desc"), "height").sql.toLowerCase
    hdrSql should include("where main_chain = true")
  }
}
