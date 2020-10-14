#!/bin/bash

VER=0.1.3-SNAPSHOT
#BASE_URL="http://cardano-wallet-testnet.iog.solutions:8090/v2/"
#BASE_URL="http://localhost:8090/v2/"

#run sbt assembly to create this jar
NEWEST_JAR_NAME=`ls target/scala-2.13/ -Frt | grep '.jar' | tail -n 1`
exec java -jar target/scala-2.13/${NEWEST_JAR_NAME} -baseUrl ${BASE_URL} "$@"
