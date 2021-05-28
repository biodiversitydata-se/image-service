import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanDefinition

// Place your Spring DSL code here
beans = {
    if (application.config.flyway.enabled) {

        flyway(Flyway) { bean ->
            bean.initMethod = 'migrate'
            dataSource = { HikariDataSource hds ->
                jdbcUrl = application.config.flyway.jdbcUrl ?: application.config.dataSource.url
                username = application.config.flyway.username ?: application.config.dataSource.username
                password = application.config.flyway.password ?: application.config.dataSource.password
                maximumPoolSize = application.config.flyway.maximumPoolSize ?: 2
            }

            baselineOnMigrate = application.config.getProperty('flyway.baselineOnMigrate', Boolean, true)
            def outOfOrderProp = application.config.getProperty('flyway.outOfOrder', Boolean, false)
            outOfOrder = outOfOrderProp
            placeholders = [
                    'imageRoot': application.config.getProperty('imageservice.imagestore.root'),
                    'exportRoot': application.config.getProperty('imageservice.imagestore.exportDir', '/data/image-service/exports'),
                    'baseUrl': application.config.getProperty('grails.serverURL')
            ]
            locations = application.config.flyway.locations ?: 'classpath:db/migration'
            if (application.config.flyway.baselineVersion) baselineVersionAsString = application.config.flyway.baselineVersion.toString()
        }

        BeanDefinition sessionFactoryBeanDef = getBeanDefinition('sessionFactory')

        if (sessionFactoryBeanDef) {
            addDependency(sessionFactoryBeanDef, 'flyway')
        }

        BeanDefinition hibernateDatastoreBeanDef = getBeanDefinition('hibernateDatastore')
        if (hibernateDatastoreBeanDef) {
            addDependency(hibernateDatastoreBeanDef, 'flyway')
        }

        BeanDefinition dataSourceBeanDef = getBeanDefinition('dataSource')
        if (dataSourceBeanDef) {
            addDependency(dataSourceBeanDef, 'flywayConfiguration')
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
