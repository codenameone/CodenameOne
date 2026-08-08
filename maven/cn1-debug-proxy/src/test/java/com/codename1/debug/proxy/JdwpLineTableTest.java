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
package com.codename1.debug.proxy;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Every frame the IDE is shown carries a location it can actually resolve.
 *
 * <p>A debugger resolves a frame strictly against the line table it was handed
 * for that frame's method, and reacts to a code index the table does not
 * describe by throwing rather than by degrading. jdb raises
 * {@code InternalError: Location with invalid code index} out of {@code where}
 * and the session ends there — so one frame in a method compiled without line
 * information takes down the whole stack view, not just its own row.</p>
 *
 * <p>Found with jdb attached to an app in the simulator: a breakpoint in
 * {@code Form.show()} stopped and printed three frames before {@code where}
 * threw on the fourth.</p>
 */
public class JdwpLineTableTest {

    /**
     * Method 0 has lines, method 1 has none — the shape a method the
     * translator emitted without line information takes in the symbol stream.
     */
    private static final String TABLE =
            "version\t1\n"
          + "class\t0\tcom_example_Main\tMain.java\t-1\tcom/example/Main\n"
          + "method\t0\t0\thandler\t(Ljava/lang/Object;)V\t0\n"
          + "line\t0\t100\n"
          + "line\t0\t110\n"
          + "line\t0\t120\n"
          + "method\t1\t0\tsynthetic\t()V\t0\n";

    private static final long TID = 3;

    /** A method with lines reports them, ordered, as both index and line. */
    @Test
    public void aMethodWithLinesReportsItsTable() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            JdwpTestClient.Reply reply = lineTable(client, 0);
            assertEquals(0, reply.errorCode);

