CURDIR = $(shell pwd)

ifeq (upload_images,$(firstword $(MAKECMDGOALS)))
  # use the rest as arguments for "run"
  RUN_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  # ...and turn them into do-nothing targets
  $(eval $(RUN_ARGS):;@:)
endif

PROJECT_ID = "mcc-2016-g01-p2"

start_db:
	docker run --name mongo-database -d mongo

start_dev: start_db
	docker run -p 80:80 -v $(CURDIR)/language:/usr/share/tessdata/:ro -v $(CURDIR):/usr/src/server -e MODE=dev -e FLASK_DEBUG=1 --link mongo-database server python server.py


upload_images:
	docker build -t gcr.io/$(PROJECT_ID)/server:$(RUN_ARGS) .
	gcloud docker -- push gcr.io/$(PROJECT_ID)/server:$(RUN_ARGS)

cleanup:
	kubectl delete po ocr-server mongo-database
	kubectl delete service ocr mongo-database
	kubectl delete ing ssl-terminator

run:
	kubectl create -f resources/mongodb.yaml
	kubectl create -f resources/server.yaml
	kubectl create -f resources/mongodb-service.yaml
	kubectl create -f resources/server-service.yaml
	kubectl create -f resources/ingress.yaml

