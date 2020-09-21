package au.org.ala.images

import au.org.ala.images.metadata.MetadataExtractor
import au.org.ala.images.thumb.ThumbnailingResult
import au.org.ala.images.tiling.TileFormat
import grails.gorm.transactions.Transactional
import groovy.sql.Sql
import groovy.transform.Synchronized
import org.apache.commons.codec.binary.Base64
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.ImageMetadata
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.tiff.TiffField
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.apache.tika.mime.MimeType
import org.apache.tika.mime.MimeTypes
import org.grails.plugins.codecs.MD5CodecExtensionMethods
import org.grails.plugins.codecs.SHA1CodecExtensionMethods
import org.hibernate.FlushMode
import org.springframework.web.multipart.MultipartFile
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentLinkedQueue

class ImageService {

    def dataSource
    def imageStoreService
    def tagService
    def grailsApplication
    def logService
    def auditService
    def sessionFactory
    def imageService
    def elasticSearchService
    def settingService

    private static Queue<BackgroundTask> _backgroundQueue = new ConcurrentLinkedQueue<BackgroundTask>()
    private static Queue<BackgroundTask> _tilingQueue = new ConcurrentLinkedQueue<BackgroundTask>()

    private static int BACKGROUND_TASKS_BATCH_SIZE = 100

    Map imagePropertyMap = null

    ImageStoreResult storeImage(MultipartFile imageFile, String uploader, Map metadata = [:]) {

        if (imageFile) {
            // Store the image
            def originalFilename = imageFile.originalFilename
            def bytes = imageFile?.bytes
            def result = storeImageBytes(bytes, originalFilename, imageFile.size, imageFile.contentType, uploader, metadata)
            auditService.log(result.image,"Image stored from multipart file ${originalFilename}", uploader ?: "<unknown>")
            return result
        }
        return null
    }

