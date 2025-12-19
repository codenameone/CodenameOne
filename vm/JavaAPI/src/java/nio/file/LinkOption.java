package java.nio.file;

/**
 * Minimal stub for link options.
 */
public final class LinkOption {
    public static final LinkOption NOFOLLOW_LINKS = new LinkOption("NOFOLLOW_LINKS");

    private final String name;

    private LinkOption(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
