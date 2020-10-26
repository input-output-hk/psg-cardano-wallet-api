package iog.psg.cardano.jpi;

import akka.actor.ActorSystem;
import scala.concurrent.ExecutionContext;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardanoApiBuilder {

    final private String url;
    private ExecutorService executorService;
    private ActorSystem actorSystem;
    private ApiRequestExecutor apiRequestExecutor;

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

    public CardanoApiBuilder withApiExecutor(ApiRequestExecutor apiExecutor) {
        this.apiRequestExecutor = apiExecutor;
        Objects.requireNonNull(apiExecutor, "apiExecutor is 'null'");
        return this;
    }

    public CardanoApiImpl build() {

        if (actorSystem == null) {
            actorSystem = ActorSystem.create("CardanoJPIActorSystem");
        }

        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        ExecutionContext ec = ExecutionContext.fromExecutorService(executorService);

        HelpExecute helpExecute;

        if(apiRequestExecutor == null) {
            helpExecute = new HelpExecute(ec, actorSystem);
        } else {
            helpExecute = new HelpExecute(ec, actorSystem) {
                @Override
                public <T> CompletionStage<T> execute(iog.psg.cardano.CardanoApi.CardanoApiRequest<T> request) throws CardanoApiException {
                    return apiRequestExecutor.execute(request);
                }
            };
        }

        iog.psg.cardano.CardanoApiImpl api = new iog.psg.cardano.CardanoApiImpl(url, ec, actorSystem);

        return new CardanoApiImpl(api, helpExecute);
    }

}
