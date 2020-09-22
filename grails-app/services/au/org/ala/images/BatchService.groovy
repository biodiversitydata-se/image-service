package au.org.ala.images

import com.google.common.io.Files
import groovyx.gpars.GParsPool
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.commons.io.FileUtils

import java.security.MessageDigest

class BatchService {

    private final static Map<String, BatchStatus> _Batches = [:]

    def imageService

    String createNewBatch() {
        BatchStatus status = new BatchStatus()

        synchronized (_Batches) {
            _Batches[status.batchId] = status
        }

        return status.batchId
    }

    void addTaskToBatch(String batchId, BackgroundTask task) {

        synchronized (_Batches) {
            if (!_Batches.containsKey(batchId)) {
                throw new RuntimeException("Unknown or invalid batch id!")
            }
            def status = _Batches[batchId]
            def batchTask = new BatchBackgroundTask(batchId, status.taskCount, task, this)
            status.taskCount = status.taskCount + 1

            imageService.scheduleBackgroundTask(batchTask)
        }
    }

    void notifyBatchTaskComplete(String batchId, int taskSequenceNumber, Object result) {

        if (!_Batches.containsKey(batchId)) {
            return
        }

        def status = _Batches[batchId]
        status.tasksCompleted = status.tasksCompleted + 1
        status.results[taskSequenceNumber] = result

        if (status.tasksCompleted == status.taskCount) {
            status.timeFinished = new Date()
        }
    }

    BatchStatus getBatchStatus(String batchId) {

        if (!_Batches.containsKey(batchId)) {
            return null
        }

        return _Batches[batchId]
    }

    void finaliseBatch(String batchId) {
        synchronized (_Batches) {
            if (!_Batches.containsKey(batchId)) {
                return
            }

            _Batches.remove(batchId)
        }
    }

    def getBatchFileUploadsFor(String dataResourceUid){
        BatchFileUpload.findAllByDataResourceUid(dataResourceUid)
    }

    def getBatchFileUpload(String uploadId){
        BatchFileUpload.findById(uploadId)
    }

    def getActiveBatchUploadCount(){
        BatchFileUpload.countByStatusNotEqual("COMPLETED")
    }


    BatchFileUpload createBatchFileUploadsFromZip(String dataResourceUid, File uploadedFile){

        String md5Hash = generateMD5(uploadedFile)
        BatchFileUpload upload = BatchFileUpload.findByMd5Hash(md5Hash)
        if (upload){
            // prevent multiple uploads of the same file
            return upload
        }

        upload = new BatchFileUpload(
                filePath: uploadedFile.getAbsolutePath(),
                md5Hash: md5Hash,
                dataResourceUid: dataResourceUid,
                status: "UNPACKING"
        )
        upload.save(flush:true)

        try {
            def ant = new AntBuilder()   // create an antbuilder
            ant.unzip(src: uploadedFile.getAbsolutePath(),
                    dest: uploadedFile.getParentFile().getAbsolutePath(),
                    overwrite: "true")

            File newDir = new File("/data/image-service/uploads/" + upload.getId() + "/")
            uploadedFile.getParentFile().renameTo(newDir)
            upload.filePath = newDir.getAbsolutePath() + "/" +  uploadedFile.getName();
            upload.status = "UNZIPPED"
            upload.save(flush:true)

            def batchFiles = []

            //create BatchFileUpload jobs for each AVRO
            newDir.listFiles().each { File avroFile ->
                //read the file
                if (avroFile.getName().toLowerCase().endsWith(".avro")){
                    String md5HashBatchFile = generateMD5(avroFile)
                    // have we seen this file before
                    BatchFile batchFile = BatchFile.findByMd5Hash(md5HashBatchFile)
                    if (!batchFile) {
                        batchFile = new BatchFile()
                        def result = validateAvro(avroFile)
                        batchFile.filePath = avroFile.getAbsolutePath()
                        batchFile.recordCount = result.recordCount
                        batchFile.invalidRecords = result.invalidRecords
                        batchFile.status = batchFile.invalidRecords == 0 ? "VALID" : "INVALID"
                        batchFile.batchFileUpload = upload
                        batchFile.md5Hash = md5HashBatchFile
                        batchFiles << batchFile
                    } else {
                        log.info("Ignoring " + avroFile.getAbsolutePath() + ", upload previously registered")
                    }
                }
            }
            upload.batchFiles = batchFiles as Set
            upload.status = "WAITING_PROCESSING"

        } catch (Exception e){
            log.error("Problem unpacking zip " + e.getMessage(), e)
            upload.status = "CORRUPT_AVRO_FILES"
        }

        upload.save(flush:true)

        upload
    }

