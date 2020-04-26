package org.ergoplatform.explorer.context

import cats.Parallel
import cats.effect.{ConcurrentEffect, ContextShift, Resource}
import doobie.free.connection.ConnectionIO
import org.ergoplatform.explorer.clients.ergo.ErgoNetworkClient
import org.ergoplatform.explorer.db.{DoobieTrans, Trans}
import org.http4s.client.blaze.BlazeClientBuilder
import tofu.optics.macros.{ClassyOptics, promote}

import scala.concurrent.ExecutionContext

@ClassyOptics
final case class GrabberContext[F[_], D[_]](
  @promote settings: SettingsContext,
  @promote repos: RepositoryContext[D, fs2.Stream],
  networkClient: ErgoNetworkClient[F, fs2.Stream],
  trans: D Trans F
)

object GrabberContext {

  def make[
    F[_]: ConcurrentEffect: ContextShift: Parallel
  ](configPathOpt: Option[String])(
    implicit ec: ExecutionContext
  ): Resource[F, GrabberContext[F, ConnectionIO]] =
    for {
      settings <- Resource.liftF(SettingsContext.make[F](configPathOpt))
      repos    <- Resource.liftF(RepositoryContext.make[F, ConnectionIO])
      client   <- BlazeClientBuilder[F](ec).resource
      xa       <- DoobieTrans[F]("GrabberPool", settings.db)
      ns       <- Resource.liftF(ErgoNetworkClient[F](client, settings.masterNodesAddresses))
    } yield GrabberContext(settings, repos, ns, Trans.fromDoobie(xa))
}
