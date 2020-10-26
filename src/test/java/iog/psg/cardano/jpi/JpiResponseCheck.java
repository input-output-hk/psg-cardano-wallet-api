package iog.psg.cardano.jpi;

import akka.actor.ActorSystem;
import iog.psg.cardano.CardanoApiCodec;
import scala.Enumeration;
import scala.Option;
import scala.concurrent.Future;
import scala.jdk.CollectionConverters;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import static scala.compat.java8.FutureConverters.*;
import scala.util.Either;

public class JpiResponseCheck {

    public final CardanoApiImpl jpi;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    private JpiResponseCheck() {
        jpi = null;
        timeout = 0;
        timeoutUnit = null;
    }

    public JpiResponseCheck(CardanoApiImpl jpi, long timeout, TimeUnit timeoutUnit) {
        this.jpi = jpi;
        this.timeoutUnit = timeoutUnit;
        this.timeout = timeout;
    }

    static String get(CardanoApiCodec.NetworkInfo info) {
        return info.syncProgress().status().toString();
    }

    public void createBadWallet() throws CardanoApiException, InterruptedException, TimeoutException, ExecutionException {
        List<String> mnem = Arrays.asList("", "sdfa", "dfd");
        jpi.createRestore("some name", "password99", mnem,4).toCompletableFuture().get(timeout, timeoutUnit);
    }

    public boolean findOrCreateTestWallet(String ourWalletId, String ourWalletName, String walletPassphrase, List<String> wordList, int addressPoolGap) throws CardanoApiException, InterruptedException, TimeoutException, ExecutionException {
        List<CardanoApiCodec.Wallet> wallets = jpi.listWallets().toCompletableFuture().get(timeout, timeoutUnit);
        for(CardanoApiCodec.Wallet w: wallets) {
            if(w.id().contentEquals(ourWalletId)) {
                return true;
            }
        }

        CardanoApiCodec.Wallet created = createTestWallet(ourWalletName, walletPassphrase, wordList, addressPoolGap);
        return created.id().contentEquals(ourWalletId);
    }

    public CardanoApiCodec.Wallet createTestWallet(String ourWalletName, String walletPassphrase, List<String> wordList, int addressPoolGap) throws CardanoApiException, InterruptedException, ExecutionException, TimeoutException {
        CardanoApiCodec.Wallet wallet = jpi.createRestore(ourWalletName, walletPassphrase, wordList, addressPoolGap).toCompletableFuture().get(timeout, timeoutUnit);
        return wallet;
    }

    public CardanoApiCodec.Wallet createTestWallet(String ourWalletName, String walletPassphrase, List<String> wordList, List<String> mnemSecondaryWordList, int addressPoolGap) throws CardanoApiException, InterruptedException, ExecutionException, TimeoutException {
        CardanoApiCodec.Wallet wallet = jpi.createRestore(ourWalletName, walletPassphrase, wordList, mnemSecondaryWordList, addressPoolGap).toCompletableFuture().get(timeout, timeoutUnit);
        return wallet;
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
        CardanoApiCodec.FundPaymentsResponse response = jpi.fundPayments(walletId, Collections.singletonList(p)).toCompletableFuture().get(timeout, timeoutUnit);
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

        CardanoApiCodec.TxMetadataMapIn in = MetadataBuilder.withMap(metadataLongKey);
        List<CardanoApiCodec.WalletAddressId> unused = jpi.listAddresses(wallet1Id, AddressFilter.UNUSED).toCompletableFuture().get(timeout, timeoutUnit);
        String unusedAddrIdWallet1 = unused.get(0).id();
        CardanoApiCodec.QuantityUnit amount = new CardanoApiCodec.QuantityUnit(amountToTransfer, CardanoApiCodec.Units$.MODULE$.lovelace());
        List<CardanoApiCodec.Payment> payments = Collections.singletonList(new CardanoApiCodec.Payment(unusedAddrIdWallet1, amount));
        CardanoApiCodec.EstimateFeeResponse response = jpi.estimateFee(wallet1Id, payments).toCompletableFuture().get(timeout, timeoutUnit);
        long max = response.estimatedMax().quantity();
        return jpi.createTransaction(wallet1Id, passphrase, payments, in, null).toCompletableFuture().get(timeout, timeoutUnit);

    }

