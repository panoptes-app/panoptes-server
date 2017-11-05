
RASP_IP :=$(shell nmap 192.168.1.0/24 -sL | grep raspberry | sed 's/.*(\(.*\))/\1/')

.PHONY: test dep build lint megalint

all: dep build
	@echo "All done. \o/"

reset-db:
	@echo "it's time to start db for runtime"
	@docker kill mongo-dev ; docker rm mongo-dev ; docker run -e "MONGO_INITDB_ROOT_USERNAME=root" -e "MONGO_INITDB_ROOT_PASSWORD=root" -p 27017:27017 -v ${PWD}/test/mongo/:/docker-entrypoint-initdb.d/ --name mongo-dev -d mongo --auth
	@./test/wait-for-mongo.sh

run: reset-db
	@echo "let's have fun"
	@./gradlew run -conf src/main/conf/my-application-conf.json

build:
	@./gradlew clean shadowJar

deploy-rasp: build
	@scp build/libs/panoptes-3.5.0-fatJar.jar pi@$(RASP_IP):./
	@GOARCH=arm GOARM=6 go build -o "build/panoptes" main.go
	@scp ./build/panoptes pi@$(RASP_IP):./panoptes

test:
	@echo "it's time to start db for test"
	@docker kill mongo-dev ; docker rm mongo-dev ; docker run -e "MONGO_INITDB_ROOT_USERNAME=root" -e "MONGO_INITDB_ROOT_PASSWORD=root" -p 27017:27017 -v ${PWD}/test/mongo/:/docker-entrypoint-initdb.d/ --name mongo-dev -d mongo --auth
	@./test/wait-for-mongo.sh
	@echo "let's doing some tests"
	@./gradlew test

