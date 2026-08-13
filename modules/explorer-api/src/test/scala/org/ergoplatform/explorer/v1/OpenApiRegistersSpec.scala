package org.ergoplatform.explorer.v1

import org.ergoplatform.explorer.http.api.v1.defs._
import org.ergoplatform.explorer.settings.RequestsSettings
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._

/** The v1 API returns `additionalRegisters` in the expanded form, one object per register:
  *
  * {{{
  *   "additionalRegisters": {
  *     "R4": { "serializedValue": "0e0f...", "sigmaType": "Coll[SByte]", "renderedValue": "..." }
  *   }
  * }}}
  *
  * It used to be documented as a plain string per register - which is what the **v0** API returns,
  * since `registers.convolveJson` flattens each register there - so clients generated from the v1
  * document did not match the responses (issue #258).
  */
class OpenApiRegistersSpec extends AnyFlatSpec with should.Matchers {

  private val settings = RequestsSettings(100, 100, 100)

  private val v1Docs: String =
    OpenAPIDocsInterpreter()
      .toOpenAPI(
        new TransactionsEndpointDefs(settings).endpoints ++
        new BoxesEndpointDefs(settings).endpoints ++
        new MempoolEndpointDefs().endpoints,
        "Ergo Explorer API v1",
        "1.0"
      )
      .toYaml

  it should "document v1 additionalRegisters as objects rather than strings" in {
    v1Docs should include("RegisterInfo")
    v1Docs should include("serializedValue")
    v1Docs should include("renderedValue")
    v1Docs should include("sigmaType")
  }

  it should "not describe a register as a bare string any more" in {
    // the old declaration produced `additionalProperties: {type: string}` right under the field
    val registersDeclaration = """additionalRegisters:
                                 |          type: object
                                 |          additionalProperties:
                                 |            type: string""".stripMargin
    v1Docs should not include registersDeclaration
  }

}
