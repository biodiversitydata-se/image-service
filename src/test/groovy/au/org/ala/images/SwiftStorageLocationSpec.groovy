package au.org.ala.images

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.DockerPort
import com.palantir.docker.compose.connection.waiting.Attempt
import com.palantir.docker.compose.connection.waiting.HealthCheck
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure
import grails.testing.gorm.DomainUnitTest
import org.javaswift.joss.client.factory.AuthenticationMethod
import org.junit.ClassRule

import java.util.function.Function

class SwiftStorageLocationSpec extends StorageLocationSpec implements DomainUnitTest<SwiftStorageLocation> {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
            .file("swift-aio.yml")
            .waitingForHostNetworkedPort(48080, new HealthCheck<DockerPort>() {

                @Override
                SuccessOrFailure isHealthy(DockerPort target) {
                    SuccessOrFailure.onResultOf(new Attempt() {
                        @Override
                        boolean attempt() throws Exception {
                            target.listeningNow && target.isHttpResponding({
                                "http://localhost:${it.externalPort}/healthcheck"
                            }, true)
                        }
                    })
                }
            })
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
