package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;
import scala.Enumeration;
import scala.Some;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;


public class CardanoApiImpl implements CardanoApi {

    private final iog.psg.cardano.CardanoApi api;
    private final HelpExecute helpExecute;

    private CardanoApiImpl() {
        helpExecute = null;
        api = null;
    }

    /**
     * CardanoApi constructor
     *
     * @param api iog.psg.cardano.CardanoApi instance
     * @param helpExecute og.psg.cardano.jpi.HelpExecute instance
     */
    public CardanoApiImpl(iog.psg.cardano.CardanoApi api, HelpExecute helpExecute) {
        this.helpExecute = helpExecute;
        this.api = api;
        Objects.requireNonNull(api, "Api cannot be null");
        Objects.requireNonNull(helpExecute, "HelpExecute cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            int addressPoolGap) throws CardanoApiException {
        return createRestore(name, passphrase, mnemonicWordList, null, addressPoolGap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.Wallet> createRestoreWithKey(
            String name,
            String accountPublicKey,
            int addressPoolGap
    ) throws CardanoApiException {
        return helpExecute.execute(
                api.createRestoreWalletWithKey(name, accountPublicKey, option(addressPoolGap))
        );
    }

    /**
     * {@inheritDoc}
     */
   @Override
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
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments
    ) throws CardanoApiException {

        return createTransaction(fromWalletId, passphrase, payments, null, "self");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.Wallet> getWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.getWallet(fromWalletId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<Void> deleteWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.deleteWallet(fromWalletId)).thenApply(x -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> getTransaction(
            String walletId, String transactionId) throws CardanoApiException {

        return helpExecute.execute(
                api.getTransaction(walletId, transactionId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<Void> deleteTransaction(String walletId, String transactionId) throws CardanoApiException {
        return helpExecute.execute(api.deleteTransaction(walletId, transactionId)).thenApply(x -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return estimateFee(walletId, payments, "self", null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.FundPaymentsResponse> fundPayments(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return helpExecute.execute(
                api.fundPayments(walletId,
                        new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId) throws CardanoApiException {
        return listAddresses(walletId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.WalletAddress> inspectAddress(
            String addressId) throws CardanoApiException {
        return helpExecute.execute(api.inspectAddress(addressId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<List<CardanoApiCodec.Wallet>> listWallets() throws CardanoApiException {
        return helpExecute.execute(
                api.listWallets())
                .thenApply(CollectionConverters::asJava);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<Void> updatePassphrase(
            String walletId,
            String oldPassphrase,
            String newPassphrase) throws CardanoApiException {

        return helpExecute.execute(api.updatePassphrase(walletId, oldPassphrase, newPassphrase)).thenApply(x -> null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.Wallet> updateName(
            String walletId,
            String name) throws CardanoApiException {
        return helpExecute.execute(api.updateName(walletId, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.NetworkInfo> networkInfo() throws CardanoApiException {
        return helpExecute.execute(api.networkInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.NetworkClock> networkClock() throws CardanoApiException {
        return networkClock(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.NetworkClock> networkClock(Boolean forceNtpCheck) throws CardanoApiException {
        return helpExecute.execute(api.networkClock(option(forceNtpCheck)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.NetworkParameters> networkParameters() throws CardanoApiException {
        return helpExecute.execute(api.networkParameters());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.UTxOStatistics> getUTxOsStatistics(String walletId) throws CardanoApiException {
        return helpExecute.execute(api.getUTxOsStatistics(walletId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.PostExternalTransactionResponse> postExternalTransaction(String binary) throws CardanoApiException {
        return helpExecute.execute(api.postExternalTransaction(binary));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<List<CardanoApiCodec.MigrationResponse>> migrateShelleyWallet(String walletId, String passphrase, List<String> addresses) throws CardanoApiException {
        IndexedSeq<String> addressesList = CollectionConverters.asScala(addresses).toIndexedSeq();
        CompletionStage<Seq<CardanoApiCodec.MigrationResponse>> response = helpExecute.execute(api.migrateShelleyWallet(walletId, passphrase, addressesList));
        return response.thenApply(CollectionConverters::asJava);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.MigrationCostResponse> getShelleyWalletMigrationInfo(String walletId) throws CardanoApiException {
        return helpExecute.execute(api.getShelleyWalletMigrationInfo(walletId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<List<CardanoApiCodec.StakePool>> listStakePools(Integer stake) throws CardanoApiException {
        CompletionStage<Seq<CardanoApiCodec.StakePool>> stakePools = helpExecute.execute(api.listStakePools(stake));
        return stakePools.thenApply(CollectionConverters::asJava);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFeeStakePool(String walletId) throws CardanoApiException {
        return helpExecute.execute(api.estimateFeeStakePool(walletId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.MigrationResponse> joinStakePool(String walletId, String stakePoolId, String passphrase) throws CardanoApiException {
        return helpExecute.execute(api.joinStakePool(walletId, stakePoolId, passphrase));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.MigrationResponse> quitStakePool(String walletId, String passphrase) throws CardanoApiException {
        return helpExecute.execute(api.quitStakePool(walletId, passphrase));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<CardanoApiCodec.StakePoolMaintenanceActionsStatus> getMaintenanceActions() throws CardanoApiException {
        return helpExecute.execute(api.getMaintenanceActions());
    }

    @Override
    public CompletionStage<Void> postMaintenanceAction() throws CardanoApiException {
        return helpExecute.execute(api.postMaintenanceAction()).thenApply(x -> null);
    }

    private static <T> scala.Option<T> option(final T value) {
        return (value != null) ? new Some<T>(value) : scala.Option.apply((T) null);
    }

    private static <T> scala.Option<T> option(final Optional<T> value) {
        return value.map(CardanoApiImpl::option).orElse(scala.Option.apply((T) null));
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
