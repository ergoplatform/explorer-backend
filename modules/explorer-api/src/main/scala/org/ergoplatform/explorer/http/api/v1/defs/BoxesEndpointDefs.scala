package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer._
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.Sorting.SortOrder
import org.ergoplatform.explorer.http.api.models.{HeightRange, Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.{
  AnyOutputInfo,
  BoxAssetsQuery,
  BoxQuery,
  MOutputInfo,
  OutputInfo
}
import org.ergoplatform.explorer.settings.RequestsSettings
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._

final class BoxesEndpointDefs[F[_]](settings: RequestsSettings) {

  private val PathPrefix = "boxes"

  def endpoints: List[Endpoint[_, _, _, _]] =
    streamUnspentOutputsByEpochsDef ::
    streamUnspentOutputsByGixDef ::
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
    `getUnspent&UnconfirmedOutputsMergedByAddressDef` ::
    getUnspentOutputsByAddressDef ::
    streamOutputsByGixDef ::
    searchUnspentOutputsByTokensUnionDef ::
    searchUnspentOutputsDef ::
    searchOutputsDef ::
    Nil

  def streamUnspentOutputsDef: Endpoint[HeightRange, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "stream")
      .in(blocksSlicing(settings.maxBlocksPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))

  def streamUnspentOutputsByEpochsDef: Endpoint[Int, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byLastEpochs" / "stream")
      .in(lastBlocks(settings.maxBlocksPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))

  def streamUnspentOutputsByGixDef: Endpoint[(Long, Int), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byGlobalIndex" / "stream")
      .in(minGlobalIndex)
      .in(limit(settings.maxEntitiesPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))
      .description("Get a stream of unspent outputs ordered by global index")

  def streamOutputsByGixDef: Endpoint[(Long, Int), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byGlobalIndex" / "stream")
      .in(minGlobalIndex)
      .in(limit(settings.maxEntitiesPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))
      .description("Get a stream of outputs ordered by global index")

  def streamOutputsByErgoTreeTemplateHashDef
    : Endpoint[(ErgoTreeTemplateHash, HeightRange), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash] / "stream")
      .in(blocksSlicing(settings.maxBlocksPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))
      .description("Get a stream of unspent outputs by a hash of the given ErgoTreeTemplate")

  def streamUnspentOutputsByErgoTreeTemplateHashDef
    : Endpoint[(ErgoTreeTemplateHash, HeightRange), ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byErgoTreeTemplateHash" / path[ErgoTreeTemplateHash] / "stream")
      .in(blocksSlicing(settings.maxBlocksPerRequest))
      .out(streamBody(Fs2Streams[F])(Schema.derived[List[OutputInfo]], CodecFormat.Json(), None))

  def outputsByTokenIdDef: Endpoint[(TokenId, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "byTokenId" / path[TokenId])
      .in(paging(settings.maxEntitiesPerHeavyRequest))
      .out(jsonBody[Items[OutputInfo]])

  def unspentOutputsByTokenIdDef: Endpoint[(TokenId, Paging, SortOrder), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byTokenId" / path[TokenId])
      .in(paging(settings.maxEntitiesPerHeavyRequest))
      .in(ordering)
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

  def getUnspentOutputsByErgoTreeDef: Endpoint[(HexString, Paging, SortOrder), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byErgoTree" / path[HexString])
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
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

  def `getUnspent&UnconfirmedOutputsMergedByAddressDef`
    : Endpoint[(Address, SortOrder), ApiErr, List[MOutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "unconfirmed" / "byAddress" / path[Address])
      .in(ordering)
      .out(jsonBody[List[MOutputInfo]])

  def getUnspentOutputsByAddressDef: Endpoint[(Address, Paging, SortOrder), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byAddress" / path[Address])
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .out(jsonBody[Items[OutputInfo]])

  def searchOutputsDef: Endpoint[(BoxQuery, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.post
      .in(PathPrefix / "search")
      .in(jsonBody[BoxQuery])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])
      .description("Detailed search among all boxes in the chain")

  def searchUnspentOutputsDef: Endpoint[(BoxQuery, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.post
      .in(PathPrefix / "unspent" / "search")
      .in(jsonBody[BoxQuery])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])
      .description("Detailed search among UTXO set")

  def searchUnspentOutputsByTokensUnionDef: Endpoint[(BoxAssetsQuery, Paging), ApiErr, Items[OutputInfo], Any] =
    baseEndpointDef.post
      .in(PathPrefix / "unspent" / "search" / "union")
      .in(jsonBody[BoxAssetsQuery])
      .in(paging(settings.maxEntitiesPerRequest))
      .out(jsonBody[Items[OutputInfo]])
      .description(
        "Search among UTXO set by ergoTreeTemplateHash and tokens. " +
        "The resulted UTXOs will contain at lest one of the given tokens."
      )

  def getAllUnspentOutputsByAddressDef: Endpoint[(Address, Paging, SortOrder), ApiErr, Items[AnyOutputInfo], Any] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "all" / "byAddress" / path[Address])
      .in(paging(settings.maxEntitiesPerRequest))
      .in(ordering)
      .out(jsonBody[Items[AnyOutputInfo]])
}
