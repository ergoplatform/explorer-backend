package org.ergoplatform.explorer.http.api.v1

import sttp.tapir._

package object routes {

  val BasePathPrefix: EndpointInput[Unit] = "api" / "v1"
}
