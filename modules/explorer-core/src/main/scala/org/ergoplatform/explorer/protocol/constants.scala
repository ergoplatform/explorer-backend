package org.ergoplatform.explorer.protocol

import org.ergoplatform.ErgoScriptPredef
import scorex.util.encode.Base16
import sigmastate.basics.BcDlogGroup
import sigmastate.interpreter.CryptoConstants
import sigmastate.interpreter.CryptoConstants.EcPointType

// Ergo protocol constants. See: https://github.com/ergoplatform/ergo/blob/master/src/main/scala/org/ergoplatform/settings/Constants.scala
object constants {

  val PreGenesisHeight = 0

  val GenesisHeight: Int = PreGenesisHeight + 1

  val PublicKeyLength = 33

  val EpochLength = 1024

  val MinerRewardDelta = 720

  val TeamTreasuryThreshold = 67500000000L

  val group: BcDlogGroup[EcPointType] = CryptoConstants.dlogGroup

  val FeePropositionScriptHex: String = {
    val script = ErgoScriptPredef.feeProposition(MinerRewardDelta)
    Base16.encode(script.bytes)
  }

  val CoinsInOneErgo: Long = 1000000000L

  val ErgoDecimalPlacesNum: Int = 9

}
