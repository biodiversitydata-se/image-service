package au.org.ala.images

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared

@Slf4j
class FileSystemStorageLocationSpec extends StorageLocationSpec implements DomainUnitTest<FileSystemStorageLocation> {

    @ClassRule @Shared TemporaryFolder tempFolder = new TemporaryFolder()
    @Shared File tempDir
    @Shared File tempDir2
    @Shared File altTempDir

    List<FileSystemStorageLocation> getStorageLocations() { [
                new FileSystemStorageLocation(basePath: tempDir.toString()).save()
                ,new FileSystemStorageLocation(basePath: tempDir2.toString()+"/").save()
    ]}
    FileSystemStorageLocation alternateStorageLocation

    def setupSpec() {
        tempDir = tempFolder.newFolder('storage')
        tempDir2 = tempFolder.newFolder('storage2')
        altTempDir = tempFolder.newFolder('altStorage')
    }

    def setup() {
        alternateStorageLocation = new FileSystemStorageLocation(basePath: altTempDir.toString()).save()
    }

}
