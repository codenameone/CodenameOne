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
package com.bench;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File I/O microbenchmarks, shaped after what the translator actually does.
 *
 * <p>The self-hosting profile spends 11.8% of mutator time in open/write/close, and
 * the shape is unusual: {@code open} is 7.0% and {@code close} 3.9% while bulk
 * {@code write} does not reach the top ten. That says the cost is the PER-FILE path
 * -- the syscall boundary -- not moving bytes, so the arms below separate those two
 * costs instead of measuring a single blended number.</p>
 *
 * <p>Every arm uses the API the translator uses, because the question is whether OUR
 * implementation of these calls is slower than HotSpot's, not whether some better API
 * exists:</p>
 *
 * <ul>
 *   <li>{@code Parser.writeFile} -- {@code new FileOutputStream(f)}, ONE
 *       {@code write(byte[])} of the whole generated source, {@code close()}.
 *       Unbuffered by design: the content is already a single String.</li>
 *   <li>{@code ByteCodeTranslator.copy} -- 8192-byte chunks between a
 *       FileInputStream and a FileOutputStream.</li>
 *   <li>{@code Parser.parse} -- {@code new FileInputStream(f)} handed to ASM's
 *       ClassReader, which pulls the stream in chunks.</li>
 *   <li>{@code ByteCodeTranslator:1682} -- {@code DataInputStream.readFully}.</li>
 * </ul>
 *
 * <p>Sizes come from the real corpus: 855 emitted files totalling 25,976,388 bytes,
 * about 30KB each. FILES x SIZE is kept near that so the page-cache behaviour is
 * representative rather than a cache-resident toy.</p>
 *
 * <p>The checksum is over bytes actually transferred, so the harness's cross-check
 * still catches a VM that silently reads or writes the wrong thing.</p>
 */
public final class IoBench {
    private static final int WARMUP = 2;
    private static final int MEASURE = 5;

    private static int FILES = 400;
    private static final int SIZE = 30 * 1024;
    private static final int COPY_BUFFER = 8192;

    private static byte[] payload;
    private static File dir;

    private interface BenchFn {
        long run() throws IOException;
    }

    private static void runBench(String name, BenchFn fn) throws IOException {
        for (int warmup = 0; warmup < WARMUP; warmup++) {
            fn.run();
        }
        for (int repetition = 0; repetition < MEASURE; repetition++) {
            long started = System.nanoTime();
            long checksum = fn.run();
            long elapsed = System.nanoTime() - started;
            System.out.println("BENCH " + name + " rep " + repetition
                    + " ns=" + elapsed + " checksum=" + checksum);
        }
    }

    private static File fileAt(int i) {
        return new File(dir, "io_" + i + ".dat");
    }

    /** open + one full write + close, per file: exactly Parser.writeFile. */
    private static long writeWhole() throws IOException {
        long n = 0;
        for (int i = 0; i < FILES; i++) {
            FileOutputStream out = new FileOutputStream(fileAt(i));
            out.write(payload);
            out.close();
            n += payload.length;
        }
        return n;
    }

    /** open + close only. Isolates the per-file syscall path the profile points at. */
    private static long openCloseOnly() throws IOException {
        long n = 0;
        for (int i = 0; i < FILES; i++) {
            FileInputStream in = new FileInputStream(fileAt(i));
            in.close();
            n++;
        }
        return n;
    }

    /** chunked read to exhaustion: the shape ASM's ClassReader drives. */
    private static long readChunked() throws IOException {
        byte[] buffer = new byte[COPY_BUFFER];
        long n = 0;
        for (int i = 0; i < FILES; i++) {
            InputStream in = new FileInputStream(fileAt(i));
            int size = in.read(buffer);
            while (size > -1) {
                n += size;
                size = in.read(buffer);
            }
            in.close();
        }
        return n;
    }

    /** DataInputStream.readFully, the other read shape in the translator. */
    private static long readFully() throws IOException {
        byte[] buffer = new byte[SIZE];
        long n = 0;
        for (int i = 0; i < FILES; i++) {
            DataInputStream in = new DataInputStream(new FileInputStream(fileAt(i)));
            in.readFully(buffer);
            n += buffer.length;
            in.close();
        }
        return n;
    }

