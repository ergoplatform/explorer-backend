package org.ergoplatform.explorer.indexer.models

import cats.Applicative
import org.ergoplatform.explorer.Err.ProcessingErr.IncorrectNetworkView
import org.ergoplatform.explorer.Err.RequestProcessingErr.InconsistentDbData
import org.ergoplatform.explorer.{CRaise, Id, UrlString}
import tofu.Raise
import tofu.logging.Loggable
import tofu.syntax.monadic._
import tofu.syntax.raise._

import scala.collection.immutable.SortedMap

final case class HeightsResult(
  bestHeight: Int,
  distinguishHeights: List[(UrlString, Int)]
)

object HeightsResult {

  def apply[F[_]: CRaise[*[_], IncorrectNetworkView]: Applicative](heights: List[(UrlString, Int)]): F[HeightsResult] = {
    val sorted = SortedMap(heights.groupBy(_._2).toSeq: _*)
    sorted.lastOption
      .fold(IncorrectNetworkView("No one height was parsed").raise[F, HeightsResult])(last =>
        HeightsResult(last._1, sorted.init.values.flatten.toList).pure[F]
      )
  }
}
