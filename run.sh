#!/usr/env/bin bash

mvn clean install
cd target
java -cp "ReactiveTest-1.0-SNAPSHOT-jar-with-dependencies.jar" com.rw.Main
