package gr.gm.industry.actors

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import akka.stream.Materializer
import gr.gm.industry.utils.Constants.BINANCE_WS_URL
import gr.gm.industry.utils.enums.{Coin, Currency}

import scala.concurrent.ExecutionContextExecutor

object BinanceWebSocketClient {

  sealed trait BinanceWebSocketMessage
  case class TradeEvent(price: BigDecimal, quantity: BigDecimal) extends BinanceWebSocketMessage
  case object ConnectionClosed extends BinanceWebSocketMessage


  def apply(coin: Coin,
            currency: Currency,
            handler: BinanceWebSocketMessage => Unit

           ): Behavior[Unit] = {

    // todo - set up streaming
    Behaviors.setup { context =>
      implicit val system: ActorSystem[_] = context.system
      implicit val materializer: Materializer = Materializer(context)
      implicit val ec: ExecutionContextExecutor = context.executionContext
      val logger = context.log

      val priceWs = Uri(s"$BINANCE_WS_URL")
        .withQuery(Query("symbol" -> s"${coin.name}${currency.name}"))
      logger.info(s"url=${priceWs.toString()}")
      Behaviors.same
    }
  }
}
