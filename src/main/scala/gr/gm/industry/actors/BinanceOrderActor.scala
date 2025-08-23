package gr.gm.industry.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import gr.gm.industry.clients.BinanceHttpClient
import gr.gm.industry.messages.BinanceOrderActorMessages._
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.orders.submitted.{FailedPlacedOrder, SuccessfullyPlacedOrder}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object BinanceOrderActor {

  def apply(binanceHttpClient: BinanceHttpClient): Behavior[BinanceOrder] = {
    Behaviors.receive { (context, message) =>
      implicit val ec: ExecutionContextExecutor = context.executionContext
      message match {
        case PlaceOrder(orderIntent, replyTo) =>
          binanceHttpClient.placeLimitBuy(orderIntent).onComplete {
            case Success(order) =>
              order match {
                case placedOrder: SuccessfullyPlacedOrder =>
                  replyTo ! OrderPlacementFulfilled(placedOrder)
                case failedOrder: FailedPlacedOrder =>
                  replyTo ! OrderPlacementFailed(failedOrder.reason)
                case other =>
                  // Handle any other unexpected types
                  replyTo ! OrderPlacementFailed(s"Unexpected order type: ${other.getClass.getName}")
              }
            case Failure(reason) =>
              replyTo ! OrderPlacementFailed(reason.toString)
          }
          Behaviors.same
      }
    }
  }
}
