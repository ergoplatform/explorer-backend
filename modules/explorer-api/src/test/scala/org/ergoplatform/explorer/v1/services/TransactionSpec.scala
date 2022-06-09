package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.IO
import cats.syntax.option._
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.Address
import org.ergoplatform.explorer.commonGenerators.forSingleInstance
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.generators.`headerTxsOutputs&InputGen`
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.db.repositories.{HeaderRepo, InputRepo, OutputRepo, TransactionRepo}
import org.ergoplatform.explorer.http.api.models.{InclusionHeightRange, Paging}
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
  import TransactionSpec.{withLiveRepos, withTransactionService}

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Transactions Services" should "get transactions by address between inclusion-height range" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val addressS                                = "3WzSdM7NrjDJswpu2ThfhWvVM1mKJhgnGNieWYcGVsYp3AoirgR5"
    val addressT                                = Address.fromString[Try](addressS)
    val addressTree                             = sigma.addressToErgoTreeHex(addressT.get)
    withTransactionService[IO, ConnectionIO] { txService =>
      addressT.isSuccess should be(true)
      withLiveRepos[ConnectionIO] { (headerRepo, txRepo, outputRepo, inputRepo) =>
        forSingleInstance(`headerTxsOutputs&InputGen`(mainChain = true, 10, 20, addressT.get, addressTree)) { hTxList =>
          hTxList.foreach { case (header, tx, out, in) =>
            headerRepo.insert(header).runWithIO()
            txRepo.insert(tx).runWithIO()
            outputRepo.insert(out).runWithIO()
            inputRepo.insert(in).runWithIO()
          }

          val txByAddr = txService
            .getByAddress(
              addressT.get,
              Paging(0, Int.MaxValue),
              concise = false,
              InclusionHeightRange(fromHeight = 10, toHeight = 20).some
            )
            .unsafeRunSync()

          txByAddr.items.map(_.id) should contain theSameElementsAs hTxList.map(_._2.id)
          txByAddr.total should be(11)
        }
      }
    }.unsafeRunSync()
  }
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
    body: (HeaderRepo[D, fs2.Stream], TransactionRepo[D, fs2.Stream], OutputRepo[D, fs2.Stream], InputRepo[D]) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.InputRepo[IO, D].unsafeRunSync()
    )
}
