package au.org.ala.images

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks
import grails.testing.gorm.DomainUnitTest
import org.javaswift.joss.client.factory.AuthenticationMethod
import org.junit.ClassRule
import spock.lang.Shared


class SwiftStorageLocationSpec extends StorageLocationSpec implements DomainUnitTest<SwiftStorageLocation> {

    @ClassRule @Shared DockerComposeRule docker = DockerComposeRule.builder()
            .file("swift-aio.yml")
            .waitingForService("swift", HealthChecks.toRespond2xxOverHttp(8080) { port -> port.inFormat('http://$HOST:$EXTERNAL_PORT/healthcheck') })
            .build()

    @Override
    List<StorageLocation> getStorageLocations() {[
        new SwiftStorageLocation(username: 'test',  password: 'test', authUrl: 'https://example.org/', tenantId: '1', tenantName: 'name', containerName: 'images').save().with { mock = true; return it }
        , new SwiftStorageLocation(username: 'test:tester',  password: 'testing', authUrl: 'http://127.0.0.1:48080/auth/v1.0', tenantId: '', tenantName: '', containerName: 'images', authenticationMethod: AuthenticationMethod.BASIC).save()
    ]}

    @Override
    StorageLocation getAlternateStorageLocation() {
        new SwiftStorageLocation(username: 'test:tester',  password: 'testing', authUrl: 'http://127.0.0.1:48080/auth/v1.0', tenantId: '', tenantName: '', containerName: 'images-alt', authenticationMethod:  AuthenticationMethod.BASIC).save()
    }
}
