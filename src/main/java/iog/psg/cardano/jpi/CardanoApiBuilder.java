package iog.psg.cardano.jpi;

import akka.actor.ActorSystem;
import scala.concurrent.ExecutionContext;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardanoApiBuilder {

    final private String url;
    private ExecutorService executorService;
    private ActorSystem actorSystem;

    private CardanoApiBuilder() {
        url = null;
    }

    private CardanoApiBuilder(String url) {
        this.url = url;
        Objects.requireNonNull(url,
                "Provide the url to a cardano wallet instance e.g. http://127.0.0.1:8090/v2/");
    }

    public static CardanoApiBuilder create(String url) {
        return new CardanoApiBuilder(url);
    }

    public CardanoApiBuilder withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        Objects.requireNonNull(executorService, "ExecutorService is 'null'");
        return this;
    }


    public CardanoApiBuilder withActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        Objects.requireNonNull(actorSystem, "ActorSystem is 'null'");
        return this;
    }

    public CardanoApi build() {

        if (actorSystem == null) {
            actorSystem = ActorSystem.create("Cardano JPI ActorSystem");
        }

        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        ExecutionContext ec = ExecutionContext.fromExecutorService(executorService);
        iog.psg.cardano.CardanoApi api = new iog.psg.cardano.CardanoApi(url, ec, actorSystem);
        HelpExecute helpExecute = new HelpExecute(ec, actorSystem);
        return new CardanoApi(api, helpExecute);
    }

}
