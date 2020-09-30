package au.org.ala.images

import groovyx.gpars.GParsPool
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

class BatchService {

    private final static Map<String, BatchStatus> _Batches = [:]
    public static final String LOADING = "LOADING"
    public static final String NOT_LOADED = "NOT_LOADED"
    public static final String COMPLETE = "COMPLETE"
    public static final String PARTIALLY__COMPLETE = "PARTIALLY_COMPLETE"
    public static final String QUEUED = "QUEUED"
    public static final String WAITING__PROCESSING = "WAITING_PROCESSING"
    public static final String CORRUPT__AVRO__FILES = "CORRUPT_AVRO_FILES"
    public static final String INVALID = "INVALID"

    def imageService
    def settingService

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
        BatchFile.countByStatusNotEqual(COMPLETE)
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
                        batchFile.status = batchFile.recordCount > 0 ? QUEUED : INVALID
                        batchFile.batchFileUpload = upload
                        batchFile.md5Hash = md5HashBatchFile
                        batchFiles << batchFile
                    } else {
                        log.info("Ignoring " + avroFile.getAbsolutePath() + ", upload previously registered")
                    }
                }
            }
            upload.batchFiles = batchFiles as Set
            upload.status = WAITING__PROCESSING

        } catch (Exception e){
            log.error("Problem unpacking zip " + e.getMessage(), e)
            upload.status = CORRUPT__AVRO__FILES
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
                } else {
                    recordCount ++;
                }
            }
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

    boolean batchEnabled(){
        def setting = Setting.findByName("batch.service.processing.enabled")
        setting.refresh()
        Boolean.parseBoolean(setting ? setting.value : "true")
    }

    private boolean loadBatchFile(BatchFile batchFile) {

        log.info("Loading batch file: " + batchFile.filePath)

        def inStream = new FileInputStream(new File(batchFile.filePath))
        DataFileStream<GenericRecord> reader = new DataFileStream<>(inStream, new GenericDatumReader<GenericRecord>())

        def batch = []
        int count = 0

        def dataResourceUid = batchFile.batchFileUpload.dataResourceUid

        long newImageCount = 0
        long metadataUpdateCount = 0

        final int batchThreads = settingService.getBatchServiceThreads().intValue()
        final Long batchThrottleInMillis = settingService.getBatchServiceThrottleInMillis()
        final int batchReadSize = settingService.getBatchServiceReadSize().intValue()

        boolean completed = false

        while (reader.hasNext() && batchEnabled()){

            int batchSize = 0

            // read a batch of records
            while (reader.hasNext() && batchSize < batchReadSize ) {
                GenericRecord currRecord = reader.next()
                // Here we can add in data manipulation like anonymization etc
                def multimediaRecords = currRecord.get("multimediaItems");
                multimediaRecords.each { GenericRecord record ->
                    // check URL
                    batch << [
                        sourceUrl: record.get("identifier"),
                        originalFilename: record.get("identifier"),
                        dataResourceUid: dataResourceUid,
                        creator: record.get("creator"),
                        title: record.get("title"),
                        rightsHolder: record.get("rightsHolder"),
                        rights: record.get("rights"),
                        license: record.get("license"),
                        description: record.get("description")
                    ]
                    count ++;
                }
                batchSize ++
            }

            List<Map<String, Object>> results = Collections.synchronizedList(new ArrayList<Map<String, Object>>());

            GParsPool.withPool(batchThreads) {
                batch.eachParallel { image ->
                    def result = execute(image, "test")
                    if (result){
                        results.add(result)
                    }
                    if (batchThrottleInMillis > 0){
                        Thread.sleep(batchThrottleInMillis)
                    }
                }
            }

            //get counts
            results.each { result ->
                if (result) {
                    if (!result.alreadyStored) {
                        newImageCount ++
                    }
                    if (result.metadataUpdated) {
                        metadataUpdateCount ++
                    }
                }
            }

            batchFile.metadataUpdates = metadataUpdateCount
            batchFile.newImages = newImageCount
            batchFile.processedCount = count
            batchFile.save(flush:true)

            completed = !reader.hasNext()
        }

        if (completed) {
            log.info("Completed loading of batch file: " + batchFile.filePath)
        } else {
            log.info("Exiting the loading of batch file: " + batchFile.filePath + ", complete: " + completed)
        }
        completed
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
                imageService.scheduleImageIndex(newImage.image.id)
                //Only run for new images......
//                imageService.scheduleImageMetadataPersist(
//                        newImage.image.id,
//                        newImage.image.imageIdentifier,
//                        newImage.image.originalFilename,
//                        MetaDataSourceType.SystemDefined,
//                        _userId)
            }
        }
        newImage
    }

    def initialize(){
        BatchFile.findAllByStatus(LOADING).each {
            it.setStatus(NOT_LOADED)
            it.save(flush:true)
        }
    }

    def processNextInQueue(){

        if (!settingService.getBatchServiceProcessingEnabled()){
            log.info("Batch service is currently disabled")
            return
        }

        //is there an executing job ??
        def loading = BatchFile.findAllByStatus(LOADING)
        if (loading){
            return
        }

        log.info("Running batch file load job")

        // Read a job from BatchFile - oldest file that is set to "READY_TO_LOAD"
        BatchFile batchFile = BatchFile.findByStatus(QUEUED, [sort: 'dateCreated', order: 'asc'])

        if (batchFile){

            // Read AVRO, download for new, enqueue for metadata updates....
            batchFile.status = LOADING
            batchFile.lastUpdated = new Date()
            batchFile.processedCount = 0
            batchFile.newImages = 0
            batchFile.metadataUpdates = 0
            batchFile.dateCompleted = null
            batchFile.save(flush:true)

            batchFile.batchFileUpload.status = LOADING
            batchFile.batchFileUpload.save(flush:true)

            //load batch file
            def complete = loadBatchFile(batchFile)

            Date now = new Date()
            if (complete) {
                batchFile.status = COMPLETE
                batchFile.lastUpdated = now
                batchFile.dateCompleted = now
                batchFile.save(flush: true)
            } else {
                batchFile.status = NOT_LOADED
                batchFile.lastUpdated = now
                batchFile.save(flush: true)
            }

            // if all loaded, mark as complete
            boolean allComplete = batchFile.batchFileUpload.batchFiles.every { it.status == COMPLETE }
            if (allComplete){
                batchFile.batchFileUpload.status =  COMPLETE
                batchFile.batchFileUpload.dateCompleted = now
            } else {
                batchFile.batchFileUpload.status = PARTIALLY__COMPLETE
            }
            batchFile.batchFileUpload.status = allComplete ? COMPLETE : PARTIALLY__COMPLETE
        }
    }

    def reloadFile(fileId){
        BatchFile batchFile = BatchFile.findById(fileId)
        if (batchFile){
            batchFile.status = QUEUED
            batchFile.save(flush:true)
        }
    }

    def deleteFileFromQueue(fileId){
        BatchFile batchFile = BatchFile.findById(fileId)
        if (batchFile){
            batchFile.delete(flush:true)
        }
    }

    def clearFileQueue(){
        BatchFile.findAllByStatusNotEqual(LOADING).each {
            it.delete(flush:true)
        }
    }

    def clearUploads(){
        BatchFileUpload.findAll().each {
            if (it.batchFiles.every {it.status != LOADING }){
                it.delete(flush:true)
            }
        }
    }

    def getUploads(){
        BatchFileUpload.findAll()
    }

    def getFiles(){
        BatchFile.findAll()
    }
}
