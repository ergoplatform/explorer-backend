package org.ergoplatform.explorer.http.api

import sttp.tapir.EndpointInput
import sttp.tapir._

// TODO document the optioning schema of the API
package object v0 {

  val BasePathPrefix: EndpointInput[Unit] = "api" / "v1"  // TODO shouldn't it be v0?
}
