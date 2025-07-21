package gr.gm.industry.utils.enums.binance

sealed trait StreamType {
  val name = ""
}

object StreamType {

  case object AggTrade extends StreamType {
    override val name = "aggTrade"
  }

  case object BookTicker extends StreamType {
    override val name = "bookTicker"
  }

  case object Trade extends StreamType {
    override val name = "trade"
  }
}
