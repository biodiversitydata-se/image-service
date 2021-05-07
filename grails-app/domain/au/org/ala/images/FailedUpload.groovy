package au.org.ala.images

class FailedUpload {

    String url
    Date dateCreated

    static constraints = {
        url nullable: false
    }

    static mapping = {
        version false
        id name: 'url', generator: 'assigned'
    }
}
