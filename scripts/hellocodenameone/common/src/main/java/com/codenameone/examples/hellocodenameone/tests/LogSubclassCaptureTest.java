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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Regression coverage for codenameone/CodenameOne#5519.
 *
 * <p>{@code Log} is an extension point: an app subclasses it, overrides
 * {@code createWriter()} to hand back its own {@code Writer}, installs the
 * subclass, and reads the captured text back. That is the documented way to
 * turn a caught {@code Throwable} into a string, and it is how the reporter's
 * app got stack traces on every other port.</p>
 *
 * <p>It silently stopped working on the JavaScript port, because
 * {@code port.js} bound a console-printing stub over {@code Log.e(Throwable)}
 * -- and {@code bindNative} overwrites the translated method rather than
 * falling back to it. {@code Log.logThrowable} never ran, so
 * {@code createWriter()} was never called and the subclass read back a null
 * writer. Nothing failed loudly; the app just got a {@code NullPointerException}
 * from inside its own logging code.</p>
 *
 * <p>The assertions below are deliberately about behaviour a stub cannot fake:
 * that the port called back into the app's {@code createWriter()}, and that the
 * trace reached the writer it returned.</p>
 */
public class LogSubclassCaptureTest extends BaseTest {

    private static final String PROBE_MESSAGE = "cn1-log-subclass-capture-probe";

    /**
     * Mirrors the shape of the reproducer attached to the issue: capture the
     * log into a {@link StringWriter} for the duration of one {@code Log.e}.
     */
    private static final class CapturingLog extends Log {
        private StringWriter captured;
        private boolean createWriterCalled;

        @Override
        protected Writer createWriter() throws IOException {
            createWriterCalled = true;
            captured = new StringWriter();
            return captured;
        }
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        Log previousInstance = Log.getInstance();
        int previousLevel = Log.getLevel();
        try {
            CapturingLog capturing = new CapturingLog();
            Log.install(capturing);
            Log.e(new IllegalStateException(PROBE_MESSAGE));

            assertTrue(capturing.createWriterCalled,
                    "Log.e(Throwable) never reached Log.logThrowable: the installed Log "
                            + "subclass's createWriter() was not called (issue #5519)");
            assertTrue(capturing.captured != null,
                    "Log.createWriter() ran but left no writer behind");

            String text = capturing.captured.toString();
            assertTrue(text != null && text.length() > 0,
                    "Log.e(Throwable) wrote nothing into the writer the Log subclass returned");
            assertTrue(text.indexOf("IllegalStateException") >= 0,
                    "The captured log does not name the thrown class. Captured: " + text);
            assertTrue(text.indexOf(PROBE_MESSAGE) >= 0,
                    "The captured log does not carry the exception message. Captured: " + text);
        } catch (Throwable t) {
            fail("Log subclass capture test failed: " + t);
            return false;
        } finally {
            Log.install(previousInstance);
            Log.setLevel(previousLevel);
        }
        done();
        return true;
    }
}
