package org.ergoplatform.explorer

import cats.effect.{ContextShift, IO, Timer}
import doobie.util.ExecutionContexts

trait CatsInstances {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  implicit val ioTimer: Timer[IO] = IO.timer(ExecutionContexts.synchronous)
}
