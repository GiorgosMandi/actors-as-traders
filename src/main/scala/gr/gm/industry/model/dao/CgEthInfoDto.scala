package gr.gm.industry.model.dao

import gr.gm.industry.utils.Constants.{Coin, ETH, EUR}
import spray.json.{DeserializationException, JsNumber, JsValue}

import java.time.LocalDateTime

case class CgEthInfoDto(price: BigDecimal,
                        marketCap: BigDecimal,
                        volume24h: BigDecimal,
                        change24h: BigDecimal,
                        timestamp: LocalDateTime
                    ) {
  def toPrice(): CoinPrice= {
    CoinPrice(coin = ETH, currency= EUR, price = price, timestamp = timestamp)
  }
}

object CgEthInfoDto {
  def apply(jsValuesMap: Map[String, JsValue]): CgEthInfoDto = {
    jsValuesMap.toList match {
      case List(
      ("eur", JsNumber(eur)),
      ("eur_24h_change", JsNumber(change24h)),
      ("eur_24h_vol", JsNumber(volume24h)),
      ("eur_market_cap", JsNumber(marketCap))
      ) => CgEthInfoDto(eur, marketCap, volume24h, change24h, LocalDateTime.now())
      case _ => throw DeserializationException(s"Necessary fields were missing. Received $jsValuesMap")
    }
  }
}