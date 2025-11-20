package gr.gm.industry.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.clients.BinanceHttpClient
import gr.gm.industry.messages.OrderEvents._
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.submitted.PlacedOrder

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object BinanceOrderMonitoringActor {

  /**
   * Monitors Binance user-data stream:
   * - fetches and refreshes the listenKey
   * - opens the WebSocket connection
   * - routes execution reports back to the actor tracking each order
   */
  def apply(binanceHttpClient: BinanceHttpClient): Behavior[OrderEvent] = {

    var tracked: Map[Long, (PlacedOrder, ActorRef[FinalizedOrder])] = Map.empty

    Behaviors.withTimers { timers =>
      Behaviors.setup { context =>
        implicit val ec: ExecutionContextExecutor = context.executionContext
        // kick off initialization
        context.self ! Init

        Behaviors.receiveMessage {
          case Init =>
            context.log.info("Fetching initial listenKey...")
            binanceHttpClient.fetchListenerKey().map(ListenKeyFetched.apply).recover {
              case ex => StreamFailure(ex)
            }.foreach(context.self ! _)
            Behaviors.same

          case ListenKeyFetched(key) =>
            context.log.info(s"Received listenKey: $key. Starting monitorOrders...")
            binanceHttpClient.monitorOrders(key, context.self)
            // schedule refresh every 30m
            timers.startTimerAtFixedRate(RefreshListenKey(key), 30.minutes)
            Behaviors.same

          case RefreshListenKey(key) =>
            context.log.info("Refreshing listenKey...")
            binanceHttpClient.keepAliveListenKey(key).failed.foreach { ex =>
              context.self ! StreamFailure(ex)
            }
            Behaviors.same

          case WsEvent(report) =>
            tracked.get(report.orderId).foreach { case (placedOder, ref) =>
              ref ! FinalizedOrder(placedOder, report)
            }
            Behaviors.same

          case MonitorOrder(placedOrder, replyTo) =>
            context.log.info(s"Monitoring order ${placedOrder.orderId}")
            tracked += (placedOrder.orderId -> (placedOrder, replyTo))
            Behaviors.same

          case StreamFailure(ex) =>
            context.log.error("Stream failure in OrderMonitorActor", ex)
            // could retry by re-fetching listenKey
            context.self ! Init
            Behaviors.same
        }
      }
    }
  }
}
