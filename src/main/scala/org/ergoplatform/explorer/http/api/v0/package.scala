package org.ergoplatform.explorer.http.api

import sttp.tapir.EndpointInput
import sttp.tapir._

package object v1 {

  val BasePathPrefix: EndpointInput[Unit] = "api" / "v1"
}
