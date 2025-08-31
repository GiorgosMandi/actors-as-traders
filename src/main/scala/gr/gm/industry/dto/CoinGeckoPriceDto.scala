package gr.gm.industry.dto

import gr.gm.industry.model.CoinPrice
import gr.gm.industry.utils.enums.Coin.ETH
import gr.gm.industry.utils.enums.Currency.EUR
import gr.gm.industry.utils.model.TradingSymbol
import spray.json.{DeserializationException, JsNumber, JsValue}

import java.time.Instant

case class CoinGeckoPriceDto(price: BigDecimal,
                             marketCap: BigDecimal,
                             volume24h: BigDecimal,
                             change24h: BigDecimal,
                             timestamp: Instant
                           ) {
  def toPrice: CoinPrice = {
    CoinPrice(price, TradingSymbol(ETH, EUR), timestamp)
  }
}

object CoinGeckoPriceDto {
  def apply(jsValuesMap: Map[String, JsValue]): CoinGeckoPriceDto = {
    jsValuesMap.toList match {
      case List(
      ("eur", JsNumber(eur)),
      ("eur_24h_change", JsNumber(change24h)),
      ("eur_24h_vol", JsNumber(volume24h)),
      ("eur_market_cap", JsNumber(marketCap))
      ) => CoinGeckoPriceDto(eur, marketCap, volume24h, change24h, Instant.now())
      case _ => throw DeserializationException(s"Necessary fields were missing. Received $jsValuesMap")
    }
  }
}