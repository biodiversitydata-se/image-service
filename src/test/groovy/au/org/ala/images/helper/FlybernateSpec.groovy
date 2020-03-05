package au.org.ala.images.helper

import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.opentable.db.postgres.junit.SingleInstancePostgresRule
import grails.config.Config
import grails.persistence.Entity
import groovy.transform.CompileStatic
import org.flywaydb.core.Flyway
import org.grails.config.PropertySourcesConfig
import org.grails.orm.hibernate.HibernateDatastore
import org.grails.orm.hibernate.cfg.Settings
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.junit.ClassRule
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.env.PropertySourcesLoader
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertyResolver
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * This is the HibernateSpec with Flyway migrate / clean integrated instead of using Hibernate create-drop.
 *
 * It also integrates embedded postgres rule and sets it up with the port defined the application.yml file.
 */
@CompileStatic
abstract class FlybernateSpec extends Specification {

    @ClassRule @Shared SingleInstancePostgresRule postgresRule = EmbeddedPostgresRules.singleInstance().customize { builder ->
        builder.port = getConfig().getProperty('dataSource.embeddedPort', Integer)
    }

    @Shared @AutoCleanup HibernateDatastore hibernateDatastore
    @Shared PlatformTransactionManager transactionManager
    @Shared Flyway flyway = new Flyway()

    static Config getConfig() { // CHANGED extracted from setupSpec so postgresRule can access
        PropertySourcesLoader loader = new PropertySourcesLoader()
        ResourceLoader resourceLoader = new DefaultResourceLoader()
        MutablePropertySources propertySources = loader.propertySources
        loader.load resourceLoader.getResource("application.yml")
        loader.load resourceLoader.getResource("application.groovy")
        propertySources.addFirst(new MapPropertySource("defaults", getConfiguration()))
        return new PropertySourcesConfig(propertySources)
    }

    void setupSpec() {
        Config config = getConfig()

        // CHANGED added flyway migrate
        flyway.setDataSource(config.getProperty('dataSource.url'), config.getProperty('dataSource.username'), config.getProperty('dataSource.password'))
        flyway.placeholders = ['imageRoot': config.getProperty('imageservice.imagestore.root')]
        flyway.setLocations('db/migration')
        flyway.clean()
        flyway.migrate()
        // end CHANGED

        List<Class> domainClasses = getDomainClasses()
        String packageName = getPackageToScan(config)

        if (!domainClasses) {
            Package packageToScan = Package.getPackage(packageName) ?: getClass().getPackage()
            hibernateDatastore = new HibernateDatastore(
                    (PropertyResolver)config,
                    packageToScan)
        }
        else {
            hibernateDatastore = new HibernateDatastore(
                    (PropertyResolver)config,
                    domainClasses as Class[])
        }
        transactionManager = hibernateDatastore.getTransactionManager()
    }

    /**
     * The transaction status
     */
    TransactionStatus transactionStatus

    void setup() {
        transactionStatus = transactionManager.getTransaction(new DefaultTransactionAttribute())
    }

    void cleanup() {
        if(isRollback()) {
            transactionManager.rollback(transactionStatus)
        }
        else {
            transactionManager.commit(transactionStatus)
        }
        flyway.clean() // CHANGED added flyway.clean() to drop all db content
    }

    /**
     * @return The configuration
     */
    static Map getConfiguration() { // changed to static
        Collections.singletonMap(Settings.SETTING_DB_CREATE, "validate") // CHANGED from 'create-drop' to 'validate'
    }

    /**
     * @return the current session factory
     */
    SessionFactory getSessionFactory() {
        hibernateDatastore.getSessionFactory()
    }

    /**
     * @return the current Hibernate session
     */
    Session getHibernateSession() {
        getSessionFactory().getCurrentSession()
    }

    /**
     * Whether to rollback on each test (defaults to true)
     */
    boolean isRollback() {
        return true
    }
    /**
     * @return The domain classes
     */
    List<Class> getDomainClasses() { [] }

    /**
     * Obtains the default package to scan
     *
     * @param config The configuration
     * @return The package to scan
     */
    protected String getPackageToScan(Config config) {
        config.getProperty('grails.codegen.defaultPackage', getClass().package.name)
    }
}
