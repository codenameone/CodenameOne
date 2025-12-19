package java.nio.file;

/**
 * Minimal FileSystems helper returning a singleton stub FileSystem.
 */
public final class FileSystems {
    private static final FileSystem DEFAULT = new FileSystem();

    private FileSystems() {}

    public static FileSystem getDefault() {
        return DEFAULT;
    }
}
