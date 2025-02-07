package au.org.ala.images

class SpaceSavingFileSystemStorageLocation extends FileSystemStorageLocation {

    static constraints = {
    }

    static mapping = {
        cache true
    }

    @Override
    String toString() {
        "Space-saving-filesystem($id): $basePath"
    }
}
