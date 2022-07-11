package graphs

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import graphs.actors.listeners.CryptoApiListenerFMS
import graphs.actors.listeners.CryptoApiListenerFMS._
import utils.constants.TradeAction.PRINT

object Main extends App {

    val config = ConfigFactory.parseString(
        """
          |akka {
          |     loglevel="INFO"
          |     stdout-loglevel="INFO"
          | }
        """.stripMargin)

    implicit val system: ActorSystem = ActorSystem("CryptoTrader", ConfigFactory.load(config))


    val priceGraph: PriceGraphActor = PriceGraphActor("MainGraphActor", system)
    val priceGraphActor = priceGraph.graph
    val listener = system.actorOf(Props(classOf[CryptoApiListenerFMS], priceGraphActor), "CoinGeckoListener")
    listener ! Initialize(2)
    Thread.sleep(20000)
    listener ! Stop
    //    val listener = system.actorOf(Props(classOf[CoinGeckoListener], priceGraphActor), "CoinGeckoListener")
    //    listener ! CoinGeckoListener.Start(2)
    //    Thread.sleep(20000)
    //    listener ! CoinGeckoListener.Stop
    priceGraph.trader ! PRINT
}

