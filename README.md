# image-service   [![Build Status](https://travis-ci.com/AtlasOfLivingAustralia/image-service.svg?branch=master)](https://travis-ci.org/AtlasOfLivingAustralia/image-service)

This Grails application provides the webservices and backend for the storage of all images in the Atlas.
It includes:

* Support for large images, generation of thumbnails and tile views
* Extensible key/value pair storage for image metadata
* Support for subimaging and maintaining the relationships between parent and child images
* Exif extraction
* Tile view for large images compatible with GIS Javascript clients such as LeafletJS, OpenLayers and Google Maps
* Web services for image upload
* Generate of derivative images for thumbnail presentation
* Tagging support via webservices
* Administrator console for image management
* Swagger API definition
* Integration with google analytics to monitor image usage by data resource
* Support for image storage in S3, Swift
* Support for batch uploads with AVRO

There are other related repositories to this one:
* [images-client-plugin](https://github.com/AtlasOfLivingAustralia/images-client-plugin) - a grails plugin to provide a Javascript based viewer to be used in other applications requiring a image viewer. This viewer is based on LeafletJS.
* [image-tiling-agent](https://github.com/AtlasOfLivingAustralia/image-tiling-agent) - a utility to run tiling jobs for the image-service. This is intended to used on multiple machine as tiling is CPU intensive and best parallelised.
* [image-loader](https://github.com/AtlasOfLivingAustralia/image-loader) - utility for bulk loading images into the image-service.

## Upgrading from 1.0

Image Service 1.1 removes the reliance on the database user being a PostgreSQL superuser.  When upgrading using the `ala-install` ansible playbook, you may find that some database objects are owned by the `postgres` superuser role.  To ensure that all database objects belong to the correct user, run the following just prior to upgrading:

```bash
sudo -u postgres -s
export IMAGES_USER=images
psql -c "ALTER DATABASE images OWNER TO $IMAGES_USER"
for tbl in `psql -qAt -c "select tablename from pg_tables where schemaname = 'public';" images` ; do psql -c "alter table \"$tbl\" owner to $IMAGES_USER" images ; done
for tbl in `psql -qAt -c "select sequence_name from information_schema.sequences where sequence_schema = 'public';" images` ; do  psql -c "alter sequence \"$tbl\" owner to $IMAGES_USER" images ; done
for tbl in `psql -qAt -c "select table_name from information_schema.views where table_schema = 'public';" images` ; do  psql -c "alter view \"$tbl\" owner to $IMAGES_USER" images ; done
psql -c "alter function if exists "export_images" owner to $IMAGES_USER;" images
psql -c "alter function if exists "export_index" owner to $IMAGES_USER;" images
```

## Architecture

* Grails 3 web application ran as standalone executable jar
* Open JDK 8
* Postgres database (9.6 or above)
* Elastic search 7
* Debian package install

## Installation

There are ansible scripts for this applications (and other ALA tools) in the [ala-install](https://github.com/AtlasOfLivingAustralia/ala-install) project. The ansible playbook for the image-service is [here](https://github.com/AtlasOfLivingAustralia/ala-install/blob/master/ansible/image-service.yml)

You can also run this application locally by following the instructions on its [wiki page](https://github.com/AtlasOfLivingAustralia/image-service/wiki)

## Running it locally

### Postgres
There is a docker-compose YML file that can be used to run postgres locally for local development purposes.
To use run:
```$xslt
docker-compose -f postgres.yml up -d
```
And to shutdown
```$xslt
docker-compose -f postgres.yml kill
```

### Elastic search
There is a docker-compose YML file that can be used to run elastic search locally for local development purposes.
To use run:
```$xslt
docker-compose -f elastic.yml up -d
```
And to shutdown
```$xslt
docker-compose -f elastic.yml kill
```
