package org.ergoplatform.explorer.db.models

import org.ergoplatform.explorer.Address

/** Represents `known_miners` table.
  */
final case class Miner(address: Address, name: String)
