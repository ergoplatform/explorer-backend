package org.ergoplatform.explorer.db.queries

import cats.implicits._
import doobie.implicits._
import doobie.util.{Read, Write}
import doobie.{ConnectionIO, Update}

/** Database table access operations layer.
  */
trait QuerySet {

  /** Name of the table according to a database schema.
    */
  val tableName: String

  /** Table column names listing according to a database schema.
    */
  val fields: List[String]

  def insert[M: Read: Write](m: M): ConnectionIO[M] =
    insert.withUniqueGeneratedKeys[M](fields: _*)(m)

  def insertNoConflict[M: Read: Write](m: M): ConnectionIO[M] =
    insertNoConflict.withUniqueGeneratedKeys[M](fields: _*)(m)

  def insertMany[M: Read: Write](list: List[M]): ConnectionIO[List[M]] =
    insert.updateManyWithGeneratedKeys[M](fields: _*)(list).compile.to(List)

  def insertManyNoConflict[M: Read: Write](list: List[M]): ConnectionIO[List[M]] =
    insertNoConflict.updateManyWithGeneratedKeys[M](fields: _*)(list).compile.to(List)

  private def insert[M: Write]: Update[M] =
    Update[M](s"insert into $tableName ($fieldsString) values ($holdersString)")

  private def insertNoConflict[M: Write]: Update[M] =
    Update[M](s"insert into $tableName ($fieldsString) values ($holdersString) on conflict do nothing")

  private def fieldsString: String =
    fields.mkString(", ")

  private def holdersString: String =
    fields.map(_ => "?").mkString(", ")
}