    public CardanoApiCodec.CreateTransactionResponse getTx(String walletId, String txId) throws Exception {
        return jpi.getTransaction(walletId, txId).toCompletableFuture().get(timeout, timeoutUnit);
    }

    public static CardanoApiImpl buildWithPredefinedApiExecutor(iog.psg.cardano.ApiRequestExecutor executor, ActorSystem as) {
        CardanoApiBuilder builder = CardanoApiBuilder.create("http://fake:1234/").withApiExecutor(new ApiRequestExecutor() {
            @Override
            public <T> CompletionStage<T> execute(iog.psg.cardano.CardanoApiImpl.CardanoApiRequest<T> request) throws CardanoApiException {
                Future<Either<iog.psg.cardano.CardanoApiImpl.ErrorMessage, T>> sResponse = executor.execute(request, as.dispatcher(), as);
                CompletionStage<T> jResponse = toJava(HelpExecute.unwrap(sResponse, as.dispatcher()));
                return jResponse;
            }
        });

        return builder.build();
    }

    public static CardanoApiImpl buildWithDummyApiExecutor() {
        CardanoApiBuilder builder = CardanoApiBuilder.create("http://fake/").withApiExecutor(new ApiRequestExecutor() {
            @Override
            public <T> CompletionStage<T> execute(iog.psg.cardano.CardanoApiImpl.CardanoApiRequest<T> request) throws CardanoApiException {
                CompletableFuture<T> result = new CompletableFuture<>();

                if(request.request().uri().path().endsWith("wallets", true)) {
                    Enumeration.Value lovelace = CardanoApiCodec.Units$.MODULE$.Value(CardanoApiCodec.Units$.MODULE$.lovelace().toString());
                    Enumeration.Value sync = CardanoApiCodec.SyncState$.MODULE$.Value(CardanoApiCodec.SyncState$.MODULE$.ready().toString());
                    CardanoApiCodec.QuantityUnit dummy = new CardanoApiCodec.QuantityUnit(1, lovelace);
                    CardanoApiCodec.SyncStatus state = new CardanoApiCodec.SyncStatus(
                            sync,
                            Option.apply(null)
                    );
                    CardanoApiCodec.NetworkTip tip = new CardanoApiCodec.NetworkTip(3,4,Option.apply(null), Option.apply(10));

                    ZonedDateTime dummyDate = ZonedDateTime.parse("2000-01-02T10:01:02+01:00");
                    Enumeration.Value delegatingStatus = CardanoApiCodec.DelegationStatus$.MODULE$.Value(CardanoApiCodec.DelegationStatus$.MODULE$.delegating().toString());
                    Enumeration.Value notDelegatingStatus = CardanoApiCodec.DelegationStatus$.MODULE$.Value(CardanoApiCodec.DelegationStatus$.MODULE$.notDelegating().toString());
                    CardanoApiCodec.DelegationActive delegationActive = new CardanoApiCodec.DelegationActive(delegatingStatus, Option.apply("1234567890"));
                    CardanoApiCodec.DelegationNext delegationNext = new CardanoApiCodec.DelegationNext(notDelegatingStatus, Option.apply(new CardanoApiCodec.NextEpoch(dummyDate, 10)));
                    List<CardanoApiCodec.DelegationNext> nexts =  Arrays.asList(delegationNext);
                    scala.collection.immutable.List<CardanoApiCodec.DelegationNext> nextsScalaList = CollectionConverters.ListHasAsScala(nexts).asScala().toList();
                    CardanoApiCodec.Delegation delegation = new CardanoApiCodec.Delegation(delegationActive, nextsScalaList);

                    result.complete((T) new CardanoApiCodec.Wallet(
                            "id",
                            10,
                            new CardanoApiCodec.Balance(dummy, dummy, dummy),
                            Option.apply(delegation),
                            "name",
                            new CardanoApiCodec.Passphrase(dummyDate),
                            state,
                            tip));
                    return result.toCompletableFuture();
                } else {
                    throw new CardanoApiException("Unexpected", "request");
                }
            }

        });

        return builder.build();
    }
}
