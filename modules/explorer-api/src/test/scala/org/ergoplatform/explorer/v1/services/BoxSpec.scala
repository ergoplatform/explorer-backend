package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import cats.syntax.list._
import dev.profunktor.redis4cats.algebra.RedisCommands
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, BoxId, ErgoTree}
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.commonGenerators.forSingleInstance
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.http.api.models.{Paging, Sorting}
import org.ergoplatform.explorer.http.api.models.Sorting.{Desc, SortOrder}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.OutputInfo
import org.ergoplatform.explorer.http.api.v1.services.{Boxes, Mempool}
import org.ergoplatform.explorer.settings.{RedisSettings, ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.v1.services.constants._
import org.ergoplatform.explorer.protocol.sigma
import org.scalatest.{PrivateMethodTester, TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try
import tofu.syntax.monadic._

class BoxSpec extends AnyFlatSpec with should.Matchers with TryValues with PrivateMethodTester with RealDbTest {
  import org.ergoplatform.explorer.v1.services.BoxSpec._
  import org.ergoplatform.explorer.db.models.generators._

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Box Service" should "get unspent outputs with exclusions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    val getUnspentOutputsByAddressF             = PrivateMethod[IO[List[OutputInfo]]]('getUnspentOutputsByAddressF)
    withResources[IO]
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (_, box) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out_, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out_).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                forSingleInstance(
                  balanceOfAddressGen(
                    mainChain = true,
                    address1T.get,
                    address1Tree,
                    (100.toNanoErgo, 1) :: (200.toNanoErgo, 1) :: List[(Long, Int)]()
                  )
                ) { infoTupleList =>
                  infoTupleList.foreach { case (header, out, tx) =>
                    hRepo.insert(header).runWithIO()
                    outRepo.insert(out).runWithIO()
                    txRepo.insert(tx).runWithIO()
                  }
                  box.getOutputsByAddress(address1T.get, Paging(0, Int.MaxValue)).unsafeRunSync().total should be(3)
                  (box invokePrivate getUnspentOutputsByAddressF(
                    address1T.get,
                    Desc,
                    List(out_.boxId).map(x => BoxId(x.value)).toNel
                  ))
                    .unsafeRunSync()
                    .map(_.boxId) should not contain theSameElementsAs(List(out_.boxId))
                }
            }
          }
        }
      }
      .unsafeRunSync()
  }

  it should "merge unconfirmed outputs (from mempool) & unspent outputs with exclusions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    withResources[IO]
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (mem, box) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out_, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out_).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                forSingleInstance(
                  balanceOfAddressGen(
                    mainChain = true,
                    address1T.get,
                    address1Tree,
                    (100.toNanoErgo, 1) :: (200.toNanoErgo, 1) :: List[(Long, Int)]()
                  )
                ) { infoTupleList =>
                  infoTupleList.foreach { case (header, out, tx) =>
                    hRepo.insert(header).runWithIO()
                    outRepo.insert(out).runWithIO()
                    txRepo.insert(tx).runWithIO()
                  }
                  box.getOutputsByAddress(address1T.get, Paging(0, Int.MaxValue)).unsafeRunSync().total should be(3)
                  val data = box
                    .`getUnspent&UnconfirmedOutputsMergedByAddress`(
                      address1T.get,
                      Desc,
                      mem.getBoxesSpentInMempool,
                      mem.getUOutputsByAddress
                    )
                    .unsafeRunSync()
                    .map(_.boxId)

                  data.length should be(3)
                  data should contain theSameElementsAs (List(uout.boxId) ++ infoTupleList.map(_._2.boxId))
                }
            }
          }
        }
      }
      .unsafeRunSync()
  }
}

object BoxSpec {
  import cats.effect.Sync
  import scala.concurrent.duration._
  import cats.syntax.traverse._
  import org.ergoplatform.explorer.db.repositories.{
    HeaderRepo,
    OutputRepo,
    TransactionRepo,
    UInputRepo,
    UOutputRepo,
    UTransactionRepo
  }

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private def withResources[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift] = for {
    redis <- Some(RedisSettings("redis://localhost:6379")).map(Redis[F]).sequence
  } yield (ServiceSettings(chunkSize = 100), UtxCacheSettings(transactionTtl = 10.minute), redis)

  private def withServices[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift, D[
    _
  ]: LiftConnectionIO: CompileStream: Sync](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(body: (Mempool[F], Boxes[F]) => Any)(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      mempool <- Mempool[F, D](
                   settings,
                   utxCacheSettings,
                   redis
                 )(trans)
      boxes <- Boxes[F, D](settings)(trans)
      _ = body(mempool, boxes)
    } yield ()

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      UOutputRepo[D, fs2.Stream],
      UInputRepo[D, fs2.Stream],
      UTransactionRepo[D, fs2.Stream]
    ) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.UOutputRepo[IO, D].unsafeRunSync(),
      repositories.UInputRepo[IO, D].unsafeRunSync(),
      repositories.UTransactionRepo[IO, D].unsafeRunSync()
    )
}
