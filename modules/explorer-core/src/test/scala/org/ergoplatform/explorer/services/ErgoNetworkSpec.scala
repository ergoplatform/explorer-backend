package org.ergoplatform.explorer.services

import cats.effect.{BracketThrow, Resource}
import org.http4s.client.Client
import org.scalatest.{Matchers, PropSpec}

class ErgoNetworkSpec extends PropSpec with Matchers {

  def createFakeClient[F[_]](implicit F: BracketThrow[F]) = Client[F]{
    case _ => Resource.eval()
  }

  property("Ergo network service should correctly determine node with best height"){

  }
}
