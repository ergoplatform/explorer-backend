package org.ergoplatform.explorer.db.models

import cats.instances.try_._
import cats.syntax.option._
import io.estatico.newtype.ops._
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedInput, ExtendedOutput}
import org.ergoplatform.explorer.protocol.sigma
import org.scalacheck.Gen

import scala.util.Try

object generators {

  import commonGenerators._

  implicit class IntToNanoErgo(val value: Int) extends AnyVal {
    def toNanoErgo: Long = value.toLong * 1000000000L
  }

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
    } yield Header(
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

  def transactionGen(mainChain: Boolean): Gen[Transaction] =
    for {
      id       <- txIdGen
      headerId <- idGen
      height   <- Gen.posNum[Int]
      coinbase <- Gen.oneOf(true, false)
      ts       <- Gen.posNum[Long]
      size     <- Gen.posNum[Int]
      index    <- Gen.posNum[Int]
      gix      <- Gen.posNum[Long]
    } yield Transaction(id, headerId, height, coinbase, ts, size, index, gix, mainChain)

  def `headerTxsOutputs&InputGen`(
    mainChain: Boolean,
    from: Int,
    to: Int,
    address: Address,
    tree: HexString
  ): Gen[List[(Header, Transaction, Output, Input)]] =
    Gen.sequence[List[(Header, Transaction, Output, Input)], (Header, Transaction, Output, Input)](
      Range.inclusive(from, to).toList.map { inH =>
        for {
          header <- headerGen.map(_.copy(mainChain = mainChain))
          tx     <- transactionGen(mainChain).map(_.copy(inclusionHeight = inH, headerId = header.id))
          out <-
            outputGen(mainChain).map(_.copy(headerId = header.id, ergoTree = tree, address = address, txId = tx.id))
          in <- inputGen(mainChain).map(_.copy(txId = tx.id, headerId = header.id, boxId = out.boxId))
        } yield (header, tx, out, in)
      }
    )

  def transactionGen(mainChain: Boolean, txId: TxId, height: Int, headerId: BlockId): Gen[Transaction] =
    for {
      coinbase <- Gen.oneOf(true, false)
      ts       <- Gen.posNum[Long]
      size     <- Gen.posNum[Int]
      index    <- Gen.posNum[Int]
      gix      <- Gen.posNum[Long]
    } yield Transaction(txId, headerId, height, coinbase, ts, size, index, gix, mainChain)

  def headerWithTxsGen(mainChain: Boolean): Gen[(Header, List[Transaction])] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      txs <- Gen
               .nonEmptyListOf(transactionGen(mainChain = mainChain))
               .map(_.map(_.copy(headerId = header.id)))
    } yield (header, txs)

  def outputGen(mainChain: Boolean, ergoTreeGen: Gen[HexString]): Gen[Output] =
    for {
      boxId    <- boxIdGen
      txId     <- txIdGen
      headerId <- idGen
      value    <- Gen.posNum[Long]
      height   <- Gen.posNum[Int]
      idx      <- Gen.posNum[Int]
      gix      <- Gen.posNum[Long]
      tree     <- ergoTreeGen
      template = sigma.deriveErgoTreeTemplateHash[Try](tree).get
      address <- addressGen
      regs    <- jsonFieldsGen
      ts      <- Gen.posNum[Long]
    } yield Output(
      boxId,
      txId,
      headerId,
      value,
      height,
      height,
      idx,
      gix,
      tree,
      template,
      address,
      regs,
      ts,
      mainChain
    )

  def outputGen(mainChain: Boolean): Gen[Output] = outputGen(mainChain, sellOrderErgoTree)

  def outputGen(mainChain: Boolean, address: Address, tree: HexString, value: Long): Gen[Output] = for {
    boxId    <- boxIdGen
    txId     <- txIdGen
    headerId <- idGen
    height   <- Gen.posNum[Int]
    idx      <- Gen.posNum[Int]
    gix      <- Gen.posNum[Long]
    template = sigma.deriveErgoTreeTemplateHash[Try](tree).get
    regs <- jsonFieldsGen
    ts   <- Gen.posNum[Long]
  } yield Output(
    boxId,
    txId,
    headerId,
    value,
    height,
    height,
    idx,
    gix,
    tree,
    template,
    address,
    regs,
    ts,
    mainChain
  )

  def outputGen(mainChain: Boolean, address: Address, tree: HexString, values: List[Long]): Gen[List[Output]] =
    Gen.sequence[List[Output], Output](values.map(outputGen(mainChain, address, tree, _)))

  // create output with value and corresponding transaction @height
  def balanceOfAddressGen(
    mainChain: Boolean,
    address: Address,
    tree: HexString,
    value: Long,
    height: Int
  ): Gen[(Header, Output, Transaction)] =
    for {
      header      <- headerGen.map(_.copy(mainChain = mainChain))
      output      <- outputGen(mainChain, address, tree, value).map(_.copy(headerId = header.id))
      transaction <- transactionGen(mainChain, output.txId, height, header.id)
    } yield (header, output, transaction)

  def balanceOfAddressGen(
    mainChain: Boolean,
    address: Address,
    tree: HexString,
    values: List[(Long, Int)] // Value(Ergo), Height
  ): Gen[List[(Header, Output, Transaction)]] =
    Gen.sequence[List[(Header, Output, Transaction)], (Header, Output, Transaction)](values.map {
      case (value, height) =>
        balanceOfAddressGen(mainChain, address, tree, value, height)
    })

  def balanceOfAddressWithTokenGen(
    mainChain: Boolean,
    address: Address,
    tree: HexString,
    height: Int
  ): Gen[(Header, Output, Transaction, Input, Token, Asset)] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      out    <- outputGen(mainChain).map(_.copy(headerId = header.id, address = address, ergoTree = tree))
      tx     <- transactionGen(mainChain, out.txId, height, header.id)
      posIn  <- inputGen(mainChain).map(_.copy(txId = tx.id, headerId = header.id, boxId = out.boxId))
      token  <- tokenGen
      asset  <- assetGen.map(_.copy(boxId = out.boxId, tokenId = token.id))
    } yield (header, out, tx, posIn, token, asset)

  def balanceOfAddressWithTokenGen(
    mainChain: Boolean,
    address: Address,
    tree: HexString,
    height: Int,
    number: Int
  ): Gen[List[(Header, Output, Transaction, Input, Token, Asset)]] =
    Gen.listOfN(number, balanceOfAddressWithTokenGen(mainChain, address, tree, height))

  def extOutputsWithTxWithHeaderGen(
    mainChain: Boolean
  ): Gen[(Header, Transaction, List[ExtendedOutput])] =
    for {
      header <- headerGen.map(_.copy(mainChain = mainChain))
      tx     <- transactionGen(mainChain = mainChain).map(_.copy(headerId = header.id))
      outs   <- Gen.nonEmptyListOf(outputGen(mainChain)).map(_.map(_.copy(txId = tx.id)))
      extOuts = outs.map(o => ExtendedOutput(o, None))
    } yield (header, tx, extOuts)

  def inputGen(mainChain: Boolean = true): Gen[Input] =
    for {
      boxId    <- boxIdGen
      txId     <- txIdGen
      headerId <- idGen
      index    <- Gen.posNum[Int]
      proof    <- hexStringRGen
      ext      <- jsonFieldsGen
    } yield Input(boxId, txId, headerId, proof.some, ext, index, mainChain)

  def inputGen(mainChain: Boolean, boxId: BoxId): Gen[Input] =
    for {
      txId     <- txIdGen
      headerId <- idGen
      index    <- Gen.posNum[Int]
      proof    <- hexStringRGen
      ext      <- jsonFieldsGen
    } yield Input(boxId, txId, headerId, proof.some, ext, index, mainChain)

  def extInputWithOutputGen(mainChain: Boolean = true): Gen[(Output, ExtendedInput)] =
    outputGen(mainChain).flatMap { out =>
      inputGen(mainChain).map { in =>
        val inModified = in.copy(boxId = out.boxId)
        val extIn =
          ExtendedInput(inModified, out.value.some, out.txId.some, out.index.some, out.address.some)
        out -> extIn
      }
    }

  def assetGen: Gen[Asset] =
    for {
      id       <- assetIdGen
      boxId    <- boxIdGen
      headerId <- idGen
      index    <- Gen.posNum[Int]
      amt      <- Gen.posNum[Long]
    } yield Asset(id, boxId, headerId, index, amt)

  def tokenGen: Gen[Token] =
    for {
      id             <- assetIdGen
      boxId          <- boxIdGen
      emissionAmount <- Gen.posNum[Long]
      name           <- Gen.option(Gen.alphaLowerStr)
      description    <- Gen.const[Option[String]](None)
      t              <- Gen.option(Gen.oneOf[TokenType](List(TokenType("EIP-004"), TokenType("EIP-0021"))))
      decimals       <- Gen.const[Option[Int]](Some(2))
    } yield Token(id, boxId, emissionAmount, name, description, t, decimals)

  def assetsWithBoxIdGen: Gen[(BoxId, List[Asset])] =
    boxIdGen.flatMap { boxId =>
      Gen.nonEmptyListOf(assetGen).map(x => boxId -> x.map(_.copy(boxId = boxId)))
    }

  def transactionWithInputsGen(
    mainChain: Boolean
  ): Gen[(Transaction, List[Input])] =
    for {
      tx  <- transactionGen(mainChain)
      ins <- Gen.nonEmptyListOf(inputGen(mainChain)).map(_.map(_.copy(txId = tx.id)))
    } yield tx -> ins

  def transactionWithOutputsGen(
    mainChain: Boolean
  ): Gen[(Transaction, List[Output])] =
    for {
      tx   <- transactionGen(mainChain)
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

  def issueTokenGen: Gen[(Input, Output, Asset)] =
    for {
      out   <- outputGen(true)
      token <- assetGen.map(_.copy(boxId = out.boxId))
      input <- inputGen(mainChain = true).map(
                 _.copy(txId = out.txId, boxId = token.tokenId.toString.coerce[BoxId])
               )
    } yield (input, out, token)

  def issueTokensGen(num: Int): Gen[List[(Input, Output, Asset)]] =
    Gen.listOfN(num, issueTokenGen)

  /**  from [[http://github.com/aslesarenko/ergo-tool/blob/3b948e527a816e51acd4d85d99595cc93d735a59/src/test/resources/mockwebserver/node_responses/response_Box_AAE_seller_contract.json#L4-L4 response_Box_AAE_seller_contract.json#L4-L4]]<br />
    * which was generated with [[http://github.com/aslesarenko/ergo-tool/blob/3b948e527a816e51acd4d85d99595cc93d735a59/src/main/scala-2.12/org/ergoplatform/appkit/ergotool/dex/CreateSellOrderCmd.scala#L58-L58 CreateSellOrderCmd.scala#L58-L58]]
    */
  val sellOrderErgoTree: HexString = HexString
    .fromString[Try](
      "100808cd036ba5cfbc03ea2471fdf02737f64dbcd58c34461a7ec1e586dcd713dacbf89a1204020402040204020580c2d72f040208cd036ba5cfbc03ea2471fdf02737f64dbcd58c34461a7ec1e586dcd713dacbf89a12eb027300d1eded91b1a57301e6c6b2a5730200040ed801d60193e4c6b2a5730300040ec5a7eded92c1b2a57304007305720193c2b2a5730600d07307"
    )
    .get

  def dexSellOrderErgoTreeGen: Gen[HexString] = Gen.const(sellOrderErgoTree)

  def dexSellOrderGen: Gen[(Output, Asset)] =
    for {
      out   <- outputGen(mainChain = true, dexSellOrderErgoTreeGen)
      token <- assetGen.map(_.copy(boxId = out.boxId))
    } yield (out, token)

  def dexSellOrdersGen(num: Int): Gen[List[(Output, Asset)]] =
    Gen.listOfN(num, dexSellOrderGen)

  /** from [[http://github.com/aslesarenko/ergo-tool/blob/3b948e527a816e51acd4d85d99595cc93d735a59/src/test/resources/mockwebserver/node_responses/response_Box_AAE_buyer_contract.json#L4-L4  response_Box_AAE_buyer_contract.json#L4-L4 ]] <br/>
    * which was generated with [[http://github.com/aslesarenko/ergo-tool/blob/3b948e527a816e51acd4d85d99595cc93d735a59/src/main/scala-2.12/org/ergoplatform/appkit/ergotool/dex/CreateBuyOrderCmd.scala#L56-L56 CreateBuyOrderCmd.scala#L56-L56]]
    */
  val buyOrderErgoTree: HexString = HexString
    .fromString[Try](
      "100c08cd036ba5cfbc03ea2471fdf02737f64dbcd58c34461a7ec1e586dcd713dacbf89a12040004000400040004000e2021f84cf457802e66fb5930fb5d45fbe955933dc16a72089bf8980797f24e2fa1040005780400040008cd036ba5cfbc03ea2471fdf02737f64dbcd58c34461a7ec1e586dcd713dacbf89a12eb027300d1eded91b1a57301e6c6b2a5730200040ed803d601e4c6b2a5730300020c4d0ed602eded91b172017304938cb27201730500017306928cb27201730700027308d60393e4c6b2a5730900040ec5a7eded720293c2b2a5730a00d0730b7203"
    )
    .get

  def dexBuyOrderErgoTreeGen: Gen[HexString] = Gen.const(buyOrderErgoTree)

  def dexBuyOrderGen: Gen[Output] =
    for {
      out <- outputGen(mainChain = true, dexBuyOrderErgoTreeGen)
    } yield out

  // unconfirmed transactions
  def UTransactionGen: Gen[UTransaction] =
    for {
      ts <- Gen.posNum[Long]
      id <- txIdGen
      sz <- Gen.posNum[Int]
    } yield UTransaction(id, ts, sz)

  def UInputGen: Gen[UInput] =
    for {
      boxId <- boxIdGen
      txId  <- txIdGen
      index <- Gen.posNum[Int]
      proof <- hexStringRGen
      ext   <- jsonFieldsGen
    } yield UInput(boxId, txId, index, proof.some, ext)

  def UOutputGen(address: Address, tree: HexString): Gen[UOutput] =
    for {
      boxId  <- boxIdGen
      txId   <- txIdGen
      value  <- Gen.posNum[Long]
      height <- Gen.posNum[Int]
      idx    <- Gen.posNum[Int]
      template = sigma.deriveErgoTreeTemplateHash[Try](tree).get
      regs <- jsonFieldsGen
    } yield UOutput(
      boxId,
      txId,
      value,
      height,
      idx,
      tree,
      template,
      address,
      regs
    )

  def `unconfirmedTransactionWithUInput&UOutputGen`(
    address: Address,
    tree: HexString
  ): Gen[(Output, UOutput, UInput, UTransaction, Header, Transaction)] =
    for {
      header <- headerGen.map(_.copy(mainChain = true))
      txId   <- txIdGen
      boxId  <- boxIdGen
      uout   <- UOutputGen(address, tree).map(_.copy(ergoTree = tree, address = address, txId = txId))
      uin    <- UInputGen.map(_.copy(txId = txId, boxId = boxId))
      uTx    <- UTransactionGen.map(_.copy(id = txId))
      tx     <- transactionGen(mainChain = true).map(_.copy(headerId = header.id))
      out <-
        outputGen(mainChain = true).map(_.copy(boxId = boxId, ergoTree = tree, address = address, txId = txId))
    } yield (out, uout, uin, uTx, header, tx)

  //  Gen[List[GenuineToken]]
  def genuineTokenListGen(gtList: Iterable[(String, Boolean)]): Gen[List[GenuineToken]] =
    Gen.sequence[List[GenuineToken], GenuineToken](gtList.map { case (name, unique) => genuineTokenGen(name, unique) })

  def genuineTokenGen(tokenName: String, uniqueName: Boolean): Gen[GenuineToken] =
    for {
      id     <- assetIdGen
      issuer <- Gen.oneOf[Option[String]](Some("ISSUER#1"), Some("ISSUER#2"), None)
    } yield GenuineToken(id, tokenName, uniqueName, issuer)

  def `tokenName&IDGen`(tokenName: String): Gen[(TokenId, String)] =
    for {
      id <- assetIdGen
    } yield (id, tokenName)

  def blockedTokenGen: Gen[BlockedToken] =
    for {
      id        <- assetIdGen
      tokenName <- Gen.oneOf("BLOCKED#1", "BLOCKED#2", "BLOCKED#3")
    } yield BlockedToken(id, tokenName)

}
