package org.ergoplatform.explorer.db.models

import cats.syntax.option._
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.composite.{ExtendedInput, ExtendedOutput}
import org.scalacheck.Gen

object generators {

  import commonGenerators._

  def headerGen: Gen[Header] =
    for {
      id            <- idGen
      parentId      <- idGen
      version       <- Gen.posNum[Byte]
      height        <- Gen.posNum[Int]
      nBits         <- Gen.posNum[Long]
      diff          <- Gen.posNum[Long]
      ts            <- Gen.posNum[Long]
      stateRoot     <- hexStringRGen
      adProofsRoot  <- hexStringRGen
      extensionHash <- hexStringRGen
      txsRoot       <- hexStringRGen
      minerPk       <- hexStringRGen
      w             <- hexStringRGen
      n             <- hexStringRGen
      d             <- Gen.posNum[Double].map(_.toString)
      votes         <- hexStringGen
      mainChain     <- Gen.oneOf(List(true, false))
    } yield
      Header(
        id,
        parentId,
        version,
        height,
        nBits,
        diff,
        ts,
        stateRoot,
        adProofsRoot,
        extensionHash,
        txsRoot,
        minerPk,
        w,
        n,
        d,
        votes,
        mainChain
      )

  def adProofGen: Gen[AdProof] =
    for {
      headerId <- idGen
      proof    <- hexStringRGen
      digest   <- hexStringRGen
    } yield AdProof(headerId, proof, digest)

  def adProofWithHeaderGen: Gen[(Header, AdProof)] =
    headerGen.flatMap { header =>
      adProofGen.map(x => header -> x.copy(headerId = header.id))
    }

  def blockExtensionGen: Gen[BlockExtension] =
    for {
      headerId <- idGen
      digest   <- hexStringRGen
      fields   <- jsonFieldsGen
    } yield BlockExtension(headerId, digest, fields)

  def blockExtensionWithHeaderGen: Gen[(Header, BlockExtension)] =
    headerGen.flatMap { header =>
      blockExtensionGen.map(x => header -> x.copy(headerId = header.id))
    }

  def transactionGen: Gen[Transaction] =
    for {
      id       <- txIdGen
      headerId <- idGen
      coinbase <- Gen.oneOf(true, false)
      ts       <- Gen.posNum[Long]
      size     <- Gen.posNum[Int]
    } yield Transaction(id, headerId, coinbase, ts, size)

  def headerWithTxsGen(mainChain: Boolean): Gen[(Header, List[Transaction])] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      txs <- Gen
              .nonEmptyListOf(transactionGen)
              .map(_.map(_.copy(headerId = header.id)))
    } yield (header, txs)

  def outputGen(mainChain: Boolean): Gen[Output] =
    for {
      boxId   <- boxIdGen
      txId    <- txIdGen
      value   <- Gen.posNum[Long]
      height  <- Gen.posNum[Int]
      idx     <- Gen.posNum[Int]
      tree    <- hexStringRGen
      address <- addressGen
      regs    <- jsonFieldsGen
      ts      <- Gen.posNum[Long]
    } yield Output(boxId, txId, value, height, idx, tree, address, regs, ts, mainChain)

  def extOutputsWithTxWithHeaderGen(
    mainChain: Boolean
  ): Gen[(Header, Transaction, List[ExtendedOutput])] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      tx     <- transactionGen.map(_.copy(headerId = header.id))
      outs   <- Gen.nonEmptyListOf(outputGen(mainChain)).map(_.map(_.copy(txId = tx.id)))
      extOuts = outs.map(o => ExtendedOutput(o, None))
    } yield (header, tx, extOuts)

  def inputGen(mainChain: Boolean = true): Gen[Input] =
    for {
      boxId <- boxIdGen
      txId  <- txIdGen
      proof <- hexStringRGen
      ext   <- jsonFieldsGen
    } yield Input(boxId, txId, proof, ext, mainChain)

  def extInputWithOutputGen(mainChain: Boolean = true): Gen[(Output, ExtendedInput)] =
    outputGen(mainChain).flatMap { out =>
      inputGen(mainChain).map { in =>
        val inModified = in.copy(boxId = out.boxId)
        val extIn =
          ExtendedInput(inModified, out.value.some, out.txId.some, out.address.some)
        out -> extIn
      }
    }

  def assetGen: Gen[Asset] =
    for {
      id    <- assetIdGen
      boxId <- boxIdGen
      amt   <- Gen.posNum[Long]
    } yield Asset(id, boxId, amt)

  def assetsWithBoxIdGen: Gen[(BoxId, List[Asset])] =
    boxIdGen.flatMap { boxId =>
      Gen.nonEmptyListOf(assetGen).map(x => boxId -> x.map(_.copy(boxId = boxId)))
    }

  def transactionWithInputsGen(
    mainChain: Boolean
  ): Gen[(Transaction, List[Input])] =
    for {
      tx  <- transactionGen
      ins <- Gen.nonEmptyListOf(inputGen(mainChain)).map(_.map(_.copy(txId = tx.id)))
    } yield tx -> ins

  def transactionWithOutputsGen(
    mainChain: Boolean
  ): Gen[(Transaction, List[Output])] =
    for {
      tx   <- transactionGen
      outs <- Gen.nonEmptyListOf(outputGen(mainChain)).map(_.map(_.copy(txId = tx.id)))
    } yield tx -> outs

  def fullBlockGen(
    mainChain: Boolean
  ): Gen[(Header, List[Transaction], List[Input], List[Output])] =
    for {
      h <- headerGen.map(_.copy(mainChain = mainChain))
      (txsIn, inputs) <- Gen.listOfN(2, transactionWithInputsGen(mainChain)).map {
                          _.foldLeft((List.empty[Transaction], List.empty[Input])) {
                            case ((txsAcc, insAcc), (tx, ins)) =>
                              (tx.copy(headerId = h.id) :: txsAcc) -> (insAcc ++ ins)
                          }
                        }
      (txsOut, outs) <- Gen.listOfN(2, transactionWithOutputsGen(mainChain)).map {
                         _.foldLeft((List.empty[Transaction], List.empty[Output])) {
                           case ((txsAcc, outsAcc), (tx, out)) =>
                             (tx.copy(headerId = h.id) :: txsAcc) -> (outsAcc ++ out)
                         }
                       }
    } yield (h, txsIn ++ txsOut, inputs, outs)
}
