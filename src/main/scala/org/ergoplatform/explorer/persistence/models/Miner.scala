package org.ergoplatform.explorer.persistence.models

import org.ergoplatform.explorer.Address

/** Entity representing `known_miners` table.
  */
final case class Miner(address: Address, name: String)
