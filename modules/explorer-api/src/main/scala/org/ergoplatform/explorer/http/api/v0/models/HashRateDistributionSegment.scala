package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.MinerStats
import sttp.tapir.{Schema, Validator}
import sttp.tapir.generic.Derived

final case class HashRateDistributionSegment(name: String, value: Int)

object HashRateDistributionSegment {

  implicit val codec: Codec[HashRateDistributionSegment] = deriveCodec

  implicit val schema: Schema[HashRateDistributionSegment] =
    Schema
      .derive[HashRateDistributionSegment]
      .modify(_.name)(_.description("Segment name"))
      .modify(_.value)(_.description("Number of blocks mined"))

  implicit val validator: Validator[HashRateDistributionSegment] = Validator.derive

  def batch(stats: List[MinerStats]): List[HashRateDistributionSegment] = {
    val totalCount = stats.map(_.blocksMined).sum
    def threshold(m: MinerStats): Boolean =
      ((m.blocksMined.toDouble * 100) / totalCount.toDouble) > 1.0

    val (major, other) = stats.partition(threshold)
    val otherSumStats  = HashRateDistributionSegment("other", other.map(_.blocksMined).sum)
    val majorSegmants = major.map { info =>
      HashRateDistributionSegment(info.verboseName, info.blocksMined)
    }
    (majorSegmants :+ otherSumStats).sortBy(x => -x.value).filterNot(_.value == 0L)
  }
}
