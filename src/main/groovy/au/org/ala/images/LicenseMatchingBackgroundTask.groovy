package au.org.ala.images

class LicenseMatchingBackgroundTask extends BackgroundTask {

    private ImageService _imageService
    private ElasticSearchService _elasticSearchService
    private Long _imageId

    LicenseMatchingBackgroundTask(Long imageId, ImageService imageService, ElasticSearchService elasticSearchService) {
        _imageId = imageId
        _imageService = imageService
        _elasticSearchService = elasticSearchService
    }

    @Override
    void execute() {
        def imageInstance = Image.get(_imageId)
        if (imageInstance) {
            def image = _imageService.updateLicence(imageInstance)
            image.save(flush:true)
        }
    }

    @Override
    String toString() {
        return "LicenseMatchingBackgroundTask," + _imageId
    }
}


class ScheduleLicenseReMatchAllBackgroundTask extends BackgroundTask {

    private ImageService _imageService

    ScheduleLicenseReMatchAllBackgroundTask(ImageService imageService) {
        _imageService = imageService
    }

    @Override
    void execute() {
        _imageService.updateLicences()
    }
}
