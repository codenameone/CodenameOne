/*
 * Lightweight FilenameFilter stub to mirror the Java SE API.
 */
package java.io;

/**
 * Filters file names within a directory listing.
 */
public interface FilenameFilter {
    boolean accept(File dir, String name);
}
