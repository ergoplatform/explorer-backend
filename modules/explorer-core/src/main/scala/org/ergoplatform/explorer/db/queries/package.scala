package org.ergoplatform.explorer.db

import org.ergoplatform.explorer.db.models.{schema, Asset, Input, Output}

package object queries {

  import schema.ctx._

  implicit val assetsSchemaMeta  = Asset.quillSchemaMeta
  implicit val outputsSchemaMeta = Output.quillSchemaMeta
  implicit val inputsSchemaMeta  = Input.quillSchemaMeta
}
