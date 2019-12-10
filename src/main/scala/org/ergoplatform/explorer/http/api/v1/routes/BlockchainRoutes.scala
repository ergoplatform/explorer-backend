package org.ergoplatform.explorer.http.api.v1.routes

import io.circe.generic.auto._
import sttp.tapir.json.circe._
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.http.api.v1.models.BlockSummary
import org.ergoplatform.explorer.services.BlockchainService
import sttp.tapir._

final class BlockchainRoutes[F[_]](blockchainService: BlockchainService[F]) {

  private val PathPrefix = "blocks"

  def blockSummaryById: Endpoint[Id, Unit, BlockSummary, Nothing] = ???
//    endpoint.get
//      .in(BasePathPrefix)
//      .in(PathPrefix)
//      .in(path[Id])
//      .out(jsonBody[BlockSummary])
}
