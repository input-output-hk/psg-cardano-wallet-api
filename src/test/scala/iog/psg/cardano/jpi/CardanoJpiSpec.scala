package iog.psg.cardano.jpi

import java.util.Date
import java.util.concurrent.TimeUnit

import iog.psg.cardano.CardanoApiCodec.GenericMnemonicSentence
import iog.psg.cardano.util.Configure
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}


class CardanoJpiSpec extends AnyFlatSpec with Matchers with Configure {

  private val baseUrl = config.getString("cardano.wallet.baseUrl")
  private val testWalletName = config.getString("cardano.wallet.name")
  private val testWallet2Name = config.getString("cardano.wallet2.name")
  private val testWalletMnemonic = config.getString("cardano.wallet.mnemonic")
  private val testWallet2Mnemonic = config.getString("cardano.wallet2.mnemonic")
  private val testWalletId = config.getString("cardano.wallet.id")
  private val testWallet2Id = config.getString("cardano.wallet2.id")
  private val testWalletPassphrase = config.getString("cardano.wallet.passphrase")
  private val testWallet2Passphrase = config.getString("cardano.wallet2.passphrase")
  private val testAmountToTransfer = config.getString("cardano.wallet.amount")
  private val timeoutValue: Long = 10
  private val timeoutUnits = TimeUnit.SECONDS
  private lazy val sut = new JpiResponseCheck(new CardanoApiFixture(baseUrl).getJpi,timeoutValue, timeoutUnits)

  "NetworkInfo status" should "be 'ready'" in {
    val info = sut.jpi.networkInfo().toCompletableFuture.get(timeoutValue, timeoutUnits)
    val networkState = JpiResponseCheck.get(info)
    assert(networkState == "ready")
  }

  "Bad wallet creation" should "be prevented" in {
    intercept[IllegalArgumentException](sut.createBadWallet())
  }

  "Test wallet" should "exist or be created" in {
    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    assert(sut.findOrCreateTestWallet(testWalletId, testWalletName, testWalletPassphrase, mnem.mnemonicSentence.asJava, 10))
  }

  it should "get our wallet" in {
    assert(sut.getWallet(testWalletId))
  }

  it should "create r find wallet 2" in {
    val mnem = GenericMnemonicSentence(testWallet2Mnemonic)
    assert(sut.findOrCreateTestWallet(testWallet2Id, testWallet2Name, testWallet2Passphrase, mnem.mnemonicSentence.asJava, 10))
  }

  it should "allow password change in wallet 2" in {
    sut.passwordChange(testWallet2Id, testWallet2Passphrase, testWalletPassphrase)
    //now this is the wrong password
    intercept[Exception](sut.passwordChange(testWallet2Id, testWallet2Passphrase, testWalletPassphrase))
    sut.passwordChange(testWallet2Id, testWalletPassphrase, testWallet2Passphrase)
  }

  it should "fund payments" in {
    sut.fundPayments(testWalletId, testAmountToTransfer.toInt)
  }

  it should "transact from a to a with metadata" in {

    val metadata: Map[String, String] = Map(
      Long.box(Long.MaxValue).toString -> "0" * 64,
      Long.box(Long.MaxValue - 1).toString -> "1" * 64
    )

    val createTxResponse =
      sut.paymentToSelf(testWalletId, testWalletPassphrase, testAmountToTransfer.toInt, metadata.asJava)
    val id = createTxResponse.id
    val getTxResponse = sut.getTx(testWalletId, createTxResponse.id)
    assert(createTxResponse.id == getTxResponse.id)
    assert(createTxResponse.amount == getTxResponse.amount)
    assert(createTxResponse.metadata.isDefined)
    assert(createTxResponse.metadata.get.size == 2)
    assert(createTxResponse.metadata.get.apply(Long.MaxValue) == "0" * 64)
    assert(createTxResponse.metadata.get.apply(Long.MaxValue - 1) == "1" * 64)
  }


  it should "delete wallet 2" in {
    sut.deleteWallet(testWallet2Id)
    intercept[Exception](sut.getWallet(testWallet2Id), "Wallet should not be retrieved")
  }
}