            DataInputStream body = reply.stream();
            assertEquals("lowest code index", 100L, body.readLong());
            assertEquals("highest code index", 120L, body.readLong());
            assertEquals(3, body.readInt());
            long[] expected = { 100, 110, 120 };
            for (long line : expected) {
                assertEquals(line, body.readLong());
                assertEquals(line, body.readInt());
            }
        }
    }

    /**
     * A method with no lines still gets a usable table: one entry at code
     * index 0 whose line number is "unknown".
     *
     * <p>The two answers that look right both end the session. An empty table
     * is taken as authoritative and no location in the method resolves;
     * ABSENT_INFORMATION is what the spec reserves for this, but jdb has no
     * case for that error on this path and raises {@code InternalException}
     * just the same. A one-entry table is the only shape that lets the frame
     * print and the rest of the stack survive.</p>
     */
    @Test
    public void aMethodWithoutLinesStillGetsATableTheDebuggerCanResolve() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            JdwpTestClient.Reply reply = lineTable(client, 1);
            assertEquals(0, reply.errorCode);

            DataInputStream body = reply.stream();
            assertEquals(0L, body.readLong());
            assertEquals(0L, body.readLong());
            assertEquals("never an empty table", 1, body.readInt());
            assertEquals("covers the index frames report", 0L, body.readLong());
            assertEquals("line unknown", -1, body.readInt());
        }
    }

    /** A method the symbol table never described answers the same way. */
    @Test
    public void anUnknownMethodGetsTheSameUnknownLineTable() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            JdwpTestClient.Reply reply = lineTable(client, 9999);
            assertEquals(0, reply.errorCode);
            DataInputStream body = reply.stream();
            body.readLong(); body.readLong();
            assertEquals(1, body.readInt());
        }
    }

    /**
     * A frame stopped between two tracked lines is reported at the later of
     * the two — the last line known to have started, which is where execution
     * actually is.
     */
    @Test
    public void aFrameBetweenTrackedLinesSnapsBackToTheLastOneThatStarted() throws Exception {
        assertEquals(110L, frameCodeIndexFor(0, 115));
    }

    /** A line the table does have travels unchanged. */
    @Test
    public void aFrameOnATrackedLineIsReportedAsItself() throws Exception {
        assertEquals(110L, frameCodeIndexFor(0, 110));
    }

    /**
     * A frame that has not reached a tracked line yet — the device reports 0 —
     * shows the method's first line rather than an index the table cannot
     * resolve. Naming the wrong line in the right method beats ending the
     * stack view.
     */
    @Test
    public void aFrameWithNoLineYetIsReportedAtTheMethodsFirstLine() throws Exception {
        assertEquals(100L, frameCodeIndexFor(0, 0));
    }

    /**
     * A frame past the last tracked line snaps back to it rather than running
     * off the end of the table.
     */
    @Test
    public void aFramePastTheLastTrackedLineSnapsBackToIt() throws Exception {
        assertEquals(120L, frameCodeIndexFor(0, 500));
    }

    /**
     * A frame in a method with no lines is reported at code index 0, the one
     * index that method's table describes. Passing the device's line through
     * would name an index its table does not cover, which is the same
     * unresolvable location by another route.
     */
    @Test
    public void aFrameInAMethodWithoutLinesIsReportedAtTheIndexItsTableCovers() throws Exception {
        assertEquals(0L, frameCodeIndexFor(1, 42));
    }

    /**
     * The whole point, stated as the debugger sees it: every frame's code
     * index either resolves against that frame's own table or belongs to a
     * method that admits it has none.
     */
    @Test
    public void everyReportedFrameResolvesAgainstItsOwnLineTable() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            // A stack shaped like the one that broke: a tracked frame, a frame
            // at an untracked line, and a frame in a method with no lines.
            server.onStack(TID, new int[] { 0, 0, 1 }, new int[] { 110, 115, 42 });

            for (Frame frame : frames(client)) {
                JdwpTestClient.Reply table = lineTable(client, (int) frame.methodId - 1);
                assertEquals(0, table.errorCode);
                assertTrue("frame at code index " + frame.codeIndex
                                + " should be covered by its own method's table",
                        indicesOf(table).contains(frame.codeIndex));
            }
        }
    }

    /**
     * An array reference type's signature starts with {@code [}.
     *
     * <p>The device reports an array by naming its component class and setting
     * a flag, so the array and its component shared one reference-type ID and
     * the array answered with the component's own signature. A debugger reads
     * the component type back out by removing the leading character, so the
     * missing {@code [} is not merely a wrong label: jdb strips the first
     * character regardless and parses the rest, which is how printing an array
     * local ended in {@code Invalid JNI signature character 'j'} — the 'j' of
     * "java/lang/..." after the 'L' had been eaten.</p>
     */
    @Test
    public void anArrayTypeReportsAnArraySignature() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            String plain = signatureOf(client, refTypeOf(0, false));
            String array = signatureOf(client, refTypeOf(0, true));

            assertEquals("Lcom/example/Main;", plain);
            assertEquals("[Lcom/example/Main;", array);
            assertEquals("the component type is what remains after the '['",
                    plain, array.substring(1));
        }
    }

    /** Reference-type id for a class, as an array type or as itself. */
    private long refTypeOf(int classId, boolean array) {
        long ref = classId + 1L;
        return array ? (0x2000000000000000L | ref) : ref;
    }

    private String signatureOf(JdwpTestClient client, long refType) throws Exception {
        byte[] payload = new byte[8];
        for (int i = 0; i < 8; i++) {
            payload[7 - i] = (byte) (refType >>> (8 * i));
        }
        JdwpTestClient.Reply reply = client.send(2 /* ReferenceType */, 1 /* Signature */, payload);
        assertEquals(0, reply.errorCode);
        DataInputStream body = reply.stream();
        byte[] utf8 = new byte[body.readInt()];
        body.readFully(utf8);
        return new String(utf8, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- helpers -----------------------------------------------------------

    /** Runs Method.LineTable for a method and returns the raw reply. */
    private JdwpTestClient.Reply lineTable(JdwpTestClient client, int methodId) throws Exception {
        byte[] payload = new byte[16];
        payload[7] = 1;  // refType id 1 -> classId 0
        long ref = methodId + 1L;
        for (int i = 0; i < 8; i++) {
            payload[15 - i] = (byte) (ref >>> (8 * i));
        }
        return client.send(6 /* Method */, 1 /* LineTable */, payload);
    }

    /** The code index Frames reports for a frame the device places at {@code line}. */
    private long frameCodeIndexFor(int methodId, int line) throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);
            server.onStack(TID, new int[] { methodId }, new int[] { line });

            List<Frame> frames = frames(client);
            assertEquals(1, frames.size());
            return frames.get(0).codeIndex;
        }
    }

    /** Runs ThreadReference.Frames over the whole stack. */
    private List<Frame> frames(JdwpTestClient client) throws Exception {
        byte[] payload = new byte[16];
        for (int i = 0; i < 8; i++) {
            payload[7 - i] = (byte) (TID >>> (8 * i));
        }
        payload[11] = 0;            // startFrame 0
        payload[12] = (byte) 0xFF;  // length -1: all of them
        payload[13] = (byte) 0xFF;
        payload[14] = (byte) 0xFF;
        payload[15] = (byte) 0xFF;

        JdwpTestClient.Reply reply = client.send(JdwpTestClient.CS_THREAD_REFERENCE, 6, payload);
        assertEquals(0, reply.errorCode);

        DataInputStream body = reply.stream();
        int count = body.readInt();
        List<Frame> frames = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            body.readLong();  // frameId
            body.readByte();  // typeTag
            body.readLong();  // classId
            long methodId = body.readLong();
            frames.add(new Frame(methodId, body.readLong()));
        }
        return frames;
    }

    /** The code indices a line table describes. */
    private List<Long> indicesOf(JdwpTestClient.Reply table) throws Exception {
        DataInputStream body = table.stream();
        body.readLong();  // start
        body.readLong();  // end
        int count = body.readInt();
        List<Long> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(body.readLong());
            body.readInt();  // line number
        }
        return indices;
    }

    private void primeSymbols(JdwpServer server) throws Exception {
        server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                TABLE.getBytes(StandardCharsets.UTF_8))));
        server.onHello(1);
    }

    private static final class Frame {
        final long methodId;
        final long codeIndex;

        Frame(long methodId, long codeIndex) {
            this.methodId = methodId;
            this.codeIndex = codeIndex;
        }
    }
}
