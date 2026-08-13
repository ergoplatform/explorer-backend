package org.ergoplatform.explorer.http.api.v1.models

import io.circe.Json
import sttp.tapir.{Schema, SchemaType}

/**
  * One entry of `additionalRegisters`, as the v1 API actually returns it.
  *
  * The field is carried as raw `io.circe.Json` in the response models, so its documented shape has
  * to be declared by hand. It used to be declared as a plain string per register id, which is what
  * the **v0** API returns - there `registers.convolveJson` flattens every register down to its
  * serialized value. The v1 endpoints hand the expanded form through untouched, so the documented
  * type did not match the response and clients generated from it were wrong.
  *
  * Mirrors `org.ergoplatform.explorer.protocol.models.ExpandedRegister`, which is what the raw JSON
  * decodes to. It is kept separate rather than reused, since this type exists only to describe the
  * response and carries no `HexString` or `SigmaType` refinement.
  */
final case class RegisterInfo(
  serializedValue: String,
  sigmaType: Option[String],
  renderedValue: Option[String]
)

object RegisterInfo {

  implicit val schema: Schema[RegisterInfo] =
    Schema
      .derived[RegisterInfo]
      .modify(_.serializedValue)(_.description("Serialized value of the register"))
      .modify(_.sigmaType)(_.description("Sigma type of the value, absent when it could not be parsed"))
      .modify(_.renderedValue)(_.description("Rendered value of the register, absent when it could not be parsed"))
      .description("Value of a register")

  /** Schema of the `additionalRegisters` field of v1 responses: register id -> expanded register */
  val registersSchema: Schema[Json] =
    Schema(SchemaType.SOpenProduct[Json, RegisterInfo](schema)(_ => Map.empty))
      .description("Registers of the box, by register id")
}
