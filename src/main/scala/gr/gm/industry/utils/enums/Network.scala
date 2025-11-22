package gr.gm.industry.utils.enums

sealed trait Network
object Network {
  case object Mainnet extends Network
  case object Testnet extends Network
}