package org.ergoplatform.explorer.protocol

import io.circe.parser.decode
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.ErgoAddressEncoder.TestnetNetworkPrefix
import org.ergoplatform.explorer.{Address, HexString, PrettyPrintErgoTree, SigmaType}
import org.ergoplatform.explorer.protocol.ergoInstances._
import org.ergoplatform.explorer.protocol.models.RegisterValue
import scorex.util.encode.Base16
import sigma.VersionContext
import sigma.ast._
import sigma.ast.syntax.TrueSigmaProp
import sigma.serialization.{ErgoTreeSerializer, ValueSerializer}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.math.BigInteger
import scala.util.{Success, Try}

class SigmaV6VersionContextSpec extends AnyFlatSpec with should.Matchers {

  implicit private val encoder: ErgoAddressEncoder = ErgoAddressEncoder(TestnetNetworkPrefix)

  private val v6Version: Byte = VersionContext.V6SoftForkVersion

  private val unsignedBigIntConst: Constant[SUnsignedBigInt.type] =
    UnsignedBigIntConstant(BigInteger.valueOf(42L))

  private val unsignedBigIntBytes: Array[Byte] =
    VersionContext.withVersions(v6Version, v6Version) {
      ValueSerializer.serialize(unsignedBigIntConst)
    }

  private val v6Tree: ErgoTree =
    VersionContext.withVersions(v6Version, v6Version) {
      val header = ErgoTree.setConstantSegregation(ErgoTree.defaultHeaderWithVersion(v6Version))
      ErgoTree(header, Vector(unsignedBigIntConst), TrueSigmaProp)
    }

  private val v6TreeBytes: Array[Byte] =
    VersionContext.withVersions(v6Version, v6Version) {
      ErgoTreeSerializer.DefaultSerializer.serializeErgoTree(v6Tree)
    }

  "ergoInstances decoders and encoders" should "apply V6 VersionContext at execution time and return DecodingFailure on invalid box ErgoTree" in {
    val valJson    = (unsignedBigIntConst: EvaluatedValue[_ <: SType]).asJson.noSpaces
    val decodedVal = decode[EvaluatedValue[_ <: SType]](valJson)
    decodedVal.isRight shouldBe true
    decodedVal.right.get.tpe shouldBe SUnsignedBigInt

    val treeJson    = v6Tree.asJson.noSpaces
    val decodedTree = decode[ErgoTree](treeJson)
    decodedTree.isRight shouldBe true
    decodedTree.right.get.isRightParsed shouldBe true
    decodedTree.right.get.version shouldBe v6Version
    decodedTree.right.get.constants shouldBe Vector(unsignedBigIntConst)

    val invalidBoxJson =
      """{
        |  "value": 1000000,
        |  "ergoTree": "ff",
        |  "assets": [],
        |  "creationHeight": 1,
        |  "additionalRegisters": {},
        |  "transactionId": "0000000000000000000000000000000000000000000000000000000000000000",
        |  "index": 0
        |}""".stripMargin
    val decodedBox = decode[org.ergoplatform.ErgoBox](invalidBoxJson)
    decodedBox.isLeft shouldBe true
  }

  "RegistersParser and SigmaType" should "deserialize and render Sigma v6 UnsignedBigInt values" in {
    SigmaType.parse("SUnsignedBigInt") shouldBe Some(SigmaType.SimpleKindSigmaType.SUnsignedBigInt)

    val raw = HexString.fromStringUnsafe(Base16.encode(unsignedBigIntBytes))
    RegistersParser[Try].parseAny(raw) shouldBe Success(
      RegisterValue(SigmaType.SimpleKindSigmaType.SUnsignedBigInt, unsignedBigIntConst.value.toString)
    )
  }

  "sigmaWrappers" should "decode v6 P2S addresses and extract v6 ErgoTree constants under V6 VersionContext" in {
    val treeHex   = HexString.fromStringUnsafe(Base16.encode(v6TreeBytes))
    val extracted = sigmaWrappers.extractErgoTreeConstants[Try](treeHex)
    extracted.isSuccess shouldBe true
    extracted.get.head._3.unwrapped shouldBe Base16.encode(unsignedBigIntBytes)

    val p2s  = encoder.fromProposition(v6Tree).get
    val addr = Address.fromStringUnsafe(encoder.toString(p2s))
    VersionContext.withVersions(2.toByte, 2.toByte) {
      sigmaWrappers.addressToErgoTreeHex(addr).unwrapped shouldBe Base16.encode(v6TreeBytes)
    }
  }

  "PrettyPrintErgoTree" should "format SUnsignedBigInt constants, placeholders, and typed variables without MatchError" in {
    PrettyPrintErgoTree.prettyPrint(unsignedBigIntConst) shouldBe "{42.toUnsignedBigInt}"
    PrettyPrintErgoTree.prettyPrint(ConstantPlaceholder(0, SUnsignedBigInt)) should include("UnsignedBigInt")
    PrettyPrintErgoTree.prettyPrint(ValUse(1, SUnsignedBigInt)) should include("ubi1")
  }
}
