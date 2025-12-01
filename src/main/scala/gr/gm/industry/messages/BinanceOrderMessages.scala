package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.PlacedOrder

object BinanceOrderMessages {

  // commands
  sealed trait BinanceOrderMessage
  sealed trait OrderPlacementResponse

  final case class PlaceOrder(
                               orderIntent: OrderIntent,
                               replyTo: ActorRef[OrderPlacementResponse]
  ) extends BinanceOrderMessage

  final case class OrderPlacementFulfilled(placedOrder: PlacedOrder) extends OrderPlacementResponse
  final case class OrderPlacementFailed(reason: String) extends OrderPlacementResponse

}
