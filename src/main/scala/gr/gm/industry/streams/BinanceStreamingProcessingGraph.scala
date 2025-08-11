package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink}
import gr.gm.industry.dto.BookTickerPriceDto
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.jsonProtocols.BookTickerPriceProtocol._
import gr.gm.industry.utils.model.TradingSymbol
import spray.json._

import scala.concurrent.ExecutionContext

object BinanceStreamingProcessingGraph {

  def apply(coin: Coin, currency: Currency)(
    implicit system: ClassicActorSystemProvider,
    ec: ExecutionContext,
    mat: Materializer,
  ): RunnableGraph[NotUsed] = {
    val tradingSymbol = TradingSymbol(coin, currency)
    val priceSource = BinanceStreamSource(tradingSymbol)
    val parsePriceFlow = Flow[String].map(_.parseJson.convertTo[BookTickerPriceDto])
    priceSource
      .via(parsePriceFlow)
      .to(Sink.foreach { price => println(s"Price: $price") })
  }
}
