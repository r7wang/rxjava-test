#!/usr/bin/env bash

mvn clean install
cd target
java -cp "rxjava-test-1.0-SNAPSHOT-jar-with-dependencies.jar" com.rw.Main
