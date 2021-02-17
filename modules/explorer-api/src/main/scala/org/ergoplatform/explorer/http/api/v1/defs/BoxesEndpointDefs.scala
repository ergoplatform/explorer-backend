package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.{Epochs, Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{BoxQuery, OutputInfo}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._

final class BoxesEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "boxes"

  def endpoints: List[Endpoint[_, _, _, _]] =
    streamUnspentOutputsByEpochsDef ::
    streamUnspentOutputsDef ::
    streamOutputsByErgoTreeTemplateHashDef ::
    streamUnspentOutputsByErgoTreeTemplateHashDef ::
    unspentOutputsByTokenIdDef ::
    outputsByTokenIdDef ::
    getOutputByIdDef ::
    getOutputsByErgoTreeDef ::
    getOutputsByErgoTreeTemplateHashDef ::
    getUnspentOutputsByErgoTreeDef ::
    getUnspentOutputsByErgoTreeTemplateHashDef ::
    getOutputsByAddressDef ::
    getUnspentOutputsByAddressDef ::
    searchOutputsDef ::
    Nil

  def streamUnspentOutputsDef: Endpoint[Epochs, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "stream")
      .in(epochSlicing(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def streamUnspentOutputsByEpochsDef: Endpoint[Int, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byLastEpochs" / "stream")
      .in(lastEpochs(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def streamOutputsByErgoTreeTemplateHashDef
    : Endpoint[(ErgoTreeTemplateHash, Epochs), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash] / "stream")
      .in(epochSlicing(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def streamUnspentOutputsByErgoTreeTemplateHashDef
    : Endpoint[(ErgoTreeTemplateHash, Epochs), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash] / "stream")
      .in(epochSlicing(settings.maxEpochsPerRequest))
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

  def getOutputsByErgoTreeTemplateHashDef: Endpoint[(ErgoTreeTemplateHash, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])

  def getUnspentOutputsByErgoTreeTemplateHashDef
    : Endpoint[(ErgoTreeTemplateHash, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash])
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

  def searchOutputsDef: Endpoint[(BoxQuery, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.post
      .in(PathPrefix / "search")
      .in(jsonBody[BoxQuery])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])
}
