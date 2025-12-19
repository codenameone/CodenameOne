/*
 * Minimal Paths helper mirroring Java SE.
 */
package java.nio.file;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.FileSystems;

public final class Paths {
    private Paths() {}

    public static Path get(String first, String... more) {
        StringBuilder sb = new StringBuilder();
        if (first != null) {
            sb.append(first);
        }
        if (more != null) {
            for (int i = 0; i < more.length; i++) {
                if (sb.length() > 0 && !sb.toString().endsWith("/")) {
                    sb.append("/");
                }
                sb.append(more[i]);
            }
        }
        return new SimplePath(sb.toString());
    }

    /**
        * Simple Path implementation used by the JavaAPI.
        */
    static class SimplePath implements Path {
        private final String rawPath;

        SimplePath(String path) {
            this.rawPath = path == null ? "" : path.replace('\\', '/');
        }

        public FileSystem getFileSystem() {
            return FileSystems.getDefault();
        }

        public boolean isAbsolute() {
            return rawPath.startsWith("/") || (rawPath.length() > 1 && rawPath.charAt(1) == ':');
        }

        public Path getRoot() {
            if (isAbsolute()) {
                if (rawPath.startsWith("/")) {
                    return new SimplePath("/");
                }
                return new SimplePath(rawPath.substring(0, 2));
            }
            return null;
        }

        public Path getFileName() {
            int idx = rawPath.lastIndexOf('/');
            if (idx < 0) {
                return this;
            }
            return new SimplePath(rawPath.substring(idx + 1));
        }

        public Path getParent() {
            int idx = rawPath.lastIndexOf('/');
            if (idx <= 0) {
                return null;
            }
            return new SimplePath(rawPath.substring(0, idx));
        }

        public int getNameCount() {
            return components().size();
        }

        public Path getName(int index) {
            List<String> c = components();
            if (index < 0 || index >= c.size()) {
                throw new IllegalArgumentException("Index out of range");
            }
            return new SimplePath(c.get(index));
        }

        public Path subpath(int beginIndex, int endIndex) {
            List<String> c = components();
            if (beginIndex < 0 || endIndex > c.size() || beginIndex >= endIndex) {
                throw new IllegalArgumentException("Invalid subpath range");
            }
            StringBuilder sb = new StringBuilder();
            for (int i = beginIndex; i < endIndex; i++) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(c.get(i));
            }
            return new SimplePath(sb.toString());
        }

        public boolean startsWith(Path other) {
            return other != null && rawPath.startsWith(other.toString());
        }

        public boolean startsWith(String other) {
            return other != null && rawPath.startsWith(other);
        }

        public boolean endsWith(Path other) {
            return other != null && rawPath.endsWith(other.toString());
        }

        public boolean endsWith(String other) {
            return other != null && rawPath.endsWith(other);
        }

        public Path normalize() {
            List<String> c = components();
            List<String> normalized = new ArrayList<String>();
            for (int i = 0; i < c.size(); i++) {
                String comp = c.get(i);
                if (".".equals(comp)) {
                    continue;
                }
                if ("..".equals(comp)) {
                    if (!normalized.isEmpty()) {
                        normalized.remove(normalized.size() - 1);
                    }
                } else {
                    normalized.add(comp);
                }
            }
            StringBuilder sb = new StringBuilder();
            if (isAbsolute()) {
                sb.append("/");
            }
            for (int i = 0; i < normalized.size(); i++) {
                if (i > 0) {
                    sb.append('/');
                }
                sb.append(normalized.get(i));
            }
            return new SimplePath(sb.toString());
        }

        public Path resolve(Path other) {
            if (other == null) {
                return this;
            }
            return resolve(other.toString());
        }

        public Path resolve(String other) {
            if (other == null || other.length() == 0) {
                return this;
            }
            if (other.startsWith("/")) {
                return new SimplePath(other);
            }
            if (rawPath.endsWith("/")) {
                return new SimplePath(rawPath + other);
            }
            if (rawPath.length() == 0) {
                return new SimplePath(other);
            }
            return new SimplePath(rawPath + "/" + other);
        }

        public Path resolveSibling(Path other) {
            return resolveSibling(other == null ? null : other.toString());
        }

        public Path resolveSibling(String other) {
            Path parent = getParent();
            if (parent == null) {
                return other == null ? this : new SimplePath(other);
            }
            return parent.resolve(other);
        }

        public Path relativize(Path other) {
            if (other == null) {
                throw new NullPointerException("Other path cannot be null");
            }
            List<String> base = components();
            List<String> target = ((SimplePath) other.normalize()).components();
            int i = 0;
            while (i < base.size() && i < target.size() && base.get(i).equals(target.get(i))) {
                i++;
            }
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < base.size(); j++) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append("..");
            }
            for (int j = i; j < target.size(); j++) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(target.get(j));
            }
            return new SimplePath(sb.toString());
        }

        public java.net.URI toUri() {
            return new java.io.File(rawPath).toURI();
        }

        public Path toAbsolutePath() {
            return new SimplePath(new java.io.File(rawPath).getAbsolutePath());
        }

        public Path toRealPath(LinkOption... options) throws java.io.IOException {
            return new SimplePath(new java.io.File(rawPath).getCanonicalPath());
        }

        public java.io.File toFile() {
            return new java.io.File(rawPath);
        }

        public Path register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws java.io.IOException {
            throw new java.io.IOException("WatchService not supported in JavaAPI");
        }

        public Path register(WatchService watcher, WatchEvent.Kind<?>... events) throws java.io.IOException {
            throw new java.io.IOException("WatchService not supported in JavaAPI");
        }

        public java.util.Iterator<Path> iterator() {
            final List<String> c = components();
            return new java.util.Iterator<Path>() {
                int idx;
                public boolean hasNext() { return idx < c.size(); }
                public Path next() { return new SimplePath(c.get(idx++)); }
                public void remove() { throw new UnsupportedOperationException(); }
            };
        }

        public int compareTo(Path other) {
            if (other == null) {
                return 1;
            }
            return rawPath.compareTo(other.toString());
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Path)) {
                return false;
            }
            return rawPath.equals(other.toString());
        }

        public int hashCode() {
            return rawPath.hashCode();
        }

        public String toString() {
            return rawPath;
        }

        private List<String> components() {
            List<String> c = new ArrayList<String>();
            String normalized = rawPath;
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.length() == 0) {
                return c;
            }
            int start = 0;
            for (int i = 0; i <= normalized.length(); i++) {
                if (i == normalized.length() || normalized.charAt(i) == '/') {
                    if (i > start) {
                        c.add(normalized.substring(start, i));
                    }
                    start = i + 1;
                }
            }
            return c;
        }
    }
}
