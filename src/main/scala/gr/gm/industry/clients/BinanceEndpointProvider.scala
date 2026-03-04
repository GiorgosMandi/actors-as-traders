package gr.gm.industry.clients

import gr.gm.industry.utils.enums.Network

object BinanceEndpointProvider {

  def endpoints(net: Network): BinanceEndpoints = net match {

    case Network.Mainnet =>
      BinanceEndpoints(
        orderHttp = "https://api.binance.com/api/v3/order",
        futuresWs = "wss://stream.binance.com:9443/ws",
        timeHttp = "https://api.binance.com/api/v3/time",
        userDataStreamHttp = "https://api.binance.com/api/v3/userDataStream",
        userDataStreamWs = "wss://stream.binance.com:9443/ws"
      )

    case Network.Testnet =>
      BinanceEndpoints(
        orderHttp = "https://testnet.binance.vision/api/v3/order",
        futuresWs = "wss://stream.binancefuture.com/ws",
        timeHttp = "https://testnet.binance.vision/api/v3/time",
        userDataStreamHttp =
          "https://testnet.binance.vision/api/v3/userDataStream",
        userDataStreamWs = "wss://testnet.binance.vision/ws"
      )
  }

  // Helper if you want the full WS with key:
  def userDataStreamWs(base: String, listenKey: String): String =
    s"$base/$listenKey"
}
