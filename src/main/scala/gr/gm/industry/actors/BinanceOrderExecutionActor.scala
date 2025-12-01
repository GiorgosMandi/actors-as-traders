package gr.gm.industry.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.clients.BinanceHttpClient
import gr.gm.industry.messages.BinanceOrderMessages._
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.{FailedPlacedOrder, PlacedOrder, PlacedOrderTrait}
import gr.gm.industry.utils.enums.Side.{BUY, SELL, UNKNOWN}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object BinanceOrderExecutionActor {

  /** Places orders on Binance: receives order intents, calls the HTTP client to
   * submit a limit BUY, and replies to the requester with either a placed
   * order or a failure reason.
   *
   */
  def apply(binanceHttpClient: BinanceHttpClient): Behavior[BinanceOrderMessage] = {
    Behaviors.receive { (context, message) =>
      implicit val ec: ExecutionContextExecutor = context.executionContext
      implicit val ac: ActorContext[BinanceOrderMessage] = context

      message match {
        case PlaceOrder(orderIntent, replyTo) =>
          orderIntent.side match {
            case BUY => placeLimitBuy(replyTo, binanceHttpClient, orderIntent)
            case SELL => placeLimitSell(replyTo, binanceHttpClient, orderIntent)
            case UNKNOWN =>
              val reason = s"Unknown side for order intent ${orderIntent.symbol}"
              context.log.warn(reason)
              replyTo ! OrderPlacementFailed(reason)
          }
          Behaviors.same
      }
    }
  }

  def placeLimitBuy(
                     replyTo: ActorRef[OrderPlacementResponse],
                     binanceHttpClient: BinanceHttpClient,
                     orderIntent: OrderIntent
                   )(implicit
                     ac: ActorContext[BinanceOrderMessage],
                     ec: ExecutionContextExecutor
                   ): Unit = {
    binanceHttpClient.placeLimitBuy(orderIntent).onComplete {
      case Success(order) =>
        respondToOrder(order, replyTo)
      case Failure(reason) =>
        ac.log.warn("Order placement failed", reason)
        replyTo ! OrderPlacementFailed(reason.toString)
    }
  }


  def placeLimitSell(
                      replyTo: ActorRef[OrderPlacementResponse],
                      binanceHttpClient: BinanceHttpClient,
                      orderIntent: OrderIntent
                    )(implicit
                      ac: ActorContext[BinanceOrderMessage],
                      ec: ExecutionContextExecutor
                    ): Unit = {
    binanceHttpClient.placeLimitSell(orderIntent).onComplete {
      case Success(order) =>
        respondToOrder(order, replyTo)
      case Failure(reason) =>
        ac.log.warn("Order placement failed", reason)
        replyTo ! OrderPlacementFailed(reason.toString)
    }
  }

  private def respondToOrder(order: PlacedOrderTrait,
                             replyTo: ActorRef[OrderPlacementResponse])
                            (implicit
                             ac: ActorContext[BinanceOrderMessage]
                            ): Unit =
    order match {
      case placedOrder: PlacedOrder =>
        replyTo ! OrderPlacementFulfilled(placedOrder)
      case failedOrder: FailedPlacedOrder =>
        replyTo ! OrderPlacementFailed(failedOrder.reason)
      case other =>
        // Handle any other unexpected types
        val reason = s"Unexpected order type: ${other.getClass.getName}"
        ac.log.warn(reason)
        replyTo ! OrderPlacementFailed(reason)
    }
}
