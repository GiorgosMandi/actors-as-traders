package gr.gm.industry.gecko

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import gr.gm.industry.strategies.RandomStrategy
import gr.gm.industry.model.{CoinPrice, TradeDecision}

// TODO remove second argument
object randomTradingWithZipFlow {


    // PriceDao -> [broadcast] -> [randomDecider1]  --> [Zip] -> [makeDecisionFlow] -> out
    //                       \                       /
    //                         -> [randomDecider2] -
    val apply: Flow[CoinPrice, TradeDecision, NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[CoinPrice](2))
//            val randomDecider1 = builder.add(Flow[CoinPrice].map(price => RandomStrategy.decide(price)))
//            val randomDecider2 = builder.add(Flow[CoinPrice].map(price => RandomStrategy.decide(price)))

            val zipDecisions = builder.add(Zip[TradeDecision, TradeDecision])
            val makeDecisionFlow = builder.add(Flow[Product].map(decisions => decisions.productIterator.map(_.asInstanceOf[TradeDecision]).toList.last))

//            broadcast.out(0) ~> randomDecider1 ~> zipDecisions.in0
//            broadcast.out(1) ~> randomDecider2 ~> zipDecisions.in1
//            zipDecisions.out ~> makeDecisionFlow

            FlowShape(broadcast.in, makeDecisionFlow.out)
        })
}
