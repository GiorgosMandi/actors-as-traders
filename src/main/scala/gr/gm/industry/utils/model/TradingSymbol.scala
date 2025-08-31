package gr.gm.industry.utils.model

import gr.gm.industry.utils.enums.{Coin, Currency}

class TradingSymbol(symbol: String) {
}

object TradingSymbol {

  def apply(symbol: String): TradingSymbol = {
    val coinOpt = Coin.get(symbol.substring(0, 3).toUpperCase())
    val currencyOtp = Currency.get(symbol.substring(3).toUpperCase())
    if (coinOpt.isEmpty || currencyOtp.isEmpty) {
      new TradingSymbol(symbol)
    }
    TradingSymbol(coinOpt.orNull, currencyOtp.orNull)
  }

  def apply(coin:Coin, currency: Currency): TradingSymbol =
    new TradingSymbol(s"${coin.name}${currency.name}")
}
