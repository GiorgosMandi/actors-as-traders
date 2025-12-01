package gr.gm.industry.actors.traders

import akka.actor.typed.Behavior
import gr.gm.industry.messages.TraderMessages.TraderMessage
import gr.gm.industry.strategies.Strategy


trait Trader {

  def apply(strategy: Strategy): Behavior[TraderMessage]

}
