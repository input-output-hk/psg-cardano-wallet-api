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

    public CardanoApi(iog.psg.cardano.CardanoApi api, HelpExecute helpExecute) {
        this.helpExecute = helpExecute;
        this.api = api;
        Objects.requireNonNull(api, "Api cannot be null");
        Objects.requireNonNull(helpExecute, "HelpExecute cannot be null");
    }

    public CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            int addressPoolGap) throws CardanoApiException {
        CardanoApiCodec.MnemonicSentence mnem = createMnemonic(mnemonicWordList);
        return helpExecute.execute(
                api.createRestoreWallet(name, passphrase, mnem, option(addressPoolGap))
        );
    }

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

    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments
            ) throws CardanoApiException {

        return createTransaction(fromWalletId, passphrase, payments, null, "self");
    }

    public CompletionStage<CardanoApiCodec.Wallet> getWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.getWallet(fromWalletId));
    }

    public CompletionStage<Void> deleteWallet(
            String fromWalletId) throws CardanoApiException {

        return helpExecute.execute(
                api.deleteWallet(fromWalletId)).thenApply(x -> null);
    }

    public CompletionStage<CardanoApiCodec.CreateTransactionResponse> getTransaction(
            String walletId, String transactionId) throws CardanoApiException {

        return helpExecute.execute(
                api.getTransaction(walletId, transactionId));
    }

    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return estimateFee(walletId, payments, "self");
    }

    public CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments, String withdrawal) throws CardanoApiException {
        return helpExecute.execute(
                api.estimateFee(walletId,
                        new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq()),
                        withdrawal));
    }

    public CompletionStage<CardanoApiCodec.FundPaymentsResponse> fundPayments(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException {
        return helpExecute.execute(
                api.fundPayments(walletId,
                        new CardanoApiCodec.Payments(CollectionConverters.asScala(payments).toSeq())));
    }

    public CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId, AddressFilter addressFilter) throws CardanoApiException {
        Enumeration.Value v = CardanoApiCodec.AddressFilter$.MODULE$.Value(addressFilter.name().toLowerCase());
        return helpExecute.execute(
                api.listAddresses(walletId, scala.Option.apply(v))).thenApply(CollectionConverters::asJava);
    }

    public CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId) throws CardanoApiException {
        return helpExecute.execute(
                api.listAddresses(walletId, scala.Option.empty())).thenApply(CollectionConverters::asJava);
    }


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


    public CompletionStage<List<CardanoApiCodec.Wallet>> listWallets() throws CardanoApiException {
        return helpExecute.execute(
                api.listWallets())
                .thenApply(CollectionConverters::asJava);
    }

    public CompletionStage<Void> updatePassphrase(
            String walletId,
            String oldPassphrase,
            String newPassphrase) throws CardanoApiException {

        return helpExecute.execute(api.updatePassphrase(walletId, oldPassphrase, newPassphrase)).thenApply(x -> null);
    }

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

}
