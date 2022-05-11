package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import dev.profunktor.redis4cats.algebra.RedisCommands
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, ErgoTree}
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.commonGenerators.forSingleInstance
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services.{Addresses, Mempool}
import org.ergoplatform.explorer.settings.{RedisSettings, ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.v1.services.constants._
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.testContainers.RedisTest
import org.scalatest.{PrivateMethodTester, TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try
import tofu.syntax.monadic._

class MempoolSpec
  extends AnyFlatSpec
  with should.Matchers
  with TryValues
  with PrivateMethodTester
  with RealDbTest
  with RedisTest {
  import org.ergoplatform.explorer.v1.services.MempoolSpec._
  import org.ergoplatform.explorer.db.models.generators._

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Mempool Service" should "check if address has unconfirmed transactions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address2S                               = ReceiverAddressString
    val address1T                               = Address.fromString[Try](address1S)
    val address2T                               = Address.fromString[Try](address2S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    lazy val address2Tree                       = sigma.addressToErgoTreeHex(address2T.get)
    withResources[IO](container.mappedPort(6379))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (mem, _) =>
          address1T.isSuccess should be(true)
          address2T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out1, _, _, _, header1, tx1) =>
                hRepo.insert(header1).runWithIO()
                txRepo.insert(tx1).runWithIO()
                outRepo.insert(out1).runWithIO()
                mem.hasUnconfirmedBalance(ErgoTree(address1Tree)).unsafeRunSync() should be(false)
                forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address2T.get, address2Tree)) {
                  case (out, uout, uin, utx, header, tx) =>
                    hRepo.insert(header).runWithIO()
                    txRepo.insert(tx).runWithIO()
                    outRepo.insert(out).runWithIO()
                    uTxRepo.insert(utx).runWithIO()
                    uinRepo.insert(uin).runWithIO()
                    uoutRepo.insert(uout).runWithIO()
                    mem.hasUnconfirmedBalance(ErgoTree(address2Tree)).unsafeRunSync() should be(true)
                }
            }
          }
        }
      }
      .unsafeRunSync()
  }

  it should "get unconfirmed spent boxes in mempool" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    withResources[IO](container.mappedPort(6379))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (mem, _) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                mem.getBoxesSpentInMempool(address1T.get).unsafeRunSync() should be(List(out.boxId))

            }
          }
        }
      }
      .unsafeRunSync()
  }

  it should "get unconfirmed outputs in mempool" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    withResources[IO](container.mappedPort(6379))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (mem, _) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                mem.getUOutputsByAddress(address1T.get).unsafeRunSync().map(_.transactionId) should be(List(utx.id))
            }
          }
        }
      }
      .unsafeRunSync()
  }

  it should "get Total balance considering mempool transactions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    withResources[IO](container.mappedPort(6379))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (mem, addr) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(
              balanceOfAddressGen(
                mainChain = true,
                address1T.get,
                address1Tree,
                (100.toNanoErgo, 1) :: (200.toNanoErgo, 1) :: (300.toNanoErgo, 1) :: List[(Long, Int)]()
              )
            ) { infoTupleList =>
              infoTupleList.foreach { case (header, out, tx) =>
                hRepo.insert(header).runWithIO()
                outRepo.insert(out).runWithIO()
                txRepo.insert(tx).runWithIO()
              }
              val tb = mem.getTotalBalance(address1T.get, addr.confirmedBalanceOf).unsafeRunSync()
              tb.confirmed.nanoErgs should be(600.toNanoErgo)
            }
          }
        }
      }
      .unsafeRunSync()
  }
}

object MempoolSpec {
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

  private def withResources[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift](port: Int) = for {
    redis <- Some(RedisSettings(s"redis://localhost:$port")).map(Redis[F]).sequence
  } yield (ServiceSettings(chunkSize = 100), UtxCacheSettings(transactionTtl = 10.minute), redis)

  private def withServices[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift, D[
    _
  ]: LiftConnectionIO: CompileStream: Sync](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(body: (Mempool[F], Addresses[F]) => Any)(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      mempool <- Mempool[F, D](
                   settings,
                   utxCacheSettings,
                   redis
                 )(trans)
      addresses <- Addresses[F, D](trans)
      _ = body(mempool, addresses)
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
