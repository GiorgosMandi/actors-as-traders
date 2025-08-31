package gr.gm.industry.factories

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import gr.gm.industry.utils.enums.binance.StreamType
import gr.gm.industry.utils.enums.binance.StreamType.BookTicker
import gr.gm.industry.utils.model.TradingSymbol

object BinanceUriFactory {

  val BINANCE_API_URL = "https://www.binance.com/api/v3/ticker/bookTicker"
  val BINANCE_WS_URL = "wss://fstream.binance.com"

  def getPriceRestUri(symbol: TradingSymbol): Uri =  Uri(s"$BINANCE_API_URL")
    .withQuery(Query("symbol" -> symbol.toString))

  def getPriceWsUri(symbol: TradingSymbol, streamType: StreamType = BookTicker): Uri =
    Uri(s"$BINANCE_WS_URL/ws/${symbol.toString}@${streamType.name}")

  def getOrderMonitoringWsURI(listenKey: String): String = {
    s"wss://stream.binance.com:9443/ws/$listenKey"
  }

  val BINANCE_USER_DATASTREAM_URI = "https://api.binance.com/api/v3/userDataStream"

  def getBinanceDataStreamWsURI(listenKey: String) = s"wss://stream.binance.com:9443/ws/$listenKey"

}
