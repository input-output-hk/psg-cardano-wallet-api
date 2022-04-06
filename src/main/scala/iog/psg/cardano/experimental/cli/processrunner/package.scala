package iog.psg.cardano.experimental.cli

package object processrunner {

  case class BlockingProcessResultException(errorResult: BlockingProcessResult) extends RuntimeException(errorResult.toString)

  case class BlockingProcessResult(
                                    exitValue: Int,
                                    result: List[String],
                                    errors: Option[List[String]]
                                  )



  trait FromBlockingProcessResult[T] {
    def apply(result: BlockingProcessResult): T
  }

  object Ops {

    implicit val asString = new FromBlockingProcessResult[String] {
      override def apply(result: BlockingProcessResult): String = result.result.mkString
    }

    implicit val asStrings = new FromBlockingProcessResult[List[String]] {
      override def apply(result: BlockingProcessResult): List[String] = result.result
    }

    implicit val asUnit = new FromBlockingProcessResult[Unit] {
      override def apply(result: BlockingProcessResult): Unit = ()
    }

    implicit class ToResult(val result: BlockingProcessResult) extends AnyVal {

      def as[T](implicit a: FromBlockingProcessResult[T]): Either[BlockingProcessResultException, T] = {
        toEitherProcessResult.map(a(_))
      }

      def asUnsafe[T](implicit a: FromBlockingProcessResult[T]): T = {
        as[T].fold(throw _, identity)
      }

      def toEitherProcessResult: Either[BlockingProcessResultException, BlockingProcessResult] = {
        if(result.exitValue == 0) {
          Right(result)
        } else {
          Left(BlockingProcessResultException(result))
        }
      }
    }
  }
}
