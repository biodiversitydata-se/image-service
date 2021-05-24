package au.org.ala.images

class ScheduleLicenseReMatchAllBackgroundTask extends BackgroundTask {

    private ImageService _imageService
    boolean requiresSession = true

    ScheduleLicenseReMatchAllBackgroundTask(ImageService imageService) {
        _imageService = imageService
    }

    @Override
    void execute() {
        _imageService.updateLicences()
    }
}
