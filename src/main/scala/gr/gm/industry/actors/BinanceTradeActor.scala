package gr.gm.industry.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.model.dao.Order.Order

import java.util.UUID
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

object BinanceTradeActor {

  // supported messages
  sealed trait BinanceOrder
  sealed trait BinanceOrderRequest extends BinanceOrder
  sealed trait BinanceOrderResponse extends BinanceOrder

  final case class OrderRequest(order: Order, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderRequest
  final case class OrderPlaced(order: Order, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderResponse
  final case class OrderPlacementFailed(error: String, id: UUID, replyTo: ActorRef[BinanceOrder]) extends BinanceOrderResponse
  final case class OrderFulfilled(id: UUID) extends BinanceOrderResponse

  private def placeOrder(order: Order)
                        (implicit dispatcher: ExecutionContextExecutor):
  Future[Order] = Future {
    // todo - implement
    Thread.sleep(Random.nextInt(8) * 1000)
    order
  }

  private def handleRequest(request: BinanceOrderRequest,
                            context: ActorContext[BinanceOrder],
                   ): Behavior[BinanceOrder] = {
    implicit val ec: ExecutionContextExecutor = context.executionContext
    request match {
      case OrderRequest(order, replyTo) =>
        val orderF = placeOrder(order)
        context.pipeToSelf(orderF) {
          case Success(order) =>
            OrderPlaced(order, replyTo)
          case Failure(reason) =>
            OrderPlacementFailed(reason.toString, order.id, replyTo)
        }
        Behaviors.same
    }
  }

  private def handleResponse(response: BinanceOrderResponse,
                             context: ActorContext[BinanceOrder]
                    ): Behavior[BinanceOrder] = {
    response match {
      case OrderPlaced(order, replyTo) =>
        replyTo ! OrderFulfilled(order.id)
        Behaviors.same

      case OrderPlacementFailed(msg, orderId, replyTo) =>
        context.log.warn(s"Order failed with message: $msg")
        // replyTo ! OrderFailed(orderId)
        Behaviors.same
    }
  }

  def apply(): Behavior[BinanceOrder] =
    Behaviors.receive { (context, message) =>
      message match {
        case request: BinanceOrderRequest =>
          handleRequest(request, context)
        case response: BinanceOrderResponse =>
          handleResponse(response, context)
      }
    }
}
