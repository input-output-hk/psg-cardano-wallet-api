package iog.psg.cardano.jpi

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.TestWalletsConfig
import iog.psg.cardano.TestWalletsConfig.baseUrl
import iog.psg.cardano.common.TestWalletFixture
import iog.psg.cardano.util.{Configure, CustomPatienceConfiguration, ModelCompare}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

class CardanoJpiITSpec extends AnyFlatSpec with Matchers with Configure with ModelCompare with BeforeAndAfterAll with CustomPatienceConfiguration {

  override def afterAll(): Unit = {
    sut.deleteWallet(TestWalletsConfig.walletsMap(3).id)
    super.afterAll()
  }

  private val timeoutValue: Long = 10
  private val timeoutUnits = TimeUnit.SECONDS
  private lazy val sut = new JpiResponseCheck(new CardanoApiFixture(baseUrl).getJpi, timeoutValue, timeoutUnits)

  "NetworkInfo status" should "be 'ready'" in {
    val info = sut.jpi.networkInfo().toCompletableFuture.get(timeoutValue, timeoutUnits)
    val networkState = JpiResponseCheck.get(info)
    networkState shouldBe "ready"
  }

  "Jpi CardanoAPI" should "allow override of execute" in new TestWalletFixture(1) {
    val api = JpiResponseCheck.buildWithDummyApiExecutor()
    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    val createdWallet = api
      .createRestore(
        testWalletName,
        testWalletPassphrase,
        mnem.mnemonicSentence.asJava,
        10
      ).toCompletableFuture.get()

    val networkTip = NetworkTip(
      epochNumber = 3,
      slotNumber = 4,
      height = None,
      absoluteSlotNumber = Some(10)
    )

    val expectedWallet = Wallet(
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

    compareWallets(createdWallet, expectedWallet)
  }

  "Bad wallet creation" should "be prevented" in {
    an[IllegalArgumentException] shouldBe thrownBy(sut.createBadWallet())
  }

  "Test wallet" should "exist or be created" in new TestWalletFixture(1) {

    val aryLen = testWalletMnemonic.split(" ").length
    val aryLen2 = TestWalletsConfig.walletsMap(2).mnemonic.split(" ").length

    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    sut
      .findOrCreateTestWallet(
        testWalletId,
        testWalletName,
        testWalletPassphrase,
        mnem.mnemonicSentence.asJava, 10) shouldBe true
  }

  it should "get our wallet" in new TestWalletFixture(1){
    sut.getWallet(testWalletId) shouldBe true
  }

  it should "create r find wallet 2" in new TestWalletFixture(2){
    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    sut
      .findOrCreateTestWallet(
        testWalletId,
        testWalletName,
        testWalletPassphrase,
        mnem.mnemonicSentence.asJava, 10) shouldBe true
  }

  it should "allow password change in wallet 2" in new TestWalletFixture(2) {
    sut.passwordChange(testWalletId, testWalletPassphrase, testWalletPassphrase)
    //now this is the wrong password
    an[Exception] shouldBe thrownBy(sut.passwordChange(testWalletId, testWalletPassphrase.toUpperCase(), testWalletPassphrase))

    sut.passwordChange(testWalletId, testWalletPassphrase, testWalletPassphrase.toUpperCase())
  }

  it should "create wallet with secondary factor" in new TestWalletFixture(3) {
    val mnem = GenericMnemonicSentence(testWalletMnemonic)
    val mnemSecondary = GenericMnemonicSecondaryFactor(testWalletMnemonicSecondary.get)

    val createdWallet = sut.createTestWallet(
      testWalletName,
      testWalletPassphrase,
      mnem.mnemonicSentence.asJava,
      mnemSecondary.mnemonicSentence.asJava,
      10)

    createdWallet.id shouldBe testWalletId
  }

  it should "fund payments" in new TestWalletFixture(1) {
    val response = sut.fundPayments(testWalletId, testAmountToTransfer.get.toInt)

  }

  it should "transact from a to a with metadata" in new TestWalletFixture(1) {

    val metadata: Map[String, String] = Map(
      Long.box(Long.MaxValue).toString -> "0" * 64,
      Long.box(Long.MaxValue - 1).toString -> "1" * 64
    )

    val createTxResponse =
      sut.paymentToSelf(testWalletId, testWalletPassphrase, testAmountToTransfer.get.toInt, metadata.asJava)
    val id = createTxResponse.id
    val getTxResponse = sut.getTx(testWalletId, createTxResponse.id)

    createTxResponse.id shouldBe getTxResponse.id
    createTxResponse.amount shouldBe getTxResponse.amount

    val responseMetadataMap = createTxResponse.metadata.get.toMapMetadataStr.getOrElse(fail("Invalid metadata json."))

    responseMetadataMap(Long.MaxValue) shouldBe MetadataValueStr("0" * 64)
    responseMetadataMap(Long.MaxValue - 1) shouldBe MetadataValueStr("1" * 64)
  }


  it should "delete wallet 2" in new TestWalletFixture(2){
    sut.deleteWallet(testWalletId)
    an[Exception] shouldBe thrownBy(sut.getWallet(testWalletId), "Wallet should not be retrieved")
  }

}

