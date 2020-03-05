package au.org.ala.images

import au.org.ala.web.AuthService
import grails.plugins.cacheheaders.CacheHeadersService
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.grails.web.util.GrailsApplicationAttributes
import org.jasig.cas.client.authentication.AttributePrincipalImpl
import org.springframework.core.io.ClassPathResource
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicLong

@Slf4j
class ImageControllerSpec extends Specification implements ControllerUnitTest<ImageController>, DataTest {

    def setupSpec() {
        mockDomains Image, Subimage, FileSystemStorageLocation, ImageThumbnail
    }

    @Override
    Closure doWithSpring() {{ ->
        imageService(ImageService)
        cacheHeadersService(CacheHeadersService)
        authService(AuthService)
    }}

    @Override
    Closure doWithConfig() {{ config ->
        config.grails.mime.use.accept.header = true
        config.grails.disable.accept.header.userAgents = []
        config.grails.mime.types.with {
            all = '*/*'
            form = 'application/x-www-form-urlencoded'
            html = [ 'text/html', 'application/xhtml+xml' ]
            image = [ 'image/jpeg', 'image/jpg', 'image/png', 'image/bmp', 'image/tiff', 'image/webp', 'image/apng', 'image/*' ]
            json = [ 'application/json', 'text/json' ]
            xml = [ 'text/xml', 'application/xml' ]

        }
        config.images.cache.headers = true
        config.analytics.trackThumbnails = true
        config.placeholder.sound.thumbnail = "classpath:images/200px-Speaker_Icon.svg.png"
        config.placeholder.sound.large = "classpath:images/500px-Speaker_Icon.svg.png"
        config.placeholder.document.thumbnail = "classpath:images/200px-Document_icon.svg.png"
        config.placeholder.document.large = "classpath:images/500px-Document_icon.svg.png"
        config.placeholder.missing.thumbnail = "classpath:images/200px-Document_icon.svg.png"
    }}

