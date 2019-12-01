package org.ergoplatform.explorer.grabber

import cats.effect._
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.refineV
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.explorer.protocol.models.ApiFullBlock
import monocle.macros.syntax.lens._
import org.ergoplatform.explorer.db.RealDbTest
import org.ergoplatform.explorer.grabber.GrabberTestNetworkService.Source
import org.ergoplatform.explorer.settings.{ProtocolSettings, Settings}
import org.ergoplatform.settings.MonetarySettings
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.ScalacheckShapeless._
import org.scalatest.PropSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.concurrent.duration._

class ChainGrabberSpec
  extends PropSpec
  with ScalaCheckDrivenPropertyChecks
  with RealDbTest {

  import org.ergoplatform.explorer.commonGenerators._

  ignore("Network scanning") {
    forSingleInstance(
      Gen.listOfN(10, implicitly[Arbitrary[ApiFullBlock]].arbitrary)
    ) { rawBlocks =>
      val apiBlocks = rawBlocks.zipWithIndex.map {
        case (block, idx) => block.lens(_.header.height).modify(_ => idx)
      }
      val networkService = new GrabberTestNetworkService[IO](Source(apiBlocks))
      val settings = Settings(
        1.second,
        null,
        ProtocolSettings(refineV[ValidByte]("16").right.get, "0x01", MonetarySettings())
      )
      val grabber = ChainGrabber[IO, ConnectionIO](settings, networkService)(xa.trans).unsafeRunSync()

      grabber.run.compile.drain.unsafeRunSync()
    }
  }

}
