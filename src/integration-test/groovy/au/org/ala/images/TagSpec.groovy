package au.org.ala.images

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import groovy.json.JsonSlurper
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Shared
import spock.lang.Specification

@Integration(applicationClass = Application.class)
@Rollback
class TagSpec extends Specification {

    @Shared RestBuilder rest = new RestBuilder()

    def grailsApplication

    private String getBaseUrl() {
        def serverContextPath = grailsApplication.config.getProperty('server.servlet.context-path', String, '')
        def url = "http://localhost:${serverPort}${serverContextPath}"
        return url
    }

    def setup() {}

    def cleanup() {}

    void "test home page"() {
        when:
        RestResponse resp = rest.get("${baseUrl}")
        then:
        resp.status == 200
    }

    void "test create tag"() {
        when:
        RestResponse resp = rest.put("${baseUrl}/ws/tag?tagPath=Birds/Colour/Red")
        def jsonResponse = new JsonSlurper().parseText(resp.body)
        then:
        resp.status == 200
        jsonResponse.tagId != null
    }

    void "test get tag model"() {
        when:
        RestResponse resp = rest.get("${baseUrl}/ws/tags")
        def jsonResponse = new JsonSlurper().parseText(resp.body)
        then:
        resp.status == 200
        jsonResponse.size() > 0
    }

    void "test tag an image"(){
        when:

        //upload an image
        MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>()
        form.add("imageUrl", "https://www.ala.org.au/app/uploads/2019/05/palm-cockatoo-by-Alan-Pettigrew-1920-1200-CCBY-28072018-640x480.jpg")
        RestResponse resp = rest.post("${baseUrl}/ws/uploadImage", {
            contentType("application/x-www-form-urlencoded")
            body(form)
        })

        def jsonResponse = new JsonSlurper().parseText(resp.body)
        def imageId = jsonResponse.imageId

        println("Created image: " + imageId)

        //create a tag
        RestResponse createTag = rest.put("${baseUrl}/ws/tag?tagPath=Birds/Colour/Blue")
        def tagId = new JsonSlurper().parseText(createTag.body).tagId

        //remove existing tags if present
        RestResponse tagRemoveResp = rest.delete("${baseUrl}/ws/tag/${tagId}/image/${imageId}")
        println("Delete response status: " + tagRemoveResp.body)

        //tag the image
        RestResponse tagResp = rest.put("${baseUrl}/ws/tag/${tagId}/image/${imageId}")
        def taggedJson = new JsonSlurper().parseText(tagResp.body)

        println("Create Response status: " + resp.status)
        println(resp.body)


        then:
        tagResp.status == 200
        taggedJson.success == true
    }

}
