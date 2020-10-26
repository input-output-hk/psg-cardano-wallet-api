package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CardanoApi {
    CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            int addressPoolGap) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            List<String> mnemonicSecondFactor,
            int addressPoolGap) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments,
            CardanoApiCodec.TxMetadataIn metadata,
            String withdrawal
    ) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments
    ) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.Wallet> getWallet(
            String fromWalletId) throws CardanoApiException;

    CompletionStage<Void> deleteWallet(
            String fromWalletId) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.CreateTransactionResponse> getTransaction(
            String walletId, String transactionId) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId,
            List<CardanoApiCodec.Payment> payments,
            String withdrawal,
            CardanoApiCodec.TxMetadataIn metadata) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.FundPaymentsResponse> fundPayments(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException;

    CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId, AddressFilter addressFilter) throws CardanoApiException;

    CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId) throws CardanoApiException;

    CompletionStage<List<CardanoApiCodec.CreateTransactionResponse>> listTransactions(
            ListTransactionsParamBuilder builder) throws CardanoApiException;

    CompletionStage<List<CardanoApiCodec.Wallet>> listWallets() throws CardanoApiException;

    CompletionStage<Void> updatePassphrase(
            String walletId,
            String oldPassphrase,
            String newPassphrase) throws CardanoApiException;

    CompletionStage<CardanoApiCodec.NetworkInfo> networkInfo() throws CardanoApiException;
}
