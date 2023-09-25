# Image-service

## Setup

### Config and data directory
Create data directory at `/data/image-service` and populate as below (it is easiest to symlink the config files to the ones in this repo):
```
mats@xps-13:/data/image-service$ tree
.
├── config
│   └── image-service-config.yml -> /home/mats/src/biodiversitydata-se/image-service/sbdi/data/config/image-service-config.yml
├── exports
├── incoming
└── store
    └── staging
```

### Database and search index
An empty database will be created the first time the application starts. You can then export the database from production and import it.

You can also copy the actual images from production and put them in `/data/image-service/store`.

## Usage
Run locally:
```
make run
```

Build and run in Docker (using Tomcat). This requires a small change in the config file to work. See comment in Makefile.
```
make run-docker
```

Make a release. This will create a new tag and push it. A new Docker container will be built on Github.
```
mats@xps-13:~/src/biodiversitydata-se/image-service (master *)$ make release

Current version: 1.0.1. Enter the new version (or press Enter for 1.0.2): 
Updating to version 1.0.2
Tag 1.0.2 created and pushed.
```

## Rebuild image index

- Go to /admin
- Select **Tools**
- Click **Reindex all images**