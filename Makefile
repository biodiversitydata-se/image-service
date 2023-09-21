run:
	docker compose up --detach pgsql elasticsearch
	./gradlew bootRun

run-docker:
	./gradlew bootWar
	docker compose build --no-cache
	docker compose up --detach

release:
	@./sbdi/make-release.sh
