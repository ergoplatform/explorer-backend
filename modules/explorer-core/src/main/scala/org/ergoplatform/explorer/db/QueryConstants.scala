package org.ergoplatform.explorer.db

/** PostgreSQL query parameter limits and constants.
  * 
  * PostgreSQL has a hard limit on the number of parameters that can be passed to a single query.
  * When using `IN (?, ?, ?)` clauses with large lists, we must chunk the data to avoid exceeding this limit.
  * 
  * Error when limit exceeded: "Tried to send an out-of-range integer as a 2-byte value: XXXXX"
  * 
  * @see https://github.com/ergoplatform/explorer-backend/issues/156
  */
object QueryConstants {
  
  /** Maximum number of IDs that can be safely passed to a single database query.
    * 
    * PostgreSQL hard limit: 32,767 (Short.MaxValue) parameters per query
    * 
    * We use 1/4 of this limit (8,191) to provide a safety margin because:
    * 1. Queries may have other parameters beyond the ID list
    * 2. Some queries join multiple tables with their own parameters
    * 3. Provides buffer for edge cases
    * 
    * When a list exceeds this size, it should be chunked into multiple queries
    * and the results combined using `flatTraverse`.
    */
  val MaxIdsPerQuery: Int = scala.Short.MaxValue / 4  // 8,191
}
