package graphs

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import utils.TradeActions
import utils.TradeActions.{BUY, OMIT, SELL}

import scala.util.Random

object PriceFlow {

    val priceFlow: Flow[Map[String, String], (TradeActions, Map[String, String]), NotUsed] =
        Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

            val broadcast = builder.add(Broadcast[Map[String, String]](2))
            val zip = builder.add(Zip[TradeActions, Map[String, String]])
            val actionDecider = () => new Random().nextInt(new java.util.Date().hashCode) % 6 match {
                case 1 => BUY
                case 2 => SELL
                case _ => OMIT
            }
            val actionDeciderFlow = builder.add(Flow[Map[String, String]].map(map => actionDecider()))

            broadcast.out(0) ~> actionDeciderFlow ~> zip.in0
            broadcast.out(1) ~> zip.in1

            FlowShape(broadcast.in, zip.out)
        })

}
