package org.ergoplatform.explorer.db.models.aggregates

final case class TimePoint[A](ts: Long, value: A, dummy: String)
