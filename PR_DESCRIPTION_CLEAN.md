## Summary

Fixes Issue #259: Blockchain reorganization bug where `global_index` becomes inconsistent with chronological ordering after reorgs.

⚠️ **DRAFT PR** - Implementation complete, tests pending. Seeking feedback on approach before adding test suite.

**Problem:** Only `main_chain` flag updates, `global_index` stays wrong → wrong transaction order.

**Solution:** Recalculate `global_index` using window function with explicit locking.

---

## Implementation

### 1. TransactionQuerySet.scala

Added `recalculateGlobalIndexFromHeight()` method:

```scala
def recalculateGlobalIndexFromHeight(height: Int)(implicit lh: LogHandler): Update0 =
  sql"""
       |WITH base_index AS (
       |  SELECT COALESCE(MAX(global_index), -1) AS last_index
       |  FROM node_transactions
       |  WHERE inclusion_height < $height AND main_chain = true
       |),
       |ordered_txs AS (
       |  SELECT 
       |    t.id, t.header_id,
       |    (SELECT last_index FROM base_index) + 
       |      ROW_NUMBER() OVER (
       |        ORDER BY t.inclusion_height ASC, 
       |                 t.timestamp ASC, 
       |                 t.index ASC
       |      ) AS new_global_index
       |  FROM node_transactions t
       |  WHERE t.inclusion_height >= $height AND t.main_chain = true
       |  FOR UPDATE
       |)
       |UPDATE node_transactions t
       |SET global_index = o.new_global_index
       |FROM ordered_txs o
       |WHERE t.id = o.id AND t.header_id = o.header_id
       |""".stripMargin.update
```

### 2. TransactionRepo.scala

Added method to trait and implementation:

```scala
// Trait
def recalculateGlobalIndexFromHeight(height: Int): D[Unit]

// Implementation
def recalculateGlobalIndexFromHeight(height: Int): D[Unit] =
  QS.recalculateGlobalIndexFromHeight(height).run.void.liftConnectionIO
```

### 3. ChainIndexer.scala

Modified `updateChainStatus()` to trigger recalculation:

```scala
private def updateChainStatus(blockId: BlockId, mainChain: Boolean): D[Unit] =
  for {
    _ <- repos.headers.updateChainStatusById(blockId, mainChain)
    _ <- if (settings.indexes.blockStats) 
           repos.blocksInfo.updateChainStatusByHeaderId(blockId, mainChain)
         else unit[D]
    _ <- repos.txs.updateChainStatusByHeaderId(blockId, mainChain)
    _ <- repos.outputs.updateChainStatusByHeaderId(blockId, mainChain)
    _ <- repos.inputs.updateChainStatusByHeaderId(blockId, mainChain)
    _ <- repos.dataInputs.updateChainStatusByHeaderId(blockId, mainChain)
    
    headerOpt <- repos.headers.get(blockId)
    _ <- headerOpt match {
      case Some(header) if mainChain =>
        repos.txs.recalculateGlobalIndexFromHeight(header.height)
      case _ =>
        unit[D]
    }
  } yield ()
```

---

## Why Different from PR #266?

PR #266 provided test infrastructure only.

This implementation:
- Uses window function instead of recursive CTE
- Explicit `FOR UPDATE` locking
- Simpler and easier to maintain

---
