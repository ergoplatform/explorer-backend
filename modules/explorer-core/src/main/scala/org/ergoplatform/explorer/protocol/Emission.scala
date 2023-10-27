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

  def issuedCoinsAfterHeight(h: Long): Long =
    if (h < settings.fixedRatePeriod) {
      settings.fixedRate * h
    } else if (!reemission.applyReemissionRules || h < reemission.activationHeight) {
      val fixedRateEmission: Long = settings.fixedRate * (settings.fixedRatePeriod - 1)
      val currentEpoch            = epoch(h)
      val completeNonFixedRateEpochsEmission: Long = (1 to currentEpoch.toInt).map { e =>
        math.max(settings.fixedRate - settings.oneEpochReduction * e, 0) * settings.epochLength
      }.sum
      val heightInThisEpoch       = (h - settings.fixedRatePeriod) % settings.epochLength + 1
      val rateThisEpoch           = math.max(settings.fixedRate - settings.oneEpochReduction * (currentEpoch + 1), 0)
      val incompleteEpochEmission = heightInThisEpoch * rateThisEpoch

      completeNonFixedRateEpochsEmission + fixedRateEmission + incompleteEpochEmission
    } else {
      val emissionBeforeEip27 = issuedCoinsAfterHeight(reemission.activationHeight - 1)
      val firstEpochAfterActivation =
        (reemission.activationHeight - settings.fixedRatePeriod) / settings.epochLength + 1
      val firstReductionAfterActivation = firstEpochAfterActivation * settings.epochLength
      val currentEpoch                  = epoch(h)
      val defaultRewardPerBlockInCurrentEpoch =
        math.max(settings.fixedRate - settings.oneEpochReduction * currentEpoch, 0)
      val adjustedReward = defaultRewardPerBlockInCurrentEpoch - getReemission(
        defaultRewardPerBlockInCurrentEpoch
      )
      if (h < firstReductionAfterActivation) {
        val blocksSinceActivation              = h - reemission.activationHeight
        val accumulatedEmissionSinceActivation = blocksSinceActivation * adjustedReward
        emissionBeforeEip27 + accumulatedEmissionSinceActivation
      } else {
        val accumulatedEmissionSinceActivationBeforeFirstReduction =
          (firstReductionAfterActivation - reemission.activationHeight) * adjustedReward
        if (h < reemission.reemissionStartHeight) {
          val accumulatedEmissionSinceFirstReduction =
            (firstEpochAfterActivation to currentEpoch.toInt).map(e => getEpochEmissionAfterEip27(e)).sum
          val heightInThisEpoch           = (h - settings.fixedRatePeriod) % settings.epochLength + 1
          val rateThisEpoch               = math.max(settings.fixedRate - settings.oneEpochReduction * (currentEpoch + 1), 0)
          val rateThisEpochWithReemission = rateThisEpoch - getReemission(rateThisEpoch)
          val incompleteEpochEmission     = heightInThisEpoch * rateThisEpochWithReemission
          emissionBeforeEip27 + accumulatedEmissionSinceActivationBeforeFirstReduction + accumulatedEmissionSinceFirstReduction + incompleteEpochEmission
        } else {
          val lastEmissionEpoch =
            (reemission.reemissionStartHeight - settings.fixedRatePeriod) / settings.epochLength + 1
          val accumulatedEmissionSinceFirstReductionUntilReemission =
            (firstEpochAfterActivation to lastEmissionEpoch).map(e => getEpochEmissionAfterEip27(e)).sum
          val reemissionTail =
            math.min(h - reemission.reemissionStartHeight, reemissionLen) * constants.Eip27ResidualEmission
          emissionBeforeEip27 + accumulatedEmissionSinceActivationBeforeFirstReduction + accumulatedEmissionSinceFirstReductionUntilReemission + reemissionTail
        }
      }
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
    val defaultRewardInEpoch = math.max(settings.fixedRate - settings.oneEpochReduction * e, 0) * settings.epochLength
    defaultRewardInEpoch - getReemission(defaultRewardInEpoch) * settings.epochLength
  }

  private def getReemission(reward: Long): Long =
    if (reward >= constants.Eip27UpperPoint) constants.Eip27DefaultReEmission
    else math.max(reward - constants.Eip27ResidualEmission, 0)
}
