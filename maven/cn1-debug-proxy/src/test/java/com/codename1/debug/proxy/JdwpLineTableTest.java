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
     * A method with no lines answers ABSENT_INFORMATION rather than an empty
     * table. The empty table is the crash: the debugger takes it as
     * authoritative and every location in the method becomes unresolvable.
     */
    @Test
    public void aMethodWithoutLinesReportsAbsentInformationRatherThanAnEmptyTable() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            JdwpTestClient.Reply reply = lineTable(client, 1);
            assertEquals("ABSENT_INFORMATION", 101, reply.errorCode);
            assertEquals("an error reply carries no table", 0, reply.body.length);
        }
    }

    /** A method the symbol table never described answers the same way. */
    @Test
    public void anUnknownMethodReportsAbsentInformation() throws Exception {
        int port = JdwpTestClient.freePort();
        JdwpServer server = new JdwpServer(port);
        try (JdwpTestClient client = JdwpTestClient.attach(server, port)) {
            primeSymbols(server);

            assertEquals(101, lineTable(client, 9999).errorCode);
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
     * For a method with no table at all there is nothing to be consistent
     * with, so the device's line passes through; LineTable reports the absence
     * and the debugger stops consulting the index.
     */
    @Test
    public void aFrameInAMethodWithoutATablePassesItsLineThrough() throws Exception {
        assertEquals(42L, frameCodeIndexFor(1, 42));
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
                if (table.errorCode == 101) {
                    continue;  // admits it has no lines; index is never consulted
                }
                assertEquals(0, table.errorCode);
                assertTrue("frame at " + frame.codeIndex + " should be in its method's table",
                        linesOf(table).contains(frame.codeIndex));
            }
        }
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

    private List<Long> linesOf(JdwpTestClient.Reply table) throws Exception {
        DataInputStream body = table.stream();
        body.readLong();  // start
        body.readLong();  // end
        int count = body.readInt();
        List<Long> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(body.readLong());
            body.readInt();
        }
        return lines;
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
