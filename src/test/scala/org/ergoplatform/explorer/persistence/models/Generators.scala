package org.ergoplatform.explorer.persistence.models

import eu.timepit.refined._
import io.estatico.newtype.ops._
import org.ergoplatform.explorer._
import org.scalacheck.Gen
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16

object Generators {

  def hexStringGen: Gen[String] =
    Gen
      .nonEmptyListOf(Gen.alphaNumChar)
      .map(x => Base16.encode(Blake2b256.hash(x.mkString)))

  def hexStringRGen: Gen[HexString] =
    hexStringGen.map(x => refineV[HexStringP](x).right.get)

  def idGen: Gen[Id] =
    hexStringGen.map(_.coerce[Id])

  def txIdGen: Gen[TxId] =
    hexStringGen.map(_.coerce[TxId])

  def boxIdGen: Gen[BoxId] =
    hexStringGen.map(_.coerce[BoxId])

  def assetIdGen: Gen[AssetId] =
    hexStringGen.map(_.coerce[AssetId])

  def adProofGen: Gen[AdProof] =
    for {
      headerId <- idGen
      proof    <- hexStringRGen
      digest   <- hexStringRGen
    } yield AdProof(headerId, proof, digest)

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
      txs    <- Gen
              .nonEmptyListOf(transactionGen)
              .map(_.map(_.copy(headerId = header.id)))
    } yield (header, txs)

  def withSingleInstance[T](gen: Gen[T])(test: T => Any): Any =
    gen.sample.foreach(test)
}
