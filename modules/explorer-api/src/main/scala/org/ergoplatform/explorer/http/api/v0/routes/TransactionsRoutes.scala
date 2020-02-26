package org.ergoplatform.explorer.http.api.v0.routes

import cats.effect.{ContextShift, Sync}
import cats.syntax.semigroupk._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.v0.services.TransactionsService
import org.ergoplatform.explorer.http.api.syntax.applicativeThrow._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class TransactionsRoutes[F[_]: Sync: ContextShift: Logger](
  service: TransactionsService[F, Stream]
) {

  import org.ergoplatform.explorer.http.api.v0.defs.TransactionsEndpointDefs._

  val routes: HttpRoutes[F] =
    getTxByIdR <+> getUnconfirmedTxByIdR <+> getTxsSinceR

  private def getTxByIdR: HttpRoutes[F] =
    getTxByIdDef.toRoutes { txId =>
      service
        .getTxInfo(txId)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Transaction with id: $txId")))
        .either
    }

  private def getUnconfirmedTxByIdR: HttpRoutes[F] =
    getUnconfirmedTxByIdDef.toRoutes { txId =>
      service
        .getUnconfirmedTxInfo(txId)
        .flatMap(_.liftTo[F](ApiErr.NotFound(s"Unconfirmed transaction with id: $txId")))
        .either
    }

  private def getTxsSinceR: HttpRoutes[F] =
    getTxsSinceDef.toRoutes {
      case (paging, height) =>
        service.getTxsSince(height, paging).compile.toList.either
    }
}

object TransactionsRoutes {

  def apply[F[_]: Sync: ContextShift](
    service: TransactionsService[F, Stream]
  ): F[HttpRoutes[F]] =
    Slf4jLogger.create.map { implicit logger =>
      new TransactionsRoutes(service).routes
    }
}
