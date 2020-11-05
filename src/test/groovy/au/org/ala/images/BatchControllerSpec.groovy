package au.org.ala.images

import grails.testing.web.controllers.ControllerUnitTest
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.avro.io.DatumWriter
import org.grails.plugins.testing.GrailsMockMultipartFile
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream;

class BatchControllerSpec extends Specification implements ControllerUnitTest<BatchController> {

    def uploadFile = null

    def setup() {
        uploadFile = AvroUtils.generateTestArchive()
    }

    def "test avro zip file upload - missing zip file"() {
        setup:
        controller.batchService = Mock(BatchService)

        when:
        controller.upload()

        then:
        response.status == 400
    }

    def "test avro zip file upload - missing data resource uid"() {
        setup:
        controller.batchService = Mock(BatchService)

        def multipartFile = new GrailsMockMultipartFile(
                'archive',
                '/tmp/data.avro.zip',
                'application/zip', uploadFile.bytes)
        request.addFile(multipartFile)

        when:
        controller.upload()

        then:
        response.status == 400
    }

    def "test avro zip file upload - 200"() {
        setup:
        controller.batchService = Mock(BatchService)
        controller.batchService.createBatchFileUploadsFromZip( _ as String, _ as File) >>  { String dataResourceUid, File uploadFile ->
            return new BatchFileUpload(id:"1")
        }

        def multipartFile = new GrailsMockMultipartFile(
                'archive',
                '/tmp/data.avro.zip',
                'application/zip',
                uploadFile.bytes)
        request.addFile(multipartFile)
        params.dataResourceUid = 'dr1'

        when:
        controller.upload()

        then:
        response.status == 200
    }
}
