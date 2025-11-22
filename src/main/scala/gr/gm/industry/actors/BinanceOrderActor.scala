package gr.gm.industry.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import gr.gm.industry.clients.BinanceHttpClient
import gr.gm.industry.messages.OrderMessages._
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.orders.submitted.{FailedPlacedOrder, PlacedOrder}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import gr.gm.industry.utils.enums.Side.BUY
import gr.gm.industry.utils.enums.Side.SELL
import gr.gm.industry.utils.enums.Side.UNKNOWN
import gr.gm.industry.model.TradeDecision.OrderIntent
import akka.actor.typed.ActorRef
import akka.dispatch.ExecutionContexts

object BinanceOrderActor {

  /** Places orders on Binance: receives order intents, calls the HTTP client to
    * submit a limit BUY, and replies to the requester with either a placed
    * order or a failure reason.
    *
    */
  def apply(binanceHttpClient: BinanceHttpClient): Behavior[BinanceOrder] = {
    Behaviors.receive { (context, message) =>
      implicit val ec: ExecutionContextExecutor = context.executionContext



      message match {
        case PlaceOrder(orderIntent, replyTo) =>
          orderIntent.side match {
            case BUY  => placeLimitBuy(replyTo, binanceHttpClient, orderIntent)
            case SELL => placeLimitSell(replyTo, binanceHttpClient, orderIntent)
            case UNKNOWN =>
          }
          Behaviors.same
      }
    }
  }

  def placeLimitBuy(
      replyTo: ActorRef[OrderPlacementStatus],
      binanceHttpClient: BinanceHttpClient,
      orderIntent: OrderIntent
  )(implicit
      ec: ExecutionContextExecutor
  ): Unit = {
    binanceHttpClient.placeLimitBuy(orderIntent).onComplete {
      case Success(order) =>
        order match {
          case placedOrder: PlacedOrder =>
            replyTo ! OrderPlacementFulfilled(placedOrder)
          case failedOrder: FailedPlacedOrder =>
            replyTo ! OrderPlacementFailed(failedOrder.reason)
          case other =>
            // Handle any other unexpected types
            replyTo ! OrderPlacementFailed(
              s"Unexpected order type: ${other.getClass.getName}"
            )
        }
      case Failure(reason) =>
        replyTo ! OrderPlacementFailed(reason.toString)
    }
  }


    def placeLimitSell(
      replyTo: ActorRef[OrderPlacementStatus],
      binanceHttpClient: BinanceHttpClient,
      orderIntent: OrderIntent
  )(implicit
      ec: ExecutionContextExecutor
  ): Unit = {
    binanceHttpClient.placeLimitSell(orderIntent).onComplete {
      case Success(order) =>
        order match {
          case placedOrder: PlacedOrder =>
            replyTo ! OrderPlacementFulfilled(placedOrder)
          case failedOrder: FailedPlacedOrder =>
            replyTo ! OrderPlacementFailed(failedOrder.reason)
          case other =>
            // Handle any other unexpected types
            replyTo ! OrderPlacementFailed(
              s"Unexpected order type: ${other.getClass.getName}"
            )
        }
      case Failure(reason) =>
        replyTo ! OrderPlacementFailed(reason.toString)
    }
  }
}
