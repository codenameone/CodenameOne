package com.codenameone.playground;

import bsh.cn1.CN1AccessRegistry;
import bsh.cn1.GeneratedCN1Access;
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
 *   <li>Registry first use — triggers GeneratedCN1Access
 *       {@code <clinit>} and the CLASS_INDEX holder build.</li>
 *   <li>First ui-package static field — lazy-loads
 *       {@code GeneratedAccess_com_codename1_ui}.</li>
 *   <li>First diagnostic — forces FIELD_INDEX holder to init.</li>
 *   <li>CN1 Display init + Form/Container allocation — the simulator
 *       wiring the matrix harness uses.</li>
 *   <li>Full snippet round-trip via PlaygroundRunner — representative
 *       user-visible cold start.</li>
 * </ol>
 */
public final class PlaygroundColdStartHarness {
    public static void main(String[] args) throws Exception {
        long t0 = nanoTime();
        Class<?> displayClass = CN1AccessRegistry.getInstance().findClass("com.codename1.ui.Display");
        long t1 = nanoTime();
        report("1. registry.findClass Display (CLASS_INDEX)", t0, t1);

        long t2 = nanoTime();
        Object val = CN1AccessRegistry.getInstance()
                .getStaticField(com.codename1.ui.Display.class, "PICKER_TYPE_DATE");
        long t3 = nanoTime();
        report("2. first ui-package getStaticField", t2, t3);
        if (val == null) throw new IllegalStateException("PICKER_TYPE_DATE resolved to null");

        long t4 = nanoTime();
        String[] names = ((GeneratedCN1Access) CN1AccessRegistry.getInstance())
                .getFieldNames("com.codename1.ui.Display");
        long t5 = nanoTime();
        report("3. first getFieldNames (FIELD_INDEX holder)", t4, t5);

        long t6 = nanoTime();
        Display.init(null);
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
