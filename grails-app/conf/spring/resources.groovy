import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.beans.factory.config.BeanDefinition

// Place your Spring DSL code here
beans = {
    if (application.config.flyway.enabled) {

        flywayDataSource(PGSimpleDataSource) { bean ->
            url = application.config.getProperty('flyway.jdbcUrl') ?: application.config.getProperty('dataSource.url')
            user = application.config.getProperty('flyway.username') ?: application.config.getProperty('dataSource.username')
            password = application.config.getProperty('flyway.password') ?: application.config.getProperty('dataSource.password')
        }

        flywayConfiguration(ClassicConfiguration) { bean ->
            dataSource = ref('flywayDataSource')
            table = application.config.flyway.table
            baselineOnMigrate = application.config.getProperty('flyway.baselineOnMigrate', Boolean, true)
            def outOfOrderProp = application.config.getProperty('flyway.outOfOrder', Boolean, false)
            outOfOrder = outOfOrderProp
            placeholders = [
                    'imageRoot': application.config.getProperty('imageservice.imagestore.root'),
                    'exportRoot': application.config.getProperty('imageservice.imagestore.exportDir', '/data/image-service/exports'),
                    'baseUrl': application.config.getProperty('grails.serverURL')
            ]
            locationsAsStrings = application.config.flyway.locations ?: 'classpath:db/migration'
            if (application.config.flyway.baselineVersion) baselineVersionAsString = application.config.flyway.baselineVersion.toString()
        }

        flyway(Flyway, ref('flywayConfiguration')) { bean ->
            bean.initMethod = 'migrate'
        }

        BeanDefinition sessionFactoryBeanDef = getBeanDefinition('sessionFactory')

        if (sessionFactoryBeanDef) {
            addDependency(sessionFactoryBeanDef, 'flyway')
        }

        BeanDefinition hibernateDatastoreBeanDef = getBeanDefinition('hibernateDatastore')
        if (hibernateDatastoreBeanDef) {
            addDependency(hibernateDatastoreBeanDef, 'flyway')
        }
    }
    else {
        log.info "Grails Flyway plugin has been disabled"
    }
}

def addDependency(BeanDefinition beanDef, String dependencyName) {
    def dependsOnList = [ dependencyName ] as Set
    if (beanDef.dependsOn?.length > 0) {
        dependsOnList.addAll(beanDef.dependsOn)
    }
    beanDef.dependsOn = dependsOnList as String[]
}
