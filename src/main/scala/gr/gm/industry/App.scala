package gr.gm.industry

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.api.BinanceWebClientActor
import gr.gm.industry.api.BinanceWebClientActor.WebClientRequest
import gr.gm.industry.core.source.BinancePriceSource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object App extends App {


  val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
    implicit val system: ActorSystem[Nothing] = context.system

    val logger = context.log

    val binanceActor: ActorRef[WebClientRequest] = context.spawn(BinanceWebClientActor(), "binanceActor")

    val priceSource = BinancePriceSource(binanceActor, parallelism = 5, throttle = (4, 1))
    //        val decisionMakerFlow = PriceFlow.decisionMakerFlow
    //        val trader: ActorRef = system.actorOf(Props[NaivePendingTrader], "NaivePendingTrader")
    //        val initState = Initialize(200, Nil)
    //        trader ! initState

    priceSource.apply().runForeach(p => logger.info(p.toString))
    //            .via(decisionMakerFlow)
    //            .to(Sink.foreach { tradeAction => trader ! tradeAction })
    //            .run()

    Behaviors.empty
  }

  val conf: Config = ConfigFactory.load()
  val system = ActorSystem(rootBehavior, "hft-app", conf)
  implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())
  system.scheduler.scheduleOnce(10.second, () => system.terminate())
}
