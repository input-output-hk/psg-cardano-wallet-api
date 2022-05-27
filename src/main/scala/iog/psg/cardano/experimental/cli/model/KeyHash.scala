package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.api.{IsKeyHash, KeyType}



case class KeyHash[A <: KeyType] private[cli] (content: String) extends IsKeyHash[A]
