package graphs

import akka.NotUsed
import akka.actor.Status.Success
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.CompletionStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import graphs.actors.listeners.CryptoApiListenerFMS
import graphs.actors.listeners.CryptoApiListenerFMS.{ACK, _}
import graphs.actors.traders.SimpleTrader
import graphs.actors.traders.SimpleTrader._
import models.CoinGeckoResponse
import utils.constants.TradeAction

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

    val config = ConfigFactory.parseString(
        """
          |akka {
          |     loglevel="INFO"
          |     stdout-loglevel="INFO"
          | }
        """.stripMargin)

    implicit val system: ActorSystem = ActorSystem("CryptoTrader", ConfigFactory.load(config))
    implicit val timeout: Timeout = Timeout(2 seconds)

    // actor as the source of the stream
    val actorPoweredSource = Source.actorRefWithBackpressure[ListenerMessages](
        ackMessage = ACK,
        completionMatcher = {
            case _: Success => CompletionStrategy.immediately
        },
        failureMatcher = PartialFunction.empty)

    // listener is a flow Component of the graph
    val listener = system.actorOf(Props(classOf[CryptoApiListenerFMS]), "CoinGeckoListener")
    val listenerFlow = Flow[ListenerMessages].ask[CoinGeckoResponse](parallelism = 4)(listener)

    // flow of the money -> decision making based on the price
    val decisionMaking: Flow[CoinGeckoResponse, (TradeAction.TradeActionT, CoinGeckoResponse), NotUsed] = PriceFlow.flow

    // trader as a sink - all the decisions end up to the trader
    val trader = system.actorOf(Props[SimpleTrader](), "SimpleTrader")
    val traderPoweredSink = Sink.actorRefWithBackpressure(
        trader,
        onInitMessage = Init,
        onCompleteMessage = Terminate,
        ackMessage = ACK,
        onFailureMessage = throwable => Fail(throwable)
    )

    // the graph
    val graphActorMat: ActorRef = actorPoweredSource.via(listenerFlow).via(decisionMaking).to(traderPoweredSink).run()

    graphActorMat ! Initialize(2)
    Thread.sleep(20000)
    graphActorMat ! Stop
}

