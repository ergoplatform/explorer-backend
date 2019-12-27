package org.ergoplatform.explorer.http.api.v0.models

final case class Items[A](items: List[A], total: Long)
