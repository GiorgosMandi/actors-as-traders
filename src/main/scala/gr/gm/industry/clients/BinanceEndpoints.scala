package gr.gm.industry.clients

import gr.gm.industry.utils.enums.binance.StreamType.BookTicker
import akka.http.scaladsl.model.Uri
import gr.gm.industry.utils.enums.binance.StreamType
import gr.gm.industry.utils.model.TradingSymbol

final case class BinanceEndpoints(
    orderHttp: String,
    futuresWs: String,
    userDataStreamHttp: String,
    userDataStreamWs: String
) {
  def getPriceWsUri(
      symbol: TradingSymbol,
      streamType: StreamType = BookTicker
  ): Uri =
    Uri(s"$futuresWs/ws/${symbol.toString}@${streamType.name}")

  def getBinanceDataStreamWsURI(listenKey: String) = s"$userDataStreamWs/$listenKey"

}
