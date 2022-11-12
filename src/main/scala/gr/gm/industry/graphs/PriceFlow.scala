package gr.gm.industry.graphs

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import gr.gm.industry.deciders.RandomDecider
import gr.gm.industry.dao.{CoinGeckoResponse, PriceDao}
import gr.gm.industry.utils.constants.TradeActions
import gr.gm.industry.utils.constants.TradeActions.TradeAction

object PriceFlow {

    val decisionMakerFlow: Flow[PriceDao, TradeAction, NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[PriceDao](2))

            val randomDecider1 = builder.add(Flow[PriceDao].map(price => RandomDecider.decide(price)))
            val randomDecider2 = builder.add(Flow[PriceDao].map(price => RandomDecider.decide(price)))

            val zipDecisions = builder.add(Zip[TradeAction, TradeAction])
            val makeDecisionFlow = builder.add(Flow[Product].map(decisions => TradeActions.elect(decisions)))

            broadcast.out(0) ~> randomDecider1 ~> zipDecisions.in0
            broadcast.out(1) ~> randomDecider2 ~> zipDecisions.in1
            zipDecisions.out ~> makeDecisionFlow

            FlowShape(broadcast.in, makeDecisionFlow.out)
        })

    val priceFlow: Flow[PriceDao, (TradeAction, PriceDao), NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[PriceDao](2))
            val decisionMaker = builder.add(decisionMakerFlow)
            val zip = builder.add(Zip[TradeAction, PriceDao])

            broadcast.out(0) ~> decisionMaker ~> zip.in0
            broadcast.out(1) ~> zip.in1
            FlowShape(broadcast.in, zip.out)
        })


}
