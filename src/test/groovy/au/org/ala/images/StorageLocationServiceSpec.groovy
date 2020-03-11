package au.org.ala.images

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.javaswift.joss.client.factory.AuthenticationMethod
import spock.lang.Specification

class StorageLocationServiceSpec extends Specification implements ServiceUnitTest<StorageLocationService>, DataTest {


    def setupSpec() {
        mockDomains StorageLocation, FileSystemStorageLocation, S3StorageLocation, SwiftStorageLocation, Image
    }

    def "test createStorageLocation"() {
        when: "creating a FS Storage Location"
        def json = [
                type: 'fs',
                basePath: '/tmp/something'
        ]
        def fssl = service.createStorageLocation(json)

        then: "as FS Storage Location is created"
        fssl.class == FileSystemStorageLocation
        fssl.basePath == json.basePath

        when: "creating a duplicate FS Storage Location"
        def fssl2 = service.createStorageLocation(json)

        then: "the Storage Location is rejected"
        thrown StorageLocationService.AlreadyExistsException

        when: "creating an S3 Storage Location"
        json = [
                type: 's3',
                region: 'ap-southeast-2',
                bucket: 'test-bucket',
                prefix: '/some/prefix',
                accessKey: 'asdfasdf',
                secretKey: 'asdfasdf',
                publicRead: false
        ]

        def s3sl = service.createStorageLocation(json)

        then: "An S3 Storage Location is created"
        s3sl.class == S3StorageLocation
        s3sl.region == 'ap-southeast-2'
        s3sl.bucket == 'test-bucket'
        s3sl.prefix == '/some/prefix'
        s3sl.accessKey == 'asdfasdf'
        s3sl.secretKey == 'asdfasdf'
        s3sl.publicRead == false

        when: "creating a duplicate S3 Storage Location"
        def s3sl2 = service.createStorageLocation(json)

        then: "the Storage Location is rejected"
        thrown StorageLocationService.AlreadyExistsException

        when: "creating a Swift Storage Location"
        json = [
                type: 'swift',
                authUrl: 'http://localhost:8080/v1/auth',
                authenticationMethod: 'BASIC',
                username: 'test:testing',
                password: 'tester',
                containerName: 'images',
                publicContainer: true
        ]

        def swiftsl = service.createStorageLocation(json)

        then: "A Swift Storage Location is created"
        swiftsl.class == SwiftStorageLocation
        swiftsl.authUrl == 'http://localhost:8080/v1/auth'
        swiftsl.authenticationMethod == AuthenticationMethod.BASIC
        swiftsl.username == 'test:testing'
        swiftsl.password == 'tester'
        swiftsl.containerName == 'images'
        swiftsl.publicContainer == true

        when: "creating a duplicate Swift Storage Location"
        def swiftsl2 = service.createStorageLocation(json)

        then: "The storage location is rejected"
        thrown StorageLocationService.AlreadyExistsException

        when: "creating an unsupported storage location type"
        json = [
                type: 'gcs',
                path: '/something'
        ]

        def gcssl = service.createStorageLocation(json)

        then: "The storage location is rejected"
        thrown RuntimeException
    }

}
