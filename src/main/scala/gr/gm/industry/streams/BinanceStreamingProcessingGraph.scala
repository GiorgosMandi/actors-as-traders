package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.stream.Materializer
import akka.stream.scaladsl.{RunnableGraph, Sink}
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.model.TradingSymbol

import scala.concurrent.ExecutionContext

object BinanceStreamingProcessingGraph {

  def apply(coin: Coin, currency: Currency)(
    implicit system: ClassicActorSystemProvider,
    ec: ExecutionContext,
    mat: Materializer,
  ): RunnableGraph[NotUsed] = {
    val tradingSymbol = TradingSymbol(coin, currency)
    val priceSource = BinanceStreamSource(tradingSymbol)
    priceSource
      .to(Sink.foreach { price => println(s"Price: $price") })
  }
}
