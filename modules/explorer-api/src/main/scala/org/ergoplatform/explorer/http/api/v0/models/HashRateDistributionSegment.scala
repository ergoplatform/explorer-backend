package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.magnolia.derivation.decoder.semiauto.deriveMagnoliaDecoder
import io.circe.magnolia.derivation.encoder.semiauto.deriveMagnoliaEncoder
import org.ergoplatform.explorer.db.models.aggregates.MinerStats
import sttp.tapir.{Schema, Validator}

final case class HashRateDistributionSegment(name: String, value: Int)

object HashRateDistributionSegment {

  implicit val codec: Codec[HashRateDistributionSegment] = Codec.from(deriveMagnoliaDecoder, deriveMagnoliaEncoder)

  implicit val schema: Schema[HashRateDistributionSegment] =
    Schema
      .derived[HashRateDistributionSegment]
      .modify(_.name)(_.description("Segment name"))
      .modify(_.value)(_.description("Number of blocks mined"))

  implicit val validator: Validator[HashRateDistributionSegment] = schema.validator

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
