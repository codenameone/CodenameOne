/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codenameone.examples.hellocodenameone.tests.Cn1ssAsyncSink;

import java.util.ArrayList;
import java.util.List;

/// Drives the JavaScript port's screenshot sink state machine against a fake
/// socket. Run by cn1ss-async-sink-test.sh.
///
/// The first case is the CI flake this guards: the test server sheds the sink's
/// socket after five idle minutes (close 1006), and the next Java-side capture
/// (VideoIODecodedFrames) must redial and deliver instead of reporting
/// `websocket-unavailable`.
public final class Cn1ssAsyncSinkTest {
    private static int failures;

    /// A socket whose events the test fires by hand.
    static final class FakeTransport implements Cn1ssAsyncSink.Transport {
        boolean supported = true;
        boolean failSend;
        int lastGeneration;
        int connects;
        final List<String> sent = new ArrayList<String>();

        public boolean isSupported() {
            return supported;
        }

        public void connect(int generation) {
            connects++;
            lastGeneration = generation;
        }

        public void send(String meta, byte[] png) throws Exception {
            if (failSend) {
                throw new Exception("socket gone");
            }
            int start = meta.indexOf("\"test\":\"") + 8;
            sent.add(meta.substring(start, meta.indexOf('"', start)) + ":" + png.length);
        }
    }

    static final class Counter implements Runnable {
        int runs;

        public void run() {
            runs++;
        }
    }

    public static void main(String[] args) {
        idleDropRedialsOnNextSend();
        dropWhileAwaitingAckResendsOnce();
        resendIsBounded();
        unreachableServerGivesUpAfterBoundedDials();
        lateCloseFromReplacedSocketIsIgnored();
        unsupportedPlatformRefuses();
        sendFailureCompletesTheTest();
        if (failures > 0) {
            System.out.println("Cn1ssAsyncSinkTest: " + failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("Cn1ssAsyncSinkTest: all cases passed");
    }

    private static void idleDropRedialsOnNextSend() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter first = new Counter();
        check("first send owned", sink.send("LottieAnimated", new byte[3], "h", first));
        sink.onOpen(t.lastGeneration);
        sink.onText("ACK LottieAnimated");
        check("first completed by its ACK", first.runs == 1);

        // Minutes later the server sheds the idle socket.
        sink.onClosed(t.lastGeneration, "closed:1006");

        Counter second = new Counter();
        check("send after an idle drop is still owned",
                sink.send("VideoIODecodedFrames", new byte[5], "h", second));
        check("the drop was redialled", t.connects == 2);
        check("nothing completes before the ACK", second.runs == 0);
        sink.onOpen(t.lastGeneration);
        check("sent on the new socket", t.sent.contains("VideoIODecodedFrames:5"));
        sink.onText("ACK VideoIODecodedFrames png_bytes=5");
        check("second completed by its ACK", second.runs == 1);
    }

    private static void dropWhileAwaitingAckResendsOnce() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter done = new Counter();
        sink.send("A", new byte[2], "h", done);
        sink.onOpen(t.lastGeneration);
        sink.onClosed(t.lastGeneration, "closed:1006");
        check("an unacknowledged send redials at once", t.connects == 2);
        check("not completed by the drop", done.runs == 0);
        sink.onOpen(t.lastGeneration);
        check("resent on the new socket", t.sent.size() == 2 && "A:2".equals(t.sent.get(1)));
        sink.onText("ACK A");
        check("completed exactly once", done.runs == 1);
    }

    private static void resendIsBounded() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter done = new Counter();
        sink.send("A", new byte[2], "h", done);
        sink.onOpen(t.lastGeneration);
        sink.onClosed(t.lastGeneration, "closed:1006");
        sink.onOpen(t.lastGeneration);
        sink.onClosed(t.lastGeneration, "closed:1006");
        check("given up after its resend, and the suite advances", done.runs == 1);
        check("no third dial for a given-up send", t.connects == 2);
        Counter next = new Counter();
        check("the sink itself still works", sink.send("B", new byte[1], "h", next));
        sink.onOpen(t.lastGeneration);
        sink.onText("ACK B");
        check("next screenshot delivered", next.runs == 1);
    }

    private static void unreachableServerGivesUpAfterBoundedDials() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter done = new Counter();
        check("owned while dialling", sink.send("A", new byte[2], "h", done));
        for (int i = 0; i < 10 && done.runs == 0; i++) {
            sink.onClosed(t.lastGeneration, "closed:1006");
        }
        check("dials are bounded", t.connects == 3);
        check("the waiting test is released", done.runs == 1);
        check("later sends are refused", !sink.send("B", new byte[1], "h", new Counter()));
    }

    private static void lateCloseFromReplacedSocketIsIgnored() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter done = new Counter();
        sink.send("A", new byte[2], "h", done);
        int oldGen = t.lastGeneration;
        sink.onOpen(oldGen);
        // The browser reports error and then close for the same socket.
        sink.onClosed(oldGen, "error:x");
        sink.onClosed(oldGen, "closed:1006");
        check("one redial for one dead socket", t.connects == 2);
        sink.onOpen(t.lastGeneration);
        sink.onText("ACK A");
        check("delivered on the replacement", done.runs == 1);
    }

    private static void unsupportedPlatformRefuses() {
        FakeTransport t = new FakeTransport();
        t.supported = false;
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        Counter done = new Counter();
        check("refused without websockets", !sink.send("A", new byte[1], "h", done));
        check("caller keeps completion", done.runs == 0);
    }

    private static void sendFailureCompletesTheTest() {
        FakeTransport t = new FakeTransport();
        Cn1ssAsyncSink sink = new Cn1ssAsyncSink(t);
        sink.send("A", new byte[1], "h", null);
        sink.onOpen(t.lastGeneration);
        t.failSend = true;
        Counter done = new Counter();
        check("owned", sink.send("B", new byte[1], "h", done));
        check("a throwing send never stalls the suite", done.runs == 1);
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }
}
