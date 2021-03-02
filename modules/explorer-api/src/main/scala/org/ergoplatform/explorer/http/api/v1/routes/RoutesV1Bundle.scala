package org.ergoplatform.explorer.http.api.v1.routes

import cats.Monad
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services._
import org.ergoplatform.explorer.settings.{RequestsSettings, ServiceSettings}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.Throws
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext

final case class RoutesV1Bundle[F[_]](routes: HttpRoutes[F])

object RoutesV1Bundle {

  def apply[
    F[_]: Concurrent: ContextShift: Timer,
    D[_]: Monad: Throws: LiftConnectionIO: CompileStream
  ](serviceSettings: ServiceSettings, requestsSettings: RequestsSettings)(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F]
  ): F[RoutesV1Bundle[F]] =
    for {
      implicit0(log: Logger[F]) <- Slf4jLogger.create
      boxes                     <- Boxes(serviceSettings)(trans)
      assets                    <- Assets(trans)
      epochs                    <- Epochs(trans)
      transactions              <- Transactions(serviceSettings)(trans)
      boxesRoutes  = BoxesRoutes(requestsSettings, boxes)
      epochsRoutes = EpochsRoutes(epochs)
      assetsRoutes = AssetsRoutes(requestsSettings, assets)
      txsRoutes    = TransactionsRoutes(requestsSettings, transactions)
      docs         = DocsRoutes(requestsSettings)
      routes       = txsRoutes <+> boxesRoutes <+> epochsRoutes <+> assetsRoutes <+> docs
    } yield RoutesV1Bundle(routes)
}
