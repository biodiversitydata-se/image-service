package au.org.ala.images

class LogService {

    static transactional =  false

    def log(String message) {
        log.info message
    }

    def error(String message, Throwable error) {
        log.error message, error
    }

    def debug(String message) {
        log.debug message
    }

}
