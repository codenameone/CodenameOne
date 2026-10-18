/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/// java.io's buffered streams, readers and writers and PrintWriter.
public final class IoChecks {
    private IoChecks() {
    }

    private static final String[] TEXTS = {"", "a", "a\n", "a\r\nb\rc\n\nd", "\r", "\r\n", "\n\n", "x\r\r\ny",
        "caf\u00e9\nline two\r\n", "no newline at the end"};

    private static String lines(BufferedReader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append('<').append(line).append('>');
        }
        return sb.append(r.read()).toString();
    }


    /// Runs the group.
    public static void run() {
        // A PrintWriter over a PrintWriter reports the inner one's failure.
        Bulk.check("Io.aClosedReaderOrWriterHasNoEncoding", () -> {
            java.io.InputStreamReader r = new java.io.InputStreamReader(new java.io.ByteArrayInputStream(new byte[0]), "UTF-8");
            java.io.OutputStreamWriter w = new java.io.OutputStreamWriter(new java.io.ByteArrayOutputStream(), "UTF-8");
            Bulk.p("open", r.getEncoding() + "," + w.getEncoding());
            r.close();
            w.close();
            Bulk.p("closed", r.getEncoding() + "," + w.getEncoding());
        });
        Bulk.check("Io.aBufferedWritersFlushFailureWinsOverClose", () -> {
            java.io.Writer failing = new java.io.Writer() {
                @Override
                public void write(char[] c, int off, int len) throws java.io.IOException {
                    throw new java.io.IOException("write");
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws java.io.IOException {
                    throw new java.io.IOException("close");
                }
            };
            java.io.BufferedWriter bw = new java.io.BufferedWriter(failing);
            bw.write("x");
            String got;
            try {
                bw.close();
                got = "none";
            } catch (java.io.IOException e) {
                got = e.getMessage() + " suppressed=" + e.getSuppressed().length;
            }
            Bulk.p("got", got);
        });
        Bulk.check("Io.aBufferedStreamsFlushFailureWinsOverClose", () -> {
            java.io.OutputStream failing = new java.io.OutputStream() {
                @Override
                public void write(int b) throws java.io.IOException {
                    throw new java.io.IOException("write");
                }

                @Override
                public void write(byte[] b, int off, int len) throws java.io.IOException {
                    throw new java.io.IOException("write");
                }

                @Override
                public void close() throws java.io.IOException {
                    throw new java.io.IOException("close");
                }
            };
            java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(failing);
            bos.write(1);
            String got;
            try {
                bos.close();
                got = "none";
            } catch (java.io.IOException e) {
                // JDK 8 makes close's failure the primary one, later JDKs the
                // write's (as this runtime does): compare both, unordered.
                java.util.TreeSet<String> both = new java.util.TreeSet<String>();
                both.add(e.getMessage());
                for (Throwable t : e.getSuppressed()) {
                    both.add(t.getMessage());
                }
                got = both.toString();
            }
            Bulk.p("got", got);
        });
        Bulk.check("Io.anInterruptedWriteReInterrupts", () -> {
            java.io.Writer interrupted = new java.io.Writer() {
                @Override
                public void write(char[] c, int off, int len) throws java.io.IOException {
                    throw new java.io.InterruptedIOException();
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            java.io.PrintWriter pw = new java.io.PrintWriter(interrupted);
            pw.write("x");
            Bulk.p("interrupted", Thread.interrupted());
            Bulk.p("error", pw.checkError());
        });
        Bulk.check("Io.aNestedPrintWritersErrorIsReported", () -> {
            java.io.Writer failing = new java.io.Writer() {
                public void write(char[] b, int o, int l) throws java.io.IOException {
                    throw new java.io.IOException("refused");
                }
                public void flush() {
                }
                public void close() {
                }
            };
            java.io.PrintWriter outer = new java.io.PrintWriter(new java.io.PrintWriter(failing));
            outer.print("x");
            Bulk.p("checkError", outer.checkError());
        });
        Bulk.check("Io.aPendingCrlfLfDoesNotMakeAReaderReady", () -> {
            // readLine stops at the CR; the LF it still owes is buffered but is never
            // returned, so it cannot make the reader ready over a source that is not.
            BufferedReader r = new BufferedReader(new java.io.Reader() {
                private boolean given;

                @Override
                public int read(char[] b, int off, int len) {
                    if (given) {
                        return -1;
                    }
                    given = true;
                    b[off] = 'a';
                    b[off + 1] = '\r';
                    b[off + 2] = '\n';
                    return 3;
                }

                @Override
                public boolean ready() {
                    return false;
                }

                @Override
                public void close() {
                }
            });
            Bulk.p("line", r.readLine());
            Bulk.p("ready", r.ready());
        });
        Bulk.check("Io.aBufferedInputStreamsAvailableSaturates", () -> {
            BufferedInputStream in = new BufferedInputStream(new java.io.InputStream() {
                @Override
                public int read() {
                    return 'x';
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    b[off] = 'x';
                    b[off + 1] = 'y';
                    return 2;
                }

                @Override
                public int available() {
                    return Integer.MAX_VALUE;
                }
            }, 16);
            Bulk.p("first", in.read());
            Bulk.p("available", in.available());
        });
        Bulk.check("Io.readLineSplitsAsTheJdkDoes", () -> {
            for (int t = 0; t < TEXTS.length; t++) {
                for (int size : new int[] {1, 2, 3, 8192}) {
                    Bulk.p("text" + t + "/" + size, lines(new BufferedReader(new StringReader(TEXTS[t]), size)));
                }
            }
        });
        Bulk.check("Io.markResetAndSkipOnAReader", () -> {
            BufferedReader r = new BufferedReader(new StringReader("0123456789abcdef"), 4);
            Bulk.p("markSupported", r.markSupported());
            Bulk.p("read", r.read());
            r.mark(10);
            char[] a = new char[6];
            Bulk.p("readArray", r.read(a, 0, 6));
            Bulk.p("chars", a);
            r.reset();
            Bulk.p("skip3", r.skip(3));
            Bulk.p("read2", r.read());
            Bulk.p("skip100", r.skip(100));
            Bulk.p("readEnd", r.read());
            Bulk.p("ready", r.ready());
            r.close();
            Bulk.pThrown("readClosed", IOException.class, () -> r.read());
        });
        Bulk.check("Io.aBufferedWriterWritesWhatItWasGiven", () -> {
            StringWriter out = new StringWriter();
            BufferedWriter w = new BufferedWriter(out, 3);
            for (String s : new String[] {"ab", "", "cdefgh", "i"}) {
                w.write(s);
                w.write('-');
            }
            w.write("0123456789", 2, 5);
            w.newLine();
            w.flush();
            Bulk.p("written", out.toString().replace(System.lineSeparator(), "\n"));
            w.close();
            w.close();
            Bulk.pThrown("writeClosed", IOException.class, () -> w.write("x"));
        });
        Bulk.check("Io.aPrintWriterPrintsAsTheJdkDoes", () -> {
            StringWriter out = new StringWriter();
            PrintWriter w = new PrintWriter(out);
            w.print(1);
            w.print('c');
            w.print(2.5);
            w.print(true);
            w.print((Object) null);
            w.print((String) null);
            w.println("line");
            w.printf("%d-%s-%5.2f|", 42, "x", 3.14159);
            w.format("%08X", 255);
            w.append("abc", 1, 2).append('z').append(null);
            w.flush();
            Bulk.p("printed", out.toString().replace(System.lineSeparator(), "\n"));
            Bulk.p("checkError", w.checkError());
        });
        Bulk.check("Io.anUnknownCharsetNameIsRefusedAndAnAliasIsHonoured", () -> {
            // ParparVM's encoding natives took any name they did not know for
            // UTF-8, so all of these answered UTF-8 bytes instead of throwing.
            Bulk.pThrown("getBytes", UnsupportedEncodingException.class, () -> "x".getBytes("no-such-charset"));
            Bulk.pThrown("newString", UnsupportedEncodingException.class,
                    () -> new String(new byte[] {65}, "no-such-charset"));
            Bulk.pThrown("newStringRange", UnsupportedEncodingException.class,
                    () -> new String(new byte[] {65}, 0, 1, "no-such-charset"));
            Bulk.pThrown("writer", UnsupportedEncodingException.class,
                    () -> new OutputStreamWriter(new ByteArrayOutputStream(), "no-such-charset"));
            Bulk.pThrown("reader", UnsupportedEncodingException.class,
                    () -> new InputStreamReader(new ByteArrayInputStream(new byte[0]), "no-such-charset"));
            Bulk.pThrown("writerNull", NullPointerException.class,
                    () -> new OutputStreamWriter(new ByteArrayOutputStream(), (String) null));
            // An alias in another case names the same charset, and the bytes
            // and the decoded text are that charset's, not UTF-8's.
            Bulk.p("latin1Bytes", "caf\u00e9".getBytes("latin1"));
            Bulk.p("isoLowerBytes", "caf\u00e9".getBytes("iso-8859-1"));
            Bulk.p("utf8LowerBytes", "caf\u00e9".getBytes("utf-8"));
            Bulk.p("latin1String", new String(new byte[] {99, 97, 102, (byte) 0xe9}, "Latin1"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(out, "iso-8859-1");
            Bulk.p("writerEncoding", w.getEncoding());
            w.write("caf\u00e9");
            w.close();
            Bulk.p("writerBytes", out.toByteArray());
            InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(new byte[] {99, 97, 102, (byte) 0xe9}), "ISO8859_1");
            Bulk.p("readerEncoding", r.getEncoding());
            char[] buf = new char[8];
            int n = r.read(buf);
            Bulk.p("readerText", new String(buf, 0, Math.max(n, 0)));
        });
        Bulk.check("Io.aBufferedInputStreamReadsMarksAndSkipsAsTheJdkDoes", () -> {
            byte[] data = new byte[100];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i * 7);
            }
            for (int size : new int[] {1, 3, 16, 8192}) {
                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data), size);
                String m = "[" + size + "]";
                Bulk.p("read" + m, in.read());
                in.mark(20);
                byte[] a = new byte[10];
                Bulk.p("readArray" + m, in.read(a, 0, 10));
                Bulk.p("bytes" + m, a);
                in.reset();
                Bulk.p("skip" + m, in.skip(5));
                Bulk.p("read2" + m, in.read());
                Bulk.p("markSupported" + m, in.markSupported());
                byte[] rest = new byte[200];
                int total = 0;
                int n;
                while ((n = in.read(rest, total, rest.length - total)) > 0) {
                    total += n;
                }
                Bulk.p("restCount" + m, total);
                Bulk.p("rest" + m, Arrays.copyOf(rest, total));
                Bulk.p("readEnd" + m, in.read());
                in.close();
                Bulk.pThrown("readClosed" + m, IOException.class, () -> in.read());
            }
        });
        Bulk.check("Io.aBufferedOutputStreamWritesWhatItWasGiven", () -> {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(sink, 4);
            out.write(1);
            out.write(new byte[] {2, 3, 4, 5, 6, 7}, 1, 4);
            Bulk.p("atMostFiveBeforeFlush", sink.size() <= 5);
            out.write(new byte[10], 0, 10);
            out.flush();
            Bulk.p("size", sink.size());
            Bulk.p("first", Arrays.copyOf(sink.toByteArray(), 5));
            out.close();
        });
    }
}
