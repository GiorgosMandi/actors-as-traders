package graphs

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import decisionMakers.RandomDecider
import utils.{TradeAction, TradeActionT}

object PriceFlow {

    val priceFlow: Flow[Map[String, String], (TradeActionT, Map[String, String]), NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

            val broadcast = builder.add(Broadcast[Map[String, String]](2))
            val getPricesFlow: FlowShape[Map[String, String], Float] =
                builder.add(
                    Flow[Map[String, String]].map(map => map.get("PRICE"))
                      .filter(priceOpt => priceOpt.isDefined)
                      .map(priceStr => priceStr.get.toFloat)
                )
            val broadcastToDeciders =  builder.add(Broadcast[Float](2))
            val randomDecider1 = builder.add(Flow[Float].map(price => RandomDecider.decide(price)))
            val randomDecider2 = builder.add(Flow[Float].map(price => RandomDecider.decide(price)))

            val zipDecisions = builder.add(Zip[TradeActionT, TradeActionT])
            val makeDecisionFlow =  builder.add(Flow[Product].map(decisions => TradeAction.elect(decisions)))
            val zip = builder.add(Zip[TradeActionT, Map[String, String]])

            broadcast.out(0) ~> getPricesFlow ~> broadcastToDeciders
            broadcastToDeciders.out(0) ~> randomDecider1
            zipDecisions.in0 <~ randomDecider1
            broadcastToDeciders.out(1) ~> randomDecider2
            zipDecisions.in1 <~ randomDecider2
            zipDecisions.out ~> makeDecisionFlow ~> zip.in0
            broadcast.out(1) ~> zip.in1

            FlowShape(broadcast.in, zip.out)
        })

}
