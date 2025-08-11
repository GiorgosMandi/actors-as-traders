package gr.gm.industry

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.actors.CoinGeckoListenerActor
import gr.gm.industry.strategies.RandomStrategy
import gr.gm.industry.streams.BinanceStreamingProcessingGraph
import gr.gm.industry.utils.enums.{Coin, Currency}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object App extends App {

  private def testTradingBehavior(budget: Long = 200): Behavior[NotUsed] = {
    Behaviors.setup { context =>
//      implicit val system: ActorSystem[Nothing] = context.system
//      val binanceActor: ActorRef[BinanceMessage] = context.spawn(BinanceWebClientActor(), "BinanceActor")
//      val priceSource = BinancePriceSource(binanceActor, parallelism = 5, throttle = (4, 1))
//
//      val naiveTrader = context.spawn(NaivePendingTrader(budget, binanceActor), "NaiveTrader")
//      val decisionMakerFlow = PriceFlow.decisionMakerFlow
//      priceSource.apply()
//        .via(decisionMakerFlow)
//        .to(Sink.foreach { tradeAction => naiveTrader ! tradeAction })
//        .run()

      Behaviors.empty
    }
  }

  private def testBinanceWebSocketSource(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      implicit val ec: ExecutionContext = context.executionContext
      implicit val mat: Materializer = Materializer(context)
      val graph = BinanceStreamingProcessingGraph(Coin.BTC, Currency.USDT)
      graph.run()
      Behaviors.empty
    }
  }

  def testCoinGeckoBehavior(delay: Int = 2): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val listener = context.spawn(CoinGeckoListenerActor(RandomStrategy), "CoinGeckoListener")
      listener ! CoinGeckoListenerActor.Start(delay)
      Behaviors.empty
    }
  }

  val selectedBehavior = testBinanceWebSocketSource()
  val conf: Config = ConfigFactory.load()

  val system = ActorSystem(selectedBehavior, "actors-as-traders", conf)
  implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())


  system.scheduler.scheduleOnce(60.second, () => system.terminate())
}