    Map validateAvro(File avroFile) {

        def inStream = new FileInputStream(avroFile)
        DataFileStream<GenericRecord> reader = new DataFileStream<>(inStream, new GenericDatumReader<GenericRecord>())
        long recordCount = 0
        long invalidRecords = 0

        // process record by record
        while (reader.hasNext() ) {
            GenericRecord currRecord = reader.next()
            // Here we can add in data manipulation like anonymization etc
            def multimediaRecords = currRecord.get("multimediaItems");
            multimediaRecords.each { GenericRecord record ->
                def identifier = record.get("identifier")
                if (!identifier){
                    invalidRecords++
                }
            }
            recordCount ++;
        }
        [recordCount: recordCount, invalidRecords: invalidRecords]
    }

    String generateMD5(final file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream(){is->
            byte[] buffer = new byte[8192]
            int read = 0
            while( (read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] md5sum = digest.digest()
        BigInteger bigInt = new BigInteger(1, md5sum)
        bigInt.toString(16).padLeft(32, '0')
    }

    def loadBatchFile(BatchFile batchFile) {

        log.info("Loading batch file: " + batchFile.filePath)

        def inStream = new FileInputStream(new File(batchFile.filePath))
        DataFileStream<GenericRecord> reader = new DataFileStream<>(inStream, new GenericDatumReader<GenericRecord>())

        def batch = []
        int count = 0

        // process record by record
        while (reader.hasNext()) {
//        while (reader.hasNext() && count < 500) {
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

        GParsPool.withPool(10) {
            batch.eachParallel { srcImage ->
                execute(srcImage, "test")
            }
        }

        log.info("Completed loading of batch file: " + batchFile.filePath)
    }

    Map execute(_imageSource,  String _userId) {

        def results = imageService.batchUploadFromUrl([_imageSource], _userId)
        def newImage = results[_imageSource.sourceUrl ?: _imageSource.imageUrl]
        if (newImage && newImage.success && newImage.image) {
            if (newImage.metadataUpdated) {
                imageService.scheduleImageIndex(newImage.image.id)
            }

            if (!newImage.alreadyStored){
                imageService.scheduleArtifactGeneration(newImage.image.id, _userId)
                //Only run for new images......
                imageService.scheduleImageMetadataPersist(
                        newImage.image.id,
                        newImage.image.imageIdentifier,
                        newImage.image.originalFilename,
                        MetaDataSourceType.SystemDefined,
                        _userId)
            }
        }
        newImage
    }

    def processNextInQueue(){

        log.info("Running batch file load job")

        // Read a job from BatchFile - oldest file that is set to "READY_TO_LOAD"
        BatchFile batchFile = BatchFile.findByStatus('VALID', [sort: 'dateCreated', order: 'asc'])

        if (batchFile){

            // Read AVRO, download for new, enqueue for metadata updates....
            batchFile.status = "LOADING"
            batchFile.lastUpdated = new Date()
            batchFile.save(flush:true)

            batchFile.batchFileUpload.status = "LOADING"
            batchFile.batchFileUpload.save(flush:true)

            //load batch file
            loadBatchFile(batchFile)

            Date now = new Date()
            batchFile.status = "COMPLETE"
            batchFile.lastUpdated = now
            batchFile.dateCompleted = now
            batchFile.save(flush:true)

            // if all loaded, mark as complete
            boolean allComplete = batchFile.batchFileUpload.batchFiles.every { it.status == "COMPLETE" }
            if (allComplete){
                batchFile.batchFileUpload.status =  "COMPLETE"
                batchFile.batchFileUpload.dateCompleted = now
            } else {
                batchFile.batchFileUpload.status =  "PARTIALLY_COMPLETE"
            }
            batchFile.batchFileUpload.status = allComplete ? "COMPLETE" : "PARTIALLY_COMPLETE"
        }
    }

    def reloadFile(fileId){
        BatchFile batchFile = BatchFile.findById(fileId)
        if (batchFile){
            batchFile.status = "VALID"
            batchFile.save(flush:true)
        }
    }

    def getUploads(){
        BatchFileUpload.findAll()
    }

    def getFiles(){
        BatchFile.findAll()
    }
}
