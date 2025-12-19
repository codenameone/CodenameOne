/*
 * Lightweight FileFilter stub to mirror the Java SE API.
 */
package java.io;

/**
 * Simple filter for pathnames.
 */
public interface FileFilter {
    boolean accept(File pathname);
}
