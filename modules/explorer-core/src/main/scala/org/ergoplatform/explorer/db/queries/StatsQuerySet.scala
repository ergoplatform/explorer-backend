package org.ergoplatform.explorer.db.queries

import doobie.Query0
import doobie.implicits._
import doobie.util.log.LogHandler

object StatsQuerySet {

  def countUniqueAddrs(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select count(distinct address) from node_outputs
         |""".stripMargin.query
}
