package au.org.ala.images

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

@Integration(applicationClass = Application.class)
@Rollback
class SearchSpec extends Specification {

    @Shared RestBuilder rest = new RestBuilder()

    def grailsApplication

    private String getBaseUrl() {
        def serverContextPath = grailsApplication.config.getProperty('server.contextPath', String, '')
        def url = "http://localhost:${serverPort}${serverContextPath}"
        return url
    }

    def setup() {}

    def cleanup() {}

    void 'test upload'() {

        when:
        def occurrenceID = "f4c13adc-2926-44c8-b2cd-fb2d62378a1a"

        //first upload an image
        def testUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Red_kangaroo_-_melbourne_zoo.jpg/800px-Red_kangaroo_-_melbourne_zoo.jpg"

        RestResponse uploadResponse = rest.post("${baseUrl}/ws/uploadImagesFromUrls", {
            json {
                [images: [[
                                  sourceUrl   : testUrl,
                                  occurrenceID: occurrenceID
                          ]]]
            }
        })
        def jsonUploadResponse = new JsonSlurper().parseText(uploadResponse.body)

        then:
        uploadResponse.status == 200
        jsonUploadResponse.success == true
    }


    void 'test search for previous upload'() {

        when:

        Thread.sleep(5000)

        boolean hasBacklog = true
        int counter = 0
        int MAX_CHECKS = 10

        while (hasBacklog && counter < MAX_CHECKS) {
            RestResponse statsResp = rest.get("${baseUrl}/ws/backgroundQueueStats")
            def json = new JsonSlurper().parseText(statsResp.body)
            if (json.queueLength > 0) {
                println("Queue length: " + json.queueLength)
                Thread.sleep(5000)
            } else {
                hasBacklog = false
            }
            counter += 1
        }

        RestResponse countsResp = rest.get("${baseUrl}/ws/search")
        def jsonCount = new JsonSlurper().parseText(countsResp.body)
        jsonCount

        //search by occurrence ID
        RestResponse resp = rest.post("${baseUrl}/ws/findImagesByMetadata", {
            json {
                [
                    "key"   : "occurrenceid",
                    "values": ["f4c13adc-2926-44c8-b2cd-fb2d62378a1a"]
                ]
            }
        })
        def jsonResponse = new JsonSlurper().parseText(resp.body)

        then:
        resp.status == 200
        jsonResponse.count > 0
        //check for legacy fields
        jsonResponse.images.size() > 0
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].imageId != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].tileZoomLevels != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].filesize != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].imageUrl != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].largeThumbUrl != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].squareThumbUrl != null
        jsonResponse.images.get("f4c13adc-2926-44c8-b2cd-fb2d62378a1a")[0].thumbUrl != null
    }
}
