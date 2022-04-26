package org.ergoplatform.explorer.protocol

import cats.data.NonEmptyList
import org.ergoplatform.explorer.HexString
import org.ergoplatform.explorer.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.explorer.SigmaType.{SCollection, STupleN}
import org.ergoplatform.explorer.protocol.models.RegisterValue

import org.scalatest._
import flatspec._
import matchers._

import scala.util.{Success, Try}

class RegistersParserSpec extends AnyFlatSpec with should.Matchers {
  "RegistersParser[T] parse method" should "parse primitive register" in {
    val raw = HexString.fromStringUnsafe("0406")
    RegistersParser[Try].parseAny(raw) should be(Success(RegisterValue(SInt, "3")))
  }

  it should "parse coll register" in {
    val raw = HexString.fromStringUnsafe("100204a00b")
    RegistersParser[Try].parseAny(raw) should be(Success(RegisterValue(SCollection(SInt), "[2,720]")))
  }

  it should "parse nested Coll register" in {
    val raw = HexString.fromStringUnsafe("0c400504b40180febe81027880d4d4ab015a80bfdf80013c80aaea55")
    RegistersParser[Try].parseAny(raw) should be(
      Success(
        RegisterValue(
          SCollection(STupleN(NonEmptyList.of(SInt, SLong))),
          "[[90,270000000],[60,180000000],[45,135000000],[30,90000000]]"
        )
      )
    )
  }

  it should "parse GroupElement register" in {
    val raw = HexString.fromStringUnsafe("0702d73c1f31a706c42ff3f50a57474e0319556e175e4c31cfcc327ad23f1bbbaafd")
    RegistersParser[Try].parseAny(raw) should be(
      Success(
        RegisterValue(SGroupElement, "02d73c1f31a706c42ff3f50a57474e0319556e175e4c31cfcc327ad23f1bbbaafd")
      )
    )
  }

  it should "parse ByteArray register" in {
    val raw = HexString.fromStringUnsafe(
      "0e6f98040483030808cd039bb5fe52359a64c99a60fd944fc5e388cbdc4d37ff091cc841c3ee79060b864708cd031fb52cf6e805f80d97cde289f4f757d49accf0c83fb864b27d2cf982c37f9a8b08cd0352ac2a471339b0d23b3d2c5ce0db0e81c969f77891b9edf0bda7fd39a78184e7"
    )
    RegistersParser[Try].parseAny(raw) should be(
      Success(
        RegisterValue(
          SCollection(SByte),
          "98040483030808cd039bb5fe52359a64c99a60fd944fc5e388cbdc4d37ff091cc841c3ee79060b864708cd031fb52cf6e805f80d97cde289f4f757d49accf0c83fb864b27d2cf982c37f9a8b08cd0352ac2a471339b0d23b3d2c5ce0db0e81c969f77891b9edf0bda7fd39a78184e7"
        )
      )
    )
  }
}
