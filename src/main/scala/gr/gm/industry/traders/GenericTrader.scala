package gr.gm.industry.traders

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import gr.gm.industry.messages.OrderMessages._
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.TradeDecision.{NoAction, OrderIntent}
import gr.gm.industry.model.orders.submitted.PlacedOrder
import gr.gm.industry.strategies.Strategy

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GenericTrader extends Trader {

  /**
   * Generic trader actor that delegates decision-making to a `Strategy` and submits resulting orders.
   *
   * For each inbound price update:
   * - ask the strategy for a `TradeDecision`
   * - if it returns an `OrderIntent`, forward it to the Binance order actor and reply with the placed order
   * - otherwise reply with `None`
   */
  override def apply(strategy: Strategy, binanceActor: ActorRef[BinanceOrder]): Behavior[TraderMessage] = {
    Behaviors.setup { context =>
      implicit val c: ActorContext[TraderMessage] = context
      implicit val ec: ExecutionContext = context.executionContext
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val timeout: Timeout = 3.seconds

      Behaviors.receiveMessage {

        case PriceUpdate(price, replyTo) =>
          strategy.decide(price) match {
            case NoAction =>
              context.log.debug(s"No action for price '${price.bestBidPrice}'.")
              replyTo ! Option.empty
              Behaviors.same

            case orderIntent: OrderIntent =>
              placeOrder(orderIntent, binanceActor).onComplete {
                case Success(po) => replyTo ! po
                case Failure(reason) =>
                  context.log.warn(s"Order placement failed with '$reason'")
                  replyTo ! Option.empty[PlacedOrder]
              }
              Behaviors.same

            case _ =>
              Behaviors.unhandled
          }
      }
    }
  }


  private def placeOrder(
                          orderIntent: OrderIntent,
                          binanceActor: ActorRef[BinanceOrder]
                        )
                        (
                          implicit context: ActorContext[TraderMessage],
                          ec: ExecutionContext,
                          system: ActorSystem[Nothing],
                          timeout: Timeout
                        ): Future[Option[PlacedOrder]] = {
    // Send order to Binance actor
    binanceActor.ask(replyTo => PlaceOrder(orderIntent, replyTo))
      .map {
        case OrderPlacementFulfilled(placedOrder) =>
          context.log.debug(s"Order with id: ${placedOrder.orderId} was placed to the market.")
          Option.apply(placedOrder)
        case OrderPlacementFailed(reason) =>
          context.log.warn(s"Order placement failed with '$reason'")
          Option.empty[PlacedOrder]
      }
  }
}
