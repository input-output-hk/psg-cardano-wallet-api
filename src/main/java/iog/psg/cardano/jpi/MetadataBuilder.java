package iog.psg.cardano.jpi;

import io.circe.Json;
import iog.psg.cardano.codecs.CardanoApiCodec;

import java.util.Map;

public class MetadataBuilder {

    private MetadataBuilder() { }

    public static CardanoApiCodec.JsonMetadata withJson(Json metadataCompliantJson) {
        return new CardanoApiCodec.JsonMetadata(metadataCompliantJson);
    }

    public static CardanoApiCodec.JsonMetadata withJsonString(String metadataCompliantJson) {
        return CardanoApiCodec.JsonMetadata$.MODULE$.apply(metadataCompliantJson);
    }

    public static CardanoApiCodec.TxMetadataMapIn withMap(Map<Long, String> metadataMap) {
        return new CardanoApiCodec.TxMetadataMapIn(
                HelpExecute$.MODULE$.toMetadataMap(metadataMap));
    }

}
