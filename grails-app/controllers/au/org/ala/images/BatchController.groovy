package au.org.ala.images

import grails.converters.JSON
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses

@Api(value = "/ws/batch", description = "Image Web Services")
class BatchController {

    def batchService

    def index() { }

    @ApiOperation(
            value = "Upload zipped AVRO files for loading",
            nickname = "upload",
            produces = "application/jdson",
            consumes = "application/gzip",
            httpMethod = "GET",
            response = Map.class,
            tags = ["Export"]
    )
    @ApiResponses([
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Missing dataResourceUid parameter"),
            @ApiResponse(code = 405, message = "Method Not Allowed. Only GET is allowed")]
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "dataResourceUid", paramType = "path", required = true, value = "Data resource UID", dataType = "string")
    ])
    def upload(){

        //multi part upload

        //data resource uid
        def dataResource = "dr1411"
        def zipFile = new File("/data/pipelines-data/dr893/1/interpreted/multimedia/interpret-1600456272.avro.zip")

        //write zip file to filesystem
        def upload = batchService.createBatchFileUploadsFromZip(dataResource, zipFile)

        //schedule an upload job

        //return an async response
        render (upload as JSON)
    }
}
