package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.TimePoint

final case class ChartPoint(ts: Long, value: Long)

object ChartPoint {

  def apply(point: TimePoint[Long]): ChartPoint =
    ChartPoint(point.ts, point.value)

  implicit val codec: Codec[ChartPoint] = deriveCodec
}
