package au.org.ala.images

import grails.core.GrailsApplication
import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.testing.mixin.integration.Integration
import grails.transaction.*
import groovy.json.JsonSlurper
import image.service.Application
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

@Integration(applicationClass = Application.class)
@Rollback
class ImageUploadSpec extends Specification {

    @Shared RestBuilder rest = new RestBuilder()

    GrailsApplication grailsApplication

    private String getBaseUrl() {
        def serverContextPath = grailsApplication.config.getProperty('server.contextPath', String, '')
        def url = "http://localhost:${serverPort}${serverContextPath}"
        return url
    }

    def setup() {}

    def cleanup() {}

    void "test home page"() {
        when:
        RestResponse resp = rest.get("${baseUrl}")
        println(resp.status)
        then:
        resp.status == 200
    }

    void "test upload image - empty request - should result in 400"() {
        when:
        RestResponse resp = rest.post("${baseUrl}/ws/uploadImagesFromUrls", [:])
        then:
        resp.status == 400
    }

    void "test upload image"() {
        when:
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>()
        form.add("imageUrl", "https://www.ala.org.au/app/uploads/2019/05/palm-cockatoo-by-Alan-Pettigrew-1920-1200-CCBY-28072018-640x480.jpg")

        RestResponse resp = rest.post("${baseUrl}/ws/uploadImage", {
            contentType("application/x-www-form-urlencoded")
            body(form)
        })
        then:
        resp.status == 200
    }

    void "test multi upload image - bad submission"() {
        when:

        def url1 = "https://www.ala.org.au/app/uploads/2019/05/mycena-epipterygia-by-Reiner-Richter-CCBYNCInt-26052018-1920-1200--640x480.jpg"
        def url2 = "https://www.ala.org.au/app/uploads/2019/06/Rufous-Betting-by-Graham-Armstrong-CCBY-25-Apr-2019-1920-x-1200-640x480.jpg"

        RestResponse resp = rest.post("${baseUrl}/ws/uploadImagesFromUrls",{
            json {
                [images:[[sourceURL:url1], [sourceURL: url2]]]
            }
        })

        then:
        resp.status == 400
    }


    void "test multi upload image - good submission"() {
        when:

        def url1 = "https://www.ala.org.au/app/uploads/2019/05/mycena-epipterygia-by-Reiner-Richter-CCBYNCInt-26052018-1920-1200--640x480.jpg"
        def url2 = "https://www.ala.org.au/app/uploads/2019/06/Rufous-Betting-by-Graham-Armstrong-CCBY-25-Apr-2019-1920-x-1200-640x480.jpg"


        RestResponse resp = rest.post("${baseUrl}/ws/uploadImagesFromUrls",{
            json {
                [images:[[sourceUrl:url1], [sourceUrl: url2]]]
            }
        })
        def jsonResponse = new JsonSlurper().parseText(resp.body)

        then:
        resp.status == 200
        jsonResponse.success == true
        jsonResponse.results.get(url1).success == true
        jsonResponse.results.get(url2).success == true
        jsonResponse.results.get(url1).imageId != null
        jsonResponse.results.get(url2).imageId != null
    }

    @Ignore
    /* Set to ignore as it is problematic in Travis - working here */
    void 'test iNaturalist bug'(){
        when:

        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>()
        form.add("imageUrl", "https://static.inaturalist.org/photos/35335345/original.jpeg?1555821308")
        RestResponse resp = rest.post("${baseUrl}/ws/uploadImage", {
            contentType("application/x-www-form-urlencoded")
            body(form)
        })
        def jsonResponse1 = new JsonSlurper().parseText(resp.body)

        MultiValueMap<String, String> form2 = new LinkedMultiValueMap<String, String>()
        form2.add("imageUrl", "https://static.inaturalist.org/photos/35335341/original.jpeg?1555821307")
        RestResponse resp2 = rest.post("${baseUrl}/ws/uploadImage", {
            contentType("application/x-www-form-urlencoded")
            body(form2)
        })
        def jsonResponse2 = new JsonSlurper().parseText(resp2.body)

        then:
        resp.status == 200
        resp2.status == 200
        jsonResponse1.imageId != null
        jsonResponse2.imageId != null
        jsonResponse1.imageId != jsonResponse2.imageId
    }

    void 'test multi-part upload submission'(){
        when:

        File imageFile = new File("src/integration-test/resources/test.jpg")

        RestResponse resp = rest.post("${baseUrl}/ws/uploadImage") {
            contentType "multipart/form-data"
            setProperty "image", imageFile
        }
        def jsonResponse = new JsonSlurper().parseText(resp.body)

        println("Response status: " + resp.status)
        println(resp.body)

        then:
        resp.status == 200
        jsonResponse.imageId != null
    }
}
