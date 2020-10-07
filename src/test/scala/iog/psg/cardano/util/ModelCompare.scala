package iog.psg.cardano.util

import iog.psg.cardano.codecs.CardanoApiCodec._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait ModelCompare extends Matchers {

  final def compareInAddress(decoded: InAddress, expected: InAddress): Assertion = {
    decoded.address shouldBe expected.address
    compareQuantityUnitOpts(decoded.amount, expected.amount)
    decoded.id shouldBe expected.id
    decoded.index shouldBe expected.index
  }

  final def compareOutAddress(decoded: OutAddress, expected: OutAddress): Assertion = {
    decoded.address shouldBe expected.address
    compareQuantityUnit(decoded.amount, expected.amount)
  }

  final def compareInputs(decoded: Seq[InAddress], expected: Seq[InAddress]): Seq[Assertion] =
    decoded.zip(expected).map {
      case (decodedAddress, expectedAddress) => compareInAddress(decodedAddress, expectedAddress)
    }

  final def compareOutputs(decoded: Seq[OutAddress], expected: Seq[OutAddress]): Seq[Assertion] =
    decoded.zip(expected).map {
      case (decodedAddress, expectedAddress) => compareOutAddress(decodedAddress, expectedAddress)
    }

  final def compareFundPaymentsResponse(decoded: FundPaymentsResponse, expected: FundPaymentsResponse): Seq[Assertion] = {
    compareInputs(decoded.inputs, expected.inputs)
    compareOutputs(decoded.outputs, expected.outputs)
  }

  final def compareEstimateFeeResponse(decoded: EstimateFeeResponse, expected: EstimateFeeResponse): Assertion = {
    compareQuantityUnit(decoded.estimatedMax, expected.estimatedMax)
    compareQuantityUnit(decoded.estimatedMin, expected.estimatedMin)
  }

  final def compareStakeAddress(decoded: StakeAddress, expected: StakeAddress): Assertion = {
    compareQuantityUnit(decoded.amount, expected.amount)
    decoded.stakeAddress shouldBe expected.stakeAddress
  }

  final def compareStakeAddresses(decoded: Seq[StakeAddress], expected: Seq[StakeAddress]): Seq[Assertion] = {
    decoded.zip(expected).map {
      case (decodedAddress, expectedAddress) => compareStakeAddress(decodedAddress, expectedAddress)
    }
  }

  final def compareTransaction(decoded: CreateTransactionResponse, expected: CreateTransactionResponse): Assertion = {
    decoded.id shouldBe expected.id
    compareQuantityUnit(decoded.amount, expected.amount)
    decoded.insertedAt shouldBe expected.insertedAt
    decoded.pendingSince shouldBe expected.pendingSince
    decoded.depth shouldBe expected.depth
    decoded.direction shouldBe expected.direction
    compareInputs(decoded.inputs, expected.inputs)
    compareOutputs(decoded.outputs, expected.outputs)
    compareStakeAddresses(decoded.withdrawals, expected.withdrawals)
    decoded.status shouldBe expected.status
    decoded.metadata shouldBe expected.metadata
  }

  final def compareAddress(decoded: WalletAddressId, expected: WalletAddressId): Assertion = {
    decoded.id shouldBe expected.id
    decoded.state shouldBe expected.state
  }

  final def compareNetworkInformation(decoded: NetworkInfo, expected: NetworkInfo): Assertion = {
    decoded.nextEpoch shouldBe expected.nextEpoch
    decoded.nodeTip shouldBe expected.nodeTip
    decoded.networkTip shouldBe expected.networkTip
    decoded.syncProgress.status.toString shouldBe expected.syncProgress.status.toString

    compareQuantityUnitOpts(decoded.syncProgress.progress, expected.syncProgress.progress)
  }

  final def compareQuantityUnitOpts(decoded: Option[QuantityUnit], expected: Option[QuantityUnit]): Assertion = {
    if (decoded.isEmpty && expected.isEmpty) assert(true)
    else (for {
      decodedQU <- decoded
      expectedQU <- expected
    } yield compareQuantityUnit(decodedQU, expectedQU)).getOrElse(assert(false, "one of units is none"))
  }

  final def compareQuantityUnit(decoded: QuantityUnit, expected: QuantityUnit): Assertion = {
    decoded.unit.toString shouldBe expected.unit.toString
    decoded.quantity shouldBe expected.quantity
  }

  final def compareBalance(decoded: Balance, expected: Balance): Assertion = {
    decoded.available.quantity shouldBe expected.available.quantity
    decoded.available.unit.toString shouldBe expected.available.unit.toString

    decoded.reward.quantity shouldBe expected.reward.quantity
    decoded.reward.unit.toString shouldBe expected.reward.unit.toString

    decoded.total.quantity shouldBe expected.total.quantity
    decoded.total.unit.toString shouldBe expected.total.unit.toString
  }

  final def compareState(decoded: SyncStatus, expected: SyncStatus): Assertion = {
    decoded.status.toString shouldBe expected.status.toString
    decoded.progress shouldBe expected.progress
  }

  final def compareDelegation(decoded: Delegation, expected: Delegation): Seq[Assertion] = {
    decoded.active.status.toString shouldBe expected.active.status.toString
    decoded.active.target shouldBe expected.active.target

    decoded.next.zip(expected.next).map {
      case (decodedNext, expectedNext) =>
        decodedNext.status.toString shouldBe expectedNext.status.toString
        decodedNext.changesAt shouldBe expectedNext.changesAt
    }
  }

  final def compareDelegationOpts(decoded: Option[Delegation], expected: Option[Delegation]): Seq[Assertion] = {
    if (decoded.nonEmpty && expected.nonEmpty) compareDelegation(decoded.get, expected.get)
    else Seq(assert(false, "one of delegations is none"))
  }

  final def compareWallets(decoded: Wallet, expected: Wallet): Assertion = {
    decoded.id shouldBe expected.id
    decoded.addressPoolGap shouldBe expected.addressPoolGap
    compareBalance(decoded.balance, expected.balance)
    compareDelegationOpts(decoded.delegation, expected.delegation)
    decoded.name shouldBe expected.name
    decoded.passphrase shouldBe expected.passphrase
    compareState(decoded.state, expected.state)
    decoded.tip shouldBe expected.tip
  }
  
}
