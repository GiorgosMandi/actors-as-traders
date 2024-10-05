package gr.gm.industry

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import gr.gm.industry.api.BinanceWebClientActor
import gr.gm.industry.api.BinanceWebClientActor.{BinanceMessage, RepetitivePriceRequest}
import gr.gm.industry.core.deciders.RandomDecider
import gr.gm.industry.core.flow.PriceFlow
import gr.gm.industry.core.source.{BinancePriceSource, CoinGeckoListener}
import gr.gm.industry.core.traders.NaivePendingTrader
import gr.gm.industry.core.traders.NaivePendingTrader.TraderEvent
import gr.gm.industry.utils.Constants.{Coin, Currency, ETH, EUR}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object App extends App {

  def testTradingBehavior(budget: Long = 200): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      val binanceActor: ActorRef[BinanceMessage] = context.spawn(BinanceWebClientActor(), "BinanceActor")
      val naiveTrader: ActorRef[TraderEvent] = context.spawn(NaivePendingTrader(budget, binanceActor), "NaiveTrader")
      val priceSource = BinancePriceSource(binanceActor, parallelism = 5, throttle = (4, 1))
      val decisionMakerFlow = PriceFlow.decisionMakerFlow
      priceSource.apply()
        .via(decisionMakerFlow)
        .to(Sink.foreach { tradeAction => naiveTrader ! tradeAction })
        .run()

      Behaviors.empty
    }
  }

  def testCoinGeckoBehavior(delay: Int = 2): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val listener = context.spawn(CoinGeckoListener(RandomDecider), "CoinGeckoListener")
      listener ! CoinGeckoListener.Start(delay)
      Behaviors.empty
    }
  }

  def testBinanceBehavior(delay: Int = 2): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      val binanceWebClientActor = context.spawn(BinanceWebClientActor(), "BinanceWebClientActor")
      binanceWebClientActor ! RepetitivePriceRequest(ETH, EUR, binanceWebClientActor, delay)
      Behaviors.empty
    }
  }

  val selectedBehavior = testBinanceBehavior()
  val conf: Config = ConfigFactory.load()
  val system = ActorSystem(selectedBehavior, "hft-app", conf)

  implicit val ec: ExecutionContext = system.dispatchers.lookup(DispatcherSelector.default())
  system.scheduler.scheduleOnce(60.second, () => system.terminate())
}
