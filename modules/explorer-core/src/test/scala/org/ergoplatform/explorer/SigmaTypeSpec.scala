package org.ergoplatform.explorer

import cats.data.NonEmptyList
import org.ergoplatform.explorer.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.explorer.SigmaType.{SCollection, STupleN}

import org.scalatest._
import flatspec._
import matchers._

class SigmaTypeSpec extends AnyFlatSpec with should.Matchers {

  "SigmaType parse method" should "parse primitive type signature" in {
    val sig = "SInt"
    SigmaType.parse(sig) should be(Some(SInt))
  }

  it should "parse HKT signature" in {
    val sig = "Coll[SInt]"
    SigmaType.parse(sig) should be(Some(SCollection(SInt)))
  }

  it should "parse Nested HKT signature" in {
    val sig = "Coll[(SInt,SLong)]"
    SigmaType.parse(sig) should be(Some(SCollection(STupleN(NonEmptyList.of(SInt, SLong)))))
  }

}
