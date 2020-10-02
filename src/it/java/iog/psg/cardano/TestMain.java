package iog.psg.cardano;

import akka.actor.ActorSystem;
import iog.psg.cardano.jpi.CardanoApi;
import iog.psg.cardano.jpi.*;
import scala.Enumeration;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestMain {

    public static void main(String[] args) throws CardanoApiException, ExecutionException, InterruptedException {

        try {
            ActorSystem as = ActorSystem.create();
            ExecutorService es = Executors.newFixedThreadPool(10);
            CardanoApiBuilder builder =
                    CardanoApiBuilder.create("http://localhost:8090/v2/")
                            .withActorSystem(as)
                            .withExecutorService(es);

            CardanoApi api = builder.build();
            String passphrase = "password10";
            String menmString = "receive post siren monkey mistake morning teach section mention rural idea say offer number ribbon toward rigid pluck begin ticket auto";
            List<String> menmLst = Arrays.asList(menmString.split(" "));
            String walletId = "b63eacb4c89bd942cacfe0d3ed47459bbf0ce5c9";


            CardanoApiCodec.Wallet wallet = null;
            try {
                wallet =
                        api.getWallet(walletId).toCompletableFuture().get();
            } catch(Exception e) {
                wallet = api.createRestore("cardanoapimainspec", passphrase, menmLst, Optional.empty(),10).toCompletableFuture().get();
            }

            CardanoApiCodec.WalletAddressId unusedAddr = api.listAddresses(wallet.id(), AddressFilter.UNUSED).toCompletableFuture().get().get(0);

            Enumeration.Value lovelace = CardanoApiCodec.Units$.MODULE$.lovelace();
            Map<Long, String> meta = new HashMap();
            String l = Long.toString(Long.MAX_VALUE);
            meta.put(Long.MAX_VALUE, "hello world");

            //9223372036854775807
            //meta.put(l, "0123456789012345678901234567890123456789012345678901234567890123");

            List<CardanoApiCodec.Payment> pays =
                    Arrays.asList(
                            new CardanoApiCodec.Payment(unusedAddr.id(),
                                    new CardanoApiCodec.QuantityUnit(1000000, lovelace)
                            )
                    );
            CardanoApiCodec.CreateTransactionResponse resp =
                    api.createTransaction(
                            wallet.id(),
                            passphrase,
                            pays,
                            MetadataBuilder.withMap(meta),
                            "self").toCompletableFuture().get();
            System.out.println(resp.status().toString());
            System.out.println(resp.id());
            System.out.println(resp.metadata());

            //executeHelper.execute(req);
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
            System.exit(9);
        }

    }
}
