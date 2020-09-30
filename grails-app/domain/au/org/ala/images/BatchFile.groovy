package au.org.ala.images

class BatchFile {

    String filePath
    Date dateCreated
    Date lastUpdated
    String status
    Long recordCount
    Long invalidRecords
    Long newImages
    Long metadataUpdates
    Long processedCount
    Date dateCompleted

    String md5Hash

    static belongsTo = [ batchFileUpload: BatchFileUpload ]

    static constraints = {
        newImages nullable: true
        metadataUpdates nullable: true
        processedCount nullable: true
        dateCompleted nullable: true
    }

    static mapping = {
        version false
    }
}