    /** ByteCodeTranslator.copy: 8192-byte chunks, stream to stream. */
    private static long copyStreams() throws IOException {
        byte[] buffer = new byte[COPY_BUFFER];
        long n = 0;
        for (int i = 0; i < FILES; i++) {
            InputStream in = new FileInputStream(fileAt(i));
            OutputStream out = new FileOutputStream(new File(dir, "copy.tmp"));
            int size = in.read(buffer);
            while (size > -1) {
                out.write(buffer, 0, size);
                n += size;
                size = in.read(buffer);
            }
            out.close();
            in.close();
        }
        return n;
    }

    /** many small writes to ONE stream: the ConcatenatingFileOutputStream shape. */
    private static long writeManySmall() throws IOException {
        byte[] chunk = new byte[512];
        System.arraycopy(payload, 0, chunk, 0, chunk.length);
        long n = 0;
        FileOutputStream out = new FileOutputStream(new File(dir, "concat.tmp"));
        for (int i = 0; i < FILES * 8; i++) {
            out.write(chunk, 0, chunk.length);
            n += chunk.length;
        }
        out.close();
        return n;
    }

    public static void main(String[] args) throws IOException {
        // No System.getProperty(String,String) in vm/JavaAPI -- the two-arg overload
        // does not exist there, so the directory is passed in or defaults literally.
        String base = args.length > 0 ? args[0] : "/tmp";
        // Optional second argument: how many files land in ONE directory. The
        // translator writes ~2,933 files into a single output directory, and APFS
        // create cost is not necessarily flat in directory size -- if it is not,
        // that is a real cost both arms pay and a real thing to fix, by sharding.
        if (args.length > 1) {
            FILES = Integer.parseInt(args[1]);
        }
        dir = new File(base, "cn1-iobench");
        dir.mkdirs();

        payload = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            payload[i] = (byte) ((i * 31 + 7) & 0xff);
        }
        // the read arms need the files to exist before they are timed
        writeWhole();

        runBench("writeWhole", new BenchFn() {
            public long run() throws IOException { return writeWhole(); }
        });
        runBench("openCloseOnly", new BenchFn() {
            public long run() throws IOException { return openCloseOnly(); }
        });
        runBench("readChunked", new BenchFn() {
            public long run() throws IOException { return readChunked(); }
        });
        runBench("readFully", new BenchFn() {
            public long run() throws IOException { return readFully(); }
        });
        runBench("copyStreams", new BenchFn() {
            public long run() throws IOException { return copyStreams(); }
        });
        runBench("writeManySmall", new BenchFn() {
            public long run() throws IOException { return writeManySmall(); }
        });

        // CONTENT VERIFICATION, deliberately outside the timed arms.
        //
        // The harness cross-checks a checksum, but every arm above computes it from
        // bytes handed to write() -- not from bytes that reached the disk. writeManySmall
        // runs 7.5x faster than HotSpot because this VM's FileOutputStream buffers where
        // HotSpot's does not, and a buffer that silently dropped its tail on close would
        // post exactly the same checksum and an even better time. So read the bytes back
        // and compare them.
        File probe = new File(dir, "verify.tmp");
        FileOutputStream vout = new FileOutputStream(probe);
        for (int i = 0; i < 64; i++) {
            vout.write(payload, i * 64, 64);
        }
        vout.close();
        byte[] back = new byte[64 * 64];
        DataInputStream vin = new DataInputStream(new FileInputStream(probe));
        vin.readFully(back);
        vin.close();
        int bad = 0;
        for (int i = 0; i < back.length; i++) {
            if (back[i] != payload[i]) {
                bad++;
            }
        }
        long onDisk = probe.length();
        if (bad != 0 || onDisk != back.length) {
            System.out.println("IOBENCH VERIFY FAILED mismatchedBytes=" + bad
                    + " fileLength=" + onDisk + " expected=" + back.length);
        } else {
            System.out.println("IOBENCH VERIFY OK bytes=" + back.length);
        }
        probe.delete();

        for (int i = 0; i < FILES; i++) {
            fileAt(i).delete();
        }
        new File(dir, "copy.tmp").delete();
        new File(dir, "concat.tmp").delete();
    }
}
