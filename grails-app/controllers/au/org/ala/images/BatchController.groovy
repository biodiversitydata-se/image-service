package au.org.ala.images

import au.ala.org.ws.security.RequireApiKey
import grails.converters.JSON
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse

import org.apache.commons.io.FileUtils

import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH

class BatchController {

    def batchService

    def index() { }

    @Operation(
            method = "POST",
            summary = "Upload zipped AVRO files for loading",
            parameters = [
                    @Parameter(name="dataResourceUid", in = PATH, description = 'Data Resource UID', required = true)
            ],
            requestBody = @RequestBody(
                    description = "The gzipped upload file",
                    required = true,
                    content = [
                            @Content(mediaType = 'application/gzip', schema = @Schema(name='archive', title='The file to upload', type='string', format='binary'))
                    ]
            ),
            responses = [
                    @ApiResponse(content = [
                            @Content(mediaType='application/json', schema = @Schema(implementation=Map))
                    ])
            ],
            tags = ['BatchUpdate']
    )
    @Consumes('multipart/form-data')
    @Produces("application/json")
    @Path("/ws/batch/upload")
    @RequireApiKey(scopes = ["images:write"])
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

        // move zip file from tmp working directory to uploads directory
        File uploadDir = new File(
                grailsApplication.config.imageservice.batchUpload +
                "/tmp-" + System.currentTimeMillis() + "/")
        FileUtils.forceMkdir(uploadDir)
        File tmpFile = new File(uploadDir, zipFile.originalFilename)
        zipFile.transferTo(tmpFile)

        // unpack zip
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

        // add file details to response for validation by client
        upload.batchFiles.each {
            response.files << [
                    status: it.status,
                    file: it.filePath,
                    recordCount: it.recordCount
            ]
        }
        render (response as JSON)
    }

    @Operation(
            method = "GET",
            summary = "Get batch update status",
            parameters = [
                    @Parameter(name="batchID", in = PATH, description = 'The batch id', required = true)
            ],
            responses = [
                    @ApiResponse(content = [
                            @Content(mediaType='application/json', schema = @Schema(implementation=Map))
                    ]),
                    @ApiResponse(responseCode = "404")
            ],
            tags = ['BatchUpdate']
    )
    @Produces("application/json")
    @Path("/ws/batch/status/{batchID}")
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

    @Operation(
            method = "GET",
            summary = "Get batch update status",
            parameters = [
                    @Parameter(name="dataResourceUid", in = PATH, description = 'Data Resource UID', required = true)
            ],
            responses = [
                    @ApiResponse(content = [
                            @Content(mediaType='application/json', array = @ArraySchema(schema = @Schema(implementation=Map)))
                    ]),
                    @ApiResponse(responseCode = "404")
            ],
            tags = ['BatchUpdate']
    )
    @Produces("application/json")
    @Path("/ws/batch/dataresource/{dataResourceUid}")
    def statusForDataResource(){

        //write zip file to filesystem
        def uploads = batchService.getBatchFileUploadsFor(params.dataResourceUid) ?: []
        //return an async response
        def responses = uploads.collect { upload ->
            createResponse(upload)
        }
        render (responses as JSON)
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
                    recordCount: it.recordCount
            ]
        }
        response
    }
}
