package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApiCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JpiResponseCheck {

    public final CardanoApi jpi;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private JpiResponseCheck() {
        jpi = null;
        timeout = 0;
        timeoutUnit = null;

    }

    public JpiResponseCheck(CardanoApi jpi, long timeout, TimeUnit timeoutUnit) {
        this.jpi = jpi;
        this.timeoutUnit = timeoutUnit;
        this.timeout = timeout;
    }

    static String get(CardanoApiCodec.NetworkInfo info) {
        return info.syncProgress().status().toString();
    }

    public void createBadWallet() throws CardanoApiException, InterruptedException, TimeoutException, ExecutionException {
        List<String> mnem = List.of("", "sdfa", "dfd");
        jpi.createRestore("some name", "password99", mnem, 4).toCompletableFuture().get(timeout, timeoutUnit);
    }

    public boolean findOrCreateTestWallet(String ourWalletId, String ourWalletName, String walletPassphrase, List<String> wordList, int addressPoolGap) throws CardanoApiException, InterruptedException, TimeoutException, ExecutionException {
        List<CardanoApiCodec.Wallet> wallets = jpi.listWallets().toCompletableFuture().get(timeout, timeoutUnit);
        for(CardanoApiCodec.Wallet w: wallets) {
            if(w.id().contentEquals(ourWalletId)) {
                return true;
            }
        }
        CardanoApiCodec.Wallet created =  jpi.createRestore(ourWalletName, walletPassphrase, wordList,addressPoolGap).toCompletableFuture().get(timeout, timeoutUnit);
        return created.id().contentEquals(ourWalletId);
    }

    public boolean getWallet(String walletId) throws CardanoApiException, InterruptedException, TimeoutException, ExecutionException {
        CardanoApiCodec.Wallet w = jpi.getWallet(walletId).toCompletableFuture().get(timeout, timeoutUnit);
        return w.id().contentEquals(walletId);
    }

    public void passwordChange(String walletId, String passphrase, String newPassphrase) throws CardanoApiException, InterruptedException, ExecutionException, TimeoutException {
        jpi.updatePassphrase(walletId, passphrase, newPassphrase).toCompletableFuture().get(timeout, timeoutUnit);

    }


    public CardanoApiCodec.FundPaymentsResponse fundPayments(String walletId, long amountToTransfer) throws Exception {
        List<CardanoApiCodec.WalletAddressId> unused = jpi.listAddresses(walletId, AddressFilter.UNUSED).toCompletableFuture().get(timeout, timeoutUnit);
        String unusedAddrId = unused.get(0).id();
        CardanoApiCodec.QuantityUnit amount = new CardanoApiCodec.QuantityUnit(amountToTransfer, CardanoApiCodec.Units$.MODULE$.lovelace());
        CardanoApiCodec.Payment p = new CardanoApiCodec.Payment(unusedAddrId, amount);
        CardanoApiCodec.FundPaymentsResponse response = jpi.fundPayments(walletId, List.of(p)).toCompletableFuture().get(timeout, timeoutUnit);
        return response;
    }

    public void deleteWallet(String walletId) throws Exception {
        jpi.deleteWallet(walletId).toCompletableFuture().get(timeout, timeoutUnit);

    }

    public CardanoApiCodec.CreateTransactionResponse paymentToSelf(String wallet1Id, String passphrase, int amountToTransfer, Map<String, String> metadata) throws Exception {

        Map<Long, String> metadataLongKey = new HashMap();
        metadata.forEach((k,v) -> {
            metadataLongKey.put(Long.parseLong(k), v);
        });

        List<CardanoApiCodec.WalletAddressId> unused = jpi.listAddresses(wallet1Id, AddressFilter.UNUSED).toCompletableFuture().get(timeout, timeoutUnit);
        String unusedAddrIdWallet1 = unused.get(0).id();
        CardanoApiCodec.QuantityUnit amount = new CardanoApiCodec.QuantityUnit(amountToTransfer, CardanoApiCodec.Units$.MODULE$.lovelace());
        List<CardanoApiCodec.Payment> payments = List.of(new CardanoApiCodec.Payment(unusedAddrIdWallet1, amount));
        CardanoApiCodec.EstimateFeeResponse response = jpi.estimateFee(wallet1Id, payments).toCompletableFuture().get(timeout, timeoutUnit);
        long max = response.estimatedMax().quantity();
        return jpi.createTransaction(wallet1Id, passphrase, payments, metadataLongKey).toCompletableFuture().get(timeout, timeoutUnit);

    }

    public CardanoApiCodec.CreateTransactionResponse getTx(String walletId, String txId) throws Exception {
        return jpi.getTransaction(walletId, txId).toCompletableFuture().get(timeout, timeoutUnit);
    }
}
