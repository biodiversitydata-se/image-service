run:
	docker compose up --detach pgsql elasticsearch
	./gradlew bootRun

# In image-service-config-yml you need to change dataSource.url to 'pgsql'
# and elasticsearch.hosts.host to 'elasticsearch' for this to work
run-docker:
	./gradlew bootWar
	docker compose build --no-cache
	docker compose up --detach

release:
	@./sbdi/make-release.sh
