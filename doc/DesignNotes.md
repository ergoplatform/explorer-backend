# Ergo Blockchain Explorer Backend

## Blocks processing & Database access layer

Explorer scans blockchain ascending from genesis block to the latest one.
Each full block when grabbed from the network is divided into the following entities:
1. `Header` - mirrors `header` entity from Ergo protocol
2. `BlockInfo` - represents block statistics
3. `BlockExtension` - mirrors `block extension` entity from Ergo protocol
4. `AdProof` - mirrors `ADProof` entity from Ergo protocol
5. `Transaction` - represents `transaction` from Ergo protocol, enriched with additional link to header
6. `Input` - represents `input` from Ergo protocol, enriched with additional link to transaction and `mainChain` flag
7. `Output` - represents `output` from Ergo protocol, enriched with additional link to transaction, `mainChain` flag and `address` derived from `ergoTree`
8. `Asset` - represents single `asset` which is stored in box registers in Ergo protocol, contains link to the corresponding box.

When extracted from the full block these entities are inserted to the database.

Database access layer is defined in `org.ergoplatform.explorer.db` module and has the following structure:

- `models` - module containing models described above (all these models has underlying tables in db schema)
    - `aggregates` - module containing models which are not mapped to a single db table exactly but consists of an aggregated data from several tables

- `queries` - module containing sets of sql queries for each entity for concrete database
- `repositories` - defines abstract interfaces for accessing entities' data and also their production implementations.
- `services` - defines sets of operations on different domain areas.
- `protocol`
    - `models` - models mirroring structure of corresponding objects returned by ergo node REST API. 

#### Chain Grabber

Chain grabber is a module responsible for blockchain synchronization between network and local db, it runs as a separate process and
operates according to the following workflow:

- Get height of the best block in the network
- Compare it with the height of the best block in the local db
- Request block ids at each height from local height to network height
- Request full block for each id
- Update chain statuses of existing blocks at this height (old blocks known to explorer at this height are marked as non-best in case of fork processing)
- For each new full block fetched from the network:
    - Lookup its parent info in the cache or in the database
        + If parent is not found fetch ids of all existing blocks from database at previous height and grab unknown before blocks from the network
        + Otherwise split API block into separate db models described above and insert them to the db
        
The key feature of grabber is that it performs update at either particular height of the single chain or range of heights of all known to explorer chains (in case of fork processing) atomically in order to preserve stable chain consistency in any corner case.
