package au.org.ala.images

class BatchFileUpload {

    String dataResourceUid
    String md5Hash
    String filePath
    Date dateCreated
    Date lastUpdated
    String status
    Date dateCompleted

    static hasMany = [batchFiles:BatchFile]
    static constraints = {
        dateCompleted nullable: true
    }
    static mapping = {
        version false
    }
}
