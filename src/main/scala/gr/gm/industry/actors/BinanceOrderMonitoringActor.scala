package gr.gm.industry.actors

import akka.actor.typed.ActorRef
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.submitted.SuccessfullyPlacedOrder

class BinanceOrderMonitoringActor {

  sealed trait OrderMonitorCommand
  final case class MonitorOrder(placedOrder: SuccessfullyPlacedOrder,
                                replyTo: ActorRef[Option[FinalizedOrder]]
                               )

}
