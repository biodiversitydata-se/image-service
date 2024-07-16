package au.org.ala.images

import au.org.ala.images.util.ByteSinkFactory
import com.amazonaws.AmazonClientException
import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.Protocol
import com.amazonaws.SdkClientException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.Grant
import com.amazonaws.services.s3.model.GroupGrantee
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.Permission
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3ObjectSummary
import groovy.transform.EqualsAndHashCode
import net.lingala.zip4j.io.inputstream.ZipInputStream
import org.apache.commons.io.FilenameUtils

@EqualsAndHashCode(includes = ['region', 'bucket', 'prefix'])
class S3StorageLocation extends StorageLocation {

    String region
    String bucket
    String prefix
    String accessKey
    String secretKey
    boolean publicRead
    boolean redirect

    // for testing only, not exposed to UI
    boolean pathStyleAccess = false
    String hostname = ''

    static transients = ['_s3Client']

    static constraints = {
        prefix nullable: false, blank: true
        pathStyleAccess nullable: true
        hostname nullable: true
    }

    static mapping = {
        cache true
    }

    private AmazonS3 _s3Client

    private AmazonS3 getS3Client() {
        if (!_s3Client) {
            def builder = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                    .withClientConfiguration(buildClientConfiguration([:], [:]))
            if (pathStyleAccess) {
                builder.pathStyleAccessEnabled = true
            }
            if (hostname) {
                builder.endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(hostname, region)
            } else {
                builder.region = region
            }

            _s3Client = builder.build()
        }
        _s3Client
    }

    @Override
    boolean isSupportsRedirect() {
        redirect
    }

