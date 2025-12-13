## Summary

Implements Issue #210: FullBlock streaming API method for explorer-backend.

Adds a new streaming endpoint that returns complete block data including all transactions, inputs, outputs, and assets.

---

## Implementation

### New Endpoint

```
GET /api/v1/blocks/stream/full?minGlobalIndex={gix}&limit={limit}
```

**Parameters:**
- `minGlobalIndex`: Starting height (blocks with height >= this value)
- `limit`: Maximum number of blocks to return

**Returns:** Stream of `FullBlockInfo` objects containing:
- Block header
- All transactions
- All inputs
- All data inputs
- All outputs
- All assets
- Block extension
- AD proofs
- Block size

---

## Changes Made

### 1. HeaderQuerySet.scala
Added SQL query to fetch headers by global index:
```scala
def getHeadersAfterGix(minGix: Long, limit: Int): Query0[Header]
```

### 2. HeaderRepo.scala
Added repository method:
```scala
def streamHeadersAfterGix(minGix: Long, limit: Int): S[D, Header]
```

### 3. Blocks.scala (Service)
Added streaming service method:
```scala
def streamFullBlocks(minGix: Long, limit: Int): Stream[F, FullBlockInfo]
```

### 4. BlocksEndpointDefs.scala
Added endpoint definition:
```scala
def streamFullBlocksDef: Endpoint[(Long, Int), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]]
```

### 5. BlocksRoutes.scala
Added route handler:
```scala
private def streamFullBlocksR: HttpRoutes[F]
```

---

## Why This Approach?

1. **Consistent with existing patterns**: Follows the same structure as `streamBlocks` and `streamBlockSummaries`
2. **Efficient streaming**: Uses fs2 streams for memory-efficient data transfer
3. **Reuses existing logic**: Leverages `getFullBlockInfo` method already in the service
4. **Proper layer separation**: SQL → Repository → Service → Route

---

## Usage Example

```bash
curl "http://localhost:8080/api/v1/blocks/stream/full?minGlobalIndex=1000000&limit=100"
```

Returns a JSON stream of full block data starting from height 1,000,000, limited to 100 blocks.

---

**Fixes:** #210
