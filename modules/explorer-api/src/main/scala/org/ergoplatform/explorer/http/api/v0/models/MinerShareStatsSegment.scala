package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.MinerStats
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class MinerShareStatsSegment(name: String, value: Int)

object MinerShareStatsSegment {

  implicit val codec: Codec[MinerShareStatsSegment] = deriveCodec

  implicit val schema: Schema[MinerShareStatsSegment] =
    implicitly[Derived[Schema[MinerShareStatsSegment]]].value
      .modify(_.name)(_.description("Segment name"))
      .modify(_.value)(_.description("Number of blocks mined"))

  def batch(stats: List[MinerStats]): List[MinerShareStatsSegment] = {
    val totalCount = stats.map(_.blocksMined).sum
    def threshold(m: MinerStats): Boolean =
      ((m.blocksMined.toDouble * 100) / totalCount.toDouble) > 1.0

    val (major, other) = stats.partition(threshold)
    val otherSumStats  = MinerShareStatsSegment("other", other.map(_.blocksMined).sum)
    val majorSegmants = major.map { info =>
      MinerShareStatsSegment(info.verboseName, info.blocksMined)
    }
    (majorSegmants :+ otherSumStats).sortBy(x => -x.value).filterNot(_.value == 0L)
  }
}
