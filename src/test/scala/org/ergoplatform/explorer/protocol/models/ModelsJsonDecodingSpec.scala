package org.ergoplatform.explorer.protocol.models

import io.circe.{Decoder, parser}
import org.scalatest.{Matchers, PropSpec}

class ModelsJsonDecodingSpec extends PropSpec with Matchers {

  property("Api full block deserialization") {
    parser.parse(MainNetBlockRaw).flatMap(Decoder[ApiFullBlock].decodeJson) shouldBe 'right
  }

  private lazy val MainNetBlockRaw =
    """
      |{
      |  "header" : {
      |    "extensionId" : "af4c9de8106960b47964d21e6eb2acdad7e3e168791e595f0806ebfb036ee7de",
      |    "difficulty" : "1199990374400",
      |    "votes" : "000000",
      |    "timestamp" : 1561978977137,
      |    "size" : 279,
      |    "stateRoot" : "18b7a08878f2a7ee4389c5a1cece1e2724abe8b8adc8916240dd1bcac069177303",
      |    "height" : 1,
      |    "nBits" : 100734821,
      |    "version" : 1,
      |    "id" : "b0244dfc267baca974a4caee06120321562784303a8a688976ae56170e4d175b",
      |    "adProofsRoot" : "766ab7a313cd2fb66d135b0be6662aa02dfa8e5b17342c05a04396268df0bfbb",
      |    "transactionsRoot" : "93fb06aa44413ff57ac878fda9377207d5db0e78833556b331b4d9727b3153ba",
      |    "extensionHash" : "0e5751c026e543b2e8ab2eb06099daa1d1e5df47778f7787faab45cdf12fe3a8",
      |    "powSolutions" : {
      |      "pk" : "03be7ad70c74f691345cbedba19f4844e7fc514e1188a7929f5ae261d5bb00bb66",
      |      "w" : "02da9385ac99014ddcffe88d2ac5f28ce817cd615f270a0a5eae58acfb9fd9f6a0",
      |      "n" : "000000030151dc63",
      |      "d" : 46909460813884299753486408728361968139945651324239558400157099627
      |    },
      |    "adProofsId" : "cfc4af9743534b30ef38deec118a85ce6f0a3741b79b7d294f3e089c118188dc",
      |    "transactionsId" : "fc13e7fd2d1ddbd10e373e232814b3c9ee1b6fbdc4e6257c288ecd9e6da92633",
      |    "parentId" : "0000000000000000000000000000000000000000000000000000000000000000"
      |  },
      |  "blockTransactions" : {
      |    "headerId" : "b0244dfc267baca974a4caee06120321562784303a8a688976ae56170e4d175b",
      |    "transactions" : [
      |      {
      |        "id" : "4c6282be413c6e300a530618b37790be5f286ded758accc2aebd41554a1be308",
      |        "inputs" : [
      |          {
      |            "boxId" : "b69575e11c5c43400bfead5976ee0d6245a1168396b2e2a4f384691f275d501c",
      |            "spendingProof" : {
      |              "proofBytes" : "",
      |              "extension" : {
      |
      |              }
      |            }
      |          }
      |        ],
      |        "dataInputs" : [
      |        ],
      |        "outputs" : [
      |          {
      |            "boxId" : "71bc9534d4a4fe8ff67698a5d0f29782836970635de8418da39fee1cd964fcbe",
      |            "value" : 93409065000000000,
      |            "ergoTree" : "101004020e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a7017300730110010204020404040004c0fd4f05808c82f5f6030580b8c9e5ae040580f882ad16040204c0944004c0f407040004000580f882ad16d19683030191a38cc7a7019683020193c2b2a57300007473017302830108cdeeac93a38cc7b2a573030001978302019683040193b1a5730493c2a7c2b2a573050093958fa3730673079973089c73097e9a730a9d99a3730b730c0599c1a7c1b2a5730d00938cc7b2a5730e0001a390c1a7730f",
      |            "assets" : [
      |            ],
      |            "creationHeight" : 1,
      |            "additionalRegisters" : {
      |
      |            }
      |          },
      |          {
      |            "boxId" : "45dc27302332bcb93604ae63c0a543894b38af31e6aebdb40291e3e8ecaef031",
      |            "value" : 67500000000,
      |            "ergoTree" : "100204a00b08cd03be7ad70c74f691345cbedba19f4844e7fc514e1188a7929f5ae261d5bb00bb66ea02d192a39a8cc7a70173007301",
      |            "assets" : [
      |            ],
      |            "creationHeight" : 1,
      |            "additionalRegisters" : {
      |
      |            }
      |          }
      |        ],
      |        "size" : 341
      |      }
      |    ],
      |    "size" : 374
      |  },
      |  "extension" : {
      |    "headerId" : "b0244dfc267baca974a4caee06120321562784303a8a688976ae56170e4d175b",
      |    "digest" : "0e5751c026e543b2e8ab2eb06099daa1d1e5df47778f7787faab45cdf12fe3a8",
      |    "fields" : [
      |    ]
      |  },
      |  "adProofs" : {
      |    "headerId" : "b0244dfc267baca974a4caee06120321562784303a8a688976ae56170e4d175b",
      |    "proofBytes" : "0200000000000000000000000000000000000000000000000000000000000000005527430474b673e4aafb08e0079c639de23e6a17e87edd00f78662b43c88aeda0000000002b69575e11c5c43400bfead5976ee0d6245a1168396b2e2a4f384691f275d501c0000012a80d6d0c7cfdad807100e040004c094400580809cde91e7b0010580acc7f03704be944004808948058080c7b7e4992c0580b4c4c32104fe884804c0fd4f0580bcc1960b04befd4f05000400ea03d192c1b2a5730000958fa373019a73029c73037e997304a305958fa373059a73069c73077e997308a305958fa373099c730a7e99730ba305730cd193c2a7c2b2a5730d00d50408000000010e6f98040483030808cd039bb5fe52359a64c99a60fd944fc5e388cbdc4d37ff091cc841c3ee79060b864708cd031fb52cf6e805f80d97cde289f4f757d49accf0c83fb864b27d2cf982c37f9a8b08cd0352ac2a471339b0d23b3d2c5ce0db0e81c969f77891b9edf0bda7fd39a78184e70000000000000000000000000000000000000000000000000000000000000000000002b8ce8cfe331e5eadfb0783bdc375c94413433f65e1e45857d71550d42e4d83bd0000011180bac28bc7e3f6a501101004020e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a7017300730110010204020404040004c0fd4f05808c82f5f6030580b8c9e5ae040580f882ad16040204c0944004c0f407040004000580f882ad16d19683030191a38cc7a7019683020193c2b2a57300007473017302830108cdeeac93a38cc7b2a573030001978302019683040193b1a5730493c2a7c2b2a573050093958fa3730673079973089c73097e9a730a9d99a3730b730c0599c1a7c1b2a5730d00938cc7b2a5730e0001a390c1a7730f000000000000000000000000000000000000000000000000000000000000000000000000031b3c7ef0d25f3c71bbc4b55d4dbafb1f5172a498f884c562cece96243282534d00000416",
      |    "digest" : "766ab7a313cd2fb66d135b0be6662aa02dfa8e5b17342c05a04396268df0bfbb",
      |    "size" : 784
      |  },
      |  "size" : 1437
      |}
      |""".stripMargin
}
