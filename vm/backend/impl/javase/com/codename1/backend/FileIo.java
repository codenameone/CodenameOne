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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Map;

/**
 * Java SE twin of FileIo.
 *
 * This arm does NOT splice. It once did -- FileChannel.transferTo is sendfile on
 * Linux and macOS, and this comment used to say so -- but sendFile here reads
 * into a ByteBuffer and writes it out, so hasSendFile answers false and the
 * shared StaticFiles logic takes its copying path, which owns one reusable
 * buffer instead of allocating per call. The simulator serving a large file
 * through a copy is the right trade; telling the shared code it was a kernel
 * splice was not.
 */
public final class FileIo {
    private FileIo() {
    }

    private static final class OpenFile {
        final FileChannel channel;
        final Path path;
        /**
         * Captured when the descriptor was opened, not read from the path later.
         *
         * A static asset replaced between the open and the stat would otherwise be
         * described by its replacement while the bytes still came from the original
         * channel: the response advertised the new length, timestamp and ETag and
         * streamed the old file, which truncates or overruns whenever the two sizes
         * differ. A descriptor is a snapshot, so its metadata has to be one too.
         */
        final long size;
        final long modified;
        final boolean directory;
        /** Whether the descriptor and the path agreed; see openRead. */
        final boolean consistent;
        /**
     * The file's identity as the PATH saw it, or null where unsupported.
     *
     * <p>As the path saw it, not as the descriptor did, and that is the whole of
     * what this arm can say: there is no fstat for a FileChannel in public Java,
     * so every attribute here comes from a pathname lookup. A caller binding a
     * served descriptor to a verified path with this -- StaticFiles does -- gets
     * a weaker answer here than on the packaged arm, where the same number comes
     * from an fstat of the open descriptor. Said out loud because the number
     * looks identical on both sides of the API and only one of them describes
     * the bytes.
     */
        final Object fileKey;
        /**
         * The same identity as a number, for the ETag's third component. See the
         * inode in the native stat: it is what distinguishes a file replaced by a
         * timestamp-preserving copy from the one it replaced. 0 where the platform
         * will not say.
         */
        final long identity;
        long position;

