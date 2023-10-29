package org.ergoplatform.explorer.protocol

import org.ergoplatform.settings.MonetarySettings
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class EmissionSpec extends AnyFlatSpec with should.Matchers {
  val emission = new Emission(MonetarySettings(), ReemissionSettings())
  "Emission.issuedCoinsAfterHeight" should "compute correct total supply" in {
    println((1L to 1119273L).map(h => emission.emissionAt(h)).sum)
    println(emission.issuedCoinsAfterHeight(8000000))
  }
}
