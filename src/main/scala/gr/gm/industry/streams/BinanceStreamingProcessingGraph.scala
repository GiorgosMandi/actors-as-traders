package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.orders.submitted.PlacedOrder
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.model.TradingSymbol

import scala.concurrent.ExecutionContext

object BinanceStreamingProcessingGraph {

  def apply(
             coin: Coin,
             currency: Currency,
             trader: ActorRef[TraderMessage]
           )(
             implicit system: ClassicActorSystemProvider,
             ec: ExecutionContext,
             mat: Materializer,
             timeout: Timeout
           ): RunnableGraph[NotUsed] = {

    val tradingSymbol = TradingSymbol(coin, currency)
    val priceSource = BinanceStreamSource(tradingSymbol)
    val traderFlow: Flow[BookTickerPriceDto, Option[PlacedOrder], NotUsed] =
      ActorFlow.ask(trader)(PriceUpdate.apply)
    // todo - monitor placed orders
    priceSource
      .via(traderFlow)
      .to(Sink.foreach { price => println(s"Price: $price") })
  }
}
