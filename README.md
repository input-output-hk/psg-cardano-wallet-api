# PSG Cardano Wallet API

_For consultancy services email [enterprise.solutions@iohk.io](mailto:enterprise.solutions@iohk.io)_
### Scala and Java client for the Cardano Wallet API

[Post](#metacmd)
The Cardano Node exposes a [REST like API](https://github.com/input-output-hk/cardano-wallet) 
allowing clients to perform a variety of tasks such as creating or restoring a wallet, submitting 
a transaction, submitting [metadata](https://github.com/input-output-hk/cardano-wallet/wiki/TxMetadata) or checking on the syncing status of the node.

The full list of capabilities can be found [here](https://input-output-hk.github.io/cardano-wallet/api/edge/). 
     
This artefact wraps calls to that API to make them easily accessible to Java or Scala developers.

It also provides an executable jar to provide very rudimentary command line access. 


#### Building 

This is an `sbt` project, so the usual commands apply.

Clone the [repository](https://github.com/input-output-hk/psg-cardano-wallet-api) 

To build and publish the project to your local repository use 

`sbt publish`

To build the command line executable jar use

`sbt assembly`  

To build the command line executable jar skipping tests, use

`sbt 'set test in assembly := {}' assembly`

This will create a jar in the `target/scala-2.13` folder. 

#### Implementation Details

The jar is part of an Akka streaming ecosystem and unsurprisingly uses [Akka Http](https://doc.akka.io/docs/akka-http/current/introduction.html) to make the http requests, 
it also uses [circe](https://circe.github.io/circe/) to marshal and unmarshal the json.

#### Usage 

The jar is published in Maven Central, the command line executable jar can be downloaded from the releases section 
of the [github repository](https://github.com/input-output-hk/psg-cardano-wallet-api)
    
##### Scala

Add the library to your dependencies 

`libraryDependencies += "iog.psg" %% "psg-cardano-wallet-api" % "0.4.1"`

The api calls return a HttpRequest set up to the correct url and a mapper to take the entity result and 
map it from Json to the corresponding case classes.

```
import iog.psg.cardano.CardanoApi.CardanoApiOps._
import iog.psg.cardano.CardanoApi._

implicit val as = ActorSystem("MyActorSystem")
val baseUri = "http://localhost:8090/v2/"
import as.dispatcher

val api = new CardanoApi(baseUri)

val networkInfoF: Future[CardanoApiResponse[NetworkInfo]] =
    api.networkInfo.toFuture.execute

val networkInfo: CardanoApiResponse[NetworkInfo] =
    api.networkInfo.toFuture.executeBlocking

networkInfo match {
  case Left(ErrorMessage(message, code)) => //do something
  case Right(netInfo: NetworkInfo) => // good! 
}
```
 
##### Java

First, add the library to your dependencies, then 

```
import iog.psg.cardano.jpi.*;

ActorSystem as = ActorSystem.create();
ExecutorService es = Executors.newFixedThreadPool(10);
CardanoApiBuilder builder =
        CardanoApiBuilder.create("http://localhost:8090/v2/")
                .withActorSystem(as)
                .withExecutorService(es);

CardanoApi api = builder.build();

String walletId = "";
CardanoApiCodec.Wallet  wallet =
            api.getWallet(walletId).toCompletableFuture().get();

```

##### Command Line 

To see the minimal help, use    

`java -jar psg-cardano-wallet-api-assembly-x.x.x-SNAPSHOT.jar`

To see the [network information](https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Network) use 

`java -jar psg-cardano-wallet-api-assembly-x.x.x-SNAPSHOT.jar -baseUrl http://localhost:8090/v2/ -netInfo`
  
#### <a name="metacmd"></a> Posting metadata from the command line
