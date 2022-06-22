package models

import io.circe._

case class CoinGeckoResponse(price: Double, marketCap: Double, volume24h: Double, change24h: Double)

object CoinGeckoResponse {

    implicit val decoder: Decoder[CoinGeckoResponse] = Decoder.instance { h =>
        for {
            price <- h.get[Double]("eur")
            marketCap <- h.get[Double]("eur_market_cap")
            volume24h <- h.get[Double]("eur_24h_vol")
            change24h <- h.get[Double]("eur_24h_change")
        } yield CoinGeckoResponse(price, marketCap, volume24h, change24h)
    }
}
