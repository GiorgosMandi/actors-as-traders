package graphs

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import models.CoinGeckoResponse

import scala.language.postfixOps

case class PriceGraphActor(name: String, implicit val system: ActorSystem) {
    val graph: ActorRef = this.createGraph()

    private def createGraph(): ActorRef = {
        val sourceRef: Source[CoinGeckoResponse, ActorRef] = Source.actorRef(
            completionMatcher = {
                case Done => CompletionStrategy.immediately
            },
            failureMatcher = PartialFunction.empty,
            bufferSize = 100,
            overflowStrategy = OverflowStrategy.dropHead)
        val priceFlow = PriceFlow.priceFlow
        val printer = Sink.foreach[CoinGeckoResponse](t => (s"OMIT ${t.price}"))

        sourceRef
          .to(printer)
          .run()
    }
}
