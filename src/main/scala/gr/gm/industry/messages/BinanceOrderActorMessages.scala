package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.messages.TraderMessages.OrderPlacementStatus
import gr.gm.industry.model.TradeDecision.OrderIntent

object BinanceOrderActorMessages {

  // supported messages
  sealed trait BinanceOrder

  // commands
  sealed trait BinanceOrderCommands extends BinanceOrder

  final case class PlaceOrder(orderIntent: OrderIntent, replyTo: ActorRef[OrderPlacementStatus]) extends BinanceOrderCommands


}
