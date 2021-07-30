package org.ergoplatform.explorer.protocol

import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.explorer.protocol.TxValidation.RuleViolation

trait TxValidation {

  def validate(tx: ErgoLikeTransaction): List[RuleViolation]
}

object TxValidation {

  type RuleViolation = String

  object PartialSemanticValidation extends TxValidation {

    def validate(tx: ErgoLikeTransaction): List[RuleViolation] = {
      val validErgs =
        if (tx.outputs.forall(bx => bx.value > 0L)) None
        else Some("nanoERG amounts must be positive")
      val validTokens =
        if (tx.outputs.forall(bx => bx.additionalTokens.forall { case (_, amt) => amt > 0L })) None
        else Some("Token amounts must be positive")
      List(validErgs, validTokens).flatten
    }
  }
}
