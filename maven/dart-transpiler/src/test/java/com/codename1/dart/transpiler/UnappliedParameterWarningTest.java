/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A stub's {@code @Unapplied} parameters are accepted so the program compiles and then
 * do nothing; a call that passes one is reported as W0002 at that argument's line, and a
 * call that does not pass one is not.
 */
public class UnappliedParameterWarningTest {

    private static final String STUB =
            "@JavaName('com.example.Thumb')\n"
          + "class Thumb {\n"
          + "  @Unapplied('memCacheWidth')\n"
          + "  external Thumb({String? url, int? memCacheWidth});\n"
          + "  @Unapplied('onSendProgress')\n"
          + "  external void send(String path, {Object? onSendProgress, int? retries});\n"
          + "}\n";

    private static List<Diagnostic> w0002(String src) {
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.<java.io.File>emptyList(),
                Collections.singletonList(new String[] {"thumb.stub.dart", STUB}));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        List<Diagnostic> out = new ArrayList<Diagnostic>();
        for (Diagnostic d : r.diags.asList()) {
            if (d.code.equals("W0002")) {
                out.add(d);
            }
        }
        return out;
    }

    @Test
    public void passingAnUnappliedArgumentIsReportedAtItsLine() {
        List<Diagnostic> w = w0002("void main() {\n"
                + "  final t = Thumb(url: 'u',\n"
                + "      memCacheWidth: 64);\n"
                + "  t.send('p', onSendProgress: (a, b) {});\n"
                + "}\n");
        assertEquals(2, w.size(), String.valueOf(w));
        assertEquals(3, w.get(0).line);
        assertEquals(4, w.get(1).line);
        assertEquals("'memCacheWidth' of Thumb is accepted but not applied on Codename One", w.get(0).message);
    }

    @Test
    public void appliedArgumentsAreNotReported() {
        assertEquals(0, w0002("void main() {\n"
                + "  Thumb(url: 'u').send('p', retries: 2);\n"
                + "}\n").size());
    }
}
