package iog.psg.cardano.jpi;

import iog.psg.cardano.CardanoApi;
import iog.psg.cardano.CardanoApi$;
import iog.psg.cardano.CardanoApiCodec;
import scala.Enumeration;

import java.time.ZonedDateTime;
import java.util.Objects;

public class ListTransactionsParamBuilder {

    private final String walletId;
    private int minwithdrawal = 1;
    private ZonedDateTime startTime = null;
    private Order order = null;

    public String getWalletId() {
        return walletId;
    }

    public Enumeration.Value getOrder() {

        Enumeration.Value result = CardanoApi.Order$.MODULE$.Value(Order.DESCENDING.name().toLowerCase());
        if(order != null) {
            result = CardanoApi.Order$.MODULE$.Value(order.name().toLowerCase());
        }
        return result;
    }

    public int getMinwithdrawal() {
        return minwithdrawal;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    private ZonedDateTime endTime = null;

    private ListTransactionsParamBuilder() {
        walletId = null;
    }

    private ListTransactionsParamBuilder(String walletId) {
        this.walletId = walletId;
        Objects.requireNonNull(walletId, "WalletId cannot be null");
    }

    static ListTransactionsParamBuilder create(String walletId) {
        return new ListTransactionsParamBuilder(walletId);
    }


    public ListTransactionsParamBuilder withEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public ListTransactionsParamBuilder withStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public ListTransactionsParamBuilder withOrder(Order order) {
        this.order = order;
        return this;
    }

    public ListTransactionsParamBuilder withMinwithdrawal(int minwithdrawal) {
        this.minwithdrawal = minwithdrawal;
        return this;
    }


}
