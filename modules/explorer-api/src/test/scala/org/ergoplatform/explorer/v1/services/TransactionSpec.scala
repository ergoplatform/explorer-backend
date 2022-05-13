package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.IO
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.commonGenerators.forSingleInstance
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.repositories.{HeaderRepo, InputRepo, OutputRepo, TransactionRepo}
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.services.Transactions
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.settings.ServiceSettings
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import tofu.syntax.monadic._

import scala.util.Try

class TransactionSpec extends AnyFlatSpec with should.Matchers with TryValues with RealDbTest {

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)
}

object TransactionSpec {

  import cats.effect.Sync

  private def withTransactionService[F[_]: Sync: Monad: Parallel, D[_]: LiftConnectionIO: CompileStream: Sync](
    body: Transactions[F] => Any
  )(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      txs <- Transactions(ServiceSettings(chunkSize = 100))(trans)
      _ = body(txs)
    } yield ()

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (HeaderRepo[D], TransactionRepo[D, fs2.Stream], OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.InputRepo[IO, D].unsafeRunSync()
    )
}
