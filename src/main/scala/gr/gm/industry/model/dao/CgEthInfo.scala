package gr.gm.industry.model.dao

import spray.json.{DeserializationException, JsNumber, JsValue}

import java.time.LocalDateTime

case class CgEthInfo(price: BigDecimal,
                     marketCap: BigDecimal,
                     volume24h: BigDecimal,
                     change24h: BigDecimal,
                     time: LocalDateTime
                    )

object CgEthInfo {
  def apply(jsValuesMap: Map[String, JsValue]): CgEthInfo = {
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
}