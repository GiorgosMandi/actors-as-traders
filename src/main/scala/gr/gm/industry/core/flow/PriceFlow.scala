package gr.gm.industry.core.flow

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import gr.gm.industry.core.deciders.RandomDecider
import gr.gm.industry.model.dao.CoinPrice
import akka.stream.scaladsl.GraphDSL.Implicits._
import gr.gm.industry.core.traders.NaivePendingTrader.TraderEvent
import gr.gm.industry.utils.TradeActions

// TODO remove second argument
object PriceFlow {


    // PriceDao -> [broadcast] -> [randomDecider1]  --> [Zip] -> [makeDecisionFlow] -> out
    //                       \                       /
    //                         -> [randomDecider2] -
    val decisionMakerFlow: Flow[CoinPrice, TraderEvent, NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[CoinPrice](2))
            val randomDecider1 = builder.add(Flow[CoinPrice].map(price => RandomDecider.decide(price)))
            val randomDecider2 = builder.add(Flow[CoinPrice].map(price => RandomDecider.decide(price)))

            val zipDecisions = builder.add(Zip[TraderEvent, TraderEvent])
            val makeDecisionFlow = builder.add(Flow[Product].map(decisions => TradeActions.elect(decisions)))

            broadcast.out(0) ~> randomDecider1 ~> zipDecisions.in0
            broadcast.out(1) ~> randomDecider2 ~> zipDecisions.in1
            zipDecisions.out ~> makeDecisionFlow

            FlowShape(broadcast.in, makeDecisionFlow.out)
        })
}
