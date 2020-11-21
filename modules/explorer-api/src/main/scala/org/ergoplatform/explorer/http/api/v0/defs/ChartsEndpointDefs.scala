package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives.timespan
import org.ergoplatform.explorer.http.api.v0.models.{ChartPoint, HashRateDistributionSegment}
import sttp.tapir._
import sttp.tapir.json.circe._

import scala.concurrent.duration.FiniteDuration

object ChartsEndpointDefs {

  private val PathPrefix = "charts"

  def endpoints: List[Endpoint[_, _, _, _]] =
    getTotalCoinsAmtDef :: getAvgBlockSizeDef :: getBlockChainSizeDef :: getAvgTxsNumPerBlockDef ::
    getTotalTxsNumDef :: getAvgDifficultyDef :: getMinersRevenueDef :: getHashRateDef ::
    getHashRateDistributionDef :: Nil

  def getTotalCoinsAmtDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get.in(PathPrefix / "total").in(timespan).out(jsonBody[List[ChartPoint]])

  def getAvgBlockSizeDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get.in(PathPrefix / "block-size").in(timespan).out(jsonBody[List[ChartPoint]])

  def getBlockChainSizeDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "blockchain-size")
      .in(timespan)
      .out(jsonBody[List[ChartPoint]])

  def getAvgTxsNumPerBlockDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "transactions-per-block")
      .in(timespan)
      .out(jsonBody[List[ChartPoint]])

  def getTotalTxsNumDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "transactions-number")
      .in(timespan)
      .out(jsonBody[List[ChartPoint]])

  def getAvgDifficultyDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get.in(PathPrefix / "difficulty").in(timespan).out(jsonBody[List[ChartPoint]])

  def getMinersRevenueDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "miners-revenue")
      .in(timespan)
      .out(jsonBody[List[ChartPoint]])

  def getHashRateDef: Endpoint[FiniteDuration, ApiErr, List[ChartPoint], Any] =
    baseEndpointDef.get.in(PathPrefix / "hash-rate").in(timespan).out(jsonBody[List[ChartPoint]])

  def getHashRateDistributionDef
    : Endpoint[Unit, ApiErr, List[HashRateDistributionSegment], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "hash-rate-distribution")
      .out(jsonBody[List[HashRateDistributionSegment]])
}