        OpenFile(FileChannel channel, Path path) {
            this.channel = channel;
            this.path = path;
            long capturedSize = 0;
            long capturedModified = 0;
            boolean capturedDirectory = false;
            boolean capturedConsistent = false;
            Object capturedKey = null;
            long capturedIdentity = 0;
            long statedSize = 0;
            try {
                if(channel != null) {
                    capturedSize = channel.size();
                }
                // ONE SNAPSHOT, THE IDENTITY INCLUDED. The inode used to be read
                // by a second Files.getAttribute(path, "unix:ino") after this one,
                // and a second lookup of a PATH can find a different file: an
                // atomic replace landing between the two paired the old file's
                // size and mtime with the replacement's inode. openRead's
                // agreement loop could not see it, because what that loop compares
                // is the fileKey, which came from the first stat -- so a
                // replacement that kept the length and the timestamp served the
                // old bytes under the new file's ETag, and every later request for
                // the new content was answered 304 for as long as the client
                // asked. The unix view reports size, mtime, fileKey and ino
                // together, so there is no second lookup left to race.
                Map unix = null;
                try {
                    unix = Files.readAttributes(path, "unix:*");
                } catch (Exception unsupported) {
                    // Not a Unix filesystem view; the basic attributes below carry
                    // the same identity in fileKey, just not as a number.
                    unix = null;
                }
                if(unix != null) {
                    Object sizeValue = unix.get("size");
                    Object modifiedValue = unix.get("lastModifiedTime");
                    Object inoValue = unix.get("ino");
                    capturedKey = unix.get("fileKey");
                    capturedDirectory = Boolean.TRUE.equals(unix.get("isDirectory"));
                    if(modifiedValue instanceof FileTime) {
                        capturedModified = ((FileTime)modifiedValue).toMillis();
                    }
                    if(sizeValue instanceof Number) {
                        statedSize = ((Number)sizeValue).longValue();
                    }
                    if(inoValue instanceof Number) {
                        capturedIdentity = ((Number)inoValue).longValue();
                    }
                } else {
                    BasicFileAttributes attributes = Files.readAttributes(path,
                            BasicFileAttributes.class);
                    capturedModified = attributes.lastModifiedTime().toMillis();
                    capturedDirectory = attributes.isDirectory();
                    capturedKey = attributes.fileKey();
                    statedSize = attributes.size();
                    // The file key's hash is the next best identity available and
                    // is derived from the same device and inode where there is
                    // one -- and from this same snapshot, like everything else.
                    capturedIdentity = capturedKey == null ? 0 : capturedKey.hashCode();
                }
                if(channel == null) {
                    capturedSize = statedSize;
                    capturedConsistent = true;
                } else {
                    // The descriptor and the path describing the same file is what
                    // makes the size/mtime pair -- and so the ETag -- describe the
                    // bytes this descriptor will actually serve.
                    capturedConsistent = statedSize == capturedSize;
                }
            } catch (Exception ignored) {
                // stat() reports the failure; there is nothing to do here.
            }
            this.identity = capturedIdentity;
            this.size = capturedSize;
            this.modified = capturedModified;
            this.directory = capturedDirectory;
            this.consistent = capturedConsistent;
            this.fileKey = capturedKey;
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

    /** openRead: the file is there and could not be opened. See the native twin. */
    public static final int OPEN_FAILED = -2;

    public static int openRead(String path) {
        try {
            Path p = Paths.get(path);
            if(Files.isDirectory(p)) {
                // A directory has no channel, but the caller stats it and retries
                // at the index file, so it must still get a descriptor back.
                return Descriptors.add(new OpenFile(null, p));
            }
            // Opened and stat'ed until the two AGREE. The size comes from the
            // descriptor and the timestamp from the path, so a file replaced
            // between them pairs the old bytes with the new file's mtime -- and
            // StaticFiles builds its ETag from exactly that pair, so a client
            // would cache the old content under the replacement's validator and
            // be told 304 for as long as it asked. Java 8 has no fstat for a
            // channel, so the race is detected rather than avoided: if the
            // descriptor's size and the path's size disagree, the file changed
            // under us and both are re-taken. A few attempts is plenty for an
            // atomic replace; a file being rewritten continuously has no
            // consistent validator to offer and gets the last pair read.
            OpenFile opened = null;
            for(int attempt = 0 ; attempt < 3 ; attempt++) {
                // The file's IDENTITY across the open, because equal sizes prove
                // nothing: a replacement by a file of the same length passes the
                // size test, and then the old bytes are served under the new
                // file's ETag -- so every later request for the new content is
                // told 304 and the client caches the old representation for as
                // long as it asks. An inode changes even when a length does not.
                // Null where the filesystem has no such notion, and there the
                // size test is all there is, which is what this did before.
                Object keyBefore = fileKeyOf(p);
                FileChannel channel = FileChannel.open(p, StandardOpenOption.READ);
                OpenFile candidate = new OpenFile(channel, p);
                boolean sameFile = keyBefore == null || candidate.fileKey == null
                        || keyBefore.equals(candidate.fileKey);
                if((candidate.consistent && sameFile) || attempt == 2) {
                    opened = candidate;
                    break;
                }
                channel.close();
            }
            return Descriptors.add(opened);
        } catch (java.nio.file.NoSuchFileException absent) {
            // A DANGLING SYMLINK IS NOT AN ABSENT FILE, and the open follows the
            // link, so the exception names the missing TARGET and looks exactly
            // like a path that names nothing. A deployment whose
            // application.properties is a symlink into a volume that failed to
            // mount then read as "no configuration": every file-based setting
            // fell back to an environment default, and a server naming its TLS
            // certificate and key in that file came up in PLAINTEXT. That is what
            // this -1/-2 split exists to prevent, arriving through the one answer
            // the split treats as benign.
            //
            // NOFOLLOW_LINKS asks about the entry rather than its target, so a
            // link that is there answers true while a path that names nothing
            // answers false. Matches the lstat the translated arm does.
            try {
                if(danglingLinkIn(java.nio.file.Paths.get(path))) {
                    return OPEN_FAILED;
                }
            } catch (Exception unnameable) {
                // A path Paths.get refuses is not a path to anything; fall
                // through to the absent answer the other catch below gives it.
            }
            return -1;
        } catch (java.nio.file.InvalidPathException unnameable) {
            // ABSENT, NOT UNREADABLE, and the translated arm says so explicitly:
            // a path holding a NUL is not a path to anything. Paths.get throws
            // this rather than NoSuchFileException, so the first version of the
            // split below answered -2 here and the two arms disagreed about the
            // one case SelfTest already checks on both.
            return -1;
        } catch (Exception err) {
            // THE SAME SPLIT THE NATIVE ARM MAKES. -1 is "no such file", which a
            // caller may treat as an absent optional file; -2 is "there is one
            // and it could not be opened", which nobody may quietly ignore.
            return OPEN_FAILED;
        }
    }

    /**
     * Whether any component of {@code p} is a symlink whose target is missing.
     *
     * <p>EVERY COMPONENT, not just the last one. NOFOLLOW_LINKS asks about the
     * entry the path names and nothing above it, so
     * cn1.config.location=/config/current with current -> missing-release opened
     * /config/current/application.properties, got NoSuchFileException for a
     * component in the MIDDLE, and answered "absent" -- the same silent discard
     * of every file-based setting, TLS paths included, that the final-component
     * check was added to stop. A release directory swung by symlink is how most
     * deployments roll forward, so the broken middle is the likelier half.
     *
     * <p>Walked from the root down, and a component that exists without following
     * links but not with them is the answer: that is a link pointing at nothing.
     * A path that simply names nothing has no such component and stays absent.
     */
    private static boolean danglingLinkIn(Path p) {
        Path walked = p.isAbsolute() ? p.getRoot() : null;
        java.util.Iterator<Path> parts = p.iterator();
        while(parts.hasNext()) {
            Path part = parts.next();
            walked = walked == null ? part : walked.resolve(part);
            if(java.nio.file.Files.exists(walked, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    && !java.nio.file.Files.exists(walked)) {
                return true;
            }
        }
        return false;
    }

    /** A file's identity, or null when the filesystem does not report one. */
    private static Object fileKeyOf(Path p) {
        try {
            return Files.readAttributes(p, BasicFileAttributes.class).fileKey();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static int stat(int fd, long[] out) {
        Object entry = Descriptors.get(fd);
        if(!(entry instanceof OpenFile) || out == null || out.length < 3) {
            return -1;
        }
        OpenFile file = (OpenFile)entry;
        // From the descriptor, so the metadata and the bytes describe one file.
        out[0] = file.size;
        out[1] = file.modified;
        out[2] = file.directory ? 1 : 0;
        if(out.length > 3) {
            out[3] = file.identity;
        }
        return 0;
    }

    /**
     * One bounded chunk of the file to the socket, THROUGH THE DEADLINE.
     *
     * <p>Not FileChannel.transferTo, which is what this was. transferTo is sendfile
     * underneath and moves the bytes without copying them, but it writes straight
     * to the channel and so goes around ServerSocket.write -- and around
     * Deadlines.writeWithDeadline, whose whole reason for existing is stated where
     * it is called: a client that stops reading otherwise parks this worker
     * indefinitely. In the pooled mode the descriptor here is BLOCKING, so a client
     * that fills its receive buffer and stops held a worker for as long as it cared
     * to, and enough slow downloads took the pool. Ordinary responses were never
     * exposed to that; a static file was.
     *
     * <p>The copy this does instead costs a buffer per call on the arm that runs
     * the local dev server, which is the arm that can afford it. The packaged
     * server has the real sendfile.
     */
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
            int want = (int)Math.min(count, 64L * 1024L);
            if(want <= 0) {
                return 0;
            }
            ByteBuffer chunk = ByteBuffer.allocate(want);
            int read = channel.read(chunk, offset);
            if(read <= 0) {
                return read < 0 ? -1 : 0;
            }
            ServerSocket.write(socketFd, chunk.array(), 0, read);
            return read;
        } catch (Exception err) {
            return -1;
        }
    }

    public static boolean hasSendFile() {
        // FALSE, because it is false. This arm allocates a ByteBuffer per call
        // and copies at most 64 KiB through it, so answering true sent
        // StaticFiles down its sendfile branch: a large file then made roughly
        // its own size in short-lived arrays instead of using copyBody's one
        // reusable buffer, and isZeroCopy() told callers the kernel was splicing
        // when nothing was. The answer was true when this arm used transferTo,
        // and was left behind when that went.
        return false;
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
