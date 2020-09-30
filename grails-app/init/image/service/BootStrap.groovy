package image.service

class BootStrap {

    def messageSource

    def elasticSearchService

    def batchService

    def init = { servletContext ->

        messageSource.setBasenames(
                "file:///var/opt/atlas/i18n/image-service/messages",
                "file:///opt/atlas/i18n/image-service/messages",
                "WEB-INF/grails-app/i18n/messages",
                "classpath:messages"
        )

        elasticSearchService.initialize()
        batchService.initialize()

    }
    def destroy = {
    }
}
