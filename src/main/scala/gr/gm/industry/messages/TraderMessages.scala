package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.model.TradeDecision.OrderIntent
import gr.gm.industry.model.orders.FinalizedOrder

object TraderMessages {

  sealed trait TraderMessage
  sealed trait TraderCommand extends TraderMessage
  final case class PriceUpdate(
                                price: BookTickerPriceDto,
                                replyTo: ActorRef[Option[OrderIntent]]
                              ) extends TraderCommand
  final case class OrderFinalized(finalizedOrder: FinalizedOrder) extends TraderMessage

}
