package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import dev.profunktor.redis4cats.algebra.RedisCommands
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
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
import org.ergoplatform.explorer.v1.services.AddressesSpec._
import org.ergoplatform.explorer.db.models.generators._
import org.ergoplatform.explorer.http.api.v1.shared.MempoolProps

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
  )(body: (Addresses[F], Mempool[F]) => Any)(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      memprops  <- MempoolProps(settings, utxCacheSettings, redis)(trans)
      addresses <- Addresses[F, D](memprops)(trans)
      mempool <- Mempool[F, D](
                   settings,
                   utxCacheSettings,
                   redis,
                   memprops
                 )(trans)
      _ = body(addresses, mempool)
    } yield ()

  def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D],
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
