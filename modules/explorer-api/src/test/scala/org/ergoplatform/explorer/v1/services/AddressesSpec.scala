package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.syntax.option._
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
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.repositories.{
  AssetRepo,
  HeaderRepo,
  InputRepo,
  OutputRepo,
  TokenRepo,
  TransactionRepo
}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services.{Addresses, Mempool}
import org.ergoplatform.explorer.settings.{RedisSettings, ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.testContainers.RedisTest
import org.scalatest.{PrivateMethodTester, TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import tofu.syntax.monadic._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try
import org.ergoplatform.explorer.http.api.v1.shared.MempoolProps
import org.ergoplatform.explorer.v1.services.AddressesSpec._
import org.ergoplatform.explorer.db.models.generators._
import org.ergoplatform.explorer.http.api.v1.models.AddressInfo
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.v1.services.constants.{ReceiverAddressString, SenderAddressString}

trait AddressesSpec
  extends AnyFlatSpec
  with should.Matchers
  with TryValues
  with PrivateMethodTester
  with RealDbTest
  with RedisTest

class AS_A extends AddressesSpec {

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Address Service" should "" in {}
}

class AS_C extends AddressesSpec {

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Address Service" should "check if address has been used via ErgoTree" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address2S                               = ReceiverAddressString
    val address1T                               = Address.fromString[Try](address1S)
    val address2T                               = Address.fromString[Try](address2S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    lazy val address2Tree                       = sigma.addressToErgoTreeHex(address2T.get)
    withResources[IO](container.mappedPort(redisTestPort))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (addr, _, _) =>
          address1T.isSuccess should be(true)
          address2T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (headerRepo, txRepo, oRepo, _, _, _) =>
            forSingleInstance(
              balanceOfAddressGen(
                mainChain = true,
                address   = address1T.get,
                address1Tree,
                values = List((100.toNanoErgo, 1), (200.toNanoErgo, 1))
              )
            ) { infoTupleList =>
              infoTupleList.foreach { case (header, out, tx) =>
                headerRepo.insert(header).runWithIO()
                oRepo.insert(out).runWithIO()
                txRepo.insert(tx).runWithIO()
              }
              val hasBeenUsedByErgoTree = PrivateMethod[IO[Boolean]]('hasBeenUsedByErgoTree)
              (addr invokePrivate hasBeenUsedByErgoTree(address1Tree)).unsafeRunSync() should be(true)
              (addr invokePrivate hasBeenUsedByErgoTree(address2Tree)).unsafeRunSync() should be(false)
            }
          }
        }
      }
      .unsafeRunSync()

  }

}

class AS_D extends AddressesSpec {

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Address Service" should "generate batch address info" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address2S                               = ReceiverAddressString
    val address1T                               = Address.fromString[Try](address1S)
    val address2T                               = Address.fromString[Try](address2S)
    lazy val address1Tree                       = sigma.addressToErgoTreeHex(address1T.get)
    lazy val address2Tree                       = sigma.addressToErgoTreeHex(address2T.get)
    val hasBeenUsedByErgoTree                   = PrivateMethod[IO[Boolean]]('hasBeenUsedByErgoTree)
    withResources[IO](container.mappedPort(redisTestPort))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (addr, _, memprop) =>
          address1T.isSuccess should be(true)
          address2T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (headerRepo, txRepo, oRepo, _, tokenRepo, assetRepo) =>
            forSingleInstance(
              balanceOfAddressWithTokenGen(mainChain = true, address = address1T.get, address1Tree, 1, 3)
            ) { infoTupleList =>
              infoTupleList.foreach { case (header, out, tx, _, token, asset) =>
                headerRepo.insert(header).runWithIO()
                oRepo.insert(out).runWithIO()
                txRepo.insert(tx).runWithIO()
                tokenRepo.insert(token).runWithIO()
                assetRepo.insert(asset).runWithIO()
              }
              forSingleInstance(
                balanceOfAddressWithTokenGen(mainChain = true, address = address2T.get, address2Tree, 1, 3)
              ) { infoTupleList2 =>
                infoTupleList.foreach { case (header, out, tx, _, token, asset) =>
                  headerRepo.insert(header).runWithIO()
                  oRepo.insert(out).runWithIO()
                  txRepo.insert(tx).runWithIO()
                  tokenRepo.insert(token).runWithIO()
                  assetRepo.insert(asset).runWithIO()
                }

                infoTupleList2.foreach { case (header, out, tx, _, token, asset) =>
                  headerRepo.insert(header).runWithIO()
                  oRepo.insert(out).runWithIO()
                  txRepo.insert(tx).runWithIO()
                  tokenRepo.insert(token).runWithIO()
                  assetRepo.insert(asset).runWithIO()
                }
                // batch addressInfo Data:
                val batchInfoResult =
                  addr
                    .addressInfoOf(List(address1T, address2T).map(_.get))
                    .unsafeRunSync()

                batchInfoResult should not be empty
                batchInfoResult.contains(address1T.get) should be(true)
                batchInfoResult.contains(address2T.get) should be(true)

                // batchInfoResult(address1)
                val b1FS         = addr.confirmedBalanceOf(address1T.get, 0).unsafeRunSync()
                val hu1FS        = memprop.hasUnconfirmedBalance(ErgoTree(address1Tree)).unsafeRunSync()
                val hbu1FS       = (addr invokePrivate hasBeenUsedByErgoTree(address1Tree)).unsafeRunSync()
                val AddressInfo1 = AddressInfo(address = address1T.get, hasUnconfirmedTxs = hu1FS, hbu1FS, b1FS)

                // batchInfoResult(address2)
                val b2FS         = addr.confirmedBalanceOf(address2T.get, 0).unsafeRunSync()
                val hu2FS        = memprop.hasUnconfirmedBalance(ErgoTree(address2Tree)).unsafeRunSync()
                val hbu2FS       = (addr invokePrivate hasBeenUsedByErgoTree(address2Tree)).unsafeRunSync()
                val AddressInfo2 = AddressInfo(address = address2T.get, hasUnconfirmedTxs = hu2FS, hbu2FS, b2FS)

                batchInfoResult.get(address1T.get) should be(AddressInfo1.some)
                batchInfoResult.get(address2T.get) should be(AddressInfo2.some)

              }
            }
          }
        }
      }
      .unsafeRunSync()

  }

}

object AddressesSpec {

  import cats.effect.Sync
  import scala.concurrent.duration._
  import cats.syntax.traverse._
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def withResources[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift](port: Int) = for {
    redis <- Some(RedisSettings(s"redis://localhost:$port")).map(Redis[F]).sequence
  } yield (ServiceSettings(chunkSize = 100), UtxCacheSettings(transactionTtl = 10.minute), redis)

  def withServices[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift, D[
    _
  ]: LiftConnectionIO: CompileStream: Sync](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(
    body: (Addresses[F], Mempool[F], MempoolProps[F, D]) => Any
  )(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      memprops  <- MempoolProps(settings, utxCacheSettings, redis)(trans)
      addresses <- Addresses[F, D](settings, memprops)(trans)
      mempool <- Mempool[F, D](
                   settings,
                   utxCacheSettings,
                   redis,
                   memprops
                 )(trans)
      _ = body(addresses, mempool, memprops)
    } yield ()

  def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D, fs2.Stream],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      InputRepo[D],
      TokenRepo[D],
      AssetRepo[D, fs2.Stream]
    ) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.InputRepo[IO, D].unsafeRunSync(),
      repositories.TokenRepo[IO, D].unsafeRunSync(),
      repositories.AssetRepo[IO, D].unsafeRunSync()
    )
}
