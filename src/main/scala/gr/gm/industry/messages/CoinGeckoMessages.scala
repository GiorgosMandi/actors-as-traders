package gr.gm.industry.messages

object CoinGeckoMessages {
  // actions
  sealed trait CoinGeckoAction

  case object ListenerKey extends CoinGeckoAction

  case class Start(delay: Int) extends CoinGeckoAction

  case object Stop extends CoinGeckoAction

  case object Hit extends CoinGeckoAction

  case object Pause extends CoinGeckoAction

  case class Error(message: String) extends CoinGeckoAction


}
