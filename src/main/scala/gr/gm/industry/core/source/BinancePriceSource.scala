package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.api.BinanceWebClientActor.{BinanceMessage, BinanceRequest, PriceRequest, PriceResponse, RepetitivePriceRequest}
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.Constants.{ETH, EUR}

import scala.concurrent.duration._


case class BinancePriceSource(binanceActor: ActorRef[BinanceMessage],
                              parallelism: Int,
                              throttle: (Int, Int)
                             ) {
  implicit val timeout: Timeout = 5.second

    def apply(): Source[PriceDao, NotUsed] = {
      val askFlow: Flow[String, BinanceMessage, NotUsed] = ActorFlow
        .ask(binanceActor)(makeMessage = (_: String, replyTo: ActorRef[BinanceMessage]) => PriceRequest(ETH, EUR, replyTo))

      Source.repeat("s")
          .throttle(throttle._1, throttle._2.seconds)
          .via(askFlow)
          .map(p => p.asInstanceOf[PriceResponse].price)
    }
  }