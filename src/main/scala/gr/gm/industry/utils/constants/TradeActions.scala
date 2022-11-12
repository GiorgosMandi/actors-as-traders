package gr.gm.industry.utils.constants

object TradeActions {
    sealed trait TradeAction

    case object BUY extends TradeAction
    case object SELL extends TradeAction
    case object OMIT extends TradeAction
    case object PRINT extends TradeAction

    def elect(tradeActions: Product): TradeAction ={
        val tradeActionsList = tradeActions.productIterator.map(_.asInstanceOf[TradeAction]).toList

        val votes = tradeActionsList.foldLeft(Map[TradeAction,Int]().withDefaultValue(0)){
            case (acc, letter) => acc + (letter -> (1 + acc(letter)))
        }
        votes.maxBy(_._2)._1
    }
}


