package org.ergoplatform.explorer.persistence.models

import cats.syntax.option._
import eu.timepit.refined._
import io.circe.Json
import io.estatico.newtype.ops._
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.persistence.models.composite.{
  ExtendedInput,
  ExtendedOutput
}
import org.scalacheck.Gen
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.{Base16, Base58}

object Generators {

  def hexStringGen: Gen[String] =
    Gen
      .nonEmptyListOf(Gen.alphaNumChar)
      .map(x => Base16.encode(Blake2b256.hash(x.mkString)))

  def hexStringRGen: Gen[HexString] =
    hexStringGen.map(x => refineV[HexStringP](x).right.get)

  def addressGen: Gen[Address] =
    Gen
      .nonEmptyListOf(Gen.alphaNumChar)
      .map(x => Base58.encode(Blake2b256.hash(x.mkString)))
      .map(x => refineV[Base58StringP](x).right.get)

  def registersGen: Gen[Json] =
    Gen.oneOf(
      List(
        Json.obj(
          "0x00" -> Json.fromString("fa209e"),
          "0x01" -> Json.fromString("871b"),
          "0x02" -> Json.fromString("6ac2e1c9")
        ),
        Json.Null
      )
    )

  def idGen: Gen[Id] =
    hexStringGen.map(_.coerce[Id])

  def txIdGen: Gen[TxId] =
    hexStringGen.map(_.coerce[TxId])

  def boxIdGen: Gen[BoxId] =
    hexStringGen.map(_.coerce[BoxId])

  def assetIdGen: Gen[AssetId] =
    hexStringGen.map(_.coerce[AssetId])

  def headerGen: Gen[Header] =
    for {
      id            <- idGen
      parentId      <- idGen
      version       <- Gen.posNum[Short]
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

  def outputGen: Gen[Output] =
    for {
      boxId   <- boxIdGen
      txId    <- txIdGen
      value   <- Gen.posNum[Long]
      height  <- Gen.posNum[Int]
      idx     <- Gen.posNum[Int]
      tree    <- hexStringRGen
      address <- addressGen
      regs    <- registersGen
      ts      <- Gen.posNum[Long]
    } yield Output(boxId, txId, value, height, idx, tree, address, regs, ts)

  def extOutputsWithTxWithHeaderGen(
    mainChain: Boolean
  ): Gen[(Header, Transaction, List[ExtendedOutput])] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      tx     <- transactionGen.map(_.copy(headerId = header.id))
      outs   <- Gen.nonEmptyListOf(outputGen).map(_.map(_.copy(txId = tx.id)))
      extOuts = outs.map(o => ExtendedOutput(o, None, mainChain))
    } yield (header, tx, extOuts)

  def inputGen: Gen[Input] =
    for {
      boxId <- boxIdGen
      txId  <- txIdGen
      proof <- hexStringRGen
      ext   <- registersGen
    } yield Input(boxId, txId, proof, ext)

  def extInputWithOutputGen: Gen[(Output, ExtendedInput)] =
    outputGen.flatMap { out =>
      inputGen.map { in =>
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

  @scala.annotation.tailrec
  def withSingleInstance[T](gen: Gen[T])(test: T => Any): Any =
    gen.sample match {
      case Some(sample) =>
        test(sample)
      case None =>
        withSingleInstance(gen)(test)
    }
}
