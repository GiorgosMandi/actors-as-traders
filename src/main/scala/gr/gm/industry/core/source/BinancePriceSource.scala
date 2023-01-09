package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.api.BinanceWebClientActor.{PriceRequest, PriceResponse, WebClientRequest, WebClientResponse}
import gr.gm.industry.model.dao.PriceDao
import gr.gm.industry.utils.Constants.{ETH, EUR}

import scala.concurrent.duration._


case class BinancePriceSource(binanceActor: ActorRef[WebClientRequest], parallelism: Int, throttle: (Int, Int)) {
  implicit val timeout: Timeout = 5.second

    def apply(): Source[PriceDao, NotUsed] = {

      val askFlow: Flow[String, WebClientResponse, NotUsed] =
        ActorFlow.ask(binanceActor)(makeMessage = (_: String, replyTo: ActorRef[WebClientResponse]) => PriceRequest(ETH, EUR, replyTo))

      Source.repeat("s")
          .throttle(throttle._1, throttle._2.seconds)
          .via(askFlow)
          .map(p => p.asInstanceOf[PriceResponse].price)
    }
  }