package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoke test: every curated sample script in {@link PlaygroundExamples} must
 * evaluate on the Codename One BeanShell runtime and produce a preview
 * component without an error diagnostic.
 *
 * <p>This guards two things at once:
 * <ul>
 *   <li>Listener wiring uses the supported lambda form — anonymous interface
 *       implementations ({@code new ActionListener() { ... }}) are rejected by
 *       the CN1 BeanShell runtime, so a sample that uses them would fail here.</li>
 *   <li>The device-API demos added for the JavaScript port
 *       (clipboard / native share / fullscreen / printing / camera) resolve to
 *       real public APIs in the generated access registry.</li>
 * </ul>
 *
 * <p>The GPU and physics samples are intentionally excluded: they need a live
 * render surface / animation loop that the headless test {@link Display} cannot
 * provide.
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
