package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import gr.gm.industry.api.BinanceWebClientActor
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.Constants.{ETH, EUR}

import scala.concurrent.duration._

case class BinancePriceSource(parallelism: Int, throttle: (Int, Int)) {
    // TODO: Stream from Binance
    def apply(implicit system: ActorSystem): Source[PriceDao, NotUsed] =
        Source.repeat(BinanceWebClientActor.getPrice(ETH, EUR))
          .throttle(throttle._1, throttle._2.seconds)
          .mapAsync(parallelism)(p => p)
          .map {
              case Right(price) =>
                  Some(price)
              case Left(priceError) =>
                  system.log.warning(s"Received price error: $priceError")
                  None
          }
          .filter(_.isDefined)
          .map(_.get)
}
