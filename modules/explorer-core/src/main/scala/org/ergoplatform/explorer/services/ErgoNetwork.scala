package org.ergoplatform.explorer.services

import cats.Parallel
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.parallel._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.Err.RequestProcessingErr.NetworkErr.InvalidTransaction
import org.ergoplatform.explorer.protocol.models.{ApiFullBlock, ApiNodeInfo, ApiTransaction}
import org.ergoplatform.explorer.protocol.ergoInstances._
import org.ergoplatform.explorer.settings.NetworkSettings
import org.ergoplatform.explorer.{BlockId, UrlString}
import org.http4s.Status.InternalServerError
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.{Method, Request, Status, Uri}
import tofu.Tries
import tofu.syntax.handle._
import tofu.syntax.monadic._
import tofu.syntax.raise._

/** A service providing an access to the Ergo network.
  */
trait ErgoNetwork[F[_]] {

  /** Get height of the best block.
    */
  def getBestHeight: F[Int]

  /** Get info of current node state
    */
  def getNodeInfo: F[ApiNodeInfo]

  /** Get block ids at the given `height`.
    */
  def getBlockIdsAtHeight(height: Int): F[List[BlockId]]

  /** Get full block by its `id`.
    */
  def getFullBlockById(id: BlockId): F[Option[ApiFullBlock]]

  /** Get unconfirmed transactions from UTX pool.
    */
  def getUnconfirmedTransactions: F[List[ApiTransaction]]

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[Unit]
}

object ErgoNetwork {

  def apply[F[_]: Sync: Parallel](
    client: Client[F],
    settings: NetworkSettings
  ): F[ErgoNetwork[F]] =
    Slf4jLogger.create[F] >>= { implicit log =>
      (NodesPool(settings.masterNodes), Ref.of(0L))
        .mapN { (pool, tickRef) =>
          new Live[F](settings, client, pool, tickRef)
        }
    }

  final private class Live[
    F[_]: Sync: Parallel: Logger: Tries
  ](
    settings: NetworkSettings,
    client: Client[F],
    pool: NodesPool[F],
    tickRef: Ref[F, Long]
  ) extends ErgoNetwork[F] {

    private val txsBatchSize = 20

    private val log = Logger[F]

    def getBestHeight: F[Int] =
      run(getBestHeight)

    def getNodeInfo: F[ApiNodeInfo] =
      run { url =>
        client.expect[ApiNodeInfo](makeGetRequest(s"$url/info"))
      }

    def getBlockIdsAtHeight(height: Int): F[List[BlockId]] =
      run { url =>
        client.expect[List[BlockId]](
          makeGetRequest(s"$url/blocks/at/$height")
        )
      }

    def getFullBlockById(id: BlockId): F[Option[ApiFullBlock]] =
      run { url =>
        client.expectOption[ApiFullBlock](
          makeGetRequest(s"$url/blocks/$id")
        )
      }

    def getUnconfirmedTransactions: F[List[ApiTransaction]] =
      run { url =>
        def go(i: Int, acc: List[ApiTransaction]): F[List[ApiTransaction]] = {
          val limit  = txsBatchSize
          val offset = i * limit
          val req = Request[F](
            Method.GET,
            Uri
              .unsafeFromString(s"$url/transactions/unconfirmed")
              .withQueryParam("offset", offset)
              .withQueryParam("limit", limit)
          )
          client.expect[List[ApiTransaction]](req).flatMap {
            case Nil => acc.pure[F]
            case xs  => go(i + 1, acc ++ xs)
          }
        }
        go(0, List())
      }

    def submitTransaction(tx: ErgoLikeTransaction): F[Unit] =
      pool.getAll.flatMap {
        _.parTraverse { url =>
          client
            .status(
              Request[F](
                Method.POST,
                Uri.unsafeFromString(s"$url/transactions")
              ).withEntity(tx)
            )
            .handleWith[Throwable] { e =>
              log.error(e)(s"Failed to submit transaction{id=${tx.id} to node{url=$url") as InternalServerError
            }
        }
          .flatMap { res =>
            if (res.exists(_.responseClass == Status.ClientError))
              InvalidTransaction(tx.id).raise
            else ().pure[F]
          }
      }

    private def updateView: F[Unit] =
      pool.getAll >>=
        (_.parTraverse(node => getBestHeight(node).tupleLeft(node))) >>=
        (xs => pool.setBest(xs.toList.maxBy(_._2)._1))

    private def run[A](reqF: UrlString => F[A]): F[A] =
      tickRef.update(_ + 1) >>
      (tickRef.get >>= (tick => (tick % settings.selfCheckIntervalRequests == 0).when_(updateView))) >>
      (pool.getBest >>= { node =>
        reqF(node).handleWith[Throwable] { e =>
          log.warn(s"Failed to execute request to `$node` due to $e") >>
          pool.rotate >>
          run(reqF)
        }
      })

    private def getBestHeight(url: UrlString): F[Int] =
      client
        .expect[ApiNodeInfo](
          makeGetRequest(s"$url/info")
        )
        .map(_.fullHeight)

    private def makeGetRequest(uri: String) =
      Request[F](Method.GET, Uri.unsafeFromString(uri))
  }
}
