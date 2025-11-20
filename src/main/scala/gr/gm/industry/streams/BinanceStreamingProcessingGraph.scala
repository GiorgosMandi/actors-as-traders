package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.messages.OrderEvents.{MonitorOrder, OrderEvent}
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.submitted.PlacedOrder
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.model.TradingSymbol

import scala.concurrent.ExecutionContext

object BinanceStreamingProcessingGraph {

  /**
   * Builds the end-to-end Akka Stream for live trading on Binance:
   * - subscribes to the Binance `bookTicker` WebSocket for the given symbol,
   * - feeds each price tick to the trader actor, which may emit an order,
   * - forwards placed orders to the order-monitoring actor to await execution reports,
   * - sinks finalized orders (TODO).
   *
   * @param coin                  base asset (e.g. BTC).
   * @param currency              quote asset (e.g. USDT).
   * @param trader                actor that decides and places orders.
   * @param orderMonitoringActor  actor that tracks execution reports for placed orders.
   */
  def apply(
             coin: Coin,
             currency: Currency,
             trader: ActorRef[TraderMessage],
             orderMonitoringActor: ActorRef[OrderEvent]
           )(
             implicit system: ClassicActorSystemProvider,
             ec: ExecutionContext,
             mat: Materializer,
             timeout: Timeout
           ): RunnableGraph[NotUsed] = {

    val tradingSymbol = TradingSymbol(coin, currency)
    val priceSource = BinanceStreamSource(tradingSymbol)
    val traderFlow: Flow[BookTickerPriceDto, Option[PlacedOrder], NotUsed] = ActorFlow.ask(trader)(PriceUpdate.apply)
    val orderMonitoringActorFlow: Flow[PlacedOrder, FinalizedOrder, NotUsed] = ActorFlow.ask(orderMonitoringActor)(MonitorOrder.apply)

    priceSource
      .via(traderFlow)
      .collect { case Some(order) => order }
      .via(orderMonitoringActorFlow)
      .to(Sink.foreach { fp => println(s"Price: $fp") })
  }
}
