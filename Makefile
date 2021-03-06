
RASP_IP :=$(shell nmap 192.168.1.0/24 -sL | grep raspberry | sed 's/.*(\(.*\))/\1/')

.PHONY: test dep build lint megalint

all: dep build
	@echo "All done. \o/"

reset-db:
	@echo "it's time to start db for runtime"
	@docker kill mongo-dev ; docker rm mongo-dev ; docker run -e "MONGO_INITDB_ROOT_USERNAME=root" -e "MONGO_INITDB_ROOT_PASSWORD=root" -p 27017:27017 -v ${PWD}/test/mongo/:/docker-entrypoint-initdb.d/ --name mongo-dev -d mongo --auth
	@./test/wait-for-mongo.sh

run: reset-db build
	@echo "let's have fun in prod mode"
	@java -jar build/libs/panoptes-0.0.1-fatJar.jar -conf src/main/conf/conf.json


build:
	@./gradlew clean shadowJar

deploy-rasp: build
	@scp build/libs/panoptes-0.0.1-fatJar.jar pi@$(RASP_IP):./

test:
	@echo "it's time to start db for test"
	@docker kill mongo-dev ; docker rm mongo-dev ; docker run -e "MONGO_INITDB_ROOT_USERNAME=root" -e "MONGO_INITDB_ROOT_PASSWORD=root" -p 27017:27017 -v ${PWD}/test/mongo/:/docker-entrypoint-initdb.d/ --name mongo-dev -d mongo --auth
	@./test/wait-for-mongo.sh
	@echo "let's doing some tests"
	@./gradlew test

