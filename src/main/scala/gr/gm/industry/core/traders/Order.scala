package gr.gm.industry.core.traders

import gr.gm.industry.utils.Constants.{Coin, Currency}

sealed trait Order {
  val coin: Coin
  val currency: Currency
  val price: BigDecimal
  val quantity: BigDecimal
}

case class Buy(coin: Coin, currency: Currency, price: BigDecimal, quantity: BigDecimal) extends Order

case class Sell(coin: Coin, currency: Currency, price: BigDecimal, quantity: BigDecimal) extends Order



