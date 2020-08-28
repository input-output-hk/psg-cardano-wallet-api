#!/bin/bash

VER=0.1.1-SNAPSHOT
BASE_URL="http://cardano-wallet-testnet.iog.solutions:8090/v2/"


#run sbt assembly to create this jar
exec java -jar target/scala-2.13/psg-cardano-wallet-api-assembly-${VER}.jar -baseUrl ${BASE_URL} "$@"
