package au.org.ala.images

class ProcessBackgroundTilingTasksJob {

    def imageService
    def logService
    def settingService

    static concurrent = true

    static triggers = {
        simple repeatInterval: 10000l
    }

    def execute() {
        try {
            if (settingService.tilingEnabled) {
                imageService.processTileBackgroundTasks()
            }
        } catch (Exception ex) {
            logService.error("Exception thrown in tiling job handler", ex)
            throw ex
        }
    }

}
