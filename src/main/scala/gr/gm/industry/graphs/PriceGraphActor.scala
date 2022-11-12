package gr.gm.industry.graphs

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import gr.gm.industry.dao.{CoinGeckoResponse, PriceDao}
import gr.gm.industry.graphs.actors.SimpleTrader

import scala.language.postfixOps

case class PriceGraphActor(name: String, implicit val system: ActorSystem) {

    val graph: ActorRef = this.createGraph()
    val trader: ActorRef = system.actorOf(Props[SimpleTrader], "SimpleTrader")

    private def createGraph(): ActorRef = {
        val sourceRef: Source[PriceDao, ActorRef] = Source.actorRef(
            completionMatcher = {
                case Done => CompletionStrategy.immediately
            },
            failureMatcher = PartialFunction.empty,
            bufferSize = 100,
            overflowStrategy = OverflowStrategy.dropHead
        )
        val priceFlow = PriceFlow.priceFlow

        sourceRef
          .via(priceFlow)
          .to(Sink.foreach { case (tradeAction, response) => trader ! (tradeAction, response.price) })
          .run()
    }
}
