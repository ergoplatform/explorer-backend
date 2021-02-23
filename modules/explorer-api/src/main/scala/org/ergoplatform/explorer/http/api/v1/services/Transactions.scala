package org.ergoplatform.explorer.http.api.v1.services

import fs2.Stream
import org.ergoplatform.explorer.ErgoTreeTemplateHash
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.repositories.{InputRepo, OutputRepo, TransactionRepo}
import org.ergoplatform.explorer.http.api.models.Items
import org.ergoplatform.explorer.http.api.v1.models.TransactionInfo

trait Transactions[F[_]] {

  def getByInputsScriptTemplate(template: ErgoTreeTemplateHash): F[Items[TransactionInfo]]
}

object Transactions {

  final class Live[F[_], D[_]](
    inputs: InputRepo[D],
    outputs: OutputRepo[D, Stream],
    transactions: TransactionRepo[D, Stream]
  )(trans: D Trans F)
    extends Transactions[F] {

    def getByInputsScriptTemplate(template: ErgoTreeTemplateHash): F[Items[TransactionInfo]] = ???
  }
}
