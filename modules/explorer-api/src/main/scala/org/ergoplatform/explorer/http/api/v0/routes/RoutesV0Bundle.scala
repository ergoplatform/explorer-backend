package org.ergoplatform.explorer.http.api.v0.routes

import cats.Monad
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.semigroupk._
import dev.profunktor.redis4cats.algebra.RedisCommands
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.v0.modules.Search
import org.ergoplatform.explorer.http.api.v0.services._
import org.ergoplatform.explorer.settings.{ProtocolSettings, UtxCacheSettings}
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext

final case class RoutesV0Bundle[F[_]](routes: HttpRoutes[F])

object RoutesV0Bundle {

  def apply[
    F[_]: Concurrent: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](
    protocolSettings: ProtocolSettings,
    utxCacheSettings: UtxCacheSettings,
    redis: Option[RedisCommands[F, String, String]]
  )(trans: D Trans F)(implicit
    ec: ExecutionContext,
    encoder: ErgoAddressEncoder,
    opts: Http4sServerOptions[F, F]
  ): F[RoutesV0Bundle[F]] =
    for {
      implicit0(log: Logger[F]) <- Slf4jLogger.create
      blockChainService         <- BlockChainService(trans)
      assetsService             <- AssetsService(trans)
      dexService                <- DexService(trans)
      addressesService          <- AddressesService(trans)
      statsService              <- StatsService(protocolSettings)(trans)
      boxesService              <- BoxesService(trans)
      txsService                <- TransactionsService(trans)
      offchainService           <- OffChainService(utxCacheSettings, redis)(trans)
      search        = Search(blockChainService, txsService, addressesService)
      blockRoutes   = BlocksRoutes(blockChainService)
      assetRoutes   = AssetsRoutes(assetsService)
      dexRoutes     = DexRoutes(dexService)
      txRoutes      = TransactionsRoutes(txsService, offchainService)
      addressRoutes = AddressesRoutes(addressesService, txsService)
      infoRoutes    = InfoRoutes(statsService)
      statsRoutes   = StatsRoutes(statsService)
      chartsRoutes  = ChartsRoutes(statsService)
      docsRoutes    = DocsRoutes[F]
      searchRoutes  = SearchRoutes(search)
      boxesRoutes   = BoxesRoutes(boxesService)
      routes = infoRoutes <+> blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+>
                 addressRoutes <+> statsRoutes <+> docsRoutes <+> searchRoutes <+> boxesRoutes <+> chartsRoutes
    } yield RoutesV0Bundle(routes)
}