    @Override
    URI redirectLocation(String path) {
//        def permission = s3Client.getObjectAcl(bucket, path).grantsAsList.find { grant ->
//            grant.grantee == GroupGrantee.AllUsers && [Permission.Read, Permission.Write, Permission.FullControl].contains(grant.permission)
//        }
        if (publicRead) {
            s3Client.getUrl(bucket, path).toURI()
        } else {
            // Set the presigned URL to expire after one hour.
            Date expiration = new Date()
            long expTimeMillis = expiration.getTime()
            expTimeMillis += 1000 * 60 * 60;
            expiration.setTime(expTimeMillis)

            // Generate the presigned URL.
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucket, path)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration)
            s3Client.generatePresignedUrl(generatePresignedUrlRequest).toURI()
        }
    }

    def updateACL() {
        walkPrefix(storagePathStrategy().basePath()) { S3ObjectSummary objectSummary ->
            def path = objectSummary.key
            try {
                s3Client.setObjectAcl(bucket, path, publicRead ? CannedAccessControlList.PublicRead : CannedAccessControlList.Private)

                // update cache control metadata
                def objectMetadata = s3Client.getObjectMetadata(bucket, path)
                objectMetadata.cacheControl = (publicRead ? 'public,s-maxage=31536000' : 'private') + ',max-age=31536000'

                CopyObjectRequest request = new CopyObjectRequest(bucket, path, bucket, path)
                        .withNewObjectMetadata(objectMetadata)

                s3Client.copyObject(request)
            } catch (AmazonS3Exception e) {
                log.error('Error updating ACL for {}, public: {}, error: {}', path, publicRead, e.message)
            }
        }
    }

    static ClientConfiguration buildClientConfiguration(defaultConfig, serviceConfig) {
        // TODO add config for S3 clients
        Map config = [
                connectionTimeout: defaultConfig.connectionTimeout ?: 0,
                maxConnections: defaultConfig.maxConnections ?: 0,
                maxErrorRetry: defaultConfig.maxErrorRetry ?: 0,
                protocol: defaultConfig.protocol ?: '',
                socketTimeout: defaultConfig.socketTimeout ?: 0,
                userAgent: defaultConfig.userAgent ?: '',
                proxyDomain: defaultConfig.proxyDomain ?: '',
                proxyHost: defaultConfig.proxyHost ?: '',
                proxyPassword: defaultConfig.proxyPassword ?: '',
                proxyPort: defaultConfig.proxyPort ?: 0,
                proxyUsername: defaultConfig.proxyUsername ?: '',
                proxyWorkstation: defaultConfig.proxyWorkstation ?: ''
        ]
        if (serviceConfig) {
            if (serviceConfig.connectionTimeout) config.connectionTimeout = serviceConfig.connectionTimeout
            if (serviceConfig.maxConnections) config.maxConnections = serviceConfig.maxConnections
            if (serviceConfig.maxErrorRetry) config.maxErrorRetry = serviceConfig.maxErrorRetry
            if (serviceConfig.protocol) config.protocol = serviceConfig.protocol
            if (serviceConfig.socketTimeout) config.socketTimeout = serviceConfig.socketTimeout
            if (serviceConfig.userAgent) config.userAgent = serviceConfig.userAgent
            if (serviceConfig.proxyDomain) config.proxyDomain = serviceConfig.proxyDomain
            if (serviceConfig.proxyHost) config.proxyHost = serviceConfig.proxyHost
            if (serviceConfig.proxyPassword) config.proxyPassword = serviceConfig.proxyPassword
            if (serviceConfig.proxyPort) config.proxyPort = serviceConfig.proxyPort
            if (serviceConfig.proxyUsername) config.proxyUsername = serviceConfig.proxyUsername
            if (serviceConfig.proxyWorkstation) config.proxyWorkstation = serviceConfig.proxyWorkstation
        }

        ClientConfiguration clientConfiguration = new ClientConfiguration()
        if (config.connectionTimeout) clientConfiguration.connectionTimeout = config.connectionTimeout
        if (config.maxConnections) clientConfiguration.maxConnections = config.maxConnections
        if (config.maxErrorRetry) clientConfiguration.maxErrorRetry = config.maxErrorRetry
        if (config.protocol) {
            if (config.protocol.toUpperCase() == 'HTTP') clientConfiguration.protocol = Protocol.HTTP
            else clientConfiguration.protocol = Protocol.HTTPS
        }
        if (config.socketTimeout) clientConfiguration.socketTimeout = config.socketTimeout
        if (config.userAgent) clientConfiguration.userAgent = config.userAgent
        if (config.proxyDomain) clientConfiguration.proxyDomain = config.proxyDomain
        if (config.proxyHost) clientConfiguration.proxyHost = config.proxyHost
        if (config.proxyPassword) clientConfiguration.proxyPassword = config.proxyPassword
        if (config.proxyPort) clientConfiguration.proxyPort = config.proxyPort
        if (config.proxyUsername) clientConfiguration.proxyUsername = config.proxyUsername
        if (config.proxyWorkstation) clientConfiguration.proxyWorkstation = config.proxyWorkstation
        clientConfiguration
    }

    private ObjectMetadata generateMetadata(String contentType, String contentDisposition = null, Long length = null) {
        ObjectMetadata metadata = new ObjectMetadata()
        metadata.setContentType(contentType)
        if (contentDisposition) {
            metadata.setContentDisposition(contentDisposition)
        }
        if (length != null) {
            metadata.setContentLength(length)
        }
        def acl
        if (publicRead) {
            acl = CannedAccessControlList.PublicRead
        } else {
            acl = CannedAccessControlList.Private
        }
        metadata.setHeader('x-amz-acl', acl.toString())
        metadata.cacheControl = (publicRead ? 'public,s-maxage=31536000' : 'private') + ',max-age=31536000'
//        metadata.setHeader('Expires', 'access + 1 year???')
        return metadata
    }

    @Override
    boolean verifySettings() {
        try {
            boolean result = s3Client.doesBucketExistV2(bucket)
            if (result) {
                String key = storagePathStrategy().basePath() + '/' + UUID.randomUUID().toString()
                def putResult = s3Client.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(new byte[1]), generateMetadata('application/octet-stream', null, 1)))
                s3Client.deleteObject(bucket, key)
            }
            return result
        } catch (SdkClientException e) {
            log.error("Exception while verifying S3 bucket {}", this, e)
            return false
        }
    }

    @Override
    void store(String uuid, InputStream stream, String contentType = 'image/jpeg', String contentDisposition = null, Long length = null) {
        String path = createOriginalPathFromUUID(uuid)
        storeInternal(stream, path, contentType, contentDisposition, length)
    }

    @Override
    byte[] retrieve(String uuid) {
        if (uuid) {
            def imagePath = createOriginalPathFromUUID(uuid)
            def bytes
            try {
                def s3object = s3Client.getObject(new GetObjectRequest(bucket, imagePath))
                bytes = s3object.objectContent.withStream { it.bytes }
            } catch (AmazonS3Exception e) {
                if (e.statusCode == 404) {
                    throw new FileNotFoundException("S3 path $imagePath")
                } else {
                    throw e
                }
            }
            return bytes
        }
        return null
    }

    @Override
    InputStream inputStream(String path, Range range) throws FileNotFoundException {
        def request = new GetObjectRequest(bucket, path)
        if (range != null && !range.empty) {
            request.setRange(range.start(), range.end())
        }
        try {
            def s3Object = s3Client.getObject(request)
            return s3Object.objectContent
        } catch (AmazonS3Exception e) {
            if (e.statusCode == 404) {
                throw new FileNotFoundException("S3 path $path")
            } else {
                throw e
            }
        }
    }

    @Override
    boolean stored(String uuid) {
        return s3Client.doesObjectExist(bucket, createOriginalPathFromUUID(uuid))
    }

    @Override
    void storeTileZipInputStream(String uuid, String zipInputFileName, String contentType, long length = 0, ZipInputStream zipInputStream) {
        def path = FilenameUtils.normalize(createTilesPathFromUUID(uuid) + '/' + zipInputFileName)
        zipInputStream.withStream { stream ->
            s3Client.putObject(bucket, path, stream, generateMetadata(contentType, null, length))
        }
    }

    long consumedSpace(String uuid) {
        return getConsumedSpaceInternal(storagePathStrategy().createPathFromUUID(uuid, ''))
    }

    @Override
    boolean deleteStored(String uuid) {

        walkPrefix(storagePathStrategy().createPathFromUUID(uuid, '')) { S3ObjectSummary s3ObjectSummary ->
            s3Client.deleteObject(bucket, s3ObjectSummary.key)
        }

        AuditService.submitLog(uuid, "Image deleted from store", "N/A")

        return true
    }

    private long getConsumedSpaceInternal(prefix) {
        ObjectListing objectListing = null
        long size = 0
        List<String> extraPrefixes = []
        def more = true
        while (more) {

            objectListing = (objectListing == null) ? s3Client.listObjects(bucket, prefix) : s3Client.listNextBatchOfObjects(objectListing)

            size += (objectListing.objectSummaries.sum { S3ObjectSummary o -> o.size } ?: 0L)

            extraPrefixes += objectListing.commonPrefixes

            more = objectListing.isTruncated()
        }

        return size + ((extraPrefixes.sum { getConsumedSpaceInternal(it) }) ?: 0L)
    }

    private <T> List<T> walkPrefix(String prefix, Closure<T> f) {
        ObjectListing objectListing = null
        List<T> results = []
        List<String> extraPrefixes = []
        def more = true
        while (more) {
            objectListing = (objectListing == null) ? s3Client.listObjects(bucket, prefix) : s3Client.listNextBatchOfObjects(objectListing)

            results += objectListing.objectSummaries.each(f)

            extraPrefixes += objectListing.commonPrefixes

            more = objectListing.isTruncated()
        }

        return results + extraPrefixes.collectMany { String extraPrefix -> walkPrefix(extraPrefix, f) }
    }

    StoragePathStrategy storagePathStrategy() {
        new DefaultStoragePathStrategy(prefix ?: '', true, false)
    }

    @Override
    ByteSinkFactory thumbnailByteSinkFactory(String uuid) {
        byteSinkFactory(uuid)
    }

    @Override
    ByteSinkFactory tilerByteSinkFactory(String uuid) {
        byteSinkFactory(uuid, 'tms')
    }

    ByteSinkFactory byteSinkFactory(String uuid, String... prefixes) {
        return new S3ByteSinkFactory(s3Client, storagePathStrategy(), bucket, publicRead, uuid, prefixes)
    }

    @Override
    protected storeAnywhere(String uuid, InputStream stream, String relativePath, String contentType = 'image/jpeg', String contentDisposition = null, Long length = null) {
        def path = storagePathStrategy().createPathFromUUID(uuid, relativePath)
        storeInternal(stream, path, contentType, contentDisposition, length)
    }

    private storeInternal(InputStream stream, String absolutePath, String contentType, String contentDisposition, Long length) {
        def client = s3Client
        try {
            def result = stream.withStream {
                client.putObject(bucket, absolutePath, stream, generateMetadata(contentType, contentDisposition, length))
            }
            log.debug("Uploaded {} to S3 {}:{}} with result etag {}}", absolutePath, region, bucket, result.ETag)
        } catch (AmazonS3Exception exception) {
            log.warn 'An amazon S3 exception was caught while storing input stream', exception
            throw new RuntimeException(exception)
        } catch (AmazonClientException exception) {
            log.warn 'An amazon client exception was caught while storing input stream', exception
            throw new RuntimeException(exception)
        }
    }

    @Override
    void migrateTo(String uuid, String contentType, StorageLocation destination) {
        def basePath = createBasePathFromUUID(uuid)
        walkPrefix(basePath) { S3ObjectSummary s3ObjectSummary ->
            def s3Object = s3Client.getObject(bucket, s3ObjectSummary.key)
            destination.storeAnywhere(uuid, s3Object.objectContent, s3ObjectSummary.key - basePath, s3Object.objectMetadata.contentType, s3Object.objectMetadata.contentDisposition, s3ObjectSummary.size)
        }
    }

    @Override
    long storedLength(String path) throws FileNotFoundException {
        try {
            def metadata = s3Client.getObjectMetadata(bucket, path)
            return metadata.contentLength
        } catch (AmazonS3Exception e) {
            if (e.statusCode == 404) {
                throw new FileNotFoundException("S3 path $path")
            } else {
                throw e
            }
        }
    }

    @Override
    String toString() {
        "S3($id): $region:$bucket:${prefix ?: ''}"
    }
}
