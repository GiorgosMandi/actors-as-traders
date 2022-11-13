package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import gr.gm.industry.model.dao
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.model.dto.PriceDto
import gr.gm.industry.utils.Constants.{ETH, EUR}

import scala.concurrent.duration._

case class BinanceListener(parallelism: Int, throttle: (Int, Int)) {
    val BINANCE_URL = "https://www.binance.com/api/v3/ticker/price"

    val binanceRequest: HttpRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(BINANCE_URL).withQuery(Query("symbol" -> s"${ETH.name}${EUR.name}"))
    )

    def apply(implicit system: ActorSystem): Source[PriceDao, NotUsed] =
        Source.repeat(binanceRequest)
          .throttle(throttle._1, throttle._2.seconds)
          .mapAsync(parallelism)(req => Http().singleRequest(req))
          .mapAsync(parallelism)(response => Unmarshal(response).to[PriceDto])
          .map(priceDto => dao.PriceDao(priceDto))
}
