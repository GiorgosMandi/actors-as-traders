package gr.gm.industry.messages

import akka.actor.typed.ActorRef
import gr.gm.industry.model.ExecutionReport
import gr.gm.industry.model.orders.FinalizedOrder
import gr.gm.industry.model.orders.PlacedOrder

object BinanceOrderMonitorMessages {
  sealed trait BinanceOrderMonitorMessage
  case object Init extends BinanceOrderMonitorMessage
  final case class WsEvent(report: ExecutionReport) extends BinanceOrderMonitorMessage
  case class RefreshListenKey(key: String) extends BinanceOrderMonitorMessage
  case class ListenKeyFetched(key: String) extends BinanceOrderMonitorMessage
  case class StreamFailure(ex: Throwable) extends BinanceOrderMonitorMessage

  sealed trait BinanceOrderMonitorCommand extends BinanceOrderMonitorMessage
  final case class MonitorOrder(
      placedOrder: PlacedOrder,
      replyTo: ActorRef[FinalizedOrder]
  ) extends BinanceOrderMonitorCommand

}
