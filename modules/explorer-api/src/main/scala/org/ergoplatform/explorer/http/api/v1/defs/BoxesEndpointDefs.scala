package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.{Epochs, Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.OutputInfo
import org.ergoplatform.explorer.settings.RequestsSettings
import org.ergoplatform.explorer.{Address, BoxId, HexString, TokenId}
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._

final class BoxesEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "boxes"

  def endpoints: List[Endpoint[_, _, _, _]] =
    streamUnspentOutputsByEpochsDef ::
    streamUnspentOutputsDef ::
    unspentOutputsByTokenIdDef ::
    outputsByTokenIdDef ::
    getOutputByIdDef ::
    getOutputsByErgoTreeDef ::
    getUnspentOutputsByErgoTreeDef ::
    getOutputsByAddressDef ::
    getUnspentOutputsByAddressDef ::
    Nil

  def streamUnspentOutputsDef: Endpoint[Epochs, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent")
      .in(epochSlicing(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def streamUnspentOutputsByEpochsDef: Endpoint[Int, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byLastEpochs")
      .in(lastEpochs(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def outputsByTokenIdDef: Endpoint[(TokenId, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byTokenId" / path[TokenId])
      .in(paging(settings.maxEntitiesPerHeavyRequest))
      .out(jsonBody[Items[OutputInfo]])

  def unspentOutputsByTokenIdDef: Endpoint[(TokenId, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byTokenId" / path[TokenId])
      .in(paging(settings.maxEntitiesPerHeavyRequest))
      .out(jsonBody[Items[OutputInfo]])

  def getOutputByIdDef: Endpoint[BoxId, ApiErr, OutputInfo, Any] =
    baseEndpointDef.get
      .in(PathPrefix / path[BoxId])
      .out(jsonBody[OutputInfo])

  def getOutputsByErgoTreeDef: Endpoint[(HexString, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTree" / path[HexString])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])

  def getUnspentOutputsByErgoTreeDef: Endpoint[(HexString, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byErgoTree" / path[HexString])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])

  def getOutputsByAddressDef: Endpoint[(Address, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byAddress" / path[Address])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])

  def getUnspentOutputsByAddressDef: Endpoint[(Address, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byAddress" / path[Address])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])
}
