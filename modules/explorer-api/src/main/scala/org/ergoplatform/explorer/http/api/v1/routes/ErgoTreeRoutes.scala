package org.ergoplatform.explorer.http.api.v1.routes

import cats.MonadError
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.ErgoTreeEndpointDefs
import org.ergoplatform.explorer.http.api.v1.models.ErgoTreeHuman
import org.ergoplatform.explorer.http.api.models.PrettyErgoTree
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s._


final class ErgoTreeRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
]()(implicit opts: Http4sServerOptions[F, F], F: MonadError[F, Throwable]) {

  val defs = new ErgoTreeEndpointDefs

  val routes: HttpRoutes[F] = convertErgoTreeR

  private def interpreter = Http4sServerInterpreter(opts)

  private def convertErgoTreeR: HttpRoutes[F] =
    interpreter.toRoutes(defs.convertErgoTreeDef) { req => {
      val hexErgoTree = HexString.fromStringUnsafe(req.hashed)
      val (ergoTreeConstants, ergoTreeScript) = PrettyErgoTree.humanErgoTree(hexErgoTree)
      F.pure(Right(ErgoTreeHuman(ergoTreeConstants, ergoTreeScript)))
    }}
}

object ErgoTreeRoutes {
  def apply[F[_]: Concurrent: ContextShift: Timer: Logger]()(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new ErgoTreeRoutes[F]().routes
}
