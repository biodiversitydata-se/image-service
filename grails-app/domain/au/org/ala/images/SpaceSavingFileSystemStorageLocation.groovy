package au.org.ala.images

import au.org.ala.images.storage.SpaceSavingFileSystemStorageOperations
import au.org.ala.images.storage.StorageOperations
import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode(includes = ['basePath'])
class SpaceSavingFileSystemStorageLocation extends StorageLocation {

    String basePath

    static transients = ['storageOperations', '$storageOperations', 'probeFilesForImageInfo']

    static constraints = {
    }

    static mapping = {
        cache true
    }

    @Delegate @Lazy SpaceSavingFileSystemStorageOperations storageOperations = { new SpaceSavingFileSystemStorageOperations(basePath: basePath) }()

    StorageOperations asStandaloneStorageOperations() {
        storageOperations
    }

    @Override
    String toString() {
        "Space-saving-filesystem($id): $basePath"
    }
}
