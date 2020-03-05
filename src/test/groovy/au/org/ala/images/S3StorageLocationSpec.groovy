package au.org.ala.images

import au.org.ala.images.helper.LocalstackRule
import cloud.localstack.Localstack
import cloud.localstack.TestUtils
import cloud.localstack.docker.annotation.LocalstackDockerProperties
import grails.testing.gorm.DomainUnitTest
import org.junit.ClassRule
import spock.lang.Shared

import static cloud.localstack.TestUtils.DEFAULT_REGION

@LocalstackDockerProperties(services = [ "s3" ])
class S3StorageLocationSpec extends StorageLocationSpec implements DomainUnitTest<S3StorageLocation> {

    @ClassRule @Shared LocalstackRule localstack = new LocalstackRule(S3StorageLocationSpec)

    List<S3StorageLocation> getStorageLocations() {[
            new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bouquet', prefix: '', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix2', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix3/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix4/subprefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix5/subprefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix6/subprefix/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix7/subprefix/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
    ]}
    S3StorageLocation alternateStorageLocation

    def setupSpec() {
        TestUtils.clientS3.createBucket('bouquet')
        TestUtils.clientS3.createBucket('bucket')
        TestUtils.clientS3.createBucket('other-bucket')
    }

    def setup() {
        def localstack = Localstack.INSTANCE
        alternateStorageLocation = new S3StorageLocation(region: DEFAULT_REGION, bucket: 'other-bucket', prefix: '/other/prefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, pathStyleAccess: true, hostname: localstack.endpointS3).save()
    }

}
