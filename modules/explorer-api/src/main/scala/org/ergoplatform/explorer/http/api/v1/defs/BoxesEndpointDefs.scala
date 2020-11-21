package org.ergoplatform.explorer.http.api.v1.defs

import org.ergoplatform.explorer.TokenId
import org.ergoplatform.explorer.http.api.ApiErr
import org.ergoplatform.explorer.http.api.commonDirectives._
import org.ergoplatform.explorer.http.api.models.{Epochs, Items, Paging}
import org.ergoplatform.explorer.http.api.v1.models.OutputInfo
import org.ergoplatform.explorer.settings.RequestsSettings
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
    Nil

  def streamUnspentOutputsDef: Endpoint[Epochs, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent")
      .in(epochSlicing(settings.maxEpochsPerRequest))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def streamUnspentOutputsByEpochsDef: Endpoint[Int, ApiErr, fs2.Stream[F, Byte], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "unspent" / "byLastEpochs")
      .in(query[Int]("lastEpochs").validate(Validator.custom(validateEpochs(_, settings.maxEpochsPerRequest))))
      .out(streamBody(Fs2Streams[F], schemaFor[OutputInfo], CodecFormat.Json(), None))

  def outputsByTokenIdDef: Endpoint[(TokenId, Paging), ApiErr, Items[OutputInfo], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byTokenId" / path[TokenId])
      .in(paging)
      .out(jsonBody[Items[OutputInfo]])

  def unspentOutputsByTokenIdDef: Endpoint[(TokenId, Paging), ApiErr, Items[OutputInfo], Fs2Streams[F]] =
    baseEndpointDef.get
      .in(PathPrefix / "byTokenId" / path[TokenId] / "unspent")
      .in(paging)
      .out(jsonBody[Items[OutputInfo]])

  private def validateEpochs(numEpochs: Int, max: Int): List[ValidationError[_]] =
    if (numEpochs > max)
      ValidationError.Custom(
        numEpochs,
        s"To many epochs requested. Max allowed number is $max"
      ) :: Nil
    else Nil
}
