package gr.gm.industry.utils.model

import gr.gm.industry.utils.enums.{Coin, Currency}

case class TradingSymbol(coin: Coin, currency: Currency) {

  override def toString: String = s"${coin.name}${currency.name}"
}

object TradingSymbol {

  def apply(tradingSymbol: String): TradingSymbol = {
    val coinOpt = Coin.get(tradingSymbol.substring(0, 3).toUpperCase())
    val currencyOtp = Currency.get(tradingSymbol.substring(3).toUpperCase())
    TradingSymbol(coinOpt.orNull, currencyOtp.orNull)
  }
}
