package org.ergoplatform.explorer.settings

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.mining.emission.EmissionRules
import org.ergoplatform.settings.MonetarySettings

final case class ProtocolSettings(
  networkPrefix: String Refined ValidByte,
  genesisAddress: Address,
  monetary: MonetarySettings
) {

  val emission = new EmissionRules(monetary)

  val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}
