package org.ergoplatform.explorer.settings

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.protocol.{Emission, ReemissionSettings}
import org.ergoplatform.mining.emission.EmissionRules
import org.ergoplatform.settings.MonetarySettings

final case class ProtocolSettings(
  networkPrefix: String Refined ValidByte,
  genesisAddress: Address,
  monetary: MonetarySettings
) {

  val emission = new Emission(monetary, ReemissionSettings())

  val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}
