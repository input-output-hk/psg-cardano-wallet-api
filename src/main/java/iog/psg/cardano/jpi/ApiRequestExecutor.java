package iog.psg.cardano.jpi;

import java.util.concurrent.CompletionStage;

public interface ApiRequestExecutor {
    <T> CompletionStage<T> execute(iog.psg.cardano.CardanoApi.CardanoApiRequest<T> request) throws CardanoApiException;

}
