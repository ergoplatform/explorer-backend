package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.v1.defs.TransactionsEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.{Assets, Transactions}
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerOptions, _}

final class TransactionsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: Transactions[F])(implicit opts: Http4sServerOptions[F]) {

  val defs = new TransactionsEndpointDefs(settings)

  val routes: HttpRoutes[F] =
    getByInputsScriptTemplateR

  private def getByInputsScriptTemplateR: HttpRoutes[F] =
    defs.getByInputsScriptTemplateDef.toRoutes { case (template, paging) =>
      service.getByInputsScriptTemplate(template, paging).adaptThrowable.value
    }
}

object TransactionsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Transactions[F]
  )(implicit opts: Http4sServerOptions[F]): HttpRoutes[F] =
    new TransactionsRoutes(settings, service).routes
}
