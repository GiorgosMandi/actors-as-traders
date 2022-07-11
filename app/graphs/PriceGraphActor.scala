package graphs

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source, Zip}
import akka.stream.{CompletionStrategy, FlowShape, OverflowStrategy}
import akka.{Done, NotUsed}
import decisionMakers.RandomDecider
import graphs.actors.traders.SimpleTrader
import models.CoinGeckoResponse
import utils.constants.TradeAction.{TradeActionT, elect}

import scala.language.postfixOps

case class PriceGraphActor(name: String, implicit val system: ActorSystem) {

    val graph: ActorRef = this.createGraph()
    val trader: ActorRef = system.actorOf(Props[SimpleTrader](), "SimpleTrader")

    val decisionMakerFlow: Flow[CoinGeckoResponse, TradeActionT, NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[CoinGeckoResponse](2))

            val randomDecider1 = builder.add(Flow[CoinGeckoResponse].map(price => RandomDecider.decide(price)))
            val randomDecider2 = builder.add(Flow[CoinGeckoResponse].map(price => RandomDecider.decide(price)))

            val zipDecisions = builder.add(Zip[TradeActionT, TradeActionT]())
            val makeDecisionFlow = builder.add(Flow[Product].map(decisions => elect(decisions)))

            broadcast.out(0) ~> randomDecider1 ~> zipDecisions.in0
            broadcast.out(1) ~> randomDecider2 ~> zipDecisions.in1
            zipDecisions.out ~> makeDecisionFlow

            FlowShape(broadcast.in, makeDecisionFlow.out)
        })

    val priceFlow: Flow[CoinGeckoResponse, (TradeActionT, CoinGeckoResponse), NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[CoinGeckoResponse](2))
            val decisionMaker = builder.add(decisionMakerFlow)
            val zip = builder.add(Zip[TradeActionT, CoinGeckoResponse]())

            broadcast.out(0) ~> decisionMaker ~> zip.in0
            broadcast.out(1) ~> zip.in1
            FlowShape(broadcast.in, zip.out)
        })

    private def createGraph(): ActorRef = {
        val sourceRef: Source[CoinGeckoResponse, ActorRef] = Source.actorRef(
            completionMatcher = {
                case Done => CompletionStrategy.immediately
            },
            failureMatcher = PartialFunction.empty,
            bufferSize = 100,
            overflowStrategy = OverflowStrategy.dropHead
        )

        sourceRef
          .via(priceFlow)
          .to(Sink.foreach { case (tradeAction, response) => trader ! (tradeAction, response.price) })
          .run()
    }
}
