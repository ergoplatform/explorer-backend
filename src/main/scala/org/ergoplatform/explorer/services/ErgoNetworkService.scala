package org.ergoplatform.explorer.services

import cats.effect.Sync
import cats.{ApplicativeError, Monad}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.circe.Decoder
import jawnfs2._
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiTransaction}
import org.ergoplatform.explorer.{Id, Settings}
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

/** A service providing an access to the Ergo network.
  */
trait ErgoNetworkService[F[_], G[_]] {

  /** Get block ids at the given `height`.
    */
  def getBlockIdsAtHeight(height: Int): F[List[Id]]

  /** Get full block by its `id`.
    */
  def getFullBlockById(id: Id): F[Option[ApiFullBlock]]

  /** Get unconfirmed transactions from UTX pool.
    */
  def getUnconfirmedTransactions: G[ApiTransaction]
}

object ErgoNetworkService {

  final class Live[F[_]: Sync](client: Client[F], logger: Logger[F], settings: Settings)
    extends ErgoNetworkService[F, Stream[F, *]] {

    import io.circe.jawn.CirceSupportParser.facade

    def getBlockIdsAtHeight(height: Int): F[List[Id]] =
      retrying { url =>
        client.expect[List[Id]](
          makeGetRequest(s"$url/blocks/at/$height")
        )(jsonOf(Sync[F], ???))
      }

    def getFullBlockById(id: Id): F[Option[ApiFullBlock]] =
      retrying { url =>
        client.expectOption[ApiFullBlock](
          makeGetRequest(s"$url/blocks/$id")
        )(jsonOf(Sync[F], implicitly[Decoder[ApiFullBlock]]))
      }

    def getUnconfirmedTransactions: Stream[F, ApiTransaction] =
      retrying[Stream[F, *], ApiTransaction] { url =>
        client
          .stream(makeGetRequest(s"$url/transactions/unconfirmed"))
          .flatMap(_.body.chunks.parseJsonStream)
          .flatMap { json =>
            implicitly[Decoder[ApiTransaction]]
              .decodeJson(json)
              .fold(
                _ => Stream.raiseError[F](new Exception("Json decoding failed")),
                Stream.emit
              )
          }
      }

    private def retrying[G[_]: Monad, A](
      f: String Refined Url => G[A]
    )(implicit G: ApplicativeError[G, Throwable]): G[A] = {
      def attempt(urls: List[String Refined Url])(i: Int): G[A] =
        urls match {
          case hd :: tl =>
            G.handleErrorWith(f(hd)) { _ =>
              attempt(tl)(i + 1)
            }
          case Nil =>
            G.raiseError(new Exception(s"Gave up after $i attempts"))
        }
      attempt(settings.masterNodesAddresses.toList)(0)
    }

    private def makeGetRequest(uri: String) =
      Request[F](Method.GET, Uri.unsafeFromString(uri))
  }
}
