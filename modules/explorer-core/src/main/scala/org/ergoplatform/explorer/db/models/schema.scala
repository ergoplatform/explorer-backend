package org.ergoplatform.explorer.db.models

import io.getquill.{idiom => _, _}

object schema {

  import doobie.quill.DoobieContext

  val ctx = new DoobieContext.Postgres(Literal) // Literal naming scheme
}
