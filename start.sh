#!/usr/bin/env bash

set -ex

#nvm use --lts

git pull
cd src/main/frontend || exit
npm run build
cd ../../../
./gradlew clean jar
java -jar build/libs/moneymaker-1.0.jarjava -jar build/libs/moneymaker-1.0.jar
