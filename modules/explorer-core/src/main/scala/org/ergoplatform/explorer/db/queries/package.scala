package org.ergoplatform.explorer.db

import cats.data.NonEmptyList
import org.ergoplatform.explorer.{RegisterId, TokenId}

package object queries {

  @inline def innerJoinAllOfConstants(
    as: String,
    tableAlias: String,
    props: NonEmptyList[(Int, String)]
  ): String =
    s"""
      |inner join (
      |  select c0.box_id from script_constants c0
      |  ${props.zipWithIndex.tail
      .map { case (_, ix) => s"inner join script_constants c$ix on c0.box_id = c$ix.box_id" }
      .mkString("\n")}
      |  where ${props.zipWithIndex.toList
      .map { case ((i, v), ix) => s"c$ix.index = $i and c$ix.rendered_value = '$v'" }
      .mkString(" and ")}
      |) as $as on $as.box_id = $tableAlias.box_id
      |""".stripMargin

  @inline def innerJoinAllOfRegisters(
    as: String,
    tableAlias: String,
    props: NonEmptyList[(RegisterId, String)]
  ): String =
    s"""
       |inner join (
       |  select r0.box_id from box_registers r0
       |  ${props.zipWithIndex.tail
      .map { case (_, ix) => s"inner join box_registers r$ix on r0.box_id = r$ix.box_id" }
      .mkString("\n")}
       |  where ${props.zipWithIndex.toList
      .map { case ((id, v), ix) => s"r$ix.id = '$id' and r$ix.rendered_value = '$v'" }
      .mkString(" and ")}
       |) as $as on $as.box_id = $tableAlias.box_id
       |""".stripMargin

  @inline def innerJoinAllOfAssets(
    as: String,
    tableAlias: String,
    props: NonEmptyList[TokenId]
  ): String =
    s"""
       |inner join (
       |  select a0.box_id from node_assets a0
       |  ${props.zipWithIndex.tail
      .map { case (_, ix) => s"inner join node_assets a$ix on a0.box_id = a$ix.box_id" }
      .mkString("\n")}
       |  where ${props.zipWithIndex.toList.map { case (id, ix) => s"a$ix.token_id = '$id'" }.mkString(" and ")}
       |) as $as on $as.box_id = $tableAlias.box_id
       |""".stripMargin
}
