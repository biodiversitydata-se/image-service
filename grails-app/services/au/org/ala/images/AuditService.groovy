package au.org.ala.images

import javax.annotation.PostConstruct

class AuditService {

    static AuditService AUDIT_SERVICE

    @PostConstruct
    void init() {
        AUDIT_SERVICE = this
    }

    def log(Image image, String message, String userId) {
        def auditMessage = new AuditMessage(imageIdentifier: image.imageIdentifier, message: message, userId: userId)
        auditMessage.save()
    }

    def log(String imageIdentifier, String message, String userId) {
        def auditMessage = new AuditMessage(imageIdentifier: imageIdentifier, message: message, userId: userId)
        auditMessage.save()
    }

    // allow domain objects to submit audit logs without forcing them to opt in to autowiring
    static void submitLog(String imageIdentifier, String message, String userId) {
        AUDIT_SERVICE?.log(imageIdentifier, message, userId)
    }

    def getMessagesForImage(String imageIdentifier) {
        return AuditMessage.findAllByImageIdentifier(imageIdentifier, [sort:'dateCreated', order:'asc'])
    }

}
