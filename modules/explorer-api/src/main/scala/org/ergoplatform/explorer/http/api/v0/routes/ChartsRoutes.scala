package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v0.services.StatsService
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

import scala.concurrent.duration._

final class ChartsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](service: StatsService[F])(implicit opts: Http4sServerOptions[F, F]) {

  import org.ergoplatform.explorer.http.api.v0.defs.ChartsEndpointDefs._

  val routes: HttpRoutes[F] =
    getTotalCoinsAmtR <+> getAvgBlockSizeR <+> getBlockChainSizeR <+>
    getAvgTxsNumPerBlockR <+> getTotalTxsNumR <+> getAvgDifficultyR <+>
    getMinersRevenuR <+> getHashRateR <+> getHashRateDistributionR

  private def getTotalCoinsAmtR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getTotalCoinsAmtDef) { timespan =>
      service.getTotalCoins(timespan).adaptThrowable.value
    }

  private def getAvgBlockSizeR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getAvgBlockSizeDef) { timespan =>
      service.getAvgBlockSize(timespan).adaptThrowable.value
    }

  private def getBlockChainSizeR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getBlockChainSizeDef) { timespan =>
      service.getBlockChainSize(timespan).adaptThrowable.value
    }

  private def getAvgTxsNumPerBlockR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getAvgTxsNumPerBlockDef) { timespan =>
      service.getAvgTxsNumPerBlock(timespan).adaptThrowable.value
    }

  private def getTotalTxsNumR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getTotalTxsNumDef) { timespan =>
      service.getTransactionsNum(timespan).adaptThrowable.value
    }

  private def getAvgDifficultyR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getAvgDifficultyDef) { timespan =>
      service.getAvgDifficulty(timespan).adaptThrowable.value
    }

  private def getMinersRevenuR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getMinersRevenueDef) { timespan =>
      service.getMinersRevenue(timespan).adaptThrowable.value
    }

  private def getHashRateR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getHashRateDef) { timespan =>
      service.getHashRate(timespan).adaptThrowable.value
    }

  private def getHashRateDistributionR: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(getHashRateDistributionDef) { _ =>
      service.getHashRateDistribution(24.hours).adaptThrowable.value
    }
}

object ChartsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](service: StatsService[F])(
    implicit opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new ChartsRoutes(service).routes
}
