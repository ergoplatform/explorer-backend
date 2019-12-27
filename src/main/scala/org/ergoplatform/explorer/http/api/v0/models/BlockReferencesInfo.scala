package org.ergoplatform.explorer.http.api.v0.models

import org.ergoplatform.explorer.Id
import sttp.tapir.Schema
import sttp.tapir.generic.Derived

final case class BlockReferencesInfo(previousId: Id, nextId: Option[Id])

object BlockReferencesInfo {

  implicit val schema: Schema[BlockReferencesInfo] =
    implicitly[Derived[Schema[BlockReferencesInfo]]].value
}
