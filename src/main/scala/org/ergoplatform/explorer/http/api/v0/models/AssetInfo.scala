package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.TokenId
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class AssetInfo(tokenId: TokenId, amount: Long)

object AssetInfo {

  implicit val schema: Schema[AssetInfo] =
    implicitly[Derived[Schema[AssetInfo]]].value
}
