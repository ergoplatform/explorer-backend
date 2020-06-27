package org.ergoplatform.explorer.clients.ergo

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.parallel._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.Err.ProcessingErr.TransactionDecodingFailed
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr.{
  InvalidTransaction,
  RequestFailed,
  TransactionSubmissionFailed
}
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiNodeInfo, ApiTransaction}
import org.ergoplatform.explorer.protocol.ergoInstances._
import org.ergoplatform.explorer.{CRaise, Id, UrlString}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.{Method, Request, Status, Uri}
import tofu.syntax.raise._

/** A service providing an access to the Ergo network.
  */
trait ErgoNetworkClient[F[_]] {

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
  def getUnconfirmedTransactions: F[List[ApiTransaction]]

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[Unit]
}

object ErgoNetworkClient {

  def apply[F[_]: Sync: Parallel](
    client: Client[F],
    masterNodesAddresses: NonEmptyList[UrlString]
  ): F[ErgoNetworkClient[F]] =
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
  ) extends ErgoNetworkClient[F] {

    def getBestHeight: F[Int] =
      retrying { url =>
        client
          .expect[ApiNodeInfo](
            makeGetRequest(s"$url/info")
          )
          .map(_.fullHeight)
      }

    def getBlockIdsAtHeight(height: Int): F[List[Id]] =
      retrying { url =>
        client.expect[List[Id]](
          makeGetRequest(s"$url/blocks/at/$height")
        )
      }

    def getFullBlockById(id: Id): F[Option[ApiFullBlock]] =
      retrying { url =>
        client.expectOption[ApiFullBlock](
          makeGetRequest(s"$url/blocks/$id")
        )
      }

    def getUnconfirmedTransactions: F[List[ApiTransaction]] =
      retrying { url =>
        client.expect[List[ApiTransaction]](makeGetRequest(s"$url/transactions/unconfirmed"))
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
            InvalidTransaction(tx.id).raise[F, Unit]
          else if (!res.exists(_.isSuccess))
            TransactionSubmissionFailed(tx.id).raise[F, Unit]
          else ().pure[F]
        }

    private def retrying[A](f: UrlString => F[A]): F[A] = {
      def attempt(urls: List[UrlString])(i: Int): F[A] =
        urls match {
          case hd :: tl =>
            f(hd).handleErrorWith { e =>
              Logger[F].error(s"Failed to execute request to '$hd'. ${e.getMessage}") >>
              attempt(tl)(i + 1)
            }
          case Nil =>
            RequestFailed(masterNodesAddresses.toList).raise[F, A]
        }
      attempt(masterNodesAddresses.toList)(0)
    }

    private def makeGetRequest(uri: String) =
      Request[F](Method.GET, Uri.unsafeFromString(uri))
  }
}
