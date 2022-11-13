package gr.gm.industry.utils

object Constants {

    sealed trait Coin{
        val name = ""
    }

    case object ADA extends Coin{
        override val name = "ADA"
    }

    case object ETH extends Coin {
        override val name = "ETH"
    }

    case object BTC extends Coin {
        override val name = "ADA"
    }

    sealed trait Currency {
        val name = ""
    }

    case object EUR extends Currency {
        override val name = "EUR"
    }

    case object DOL extends Currency {
        override val name = "DOL"
    }
}

