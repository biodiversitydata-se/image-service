package au.org.ala.images

import grails.converters.JSON
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import groovyx.gpars.GParsPool
import java.time.Duration
import java.time.Instant

class BulkImportController {

    def batchService
    def imageService
    def sessionFactory

    def index() { }

    def load(){

        Instant t1 = Instant.now()

        def map = [:]
        int total = 0
        File directory = new File("/data/pipelines-data/dr1411/1/interpreted/multimedia")
        directory.listFiles().each { file ->
            int batchSize = loadAvro(file)
            map[file.getName()] = batchSize
            total += batchSize
        }

        def ns = Duration.between(t1, Instant.now()).toMillis() / 1000;

        render ([total:total, timeTakenInSecs: ns, files:map] as JSON)
    }

    private def loadAvro(File avroFile) {

        def inStream = new FileInputStream(avroFile)
        DataFileStream<GenericRecord> reader = new DataFileStream<>(inStream, new GenericDatumReader<GenericRecord>())

        def batch = []
        int count = 0

        // process record by record
        while (reader.hasNext() && count < 100) {
            GenericRecord currRecord = reader.next()
            // Here we can add in data manipulation like anonymization etc
            def multimediaRecords = currRecord.get("multimediaItems");
            multimediaRecords.each { GenericRecord record ->
                // check URL
                batch << [
                    sourceUrl: record.get("identifier"),
                    originalFilename: record.get("identifier"),
                    dataResourceUid: "dr1411",
                    creator: record.get("creator"),
                    title: record.get("title"),
                    rightsHolder: record.get("rightsHolder"),
                    rights: record.get("rights"),
                    license: record.get("license"),
                    description: record.get("description")
                ]
            }
            count ++;
        }

        scheduleImagesUpload(batch, "test")
        batch.size()
    }

    def scheduleImagesUpload(imageList, userId){

        def batchId = batchService.createNewBatch()

        GParsPool.withPool(10) {
            imageList.eachParallel { srcImage ->
                if (!srcImage.containsKey("importBatchId")) {
                    srcImage["importBatchId"] = batchId
                }
                execute(srcImage, "test")
            }
        }
    }

    void execute(_imageSource,  String _userId) {

        def results = imageService.batchUploadFromUrl([_imageSource], _userId)
        def newImage = results[_imageSource.sourceUrl ?: _imageSource.imageUrl]
        if (newImage && newImage.success) {
            //            Image.withNewTransaction {
            //                imageService.setMetadataItemsByImageId(newImage.image.id, _imageSource, MetaDataSourceType.SystemDefined, _userId)
            //            }
            //            imageService.scheduleArtifactGeneration(newImage.image.id, _userId)
            //            imageService.scheduleImageIndex(newImage.image.id)
            //            imageService.scheduleImageMetadataPersist(newImage.image.id, newImage.image.imageIdentifier,  newImage.image.originalFilename, MetaDataSourceType.Embedded, _userId)
        }
    }
}
