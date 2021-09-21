package iog.psg.cardano.experimental

import iog.psg.cardano.util.ProcessBuilderHelper

package object cli {

  trait CopyShim {
    type CONCRETECASECLASS
    protected def copier: CanCopy[CONCRETECASECLASS]
  }

  type CanCopy[F] = { def copy(c: ProcessBuilderHelper): F }
}
