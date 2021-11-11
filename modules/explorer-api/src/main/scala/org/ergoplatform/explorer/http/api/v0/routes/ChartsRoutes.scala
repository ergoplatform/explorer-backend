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

  private def interpreter = Http4sServerInterpreter(opts)

  private def getTotalCoinsAmtR: HttpRoutes[F] =
    interpreter.toRoutes(getTotalCoinsAmtDef) { timespan =>
      service.getTotalCoins(timespan).adaptThrowable.value
    }

  private def getAvgBlockSizeR: HttpRoutes[F] =
    interpreter.toRoutes(getAvgBlockSizeDef) { timespan =>
      service.getAvgBlockSize(timespan).adaptThrowable.value
    }

  private def getBlockChainSizeR: HttpRoutes[F] =
    interpreter.toRoutes(getBlockChainSizeDef) { timespan =>
      service.getBlockChainSize(timespan).adaptThrowable.value
    }

  private def getAvgTxsNumPerBlockR: HttpRoutes[F] =
    interpreter.toRoutes(getAvgTxsNumPerBlockDef) { timespan =>
      service.getAvgTxsNumPerBlock(timespan).adaptThrowable.value
    }

  private def getTotalTxsNumR: HttpRoutes[F] =
    interpreter.toRoutes(getTotalTxsNumDef) { timespan =>
      service.getTransactionsNum(timespan).adaptThrowable.value
    }

  private def getAvgDifficultyR: HttpRoutes[F] =
    interpreter.toRoutes(getAvgDifficultyDef) { timespan =>
      service.getAvgDifficulty(timespan).adaptThrowable.value
    }

  private def getMinersRevenuR: HttpRoutes[F] =
    interpreter.toRoutes(getMinersRevenueDef) { timespan =>
      service.getMinersRevenue(timespan).adaptThrowable.value
    }

  private def getHashRateR: HttpRoutes[F] =
    interpreter.toRoutes(getHashRateDef) { timespan =>
      service.getHashRate(timespan).adaptThrowable.value
    }

  private def getHashRateDistributionR: HttpRoutes[F] =
    interpreter.toRoutes(getHashRateDistributionDef) { _ =>
      service.getHashRateDistribution(24.hours).adaptThrowable.value
    }
}

object ChartsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](service: StatsService[F])(
    implicit opts: Http4sServerOptions[F, F]
  ): HttpRoutes[F] =
    new ChartsRoutes(service).routes
}
