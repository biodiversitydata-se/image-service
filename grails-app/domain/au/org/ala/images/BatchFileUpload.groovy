package au.org.ala.images

class BatchFileUpload {

    String dataResourceUid
    String md5Hash
    String filePath
    Date dateCreated
    Date lastUpdated
    String status

    static hasMany = [batchFiles:BatchFile]
    static constraints = {}
    static mapping = {
        version false
    }
}
