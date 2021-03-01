package au.org.ala.images

import au.org.ala.images.helper.LocalstackRule
import cloud.localstack.Localstack
import cloud.localstack.deprecated.TestUtils
import cloud.localstack.docker.annotation.LocalstackDockerProperties
import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import grails.testing.gorm.DomainUnitTest
import org.junit.ClassRule
import spock.lang.Shared

import static cloud.localstack.deprecated.TestUtils.DEFAULT_REGION

@LocalstackDockerProperties(services = [ "s3" ])
class S3StorageLocationSpec extends StorageLocationSpec implements DomainUnitTest<S3StorageLocation> {

    @ClassRule @Shared LocalstackRule localstack = new LocalstackRule(S3StorageLocationSpec)

    List<S3StorageLocation> getStorageLocations() {[
            new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bouquet', prefix: '', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix2', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix3/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix4/subprefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix5/subprefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: 'prefix6/subprefix/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
            ,new S3StorageLocation(region: DEFAULT_REGION, bucket: 'bucket', prefix: '/prefix7/subprefix/', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: Localstack.INSTANCE.endpointS3).save()
    ]}
    S3StorageLocation alternateStorageLocation

    def setupSpec() {

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().
                withEndpointConfiguration(cloud.localstack.awssdkv1.TestUtils.getEndpointConfigurationS3()).
                withCredentials(cloud.localstack.awssdkv1.TestUtils.getCredentialsProvider()).
                withClientConfiguration(
                        new ClientConfiguration()
                                .withValidateAfterInactivityMillis(200))
        builder.setPathStyleAccessEnabled(true)
        AmazonS3 clientS3 = builder.build()
        clientS3.createBucket('bouquet')
        clientS3.createBucket('bucket')
        clientS3.createBucket('other-bucket')
    }

    def setup() {
        def localstack = Localstack.INSTANCE
        alternateStorageLocation = new S3StorageLocation(region: DEFAULT_REGION, bucket: 'other-bucket', prefix: '/other/prefix', accessKey: TestUtils.TEST_ACCESS_KEY, secretKey: TestUtils.TEST_SECRET_KEY, publicRead: false, redirect: false, pathStyleAccess: true, hostname: localstack.endpointS3).save()
    }

}
