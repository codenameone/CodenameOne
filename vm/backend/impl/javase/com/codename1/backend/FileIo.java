/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.backend;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Java SE twin of FileIo.
 *
 * FileChannel.transferTo IS sendfile on Linux and macOS, so the zero-copy path is
 * not lost here -- the JDK makes the same system call. The shared StaticFiles
 * logic above is untouched.
 */
public final class FileIo {
    private FileIo() {
    }

    private static final class OpenFile {
        final FileChannel channel;
        final Path path;
        long position;

        OpenFile(FileChannel channel, Path path) {
            this.channel = channel;
            this.path = path;
        }
    }

    /** Unsupported on this runtime; see the ParparVM implementation. */
    public static final int BENEATH_UNSUPPORTED = -2;

    /**
     * Always {@link #BENEATH_UNSUPPORTED} here. The local Java SE loop has no
     * openat2, and a Java-side reimplementation would be the same racy
     * open-then-check it replaces -- saying so lets the caller keep the older path
     * rather than believe a check that did not happen.
     */
    public static int openBeneath(String root, String relative) {
        return BENEATH_UNSUPPORTED;
    }

    public static int openRead(String path) {
        try {
            Path p = Paths.get(path);
            if(Files.isDirectory(p)) {
                // A directory has no channel, but the caller stats it and retries
                // at the index file, so it must still get a descriptor back.
                return Descriptors.add(new OpenFile(null, p));
            }
            return Descriptors.add(new OpenFile(
                    FileChannel.open(p, StandardOpenOption.READ), p));
        } catch (Exception err) {
            return -1;
        }
    }

    public static int stat(int fd, long[] out) {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof OpenFile) || out == null || out.length < 3) {
            return -1;
        }
        OpenFile file = (OpenFile)entry;
        try {
            BasicFileAttributes attributes = Files.readAttributes(file.path,
                    BasicFileAttributes.class);
            out[0] = attributes.size();
            out[1] = attributes.lastModifiedTime().toMillis();
            out[2] = attributes.isDirectory() ? 1 : 0;
            return 0;
        } catch (Exception err) {
            return -1;
        }
    }

    public static long sendFile(int socketFd, int fileFd, long offset, long count) {
        Object file = Descriptors.get(fileFd);
        Object socket = Descriptors.get(socketFd);
        if(!(file instanceof OpenFile) || !(socket instanceof SocketChannel)) {
            return -1;
        }
        FileChannel channel = ((OpenFile)file).channel;
        if(channel == null) {
            return -1;
        }
        try {
            return channel.transferTo(offset, count, (WritableByteChannel)socket);
        } catch (Exception err) {
            return -1;
        }
    }

    public static boolean hasSendFile() {
        // transferTo is sendfile underneath on every platform this runs on.
        return true;
    }

    public static int read(int fd, byte[] buffer, int offset, int length) {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof OpenFile) || buffer == null) {
            return -1;
        }
        OpenFile file = (OpenFile)entry;
        if(file.channel == null) {
            return -1;
        }
        try {
            ByteBuffer target = ByteBuffer.wrap(buffer, offset, length);
            // A position is tracked explicitly so successive reads advance, which
            // is what the shared code expects of a descriptor.
            int n = file.channel.read(target, file.position);
            if(n > 0) {
                file.position += n;
            }
            return n < 0 ? 0 : n;
        } catch (Exception err) {
            return -1;
        }
    }

    public static String realPath(String path) {
        try {
            // Symlinks followed, as realpath does: the containment check above is
            // only sound on a fully resolved path.
            return Paths.get(path).toRealPath().toString();
        } catch (Exception err) {
            return null;
        }
    }

    public static void close(int fd) {
        Object entry = Descriptors.remove(fd);
        if(entry instanceof OpenFile) {
            FileChannel channel = ((OpenFile)entry).channel;
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // already gone
                }
            }
        }
    }
}
