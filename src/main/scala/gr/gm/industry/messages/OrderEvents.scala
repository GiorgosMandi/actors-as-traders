package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.submitted.PlacedOrder

object OrderEvents {
  sealed trait OrderEvent
  case object Init extends OrderEvent
  final case class WsEvent(report: ExecutionReport) extends OrderEvent
  case class RefreshListenKey(key: String) extends OrderEvent
  case class ListenKeyFetched(key: String) extends OrderEvent
  case class StreamFailure(ex: Throwable) extends OrderEvent

  sealed trait OrderMonitorCommand extends OrderEvent
  final case class MonitorOrder(placedOrder: PlacedOrder,
                                replyTo: ActorRef[FinalizedOrder]
                               )extends OrderMonitorCommand

}
