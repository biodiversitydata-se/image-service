package au.org.ala.images.storage

import au.org.ala.images.AuditService
import au.org.ala.images.Image
import au.org.ala.images.ImageInfo
import au.org.ala.images.Range
import grails.gorm.transactions.Transactional
import groovy.transform.EqualsAndHashCode
import groovy.transform.NamedVariant
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.time.Duration

@EqualsAndHashCode(includes = ['basePath'])
@Slf4j
class SpaceSavingFileSystemStorageOperations extends FileSystemStorageOperations {

    String basePath

    @NamedVariant
    SpaceSavingFileSystemStorageOperations(String basePath) {
        super(basePath)
        log.debug("create: $basePath")
        this.basePath = basePath
    }

    @Override
    ImageInfo originalImageInfo(String uuid) {
        def image = Image.findByImageIdentifier(uuid, [cache: true])
        return new ImageInfo(
                exists: true,
                imageIdentifier: uuid,
                length: image.fileSize ?: -1,
                lastModified: null,
                contentType: image.mimeType ?: "image/jpeg",
                extension: image.extension ?: "jpg",
                etag: null,
                inputStreamSupplier: { Range range -> originalInputStream(uuid, range) }
        )
    }

    @Transactional(readOnly = true)
    @Override
    InputStream originalInputStream(String uuid, Range range) throws FileNotFoundException {
        def image = Image.findByImageIdentifier(uuid, [cache: true])
        log.debug("originalInputStream: $uuid -> $image.originalFilename")

        def client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        def request = new Request.Builder().url(image.originalFilename).build()

        def response
        try {
            response = client.newCall(request).execute()
        } catch (IOException e) {
            log.error("Failed to fetch original image, " +
                    "$image.dataResourceUid, $uuid, $image.originalFilename, error: $e.message, (original_failed)")
            throw e
        }
        if (!response.isSuccessful()) {
            def msg = "Failed to fetch original image, " +
                    "$image.dataResourceUid, $uuid, $image.originalFilename, http status: $response.code"
            log.error("$msg, (original_failed)")
            response.close()
            throw new IOException(msg)
        }

        def inputStream = response.body().byteStream()
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
        "SpaceSavingFilesystemStorageOperations: $basePath"
    }
}
