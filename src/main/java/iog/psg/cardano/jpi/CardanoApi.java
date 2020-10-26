package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;
import scala.Enumeration;
import scala.Some;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;


public class CardanoApi {

    private final iog.psg.cardano.CardanoApi api;
    private final HelpExecute helpExecute;

    private CardanoApi() {
        helpExecute = null;
        api = null;
    }

    /**
     * CardanoApi constructor
     *
     * @param api iog.psg.cardano.CardanoApi instance
     * @param helpExecute og.psg.cardano.jpi.HelpExecute instance
     */
    public CardanoApi(iog.psg.cardano.CardanoApi api, HelpExecute helpExecute) {
        this.helpExecute = helpExecute;
        this.api = api;
        Objects.requireNonNull(api, "Api cannot be null");
        Objects.requireNonNull(helpExecute, "HelpExecute cannot be null");
    }

    /**
     * Create and restore a wallet from a mnemonic sentence or account public key.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet">#postWallet</a>
     *
     * @param name wallet's name
     * @param passphrase A master passphrase to lock and protect the wallet for sensitive operation (e.g. sending funds)
     * @param mnemonicWordList A list of mnemonic words [ 15 .. 24 ] items ( can be generated using https://iancoleman.io/bip39> )
     * @param addressPoolGap An optional number of consecutive unused addresses allowed
     * @return Created wallet
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            int addressPoolGap) throws CardanoApiException {
        return createRestore(name, passphrase, mnemonicWordList, null, addressPoolGap);
    }

    /**
     * Create and restore a wallet from a mnemonic sentence or account public key.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet">#postWallet</a>
     *
     * @param name wallet's name
     * @param passphrase A master passphrase to lock and protect the wallet for sensitive operation (e.g. sending funds)
     * @param mnemonicWordList A list of mnemonic words [ 15 .. 24 ] items ( can be generated using https://iancoleman.io/bip39> )
     * @param mnemonicSecondFactor A passphrase used to encrypt the mnemonic sentence. [ 9 .. 12 ] items
     * @param addressPoolGap An optional number of consecutive unused addresses allowed
     * @return Created wallet
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     *
     */
    public CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            List<String> mnemonicSecondFactor,
            int addressPoolGap) throws CardanoApiException {
        CardanoApiCodec.MnemonicSentence mnem = createMnemonic(mnemonicWordList);

        Optional<CardanoApiCodec.MnemonicSentence> mnemonicSecondaryFactorOpt = Optional.empty();
        if (mnemonicSecondFactor != null) {
            CardanoApiCodec.MnemonicSentence mnemonicSentence = createMnemonicSecondary(mnemonicSecondFactor);
            mnemonicSecondaryFactorOpt = Optional.of(mnemonicSentence);
        }

        return helpExecute.execute(
                api.createRestoreWallet(name, passphrase, mnem, option(mnemonicSecondaryFactorOpt), option(addressPoolGap))
        );
    }

    /**
     * Create and send transaction from the wallet.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction">#postTransaction</a>
     *
     * @param fromWalletId wallet's id
     * @param passphrase The wallet's master passphrase. [ 0 .. 255 ] characters
     * @param payments A list of target outputs ( address, amount )
     * @param withdrawal nullable, when provided, instruments the server to automatically withdraw rewards from the source
     *                   wallet when they are deemed sufficient (i.e. they contribute to the balance for at least as much
     *                   as they cost).
     * @param metadata   Extra application data attached to the transaction.
     * @return created transaction
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
   public CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments,
            CardanoApiCodec.TxMetadataIn metadata,
            String withdrawal
            ) throws CardanoApiException {

        return helpExecute.execute(api.createTransaction(fromWalletId, passphrase,
                new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq()),
                option(metadata),
                option(withdrawal)));
    }

    /**
     * Create and send transaction from the wallet.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction">#postTransaction</a>
     *
     * @param fromWalletId wallet's id
     * @param passphrase The wallet's master passphrase. [ 0 .. 255 ] characters
     * @param payments A list of target outputs ( address, amount )
     * @return created transaction
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments
            ) throws CardanoApiException {

        return createTransaction(fromWalletId, passphrase, payments, null, "self");
    }

    /**
     * Get wallet details by id
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet">#getWallet</a>
     *
     * @param fromWalletId wallet's id
     * @return wallet
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.Wallet> getWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.getWallet(fromWalletId));
    }

    /**
     * Delete wallet by id
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteWallet">#deleteWallet</a>
     *
     * @param fromWalletId wallet's id
     * @return void
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<Void> deleteWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.deleteWallet(fromWalletId)).thenApply(x -> null);
    }

    /**
     * Get transaction by id.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction">#getTransaction</a>
     *
     * @param walletId wallet's id
     * @param transactionId transaction's id
     * @return get transaction request
     */
    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> getTransaction(
            String walletId, String transactionId) throws CardanoApiException {

        return helpExecute.execute(
                api.getTransaction(walletId, transactionId));
    }

    /**
     * Estimate fee for the transaction. The estimate is made by assembling multiple transactions and analyzing the
     * distribution of their fees. The estimated_max is the highest fee observed, and the estimated_min is the fee which
     * is lower than at least 90% of the fees observed.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee">#estimateFee</a>
     *
     * @param walletId wallet's id
     * @param payments A list of target outputs ( address, amount )
     * @return estimatedfee response
     */
    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return estimateFee(walletId, payments, "self", null);
    }

    /**
     * Estimate fee for the transaction. The estimate is made by assembling multiple transactions and analyzing the
     * distribution of their fees. The estimated_max is the highest fee observed, and the estimated_min is the fee which
     * is lower than at least 90% of the fees observed.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee">#estimateFee</a>
     *
     * @param walletId wallet's id
     * @param payments A list of target outputs ( address, amount )
     * @param withdrawal nullable, when provided, instruments the server to automatically withdraw rewards from the source
     *                   wallet when they are deemed sufficient (i.e. they contribute to the balance for at least as much
     *                   as they cost).
     * @param metadata  Extra application data attached to the transaction.
     * @return estimated fee response
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId,
            List<CardanoApiCodec.Payment> payments,
            String withdrawal,
            CardanoApiCodec.TxMetadataIn metadata) throws CardanoApiException {

        return helpExecute.execute(
                api.estimateFee(walletId,
                        new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq()),
                        option(withdrawal), option(metadata)));
    }

    /**
     * Select coins to cover the given set of payments.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Coin-Selections">#CoinSelections</a>
     *
     * @param walletId wallet's id
     * @param payments A list of target outputs ( address, amount )
     * @return fund payments
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.FundPaymentsResponse> fundPayments(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return helpExecute.execute(
                api.fundPayments(walletId,
                        new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq())));
    }

    /**
     * list of known addresses, ordered from newest to oldest
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Addresses">#Addresses</a>
     *
     *
     * @param walletId wallet's id
     * @param addressFilter addresses state: used, unused
     * @return list of wallet's addresses
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId, AddressFilter addressFilter) throws CardanoApiException {

        Optional<Enumeration.Value> addressFilterOpt = Optional.empty();
        if (addressFilter != null) {
            Enumeration.Value v = CardanoApiCodec.AddressFilter$.MODULE$.Value(addressFilter.name().toLowerCase());
            addressFilterOpt = Optional.of(v);
        }

        return helpExecute.execute(
                api.listAddresses(walletId, option(addressFilterOpt))).thenApply(CollectionConverters::asJava);
    }

    /**
     * list of known addresses, ordered from newest to oldest
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Addresses">#Addresses</a>
     *
     * @param walletId wallet's id
     * @return list of wallet's addresses
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId) throws CardanoApiException {
        return listAddresses(walletId, null);
    }

    /**
     * Lists all incoming and outgoing wallet's transactions.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listTransactions">#listTransactions</a>
     *
     * @param builder ListTransactionsParamBuilder
     * @return list of wallet's transactions
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<List<CardanoApiCodec.CreateTransactionResponse>> listTransactions(
            ListTransactionsParamBuilder builder) throws CardanoApiException {
        return helpExecute.execute(
                api.listTransactions(
                        builder.getWalletId(),
                        option(builder.getStartTime()),
                        option(builder.getEndTime()),
                        builder.getOrder(),
                        option(builder.getMinwithdrawal())))
                .thenApply(CollectionConverters::asJava);
    }

    /**
     * list of known wallets, ordered from oldest to newest.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listWallets">#listWallets</a>
     *
     * @return wallets's list
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<List<CardanoApiCodec.Wallet>> listWallets() throws CardanoApiException {
        return helpExecute.execute(
                api.listWallets())
                .thenApply(CollectionConverters::asJava);
    }

    /**
     * Update Passphrase
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWalletPassphrase">#putWalletPassphrase</a>
     * @param walletId wallet's id
     * @param oldPassphrase current passphrase
     * @param newPassphrase new passphrase
     * @return void
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<Void> updatePassphrase(
            String walletId,
            String oldPassphrase,
            String newPassphrase) throws CardanoApiException {

        return helpExecute.execute(api.updatePassphrase(walletId, oldPassphrase, newPassphrase)).thenApply(x -> null);
    }

    /**
     * Update wallet's name
     * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWallet #putWallet]]
     *
     * @param walletId wallet's id
     * @param name new wallet's name
     * @return update wallet request
     * @throws CardanoApiException
     */
    public CompletionStage<CardanoApiCodec.Wallet> updateName(
            String walletId,
            String name) throws CardanoApiException {
        return helpExecute.execute(api.updateName(walletId, name));
    }

    /**
     * Gives network information
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkInformation">#getNetworkInformation</a>
     *
     * @return network info
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    public CompletionStage<CardanoApiCodec.NetworkInfo> networkInfo() throws CardanoApiException {
        return helpExecute.execute(api.networkInfo());
    }

    private static <T> scala.Option<T> option(final T value) {
        return (value != null) ? new Some<T>(value) : scala.Option.apply((T) null);
    }

    private static <T> scala.Option<T> option(final Optional<T> value) {
        return value.map(CardanoApi::option).orElse(scala.Option.apply((T) null));
    }

    private static CardanoApiCodec.GenericMnemonicSentence createMnemonic(List<String> wordList) {
        return new CardanoApiCodec.GenericMnemonicSentence(
                CollectionConverters.asScala(wordList).toIndexedSeq()
        );
    }

    private static CardanoApiCodec.GenericMnemonicSecondaryFactor createMnemonicSecondary(List<String> wordList) {
        return new CardanoApiCodec.GenericMnemonicSecondaryFactor(
                CollectionConverters.asScala(wordList).toIndexedSeq()
        );
    }

}
