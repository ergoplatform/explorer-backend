package org.ergoplatform.explorer.syntax

import fs2.Stream

trait StreamOptionSyntax {

  implicit final def toStreamOptionOps[A](oa: Option[A]): StreamOptionOps[A] =
    new StreamOptionOps[A](oa)
}

final class StreamOptionOps[A](oa: Option[A]) {

  def toStream: fs2.Stream[fs2.Pure, A] =
    oa.fold[fs2.Stream[fs2.Pure, A]](Stream.empty)(Stream.emit)
}
