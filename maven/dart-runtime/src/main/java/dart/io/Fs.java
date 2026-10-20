/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package dart.io;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where dart:io's files live. On a device -- and in the simulator -- that is
 * {@link FileSystemStorage}; Dart's plain paths are turned into its {@code file://}
 * URLs and back. Before Codename One is initialized (the transpiler's own tests run
 * transpiled code headless) an in-memory file system stands in, so code that writes
 * and reads back a file still behaves.
 */
abstract class Fs {

    private static Fs memory;

    static Fs get() {
        if (Display.isInitialized()) {
            return Storage.INSTANCE;
        }
        synchronized (Fs.class) {
            if (memory == null) {
                memory = new Memory();
            }
            return memory;
        }
    }

    abstract boolean exists(String path);

    abstract boolean isDirectory(String path);

    abstract void mkdir(String path);

    abstract void delete(String path);

    /** Full paths of the direct children of a directory. */
    abstract List<String> list(String path);

    abstract byte[] read(String path);

    abstract void write(String path, byte[] data, boolean append);

    abstract long length(String path);

    abstract long lastModified(String path);

    static String parentOf(String path) {
        String p = trimSlash(path);
        int i = p.lastIndexOf('/');
        if (i < 0) {
            return ".";
        }
        return i == 0 ? "/" : p.substring(0, i);
    }

    static String nameOf(String path) {
        String p = trimSlash(path);
        return p.substring(p.lastIndexOf('/') + 1);
    }

    static String trimSlash(String path) {
        String p = path;
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /** Creates {@code path} and every missing parent. */
    void mkdirs(String path) {
        if (path.equals("/") || path.equals(".") || exists(path)) {
            return;
        }
        mkdirs(parentOf(path));
        mkdir(path);
    }

    /** FileSystemStorage, with Dart paths mapped to file:// URLs. */
    static final class Storage extends Fs {
        static final Storage INSTANCE = new Storage();

        private static FileSystemStorage fss() {
            return FileSystemStorage.getInstance();
        }

        static String url(String path) {
            if (path.startsWith("file:")) {
                return path;
            }
            if (path.startsWith("/")) {
                return "file://" + path;
            }
            // Dart resolves a relative path against Directory.current (the app home
            // here); FileSystemStorage would use the process working directory, which
            // on Android is not even writable.
            String base = Directory.current.path();
            return "file://" + (base.endsWith("/") ? base : base + "/") + path;
        }

        static String plain(String url) {
            return url.startsWith("file://") ? url.substring(7) : url;
        }

        @Override
        boolean exists(String path) {
            return fss().exists(url(path));
        }

        @Override
        boolean isDirectory(String path) {
            return fss().isDirectory(url(path));
        }

        @Override
        void mkdir(String path) {
            fss().mkdir(url(path));
        }

        @Override
        void delete(String path) {
            fss().delete(url(path));
        }

        @Override
        List<String> list(String path) {
            List<String> out = new ArrayList<String>();
            try {
                String base = trimSlash(path);
                String[] names = fss().listFiles(url(base));
                if (names != null) {
                    for (String n : names) {
                        String name = n.endsWith("/") ? n.substring(0, n.length() - 1) : n;
                        out.add(plain(base.endsWith("/") ? base + name : base + "/" + name));
                    }
                }
            } catch (java.io.IOException e) {
                throw new FileSystemException("Directory listing failed: " + e.getMessage(), path);
            }
            return out;
        }

        @Override
        byte[] read(String path) {
            InputStream in = null;
            try {
                in = fss().openInputStream(url(path));
                return Util.readInputStream(in);
            } catch (java.io.IOException e) {
                throw new FileSystemException("Cannot open file: " + e.getMessage(), path);
            } finally {
                Util.cleanup(in);
            }
        }

        @Override
        void write(String path, byte[] data, boolean append) {
            OutputStream out = null;
            try {
                String u = url(path);
                out = append && fss().exists(u) ? fss().openOutputStream(u, (int) fss().getLength(u))
                        : fss().openOutputStream(u);
                out.write(data);
            } catch (java.io.IOException e) {
                throw new FileSystemException("Cannot write file: " + e.getMessage(), path);
            } finally {
                Util.cleanup(out);
            }
        }

        @Override
        long length(String path) {
            return fss().getLength(url(path));
        }

        @Override
        long lastModified(String path) {
            if (!exists(path)) {
                throw new FileSystemException("Cannot retrieve modification time", path);
            }
            return fss().getLastModified(url(path));
        }
    }

    /** A process-local file system for code that runs before Codename One starts. */
    static final class Memory extends Fs {
        private final Map<String, byte[]> files = new HashMap<String, byte[]>();
        private final Map<String, Long> times = new HashMap<String, Long>();
        private final java.util.Set<String> dirs = new java.util.HashSet<String>();

        Memory() {
            dirs.add("/");
        }

        private static String key(String path) {
            return trimSlash(Storage.plain(path));
        }

        @Override
        synchronized boolean exists(String path) {
            String k = key(path);
            return files.containsKey(k) || dirs.contains(k);
        }

        @Override
        synchronized boolean isDirectory(String path) {
            return dirs.contains(key(path));
        }

        @Override
        synchronized void mkdir(String path) {
            dirs.add(key(path));
        }

        @Override
        synchronized void delete(String path) {
            String k = key(path);
            files.remove(k);
            dirs.remove(k);
        }

        @Override
        synchronized List<String> list(String path) {
            String k = key(path);
            String prefix = k.endsWith("/") ? k : k + "/";
            List<String> out = new ArrayList<String>();
            for (String f : files.keySet()) {
                if (f.startsWith(prefix) && f.indexOf('/', prefix.length()) < 0) {
                    out.add(f);
                }
            }
            for (String d : dirs) {
                if (d.startsWith(prefix) && d.length() > prefix.length() && d.indexOf('/', prefix.length()) < 0) {
                    out.add(d);
                }
            }
            java.util.Collections.sort(out);
            return out;
        }

        @Override
        synchronized byte[] read(String path) {
            byte[] b = files.get(key(path));
            if (b == null) {
                throw new FileSystemException("Cannot open file", path);
            }
            return b.clone();
        }

        @Override
        synchronized void write(String path, byte[] data, boolean append) {
            String k = key(path);
            if (!dirs.contains(parentOf(k))) {
                throw new FileSystemException("Cannot open file", path);
            }
            byte[] old = append ? files.get(k) : null;
            if (old == null) {
                files.put(k, data.clone());
            } else {
                byte[] all = new byte[old.length + data.length];
                System.arraycopy(old, 0, all, 0, old.length);
                System.arraycopy(data, 0, all, old.length, data.length);
                files.put(k, all);
            }
            times.put(k, Long.valueOf(System.currentTimeMillis()));
        }

        @Override
        synchronized long length(String path) {
            byte[] b = files.get(key(path));
            if (b == null) {
                throw new FileSystemException("Cannot retrieve length of file", path);
            }
            return b.length;
        }

        @Override
        synchronized long lastModified(String path) {
            // A missing file is an error, as in Dart, never the epoch.
            Long t = times.get(key(path));
            if (t == null || !files.containsKey(key(path))) {
                throw new FileSystemException("Cannot retrieve modification time", path);
            }
            return t.longValue();
        }
    }
}
