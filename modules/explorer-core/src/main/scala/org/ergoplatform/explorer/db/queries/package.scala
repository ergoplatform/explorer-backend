package org.ergoplatform.explorer.db

import cats.data.NonEmptyList

package object queries {

  def innerJoinAllOfConstants(as: String, tableAlias: String, props: NonEmptyList[(Int, String)]): String =
    s"""
      |inner join (
      |  select c0.box_id from script_constants c0
      |  ${props.zipWithIndex.tail.map { case (_, ix) => s"inner join script_constants c$ix on c0.box_id = c$ix.box_id" }.mkString("\n")}
      |  where ${props.zipWithIndex.toList.map { case ((i, v), ix) => s"c$ix.index = $i and c$ix.rendered_value = '$v'" }.mkString(" and ")}
      |) $as on $as.box_id = $tableAlias.box_id
      |""".stripMargin
}
