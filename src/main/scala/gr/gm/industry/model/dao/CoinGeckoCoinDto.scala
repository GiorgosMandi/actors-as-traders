package gr.gm.industry.model.dao

import gr.gm.industry.utils.enums.Coin.ETH
import gr.gm.industry.utils.enums.Currency.EUR
import spray.json.{DeserializationException, JsNumber, JsValue}

import java.time.LocalDateTime

case class CoinGeckoCoinDto(price: BigDecimal,
                            marketCap: BigDecimal,
                            volume24h: BigDecimal,
                            change24h: BigDecimal,
                            timestamp: LocalDateTime
                           ) {
  def toPrice: CoinPrice = {
    CoinPrice(Option.empty, coin = ETH, currency = EUR, price = price, timestamp = timestamp)
  }
}

object CoinGeckoCoinDto {
  def apply(jsValuesMap: Map[String, JsValue]): CoinGeckoCoinDto = {
    jsValuesMap.toList match {
      case List(
      ("eur", JsNumber(eur)),
      ("eur_24h_change", JsNumber(change24h)),
      ("eur_24h_vol", JsNumber(volume24h)),
      ("eur_market_cap", JsNumber(marketCap))
      ) => CoinGeckoCoinDto(eur, marketCap, volume24h, change24h, LocalDateTime.now())
      case _ => throw DeserializationException(s"Necessary fields were missing. Received $jsValuesMap")
    }
  }
}