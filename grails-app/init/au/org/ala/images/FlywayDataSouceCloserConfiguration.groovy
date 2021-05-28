package au.org.ala.images

import groovy.util.logging.Slf4j
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
@Slf4j
class FlywayDataSouceCloserConfiguration {

    @Autowired
    void setFlyway(Flyway flyway) {
        def flywayDs = flyway.dataSource
        if (flywayDs instanceof Closeable) {
            flywayDs.close()
        }
        log.info("Closed flyway datasource")
    }

}
