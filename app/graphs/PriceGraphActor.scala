package graphs

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import graphs.actors.traders.SimpleTrader
import models.CoinGeckoResponse

import scala.language.postfixOps

case class PriceGraphActor(name: String, implicit val system: ActorSystem) {

    val graph: ActorRef = this.createGraph()
    val trader: ActorRef = system.actorOf(Props[SimpleTrader], "SimpleTrader")

    private def createGraph(): ActorRef = {
        val sourceRef: Source[CoinGeckoResponse, ActorRef] = Source.actorRef(
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
