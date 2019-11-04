package org.ergoplatform.explorer.persistence.dao

import cats.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.{Read, Write}
import doobie.{ConnectionIO, Update}

/** Database table access operations layer.
  */
trait Dao {

  /** Name of the table according to a database schema.
    */
  val tableName: String

  /** Table column names listing according to a database schema.
    */
  val fields: List[String]

  lazy val tableNameFr: Fragment =
    Fragment.const(tableName)

  /** All fields of a model fragment.
    */
  lazy val fieldsFr: Fragment =
    Fragment.const(fieldsString)

  /** Select fragment of all fields of a model.
    */
  lazy val selectFr: Fragment =
  fr"select" ++ fieldsFr ++ fr"from" ++ tableNameFr

  /** Select fragment of all fields of a model with a given reference symbol `ref`.
    */
  final def selectWithRefFr(ref: String): Fragment =
    fr"select" ++ Fragment.const(fields.map(field => s"$ref.$field").mkString(", ")) ++
      fr"from" ++ tableNameFr ++ fr"$ref"

  def insert[M: Read: Write](m: M): ConnectionIO[M] =
    insert.withUniqueGeneratedKeys[M](fields: _*)(m)

  def insertMany[M: Read: Write](list: List[M]): ConnectionIO[List[M]] =
    insert.updateManyWithGeneratedKeys[M](fields: _*)(list).compile.to[List]

  private def fieldsString: String =
    fields.mkString(", ")

  private def holdersString: String =
    fields.map(_ => "?").mkString(", ")

  private def insert[M: Write]: Update[M] =
    Update[M](s"insert into $tableName ($fieldsString) values ($holdersString)")
}
