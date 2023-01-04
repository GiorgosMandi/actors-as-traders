package gr.gm.industry

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.core.traders.{NaivePendingTrader, SimpleTrader}
import gr.gm.industry.core.flow.PriceFlow
import gr.gm.industry.core.source.BinancePriceSource
import gr.gm.industry.core.traders.NaivePendingTrader.Initialize

object App extends App {

    val conf: Config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("CryptoTrader", conf)
    val priceSource = BinancePriceSource(parallelism = 5, throttle = (2, 1))
    val decisionMakerFlow = PriceFlow.decisionMakerFlow
    val trader: ActorRef = system.actorOf(Props[NaivePendingTrader], "NaivePendingTrader")
    val initState = Initialize(200, Nil)
    trader ! initState

    priceSource
      .apply
      .via(decisionMakerFlow)
      .to(Sink.foreach {tradeAction => trader ! tradeAction })
      .run()
}
