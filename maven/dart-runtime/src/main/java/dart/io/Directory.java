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

import dart.async.Future;
import dart.async.Stream;
import dart.core.DartList;

/** Dart's {@code Directory}. */
public class Directory extends FileSystemEntity {

    /** Dart's {@code Directory.systemTemp}: the app's cache directory on a device. */
    public static final Directory systemTemp = temp();

    /** Dart's {@code Directory.current}: the app's home directory unless the app sets it. */
    public static Directory current = home();

    public Directory(String path) {
        super(path);
    }

    private static Directory temp() {
        if (com.codename1.ui.Display.isInitialized()) {
            com.codename1.io.FileSystemStorage fss = com.codename1.io.FileSystemStorage.getInstance();
            String dir = fss.hasCachesDir() ? fss.getCachesDir() : fss.getAppHomePath();
            return new Directory(Fs.trimSlash(Fs.Storage.plain(dir)));
        }
        return new Directory("/tmp");
    }

    private static Directory home() {
        if (com.codename1.ui.Display.isInitialized()) {
            return new Directory(Fs.trimSlash(Fs.Storage.plain(
                    com.codename1.io.FileSystemStorage.getInstance().getAppHomePath())));
        }
        return new Directory("/");
    }

    public Directory absolute() {
        return isAbsolute() ? this : new Directory(absolutePath(path));
    }

    public void createSync() {
        createSync(null);
    }

    public void createSync(Boolean recursive) {
        Fs fs = Fs.get();
        if (fs.exists(path)) {
            return;
        }
        if (recursive != null && recursive.booleanValue()) {
            fs.mkdirs(path);
        } else {
            if (!fs.exists(Fs.parentOf(path))) {
                throw new FileSystemException("Creation failed", path);
            }
            fs.mkdir(path);
        }
    }

    public Future<Directory> create() {
        return create(null);
    }

    public Future<Directory> create(Boolean recursive) {
        try {
            createSync(recursive);
            return Future.value(this);
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public DartList<FileSystemEntity> listSync() {
        return listSync(null, null);
    }

    public DartList<FileSystemEntity> listSync(Boolean recursive, Boolean followLinks) {
        Fs fs = Fs.get();
        if (!fs.isDirectory(path)) {
            throw new FileSystemException("Directory listing failed", path);
        }
        DartList<FileSystemEntity> out = new DartList<FileSystemEntity>();
        for (String child : fs.list(path)) {
            if (fs.isDirectory(child)) {
                Directory d = new Directory(child);
                out.add(d);
                if (recursive != null && recursive.booleanValue()) {
                    out.addAll(d.listSync(recursive, followLinks));
                }
            } else {
                out.add(new File(child));
            }
        }
        return out;
    }

    public Stream<FileSystemEntity> list() {
        return list(null, null);
    }

    public Stream<FileSystemEntity> list(Boolean recursive, Boolean followLinks) {
        try {
            return Stream.fromIterable(listSync(recursive, followLinks));
        } catch (RuntimeException e) {
            return Stream.<FileSystemEntity>error(e);
        }
    }

    public Directory renameSync(String newPath) {
        Fs fs = Fs.get();
        fs.mkdirs(newPath);
        for (FileSystemEntity e : listSync()) {
            String target = Fs.trimSlash(newPath) + "/" + Fs.nameOf(e.path);
            if (e instanceof Directory) {
                ((Directory) e).renameSync(target);
            } else {
                ((File) e).renameSync(target);
            }
        }
        fs.delete(path);
        return new Directory(newPath);
    }

    public Future<Directory> rename(String newPath) {
        try {
            return Future.value(renameSync(newPath));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public Directory createTempSync() {
        return createTempSync(null);
    }

    public Directory createTempSync(String prefix) {
        String base = Fs.trimSlash(path) + "/" + (prefix == null ? "" : prefix);
        long n = System.currentTimeMillis();
        while (Fs.get().exists(base + n)) {
            n++;
        }
        Directory d = new Directory(base + n);
        d.createSync(Boolean.TRUE);
        return d;
    }

    public Future<Directory> createTemp() {
        return createTemp(null);
    }

    public Future<Directory> createTemp(String prefix) {
        try {
            return Future.value(createTempSync(prefix));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    @Override
    public String toString() {
        return "Directory: '" + path + "'";
    }
}
