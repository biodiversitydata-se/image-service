package au.org.ala.images

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils

class DefaultStoragePathStrategy implements StoragePathStrategy {

    boolean forceUnixSeparator
    List<String> prefixes

    DefaultStoragePathStrategy(List<String> prefixes, boolean forceUnixSeparator) {
        this.prefixes = prefixes
        this.forceUnixSeparator = forceUnixSeparator
    }

    DefaultStoragePathStrategy(String prefix, boolean forceUnixSeparator) {
        this(Arrays.asList(StringUtils.split(prefix, forceUnixSeparator ? '/' as char : File.separatorChar)), forceUnixSeparator)
    }

    @Override
    String createPathFromUUID(String uuid, String... postfix) {
        def l = new ArrayList(prefixes)
        computeAndAppendLocalDirectoryPath(uuid, l)
        l.addAll(postfix)
        def result
        if (forceUnixSeparator) {
            result = '/' + FilenameUtils.normalize(l.join('/'), true)
        } else {
            // use system separator instead
            result = File.separator + FilenameUtils.normalize(l.join(File.separator))
        }
        return result
    }

    private static void computeAndAppendLocalDirectoryPath(String uuid, List bits) {
        for (int i = 1; i <= 4; ++i) {
            bits << uuid.charAt(uuid.length() - i);
        }
        bits << uuid // each image gets it's own directory
    }
}
