package org.ergoplatform.explorer.db.repositories

import cats.effect.{IO, Sync}
import cats.instances.try_._
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.auto._
import org.ergoplatform.explorer.{Address, HexString, TokenId, TxId}
import org.ergoplatform.explorer.constraints.OrderingString
import org.ergoplatform.explorer.db.models.{Header, Output, Transaction}
import org.scalacheck.Gen
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.models.aggregates.ExtendedOutput
import org.ergoplatform.explorer.db.{repositories, RealDbTest}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._

import org.scalatest._
import flatspec._
import matchers._

import scala.util.Try

class OutputRepoSpec extends AnyFlatSpec with should.Matchers with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._
  import org.ergoplatform.explorer.db.models.generators._

  "OutputRepo" should "insert/getByBoxId" in {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _, _) =>
      forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) { case (header, tx, outputs) =>
        hRepo.insert(header).runWithIO()
        txRepo.insert(tx).runWithIO()
        outputs.foreach { extOut =>
          oRepo.getByBoxId(extOut.output.boxId).runWithIO() should be(None)
          oRepo.insert(extOut.output).runWithIO()
          oRepo.getByBoxId(extOut.output.boxId).runWithIO() should be(Some(extOut))
        }
      }
    }
  }

  it should "getAllByAddress/getAllByErgoTree" in {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _, _) =>
      forSingleInstance(hexStringRGen.flatMap(hex => addressGen.map(_ -> hex))) { case (address, ergoTree) =>
        forSingleInstance(extOutputsWithTxWithHeaderGen(mainChain = true)) { case (header, tx, outputs) =>
          hRepo.insert(header).runWithIO()
          txRepo.insert(tx).runWithIO()
          val nonMatching = outputs.head
          val matching = outputs.tail
            .map { extOut =>
              extOut.copy(
                output = extOut.output.copy(address = address, ergoTree = ergoTree)
              )
            }
          matching.foreach { extOut =>
            oRepo.insert(extOut.output).runWithIO()
          }
          oRepo.insert(nonMatching.output).runWithIO()
          val asc: OrderingString  = "asc"
          val desc: OrderingString = "desc"
          oRepo
            .streamAllByErgoTree(ergoTree, 0, Int.MaxValue, asc)
            .compile
            .toList
            .runWithIO() should contain theSameElementsAs matching
          oRepo
            .streamAllByErgoTree(ergoTree, 0, Int.MaxValue, desc)
            .compile
            .toList
            .runWithIO() should contain theSameElementsAs matching
        }
      }
    }
  }

  it should "order outputs by globalIndex asc and desc deterministically across pages" in {
    withLiveRepos[ConnectionIO] { (hRepo, txRepo, oRepo, _, inRepo) =>
      forSingleInstance(
        for {
          addr1 <- addressGen
          tree1 <- hexStringRGen
          addr2 <- addressGen
          tree2 <- hexStringRGen
        } yield (addr1, tree1, addr2, tree2)
      ) { case (address, ergoTree, otherAddr, otherTree) =>
        def genRecord(mc: Boolean, addr: Address, tree: HexString, gix: Long, h: Int) =
          for {
            header <- headerGen.map(_.copy(mainChain = mc))
            out <- outputGen(mc).map(
                     _.copy(headerId = header.id, address = addr, ergoTree = tree, globalIndex = gix)
                   )
            tx <- transactionGen(mc, out.txId, h, header.id)
          } yield (header, out, tx)

        val scrambledGix = List(50L, 10L, 90L, 30L, 70L, 20L, 100L, 40L, 80L, 60L)
        val listGen = Gen.sequence[List[(Header, Output, Transaction)], (Header, Output, Transaction)](
          scrambledGix.zipWithIndex.map { case (gix, idx) => genRecord(mc = true, address, ergoTree, gix, idx + 1) }
        )

        forSingleInstance(listGen) { tuples =>
          val spentGixSet  = Set(20L, 50L, 90L)
          val spendTxByGix = scala.collection.mutable.Map.empty[Long, TxId]
          tuples.foreach { case (header, out, tx) =>
            val gix = out.globalIndex
            hRepo.insert(header).runWithIO()
            txRepo.insert(tx).runWithIO()
            oRepo.insert(out).runWithIO()

            if (spentGixSet.contains(gix)) {
              forSingleInstance(inputGen(mainChain = true, out.boxId)) { spendIn =>
                val inOnHeader = spendIn.copy(headerId = header.id)
                inRepo.insert(inOnHeader).runWithIO()
                spendTxByGix.put(gix, inOnHeader.txId)
              }
            } else if (gix == 40L) {
              // Orphaned / forked spending input (mainChain = false) must not mark output as spent or duplicate row
              forSingleInstance(inputGen(mainChain = false, out.boxId)) { forkedIn =>
                inRepo.insert(forkedIn).runWithIO()
              }
            }
          }

          // Insert a non-main-chain output for the same ergoTree and a main-chain output for a different ergoTree
          forSingleInstance(genRecord(mc = false, address, ergoTree, 5L, 99)) {
            case (forkHeader, forkOut, forkTx) =>
              hRepo.insert(forkHeader).runWithIO()
              txRepo.insert(forkTx).runWithIO()
              oRepo.insert(forkOut).runWithIO()
          }
          forSingleInstance(genRecord(mc = true, otherAddr, otherTree, 55L, 98)) {
            case (otherHeader, otherOut, otherTx) =>
              hRepo.insert(otherHeader).runWithIO()
              txRepo.insert(otherTx).runWithIO()
              oRepo.insert(otherOut).runWithIO()
          }

          val asc: OrderingString  = "asc"
          val desc: OrderingString = "desc"
          val pageLimit            = 4
          val offsets              = List(0, 4, 8)

          val ascPages = offsets.map { off =>
            oRepo.streamAllByErgoTree(ergoTree, off, pageLimit, asc).compile.toList.runWithIO()
          }
          val descPages = offsets.map { off =>
            oRepo.streamAllByErgoTree(ergoTree, off, pageLimit, desc).compile.toList.runWithIO()
          }

          ascPages.map(_.map(_.output.globalIndex)) shouldBe List(
            List(10L, 20L, 30L, 40L),
            List(50L, 60L, 70L, 80L),
            List(90L, 100L)
          )
          descPages.map(_.map(_.output.globalIndex)) shouldBe List(
            List(100L, 90L, 80L, 70L),
            List(60L, 50L, 40L, 30L),
            List(20L, 10L)
          )

          val ascAll  = ascPages.flatten
          val descAll = descPages.flatten
          ascAll.map(_.output.boxId).distinct.size shouldBe 10
          descAll shouldBe ascAll.reverse

          ascAll.foreach { extOut =>
            extOut.spentByOpt shouldBe spendTxByGix.get(extOut.output.globalIndex)
          }
        }
      }
    }
  }

  private def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D, fs2.Stream],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      AssetRepo[D, fs2.Stream],
      InputRepo[D]
    ) => Any
  ): Any = {
    val headerRepo = repositories.HeaderRepo[IO, D].unsafeRunSync()
    val txRepo     = repositories.TransactionRepo[IO, D].unsafeRunSync()
    val outRepo    = repositories.OutputRepo[IO, D].unsafeRunSync()
    val assetRepo  = repositories.AssetRepo[IO, D].unsafeRunSync()
    val inputRepo  = repositories.InputRepo[IO, D].unsafeRunSync()
    body(headerRepo, txRepo, outRepo, assetRepo, inputRepo)
  }
}
