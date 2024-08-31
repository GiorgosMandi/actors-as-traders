package gr.gm.industry.model.dao

import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsValue, RootJsonFormat}

import java.time.LocalDateTime

case class CgEthInfo(price: BigDecimal,
                     marketCap: BigDecimal,
                     volume24h: BigDecimal,
                     change24h: BigDecimal,
                     time: LocalDateTime
                    )

object CgEthInfo extends DefaultJsonProtocol {
  implicit object CGEthInfoJsonFormat extends RootJsonFormat[CgEthInfo] {

    def extractPrice(jsValuesMap: Map[String, JsValue]): CgEthInfo = {
      jsValuesMap.toList match {
        case List(
        ("eur", JsNumber(eur)),
        ("eur_24h_change", JsNumber(change24h)),
        ("eur_24h_vol", JsNumber(volume24h)),
        ("eur_market_cap", JsNumber(marketCap))
        ) => CgEthInfo(eur, marketCap, volume24h, change24h, LocalDateTime.now())
        case _ => throw DeserializationException(s"Necessary fields were missing. Received $jsValuesMap")
      }
    }

    def read(json: JsValue): CgEthInfo = {
      json.asJsObject.getFields("ethereum") match {
        case Seq(JsObject(ethMap)) => extractPrice(ethMap)
        case _ => throw DeserializationException("CgEthInfo expected")
      }
    }

    override def write(obj: CgEthInfo): JsValue = ???
  }
}

//"ethereum":{
// "eur":2280.34,
// "eur_market_cap":274647771967.14023,
// "eur_24h_vol":10710838487.83845,
// "eur_24h_change":1.5273056883490965}
// }