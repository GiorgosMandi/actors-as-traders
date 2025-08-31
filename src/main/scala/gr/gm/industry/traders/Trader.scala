package gr.gm.industry.traders

import akka.actor.typed.{ActorRef, Behavior}
import gr.gm.industry.messages.OrderMessages.BinanceOrder
import gr.gm.industry.messages.TraderMessages.TraderMessage
import gr.gm.industry.strategies.Strategy


trait Trader {

  def apply(strategy: Strategy, binanceActor: ActorRef[BinanceOrder]): Behavior[TraderMessage]

}
