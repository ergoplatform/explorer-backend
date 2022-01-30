package org.ergoplatform.explorer.http.api.v1.routes

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import io.chrisdavenport.log4cats.Logger
import org.ergoplatform.explorer.http.api.{streaming, ApiErr}
import org.ergoplatform.explorer.http.api.algebra.AdaptThrowable.AdaptThrowableEitherT
import org.ergoplatform.explorer.http.api.syntax.adaptThrowable._
import org.ergoplatform.explorer.http.api.syntax.routes._
import org.ergoplatform.explorer.http.api.v1.defs.TransactionsEndpointDefs
import org.ergoplatform.explorer.http.api.v1.services.Transactions
import org.ergoplatform.explorer.settings.RequestsSettings
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.{Http4sServerOptions, _}

final class TransactionsRoutes[
  F[_]: Concurrent: ContextShift: Timer: AdaptThrowableEitherT[*[_], ApiErr]
](settings: RequestsSettings, service: Transactions[F])(implicit opts: Http4sServerOptions[F, F]) {

  val defs = new TransactionsEndpointDefs[F](settings)

  val routes: HttpRoutes[F] =
    streamByGixR <+> getByInputsScriptTemplateR <+> getByIdR

  private def interpreter = Http4sServerInterpreter(opts)

  private def getByIdR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getByIdDef) { txId =>
      service
        .get(txId)
        .adaptThrowable
        .orNotFound(s"Transaction with id: $txId")
        .value
    }

  private def getByInputsScriptTemplateR: HttpRoutes[F] =
    interpreter.toRoutes(defs.getByInputsScriptTemplateDef) { case (template, paging, ordering) =>
      service.getByInputsScriptTemplate(template, paging, ordering).adaptThrowable.value
    }

  private def streamByGixR: HttpRoutes[F] =
    interpreter.toRoutes(defs.streamByGixDef) { case (minGix, limit) =>
      streaming.bytesStream(service.streamAll(minGix, limit))
    }
}

object TransactionsRoutes {

  def apply[F[_]: Concurrent: ContextShift: Timer: Logger](
    settings: RequestsSettings,
    service: Transactions[F]
  )(implicit opts: Http4sServerOptions[F, F]): HttpRoutes[F] =
    new TransactionsRoutes(settings, service).routes
}
