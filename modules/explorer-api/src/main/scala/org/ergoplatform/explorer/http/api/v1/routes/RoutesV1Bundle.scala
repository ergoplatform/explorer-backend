package org.ergoplatform.explorer.http.api.v1.routes

import cats.Monad
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.v1.services._
import org.ergoplatform.explorer.settings.ServiceSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext

final case class RoutesV1Bundle[F[_]](routes: HttpRoutes[F])

object RoutesV1Bundle {

  def apply[
    F[_]: Concurrent: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](serviceSettings: ServiceSettings)(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F]
  ): F[RoutesV1Bundle[F]] =
    for {
      implicit0(log: Logger[F]) <- Slf4jLogger.create
      boxesService              <- BoxesService(trans)
      boxesRoutes = BoxesRoutes(serviceSettings, boxesService)
      docs        = DocsRoutes(serviceSettings)
      routes      = boxesRoutes <+> docs
    } yield RoutesV1Bundle(routes)
}
