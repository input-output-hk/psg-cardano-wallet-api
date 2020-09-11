package iog.psg.cardano;

import akka.actor.ActorSystem;
import iog.psg.cardano.jpi.*;
import iog.psg.cardano.jpi.CardanoApi;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestMain {

    public static void main(String[] args) throws CardanoApiException {

        ActorSystem as = ActorSystem.create();
        ExecutorService es = Executors.newFixedThreadPool(10);
        CardanoApiBuilder builder =
                CardanoApiBuilder.create("")
                        .withActorSystem(as)
                        .withExecutorService(es);

        CardanoApi api = builder.build();
        List<String> l = Arrays.asList("", "");

        CardanoApiCodec.Wallet w = null;
        long lll = w.state().progress().get().quantity();

        CompletionStage<Void> req = api.deleteWallet("");

        //executeHelper.execute(req);

    }
}
