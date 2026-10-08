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

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Prints cold-start timings for a few well-defined phases so the
 * playground's startup cost has a traceable baseline. Each phase is
 * measured with {@link System#nanoTime} from a fresh JVM, so running
 * this class twice measures the class-loading cost of the two
 * subsequent runs — for steady-state comparisons use the averages
 * over multiple invocations (run the maven harness a few times).
 *
 * <p>Phases:
 * <ol>
 *   <li>API stub library load (the classes user code compiles against).</li>
 *   <li>First completion lookup over the stubs.</li>
 *   <li>First compile of a snippet (no run).</li>
 *   <li>CN1 Display init + Form/Container allocation — the simulator
 *       wiring the matrix harness uses.</li>
 *   <li>Full snippet round-trip via PlaygroundRunner — representative
 *       user-visible cold start.</li>
 * </ol>
 */
public final class PlaygroundColdStartHarness {
    public static void main(String[] args) throws Exception {
        long t0 = nanoTime();
        int classes = PlaygroundApi.library().size();
        long t1 = nanoTime();
        report("1. API stub library load (" + classes + " classes)", t0, t1);

        long t2 = nanoTime();
        String[] names = PlaygroundApi.fieldNames("com.codename1.ui.Display");
        long t3 = nanoTime();
        report("2. first completion lookup (Display fields)", t2, t3);

        long t4 = nanoTime();
        PlaygroundRunner.compile("Label l = new Label(\"x\");\nl;\n");
        long t5 = nanoTime();
        report("3. first compile (no run)", t4, t5);

        long t6 = nanoTime();
        Display.init(null);
        HarnessSupport.install();
        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();
        long t7 = nanoTime();
        report("4. Display.init + Form/Container/show", t6, t7);

        long t8 = nanoTime();
        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() { public void log(String message) {} });
        PlaygroundRunner runner = new PlaygroundRunner();
        String script = ""
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "root.add(new Label(\"hello\"));\n"
                + "root;\n";
        PlaygroundRunner.RunResult result = runner.run(script, context);
        long t9 = nanoTime();
        report("5. first PlaygroundRunner.run (full round-trip)", t8, t9);
        if (result.getComponent() == null) {
            throw new IllegalStateException("Snippet did not return a component");
        }

        long t10 = nanoTime();
        runner.run(script, context);
        long t11 = nanoTime();
        report("6. warm PlaygroundRunner.run (second invocation)", t10, t11);

        if (names == null) throw new IllegalStateException("Display has no field names?");
        // CN1's simulator keeps the EDT alive for the shown Form — exit
        // explicitly so the harness terminates after reporting.
        System.exit(0);
    }

    private static void report(String label, long startNanos, long endNanos) {
        double ms = (endNanos - startNanos) / 1_000_000.0;
        System.out.printf("%-50s %8.2f ms%n", label, ms);
    }

    private static long nanoTime() {
        return System.nanoTime();
    }
}
