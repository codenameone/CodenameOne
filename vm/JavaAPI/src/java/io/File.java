package java.io;

import java.net.URI;
import java.net.URL;

public class File {
    public static final String pathSeparator = ":";
    public static final char pathSeparatorChar = ':';
    public static final String separator = "/";
    public static final char separatorChar = '/';
    
    private String path;
    
    public File(String pathname) {
        if (pathname == null) {
            throw new NullPointerException();
        }
        this.path = pathname;
    }
    
    public File(String parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent != null) {
            if (parent.equals("")) {
                this.path = separator + child;
            } else {
                this.path = resolve(parent, child);
            }
        } else {
            this.path = child;
        }
    }
    
    public File(File parent, String child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (parent != null) {
            if (parent.getPath().equals("")) {
                this.path = separator + child;
            } else {
                this.path = resolve(parent.getPath(), child);
            }
        } else {
            this.path = child;
        }
    }

    public File(URI uri) {
        throw new UnsupportedOperationException("URI constructor not supported");
    }

    private String resolve(String parent, String child) {
        if (child.equals("")) return parent;
        if (parent.endsWith(separator)) return parent + child;
        return parent + separator + child;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getAbsolutePath() {
        if (isAbsolute()) return path;
        return getAbsolutePathImpl(path);
    }
    
    private native String getAbsolutePathImpl(String path);

    public String getCanonicalPath() throws IOException {
        return getCanonicalPathImpl(path);
    }
    
    private native String getCanonicalPathImpl(String path);

    public File getAbsoluteFile() {
        return new File(getAbsolutePath());
    }

    public File getCanonicalFile() throws IOException {
        return new File(getCanonicalPath());
    }
    
    public String getName() {
        int index = path.lastIndexOf(separatorChar);
        if (index < 0) return path;
        return path.substring(index + 1);
    }
    
    public String getParent() {
        int index = path.lastIndexOf(separatorChar);
        if (index < 0) return null;
        return path.substring(0, index);
    }
    
    public File getParentFile() {
        String p = getParent();
        if (p == null) return null;
        return new File(p);
    }
    
    public boolean isAbsolute() {
        return path.startsWith(separator);
    }
    
    public boolean exists() {
        return existsImpl(path);
    }
    
    private native boolean existsImpl(String path);

    public boolean isDirectory() {
        return isDirectoryImpl(path);
    }
    
    private native boolean isDirectoryImpl(String path);

    public boolean isFile() {
        return isFileImpl(path);
    }
    
    private native boolean isFileImpl(String path);

    public boolean isHidden() {
        return isHiddenImpl(path);
    }
    
    private native boolean isHiddenImpl(String path);

    public long lastModified() {
        return lastModifiedImpl(path);
    }
    
    private native long lastModifiedImpl(String path);

    public long length() {
        return lengthImpl(path);
    }
    
    private native long lengthImpl(String path);

    public boolean createNewFile() throws IOException {
        return createNewFileImpl(path);
    }
    
    private native boolean createNewFileImpl(String path);

    public boolean delete() {
        return deleteImpl(path);
    }
    
    private native boolean deleteImpl(String path);

    public void deleteOnExit() {
        // Not implemented
    }
    
    public String[] list() {
        return listImpl(path);
    }
    
    private native String[] listImpl(String path);

    public File[] listFiles() {
        String[] ss = list();
        if (ss == null) return null;
        int n = ss.length;
        File[] fs = new File[n];
        for (int i = 0; i < n; i++) {
            fs[i] = new File(this, ss[i]);
        }
        return fs;
    }

    public File[] listFiles(FileFilter filter) {
        String[] names = list();
        if (names == null) return null;
        File[] results = new File[names.length];
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            File f = new File(this, names[i]);
            if (filter == null || filter.accept(f)) {
                results[count++] = f;
            }
        }
        if (count == results.length) {
            return results;
        }
        File[] trimmed = new File[count];
        System.arraycopy(results, 0, trimmed, 0, count);
        return trimmed;
    }

    public File[] listFiles(FilenameFilter filter) {
        String[] names = list();
        if (names == null) return null;
        File[] results = new File[names.length];
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (filter == null || filter.accept(this, names[i])) {
                results[count++] = new File(this, names[i]);
            }
        }
        if (count == results.length) {
            return results;
        }
        File[] trimmed = new File[count];
        System.arraycopy(results, 0, trimmed, 0, count);
        return trimmed;
    }
    
    public boolean mkdir() {
        return mkdirImpl(path);
    }
    
    private native boolean mkdirImpl(String path);

    public boolean mkdirs() {
        if (exists()) {
            return false;
        }
        if (mkdir()) {
            return true;
        }
        File p = getParentFile();
        return (p != null && (p.mkdirs() || p.exists()) && mkdir());
    }
    
    public boolean renameTo(File dest) {
        return renameToImpl(path, dest.getPath());
    }
    
    private native boolean renameToImpl(String path, String dest);

    public boolean setReadOnly() {
        return setReadOnlyImpl(path);
    }
    
    private native boolean setReadOnlyImpl(String path);

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return setWritableImpl(path, writable);
    }
    
    public boolean setWritable(boolean writable) {
        return setWritable(writable, false);
    }
    
    private native boolean setWritableImpl(String path, boolean writable);

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return setReadableImpl(path, readable);
    }
    
    public boolean setReadable(boolean readable) {
        return setReadable(readable, false);
    }
    
    private native boolean setReadableImpl(String path, boolean readable);

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return setExecutableImpl(path, executable);
    }
    
    public boolean setExecutable(boolean executable) {
        return setExecutable(executable, false);
    }
    
    private native boolean setExecutableImpl(String path, boolean executable);

    public boolean canRead() {
        return canReadImpl(path);
    }
    
    private native boolean canReadImpl(String path);

    public boolean canWrite() {
        return canWriteImpl(path);
    }
    
    private native boolean canWriteImpl(String path);

    public boolean canExecute() {
        return canExecuteImpl(path);
    }
    
    private native boolean canExecuteImpl(String path);
    
    public long getTotalSpace() {
        return getTotalSpaceImpl(path);
    }
    
    private native long getTotalSpaceImpl(String path);

    public long getFreeSpace() {
        return getFreeSpaceImpl(path);
    }
    
    private native long getFreeSpaceImpl(String path);

    public long getUsableSpace() {
        return getUsableSpaceImpl(path);
    }
    
    private native long getUsableSpaceImpl(String path);

    public int compareTo(File pathname) {
        return getPath().compareTo(pathname.getPath());
    }
    
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof File)) {
            return compareTo((File)obj) == 0;
        }
        return false;
    }
    
    public int hashCode() {
        return getPath().hashCode() ^ 1234321;
    }

    public String toString() {
        return getPath();
    }

    public static File[] listRoots() {
        return new File[]{new File(separator)};
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        if (prefix.length() < 3)
            throw new IllegalArgumentException("Prefix string too short");
        if (suffix == null)
            suffix = ".tmp";
        if (directory == null) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            if (tmpDir == null) tmpDir = "/tmp";
            directory = new File(tmpDir);
        }

        File f;
        do {
            long n = System.currentTimeMillis();
             f = new File(directory, prefix + n + suffix);
        } while (f.exists());

        if (!f.createNewFile())
            throw new IOException("Unable to create temporary file");

        return f;
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null);
    }
    
    public URI toURI() {
         throw new UnsupportedOperationException();
    }
    
    public URL toURL() throws java.net.MalformedURLException {
         return new URL("file", "", getAbsolutePath());
    }
}
