package gr.gm.industry.factories

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import gr.gm.industry.utils.enums.binance.StreamType
import gr.gm.industry.utils.enums.binance.StreamType.BookTicker
import gr.gm.industry.utils.model.TradingSymbol

object BinanceUriFactory {

  val BINANCE_API_URL = "https://www.binance.com/api/v3/ticker"
  val BINANCE_WS_URL = "wss://fstream.binance.com"

  def getPriceUri(symbol: TradingSymbol): Uri =  Uri(s"$BINANCE_API_URL/price")
    .withQuery(Query("symbol" -> symbol.toString))

  def getPriceWsUri(symbol: TradingSymbol, streamType: StreamType = BookTicker): Uri =
    Uri(s"$BINANCE_WS_URL/ws/${symbol.coin.name.toLowerCase}${symbol.currency.name.toLowerCase}@${streamType.name}")

}
