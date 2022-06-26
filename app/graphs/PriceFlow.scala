package graphs

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import decisionMakers.RandomDecider
import models.CoinGeckoResponse
import utils.constants.{TradeAction, TradeActionT}

object PriceFlow {

    val decisionMakerFlow: Flow[CoinGeckoResponse, TradeActionT, NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

            val broadcast = builder.add(Broadcast[CoinGeckoResponse](2))

            val randomDecider1 = builder.add(Flow[CoinGeckoResponse].map(price => RandomDecider.decide(price)))
            val randomDecider2 = builder.add(Flow[CoinGeckoResponse].map(price => RandomDecider.decide(price)))

            val zipDecisions = builder.add(Zip[TradeActionT, TradeActionT])
            val makeDecisionFlow = builder.add(Flow[Product].map(decisions => TradeAction.elect(decisions)))

            broadcast.out(0) ~> randomDecider1 ~> zipDecisions.in0
            broadcast.out(1) ~> randomDecider2 ~> zipDecisions.in1
            zipDecisions.out ~> makeDecisionFlow

            FlowShape(broadcast.in, makeDecisionFlow.out)
        })

    val priceFlow: Flow[CoinGeckoResponse, (TradeActionT, CoinGeckoResponse), NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder =>

        val broadcast = builder.add(Broadcast[CoinGeckoResponse](2))
        val decisionMaker = builder.add(decisionMakerFlow)
        val zip = builder.add(Zip[TradeActionT, CoinGeckoResponse])

        broadcast.out(0) ~> decisionMaker ~> zip.in0
        broadcast.out(1) ~> zip.in1
        FlowShape(broadcast.in, zip.out)
    })


}