    @Shared String sha1ContentHash = '1234'
    @Shared Date dateUploaded = new Date(1583324141000)
    @Shared ClassPathResource audioThumbnail = new ClassPathResource('images/200px-Speaker_Icon.svg.png')
    @Shared ClassPathResource documentThumbnail = new ClassPathResource('images/200px-Document_icon.svg.png')
    @Shared ClassPathResource audioTypeThumbnail = new ClassPathResource('images/500px-Speaker_Icon.svg.png')
    @Shared ClassPathResource documentTypeThumbnail = new ClassPathResource('images/500px-Document_icon.svg.png')
    @Shared byte[] fileContent = makeBytes(1234)
    static final String userAgent = 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:73.0) Gecko/20100101 Firefox/73.0'
    StorageLocation storageLocation

    def setup() {
        controller.boundaryCounter = new AtomicLong(0) // reset this for each run
        storageLocation = new FileSystemStorageLocation(basePath: '/tmp')
    }

    @Unroll
    def "test serve image (range: #rangeHeader etag: #etag lastMod: #lastModified)"(String fileMimeType, String rangeHeader, int statusCode, String contentType, long length, String etag, Date lastModified) {
        setup:
        Image image = new Image(
                imageIdentifier: UUID.randomUUID().toString(),
                contentSHA1Hash: sha1ContentHash,
                fileSize: fileContent.length,
                mimeType: fileMimeType,
                storageLocation: storageLocation,
                dateUploaded: dateUploaded).save()
        List<Range> ranges = Range.decodeRange(rangeHeader, image.fileSize)
        setupGetImageRequest(image, rangeHeader, etag, lastModified)

        when:
        controller.getOriginalFile()

        then:
        if (statusCode != 304) {
            for (def range : ranges) {
                1 * controller.imageStoreService.originalInputStream(image, range) >> { range.wrapInputStream(new ByteArrayInputStream(fileContent)) }
            }
        }
        1 * controller.analyticsService.sendAnalytics(image, 'imageview', userAgent)
        checkGetImageAssertions(image, ranges, statusCode, length, contentType, fileContent)

        where:
        fileMimeType || rangeHeader    || statusCode || contentType || length || etag || lastModified
        'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null
        'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | null
        'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | 'asdf'| null
        'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | dateUploaded - 1
        'image/jpeg'  | ''              | 304         | null         | 0       | sha1ContentHash | null
        'image/jpeg'  | ''              | 304         | null         | 0       | null | dateUploaded
        'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | sha1ContentHash | null
        'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | null | dateUploaded
        'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null
        'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null
    }

    @Unroll
    def "test serve thumbnail (mimeType: #fileMimeType range: #rangeHeader etag: #etag lastMod: #lastModified)"(String fileMimeType, String rangeHeader, int statusCode, String contentType, long length, String etag, Date lastModified, byte[] bytes) {
        setup:
        Image image = new Image(
                imageIdentifier: UUID.randomUUID().toString(),
                contentSHA1Hash: sha1ContentHash,
                fileSize: fileContent.length * 2, // * 2 so it's larger than the thumbnail
                mimeType: fileMimeType,
                storageLocation: storageLocation,
                dateUploaded: dateUploaded).save()
        List<Range> ranges = Range.decodeRange(rangeHeader, bytes.size())
        setupGetImageRequest(image, rangeHeader, etag, lastModified)

        when:
        controller.proxyImageThumbnail()

        then:
        if (statusCode != 304) {
            fileMimeType.count('image/') * controller.imageStoreService.thumbnailStoredLength(image) >> { bytes.length }
            for (def range : ranges) {
                fileMimeType.count('image/') * controller.imageStoreService.thumbnailInputStream(image, range) >> { range.wrapInputStream(new ByteArrayInputStream(bytes)) }
            }
        }
        1 * controller.analyticsService.sendAnalytics(image, 'imageview', userAgent)
        checkGetImageAssertions(image, ranges, statusCode, length, contentType, bytes)

        where:
        fileMimeType || rangeHeader    || statusCode || contentType || length || etag || lastModified || bytes
        'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        'audio/mpeg'  | ''              | 200         | 'image/png'  | 4408    | null  | null          | audioThumbnail.inputStream.bytes
        'application/pdf'  | ''         | 200         | 'image/png'  | 1260    | null  | null          | documentThumbnail.inputStream.bytes
        'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | null          | fileContent
        'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | 'asdf'| null          | fileContent
        'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | dateUploaded - 1 | fileContent
        'image/jpeg'  | ''              | 304         | null         | 0       | sha1ContentHash | null | fileContent
        'image/jpeg'  | ''              | 304         | null         | 0       | null | dateUploaded    | fileContent
        'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | sha1ContentHash | null | fileContent
        'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | null | dateUploaded    | fileContent
        'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
        'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
    }

    @Unroll
    def "test serve thumbnail #type (mimeType: #fileMimeType range: #rangeHeader etag: #etag lastMod: #lastModified)"(String type, String fileMimeType, String rangeHeader, int statusCode, String contentType, long length, String etag, Date lastModified, byte[] bytes) {
        setup:
        Image image = new Image(
                imageIdentifier: UUID.randomUUID().toString(),
                contentSHA1Hash: sha1ContentHash,
                fileSize: fileContent.length * 2,  // * 2 so it's larger than the thumbnail
                mimeType: fileMimeType,
                storageLocation: storageLocation,
                dateUploaded: dateUploaded).save()
        List<Range> ranges = Range.decodeRange(rangeHeader, bytes.size())
        params.thumbnailType = type
        setupGetImageRequest(image, rangeHeader, etag, lastModified)

        when:
        controller.proxyImageThumbnailType()

        then:
        if (statusCode != 304) {
            if (fileMimeType.startsWith('image')) {
                1 * controller.imageStoreService.thumbnailTypeStoredLength(image, type) >> { bytes.length }
            } else {
                0 * controller.imageStoreService.thumbnailTypeStoredLength(image, type)
            }
            for (def range : ranges) {
                if (fileMimeType.startsWith('image')) {
                    1 * controller.imageStoreService.thumbnailTypeInputStream(image, type, range) >> { range.wrapInputStream(new ByteArrayInputStream(bytes)) }
                } else {
                    0 * controller.imageStoreService.thumbnailTypeInputStream(image, type, range)
                }
            }
        }
        1 * controller.analyticsService.sendAnalytics(image, 'imageview', userAgent)
        checkGetImageAssertions(image, ranges, statusCode, length, contentType, bytes)

        where:
        type   || fileMimeType || rangeHeader    || statusCode || contentType || length || etag || lastModified || bytes
        'large' | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        'square_black'| 'image/jpeg' | ''         | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        'square'| 'image/jpeg'  | ''              | 200         | 'image/png' | 1234    | null  | null          | fileContent
        'large' | 'audio/mpeg'  | ''              | 200         | 'image/png'  | 11376   | null  | null          | audioTypeThumbnail.inputStream.bytes
        'large' | 'application/pdf'  | ''         | 200         | 'image/png'  | 4052    | null  | null          | documentTypeThumbnail.inputStream.bytes
        'large' | 'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | null          | fileContent
        'large' | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | 'asdf'| null          | fileContent
        'large' | 'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | dateUploaded - 1 | fileContent
        'large' | 'image/jpeg'  | ''              | 304         | null         | 0       | sha1ContentHash | null | fileContent
        'large' | 'image/jpeg'  | ''              | 304         | null         | 0       | null | dateUploaded    | fileContent
        'large' | 'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | sha1ContentHash | null | fileContent
        'large' | 'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | null | dateUploaded    | fileContent
        'large' | 'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
        'large' | 'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
    }

    @Unroll
    def "test serve tile #x, #y, #z (mimeType: #fileMimeType range: #rangeHeader etag: #etag lastMod: #lastModified)"(int x, int y, int z, String fileMimeType, String rangeHeader, int statusCode, String contentType, long length, String etag, Date lastModified, byte[] bytes) {
        setup:
        Image image = new Image(
                imageIdentifier: UUID.randomUUID().toString(),
                contentSHA1Hash: sha1ContentHash,
                fileSize: fileContent.length * 2, // * 2 so it's larger than the thumbnail
                mimeType: fileMimeType,
                storageLocation: storageLocation,
                dateUploaded: dateUploaded).save()
        List<Range> ranges = Range.decodeRange(rangeHeader, bytes.size())
        params.x = x
        params.y = y
        params.z = z
        setupGetImageRequest(image, rangeHeader, etag, lastModified)

        when:
        controller.proxyImageTile()

        then:
        if (statusCode != 304) {
            1 * controller.imageStoreService.tileStoredLength(image, x, y, z) >> {
                if (fileMimeType.startsWith('image')) bytes.length else throw new FileNotFoundException('')
            }
            for (def range : ranges) {
                if (fileMimeType.startsWith('image')) {
                    1 * controller.imageStoreService.tileInputStream(image, range, x, y, z) >> { range.wrapInputStream(new ByteArrayInputStream(bytes)) }
                } else {
                    0 * controller.imageStoreService.tileInputStream(image, range, x, y, z)
                }
            }
        }
        0 * controller.analyticsService.sendAnalytics(image, 'imageview', userAgent)
        checkGetImageAssertions(image, ranges, statusCode, length, contentType, bytes)

        where:
        x || y || z || fileMimeType || rangeHeader    || statusCode || contentType || length || etag || lastModified || bytes
        1  | 2  | 3  | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        -1 | -2 | -3 | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        0  | 0  | 0  | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | null  | null          | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | null          | fileContent
        0  | 0  | 0  | 'image/jpeg'  | ''              | 200         | 'image/jpeg' | 1234    | 'asdf'| null          | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=100-200' | 206         | 'image/jpeg' | 101     | null  | dateUploaded - 1 | fileContent
        0  | 0  | 0  | 'image/jpeg'  | ''              | 304         | null         | 0       | sha1ContentHash | null | fileContent
        0  | 0  | 0  | 'image/jpeg'  | ''              | 304         | null         | 0       | null | dateUploaded    | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | sha1ContentHash | null | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=100-200' | 304         | null         | 0       | null | dateUploaded    | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
        0  | 0  | 0  | 'image/jpeg'  | 'bytes=1-2,3-4' | 206         | "multipart/byteranges; boundary=00000000000000000001" | 202 | null | null | fileContent
        0  | 0  | 0  | 'audio/mpeg'  | ''              | 404         | 'text/plain;charset=utf-8' | 0       | null  | null          | new byte[0]
        0  | 0  | 0  | 'application/pdf'  | ''         | 404         | 'text/plain;charset=utf-8' | 0       | null  | null          | new byte[0]
    }

    def "test text/html details"() {
        setup:
        controller.collectoryService = Mock(CollectoryService)
        def imageStoreService = Mock(ImageStoreService)
        controller.imageStoreService = imageStoreService
        controller.imageService.imageStoreService = imageStoreService
        Image image = new Image(dataResourceUid: 'dr-1234', imageIdentifier: UUID.randomUUID().toString(), contentSHA1Hash: sha1ContentHash, fileSize: fileContent.length, mimeType: 'image/jpeg', storageLocation: storageLocation, dateUploaded: dateUploaded).save()
        ImageThumbnail thumbnail = new ImageThumbnail(image: image, width: 200, height: 200, isSquare: true, name: 'thumbnail_square').save()

        request.addHeader('Accept', 'text/html')
        // Grails unit testing doesn't care about the Accept header?!
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, 'html')
        params.id = image.imageIdentifier

        request.userPrincipal = new AttributePrincipalImpl('1234', [userid: '1234', email: 'test@example.org'])
        request.addUserRole('ROLE_USER')

        when:
        def model = controller.details()

        then:
        1 * controller.collectoryService.getResourceLevelMetadata('dr-1234') >> ['resource':'level']
        1 * imageStoreService.getThumbUrlByName(image.imageIdentifier, thumbnail.name) >> 'https://devt.ala.org.au/image-service/store/1/2/3/4/1234-1234-1234-1234/thumbnail_square'
        1 * imageStoreService.consumedSpace(image) >> 54321
        view == '/image/details.gsp'
        model.imageInstance == image
        model.subimages == []
        model.parentImage == null
        model.sizeOnDisk == 54321
        model.squareThumbs == ['https://devt.ala.org.au/image-service/store/1/2/3/4/1234-1234-1234-1234/thumbnail_square']
        model.isImage == true
        model.resourceLevel == ['resource':'level']
        model.isAdmin == false
        model.userId == '1234'
    }

    def "test image/* details"() {
        setup:
        controller.imageStoreService = Mock(ImageStoreService)
        controller.analyticsService = Mock(AnalyticsService)
        Image image = new Image(dataResourceUid: 'dr-1234', imageIdentifier: UUID.randomUUID().toString(), contentSHA1Hash: sha1ContentHash, fileSize: fileContent.length, mimeType: 'image/jpeg', storageLocation: storageLocation, dateUploaded: dateUploaded).save()
        ImageThumbnail thumbnail = new ImageThumbnail(image: image, width: 200, height: 200, isSquare: true, name: 'thumbnail_square').save()

        request.addHeader('Accept', 'image/jpeg')
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, 'image')
        request.addHeader('User-Agent', userAgent)
        params.id = image.imageIdentifier

        request.userPrincipal = new AttributePrincipalImpl('1234', [userid: '1234', email: 'test@example.org'])
        request.addUserRole('ROLE_USER')
        Range range = Range.emptyRange(image.fileSize)

        when:
        controller.details()

        then:
        1 * controller.imageStoreService.originalInputStream(image, range) >> { range.wrapInputStream(new ByteArrayInputStream(fileContent)) }
        1 * controller.analyticsService.sendAnalytics(image, 'imageview', userAgent)
        checkGetImageAssertions(image, [range], 200, image.fileSize, image.mimeType, fileContent)
    }

    def "test jsonp details"() {
        setup:
        controller.imageStoreService = Mock(ImageStoreService)
        controller.analyticsService = Mock(AnalyticsService)
        Image image = new Image(dataResourceUid: 'dr-1234', imageIdentifier: UUID.randomUUID().toString(), contentSHA1Hash: sha1ContentHash, fileSize: fileContent.length, mimeType: 'image/jpeg', storageLocation: storageLocation, dateUploaded: dateUploaded).save()
        ImageThumbnail thumbnail = new ImageThumbnail(image: image, width: 200, height: 200, isSquare: true, name: 'thumbnail_square').save()

        request.addHeader('Accept', 'text/json')
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, 'json')
        request.addHeader('User-Agent', userAgent)
        params.id = image.imageIdentifier
        params.callback = 'callback'

        request.userPrincipal = new AttributePrincipalImpl('1234', [userid: '1234', email: 'test@example.org'])
        request.addUserRole('ROLE_USER')
        Range range = Range.emptyRange(image.fileSize)

        when:
        controller.details()

        then:
        response.contentType == 'text/javascript'
        response.contentAsString.matches(/callback\((.*)\)/) == true
        def json = (response.contentAsString =~ /callback\((.*)\)/).with { matches() ? it[0][1] : null }?.with { new JsonSlurper().parseText(it) }
        json != null
        json.imageIdentifier == image.imageIdentifier
    }

    def "test json details"() {
        setup:
        controller.imageStoreService = Mock(ImageStoreService)
        controller.analyticsService = Mock(AnalyticsService)
        Image image = new Image(dataResourceUid: 'dr-1234', imageIdentifier: UUID.randomUUID().toString(), contentSHA1Hash: sha1ContentHash, fileSize: fileContent.length, mimeType: 'image/jpeg', storageLocation: storageLocation, dateUploaded: dateUploaded).save()
        ImageThumbnail thumbnail = new ImageThumbnail(image: image, width: 200, height: 200, isSquare: true, name: 'thumbnail_square').save()

        request.addHeader('Accept', 'text/json')
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, 'json')
        request.addHeader('User-Agent', userAgent)
        params.id = image.imageIdentifier

        request.userPrincipal = new AttributePrincipalImpl('1234', [userid: '1234', email: 'test@example.org'])
        request.addUserRole('ROLE_USER')
        Range range = Range.emptyRange(image.fileSize)

        when:
        controller.details()

        then:
        response.contentType == 'application/json;charset=UTF-8'
        def json = response.json
        json.imageIdentifier == image.imageIdentifier
    }

    def "test xml details"() {
        setup:
        controller.imageStoreService = Mock(ImageStoreService)
        controller.analyticsService = Mock(AnalyticsService)
        Image image = new Image(dataResourceUid: 'dr-1234', imageIdentifier: UUID.randomUUID().toString(), contentSHA1Hash: sha1ContentHash, fileSize: fileContent.length, mimeType: 'image/jpeg', storageLocation: storageLocation, dateUploaded: dateUploaded).save()
        ImageThumbnail thumbnail = new ImageThumbnail(image: image, width: 200, height: 200, isSquare: true, name: 'thumbnail_square').save()

        request.addHeader('Accept', 'application/xml')
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, 'xml')
        request.addHeader('User-Agent', userAgent)
        params.id = image.imageIdentifier

        request.userPrincipal = new AttributePrincipalImpl('1234', [userid: '1234', email: 'test@example.org'])
        request.addUserRole('ROLE_USER')
        Range range = Range.emptyRange(image.fileSize)

        when:
        controller.details()

        then:
        response.contentType == 'application/xml;charset=UTF-8'
        def xml = response.xml
        xml.imageIdentifier == image.imageIdentifier
    }

    private void setupGetImageRequest(Image image, String rangeHeader, String etag, Date lastModified) {
        params.id = image.imageIdentifier
        controller.imageStoreService = Mock(ImageStoreService)
        controller.analyticsService = Mock(AnalyticsService)
        request.addHeader('Accept', 'image/*')
        request.addHeader('User-Agent', userAgent)
        if (rangeHeader) {
            request.addHeader('Range', rangeHeader)
        }
        if (etag) {
            request.addHeader('If-None-Match', etag)
        }
        if (lastModified) {
            request.addHeader('If-Modified-Since', lastModified)
        }
    }

    private void checkGetImageAssertions(Image image, List<Range> ranges, int statusCode, long expectedLength, String expectedContentType, byte[] content) {
        assert response.status == statusCode
        assert response.contentLengthLong == expectedLength
        assert response.contentType == expectedContentType
        if (statusCode < 400) {
            assert response.header('etag') == image.contentSHA1Hash
            assert response.getDateHeader('last-modified') == image.dateUploaded.time
        }
        if (statusCode < 300) {
            assert response.getDateHeader('Expires') > (new Date() + 364).time
        }
        if (statusCode < 300 && ranges.size() == 1) {
            assert response.contentAsByteArray == content[(ranges[0].start())..(ranges[0].end())] as byte[]
        }
        // TODO test multipart response body
        response.reset()
        response.resetBuffer()

    }

    private static Random random = new Random()

    private static byte[] makeBytes(int length) {
        byte[] bytes = new byte[length]
        random.nextBytes(bytes)
        return bytes
    }

}
