package org.ergoplatform.explorer.settings

/** Database credentials and settings.
  */
final case class DbSettings(url: String, user: String, pass: String, cpSize: Int)
