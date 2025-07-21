package gr.gm.industry.streams

import akka.NotUsed
import akka.actor.ClassicActorSystemProvider
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink}
import gr.gm.industry.dto.BookTickerPrice
import gr.gm.industry.streams.sources.BinanceStreamSource
import gr.gm.industry.utils.enums.{Coin, Currency}
import gr.gm.industry.utils.jsonProtocols.BookTickerPriceProtocol._
import spray.json._

import scala.concurrent.ExecutionContext

object BinanceStreamingProcessingGraph {

  def apply(coin: Coin, currency: Currency)(
    implicit system: ClassicActorSystemProvider,
    ec: ExecutionContext,
    mat: Materializer,
  ): RunnableGraph[NotUsed] = {
    val priceSource = BinanceStreamSource(coin, currency)
    val parsePriceFlow = Flow[String].map(_.parseJson.convertTo[BookTickerPrice])
    priceSource
      .via(parsePriceFlow)
      .to(Sink.foreach { price => println(s"Price: $price") })
  }
}
