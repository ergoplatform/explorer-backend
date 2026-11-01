package org.ergoplatform.explorer.v1.services

import cats.{Monad, Parallel}
import cats.effect.{Concurrent, ContextShift, IO}
import cats.syntax.list._
import dev.profunktor.redis4cats.RedisCommands
import doobie.free.connection.ConnectionIO
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.ValidByte
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.{Address, BoxId}
import org.ergoplatform.explorer.cache.Redis
import org.ergoplatform.explorer.commonGenerators.forSingleInstance
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.db.{repositories, RealDbTest, Trans}
import org.ergoplatform.explorer.http.api.models.{Items, Paging}
import org.ergoplatform.explorer.http.api.models.Sorting.{Asc, Desc}
import org.ergoplatform.explorer.testSyntax.runConnectionIO._
import org.ergoplatform.explorer.http.api.streaming.CompileStream
import org.ergoplatform.explorer.http.api.v1.models.OutputInfo
import org.ergoplatform.explorer.http.api.v1.services.{Boxes, Mempool}
import org.ergoplatform.explorer.settings.{RedisSettings, ServiceSettings, UtxCacheSettings}
import org.ergoplatform.explorer.v1.services.constants._
import org.ergoplatform.explorer.protocol.sigmaWrappers
import org.ergoplatform.explorer.testContainers.RedisTest
import org.scalatest.{PrivateMethodTester, TryValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Try
import tofu.syntax.monadic._
import org.ergoplatform.explorer.v1.services.BoxSpec._
import org.ergoplatform.explorer.db.models.generators._
import org.ergoplatform.explorer.http.api.v1.shared.MempoolProps

trait BoxSpec
  extends AnyFlatSpec
  with should.Matchers
  with TryValues
  with PrivateMethodTester
  with RealDbTest
  with RedisTest

class BS_A extends BoxSpec {
  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Box Service" should "get unspent outputs with exclusions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigmaWrappers.addressToErgoTreeHex(address1T.get)
    val getUnspentOutputsByAddressD             = PrivateMethod[ConnectionIO[List[OutputInfo]]]('getUnspentOutputsByAddressD)
    withResources[IO](container.mappedPort(redisTestPort))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (_, box) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out_, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out_).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                forSingleInstance(
                  balanceOfAddressGen(
                    mainChain = true,
                    address1T.get,
                    address1Tree,
                    (100.toNanoErgo, 1) :: (200.toNanoErgo, 1) :: List[(Long, Int)]()
                  )
                ) { infoTupleList =>
                  infoTupleList.foreach { case (header, out, tx) =>
                    hRepo.insert(header).runWithIO()
                    outRepo.insert(out).runWithIO()
                    txRepo.insert(tx).runWithIO()
                  }
                  box
                    .getOutputsByAddress(address1T.get, Paging(0, Int.MaxValue), Desc)
                    .unsafeRunSync()
                    .total should be(3)
                  (box invokePrivate getUnspentOutputsByAddressD(
                    address1T.get,
                    Desc,
                    List(out_.boxId).map(x => BoxId(x.value)).toNel
                  ))
                    .runWithIO()
                    .map(_.boxId) should not contain theSameElementsAs(List(out_.boxId))
                }
            }
          }
        }
      }
      .unsafeRunSync()
  }

}

class BS_B extends BoxSpec {
  import org.ergoplatform.explorer.v1.services.BoxSpec._
  import org.ergoplatform.explorer.db.models.generators._

