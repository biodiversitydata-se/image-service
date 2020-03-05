package au.org.ala.images

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ImageServiceSpec extends Specification implements ServiceUnitTest<ImageService>, DataTest {

    def setup() {
        mockDomains Image, FileSystemStorageLocation
    }

    def "test migrate storage location happy path"() {
        setup:
        service.imageStoreService = Mock(ImageStoreService)
        service.auditService = Mock(AuditService)
        def src = new FileSystemStorageLocation(basePath: '/tmp/1').save()
        def image = new Image(imageIdentifier: '1234', mimeType: 'image/jpeg', dateDeleted: null, storageLocation: src).save()
        def dest = new FileSystemStorageLocation(basePath: '/tmp/2').save()

        when:
        service.migrateImage(image.id, dest.id, '1234')

        then:
        1 * service.imageStoreService.migrateImage(image, dest)
        1 * service.auditService.log(image.imageIdentifier, "Migrated to $dest", '1234')
        image.storageLocation == dest
    }

    def "test migrate storage location same storage location"() {
        setup:
        service.imageStoreService = Mock(ImageStoreService)
        service.auditService = Mock(AuditService)
        def src = new FileSystemStorageLocation(basePath: '/tmp').save()
        def image = new Image(imageIdentifier: '1234', mimeType: 'image/jpeg', dateDeleted: null, storageLocation: src).save()
        def dest = new FileSystemStorageLocation(basePath: '/tmp').save()

        when:
        service.migrateImage(image.id, dest.id, '1234')

        then:
        0 * service.imageStoreService.migrateImage(image, dest)
        0 * service.auditService.log(image.imageIdentifier, "Migrated to $dest", '1234')
        image.storageLocation == src
    }

    def "test migrate storage location migrate throws"() {
        setup:
        service.imageStoreService = Mock(ImageStoreService)
        service.auditService = Mock(AuditService)
        def src = new FileSystemStorageLocation(basePath: '/tmp/1').save()
        def image = new Image(imageIdentifier: '1234', mimeType: 'image/jpeg', dateDeleted: null, storageLocation: src).save()
        def dest = new FileSystemStorageLocation(basePath: '/tmp/2').save()

        when:
        service.migrateImage(image.id, dest.id, '1234')

        then:
        1 * service.imageStoreService.migrateImage(image, dest) >> { throw new IOException("Boo") }
        thrown IOException
        0 * service.auditService.log(image.imageIdentifier, "Migrated to $dest", '1234')
        image.storageLocation == src
    }
}
