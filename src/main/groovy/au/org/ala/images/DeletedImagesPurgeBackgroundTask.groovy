package au.org.ala.images

import grails.gorm.transactions.Transactional

class DeletedImagesPurgeBackgroundTask extends BackgroundTask {

    ImageService imageService

    DeletedImagesPurgeBackgroundTask(ImageService imageService) {
        this.imageService = imageService
    }

    @Override
    @Transactional
    void execute() {
        def images = Image.findAllByDateDeletedIsNotNull()
        images.each {
            imageService.deleteImagePurge(it)
        }
    }
}
