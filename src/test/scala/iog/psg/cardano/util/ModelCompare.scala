package iog.psg.cardano.util

import iog.psg.cardano.CardanoApiCodec._
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

trait ModelCompare extends Matchers {

  final def compareInAddress(decoded: InAddress, proper: InAddress): Assertion = {
    decoded.address shouldBe proper.address
    compareQuantityUnitOpts(decoded.amount, proper.amount)
    decoded.id shouldBe proper.id
    decoded.index shouldBe proper.index
  }

  final def compareOutAddress(decoded: OutAddress, proper: OutAddress): Assertion = {
    decoded.address shouldBe proper.address
    compareQuantityUnit(decoded.amount, proper.amount)
  }

  final def compareInputs(decoded: Seq[InAddress], proper: Seq[InAddress]): Seq[Assertion] =
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareInAddress(decodedAddress, properAddress)
    }

  final def compareOutputs(decoded: Seq[OutAddress], proper: Seq[OutAddress]): Seq[Assertion] =
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareOutAddress(decodedAddress, properAddress)
    }

  final def compareFundPaymentsResponse(decoded: FundPaymentsResponse, proper: FundPaymentsResponse): Seq[Assertion] = {
    compareInputs(decoded.inputs, proper.inputs)
    compareOutputs(decoded.outputs, proper.outputs)
  }

  final def compareEstimateFeeResponse(decoded: EstimateFeeResponse, proper: EstimateFeeResponse): Assertion = {
    compareQuantityUnit(decoded.estimatedMax, proper.estimatedMax)
    compareQuantityUnit(decoded.estimatedMin, proper.estimatedMin)
  }

  final def compareStakeAddress(decoded: StakeAddress, proper: StakeAddress): Assertion = {
    compareQuantityUnit(decoded.amount, proper.amount)
    decoded.stakeAddress shouldBe proper.stakeAddress
  }

  final def compareStakeAddresses(decoded: Seq[StakeAddress], proper: Seq[StakeAddress]): Seq[Assertion] = {
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareStakeAddress(decodedAddress, properAddress)
    }
  }

  final def compareTransaction(decoded: CreateTransactionResponse, proper: CreateTransactionResponse): Assertion = {
    decoded.id shouldBe proper.id
    compareQuantityUnit(decoded.amount, proper.amount)
    decoded.insertedAt shouldBe proper.insertedAt
    decoded.pendingSince shouldBe proper.pendingSince
    decoded.depth shouldBe proper.depth
    decoded.direction shouldBe proper.direction
    compareInputs(decoded.inputs, proper.inputs)
    compareOutputs(decoded.outputs, proper.outputs)
    compareStakeAddresses(decoded.withdrawals, proper.withdrawals)
    decoded.status shouldBe proper.status
    decoded.metadata shouldBe proper.metadata
  }

  final def compareAddress(decoded: WalletAddressId, proper: WalletAddressId): Assertion = {
    decoded.id shouldBe proper.id
    decoded.state shouldBe proper.state
  }

  final def compareNetworkInformation(decoded: NetworkInfo, proper: NetworkInfo): Assertion = {
    decoded.nextEpoch shouldBe proper.nextEpoch
    decoded.nodeTip shouldBe proper.nodeTip
    decoded.networkTip shouldBe proper.networkTip
    decoded.syncProgress.status.toString shouldBe proper.syncProgress.status.toString

    compareQuantityUnitOpts(decoded.syncProgress.progress, proper.syncProgress.progress)
  }

  final def compareQuantityUnitOpts(decoded: Option[QuantityUnit], proper: Option[QuantityUnit]): Assertion = {
    if (decoded.isEmpty && proper.isEmpty) assert(true)
    else (for {
      decodedQU <- decoded
      properQU <- proper
    } yield compareQuantityUnit(decodedQU, properQU)).getOrElse(assert(false, "one of units is none"))
  }

  final def compareQuantityUnit(decoded: QuantityUnit, proper: QuantityUnit): Assertion = {
    decoded.unit.toString shouldBe proper.unit.toString
    decoded.quantity shouldBe proper.quantity
  }

  final def compareBalance(decoded: Balance, proper: Balance): Assertion = {
    decoded.available.quantity shouldBe proper.available.quantity
    decoded.available.unit.toString shouldBe proper.available.unit.toString

    decoded.reward.quantity shouldBe proper.reward.quantity
    decoded.reward.unit.toString shouldBe proper.reward.unit.toString

    decoded.total.quantity shouldBe proper.total.quantity
    decoded.total.unit.toString shouldBe proper.total.unit.toString
  }

  final def compareState(decoded: SyncStatus, proper: SyncStatus): Assertion = {
    decoded.status.toString shouldBe proper.status.toString
    decoded.progress shouldBe proper.progress
  }

  final def compareDelegation(decoded: Delegation, proper: Delegation): Seq[Assertion] = {
    decoded.active.status.toString shouldBe proper.active.status.toString
    decoded.active.target shouldBe proper.active.target

    decoded.next.zip(proper.next).map {
      case (decodedNext, properNext) =>
        decodedNext.status.toString shouldBe properNext.status.toString
        decodedNext.changesAt shouldBe properNext.changesAt
    }
  }

  final def compareWallets(decoded: Wallet, proper: Wallet): Assertion = {
    decoded.id shouldBe proper.id
    decoded.addressPoolGap shouldBe proper.addressPoolGap
    compareBalance(decoded.balance, proper.balance)
    decoded.delegation shouldBe proper.delegation
    decoded.name shouldBe proper.name
    decoded.passphrase shouldBe proper.passphrase
    compareState(decoded.state, proper.state)
    decoded.tip shouldBe proper.tip
  }
  
}
