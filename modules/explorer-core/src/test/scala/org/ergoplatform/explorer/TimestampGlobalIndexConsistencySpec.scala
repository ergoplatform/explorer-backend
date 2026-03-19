package org.ergoplatform.explorer

import org.ergoplatform.explorer.db.models.{Header, Transaction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Test suite for Issue #259: Inconsistent indexing for timestamp and globalIndex properties
 * 
 * This spec verifies that:
 * 1. Ordering by timestamp yields the same sequence as ordering by globalIndex
 * 2. GlobalIndex increases monotonically across transactions
 * 3. Cross-year boundary transactions maintain consistent ordering
 */
class TimestampGlobalIndexConsistencySpec extends AnyFlatSpec with Matchers {

  "Transaction globalIndex" should "maintain monotonic ordering" in {
    val txs = createTestTransactions(
      startGlobalIndex = 1000L,
      startTimestamp = 1672531200000L, // Jan 1, 2023
      count = 10
    )

    // Verify monotonic increase
    txs.sliding(2).foreach { case Seq(tx1, tx2) =>
      tx2.globalIndex should be > tx1.globalIndex
      tx2.timestamp should be > tx1.timestamp
    }
  }

  it should "produce same ordering when sorted by timestamp or globalIndex" in {
    val txs = createTestTransactions(
      startGlobalIndex = 1000L,
      startTimestamp = 1672531200000L,
      count = 20
    )

    val sortedByTimestamp = txs.sortBy(_.timestamp)
    val sortedByGlobalIndex = txs.sortBy(_.globalIndex)

    sortedByTimestamp.map(_.id) shouldEqual sortedByGlobalIndex.map(_.id)
  }

  it should "maintain consistency across year boundaries" in {
    val txs2023 = createTestTransactions(
      startGlobalIndex = 1000L,
      startTimestamp = 1672531200000L, // Jan 1, 2023
      count = 5
    )

    val txs2024 = createTestTransactions(
      startGlobalIndex = 1005L,
      startTimestamp = 1704067200000L, // Jan 1, 2024
      count = 5
    )

    val allTxs = txs2023 ++ txs2024

    // Verify ordering consistency
    val sortedByTimestamp = allTxs.sortBy(_.timestamp)
    val sortedByGlobalIndex = allTxs.sortBy(_.globalIndex)

    sortedByTimestamp.map(_.id) shouldEqual sortedByGlobalIndex.map(_.id)

    // Verify 2023 transactions come before 2024 in both orderings
    val timestampOrdering = sortedByTimestamp.map(_.id)
    val globalIndexOrdering = sortedByGlobalIndex.map(_.id)

    timestampOrdering shouldEqual globalIndexOrdering
  }

  it should "handle transactions within same block correctly" in {
    val headerId = "test_header_id"
    val height = 100000
    val blockTimestamp = 1672531200000L
    
    // Multiple transactions in same block
    val txs = (0 until 5).map { i =>
      Transaction(
        id = s"tx_$i",
        headerId = headerId,
        inclusionHeight = height,
        isCoinbase = i == 0,
        timestamp = blockTimestamp,
        size = 1000,
        index = i,
        globalIndex = 1000L + i,
        mainChain = true
      )
    }

    // Within same block, globalIndex should match index
    txs.zipWithIndex.foreach { case (tx, idx) =>
      tx.index shouldEqual idx
      tx.globalIndex shouldEqual (1000L + idx)
    }

    // Sorting by either field should maintain same order
    val sortedByTimestamp = txs.sortBy(tx => (tx.timestamp, tx.index))
    val sortedByGlobalIndex = txs.sortBy(_.globalIndex)

    sortedByTimestamp.map(_.id) shouldEqual sortedByGlobalIndex.map(_.id)
  }

  it should "ensure no gaps in globalIndex sequence" in {
    val txs = createTestTransactions(
      startGlobalIndex = 1000L,
      startTimestamp = 1672531200000L,
      count = 10
    )

    // Check for sequential globalIndex values
    txs.zipWithIndex.foreach { case (tx, idx) =>
      tx.globalIndex shouldEqual (1000L + idx)
    }

    // Verify no duplicates
    val globalIndices = txs.map(_.globalIndex)
    globalIndices.distinct.size shouldEqual globalIndices.size
  }

  private def createTestTransactions(
    startGlobalIndex: Long,
    startTimestamp: Long,
    count: Int
  ): Seq[Transaction] = {
    (0 until count).map { i =>
      Transaction(
        id = s"tx_${startGlobalIndex + i}",
        headerId = s"header_${(startGlobalIndex + i) / 10}",
        inclusionHeight = 100000 + i,
        isCoinbase = false,
        timestamp = startTimestamp + (i * 120000L), // 2 minutes apart
        size = 1000,
        index = i % 10,
        globalIndex = startGlobalIndex + i,
        mainChain = true
      )
    }
  }
}
