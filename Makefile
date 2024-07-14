MVN = docker run --rm -p 8090:8090 --name technical-test-backend -v $(shell pwd)/target-docker/.m2:/root/.m2 -v $(shell pwd):/usr/src/mymaven \
       -w /usr/src/mymaven maven:3.9 mvn -PdockerTargetFolder

test:
	$(MVN) verify

test-unit:
	$(MVN) test

test-class:
	$(MVN) test -Dtest=$(word 2, $(MAKECMDGOALS))

dev:
	$(MVN) spring-boot:run