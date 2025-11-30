package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink}
import akka.stream.typed.scaladsl.ActorFlow
import akka.util.Timeout
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.messages.BinanceOrderMessages._
import gr.gm.industry.messages.BinanceOrderMonitorMessages.MonitorOrder
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.PlacedOrder
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.model.TradingSymbol

import scala.concurrent.ExecutionContext
import gr.gm.industry.utils.enums.Network
import gr.gm.industry.clients.BinanceHttpClient
import akka.actor.typed.scaladsl.ActorContext
import gr.gm.industry.strategies.RandomStrategy
import gr.gm.industry.actors.traders.GenericTrader
import gr.gm.industry.actors.BinanceOrderMonitoringActor
import gr.gm.industry.actors.BinanceOrderExecutionActor

object BinanceStreamingProcessingGraph {

  /**
   * Builds the end-to-end Akka Stream for live trading on Binance:
   * - subscribes to the Binance `bookTicker` WebSocket for the given symbol,
   * - feeds each price tick to the trader actor, which may emit an order,
   * - forwards placed orders to the order-monitoring actor to await execution reports,
   * - sinks finalized orders (TODO).
   * @param coin             base asset (e.g. BTC).
   * @param currency         quote asset (e.g. USDT).
   * @param net              target Binance network (main/test).
   * @param binanceApiKey    API key for REST + user data stream.
   * @param binanceSecretKey secret key for REST + user data stream.
   */
  def apply(
             coin: Coin,
             currency: Currency,
             net: Network,
             binanceApiKey: String,
             binanceSecretKey: String
           )(
             implicit system: ClassicActorSystemProvider,
             ec: ExecutionContext,
             mat: Materializer,
             timeout: Timeout,
             ac: ActorContext[NotUsed]
           ): RunnableGraph[NotUsed] = {

    val trader = ac.spawn(GenericTrader(RandomStrategy), "generic-trader")

    val binanceHttpClient = new BinanceHttpClient(binanceApiKey, binanceSecretKey, net)
    val binanceOrderExecutionActor = ac.spawn(BinanceOrderExecutionActor(binanceHttpClient), "binance-order-actor")
    val orderMonitorActor = ac.spawn(BinanceOrderMonitoringActor(binanceHttpClient), "order-monitoring-actor")

    val tradingSymbol = TradingSymbol(coin, currency)
    val priceSource = BinanceStreamSource(tradingSymbol, net)
    val traderFlow: Flow[BookTickerPriceDto, Option[OrderIntent], NotUsed] =
      ActorFlow.ask(trader)(PriceUpdate.apply)
    val orderExecutionFlow: Flow[OrderIntent, OrderPlacementResponse, NotUsed] =
      ActorFlow.ask(binanceOrderExecutionActor)(PlaceOrder.apply)
    val orderMonitoringFlow: Flow[PlacedOrder, FinalizedOrder, NotUsed] =
      ActorFlow.ask(orderMonitorActor)(MonitorOrder.apply)

    priceSource
      .via(traderFlow)
      .collect { case Some(orderIntent) => orderIntent }
      .via(orderExecutionFlow)
      .map {
        case OrderPlacementFulfilled(placedOrder) => Some(placedOrder)
        case OrderPlacementFailed(reason) =>
          ac.log.warn(s"Order placement failed: $reason")
          Option.empty[PlacedOrder]
      }
      .collect { case Some(order) => order }
      .via(orderMonitoringFlow)
      .map { finalizedOrder =>
        trader ! OrderFinalized(finalizedOrder)
        finalizedOrder
      }
      .to(Sink.foreach { finalizedOrder =>
        ac.log.info(s"Finalized order ${finalizedOrder.orderId} for ${finalizedOrder.symbol}")
      })
  }
}
