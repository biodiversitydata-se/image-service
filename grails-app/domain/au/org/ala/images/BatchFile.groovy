package au.org.ala.images

class BatchFile {

    String filePath
    Date dateCreated
    Date lastUpdated
    String status
    long recordCount
    long invalidRecords
    long newImages
    long metadataUpdates
    Date dateCompleted

    String md5Hash

    static belongsTo = [ batchFileUpload: BatchFileUpload ]

    static constraints = {
        newImages nullable: true
        metadataUpdates nullable: true
        dateCompleted nullable: true
    }

    static mapping = {
        version false
    }
}
