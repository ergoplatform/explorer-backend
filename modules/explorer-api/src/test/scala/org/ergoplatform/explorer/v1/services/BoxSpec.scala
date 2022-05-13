package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import dev.profunktor.redis4cats.algebra.RedisCommands
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services.{Boxes, Mempool}
import org.ergoplatform.explorer.settings.{RedisSettings, ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.testContainers.RedisTest
import org.scalatest.{PrivateMethodTester, TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try
import tofu.syntax.monadic._

import org.ergoplatform.explorer.v1.services.BoxSpec._
import org.ergoplatform.explorer.db.models.generators._

trait BoxSpec
  extends AnyFlatSpec
  with should.Matchers
  with TryValues
  with PrivateMethodTester
  with RealDbTest
  with RedisTest

class BS_A extends BoxSpec {
  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Box Service" should "get unspent outputs with exclusions" in {}

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

  def withResources[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift](port: Int) = for {
    redis <- Some(RedisSettings(s"redis://localhost:$port")).map(Redis[F]).sequence
  } yield (ServiceSettings(chunkSize = 100), UtxCacheSettings(transactionTtl = 10.minute), redis)

  def withServices[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift, D[
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

  def withLiveRepos[D[_]: LiftConnectionIO: Sync](
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
