package org.ergoplatform.explorer.services

import cats.data.NonEmptyList
import cats.effect.concurrent.Ref
import cats.effect.{BracketThrow, IO, Resource, Sync}
import eu.timepit.refined.refineV
import eu.timepit.refined.string.Url
import fs2.Stream
import io.circe.syntax._
import io.estatico.newtype.ops._
import org.ergoplatform.explorer.UrlString
import org.ergoplatform.explorer.settings.NetworkSettings
import org.http4s.Uri.Path.Segment
import org.http4s.client.Client
import org.http4s.{Request, Response}
import org.scalacheck.Gen.Parameters
import org.scalacheck.rng.Seed
import org.scalatest.{Matchers, PropSpec}
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext.Implicits.global

class ErgoNetworkSpec extends PropSpec with Matchers {

  def createFakeClient[F[_]: Sync](bestNode: Ref[F, (UrlString, Int)])(implicit
    F: BracketThrow[F]
  ) = Client[F] {
    case Request(_, uri, _, _, _, _) if uri.path.segments.contains(Segment("info")) =>
      Resource.eval(bestNode.get).flatMap { case (node, height) =>
        Resource.pure(
          Response[F](
            body = Stream.emits(
              nodeApiGenerator.generateNodeInfo
                .pureApply(Parameters.default, Seed(123L))
                .copy(fullHeight = if (uri.toString().dropRight(5) == node.value.value) height else height - 1)
                .asJson
                .toString()
                .getBytes
            )
          )
        )
      }
    case req => ???
  }

  property("Ergo network service should correctly determine node with best height") {

    implicit val contextShift = IO.contextShift(global)

    def genNodes: NonEmptyList[UrlString] = NonEmptyList.fromListUnsafe((0 to 100).foldLeft(List.empty[UrlString]) {
      case (list, index) =>
        list :+ refineV[Url].unsafeFrom(s"http://1.1.1.$index:9053").coerce[UrlString]
    })

    def makeSeveralRequest[F[_]: Sync](network: ErgoNetwork[F], reqQty: Int): F[Int] =
      (0 to reqQty).foldLeft(0.pure[F]) { case (prev, _) =>
        prev >> network.getBestHeight
      }

    val nodes = genNodes

    val selfCheckIntervalRequests = 10

    val settings = NetworkSettings(nodes, selfCheckIntervalRequests)

    val bestHeight = 10

    val program = for {
      ref <- Ref.of[IO, (UrlString, Int)](nodes.tail.last -> bestHeight)
      client = createFakeClient[IO](ref)
      ergoNetwork      <- ErgoNetwork[IO](client, settings)
      bestHeightOnNode <- makeSeveralRequest(ergoNetwork, selfCheckIntervalRequests)
    } yield bestHeightOnNode == bestHeight

    program.unsafeRunSync() shouldBe true
  }
}
