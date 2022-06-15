package graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, Sink}
import akka.util.ByteString
import utils.TradeActions

import java.nio.file.Paths

object MainGraph extends App {

    object Columns {
        val ID = "id"
        val TIMESTAMP = "TIMESTAMP"
        val PRICE = "VALUE"
    }

    val path: String = "/home/gmandi/Documents/myProjects/High-Frequency-Trading-App/resources/random-prices.csv"
    val delimiter: Byte = '\t'
    val quoteChar: Byte = '\"'
    val escapeChar: Byte = '\\'

    implicit val system: ActorSystem = ActorSystem("PriceListener")

    val file = Paths.get(path)
    val sourceFile = FileIO.fromPath(file)
    val csvParserFlow: Flow[ByteString, List[ByteString], NotUsed] = CsvParsing.lineScanner(delimiter, quoteChar, escapeChar)
    val csvToMapFlow: Flow[List[ByteString], Map[String, String], NotUsed] = CsvToMap.toMapAsStrings()
    val priceFlow = PriceFlow.priceFlow
    val printer = Sink.foreach[(TradeActions, Map[String, String])](t => println(t))

    sourceFile
      .via(csvParserFlow)
      .via(csvToMapFlow)
      .via(Flow[Map[String, String]].take(20))
      .via(priceFlow)
      .to(printer)
      .run()
}
