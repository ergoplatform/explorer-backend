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
    - `composite` - module containing models which are not mapped to a single db table exactly but consists of a data from several tables

- `queries` - module containing sets of sql queries for each entity for concrete database
- `repositories` - defines abstract interfaces for accessing entities' data and also their production implementations using `queries` currently.
````
