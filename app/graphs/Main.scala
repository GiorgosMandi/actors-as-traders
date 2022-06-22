package graphs

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import graphs.actors.CoinGeckoListener

object Main extends App{

    val config = ConfigFactory.parseString(
        """
          |akka {
          |     loglevel="INFO"
          |     stdout-loglevel="INFO"
          | }
        """.stripMargin)

    implicit val system: ActorSystem = ActorSystem("CryptoTrader", ConfigFactory.load(config))
    val priceGraphActor: ActorRef = PriceGraphActor("MainGraphActor", system).graph

    val listener = system.actorOf(Props(classOf[CoinGeckoListener], priceGraphActor), "CoinGeckoListener")

    listener ! CoinGeckoListener.Start(2)
    Thread.sleep(10000)
    listener ! CoinGeckoListener.Stop
}
