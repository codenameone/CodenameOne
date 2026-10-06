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
package com.codenameone.playground;

import com.codename1.tools.javac.StubLibrary;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared set-up for the JVM harnesses: compiled Playground code is defined by a
 * class loader (what the javase module's PlaygroundLoaderNativeImpl does in the
 * simulator), plus a host form and a context that records what scripts log.
 */
final class HarnessSupport {
    private HarnessSupport() {
    }

    /** Registers a JVM class definer; idempotent. */
    static void install() {
        if (PlaygroundClassDefiner.Registry.get() == null) {
            PlaygroundClassDefiner.Registry.register(new PlaygroundClassDefiner() {
                public Object defineAndInstantiate(byte[] classes, String mainClass) throws Exception {
                    final StubLibrary pack = new StubLibrary(classes);
                    ClassLoader loader = new ClassLoader(HarnessSupport.class.getClassLoader()) {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                            byte[] b = pack.classBytes(name.replace('.', '/'));
                            if (b == null) {
                                throw new ClassNotFoundException(name);
                            }
                            return defineClass(name, b, 0, b.length);
                        }
                    };
                    return loader.loadClass(mainClass).getDeclaredConstructor().newInstance();
                }
            });
        }
    }

    /** A shown host form with a preview container, and a context logging into {@code log}. */
    static PlaygroundContext context(final List<String> log) {
        Display.init(null);
        install();
        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();
        return new PlaygroundContext(host, preview, null, new PlaygroundContext.Logger() {
            public void log(String message) {
                if (log != null) {
                    log.add(message);
                }
            }
        });
    }

    static PlaygroundContext context() {
        return context(new ArrayList<String>());
    }

    static String summarize(PlaygroundRunner.RunResult result) {
        StringBuilder b = new StringBuilder();
        for (PlaygroundRunner.Diagnostic d : result.getDiagnostics()) {
            b.append("[").append(d.line).append(':').append(d.column).append("] ").append(d.message).append("; ");
        }
        for (PlaygroundRunner.InlineMessage m : result.getMessages()) {
            b.append(m.kind).append(": ").append(m.text).append("; ");
        }
        return b.length() == 0 ? "<no messages>" : b.toString();
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
