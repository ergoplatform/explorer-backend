package org.ergoplatform.explorer.db.repositories

import org.ergoplatform.explorer.BoxId
import org.ergoplatform.explorer.db.models.UAsset

/** [[UAsset]] data access operations.
 */
trait UAssetRepo[D[_]] {

  /** Put a given `asset` to persistence.
   */
  def insert(asset: UAsset): D[Unit]

  /** Put a given list of assets to persistence.
   */
  def insertMany(assets: List[UAsset]): D[Unit]

  /** Get all assets belonging to a given `boxId`.
   */
  def getAllByBoxId(boxId: BoxId): D[List[UAsset]]
}
