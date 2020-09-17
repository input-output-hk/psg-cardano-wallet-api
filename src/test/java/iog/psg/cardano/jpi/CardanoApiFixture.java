package iog.psg.cardano.jpi;

import akka.actor.ActorSystem;

import java.util.Objects;

public class CardanoApiFixture {

    public CardanoApi getJpi() {
        return jpi;
    }

    private final CardanoApi jpi;

    private CardanoApiFixture() {
        jpi = null;
    }

    public CardanoApiFixture(String url) {
        Objects.requireNonNull(url);
        ActorSystem as = ActorSystem.create("TESTING_CARDANO_JPI");
        jpi = CardanoApiBuilder.create(url).withActorSystem(as).build();
    }
}
