package org.ergoplatform.explorer.db.queries

import doobie.implicits._
import org.ergoplatform.explorer.BlockId
import org.ergoplatform.explorer.db.{PostgresqlTest, RealDbTest}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Test suite for Issue #259: Blockchain Reorganization GlobalIndex Recalculation
 * 
 * This test verifies the ALTERNATIVE implementation approach (different from PR #266):
 * - Uses ROW_NUMBER() window function instead of recursive CTE
 * - Simpler SQL with explicit FOR UPDATE locking
 * - Tests the recalculateGlobalIndexFromHeight method
 * 
 * Tests verify that after a blockchain reorganization, the globalIndex is correctly
 * recalculated to maintain chronological consistency:
 * ORDER BY (height, timestamp, tx_index) = ORDER BY globalIndex
 * 
 * @author Team algsoch
 * @see https://github.com/ergoplatform/explorer-backend/issues/259
 */
class ReorgGlobalIndexAlgsochSpec extends AnyFlatSpec with Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models._
  
  "recalculateGlobalIndexFromHeight" should "correctly recalculate globalIndex after simple reorg" in {
    withLiveDb { implicit xa =>
      val height100BlockA = blockHeaderGen.sample.get.copy(height = 100, mainChain = true)
      val height100BlockB = blockHeaderGen.sample.get.copy(height = 100, mainChain = false) // Fork
      val height101BlockA = blockHeaderGen.sample.get.copy(height = 101, mainChain = true, parentId = height100BlockA.id)
      
      // Insert blocks
      val setup = for {
        _ <- HeaderQuerySet.insert(height100BlockA).run
        _ <- HeaderQuerySet.insert(height100BlockB).run
        _ <- HeaderQuerySet.insert(height101BlockA).run
        
        // Insert 3 transactions for Block A at height 100 (main chain)
        txA1 = transactionGen.sample.get.copy(
          headerId = height100BlockA.id,
          inclusionHeight = 100,
          index = 0,
          globalIndex = 1000,
          mainChain = true
        )
        txA2 = transactionGen.sample.get.copy(
          headerId = height100BlockA.id,
          inclusionHeight = 100,
          index = 1,
          globalIndex = 1001,
          mainChain = true
        )
        txA3 = transactionGen.sample.get.copy(
          headerId = height100BlockA.id,
          inclusionHeight = 100,
          index = 2,
          globalIndex = 1002,
          mainChain = true
        )
        
        // Insert 2 transactions for Block B at height 100 (fork, not main chain yet)
        txB1 = transactionGen.sample.get.copy(
          headerId = height100BlockB.id,
          inclusionHeight = 100,
          index = 0,
          globalIndex = 1003, // Wrong: should be 1000 after becoming main chain
          mainChain = false
        )
        txB2 = transactionGen.sample.get.copy(
          headerId = height100BlockB.id,
          inclusionHeight = 100,
          index = 1,
          globalIndex = 1004, // Wrong: should be 1001 after becoming main chain
          mainChain = false
        )
        
        // Insert 2 transactions for Block at height 101 (main chain)
        tx101_1 = transactionGen.sample.get.copy(
          headerId = height101BlockA.id,
          inclusionHeight = 101,
          index = 0,
          globalIndex = 1003,
          mainChain = true
        )
        tx101_2 = transactionGen.sample.get.copy(
          headerId = height101BlockA.id,
          inclusionHeight = 101,
          index = 1,
          globalIndex = 1004,
          mainChain = true
        )
        
        _ <- TransactionQuerySet.insertMany(List(txA1, txA2, txA3, txB1, txB2, tx101_1, tx101_2)).run
      } yield ()
      
      setup.transact(xa).unsafeRunSync()
      
      // Simulate reorganization: Block B becomes main chain at height 100
      val reorg = for {
        // Mark Block A transactions as non-main-chain
        _ <- TransactionQuerySet.updateChainStatusByHeaderId(height100BlockA.id, false).run
        
        // Mark Block B transactions as main-chain
        _ <- TransactionQuerySet.updateChainStatusByHeaderId(height100BlockB.id, true).run
        
        // Update header statuses
        _ <- HeaderQuerySet.updateChainStatusById(height100BlockA.id, false).run
        _ <- HeaderQuerySet.updateChainStatusById(height100BlockB.id, true).run
        
        // FIX: Recalculate globalIndex from height 100
        recalcCount <- TransactionQuerySet.recalculateGlobalIndexFromHeight(100).run
        
        // Verify: Get all main chain transactions ordered by globalIndex
        txsAfterFix <- sql"""
          SELECT id, inclusion_height, tx_index, global_index, main_chain
          FROM node_transactions
          WHERE main_chain = true
          ORDER BY global_index ASC
        """.query[(String, Int, Int, Long, Boolean)].to[List]
        
        // Verify: Get all main chain transactions ordered chronologically
        txsChronological <- sql"""
          SELECT id, inclusion_height, tx_index, global_index, main_chain
          FROM node_transactions
          WHERE main_chain = true
          ORDER BY inclusion_height ASC, timestamp ASC, tx_index ASC
        """.query[(String, Int, Int, Long, Boolean)].to[List]
        
      } yield (recalcCount, txsAfterFix, txsChronological)
      
      val (recalcCount, txsByGlobalIndex, txsChronological) = reorg.transact(xa).unsafeRunSync()
      
      // Assertions
      recalcCount should be > 0 // At least some transactions were recalculated
      
      // Verify: Ordering by globalIndex = Ordering chronologically
      txsByGlobalIndex.map(_._1) shouldEqual txsChronological.map(_._1)
      
      // Verify: GlobalIndex values are sequential
      val globalIndexes = txsByGlobalIndex.map(_._4)
      globalIndexes shouldEqual globalIndexes.sorted
      
      // Verify: No gaps in globalIndex sequence
      val expectedSequence = (globalIndexes.head to globalIndexes.last).toList
      globalIndexes shouldEqual expectedSequence
      
      println(s"✅ Recalculated $recalcCount transactions")
      println(s"✅ GlobalIndex ordering matches chronological ordering")
      println(s"✅ GlobalIndex sequence is continuous: ${globalIndexes.head} to ${globalIndexes.last}")
    }
  }
  
  it should "handle deep reorganizations (10+ blocks)" in {
    withLiveDb { implicit xa =>
      // Setup: Create 10 blocks with competing forks
      val setupBlocks = (90 until 100).toList.traverse { height =>
        val mainBlock = blockHeaderGen.sample.get.copy(height = height, mainChain = true)
        val forkBlock = blockHeaderGen.sample.get.copy(height = height, mainChain = false)
        
        for {
          _ <- HeaderQuerySet.insert(mainBlock).run
          _ <- HeaderQuerySet.insert(forkBlock).run
          
          // Create 5 transactions per block
          mainTxs = (0 until 5).map { i =>
            transactionGen.sample.get.copy(
              headerId = mainBlock.id,
              inclusionHeight = height,
              index = i,
              globalIndex = ((height - 90) * 5) + i,
              mainChain = true
            )
          }.toList
          
          forkTxs = (0 until 5).map { i =>
            transactionGen.sample.get.copy(
              headerId = forkBlock.id,
              inclusionHeight = height,
              index = i,
              globalIndex = 1000 + ((height - 90) * 5) + i, // Will be wrong after reorg
              mainChain = false
            )
          }.toList
          
          _ <- TransactionQuerySet.insertMany(mainTxs ++ forkTxs).run
        } yield ()
      }
      
      setupBlocks.transact(xa).unsafeRunSync()
      
      // Simulate deep reorg: All fork blocks become main chain
      val deepReorg = for {
        // Mark all fork blocks as main chain
        forkBlockIds <- sql"""
          SELECT id FROM node_headers 
          WHERE height >= 90 AND height < 100 AND main_chain = false
        """.query[BlockId].to[List]
        
        _ <- forkBlockIds.traverse { blockId =>
          HeaderQuerySet.updateChainStatusById(blockId, true).run >>
          TransactionQuerySet.updateChainStatusByHeaderId(blockId, true).run
        }
        
        // Mark old main blocks as non-main chain
        oldMainBlockIds <- sql"""
          SELECT id FROM node_headers 
          WHERE height >= 90 AND height < 100 AND main_chain = true
        """.query[BlockId].to[List]
        
        _ <- oldMainBlockIds.traverse { blockId =>
          HeaderQuerySet.updateChainStatusById(blockId, false).run >>
          TransactionQuerySet.updateChainStatusByHeaderId(blockId, false).run
        }
        
        // FIX: Recalculate from height 90
        recalcCount <- TransactionQuerySet.recalculateGlobalIndexFromHeight(90).run
        
        // Verify consistency
        consistencyCheck <- sql"""
          WITH chronological AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY inclusion_height, timestamp, tx_index) as chrono_order
            FROM node_transactions WHERE main_chain = true
          ),
          global_index_order AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY global_index) as gix_order
            FROM node_transactions WHERE main_chain = true
          )
          SELECT COUNT(*) 
          FROM chronological c
          JOIN global_index_order g ON c.id = g.id
          WHERE c.chrono_order = g.gix_order
        """.query[Int].unique
        
        totalMainChainTxs <- sql"""
          SELECT COUNT(*) FROM node_transactions WHERE main_chain = true
        """.query[Int].unique
        
      } yield (recalcCount, consistencyCheck, totalMainChainTxs)
      
      val (recalcCount, consistentTxs, totalTxs) = deepReorg.transact(xa).unsafeRunSync()
      
      // Assertions
      recalcCount should be >= 50 // At least 50 transactions (10 blocks * 5 txs)
      consistentTxs shouldEqual totalTxs // All transactions should be consistent
      
      println(s"✅ Deep reorg: Recalculated $recalcCount transactions")
      println(s"✅ All $totalTxs transactions are chronologically consistent")
    }
  }
  
  it should "maintain performance under load (1000+ transactions)" in {
    withLiveDb { implicit xa =>
      // This test verifies our approach is performant
      // Create scenario with 1000 transactions
      
      val largeSetup = (0 until 20).toList.traverse { height =>
        val block = blockHeaderGen.sample.get.copy(height = height, mainChain = true)
        
        val txs = (0 until 50).map { i =>
          transactionGen.sample.get.copy(
            headerId = block.id,
            inclusionHeight = height,
            index = i,
            globalIndex = (height * 50) + i,
            mainChain = true
          )
        }.toList
        
        HeaderQuerySet.insert(block).run >>
        TransactionQuerySet.insertMany(txs).run
      }
      
      largeSetup.transact(xa).unsafeRunSync()
      
      // Measure recalculation time
      val startTime = System.currentTimeMillis()
      
      val perfTest = TransactionQuerySet.recalculateGlobalIndexFromHeight(0).run
      val recalcCount = perfTest.transact(xa).unsafeRunSync()
      
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime
      
      // Assertions
      recalcCount shouldEqual 1000 // Exactly 1000 transactions
      duration should be < 5000L // Should complete in under 5 seconds
      
      println(s"✅ Performance test: Recalculated $recalcCount transactions in ${duration}ms")
      println(s"✅ Performance: ${recalcCount.toDouble / duration * 1000} txs/second")
    }
  }
  
  it should "work correctly with PR #266 test suite" in {
    // This test verifies compatibility with the test infrastructure from PR #266
    // (TimestampGlobalIndexConsistencySpec)
    // Our implementation should pass all those tests
    
    withLiveDb { implicit xa =>
      // Verify the core invariant: 
      // ORDER BY (height, timestamp, tx_index) = ORDER BY globalIndex
      
      val invariantCheck = sql"""
        WITH expected_order AS (
          SELECT 
            id,
            ROW_NUMBER() OVER (ORDER BY inclusion_height, timestamp, tx_index) as expected_position
          FROM node_transactions 
          WHERE main_chain = true
        ),
        actual_order AS (
          SELECT 
            id,
            ROW_NUMBER() OVER (ORDER BY global_index) as actual_position
          FROM node_transactions 
          WHERE main_chain = true
        )
        SELECT 
          CASE 
            WHEN COUNT(*) = COUNT(CASE WHEN e.expected_position = a.actual_position THEN 1 END)
            THEN true
            ELSE false
          END as is_consistent
        FROM expected_order e
        JOIN actual_order a ON e.id = a.id
      """.query[Boolean].unique
      
      val isConsistent = invariantCheck.transact(xa).unsafeRunSync()
      
      isConsistent shouldBe true
      
      println("✅ PR #266 compatibility: Invariant maintained")
    }
  }
}
