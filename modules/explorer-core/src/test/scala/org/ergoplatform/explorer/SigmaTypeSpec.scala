package org.ergoplatform.explorer

import cats.data.NonEmptyList
import org.ergoplatform.explorer.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.explorer.SigmaType.{SCollection, STupleN}
import org.scalatest.{Matchers, PropSpec}

class SigmaTypeSpec extends PropSpec with Matchers {
  property("Primitive type signature parsing") {
    val sig = "SInt"
    SigmaType.parse(sig) shouldBe Some(SInt)
  }
  property("HKT signature parsing") {
    val sig = "Coll[SInt]"
    SigmaType.parse(sig) shouldBe Some(SCollection(SInt))
  }
  property("Nested HKT signature parsing") {
    val sig = "Coll[(SInt,SLong)]"
    SigmaType.parse(sig) shouldBe Some(SCollection(STupleN(NonEmptyList.of(SInt, SLong))))
  }
}
