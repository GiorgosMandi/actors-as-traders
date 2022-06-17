package graphs

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, Sink}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import utils.Constants.{Columns, DATASOURCE}
import utils.TradeAction.PRINT
import utils.TradeActionT

import java.nio.file.Paths
import scala.concurrent.duration._
import scala.language.postfixOps

object MainGraph extends App {

    val delimiter: Byte = '\t'
    val quoteChar: Byte = '\"'
    val escapeChar: Byte = '\\'

    val config = ConfigFactory.parseString(
        """
          |akka {
          |     loglevel="INFO"
          |     stdout-loglevel="INFO"
          | }
        """.stripMargin)
    implicit val system: ActorSystem = ActorSystem("PriceListener", ConfigFactory.load(config))
    val trader = system.actorOf(Props[SimpleTrader], "MainTrader")

    val file = Paths.get(DATASOURCE)
    val sourceFile = FileIO.fromPath(file)
    val csvParserFlow: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner(delimiter, quoteChar, escapeChar)
    val csvToMapFlow: Flow[List[ByteString], Map[String, String], NotUsed] = CsvToMap.toMapAsStrings()
    val priceFlow = PriceFlow.priceFlow

    val tradeSink = Sink.foreach[(TradeActionT, Map[String, String])] { case (tradeAction, priceConf) => trader ! (tradeAction, priceConf(Columns.PRICE).toDouble) }

    val printer = Sink.foreach[(TradeActionT, Map[String, String])](t => println(t._1, t._2(Columns.PRICE)))


    sourceFile
      .via(csvParserFlow)
      .via(csvToMapFlow)
      .via(Flow[Map[String, String]].take(1000))
      .via(priceFlow)
      .to(tradeSink)
      .run()

    val cancelPrint = system.scheduler
      .scheduleWithFixedDelay(3 second, 2 second, trader, PRINT)(system.dispatcher)
    Thread.sleep(15000)
    cancelPrint.cancel()
}
