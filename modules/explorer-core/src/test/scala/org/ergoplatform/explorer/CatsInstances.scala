package org.ergoplatform.explorer

import cats.effect.IO
import doobie.util.ExecutionContexts
import cats.effect.Temporal

trait CatsInstances {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  implicit val ioTimer: Temporal[IO] = IO.timer(ExecutionContexts.synchronous)
}