    ImageStoreResult storeImageFromUrl(String imageUrl, String uploader, Map metadata = [:]) {
        if (imageUrl) {
            try {
                def image = Image.findByOriginalFilename(imageUrl)
                if (image && image.stored()) {
                    scheduleMetadataUpdate(image.imageIdentifier, metadata)
                    return new ImageStoreResult(image, true)
                }
                def url = new URL(imageUrl)
                def bytes = url.bytes

                def contentType = null

                //detect from dc:mimetype field
                if (metadata.mimeType){
                    try {
                        MimeType mimeType = new MimeTypes().forName(metadata.mimeType)
                        metadata.extension = mimeType.getExtension()
                        contentType = mimeType.toString()
                    } catch (Exception e){
                        log.debug("Un-parseable mime type supplied: " + metadata.mimeType)
                    }
                }

                //detect from dc:format field
                if(contentType == null && metadata.format){
                    try {
                        MimeType mimeType =  new MimeTypes().forName(metadata.format)
                        metadata.extension = mimeType.getExtension()
                        contentType = mimeType.toString()
                    } catch (Exception e){
                        log.debug("Un-parseable mime type supplied: " + metadata.format)
                    }
                }

                //detect from file
                if (contentType == null){
                    contentType = detectMimeTypeFromBytes(bytes, imageUrl)
                }

                def result = storeImageBytes(bytes, imageUrl, bytes.length, contentType, uploader, metadata)
                auditService.log(result.image, "Image downloaded from ${imageUrl}", uploader ?: "<unknown>")
                return result
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex)
            }
        }
        return null
    }

    Map batchUploadFromUrl(List<Map<String, String>> imageSources, String uploader) {
        def results = [:]
        Image.withNewTransaction {
            sessionFactory.currentSession.setFlushMode(FlushMode.MANUAL)
            try {
                imageSources.each { imageSource ->
                    def imageUrl = (imageSource.sourceUrl ?: imageSource.imageUrl) as String
                    if (imageUrl) {
                        Image image = Image.findByOriginalFilename(imageUrl)
                        if (!image) {
                            def result = [success: false, alreadyStored: false]
                            try {
                                def url = new URL(imageUrl)
                                def bytes = url.bytes
                                def contentType = detectMimeTypeFromBytes(bytes, imageUrl)
                                ImageStoreResult storeResult = storeImageBytes(bytes, imageUrl, bytes.length, contentType, uploader, imageSource)
                                result.imageId = storeResult.image.imageIdentifier
                                result.image = storeResult.image
                                result.success = true
                                result.alreadyStored = storeResult.alreadyStored
                                result.metadataUpdated = true
                            } catch (Exception ex) {
                                log.error("Problem storing image - " + ex.getMessage(), ex)
                                result.message = ex.message
                            }
                            results[imageUrl] = result
                        } else {

                            if (image.creator != imageSource.creator){
                                image.creator = imageSource.creator
                            }
                            if (image.title != imageSource.title){
                                image.title = imageSource.title
                            }
                            if (image.rightsHolder != imageSource.rightsHolder){
                                image.rightsHolder = imageSource.rightsHolder
                            }
                            if (image.rights != imageSource.rights){
                                image.rights = imageSource.rights
                            }
                            if (image.license != imageSource.license){
                                image.license = imageSource.license
                            }
                            if (image.description != imageSource.description){
                                image.description = imageSource.description
                            }

                            def metadataUpdated = false
                            if (image.isDirty()){
                                image.save()
                                metadataUpdated = true
                            }
                            //update metadata if required
                            results[imageUrl] = [success: true,
                                                 imageId: image.imageIdentifier,
                                                 image: image,
                                                 alreadyStored: true,
                                                 metadataUpdated: metadataUpdated
                            ]
                        }
                    }
                }
            } finally {
                sessionFactory.currentSession.flush()
                sessionFactory.currentSession.setFlushMode(FlushMode.AUTO)
            }
        }
        return results
    }

    int getImageTaskQueueLength() {
        return _backgroundQueue.size()
    }

    int getTilingTaskQueueLength() {
        return _tilingQueue.size()
    }

    def clearImageTaskQueue(){
        return _backgroundQueue.clear()
    }

    def clearTilingTaskQueueLength() {
        return _tilingQueue.clear()
    }

    @Synchronized
    def updateMetadata(String imageId, Map metadata) {

        def image = Image.findByImageIdentifier(imageId, [ cache: true])

        if (image) {
            boolean toSave = false
            def toUpdate = [:]
            metadata.each { kvp ->

                if (image.hasProperty(kvp.key) && kvp.value) {
                    if (!(kvp.key in ["dateTaken", "dateUploaded", "id"])) {
                        if (image[kvp.key] != kvp.value) {
                            toUpdate[kvp.key] = kvp.value
                            toSave = true
                        }
                    }
                }
            }
            if (toSave) {
                //this has been changed to use executeUpdate to avoid
                // StaleStateExceptions which are thrown due to
                // this method being called on the same image multiple times
                // by multiple threads.
                def query  = toUpdate.keySet().collect{"${it}=:${it}" }.join(", ")
                def fullQuery = "update Image i set " + query +
                        " where i.imageIdentifier =:imageIdentifier"
                toUpdate['imageIdentifier'] = imageId
                Image.executeUpdate(fullQuery, toUpdate)
            }
        }
    }

    @Synchronized
    ImageStoreResult storeImageBytes(byte[] bytes, String originalFilename, long filesize, String contentType,
                          String uploaderId, Map metadata = [:]) {

        def md5Hash = MD5CodecExtensionMethods.encodeAsMD5(bytes)

        //check for existing image using MD5 hash
        def image = Image.findByContentMD5Hash(md5Hash)
        def preExisting = false
        if (!image) {
            def sha1Hash = SHA1CodecExtensionMethods.encodeAsSHA1(bytes)

            Long defaultStorageLocationID = settingService.getStorageLocationDefault()

            StorageLocation sl = StorageLocation.get(defaultStorageLocationID)

            def imgDesc = imageStoreService.storeImage(bytes, sl, contentType)
            // Create the image record, and set the various attributes
            image = new Image(
                    imageIdentifier: imgDesc.imageIdentifier,
                    contentMD5Hash: md5Hash,
                    contentSHA1Hash: sha1Hash,
                    uploader: uploaderId,
                    storageLocation: sl
            )


            if(metadata.extension){
                image.extension = metadata.extension
            } else {
                // this is known to be problematic
                def extension =  FilenameUtils.getExtension(originalFilename) ?: 'jpg'
                if (extension && extension.contains("?")){
                    def cleanedExtension = extension.substring(0, extension.indexOf("?"))
                    if (cleanedExtension && cleanedExtension.length() > 0){
                        extension  = cleanedExtension
                    }
                }
                image.extension = extension
            }

            image.height = imgDesc.height
            image.width = imgDesc.width
            image.fileSize = filesize
            image.mimeType = contentType
            image.dateUploaded = new Date()
            image.originalFilename = originalFilename
            image.dateTaken = getImageTakenDate(bytes) ?: image.dateUploaded
        } else {
            image.dateDeleted = null //reset date deleted if image resubmitted...
            preExisting = true
        }

        if (preExisting){
            //stick it on a queue
            scheduleMetadataUpdate(image.imageIdentifier, metadata)
            scheduleLicenseUpdate(image.id)
        } else {
            //update metadata
            metadata.each { kvp ->
                def propertyName = hasImageCaseFriendlyProperty(image, kvp.key)
                if (propertyName && kvp.value){
                    if(!(propertyName in ["dateTaken", "dateUploaded", "id"])){
                        image[propertyName] = kvp.value
                    }
                }
            }

            //try to match licence
            updateLicence(image)

            try {
                image.save(flush: true, failOnError: true)
            } catch (Exception ex){
                log.error("Problem ${preExisting ? 'updating' : 'saving'} image ${originalFilename}  - " + ex.getMessage(), ex)
            }
        }

        new ImageStoreResult(image, preExisting)
    }


    def hasImageCaseFriendlyProperty(Image image, String propertyName){
        if (!imagePropertyMap) {
            def properties = image.getProperties().keySet()
            imagePropertyMap = [:]
            properties.each { imagePropertyMap.put(it.toLowerCase(), it) }
        }
        imagePropertyMap.get(propertyName.toLowerCase())
    }

    def schedulePostIngestTasks(Long imageId, String identifier, String fileName, String uploaderId){
        scheduleArtifactGeneration(imageId, uploaderId)
        scheduleImageIndex(imageId)
        scheduleImageMetadataPersist(imageId,identifier, fileName,  MetaDataSourceType.Embedded, uploaderId)
    }

    def scheduleNonImagePostIngestTasks(Long imageId){
        scheduleImageIndex(imageId)
    }

    Map getMetadataItemValuesForImages(List<Image> images, String key, MetaDataSourceType source = MetaDataSourceType.SystemDefined) {
        if (!images || !key) {
            return [:]
        }
        def results = ImageMetaDataItem.executeQuery("select md.value, md.image.id from ImageMetaDataItem md where md.image in (:images) and lower(name) = :key and source=:source", [images: images, key: key.toLowerCase(), source: source])
        def fr = [:]
        results.each {
            fr[it[1]] = it[0]
        }
        return fr
    }

    Map getAllUrls(String imageIdentifier) {
        return imageStoreService.getAllUrls(imageIdentifier)
    }

    String getImageUrl(String imageIdentifier) {
        return imageStoreService.getImageUrl(imageIdentifier)
    }

    String getImageThumbUrl(String imageIdentifier) {
        return imageStoreService.getImageThumbUrl(imageIdentifier)
    }

    String getImageThumbLargeUrl(String imageIdentifier) {
        return imageStoreService.getImageThumbLargeUrl(imageIdentifier)
    }

    String getImageSquareThumbUrl(String imageIdentifier, String backgroundColor = null) {
        return imageStoreService.getImageSquareThumbUrl(imageIdentifier, backgroundColor)
    }

    List<String> getAllThumbnailUrls(String imageIdentifier) {
        def results = []
        def image = Image.findByImageIdentifier(imageIdentifier, [ cache: true])
        if (image) {
            def thumbs = ImageThumbnail.findAllByImage(image)
            thumbs?.each { thumb ->
                results << imageStoreService.getThumbUrlByName(imageIdentifier, thumb.name)
            }
        }
        results
    }

    String getImageTilesUrlPattern(String imageIdentifier) {
        return imageStoreService.getImageTilesUrlPattern(imageIdentifier)
    }

    Image updateLicence(Image image){
        if (image.license){

            def license = License.findByAcronymOrNameOrUrlOrImageUrl(image.license,image.license,image.license,image.license)
            if (license){
                image.recognisedLicense = license
            } else {
                def licenceMapping = LicenseMapping.findByValue(image.license)
                if (licenceMapping){
                    image.recognisedLicense = licenceMapping.license
                } else {
                    image.recognisedLicense = null
                }
            }
        } else {
            image.recognisedLicense = null
        }
        image
    }

    //this is slow on large tables
    def updateLicences(){
        log.info("Updating license mapping for all images")
        def licenseMapping = LicenseMapping.findAll()
        licenseMapping.each {
            log.info("Updating license mapping for string matching: " + it.value)
            Image.executeUpdate("Update Image i set i.recognisedLicense = :recognisedLicense " +
                    " where " +
                    " i.license = :license" +
                    "", [recognisedLicense: it.license, license: it.value])
        }

        def licenses = License.findAll()
        log.info("Updating licenses  for all images - using acronym, name and url")
        licenses.each  { license ->
            [license.url, license.name, license.acronym].each { licenceValue ->
                Image.executeUpdate("Update Image i set i.recognisedLicense = :recognisedLicense " +
                        " where " +
                        " i.license = :license" +
                        "", [recognisedLicense: license, license: licenceValue])
            }
        }
        log.info("Licence refresh complete")
    }

    private static Date getImageTakenDate(byte[] bytes) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(bytes)
            if (metadata && metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = metadata

                def date = getImageTagValue(jpegMetadata,TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL)
                if (date) {
                    def sdf = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss")
                    return sdf.parse(date.toString())
                }
            }
        } catch (Exception ex) {
            return null
        }
    }

    private static Object getImageTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field) {
            return field.value
        }
    }

    static Map<String, Object> getImageMetadataFromBytes(byte[] bytes, String filename) {
        def extractor = new MetadataExtractor()
        return extractor.readMetadata(bytes, filename)
    }

    def scheduleArtifactGeneration(long imageId, String userId) {
        scheduleBackgroundTask(new ImageBackgroundTask(imageId, this, ImageTaskType.Thumbnail, userId))
        _tilingQueue.add(new ImageBackgroundTask(imageId, this, ImageTaskType.TMSTile, userId))
    }

    def scheduleThumbnailGeneration(long imageId, String userId) {
        scheduleBackgroundTask(new ImageBackgroundTask(imageId, this, ImageTaskType.Thumbnail, userId))
    }

    def scheduleTileGeneration(long imageId, String userId) {
        _tilingQueue.add(new ImageBackgroundTask(imageId, this, ImageTaskType.TMSTile, userId))
    }

    def scheduleKeywordRebuild(long imageId, String userId) {
        scheduleBackgroundTask(new ImageBackgroundTask(imageId, this, ImageTaskType.KeywordRebuild, userId))
    }

    def scheduleImageDeletion(long imageId, String userId) {
        scheduleBackgroundTask(new ImageBackgroundTask(imageId, this, ImageTaskType.Delete, userId))
    }

    def scheduleLicenseUpdate(long imageId) {
        scheduleBackgroundTask(new LicenseMatchingBackgroundTask(imageId, imageService, elasticSearchService))
    }

    def scheduleMetadataUpdate(String imageIdentifier, Map metadata) {
        scheduleBackgroundTask(new ImageMetadataUpdateBackgroundTask(imageIdentifier, metadata, imageService))
    }

    def scheduleImageIndex(long imageId) {
        scheduleBackgroundTask(new IndexImageBackgroundTask(imageId, elasticSearchService))
    }

    def scheduleImageMetadataPersist(long imageId, String imageIdentifier, String fileName,  MetaDataSourceType metaDataSourceType, String uploaderId){
        scheduleBackgroundTask(new ImageMetadataPersistBackgroundTask(imageId, imageIdentifier, fileName, metaDataSourceType, uploaderId, imageService, imageStoreService))
    }

    def scheduleBackgroundTask(BackgroundTask task) {
        _backgroundQueue.add(task)
    }

    def schedulePollInbox(String userId) {
        def task = new PollInboxBackgroundTask(this, userId)
        scheduleBackgroundTask(task)
        return task.batchId
    }

    void processBackgroundTasks() {
        int taskCount = 0
        BackgroundTask task = null

        while (taskCount < BACKGROUND_TASKS_BATCH_SIZE && (task = _backgroundQueue.poll()) != null) {
            if (task) {
                try {
                    task.execute()
                } catch (Exception e){
                    log.error("Error executing task: " + task)
                    log.error("Error executing task: " + e.getMessage(), e)
                }
                taskCount++
            }
        }
    }

    void processTileBackgroundTasks() {
        int taskCount = 0
        BackgroundTask task = null
        while (taskCount < BACKGROUND_TASKS_BATCH_SIZE && (task = _tilingQueue.poll()) != null) {
            if (task) {
                task.execute()
                taskCount++
            }
        }
    }

    boolean isImageType(Image image) {
        return image.mimeType?.toLowerCase()?.startsWith("image/")
    }

    boolean isAudioType(Image image) {
        return image.mimeType?.toLowerCase()?.startsWith("audio/")
    }

    List<ThumbnailingResult> generateImageThumbnails(Image image) {
        List<ThumbnailingResult> results
        if (isAudioType(image)) {
            results = imageStoreService.generateAudioThumbnails(image)
        } else if (isImageType(image)) {
            results = imageStoreService.generateImageThumbnails(image)
        } else {
            results = imageStoreService.generateDocumentThumbnails(image)
        }

        // These are deprecated, but we'll update them anyway...
        if (results) {
            def defThumb = results.find { it.thumbnailName.equalsIgnoreCase("thumbnail")}
            image.thumbWidth = defThumb?.width ?: 0
            image.thumbHeight = defThumb?.height ?: 0
            image.squareThumbSize = results.find({ it.thumbnailName.equalsIgnoreCase("thumbnail_square")})?.width ?: 0
        }
        results?.each { th ->
            def imageThumb = ImageThumbnail.findByImageAndName(image, th.thumbnailName)
            if (imageThumb) {
                imageThumb.height = th.height
                imageThumb.width = th.width
                imageThumb.isSquare = th.square
            } else {
                imageThumb = new ImageThumbnail(image: image, name: th.thumbnailName, height: th.height, width: th.width, isSquare: th.square)
                imageThumb.save(flush:true)
            }
        }
    }

    void generateTMSTiles(String imageIdentifier) {
        imageStoreService.generateTMSTiles(imageIdentifier)
    }

    def deleteImage(Image image, String userId) {

        if (image) {

            deleteRelatedArtefacts(image)

            // Delete from the index...
            elasticSearchService.deleteImage(image)

            //soft deletes
            image.dateDeleted = new Date()
            image.save(flush: true, failonerror: true)

            auditService.log(image?.imageIdentifier, "Image deleted", userId)

            return true
        }

        return false
    }

    private def deleteRelatedArtefacts(Image image){

        // need to delete it from user selections
        def selected = SelectedImage.findAllByImage(image)
        selected.each { selectedImage ->
            selectedImage.delete()
        }

        // Need to delete tags
        def tags = ImageTag.findAllByImage(image)
        tags.each { tag ->
            tag.delete()
        }

        // Delete keywords
        def keywords = ImageKeyword.findAllByImage(image)
        keywords.each { keyword ->
            keyword.delete()
        }

        // If this image is a subimage, also need to delete any subimage rectangle records
        def subimagesRef = Subimage.findAllBySubimage(image)
        subimagesRef.each { subimage ->
            subimage.delete()
        }

        // This image may also be a parent image
        def subimages = Subimage.findAllByParentImage(image)
        subimages.each { subimage ->
            // need to detach this image from the child images, but we do not actually delete the sub images. They
            // will live on as root images in their own right
            subimage.subimage.parent = null
            subimage.delete()
        }

        // thumbnail records...
        def thumbs = ImageThumbnail.findAllByImage(image)
        thumbs.each { thumb ->
            thumb.delete()
        }
    }

    def deleteImagePurge(Image image) {
        if (image && image.dateDeleted) {
            deleteRelatedArtefacts(image)
            image.deleteStored()
            //hard delete
            image.delete(flush:true)
            return true
        }
        return false
    }

    List<File> listStagedImages() {
        def files = []
        def inboxLocation = grailsApplication.config.imageservice.imagestore.inbox as String
        def inboxDirectory = new File(inboxLocation)
        inboxDirectory.eachFile { File file ->
            files << file
        }
        return files
    }

    Image importFileFromInbox(File file, String batchId, String userId) {

        if (!file || !file.exists()) {
            throw new RuntimeException("Could not read file ${file?.absolutePath} - Does not exist")
        }

        Image image = null

        def fieldDefinitions = ImportFieldDefinition.list()

        Image.withNewTransaction {

            // Create the image domain object
            def bytes = file.getBytes()
            def mimeType = detectMimeTypeFromBytes(bytes, file.name)
            image = storeImageBytes(bytes, file.name, file.length(),mimeType, userId).image

            auditService.log(image, "Imported from ${file.absolutePath}", userId)

            if (image && batchId) {
                setMetaDataItem(image, MetaDataSourceType.SystemDefined,  "importBatchId", batchId)
            }

            // Is there any extra data to be applied to this image?
            if (fieldDefinitions) {
                fieldDefinitions.each { fieldDef ->
                    setMetaDataItem(image, MetaDataSourceType.SystemDefined, fieldDef.fieldName, ImportFieldValueExtractor.extractValue(fieldDef, file))
                }
            }
            generateImageThumbnails(image)

            image.save(flush: true, failOnError: true)
        }

        // If we get here, and the image is not null, it means it has been committed to the database and we can remove the file from the inbox
        if (image) {
            if (!FileUtils.deleteQuietly(file)) {
                file.deleteOnExit()
            }
            // schedule an index
            scheduleImageIndex(image.id)
            // also we should do the thumb generation (we'll defer tiles until after the load, as it will slow everything down)
            scheduleTileGeneration(image.id, userId)
        }
        return image
    }

    def pollInbox(String batchId, String userId) {
        def inboxLocation = grailsApplication.config.imageservice.imagestore.inbox as String
        def inboxDirectory = new File(inboxLocation)

        inboxDirectory.eachFile { File file ->
            _backgroundQueue.add(new ImportFileBackgroundTask(file, this, batchId, userId))
        }
    }

    private static String sanitizeString(Object value) {
        if (value) {
            value = value.toString()
        } else {
            return ""
        }

        def bytes = value?.getBytes("utf8")

        def hasZeros = bytes.contains(0)
        if (hasZeros) {
            return Base64.encodeBase64String(bytes)
        } else {
            return StringUtils.trimToEmpty(value)
        }
    }

    def updateImageMetadata(Image image, Map metadata){
        scheduleMetadataUpdate(image.imageIdentifier, metadata)
//        imageService.updateMetadata(image.imageIdentifier, metadata)
    }

    def setMetaDataItem(Image image, MetaDataSourceType source, String key, String value, String userId = "<unknown") {

        value = sanitizeString(value)
        if (image && image.id && StringUtils.isNotEmpty(key?.trim())) {

            if (value.length() > 8000) {
                auditService.log(image, "Cannot set metdata item '${key}' because value is too big! First 25 bytes=${value.take(25)}", userId)
                return false
            }

            // See if we already have an existing item...
            def existing = ImageMetaDataItem.findByImageAndNameAndSource(image, key, source)
            if (existing) {
                existing.value = value
                existing.save()
            } else {
                if (value){
                    image.addToMetadata(new ImageMetaDataItem(image: image, name: key, value: value, source: source)).save()
                }
            }
            return true
        } else {
            logService.debug("Not Setting metadata item! Image ${image?.id} key: ${key} value: ${value}")
        }

        return false
    }

    def setMetadataItemsByImageId(Long imageId, Map<String, String> metadata, MetaDataSourceType source, String userId) {
        def image = Image.get(imageId)
        if (image) {
            return setMetadataItems(image, metadata, source, userId)
        }
        return false
    }

    def setMetadataItems(Image image, Map<String, Object> metadata, MetaDataSourceType source, String userId) {
        if (!userId) {
            userId = "<unknown>"
        }
        metadata.each { kvp ->
            def value = sanitizeString(kvp.value?.toString())
            def key = kvp.key
            if (image && StringUtils.isNotEmpty(key?.trim())) {

                if (value.length() > 8000) {
                    auditService.log(image, "Cannot set metdata item '${key}' because value is too big! First 25 bytes=${value.take(25)}", userId)
                    return false
                }

                // See if we already have an existing item...
                def existing = ImageMetaDataItem.findByImageAndNameAndSource(image, key, source)
                if (existing) {
                    existing.value = value
                } else {
//                    log.info("Storing metadata: ${image.title}, name: ${key}, value: ${value}, source: ${source}")
                    if(key && value) {
                        def md = new ImageMetaDataItem(image: image, name: key, value: value, source: source)
                        md.save(failOnError: true)
                        image.addToMetadata(md)
                    }
                }

                auditService.log(image, "Metadata item ${key} set to '${value?.take(25)}' (truncated) (${source})", userId)
            } else {
                logService.debug("Not Setting metadata item! Image ${image?.id} key: ${key} value: ${value}")
            }
        }
        image.save()
        return true
    }

    def removeMetaDataItem(Image image, String key, MetaDataSourceType source, String userId="<unknown>") {
        def count = 0
        def items = ImageMetaDataItem.findAllByImageAndNameAndSource(image, key, source)
        if (items) {
            items.each { md ->
                count++
                md.delete()
            }
            scheduleImageIndex(image.id)
        }
        auditService.log(image, "Delete metadata item ${key} (${count} items)", userId)
        return count > 0
    }

    static String detectMimeTypeFromBytes(byte[] bytes, String filename) {
        return new MetadataExtractor().detectContentType(bytes, filename);
    }

    Image createSubimage(Image parentImage, int x, int y, int width, int height, String userId, Map metadata = [:]) {

        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }

        def results = imageStoreService.retrieveImageRectangle(parentImage, x, y, width, height)
        if (results.bytes) {
            int subimageIndex = Subimage.countByParentImage(parentImage) + 1
            def filename = "${parentImage.originalFilename}_subimage_${subimageIndex}"
            def subimage = storeImageBytes(results.bytes,filename, results.bytes.length, results.contentType, userId, metadata).image

            def subimageRect = new Subimage(parentImage: parentImage, subimage: subimage, x: x, y: y, height: height, width: width)
            subimage.parent = parentImage
            subimageRect.save(flush:true)

            auditService.log(parentImage, "Subimage created ${subimage.imageIdentifier}", userId)
            auditService.log(subimage, "Subimage created from parent image ${parentImage.imageIdentifier}", userId)

            scheduleArtifactGeneration(subimage.id, userId)
            scheduleImageIndex(subimage.id)

            return subimage
        }
    }

    Map getImageInfoMap(Image image) {
        def map = [
                imageId: image.imageIdentifier,
                height: image.height,
                width: image.width,
                tileZoomLevels: image.zoomLevels,
                thumbHeight: image.thumbHeight,
                thumbWidth: image.thumbWidth,
                filesize: image.fileSize,
                mimetype: image.mimeType,
                creator: image.creator,
                title: image.title,
                description: image.description,
                rights: image.rights,
                rightsHolder: image.rightsHolder,
                license: image.license
        ]
        def urls = getAllUrls(image.imageIdentifier)
        urls.each { kvp ->
            map[kvp.key] = kvp.value
        }
        return map
    }

    def createNextTileJob() {
        def task = _tilingQueue.poll() as ImageBackgroundTask
        if (task == null) {
            return [success:false, message:"No tiling jobs available at this time."]
        } else {
            if (task) {
                def image = Image.get(task.imageId)
                // Create a new pending job
                def ticket = UUID.randomUUID().toString()
                def job = new OutsourcedJob(image: image, taskType: ImageTaskType.TMSTile, expectedDurationInMinutes: 15, ticket: ticket)
                job.save()
                return [success: true, imageId: image.imageIdentifier, jobTicket: ticket, tileFormat: TileFormat.JPEG]
            } else {
                return [success:false, message: "Internal error!"]
            }
        }
    }

    def resetImageLinearScale(Image image) {
        image.mmPerPixel = null;
        image.calibratedByUser = null
        image.save(flush:true)
        scheduleImageIndex(image.id)
    }

    def calibrateImageScale(Image image, double pixelLength, double actualLength, String units, String userId) {

        double scale = 1.0
        switch (units) {
            case "inches":
                scale = 25.4
                break;
            case "metres":
                scale = 1000
                break;
            case "feet":
                scale = 304.8
                break;
            default: // unrecognized units, or mm
                break;
        }

        def mmPerPixel = (actualLength * scale) / pixelLength

        image.mmPerPixel = mmPerPixel
        image.calibratedByUser = userId
        image.save(flush:true)
        scheduleImageIndex(image.id)

        return mmPerPixel
    }

    def setHarvestable(Image image, Boolean harvestable, String userId) {
        image.setHarvestable(harvestable)
        image.save()
        scheduleImageIndex(image.id)
        auditService.log(image, "Harvestable set to ${harvestable}", userId)
    }

    /**
     *
     * @param maxRows
     * @param offset
     * @return a map with two keys - 'data' a list of maps containing the harvestable data, and 'columnHeadings', a list of strings with the distinct list of columns
     */
    def getHarvestTabularData(int maxRows = -1, int offset = 0) {

        def params = [:]
        if (maxRows > 0) {
            params.max = maxRows
        }

        if (offset > 0) {
            params.offset = offset
        }

        def images = Image.findAllByHarvestable(true)
        if (!images) {
            return [columnHeaders: ["imageUrl", "occurrenceId"], data: []]
        }

        def c = ImageMetaDataItem.createCriteria()
        // retrieve just the relevant metadata rows
        def metaDataRows = c.list {
            inList("image", images)
            or {
                eq("source", MetaDataSourceType.SystemDefined)
                eq("source", MetaDataSourceType.UserDefined)
            }
        }

        def metaDataMappedbyImage = metaDataRows.groupBy {
            it.image.id
        }

        def columnHeaders = ['imageUrl', 'occurrenceId']

        def tabularData = []

        images.each { image ->
            def map =  [occurrenceId: image.imageIdentifier, 'imageUrl': imageService.getImageUrl(image.imageIdentifier)]
            def imageMetadata = metaDataMappedbyImage[image.id]
            imageMetadata.each { md ->
                if (md.value) {
                    map[md.name] = md.value
                    if (!columnHeaders.contains(md.name)) {
                        columnHeaders << md.name
                    }
                }
            }
            tabularData << map
        }

        return [data: tabularData, columnHeaders: columnHeaders]
    }

    def deleteIndex() {
        elasticSearchService.reinitialiseIndex()
    }

    /**
     * Retrieve image via numeric ID or guid.
     * @param params
     * @return
     */
    def getImageFromParams(params) {
        def image = Image.findById(params.int("id"))
        if (!image) {
            String guid = params.id // maybe the id is a guid?
            if (!guid) {
                guid = params.imageId
            }

            image = Image.findByImageIdentifier(guid, [ cache: true])
        }
        return image
    }

    def UUID_PATTERN = ~/\b[0-9a-f]{8}\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\b[0-9a-f]{12}\b/


    def getImageGUIDFromParams(params) {

        if(params.id){
            //if it a GUID, avoid database trip if possible....
            if (UUID_PATTERN.matcher(params.id).matches()){
                return params.id
            }
            if(params.id ){
                def image = Image.findById(params.int("id"))
                if(image) {
                    return image.imageIdentifier
                }
            }
        } else if(params.imageId){
            //if it a GUID, avoid database trip if possible....
            if (UUID_PATTERN.matcher(params.imageId).matches()){
                return params.imageId
            }
            if(params.id ){
                def image = Image.findById(params.int("imageId"))
                if (image) {
                    return image.imageIdentifier
                }
            }
        }
        return null
    }

    /**
     * Export CSV. This uses a stored procedure that needs to be installed as part of the
     * service installation.
     *
     * @param outputStream
     * @return
     */
    def exportCSV(outputStream){
        exportCSVToFile().withInputStream { stream ->
            outputStream << stream
        }
    }

    /**
     * Export CSV. This uses a stored procedure that needs to be installed as part of the
     * service installation.
     *
     * @param outputStream
     * @return
     */
    File exportCSVToFile(){
        FileUtils.forceMkdir(new File(grailsApplication.config.imageservice.exportDir))
        def exportFile = grailsApplication.config.imageservice.exportDir + "/images.csv"
        new Sql(dataSource).call("""{ call export_images() }""")
        new File(exportFile)
    }

    /**
     * Export CSV. This uses a stored procedure that needs to be installed as part of the
     * service installation.
     *
     * @param outputStream
     * @return
     */
    def exportMappingCSV(outputStream){
        exportMappingCSVToFile().withInputStream { stream ->
            outputStream << stream
        }
    }

    /**
     * Export CSV. This uses a stored procedure that needs to be installed as part of the
     * service installation.
     *
     * @param outputStream
     * @return
     */
    def exportDatasetMappingCSV(String datasetID, outputStream){
        exportMappingForDatasetCSVToFile(datasetID).withInputStream { stream ->
            outputStream << stream
        }
    }

    File exportMappingForDatasetCSVToFile(String datasetID){
        FileUtils.forceMkdir(new File(grailsApplication.config.imageservice.exportDir))
        def exportFile = grailsApplication.config.imageservice.exportDir + "/images-mapping-${datasetID}.csv"
        new Sql(dataSource).call("""{ call export_dataset_mapping(?) }""", [datasetID])
        new File(exportFile)
    }

    File exportMappingCSVToFile(){
        FileUtils.forceMkdir(new File(grailsApplication.config.imageservice.exportDir))
        def exportFile = grailsApplication.config.imageservice.exportDir + "/images-mapping.csv"
        new Sql(dataSource).call("""{ call export_mapping() }""")
        new File(exportFile)
    }

    /**
     * Export CSV. This uses a stored procedure that needs to be installed as part of the
     * service installation.
     *
     * @param outputStream
     * @return
     */
    File exportIndexToFile(){
        FileUtils.forceMkdir(new File(grailsApplication.config.imageservice.exportDir))
        def exportFile = grailsApplication.config.imageservice.exportDir + "/images-index.csv"
        new Sql(dataSource).call("""{ call export_index() }""")
        new File(exportFile)
    }

    @Transactional
    void migrateImage(long imageId, long destinationStorageLocationId, String userId, boolean deleteSource) {
        log.debug("Migrating image id {} to storage location {}", imageId, destinationStorageLocationId)
        def image = Image.findById(imageId)
        def sl = StorageLocation.findById(destinationStorageLocationId)
        StorageLocation source = image.storageLocation
        String imageIdentifier = image.imageIdentifier
        if (!image.dateDeleted) {
            log.debug("Beginning migration for image {}...", imageId)
            if (source == sl) {
                log.warn("Attempt to migrate image {} to storage location {} aborted because it's already there.", imageId, sl)
                return
            }
            imageStoreService.migrateImage(image, sl)
            log.debug("Image {} migration ended", imageId)
            image.storageLocation = sl
            image.save()
            auditService.log(image.imageIdentifier, "Migrated to $sl", userId)
        }
        if (source && imageIdentifier && deleteSource) {
            source.deleteStored(imageIdentifier)
        }
    }
}
