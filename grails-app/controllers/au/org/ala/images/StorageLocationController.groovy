package au.org.ala.images

import au.org.ala.web.AlaSecured
import au.org.ala.web.CASRoles

class StorageLocationController {

    def storageLocationService
    def settingService

    @AlaSecured(value = [CASRoles.ROLE_ADMIN, "ROLE_IMAGE_ADMIN"], anyRole = true, statusCode = 403)
    def create() {
        def json = request.getJSON()
        try {
            def sl = storageLocationService.createStorageLocation(json)
            if (!sl) {
                render(status: 400)
                return
            }
        } catch (e) {
            log.error("Error saving storage location", e)
            render(status: 400)
            return
        }
        render(status: 204)
    }

    def listFragment() {
        def storageLocationList = StorageLocation.list()
        def imageCounts = storageLocationList.collectEntries {
            [(it.id): Image.countByStorageLocationAndDateDeletedIsNull(it) ]
        }
        [storageLocationList:  storageLocationList, imageCounts: imageCounts, defaultId: settingService.getStorageLocationDefault()]
    }

    @AlaSecured(value = [CASRoles.ROLE_ADMIN, "ROLE_IMAGE_ADMIN"], anyRole = true, statusCode = 403)
    def setDefault() {
        def id = params.long('id')
        StorageLocation sl = StorageLocation.get(id)
        if (sl) {
            settingService.setStorageLocationDefault(id)
            render(status: 204)
        } else {
            render(status: 404)
        }
    }

    @AlaSecured(value = [CASRoles.ROLE_ADMIN, "ROLE_IMAGE_ADMIN"], anyRole = true, statusCode = 403)
    def migrate() {
        Long source = params.long('src')
        Long destination = params.long('dst')

        if (source == destination) {
            render(status: 400, text: "Source and destination are the same")
        }

        log.error("Migrate from source {} to destination {}", source, destination)
        if (source && destination) {
            storageLocationService.migrate(source, destination, request.remoteUser)
            render(status: 202)
        } else {
            render(status: 404)
        }
    }

}
