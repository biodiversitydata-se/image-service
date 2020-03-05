package au.org.ala.images.helper

import cloud.localstack.Localstack
import cloud.localstack.docker.annotation.LocalstackDockerAnnotationProcessor
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration
import org.junit.rules.ExternalResource

/**
 * This is {@link cloud.localstack.LocalstackTestRunner} implemented as a JUnit TestRule so that it can be used
 * with Spock specifications.
 */
class LocalstackRule extends ExternalResource {

    private static final LocalstackDockerAnnotationProcessor PROCESSOR = new LocalstackDockerAnnotationProcessor();

    private Localstack localstackDocker = Localstack.INSTANCE;

    private Class testClass

    LocalstackRule(Class testClass) {
        this.testClass = testClass
    }

    @Override
    protected void before() throws Throwable {
        final LocalstackDockerConfiguration dockerConfig = PROCESSOR.process(testClass)
        localstackDocker.startup(dockerConfig);
    }

    @Override
    protected void after() {
        localstackDocker.stop()
    }
}
