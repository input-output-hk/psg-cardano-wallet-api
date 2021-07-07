package iog.psg.cardano.experimental.cli
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File



case class CardanoCli(builder: ProcessBuilderHelper) extends CliCmd {

  lazy val key: CardanoCliCmdKey = {
    CardanoCliCmdKey(builder.withCommand("key"))
  }

  lazy val address: CardanoCliCmdAddress =
    CardanoCliCmdAddress(builder.withCommand("address"))

  lazy val query: CardanoCliCmdQuery = {
    CardanoCliCmdQuery(builder.withCommand("query"))
  }

  lazy val transaction: CardanoCliCmdTransaction = {
    CardanoCliCmdTransaction(builder.withCommand("transaction"))
  }

}

case class CardanoCliCmdTransaction(protected val builder: ProcessBuilderHelper) extends CliCmd {
  lazy val calculateMinFee: CardanoCliCmdTransactionMinFee = {
    CardanoCliCmdTransactionMinFee(builder.withCommand("calculate-min-fee"))
  }

  lazy val buildRaw: CardanoCliCmdTransactionBuildRaw = {
    CardanoCliCmdTransactionBuildRaw(builder.withCommand("build-raw"))
  }

  lazy val witness: CardanoCliCmdTransactionWitness = {
    CardanoCliCmdTransactionWitness(builder.withCommand("witness"))
  }

  lazy val assemble: CardanoCliCmdTransactionAssemble = {
    CardanoCliCmdTransactionAssemble(builder.withCommand("assemble"))
  }

  lazy val submit: CardanoCliCmdTransactionSubmit = {
    CardanoCliCmdTransactionSubmit(builder.withCommand("submit"))
  }

}



  trait WitnessFile {
    self: CliCmd with CopyShim =>

    def witnessFile(txBody: File): CONCRETECASECLASS =
      copier.copy(builder.withParam("--witness-file", txBody))

  }


  trait SigningKeyFile {
    self: CliCmd with CopyShim =>

    def signingKeyFile(scriptFile: File): CONCRETECASECLASS =
      copier.copy(builder.withParam("--signing-key-file", scriptFile))

  }

  trait ScriptFile {
    self: CliCmd with CopyShim =>

    def scriptFile(scriptFile: File): CONCRETECASECLASS =
      copier.copy(builder.withParam("--script-file", scriptFile))

  }

  trait OutFile {
    self: CliCmd with CopyShim =>

    def outFile(txBody: File): CONCRETECASECLASS =
      copier.copy(builder.withParam("--out-file", txBody))

  }


  trait TxBodyFile {
    self: CliCmd with CopyShim =>

    def txBodyFile(txBody: File): CONCRETECASECLASS =
      copier.copy(builder.withParam("--tx-body-file", txBody))

  }


  trait MaryEra {
    self: CliCmd with CopyShim =>

    lazy val maryEra:CONCRETECASECLASS =
      copier.copy(builder.withParam("--mary-era"))

  }

  trait TxFile {
    self: CliCmd with CopyShim =>

    def txFile(txFile: File):CONCRETECASECLASS =
      copier.copy(builder.withParam("--tx-file", txFile))

  }

  trait TestnetMagic {
    self: CliCmd with CopyShim =>

    lazy val mainnet: CONCRETECASECLASS = {
      copier.copy(builder
        .withParam("--mainnet"))
    }

    def testnetMagic(magic: Long): CONCRETECASECLASS = {
      copier.copy(builder
        .withParam("--testnet-magic", magic.toString))
    }
  }

  case class CardanoCliCmdTransactionSubmit(protected val builder: ProcessBuilderHelper)
     extends CliCmd
      with TestnetMagic
      with CopyShim
      with TxFile {


    type CONCRETECASECLASS = CardanoCliCmdTransactionSubmit
    protected def copier = this

    def run() = exitValue()

  }

  case class CardanoCliCmdTransactionBuildRaw(protected val builder: ProcessBuilderHelper) extends CliCmd{
    //TODO
  }

  case class CardanoCliCmdTransactionWitness(protected val builder: ProcessBuilderHelper)
    extends CliCmd
      with CopyShim
      with TxBodyFile
      with OutFile
      with TestnetMagic
      with ScriptFile
      with SigningKeyFile {

    override type CONCRETECASECLASS = CardanoCliCmdTransactionWitness
    val copier = this


  }

  case class CardanoCliCmdTransactionAssemble(protected val builder: ProcessBuilderHelper)  extends CliCmd
    with TxBodyFile with OutFile with WitnessFile with CopyShim {

    override type CONCRETECASECLASS = CardanoCliCmdTransactionAssemble
    val copier = this

  }


  case class CardanoCliCmdTransactionMinFee(protected val builder: ProcessBuilderHelper) extends CliCmd{

    def protocolParamsFile(protocolParams: File): CardanoCliCmdTransactionMinFee =
      copy(builder.withParam("--protocol-params-file", protocolParams))

    def txInCount(in: Int): CardanoCliCmdTransactionMinFee =
      copy(builder.withParam("--tx-in-count", in.toString))

    def txOutCount(out: Int): CardanoCliCmdTransactionMinFee =
      copy(builder.withParam("--tx-out-count", out.toString))

    def witnessCount(witnessCount: Int):CardanoCliCmdTransactionMinFee =
      copy(builder.withParam("--witness-count", witnessCount.toString))

    def run(): String = stringValue()
  }


  case class CardanoCliCmdAddressBuild(protected val builder: ProcessBuilderHelper)
     extends CliCmd
      with TestnetMagic
      with CopyShim
      with OutFile {


    override type CONCRETECASECLASS = CardanoCliCmdAddressBuild
    val copier = this

    def paymentVerificationKey(verificationKey: String): CardanoCliCmdAddressBuild =
      CardanoCliCmdAddressBuild(builder.withParam("--payment-verification-key", verificationKey))

    def paymentVerificationKeyFile(verificationKeyFile: File): CardanoCliCmdAddressBuild =
      CardanoCliCmdAddressBuild(builder.withParam("--payment-verification-key-file", verificationKeyFile))

    def paymentScriptFile(paymentScriptFile: File): CardanoCliCmdAddressBuild =
      CardanoCliCmdAddressBuild(builder.withParam("--payment-script-file", paymentScriptFile))


    def run(): Int = exitValue()

  }

  case class CardanoCliCmdAddressBuildScript(protected val builder: ProcessBuilderHelper)
     extends CliCmd
      with TestnetMagic
      with CopyShim
      with OutFile
      with ScriptFile {


    override type CONCRETECASECLASS = CardanoCliCmdAddressBuildScript
    val copier = this

    def run(): Int = exitValue()

  }

  case class CardanoCliCmdAddress(protected val builder: ProcessBuilderHelper) extends CliCmd{

    lazy val keyHash:CardanoCliCmdAddressKeyHash =
      CardanoCliCmdAddressKeyHash(builder.withParam("key-hash"))

    lazy val keyGen:CardanoCliCmdAddressKeyGen =
      CardanoCliCmdAddressKeyGen(builder.withParam("key-gen"))

    lazy val buildScript:CardanoCliCmdAddressBuildScript =
      CardanoCliCmdAddressBuildScript(builder.withParam("build-script"))

    lazy val build:CardanoCliCmdAddressBuild =
      CardanoCliCmdAddressBuild(builder.withParam("build"))

  }

  case class CardanoCliCmdAddressKeyHash(protected val builder: ProcessBuilderHelper) extends CliCmd{
    def paymentVerificationString(bech32EncodedKey: String): CardanoCliCmdAddressKeyHashString =
      CardanoCliCmdAddressKeyHashString(builder.withParam("--payment-verification-key", bech32EncodedKey))

    def paymentVerificationFile(pathToBech32EncodedKey: File): CardanoCliCmdAddressKeyHashFile =
      CardanoCliCmdAddressKeyHashFile(builder.withParam("--payment-verification-key-file", pathToBech32EncodedKey))
  }

  case class CardanoCliCmdAddressKeyHashFile(protected val builder: ProcessBuilderHelper) extends CliCmd{
    def run(): String = stringValue()
  }

  case class CardanoCliCmdAddressKeyHashString(protected val builder: ProcessBuilderHelper)  extends CliCmd


  case class CardanoCliCmdAddressKeyGen(protected val builder: ProcessBuilderHelper) extends CliCmd{
    lazy val normalKey: CardanoCliCmdAddressKeyGenNormalKey =
      CardanoCliCmdAddressKeyGenNormalKey(builder.withParam("--normal-key"))

    def verificationKeyFile(verificationKeyFile: File): CardanoCliCmdAddressKeyGen = {
      CardanoCliCmdAddressKeyGen(
        builder.withParam("--verification-key-file",
          verificationKeyFile))
    }

    def signingKeyFile(signingKeyFile: File): CardanoCliCmdAddressKeyGen = {
      CardanoCliCmdAddressKeyGen(
        builder.withParam("--signing-key-file",
          signingKeyFile))
    }

  }

  case class CardanoCliCmdAddressKeyGenNormalKey(protected val builder: ProcessBuilderHelper) extends CliCmd{
    def run(): Int = exitValue()
  }

  case class CardanoCliCmdQuery(protected val builder: ProcessBuilderHelper) extends CliCmd{

    /*
    protocol-parameters | tip | stake-distribution |
                           stake-address-info | utxo | ledger-state |
                           protocol-state | stake-snapshot | pool-params
     */
    lazy val protocolParameters: CardanoCliCmdQueryProtocol = {
      CardanoCliCmdQueryProtocol(builder.withCommand("protocol-parameters"))
    }

    lazy val utxo: CardanoCliCmdQueryUtxo = {
      CardanoCliCmdQueryUtxo(builder.withCommand("utxo"))
    }
  }

  case class CardanoCliCmdQueryUtxo(protected val builder: ProcessBuilderHelper)
    extends CliCmd {

    def address(address:String): CardanoCliCmdQueryUtxo =
      copy(builder = builder.withParam("--address", address))

    def testnetMagic(magic: String): CardanoCliCmdQueryUtxo = {
      copy(builder.withParam("--testnet-magic", magic))
    }

    def outFile(outFile:File): CardanoCliCmdQueryUtxo = {
      copy(builder.withParam("--out-file", outFile))
    }

    def run(): Int = exitValue()

  }
  case class CardanoCliCmdKey(protected val builder: ProcessBuilderHelper) extends CliCmd{

    //    lazy val verificationKey = ???
    //    lazy val nonExtendedKey = ???
    //    lazy val convertByronKey = ???
    ////    lazy val convert-byron-genesis-vkey = ???
    ////    lazy val convert-itn-key = ???
    ///    lazy val convert-itn-extended-key = ???
    //    lazy val convert-itn-bip32-key = ???
    //    lazy val convertCardanoAddressKey = ???

    throw new IllegalArgumentException("NOT IMPLEMENTED")
  }

  case class CardanoCliCmdQueryProtocol(builder: ProcessBuilderHelper)
    extends CliCmd
      with TestnetMagic
      with MaryEra
      with OutFile
      with CopyShim {


    override type CONCRETECASECLASS = CardanoCliCmdQueryProtocol
    val copier = this

    lazy val shellyMode: CardanoCliCmdQueryProtocol = {
      CardanoCliCmdQueryProtocol(builder.withParam("--shelly-mode"))
    }

    def run(): Seq[String] = {
      allValues()
    }

  }


