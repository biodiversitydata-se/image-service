package au.org.ala.images

trait StoragePathStrategy {

    String createThumbPathFromUUID(String uuid) {
        createPathFromUUID(uuid, 'thumbnail')
    }

    String createThumbLargePathFromUUID(String uuid, String type) {
        createPathFromUUID(uuid, "thumbnail_${type ?: 'large'}")
    }

    String createTilesPathFromUUID(String uuid) {
        createPathFromUUID(uuid, 'tms')
    }

    String createTilesPathFromUUID(String uuid, int x, int y, int z) {
        createPathFromUUID(uuid, 'tms', Integer.toString(z), Integer.toString(x), "${y}.png")
    }

    String createOriginalPathFromUUID(String uuid) {
        createPathFromUUID(uuid, 'original')
    }

    abstract String createPathFromUUID(String uuid, String... postfix)

}