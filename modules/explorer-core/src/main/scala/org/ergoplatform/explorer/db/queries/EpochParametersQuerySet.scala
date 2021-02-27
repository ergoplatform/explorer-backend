package org.ergoplatform.explorer.db.queries

import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.explorer.db.models.EpochParameters
import doobie.LogHandler
import doobie.implicits._

object EpochParametersQuerySet extends QuerySet {

  /** Name of the table according to a database schema.
    */
  override val tableName: String = "epochs_parameters"
  /** Table column names listing according to a database schema.
    */
  override val fields: List[String] = List(
    "height",
    "storage_fee_factor",
    "min_value_per_byte",
    "max_block_size",
    "max_block_cost",
    "block_version",
    "token_access_cost",
    "input_cost",
    "data_input_cost",
    "output_cost"
  )

  def getById(id: Int)(implicit lh: LogHandler): Query0[EpochParameters] =
    sql"select * from epochs_parameters where id = $id".query[EpochParameters]

  def getByHeight(height: Int)(implicit lh: LogHandler): Query0[EpochParameters] =
    sql"select * from epochs_parameters where height = $height".query[EpochParameters]
}
