package au.org.ala.images

import java.nio.file.Files
import java.nio.file.Paths

import static java.util.concurrent.TimeUnit.SECONDS
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND

class StagingController {

    def serve() {
        // TODO serve ranges here as well?
        String pathArg = params.path
        log.debug("Staging serve {}", pathArg)
        if (!pathArg) {
            return render(status: SC_NOT_FOUND)
        }
        def basePath = Paths.get(grailsApplication.config.getProperty('imageservice.imagestore.staging'))
        def path = basePath.resolve(pathArg)
        if (!path.toRealPath().startsWith(basePath)) {
            return render(status: SC_BAD_REQUEST)
        }
        if (!Files.exists(path) || Files.isDirectory(path)) {
            return render(status: SC_NOT_FOUND)
        }
        withCacheHeaders {
            // nginx style etags
            etag { "${Long.toHexString(Files.getLastModifiedTime(path).to(SECONDS))}-${Long.toHexString(Files.size(path))}" }
            lastModified { new Date(Files.getLastModifiedTime(path).toMillis()) }
            generate {
                response.contentLengthLong = Files.size(path)
                render(file: path.toFile(), contentType: Files.probeContentType(path)) // fileName: path.fileName,
            }
        }
    }
}
