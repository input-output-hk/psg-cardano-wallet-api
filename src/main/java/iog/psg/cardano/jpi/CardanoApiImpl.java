package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;
import scala.Enumeration;
import scala.Some;
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
    public CompletionStage<CardanoApiCodec.NetworkInfo> networkInfo() throws CardanoApiException {
        return helpExecute.execute(api.networkInfo());
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
