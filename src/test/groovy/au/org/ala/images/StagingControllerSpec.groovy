package au.org.ala.images

import grails.plugins.cacheheaders.CacheHeadersService
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.commons.io.FileUtils
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import org.springframework.core.io.ClassPathResource
import spock.lang.Shared
import spock.lang.Specification

class StagingControllerSpec extends Specification implements ControllerUnitTest<StagingController> {

    @ClassRule @Shared TemporaryFolder tempFolder = new TemporaryFolder()

    @Override
    Closure doWithSpring() {{ ->
        cacheHeadersService(CacheHeadersService)
    }}

    @Override
    Closure doWithConfig() {{ c ->
            c.imageservice.imagestore.staging = tempFolder.newFolder('staging')
    }}

    def "test path traversal rejected"() {
        setup:
        params.path = '../../../etc/shadow'

        when:
        controller.serve()

        then:
        response.status == 400
    }

    def "test serve jpg file"() {
        setup:
        def testjpg = new ClassPathResource('test.jpg')
        FileUtils.copyInputStreamToFile(testjpg.inputStream, new File(tempFolder.newFolder('staging', 'userid'), 'test.jpg'))
        params.path = 'userid/test.jpg'

        when:
        controller.serve()

        then:
        response.contentType == 'image/jpeg'
        assert !response.getHeader('etag').empty
        assert !response.getHeader('last-modified').empty
        response.contentAsByteArray == testjpg.inputStream.bytes
    }

    def "test serve jpg HEAD"() {
        setup:
        def testjpg = new ClassPathResource('test.jpg')
        FileUtils.copyInputStreamToFile(testjpg.inputStream, new File(tempFolder.newFolder('staging', 'userid2'), 'test.jpg'))
        params.path = 'userid/test.jpg'
        request.method = 'HEAD'

        when:
        controller.serve()

        then:
        response.contentType == 'image/jpeg'
        assert !response.getHeader('etag').empty
        assert !response.getHeader('last-modified').empty
        response.contentAsByteArray == new byte[0]
    }

    def "test serve csv txt file"() {
        setup:
        def testjpg = new ClassPathResource('test.txt')
        FileUtils.copyInputStreamToFile(testjpg.inputStream, new File(tempFolder.newFolder('staging', 'userid', 'datafile'), 'datafile.txt'))
        params.path = 'userid/datafile/datafile.txt'

        when:
        controller.serve()

        then:
        response.contentType == 'text/plain'
        assert !response.getHeader('etag').empty
        assert !response.getHeader('last-modified').empty
        response.contentAsByteArray == testjpg.inputStream.bytes
    }
}
