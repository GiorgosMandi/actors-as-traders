package utils.constants

sealed trait TradeActionT

object TradeAction {
    case object BUY extends TradeActionT
    case object SELL extends TradeActionT
    case object OMIT extends TradeActionT
    case object PRINT extends TradeActionT

    def elect(tradeActions: Product): TradeActionT ={
        val tradeActionsList = tradeActions.productIterator.map(_.asInstanceOf[TradeActionT]).toList

        val votes = tradeActionsList.foldLeft(Map[TradeActionT,Int]().withDefaultValue(0)){
            case (acc, letter) => acc + (letter -> (1 + acc(letter)))
        }
        votes.maxBy(_._2)._1
    }
}


