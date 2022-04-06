package iog.psg.cardano.experimental.cli.processrunner

import iog.psg.cardano.experimental.cli.processrunner.Ops._
import iog.psg.cardano.experimental.cli.util.ProcessBuilderHelper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BlockingProcessRunnerSpec extends AnyFlatSpec with Matchers {

  val ls = "ls"
  val nonExistentFile = "THISWILLNOTEXISTINTHEROOTFOLDER"

  val goodListing = ProcessBuilderHelper(command = Vector(ls))
  val badListing = ProcessBuilderHelper(command = Vector(ls, nonExistentFile))

  "A successful ls process" should "return a list of files" in {
    BlockingProcessRunner(goodListing.processBuilder).toEitherProcessResult should matchPattern {
      case Right (BlockingProcessResult(0, result, None)) if result.nonEmpty =>
    }
  }

  "A successful ls process" should "return a list of strings safely" in {
    BlockingProcessRunner(goodListing.processBuilder).as[List[String]] should matchPattern {
      case Right (strs: List[String]) if strs.nonEmpty =>
    }
  }

  "An unsuccessful ls process" should "return an error and some error info" in {
    BlockingProcessRunner(badListing.processBuilder).as[Unit] should matchPattern {
      case Left (BlockingProcessResultException(result)) if result.exitValue != 0 && result.errors.nonEmpty =>
    }
  }

  "A successful ls process" should "return a list of files even if used unsafely" in {
    val stringResult = BlockingProcessRunner(goodListing.processBuilder).asUnsafe[String]
    stringResult.nonEmpty shouldBe true
  }

  "A successful ls process" should "return a list of files even if used unsafely (unit)" in {
    BlockingProcessRunner(goodListing.processBuilder).asUnsafe[Unit] shouldBe ()
  }

  "A successful ls process" should "return a list of files even if used unsafely (strings)" in {
    val stringsResult = BlockingProcessRunner(goodListing.processBuilder).asUnsafe[List[String]]
    stringsResult.nonEmpty shouldBe true
  }

  "A unsuccessful ls process" should "throw exception if used unsafely (strings)" in {
    intercept[BlockingProcessResultException] {
      BlockingProcessRunner(badListing.processBuilder).asUnsafe[List[String]]
    }
  }

}
