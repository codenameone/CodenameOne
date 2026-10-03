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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoke test: every curated sample in {@link PlaygroundExamples} must compile
 * against the Playground's API stubs, and the listed ones must run and produce a
 * preview component without an error diagnostic.
 *
 * <p>The device-API demos added for the JavaScript port (clipboard / native
 * share / fullscreen / printing / camera) therefore resolve to real public APIs
 * of the VM the Playground ships. The GPU and physics samples are compiled but
 * not run: they need a live render surface / animation loop that the headless
 * test {@link Display} cannot provide.
 */
public final class PlaygroundSamplesHarness {
    private PlaygroundSamplesHarness() {
    }

    private static final String[] SLUGS = {
        "welcome",
        "ui-showcase",
        "hello-world",
        "lifecycle-demo",
        "date-picker",
        "menu-list",
        "profile-form",
        "tabs",
        "network-fetch",
        "rest-request",
        "camera-capture",
        "clipboard",
        "native-share",
        "fullscreen",
        "printing"
    };

    public static void main(String[] args) {
        List<String> failures = new ArrayList<String>();
        int passed = 0;
        Display.init(null);
        HarnessSupport.install();
        for (int i = 0; i < PlaygroundExamples.SAMPLES.length; i++) {
            PlaygroundExamples.Sample sample = PlaygroundExamples.SAMPLES[i];
            try {
                PlaygroundRunner.compile(sample.script);
            } catch (PlaygroundRunner.CompileFailure f) {
                StringBuilder b = new StringBuilder();
                for (PlaygroundRunner.Diagnostic d : f.diagnostics) {
                    b.append(" [").append(d.line).append(':').append(d.column).append("] ").append(d.message);
                }
                failures.add(sample.title + ": does not compile:" + b);
            } catch (Exception e) {
                failures.add(sample.title + ": compiler failed: " + e);
            }
        }
        for (int i = 0; i < SLUGS.length; i++) {
            String slug = SLUGS[i];
            PlaygroundExamples.Sample sample = PlaygroundExamples.findBySlug(slug);
            if (sample == null) {
                failures.add(slug + ": sample not found");
                continue;
            }
            String error = evaluate(sample.script);
            if (error != null) {
                failures.add(sample.title + " (" + slug + "): " + error);
            } else {
                passed++;
                System.out.println("PASS: " + sample.title);
            }
        }

        if (!failures.isEmpty()) {
            System.out.println();
            System.out.println("FAILURES (" + failures.size() + "):");
            for (int i = 0; i < failures.size(); i++) {
                System.out.println("  - " + failures.get(i));
            }
            System.out.flush();
            // Display.init() starts the non-daemon EDT, which keeps the JVM
            // alive after main() returns; exit explicitly like the sibling
            // harnesses so the build doesn't hang.
            System.exit(1);
        }
        System.out.println();
        System.out.println("All " + passed + " sample scripts evaluated cleanly.");
        System.out.flush();
        System.exit(0);
    }

    private static String evaluate(String script) {
        Display.init(null);
        HarnessSupport.install();
        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                    }
                });

        PlaygroundRunner.RunResult result;
        try {
            result = new PlaygroundRunner().run(script, context);
        } catch (Throwable t) {
            return "threw " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }

        List<PlaygroundRunner.Diagnostic> diagnostics = result.getDiagnostics();
        for (int i = 0; i < diagnostics.size(); i++) {
            if ("error".equalsIgnoreCase(diagnostics.get(i).severity)) {
                return diagnostics.get(i).message;
            }
        }
        if (result.getComponent() == null) {
            return "no preview component produced";
        }
        return null;
    }
}