  val networkPrefix: String Refined ValidByte = "16" // strictly run test-suite with testnet network prefix
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Box Service" should "merge unconfirmed outputs (from mempool) & unspent outputs with exclusions" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    val address1S                               = SenderAddressString
    val address1T                               = Address.fromString[Try](address1S)
    lazy val address1Tree                       = sigmaWrappers.addressToErgoTreeHex(address1T.get)
    withResources[IO](container.mappedPort(redisTestPort))
      .use { case (settings, utxCache, redis) =>
        withServices[IO, ConnectionIO](settings, utxCache, redis) { (_, box) =>
          address1T.isSuccess should be(true)
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, uoutRepo, uinRepo, uTxRepo) =>
            forSingleInstance(`unconfirmedTransactionWithUInput&UOutputGen`(address1T.get, address1Tree)) {
              case (out_, uout, uin, utx, header, tx) =>
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out_).runWithIO()
                uTxRepo.insert(utx).runWithIO()
                uinRepo.insert(uin).runWithIO()
                uoutRepo.insert(uout).runWithIO()
                forSingleInstance(
                  balanceOfAddressGen(
                    mainChain = true,
                    address1T.get,
                    address1Tree,
                    (100.toNanoErgo, 1) :: (200.toNanoErgo, 1) :: List[(Long, Int)]()
                  )
                ) { infoTupleList =>
                  infoTupleList.foreach { case (header, out, tx) =>
                    hRepo.insert(header).runWithIO()
                    outRepo.insert(out).runWithIO()
                    txRepo.insert(tx).runWithIO()
                  }
                  box
                    .getOutputsByAddress(address1T.get, Paging(0, Int.MaxValue), Desc)
                    .unsafeRunSync()
                    .total should be(3)
                  val data = box
                    .`getUnspent&UnconfirmedOutputsMergedByAddress`(
                      address1T.get,
                      Desc
                    )
                    .unsafeRunSync()
                    .map(_.boxId)

                  data.length should be(3)
                  data should contain theSameElementsAs (List(uout.boxId) ++ infoTupleList.map(_._2.boxId))
                }
            }
          }
        }
      }
      .unsafeRunSync()
  }
}

class BS_C extends BoxSpec {
  import io.chrisdavenport.log4cats.Logger
  import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
  import io.circe.parser.decode
  import org.ergoplatform.explorer.http.api.decodingFailureHandler._
  import org.ergoplatform.explorer.http.api.syntax.routes._
  import org.ergoplatform.explorer.http.api.v1.defs.BoxesEndpointDefs
  import org.ergoplatform.explorer.http.api.v1.routes.BoxesRoutes
  import org.ergoplatform.explorer.settings.RequestsSettings
  import org.http4s.{Method, Request, Status, Uri}
  import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
  import sttp.tapir.openapi.circe.yaml._

  val networkPrefix: String Refined ValidByte = "16"
  implicit val addressEncoder: ErgoAddressEncoder =
    ErgoAddressEncoder(networkPrefix.value.toByte)

