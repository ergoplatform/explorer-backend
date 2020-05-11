package org.ergoplatform.explorer.clients.ergo

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.parallel._
import cats.syntax.applicative._
import cats.instances.list._
import cats.{ApplicativeError, Monad, Parallel}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Decoder
import io.circe.jawn.CirceSupportParser.facade
import jawnfs2._
import org.ergoplatform.explorer.Err.ProcessingErr.TransactionDecodingFailed
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiNodeInfo, ApiTransaction}
import org.ergoplatform.explorer.protocol.ergoInstances._
import org.ergoplatform.explorer.{CRaise, Id, UrlString}
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr.{
  InvalidTransaction,
  RequestFailed,
  TransactionSubmissionFailed
}
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.{Method, Request, Status, Uri}
import tofu.syntax.raise._

/** A service providing an access to the Ergo network.
  */
trait ErgoNetworkClient[F[_], S[_[_], _]] {

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
  def getUnconfirmedTransactions: S[F, ApiTransaction]

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[Unit]
}

object ErgoNetworkClient {

  def apply[F[_]: Sync: Parallel](
    client: Client[F],
    masterNodesAddresses: NonEmptyList[UrlString]
  ): F[ErgoNetworkClient[F, Stream]] =
    Slf4jLogger
      .create[F]
      .map { implicit logger =>
        new Live[F](client, masterNodesAddresses)
      }

  final private class Live[
    F[_]: Sync: Parallel: Logger: CRaise[*[_], TransactionDecodingFailed]: CRaise[*[_], NetworkErr]
  ](
    client: Client[F],
    masterNodesAddresses: NonEmptyList[UrlString]
  ) extends ErgoNetworkClient[F, Stream] {

    def getBestHeight: F[Int] =
      retrying(Logger[F].error(_)) { url =>
        client
          .expect[ApiNodeInfo](
            makeGetRequest(s"$url/info")
          )
          .map(_.fullHeight)
      }

    def getBlockIdsAtHeight(height: Int): F[List[Id]] =
      retrying(Logger[F].error(_)) { url =>
        client.expect[List[Id]](
          makeGetRequest(s"$url/blocks/at/$height")
        )
      }

    def getFullBlockById(id: Id): F[Option[ApiFullBlock]] =
      retrying(Logger[F].error(_)) { url =>
        client.expectOption[ApiFullBlock](
          makeGetRequest(s"$url/blocks/$id")
        )
      }

    def getUnconfirmedTransactions: Stream[F, ApiTransaction] =
      retrying[Stream[F, *], ApiTransaction](s => Stream.eval(Logger[F].error(s))) { url =>
        client
          .stream(makeGetRequest(s"$url/transactions/unconfirmed"))
          .flatMap(_.body.chunks.parseJsonStream)
          .flatMap { json =>
            implicitly[Decoder[List[ApiTransaction]]]
              .decodeJson(json)
              .fold(
                _ => Stream.eval(TransactionDecodingFailed(json.noSpaces).raise),
                Stream.emits
              )
          }
      }

    def submitTransaction(tx: ErgoLikeTransaction): F[Unit] =
      masterNodesAddresses.toList
        .parTraverse { url =>
          client
            .status(
              Request[F](
                Method.POST,
                Uri.unsafeFromString(s"$url/transactions")
              ).withEntity(tx)
            )
        }
        .flatMap { res =>
          if (res.exists(_.responseClass == Status.ClientError))
            InvalidTransaction(tx.id.toString).raise[F, Unit]
          else if (!res.exists(_.isSuccess))
            TransactionSubmissionFailed(tx.id.toString).raise[F, Unit]
          else ().pure[F]
        }

    private def retrying[M[_]: Monad: CRaise[*[_], RequestFailed], A](logError: String => M[Unit])(
      f: UrlString => M[A]
    )(implicit G: ApplicativeError[M, Throwable]): M[A] = {
      def attempt(urls: List[UrlString])(i: Int): M[A] =
        urls match {
          case hd :: tl =>
            G.handleErrorWith(f(hd)) { e =>
              logError(s"Failed to execute request to '$hd'. ${e.getMessage}") >>
              attempt(tl)(i + 1)
            }
          case Nil =>
            RequestFailed(masterNodesAddresses.toList).raise[M, A]
        }
      attempt(masterNodesAddresses.toList)(0)
    }

    private def makeGetRequest(uri: String) =
      Request[F](Method.GET, Uri.unsafeFromString(uri))
  }
}
