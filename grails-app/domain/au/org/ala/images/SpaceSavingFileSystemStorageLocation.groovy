package au.org.ala.images

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

class SpaceSavingFileSystemStorageLocation extends FileSystemStorageLocation {

    static constraints = {
    }

    static mapping = {
        cache true
    }

    @Override
    InputStream originalInputStream(String uuid, Range range) throws FileNotFoundException {
        def image = Image.findByImageIdentifier(uuid, [cache: true])
        def inputStream= new URL(image.originalFilename).openStream()
        log.debug("originalInputStream: $uuid -> $image.originalFilename")
        range?.wrapInputStream(inputStream) ?: inputStream
    }

    @Override
    void store(String uuid, InputStream inputStream, String contentType = 'image/jpeg', String contentDisposition = null, Long length = null) {
        log.debug("store: $uuid")
    }

    @Override
    byte[] retrieve(String uuid) {
        log.debug("retrieve: $uuid")
        IOUtils.toByteArray(originalInputStream(uuid, null))
    }

    @Override
    boolean stored(String uuid) {
        log.debug("stored: $uuid")
        new File(createBasePathFromUUID(uuid))?.exists() ?: false
    }

    @Override
    long originalStoredLength(String uuid) throws FileNotFoundException {
        log.debug("originalStoredLength: $uuid")
        0
    }

    @Override
    long consumedSpace(String uuid) {
        log.debug("consumedSpace: $uuid")
        def basePath = new File(createBasePathFromUUID(uuid))
        if (basePath && basePath.exists()) {
            return FileUtils.sizeOfDirectory(basePath)
        }
        0
    }

    @Override
    boolean deleteStored(String uuid) {
        log.debug("deleteStored: $uuid")
        if (uuid) {
            File f = new File(createBasePathFromUUID(uuid))
            if (f && f.exists()) {
                FileUtils.deleteQuietly(f)
                AuditService.submitLog(uuid, "Image deleted from store", "N/A")
                return true
            }
        }
        false
    }

    @Override
    String toString() {
        "Space-saving-filesystem($id): $basePath"
    }
}
