package au.org.ala.images

import grails.converters.JSON
import io.swagger.annotations.Api
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.io.FileUtils

@Api(value = "/ws/batch")
class BatchController {

    def batchService

    def index() { }

    @ApiOperation(
            value = "Upload zipped AVRO files for loading",
            nickname = "upload",
            produces = "application/json",
            consumes = "application/gzip",
            httpMethod = "POST",
            response = Map.class,
            tags = ["BatchUpdate"]
    )
    @ApiResponses([
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Missing dataResourceUid parameter or missing file"),
            @ApiResponse(code = 405, message = "Method Not Allowed. Only GET is allowed")]
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "dataResourceUid", paramType = "path", required = true, value = "Data resource UID", dataType = "string")
    ])
    def upload(){

        //multi part upload
        def zipFile = request.getFile('archive')
        def dataResourceUid = params.dataResourceUid

        if (!zipFile){
            response.sendError(400, "Missing zip file")
            return
        }

        if (!dataResourceUid){
            response.sendError(400, "Missing dataResourceUid parameter")
            return
        }

        File uploadDir = new File("/data/image-service/uploads/tmp-" + System.currentTimeMillis() + "/")
        FileUtils.forceMkdir(uploadDir)
        File tmpFile = new File(uploadDir, zipFile.filename)
        zipFile.transferTo(tmpFile)

        //write zip file to filesystem
        def upload = batchService.createBatchFileUploadsFromZip(dataResourceUid, tmpFile)

        //return an async response
        def response = [
                batchID : upload.getId(),
                dataResourceUid: upload.dataResourceUid,
                file: upload.filePath,
                dateCreated: upload.getDateCreated(),
                status: upload.status,
                files: []
        ]
        upload.batchFiles.each {
            response.files << [
                    status: it.status,
                    file: it.filePath,
                    invalidRecords: it.invalidRecords,
                    recordCount: it.recordCount
            ]
        }
        render (response as JSON)
    }

    @ApiOperation(
            value = "Get batch update status",
            nickname = "status/{batchID}",
            produces = "application/json",
            httpMethod = "GET",
            response = Map.class,
            tags = ["BatchUpdate"]
    )
    @ApiResponses([
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Missing batchID parameter or missing file"),
            @ApiResponse(code = 405, message = "Method Not Allowed. Only GET is allowed")]
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "batchId", paramType = "path", required = true, value = "Batch ID", dataType = "string")
    ])
    def status(){

        //write zip file to filesystem
        def upload = batchService.getBatchFileUpload(params.id)
        if (upload){
            //return an async response
            def response = createResponse(upload)
            render (response as JSON)
        } else {
            response.sendError(404)
        }
    }

    @ApiOperation(
            value = "Get batch update status",
            nickname = "dataresource/{dataResourceUid}",
            produces = "application/json",
            httpMethod = "GET",
            response = Map.class,
            tags = ["BatchUpdate"]
    )
    @ApiResponses([
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Missing dataResourceUid parameter or missing file"),
            @ApiResponse(code = 405, message = "Method Not Allowed. Only GET is allowed")]
    )
    @ApiImplicitParams([
            @ApiImplicitParam(name = "dataResourceUid", paramType = "path", required = true, value = "Data resource UID", dataType = "string")
    ])
    def statusForDataResource(){

        //write zip file to filesystem
        def uploads = batchService.getBatchFileUploadsFor(params.dataResourceUid)
        if (uploads){
            //return an async response
            def responses = []
            uploads.each { upload ->
                responses << createResponse(upload)
            }
            render (responses as JSON)
        } else {
            response.sendError(404)
        }
    }

    Map createResponse(BatchFileUpload upload){
        //return an async response
        def response = [
                batchID : upload.getId(),
                dataResourceUid: upload.dataResourceUid,
                file: upload.filePath,
                dateCreated: upload.getDateCreated(),
                status: upload.status,
                files: []
        ]
        upload.batchFiles.each {
            response.files << [
                    status: it.status,
                    file: it.filePath,
                    invalidRecords: it.invalidRecords,
                    recordCount: it.recordCount
            ]
        }
        response
    }
}
