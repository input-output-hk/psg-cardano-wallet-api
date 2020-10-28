package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Defines the API which wraps the Cardano API, depends on CardanoApiCodec for it's implementation,
 * so clients will import the Codec also.
 */
public interface CardanoApi {

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
    CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            int addressPoolGap) throws CardanoApiException;

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
    CompletionStage<CardanoApiCodec.Wallet> createRestore(
            String name,
            String passphrase,
            List<String> mnemonicWordList,
            List<String> mnemonicSecondFactor,
            int addressPoolGap) throws CardanoApiException;

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
    CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments,
            CardanoApiCodec.TxMetadataIn metadata,
            String withdrawal
    ) throws CardanoApiException;

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
    CompletionStage<CardanoApiCodec.CreateTransactionResponse> createTransaction(
            String fromWalletId,
            String passphrase,
            List<CardanoApiCodec.Payment> payments
    ) throws CardanoApiException;

    /**
     * Get wallet details by id
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet">#getWallet</a>
     *
     * @param fromWalletId wallet's id
     * @return wallet
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<CardanoApiCodec.Wallet> getWallet(
            String fromWalletId) throws CardanoApiException;

    /**
     * Delete wallet by id
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteWallet">#deleteWallet</a>
     *
     * @param fromWalletId wallet's id
     * @return void
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<Void> deleteWallet(
            String fromWalletId) throws CardanoApiException;

    /**
     * Get transaction by id.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction">#getTransaction</a>
     *
     * @param walletId wallet's id
     * @param transactionId transaction's id
     * @return get transaction request
     */
    CompletionStage<CardanoApiCodec.CreateTransactionResponse> getTransaction(
            String walletId, String transactionId) throws CardanoApiException;

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
    CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException;

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
    CompletionStage<CardanoApiCodec.EstimateFeeResponse> estimateFee(
            String walletId,
            List<CardanoApiCodec.Payment> payments,
            String withdrawal,
            CardanoApiCodec.TxMetadataIn metadata) throws CardanoApiException;

    /**
     * Select coins to cover the given set of payments.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Coin-Selections">#CoinSelections</a>
     *
     * @param walletId wallet's id
     * @param payments A list of target outputs ( address, amount )
     * @return fund payments
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<CardanoApiCodec.FundPaymentsResponse> fundPayments(
            String walletId, List<CardanoApiCodec.Payment> payments) throws CardanoApiException;

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
    CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId, AddressFilter addressFilter) throws CardanoApiException;

    /**
     * list of known addresses, ordered from newest to oldest
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Addresses">#Addresses</a>
     *
     * @param walletId wallet's id
     * @return list of wallet's addresses
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<List<CardanoApiCodec.WalletAddressId>> listAddresses(
            String walletId) throws CardanoApiException;

    /**
     * Lists all incoming and outgoing wallet's transactions.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listTransactions">#listTransactions</a>
     *
     * @param builder ListTransactionsParamBuilder
     * @return list of wallet's transactions
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<List<CardanoApiCodec.CreateTransactionResponse>> listTransactions(
            ListTransactionsParamBuilder builder) throws CardanoApiException;

    /**
     * list of known wallets, ordered from oldest to newest.
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listWallets">#listWallets</a>
     *
     * @return wallets's list
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<List<CardanoApiCodec.Wallet>> listWallets() throws CardanoApiException;

    /**
     * Update Passphrase
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWalletPassphrase">#putWalletPassphrase</a>
     * @param walletId wallet's id
     * @param oldPassphrase current passphrase
     * @param newPassphrase new passphrase
     * @return void
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<Void> updatePassphrase(
            String walletId,
            String oldPassphrase,
            String newPassphrase) throws CardanoApiException;

    /**
     * Gives network information
     * Api Url: <a href="https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkInformation">#getNetworkInformation</a>
     *
     * @return network info
     * @throws CardanoApiException thrown on API error response, contains error message and code from API
     */
    CompletionStage<CardanoApiCodec.NetworkInfo> networkInfo() throws CardanoApiException;
}
