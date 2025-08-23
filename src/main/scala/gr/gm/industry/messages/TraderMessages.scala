package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.model.orders.submitted.{PlacedOrder, SuccessfullyPlacedOrder}

object TraderMessages {

  sealed trait TraderMessage
  sealed trait TraderCommand extends TraderMessage
  final case class PriceUpdate(price: BookTickerPriceDto, replyTo: ActorRef[Option[PlacedOrder]]) extends TraderCommand
  sealed trait OrderPlacementStatus extends TraderMessage
  final case class OrderPlacementFulfilled(placedOrder: SuccessfullyPlacedOrder) extends OrderPlacementStatus
  final case class OrderPlacementFailed(reason: String) extends OrderPlacementStatus


}
