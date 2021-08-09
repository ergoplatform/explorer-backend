package org.ergoplatform.explorer.tracker

import java.util.concurrent.TimeUnit
import cats.effect.Clock
import cats.instances.list._
import cats.syntax.traverse._
import cats.{Applicative, Functor, Monad}
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.db.models._
import org.ergoplatform.explorer.protocol.models.ApiTransaction
import org.ergoplatform.explorer.protocol.{registers, sigma}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.ergoplatform.explorer.{Address, BuildFrom}
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.{Throws, WithContext}

object extractors {

  implicit def utxBuildFrom[F[_]: Functor: Clock]: BuildFrom[F, ApiTransaction, UTransaction] =
    BuildFrom.instance { apiTx =>
      Clock[F].realTime(TimeUnit.MILLISECONDS).map(ts => UTransaction(apiTx.id, ts, apiTx.size))
    }

  implicit def uInputsBuildFrom[F[_]: Applicative]: BuildFrom[F, ApiTransaction, List[UInput]] =
    BuildFrom.pure { apiTx =>
      apiTx.inputs.toList.zipWithIndex.map { case (apiIn, i) =>
        UInput(
          apiIn.boxId,
          apiTx.id,
          i,
          apiIn.spendingProof.proofBytes,
          apiIn.spendingProof.extension
        )
      }
    }

  implicit def uDataInputsBuildFrom[F[_]: Applicative]: BuildFrom[F, ApiTransaction, List[UDataInput]] =
    BuildFrom.pure { apiTx =>
      apiTx.dataInputs.zipWithIndex.map { case (apiIn, i) =>
        UDataInput(apiIn.boxId, apiTx.id, i)
      }
    }

  implicit def uOutputsBuildFrom[F[_]: Monad: Throws: WithContext[*[_], ProtocolSettings]]
    : BuildFrom[F, ApiTransaction, List[UOutput]] =
    BuildFrom.instance { apiTx =>
      context.flatMap { protocolSettings =>
        implicit val e: ErgoAddressEncoder = protocolSettings.addressEncoder
        apiTx.outputs.toList.zipWithIndex.traverse { case (apiOut, idx) =>
          for {
            address <- sigma
                         .ergoTreeToAddress[F](apiOut.ergoTree)
                         .map(_.toString)
                         .flatMap(Address.fromString[F])
            scriptTemplate <- sigma.deriveErgoTreeTemplateHash[F](apiOut.ergoTree)
            registersJson = registers.expand(apiOut.additionalRegisters).asJson
          } yield UOutput(
            apiOut.boxId,
            apiTx.id,
            apiOut.value,
            apiOut.creationHeight,
            idx,
            apiOut.ergoTree,
            scriptTemplate,
            address,
            registersJson
          )
        }
      }
    }

  implicit def uAssetsBuildFrom[F[_]: Applicative]: BuildFrom[F, ApiTransaction, List[UAsset]] =
    BuildFrom.pure { apiTx =>
      apiTx.outputs.toList
        .flatMap { o =>
          o.assets.zipWithIndex.map { case (asset, i) => (o.boxId, asset, i) }
        }
        .map { case (boxId, apiAsset, index) =>
          UAsset(apiAsset.tokenId, boxId, index, apiAsset.amount)
        }
    }
}
