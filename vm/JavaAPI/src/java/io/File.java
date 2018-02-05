/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.io;

import java.net.URI;
import java.net.URL;

/**
 *
 * @author shannah
 */
public class File {
    public static final String pathSeparator = ":";
    public static final char pathSeparatorChar = ':';
    public static final String separator = "/";
    public static final char separatorChar = '/';
    
    
    public File(File parent, String child) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public File(String pathname) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public File(URI uri) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public File(String parent, String child) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public boolean canExecute() {
        return false;
    }
    
    public boolean canRead() {
        return false;
    }
    
    public boolean canWrite() {
        return false;
    }
    
    public int compareTo(File pathname) {
        return 0;
    }
    public boolean createNewFile() {
        return false;
    }
    
    public static File createTempFile(String prefix, String suffix) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public static File createTempFile(String prefix, String suffix, File directory) {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public boolean delete() {
        return false;
    }
    
    public void deleteOnExit() {
        
    }
    
    public boolean exists() {
        return false;
    }
    
    public File getAbsoluteFile() {
        return null;
    }
    
    public String getAbsolutePath() {
        return null;
    }
    
    public String getCanonicalPath() {
        return null;
    }
    
    public File getCanonicalFile() {
        return null;
    }
    
    public long getFreeSpace() {
        return 0;
        
    }
    
    public String getName() {
        return null;
    }
    
    public String getParent() {
        return null;
    }
    
    public File getParentFile() {
        return null;
    }
    
    public String getPath() {
        return null;
    }
    
    public long getTotalSpace() {
        return 0;
    }
    
    public long getUsableSpace() {
        return 0;
    }
    
    public boolean isAbsolute() {
        return false;
    }
    
    public boolean isDirectory() {
        return false;
    }
    
    public boolean isFile() {
        return false;
    }
    
    public boolean isHidden() {
        return false;
    }
    
    public long lastModified() {
        return 0;
    }
    
    public long length() {
        return 0;
    }
    
    public String[] list() {
        return null;
    }
    
    public File[] listFiles() {
        return null;
    }
    
    public static File[] listRoots() {
        throw new UnsupportedOperationException("java.io.File not implemented");
    }
    
    public boolean mkdir() {
        return false;
    }
    
    public boolean mkdirs() {
        return false;
    }
    
    public boolean renameTo(File f) {
        return false;
    }
    
    public boolean setExecutable(boolean b) {
        return false;
    } 
    
    public boolean setExecutable(boolean b, boolean owner) {
        return false;
    }
    
    public boolean setReadable(boolean b) {
        return false;
        
    }
    
    public boolean setReadable(boolean b1, boolean b2) {
        return false;
    }
    
    public boolean setReadOnly() {
        return false;
    }
    
    public boolean setWritable(boolean writable) {
        return false;
    }
    
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return false;
    }
    
    public URI toURI() {
        return null;
    }
    
    public URL toURL() {
        return null;
    }
}
