package java.nio.file;

/**
 * Minimal stub FileSystem to satisfy JavaAPI dependencies.
 */
public class FileSystem {
    public Path getPath(String first, String... more) {
        return Paths.get(first, more);
    }
}
