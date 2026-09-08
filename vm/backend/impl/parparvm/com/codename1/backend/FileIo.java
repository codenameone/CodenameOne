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

/**
 * The file-system calls a static handler needs, isolated so that everything else
 * in [StaticFiles] -- ranges, conditional requests, MIME types, the containment
 * check -- is pure Java and shared by every target. That logic is the part worth
 * getting right once.
 *
 * There is one of these per target: the translated one goes to open/fstat/sendfile
 * directly, the Java SE one to the JDK's channels.
 */
public final class FileIo {
    private FileIo() {
    }

    /** Opens for reading. The descriptor, or -1. */
    /**
     * A descriptor for `relative` under `root`, or -1. Never a file outside `root`,
     * and never by checking afterwards: the kernel refuses the escape while it
     * resolves, so there is no window between the open and the check for a symlink
     * to move through.
     *
     * Returns {@link #BENEATH_UNSUPPORTED} where the platform has no such call, so
     * the caller can fall back rather than treat it as a missing file.
     */
    public static int openBeneath(String root, String relative) {
        return openBeneathImpl(root, relative);
    }

    /** openBeneath cannot answer here; fall back to open plus a resolved-path check. */
    public static final int BENEATH_UNSUPPORTED = -2;

    public static int openRead(String path) {
        return openReadImpl(path);
    }

    /**
     * Fills out[0]=size, out[1]=modified-time-millis, out[2]=1 for a directory.
     * Taken from the OPEN DESCRIPTOR rather than the path: stat-then-open lets the
     * file change in between, which is how a length header ends up disagreeing
     * with the body.
     */
    public static int stat(int fd, long[] out) {
        return statImpl(fd, out);
    }

    /**
     * Sends bytes from a file straight to a socket, without them entering this
     * process where the platform allows it. Returns how many moved, which may be
     * fewer than asked; the caller loops.
     */
    public static long sendFile(int socketFd, int fileFd, long offset, long count) {
        return sendFileImpl(socketFd, fileFd, offset, count);
    }

    /** True when [#sendFile] is a kernel copy rather than a read/write loop. */
    public static boolean hasSendFile() {
        return hasSendFileImpl();
    }

    public static int read(int fd, byte[] buffer, int offset, int length) {
        checkRange(buffer, offset, length);
        return readImpl(fd, buffer, offset, length);
    }

    /**
     * The canonical path, symlinks followed. The static handler proves a resolved
     * file is inside the document root with this: a check on the request string
     * alone is defeated by an encoded traversal or by a symlink out of the tree.
     */
    public static String realPath(String path) {
        return realPathImpl(path);
    }

    public static void close(int fd) {
        closeImpl(fd);
    }

    private static native int openBeneathImpl(String root, String relative);

    private static native int openReadImpl(String path);
    private static native int statImpl(int fd, long[] out);
    private static native long sendFileImpl(int socketFd, int fileFd, long offset, long count);
    private static native boolean hasSendFileImpl();

    /**
     * Refuses a slice that does not lie inside the array.
     *
     * The natives below index the array through the pointer they are handed and
     * ParparVM adds no bounds check of its own, so a bad offset is a native read
     * or write of whatever is next in the heap rather than an exception. The
     * JavaSE arm gets this free from its stream APIs, which is why such a bug is
     * invisible on the simulator and only appears once packaged. The subtraction
     * avoids the overflow that `offset + length` has.
     */
    private static void checkRange(byte[] buffer, int offset, int length) {
        if(buffer == null) {
            throw new NullPointerException("buffer");
        }
        if(offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException("offset " + offset + ", length "
                    + length + ", buffer " + buffer.length);
        }
    }

    private static native int readImpl(int fd, byte[] buffer, int offset, int length);
    private static native String realPathImpl(String path);
    private static native void closeImpl(int fd);
}
