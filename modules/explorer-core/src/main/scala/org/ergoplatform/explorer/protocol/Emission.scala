package org.ergoplatform.explorer.protocol

import org.ergoplatform.settings.MonetarySettings

final case class ReemissionSettings(
  applyReemissionRules: Boolean = true,
  activationHeight: Int         = 777217,
  reemissionStartHeight: Int    = 2080800
)

final class Emission(settings: MonetarySettings, reemission: ReemissionSettings) {

  private lazy val reemissionLen: Long = (reemission.activationHeight until reemission.reemissionStartHeight).map { h =>
    val defaultReward = math.max(settings.fixedRate - settings.oneEpochReduction * epoch(h), 0)
    getReemission(defaultReward)
  }.sum / constants.Eip27ResidualEmission

  def issuedCoinsAfterHeight(h: Long): Long = {
    var acc: Long = settings.fixedRate * settings.fixedRatePeriod
    var i: Long   = settings.fixedRatePeriod + 1
    while (i <= h) {
      acc += emissionAt(i)
      i += 1
    }
    acc
  }

  def emissionAt(h: Long): Long = {
    val defaultReward = math.max(settings.fixedRate - settings.oneEpochReduction * epoch(h), 0)
    if (h < settings.fixedRatePeriod) {
      settings.fixedRate
    } else if (h < reemission.activationHeight || !reemission.applyReemissionRules) {
      defaultReward
    } else if (h < reemission.reemissionStartHeight && reemission.applyReemissionRules) {
      defaultReward - getReemission(defaultReward)
    } else if (h < reemission.reemissionStartHeight + reemissionLen && reemission.applyReemissionRules) {
      constants.Eip27ResidualEmission
    } else {
      0
    }
  }

  private def epoch(h: Long): Long =
    1 + (h - settings.fixedRatePeriod) / settings.epochLength

  private def getEpochEmissionAfterEip27(e: Int): Long = {
    val defaultReward        = math.max(settings.fixedRate - settings.oneEpochReduction * e, 0)
    val defaultRewardInEpoch = defaultReward * settings.epochLength
    val reemissionInEpoch    = getReemission(defaultReward) * settings.epochLength
    defaultRewardInEpoch - reemissionInEpoch
  }

  private def getReemission(reward: Long): Long =
    if (reward >= constants.Eip27UpperPoint) constants.Eip27DefaultReEmission
    else math.max(reward - constants.Eip27ResidualEmission, 0)
}
