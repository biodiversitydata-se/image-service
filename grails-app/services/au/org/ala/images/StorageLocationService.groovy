package au.org.ala.images

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.hibernate.ScrollableResults

@Transactional
class StorageLocationService {

    def imageService

    StorageLocation createStorageLocation(json) {
        StorageLocation storageLocation
        switch (json.type?.toLowerCase()) {
            case 'fs':
                if (FileSystemStorageLocation.countByBasePath(json.basePath) > 0) {
                    throw new RuntimeException("FS $json.basePath already exists")
                }
                storageLocation = new FileSystemStorageLocation(basePath: json.basePath)
                break
            case 's3':
                if (S3StorageLocation.countByRegionAndBucketAndPrefix(json.region, json.bucket, json.prefix ?: '') > 0) {
                    throw new RuntimeException("S3 $json.region $json.bucket $json.prefix already exists")
                }
                storageLocation = new S3StorageLocation(region: json.region, bucket: json.bucket, prefix: json.prefix ?: '',
                        accessKey: json.accessKey, secretKey: json.secretKey, publicRead: [true, 'true', 'on'].contains(json.publicRead))
                break
            default:
                throw new RuntimeException("Unknown storage location type ${json.type}")
        }

        if (storageLocation.hasErrors()) {
            throw new RuntimeException("Validation error")
        }

        def sl = storageLocation.save(failOnError: true, validate: true)

        return sl

    }

    def migrate(long sourceId, long destId, String userId) {
        log.info("migrating images from storage location {} to {}", sourceId, destId)
        StorageLocation source = StorageLocation.findById(sourceId)
        StorageLocation dest = StorageLocation.findById(destId)

        if (!source || !dest) {
            throw new RuntimeException("Storage Location doesn't exist")
        }
        log.debug("Searching for images")

        def c = Image.createCriteria()
        def results = c.scroll {
            eq('storageLocation', source)
            projections {
                property('id')
            }
        }

        while (results.next()) {
            def id = results.get(0)
            log.debug("Adding MigrationStorageLocationTask for image id {} to dest storage location {}", id, dest)
            imageService.scheduleBackgroundTask(new MigrateStorageLocationTask(imageId: id, destinationStorageLocationId: destId, userId: userId, imageService: imageService))
        }
    }
}
