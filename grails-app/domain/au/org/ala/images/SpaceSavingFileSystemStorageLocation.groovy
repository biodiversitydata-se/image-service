package au.org.ala.images

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
        log.info("originalInputStream: $uuid -> $image.originalFilename")
        def inputStream= new URL(image.originalFilename).openStream()
        return range?.wrapInputStream(inputStream) ?: inputStream
    }

    @Override
    byte[] retrieve(String uuid) {
        log.info("retrieve: $uuid")
        IOUtils.toByteArray(originalInputStream(uuid, null))
    }

    @Override
    String toString() {
        "Space-saving-filesystem($id): $basePath"
    }
}