  "Box Service and BoxesRoutes" should "sort outputs by globalIndex asc and desc across multiple pages and chunks" in {
    import tofu.fs2Instances._
    implicit val trans: Trans[ConnectionIO, IO] = Trans.fromDoobie(xa)
    implicit val log: Logger[IO]                = Slf4jLogger.getLogger[IO]

    val targetAddressT = Address.fromString[Try](ReceiverAddressString)
    val otherAddressT  = Address.fromString[Try](SenderAddressString)
    targetAddressT.isSuccess should be(true)
    otherAddressT.isSuccess should be(true)

    val targetAddress = targetAddressT.get
    val targetTree    = sigmaWrappers.addressToErgoTreeHex(targetAddress)
    val otherAddress  = otherAddressT.get
    val otherTree     = sigmaWrappers.addressToErgoTreeHex(otherAddress)

    withResources[IO](container.mappedPort(redisTestPort))
      .use { case (_, utxCache, redis) =>
        // Use chunkSize = 2 with page limit = 4 across 10 boxes so each full page crosses multiple fs2 chunks
        val smallChunkSettings = ServiceSettings(chunkSize = 2)
        def withExtraRepos[D[_]: LiftConnectionIO: cats.effect.Sync](
          f: (
            repositories.InputRepo[D],
            repositories.TokenRepo[D],
            repositories.AssetRepo[D, fs2.Stream]
          ) => Any
        ): Any =
          f(
            repositories.InputRepo[IO, D].unsafeRunSync(),
            repositories.TokenRepo[IO, D].unsafeRunSync(),
            repositories.AssetRepo[IO, D].unsafeRunSync()
          )

        withServices[IO, ConnectionIO](smallChunkSettings, utxCache, redis) { (_, box) =>
          withExtraRepos[ConnectionIO] { (inRepo, tokenRepo, assetRepo) =>
          withLiveRepos[ConnectionIO] { (hRepo, txRepo, outRepo, _, _, _) =>
            val scrambledGix = List(500L, 100L, 900L, 300L, 700L, 200L, 1000L, 400L, 800L, 600L)
            val specs        = scrambledGix.zipWithIndex.map { case (_, idx) => ((idx + 1).toNanoErgo, idx + 10) }

            forSingleInstance(balanceOfAddressGen(mainChain = true, targetAddress, targetTree, specs)) { tuples =>
              val spentGixSet = Set(200L, 500L, 900L)
              val assetGixSet = Set(100L, 500L, 700L, 1000L)

              val expectedMetaByGix =
                scala.collection.mutable.Map.empty[
                  Long,
                  (BoxId, Option[org.ergoplatform.explorer.TxId], Option[org.ergoplatform.explorer.TokenId])
                ]
              tuples.zip(scrambledGix).foreach { case ((header, rawOut, tx), gix) =>
                val out = rawOut.copy(globalIndex = gix)
                hRepo.insert(header).runWithIO()
                txRepo.insert(tx).runWithIO()
                outRepo.insert(out).runWithIO()

                var spentTxOpt: Option[org.ergoplatform.explorer.TxId] = None
                if (spentGixSet.contains(gix)) {
                  forSingleInstance(inputGen(mainChain = true, out.boxId)) { spendIn =>
                    val inOnHeader = spendIn.copy(headerId = header.id)
                    inRepo.insert(inOnHeader).runWithIO()
                    spentTxOpt = Some(inOnHeader.txId)
                  }
                } else if (gix == 400L) {
                  forSingleInstance(inputGen(mainChain = false, out.boxId)) { forkedIn =>
                    inRepo.insert(forkedIn).runWithIO()
                  }
                }

                var tokenIdOpt: Option[org.ergoplatform.explorer.TokenId] = None
                if (assetGixSet.contains(gix)) {
                  forSingleInstance(tokenGen.flatMap(t => assetGen.map(a => (t, a)))) { case (tok, rawAsset) =>
                    val token = tok.copy(boxId = out.boxId)
                    val asset = rawAsset.copy(tokenId = token.id, boxId = out.boxId, headerId = header.id, index = 0)
                    tokenRepo.insert(token).runWithIO()
                    assetRepo.insert(asset).runWithIO()
                    tokenIdOpt = Some(token.id)
                  }
                }

                expectedMetaByGix.put(gix, (out.boxId, spentTxOpt, tokenIdOpt))
              }

              // Forked output for targetAddress and main-chain output for otherAddress must not pollute results
              forSingleInstance(balanceOfAddressGen(mainChain = false, targetAddress, targetTree, 50.toNanoErgo, 99)) {
                case (forkH, forkOut, forkTx) =>
                  hRepo.insert(forkH).runWithIO()
                  txRepo.insert(forkTx).runWithIO()
                  outRepo.insert(forkOut.copy(globalIndex = 50L)).runWithIO()
              }
              forSingleInstance(balanceOfAddressGen(mainChain = true, otherAddress, otherTree, 60.toNanoErgo, 98)) {
                case (otherH, otherOut, otherTx) =>
                  hRepo.insert(otherH).runWithIO()
                  txRepo.insert(otherTx).runWithIO()
                  outRepo.insert(otherOut.copy(globalIndex = 550L)).runWithIO()
              }

              val pagings = List(Paging(0, 4), Paging(4, 4), Paging(8, 4))

              // 1. Service-level verification for getOutputsByAddress (Asc & Desc across 3 pages and 5 chunks)
              val addrAscPages  = pagings.map(p => box.getOutputsByAddress(targetAddress, p, Asc).unsafeRunSync())
              val addrDescPages = pagings.map(p => box.getOutputsByAddress(targetAddress, p, Desc).unsafeRunSync())

              addrAscPages.foreach(_.total shouldBe 10)
              addrDescPages.foreach(_.total shouldBe 10)

              addrAscPages.map(_.items.map(_.globalIndex)) shouldBe List(
                List(100L, 200L, 300L, 400L),
                List(500L, 600L, 700L, 800L),
                List(900L, 1000L)
              )
              addrDescPages.map(_.items.map(_.globalIndex)) shouldBe List(
                List(1000L, 900L, 800L, 700L),
                List(600L, 500L, 400L, 300L),
                List(200L, 100L)
              )

              val addrAscAll  = addrAscPages.flatMap(_.items)
              val addrDescAll = addrDescPages.flatMap(_.items)

              addrAscAll.map(_.boxId).distinct.size shouldBe 10
              addrDescAll shouldBe addrAscAll.reverse

              addrAscAll.foreach { info =>
                val (expectedBoxId, expectedSpentTx, expectedTokenOpt) = expectedMetaByGix(info.globalIndex)
                info.boxId shouldBe expectedBoxId
                info.spentTransactionId shouldBe expectedSpentTx
                info.assets.map(_.tokenId) shouldBe expectedTokenOpt.toList
              }

              // Determinism across repeated runs
              box.getOutputsByAddress(targetAddress, Paging(0, 4), Desc).unsafeRunSync() shouldBe addrDescPages.head
              box.getOutputsByAddress(targetAddress, Paging(0, 4), Asc).unsafeRunSync() shouldBe addrAscPages.head

              // 2. Service-level verification for sibling getOutputsByErgoTree (Asc & Desc)
              val treeAscPages  = pagings.map(p => box.getOutputsByErgoTree(targetTree, p, Asc).unsafeRunSync())
              val treeDescPages = pagings.map(p => box.getOutputsByErgoTree(targetTree, p, Desc).unsafeRunSync())

              treeAscPages.flatMap(_.items) shouldBe addrAscAll
              treeDescPages.flatMap(_.items) shouldBe addrDescAll

              // 3. End-to-end HTTP route verification (BoxesRoutes)
              val reqSettings = RequestsSettings(
                maxEntitiesPerRequest      = 100,
                maxEntitiesPerHeavyRequest = 100,
                maxBlocksPerRequest        = 100
              )
              val routes = BoxesRoutes[IO](reqSettings, box)

              def getItems(pathAndQuery: String): (Status, String) = {
                val req  = Request[IO](Method.GET, Uri.unsafeFromString(pathAndQuery))
                val resp = routes.orNotFound.run(req).unsafeRunSync()
                val body = resp.as[String].unsafeRunSync()
                (resp.status, body)
              }

              def decodeGix(body: String): List[Long] =
                decode[Items[OutputInfo]](body).right.get.items.map(_.globalIndex)

              val (stAsc1, bodyAsc1) =
                getItems(s"/api/v1/boxes/byAddress/$targetAddress?offset=0&limit=4&sortDirection=asc")
              stAsc1 shouldBe Status.Ok
              decodeGix(bodyAsc1) shouldBe List(100L, 200L, 300L, 400L)

              val (stAsc2, bodyAsc2) =
                getItems(s"/api/v1/boxes/byAddress/$targetAddress?offset=4&limit=4&sortDirection=asc")
              stAsc2 shouldBe Status.Ok
              decodeGix(bodyAsc2) shouldBe List(500L, 600L, 700L, 800L)

              val (stDesc1, bodyDesc1) =
                getItems(s"/api/v1/boxes/byAddress/$targetAddress?offset=0&limit=4&sortDirection=desc")
              stDesc1 shouldBe Status.Ok
              decodeGix(bodyDesc1) shouldBe List(1000L, 900L, 800L, 700L)

              // Default omitted sortDirection should default to Desc
              val (stDef, bodyDef) =
                getItems(s"/api/v1/boxes/byAddress/$targetAddress?offset=0&limit=4")
              stDef shouldBe Status.Ok
              decodeGix(bodyDef) shouldBe List(1000L, 900L, 800L, 700L)

              // Invalid sortDirection should return 400 Bad Request
              val (stBad, _) =
                getItems(s"/api/v1/boxes/byAddress/$targetAddress?sortDirection=invalid")
              stBad shouldBe Status.BadRequest

              // Sibling byErgoTree HTTP route
              val (stTreeAsc, bodyTreeAsc) =
                getItems(s"/api/v1/boxes/byErgoTree/$targetTree?offset=0&limit=4&sortDirection=asc")
              stTreeAsc shouldBe Status.Ok
              decodeGix(bodyTreeAsc) shouldBe List(100L, 200L, 300L, 400L)

              val (stTreeDesc, bodyTreeDesc) =
                getItems(s"/api/v1/boxes/byErgoTree/$targetTree?offset=0&limit=4&sortDirection=desc")
              stTreeDesc shouldBe Status.Ok
              decodeGix(bodyTreeDesc) shouldBe List(1000L, 900L, 800L, 700L)

              // 4. OpenAPI specification verification for getApiV1BoxesByaddressP1 & getApiV1BoxesByergotreeP1
              val openApi = OpenAPIDocsInterpreter()
                .toOpenAPI(new BoxesEndpointDefs[IO](reqSettings).endpoints, "Ergo Explorer API v1", "1.0")
              val openApiYaml = openApi.toYaml
              openApiYaml should include("operationId: getApiV1BoxesByaddressP1")
              openApiYaml should include("operationId: getApiV1BoxesByergotreeP1")

              val byAddrOp = openApi.paths.pathItems("/api/v1/boxes/byAddress/{p1}").get.get
              byAddrOp.operationId shouldBe Some("getApiV1BoxesByaddressP1")
              byAddrOp.parameters.flatMap(_.right.toOption.map(_.name)) should contain("sortDirection")

              val byTreeOp = openApi.paths.pathItems("/api/v1/boxes/byErgoTree/{p1}").get.get
              byTreeOp.operationId shouldBe Some("getApiV1BoxesByergotreeP1")
              byTreeOp.parameters.flatMap(_.right.toOption.map(_.name)) should contain("sortDirection")
            }
          }
          }
        }
      }
      .unsafeRunSync()
  }
}

