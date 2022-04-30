package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{IO, Sync}
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
import org.ergoplatform.explorer.http.api.v1.services.Addresses
import org.ergoplatform.explorer.protocol.sigma
import org.ergoplatform.explorer.v1.services.constants._
import org.scalatest.TryValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import tofu.syntax.monadic._

import scala.util.Try

class AddressesSpec extends AnyFlatSpec with should.Matchers with TryValues with RealDbTest {
  import org.ergoplatform.explorer.v1.services.AddressesSpec._
  import org.ergoplatform.explorer.db.models.generators._

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Address Service" should "get confirmed Balance (nanoErgs) of address" in {
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    withAddressService[IO, ConnectionIO] { addr =>
      val testAddress1 = Address.fromString[Try](SenderAddressString)
      testAddress1.isSuccess should be(true)
      val testAddress1Tree = sigma.addressToErgoTreeHex(testAddress1.get)
      // create outputs & create transactions from output.boxId
      val boxValues = List((100.toNanoErgo, 1), (200.toNanoErgo, 1))
      withLiveRepos[ConnectionIO] { (headerRepo, txRepo, oRepo, _) =>
        forSingleInstance(
          balanceOfAddressGen(
            mainChain = true,
            address   = testAddress1.get,
            testAddress1Tree,
            values = boxValues
          )
        ) { infoTupleList =>
          infoTupleList.foreach { case (header, out, tx) =>
            headerRepo.insert(header).runWithIO()
            oRepo.insert(out).runWithIO()
            txRepo.insert(tx).runWithIO()
          }
          val balance = addr.confirmedBalanceOf(testAddress1.get, 0).unsafeRunSync().nanoErgs
          println(balance)
          balance should be(300.toNanoErgo)
        }
      }

    }.unsafeRunSync()
  }
}

object AddressesSpec {

  private def withAddressService[F[_]: Sync: Monad: Parallel, D[_]: LiftConnectionIO: Sync](
    body: Addresses[F] => Any
  )(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      addresses <- Addresses[F, D](trans)
      _ = body(addresses)
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
