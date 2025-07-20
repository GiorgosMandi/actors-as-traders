package gr.gm.industry.core.source

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.actors.BinanceWebClientActor.{BinanceMessage, BinancePriceRequest, PriceResponse}
import gr.gm.industry.model.dao.CoinPrice
import gr.gm.industry.utils.enums.Coin.ETH
import gr.gm.industry.utils.enums.Currency.EUR

import scala.concurrent.duration._


case class BinancePriceSource(binanceActor: ActorRef[BinanceMessage],
                              parallelism: Int,
                              throttle: (Int, Int)
                             ) {
  implicit val timeout: Timeout = 5.second

    def apply(): Source[CoinPrice, NotUsed] = {
      val makeMessage = (_: String, replyTo: ActorRef[BinanceMessage]) => BinancePriceRequest(ETH, EUR, replyTo)
      val askFlow: Flow[String, BinanceMessage, NotUsed] = ActorFlow
        .ask(binanceActor)(makeMessage)

      Source.repeat("s")
          .throttle(throttle._1, throttle._2.seconds)
          .via(askFlow)
          .map(p => p.asInstanceOf[PriceResponse].price)
    }
  }