package org.ergoplatform.explorer.services

import cats.effect.Sync
import cats.{ApplicativeError, Monad}
import cats.syntax.functor._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder
import jawnfs2._
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiNodeInfo, ApiTransaction}
import org.ergoplatform.explorer.{Exc, Id}
import org.ergoplatform.explorer.settings.Settings
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

/** A service providing an access to the Ergo network.
  */
trait ErgoNetworkService[F[_], G[_]] {

  /** Get height of the best block.
    */
  def getBestHeight: F[Int]

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

  def apply[F[_]: Sync](
    client: Client[F],
    settings: Settings
  ): F[ErgoNetworkService[F, Stream[F, *]]] =
    Sync[F]
      .delay(Slf4jLogger.getLogger[F])
      .map(logger => new Live[F](client, logger, settings))

  final private class Live[F[_]: Sync](
    client: Client[F],
    logger: Logger[F],
    settings: Settings
  ) extends ErgoNetworkService[F, Stream[F, *]] {

    import io.circe.jawn.CirceSupportParser.facade
    import org.http4s.circe.CirceEntityDecoder._

    def getBestHeight: F[Int] =
      retrying { uri =>
        client
          .expect[ApiNodeInfo](
            makeGetRequest(s"$uri/info")
          )
          .map(_.fullHeight)
      }

    def getBlockIdsAtHeight(height: Int): F[List[Id]] =
      retrying { uri =>
        client.expect[List[Id]](
          makeGetRequest(s"$uri/blocks/at/$height")
        )
      }

    def getFullBlockById(id: Id): F[Option[ApiFullBlock]] =
      retrying { uri =>
        client.expectOption[ApiFullBlock](
          makeGetRequest(s"$uri/blocks/$id")
        )
      }

    def getUnconfirmedTransactions: Stream[F, ApiTransaction] =
      retrying[Stream[F, *], ApiTransaction] { uri =>
        client
          .stream(makeGetRequest(s"$uri/transactions/unconfirmed"))
          .flatMap(_.body.chunks.parseJsonStream)
          .flatMap { json =>
            implicitly[Decoder[ApiTransaction]]
              .decodeJson(json)
              .fold(
                _ => Stream.raiseError[F](Exc("Json decoding failed")),
                Stream.emit
              )
          }
      }

    private def retrying[M[_]: Monad, A](
      f: String Refined Url => M[A]
    )(implicit G: ApplicativeError[M, Throwable]): M[A] = {
      def attempt(uris: List[String Refined Url])(i: Int): M[A] =
        uris match {
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
