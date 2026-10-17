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
package com.codename1.impl.process;

import com.codename1.impl.ImplementationFactory;
import com.codename1.io.Process;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeExecTest {
    private final ImplementationFactory previous = ImplementationFactory.getInstance();
    private String[] command;
    private Map<String, String> environment;
    private final Map<String, String> inherited = new HashMap<String, String>();

    @BeforeEach void install() {
        inherited.put("PATH", "/bin");
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createProcessSupport() {
                return new ProcessSupport() {
                    public Map<String, String> environment() {
                        return new HashMap<String, String>(inherited);
                    }
                    public Process start(String[] args, String dir, Map<String, String> env, boolean merge) {
                        command = args;
                        environment = env;
                        return new Fake();
                    }
                };
            }
        });
    }

    @AfterEach void restore() {
        ImplementationFactory.setInstance(previous);
    }

    @Test void aCommandStringSplitsAtWhitespaceWithNoQuoting() throws IOException {
        RuntimeExec.exec(Runtime.getRuntime(), "  ls\t-l \"a b\"\r\n\fx ");
        assertEquals(Arrays.asList("ls", "-l", "\"a", "b\"", "x"), Arrays.asList(command));
        assertNull(environment, "no envp inherits the environment");
    }

    @Test void anEmptyCommandFailsAsJavaSEDoes() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeExec.exec(Runtime.getRuntime(), ""));
        assertThrows(IndexOutOfBoundsException.class, () -> RuntimeExec.exec(Runtime.getRuntime(), " \t "));
        assertThrows(IndexOutOfBoundsException.class, () -> RuntimeExec.exec(Runtime.getRuntime(), new String[0]));
        assertThrows(NullPointerException.class, () -> RuntimeExec.exec(Runtime.getRuntime(), (String) null));
        assertThrows(NullPointerException.class, () -> RuntimeExec.exec(null, "ls"));
    }

    @Test void anArrayIsPassedThroughWordForWord() throws IOException {
        RuntimeExec.exec(Runtime.getRuntime(), new String[] {"my tool", " a "});
        assertEquals(Arrays.asList("my tool", " a "), Arrays.asList(command));
    }

    @Test void envpReplacesTheInheritedEnvironment() throws IOException {
        RuntimeExec.exec(Runtime.getRuntime(), "tool",
                new String[] {"A=1", "B==2", "NOEQUALS", "=hidden", "C=3\0junk"});
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("A", "1");
        expected.put("B", "=2");
        expected.put("C", "3");
        assertEquals(expected, environment);
        RuntimeExec.exec(Runtime.getRuntime(), new String[] {"tool"}, new String[0]);
        assertEquals(new HashMap<String, String>(), environment);
    }

    @Test void destroyForciblyFallsBackToDestroyAndPidIsUnsupported() {
        Fake fake = new Fake();
        assertSame(fake, fake.destroyForcibly());
        assertEquals(1, fake.destroyed);
        assertThrows(UnsupportedOperationException.class, fake::pid);
    }

    /// Implements exactly what is abstract in `java.lang.Process`, as a
    /// relocated JavaSE subclass would.
    private static final class Fake extends Process {
        int destroyed;
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }
        public int waitFor() {
            return 0;
        }
        public int exitValue() {
            return 0;
        }
        public void destroy() {
            destroyed++;
        }
    }
}
