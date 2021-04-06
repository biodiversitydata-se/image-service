package au.org.ala.images

class FailedUpload {

    String url
    Date dateCreated

    static constraints = {
    }

    static mapping = {
        url index: 'failed_upload_url_idx'
    }
}
