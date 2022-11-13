package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import gr.gm.industry.api.BinanceWebClient
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.Constants.{ETH, EUR}

import scala.concurrent.duration._

case class BinanceListener(parallelism: Int, throttle: (Int, Int)) {

    def apply(implicit system: ActorSystem): Source[PriceDao, NotUsed] =
        Source.repeat(BinanceWebClient.getPrice(ETH, EUR))
          .throttle(throttle._1, throttle._2.seconds)
          .mapAsync(parallelism)(p => p)
}
