package org.ergoplatform.explorer.protocol

import org.ergoplatform.settings.MonetarySettings
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class EmissionSpec extends AnyFlatSpec with should.Matchers {
  val emission = new Emission(MonetarySettings(), ReemissionSettings())
  "Emission.issuedCoinsAfterHeight" should "compute correct total supply" in {
    emission.issuedCoinsAfterHeight(6647136L) shouldBe 102624741000000000L
  }
}
