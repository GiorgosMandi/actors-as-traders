package gr.gm.industry.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.clients.BinanceHttpClient
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.submitted.{FailedPlacedOrder, PlacedOrder}

import scala.util.{Failure, Success}

object BinanceTradeActor {

  // supported messages
  sealed trait BinanceOrder
  sealed trait BinanceOrderRequest extends BinanceOrder
  final case class BuyOrderRequest(orderIntent: OrderIntent, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderRequest
  sealed trait BinanceOrderResponse extends BinanceOrder
  // responses
  final case class OrderPlaced(placedOrder: PlacedOrder, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderResponse
  final case class OrderFailed(msg: String, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderResponse

  // responses to externals
  final case class OrderPlacementFailed(error: String) extends BinanceOrderResponse
  final case class OrderPlacementFulfilled(placedOrder: PlacedOrder) extends BinanceOrderResponse



  private def handleRequest(request: BinanceOrderRequest,
                            binanceHttpClient: BinanceHttpClient
                   )(context: ActorContext[BinanceOrder]): Behavior[BinanceOrder] = {
    request match {
      case BuyOrderRequest(orderIntent, replyTo) =>
        context.pipeToSelf(binanceHttpClient.placeLimitBuy(orderIntent)) {
          case Success(order) =>
            order match {
              case placedOrder: PlacedOrder =>
                OrderPlaced(placedOrder, replyTo)
              case failedOrder: FailedPlacedOrder =>
                OrderFailed(failedOrder.error, replyTo)
              case other =>
                // Handle any other unexpected types
                OrderFailed(s"Unexpected order type: ${other.getClass.getName}", replyTo)
            }
          case Failure(reason) =>
            OrderFailed(reason.toString, replyTo)
        }
        Behaviors.same
    }
  }

  private def handleResponse(response: BinanceOrderResponse)
                            (context: ActorContext[BinanceOrder])
  : Behavior[BinanceOrder] = {
    response match {
      case OrderPlaced(order, replyTo) =>
        replyTo ! OrderPlacementFulfilled(order)
        Behaviors.same

      case OrderFailed(msg, replyTo) =>
        context.log.warn(s"Order failed with message: $msg")
         replyTo ! OrderPlacementFailed(msg)
        Behaviors.same
    }
  }

  def apply(binanceHttpClient: BinanceHttpClient): Behavior[BinanceOrder] = {
    Behaviors.receive { (context, message) =>
      implicit val c: ActorContext[BinanceOrder] = context
      message match {
        case request: BinanceOrderRequest =>
          handleRequest(request, binanceHttpClient)(c)
        case response: BinanceOrderResponse =>
          handleResponse(response)(c)
      }
    }
  }
}
