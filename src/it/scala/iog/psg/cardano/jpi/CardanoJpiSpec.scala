package iog.psg.cardano.jpi

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.{Configure, ModelCompare}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import scala.jdk.OptionConverters.RichOption


class CardanoJpiSpec extends AnyFlatSpec with Matchers with Configure with ModelCompare with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    sut.deleteWallet(testWallet3Id)
    super.afterAll()
  }

  private val baseUrl = config.getString("cardano.wallet.baseUrl")
  private val testWalletName = config.getString("cardano.wallet.name")
  private val testWallet2Name = config.getString("cardano.wallet2.name")
  private val testWalletMnemonic = config.getString("cardano.wallet.mnemonic")
  private val testWallet2Mnemonic = config.getString("cardano.wallet2.mnemonic")
  private val testWalletId = config.getString("cardano.wallet.id")
  private val testWallet2Id = config.getString("cardano.wallet2.id")
  private val testWalletPassphrase = config.getString("cardano.wallet.passphrase")
  private val testWallet2Passphrase = config.getString("cardano.wallet2.passphrase")
  private val testWallet3Name = config.getString("cardano.wallet3.name")
  private val testWallet3Mnemonic = config.getString("cardano.wallet3.mnemonic")
  private val testWallet3MnemonicSecondary = config.getString("cardano.wallet3.mnemonicsecondary")
  private val testWallet3Id = config.getString("cardano.wallet3.id")
  private val testWallet3Passphrase = config.getString("cardano.wallet3.passphrase")

  private val testAmountToTransfer = config.getString("cardano.wallet.amount")
  private val timeoutValue: Long = 10
  private val timeoutUnits = TimeUnit.SECONDS
  private lazy val sut = new JpiResponseCheck(new CardanoApiFixture(baseUrl).getJpi, timeoutValue, timeoutUnits)

  "NetworkInfo status" should "be 'ready'" in {
    val info = sut.jpi.networkInfo().toCompletableFuture.get(timeoutValue, timeoutUnits)
    val networkState = JpiResponseCheck.get(info)
    networkState shouldBe "ready"
  }

  "Jpi CardanoAPI" should "allow override of execute" in {
    val api = JpiResponseCheck.buildWithDummyApiExecutor()
    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    val wallet = api
      .createRestore(
        testWalletName,
        testWalletPassphrase,
        mnem.mnemonicSentence.asJava,
        None.toJava,
        10
      ).toCompletableFuture.get()

    val networkTip = NetworkTip(
      epochNumber = 3,
      slotNumber = 4,
      height = None,
      absoluteSlotNumber = Some(10)
    )

    val properWallet = Wallet(
      id = "id",
      addressPoolGap = 10,
      balance = Balance(
        available = QuantityUnit(1, Units.lovelace),
        reward = QuantityUnit(1, Units.lovelace),
        total = QuantityUnit(1, Units.lovelace)
      ),
      delegation = Some(
        Delegation(
          active = DelegationActive(
            status = DelegationStatus.delegating,
            target = Some("1234567890")
          ),
          next = List(
            DelegationNext(
              status = DelegationStatus.notDelegating,
              changesAt =
                Some(NextEpoch(epochStartTime = ZonedDateTime.parse("2000-01-02T10:01:02+01:00"), epochNumber = 10))
            )
          )
        )
      ),
      name = "name",
      passphrase = Passphrase(lastUpdatedAt = ZonedDateTime.parse("2000-01-02T10:01:02+01:00")),
      state = SyncStatus(SyncState.ready, None),
      tip = networkTip
    )

    compareWallets(wallet, properWallet)
  }

  "Bad wallet creation" should "be prevented" in {
    an[IllegalArgumentException] shouldBe thrownBy(sut.createBadWallet())
  }

  "Test wallet" should "exist or be created" in {

    val aryLen = testWalletMnemonic.split(" ").length
    val aryLen2 = testWallet2Mnemonic.split(" ").length

    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    sut
      .findOrCreateTestWallet(
        testWalletId,
        testWalletName,
        testWalletPassphrase,
        mnem.mnemonicSentence.asJava, 10) shouldBe true
  }

  it should "get our wallet" in {
    sut.getWallet(testWalletId) shouldBe true
  }

  it should "create r find wallet 2" in {
    val mnem = GenericMnemonicSentence(testWallet2Mnemonic)
    sut
      .findOrCreateTestWallet(
        testWallet2Id,
        testWallet2Name,
        testWallet2Passphrase,
        mnem.mnemonicSentence.asJava, 10) shouldBe true
  }

  it should "allow password change in wallet 2" in {
    sut.passwordChange(testWallet2Id, testWallet2Passphrase, testWalletPassphrase)
    //now this is the wrong password
    an[Exception] shouldBe thrownBy(sut.passwordChange(testWallet2Id, testWallet2Passphrase, testWalletPassphrase))

    sut.passwordChange(testWallet2Id, testWalletPassphrase, testWallet2Passphrase)
  }

  it should "create wallet with secondary factor" in {
    val mnem = GenericMnemonicSentence(testWallet3Mnemonic)
    val mnemSecondary = GenericMnemonicSecondaryFactor(testWallet3MnemonicSecondary)

    val wallet = sut.createTestWallet(
      testWallet3Name,
      testWallet3Passphrase,
      mnem.mnemonicSentence.asJava,
      Some(mnemSecondary.mnemonicSentence.asJava).toJava,
      10)

    wallet.id shouldBe testWallet3Id
  }

  it should "fund payments" in {
    val response = sut.fundPayments(testWalletId, testAmountToTransfer.toInt)

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

    createTxResponse.id shouldBe getTxResponse.id
    createTxResponse.amount shouldBe getTxResponse.amount
    val Right(mapOut) = createTxResponse.metadata.get.json.as[Map[Long, String]]
    mapOut(Long.MaxValue) shouldBe "0" * 64
    mapOut(Long.MaxValue - 1) shouldBe "1" * 64
  }


  it should "delete wallet 2" in {
    sut.deleteWallet(testWallet2Id)
    an[Exception] shouldBe thrownBy(sut.getWallet(testWallet2Id), "Wallet should not be retrieved")
  }

  //TODO estimate fee
}