object BoxSpec {
  import cats.effect.Sync
  import scala.concurrent.duration._
  import cats.syntax.traverse._
  import org.ergoplatform.explorer.db.repositories.{
    HeaderRepo,
    OutputRepo,
    TransactionRepo,
    UInputRepo,
    UOutputRepo,
    UTransactionRepo
  }

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def withResources[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift](port: Int) = for {
    redis <- Some(RedisSettings(s"redis://localhost:$port")).map(Redis[F]).sequence
  } yield (ServiceSettings(chunkSize = 100), UtxCacheSettings(transactionTtl = 10.minute), redis)

  def withServices[F[_]: Sync: Monad: Parallel: Concurrent: ContextShift, D[
    _
  ]: LiftConnectionIO: CompileStream: Sync](
    settings: ServiceSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(body: (Mempool[F], Boxes[F]) => Any)(implicit encoder: ErgoAddressEncoder, trans: D Trans F): F[Unit] =
    for {
      memprops <- MempoolProps(settings, utxCacheSettings, redis)(trans)
      mempool <- Mempool[F, D](
                   settings,
                   utxCacheSettings,
                   redis,
                   memprops
                 )(trans)
      boxes <- Boxes[F, D](settings, memprops)(trans)
      _ = body(mempool, boxes)
    } yield ()

  def withLiveRepos[D[_]: LiftConnectionIO: Sync](
    body: (
      HeaderRepo[D, fs2.Stream],
      TransactionRepo[D, fs2.Stream],
      OutputRepo[D, fs2.Stream],
      UOutputRepo[D, fs2.Stream],
      UInputRepo[D, fs2.Stream],
      UTransactionRepo[D, fs2.Stream]
    ) => Any
  ): Any =
    body(
      repositories.HeaderRepo[IO, D].unsafeRunSync(),
      repositories.TransactionRepo[IO, D].unsafeRunSync(),
      repositories.OutputRepo[IO, D].unsafeRunSync(),
      repositories.UOutputRepo[IO, D].unsafeRunSync(),
      repositories.UInputRepo[IO, D].unsafeRunSync(),
      repositories.UTransactionRepo[IO, D].unsafeRunSync()
    )
}
