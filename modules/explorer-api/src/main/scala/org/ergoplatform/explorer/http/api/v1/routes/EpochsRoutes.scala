package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.EpochsEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Epochs
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._

final class EpochsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](epochs: Epochs[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new EpochsEndpointDefs

  val routes: HttpRoutes[F] = getEpochInfoR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getEpochInfoR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getEpochInfoDef) { _ =>
      epochs
        .getLastEpoch
        .adaptThrowable
        .orNotFound(s"Data about last epoch is missing")
        .value
    }
}

object EpochsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](epochs: Epochs[F])(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new EpochsRoutes[F](epochs).routes
}
