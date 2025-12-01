package gr.gm.industry.actors.traders

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import gr.gm.industry.messages.TraderMessages._
import gr.gm.industry.model.TradeDecision.{NoAction, OrderIntent}
import gr.gm.industry.strategies.Strategy

object GenericTrader extends Trader {

  /** Decides how to trade for each incoming price and emits an order intent if any. */
  override def apply(strategy: Strategy): Behavior[TraderMessage] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case PriceUpdate(price, replyTo) =>
          strategy.decide(price) match {
            case NoAction =>
              context.log.debug(s"No action for price '${price.bestBidPrice}'.")
              replyTo ! Option.empty
              Behaviors.same

            case orderIntent: OrderIntent =>
              replyTo ! Option.apply(orderIntent)
              Behaviors.same

            case _ =>
              Behaviors.unhandled
          }

        case OrderFinalized(finalizedOrder) =>
          // todo manage finalized order
          context.log.info(
            s"Order ${finalizedOrder.orderId} finalized with status ${finalizedOrder.finalStatus}"
          )
          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }
  }
}
