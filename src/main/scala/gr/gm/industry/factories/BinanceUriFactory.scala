package gr.gm.industry.factories

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import gr.gm.industry.utils.enums.binance.StreamType
import gr.gm.industry.utils.enums.binance.StreamType.BookTicker
import gr.gm.industry.utils.enums.{Coin, Currency}


object BinanceUriFactory {

  val BINANCE_API_URL = "https://www.binance.com/api/v3/ticker"
  val BINANCE_WS_URL = "wss://fstream.binance.com"

  def getPriceUri(coin: Coin, currency:Currency): Uri =  Uri(s"$BINANCE_API_URL/price")
    .withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))

  def getPriceWsUri(coin: Coin, currency:Currency, streamType: StreamType = BookTicker): Uri =
    Uri(s"$BINANCE_WS_URL/ws/${coin.name.toLowerCase}${currency.name.toLowerCase}@${streamType.name}")
}
