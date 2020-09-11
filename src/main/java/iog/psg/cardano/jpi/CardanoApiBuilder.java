package iog.psg.cardano.jpi;

import akka.actor.ActorSystem;
import scala.concurrent.ExecutionContext;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardanoApiBuilder {

    final private String url;
    private ExecutorService executorService;
    private ExecutorService ioExecutorService;
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

    public CardanoApiBuilder withIOExecutorService(ExecutorService ioExecutorService) {
        this.ioExecutorService = ioExecutorService;
        Objects.requireNonNull(ioExecutorService, "IO ExecutorService is 'null'");
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

        if (ioExecutorService == null && executorService == null) {
            ioExecutorService = Executors.newCachedThreadPool();
            executorService = ioExecutorService;
        } else if(ioExecutorService == null) {
            ioExecutorService = executorService;
        } else if (executorService == null){
            executorService = ioExecutorService;
        }

        ExecutionContext ec = ExecutionContext.fromExecutorService(executorService);
        ExecutionContext ioEc = ExecutionContext.fromExecutorService(ioExecutorService);
        iog.psg.cardano.CardanoApi api = new iog.psg.cardano.CardanoApi(url, ec, actorSystem);
        iog.psg.cardano.CardanoApi.IOExecutionContext ioExec = new iog.psg.cardano.CardanoApi.IOExecutionContext(ioEc);
        HelpExecute helpExecute = new HelpExecute(ioExec, ec, actorSystem);
        return new CardanoApi(api, helpExecute);
    }

}
