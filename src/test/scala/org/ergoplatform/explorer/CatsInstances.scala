package org.ergoplatform.explorer

import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext.Implicits

trait CatsInstances {

  implicit val cs: ContextShift[IO] = IO.contextShift(Implicits.global)
}
