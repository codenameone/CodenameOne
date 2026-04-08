package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Table-driven syntax regression matrix for playground language support.
 */
public final class PlaygroundSyntaxMatrixHarness {
    private PlaygroundSyntaxMatrixHarness() {
    }

    private enum ExpectedOutcome {
        SUCCESS,
        PARSE_ERROR,
        EVAL_ERROR
    }

    private static final class Case {
        final String name;
        final String sourceSnippet;
        final ExpectedOutcome expectedOutcome;
        final String expectedDiagnosticSubstring;

        Case(String name, String sourceSnippet, ExpectedOutcome expectedOutcome, String expectedDiagnosticSubstring) {
            this.name = name;
            this.sourceSnippet = sourceSnippet;
            this.expectedOutcome = expectedOutcome;
            this.expectedDiagnosticSubstring = expectedDiagnosticSubstring;
        }
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            List<Case> cases = new ArrayList<Case>();

            // Control cases (known good behavior).
            cases.add(new Case("control_lambda_listener", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    Button b = new Button("Go");
                    b.addActionListener(e -> {});
                    root.add(b);
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("control_anonymous_listener", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.events.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    Button b = new Button("Go");
                    b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {} });
                    root.add(b);
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("control_classic_for_loop", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    int sum = 0;
                    for (int i = 0; i < 3; i++) {
                        sum += i;
                    }
                    root.add(new Label("sum=" + sum));
                    root;
                    """, ExpectedOutcome.SUCCESS, null));

            // Method references.
            cases.add(new Case("method_reference_type", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    Button b = new Button("Go");
                    b.addActionListener(System.out::println);
                    root.add(b);
                    root;
                    """, ExpectedOutcome.PARSE_ERROR, "Parse error:"));
            cases.add(new Case("method_reference_instance", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    String prefix = "X:";
                    Button b = new Button("Go");
                    b.addActionListener(prefix::concat);
                    root.add(b);
                    root;
                    """, ExpectedOutcome.PARSE_ERROR, "Parse error:"));
            cases.add(new Case("method_reference_constructor", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    import java.util.function.*;
                    Container root = new Container(BoxLayout.y());
                    Supplier<StringBuilder> ctor = StringBuilder::new;
                    root.add(new Label(ctor.get().toString()));
                    root;
                    """, ExpectedOutcome.PARSE_ERROR, "Parse error:"));

            // Try-with-resources variants.
            cases.add(new Case("twr_single_resource", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    class Res implements AutoCloseable {
                        public void close() {}
                    }
                    Container root = new Container(BoxLayout.y());
                    try (Res in = new Res()) {
                        root.add(new Label("ok"));
                    }
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("twr_multiple_resources", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    class Res implements AutoCloseable {
                        public void close() {}
                    }
                    Container root = new Container(BoxLayout.y());
                    try (Res in = new Res(); Res out = new Res()) {
                        root.add(new Label("ok"));
                    }
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("twr_trailing_semicolon", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    class Res implements AutoCloseable {
                        public void close() {}
                    }
                    Container root = new Container(BoxLayout.y());
                    try (Res in = new Res();) {
                        root.add(new Label("ok"));
                    }
                    root;
                    """, ExpectedOutcome.PARSE_ERROR, "Parse error:"));
            cases.add(new Case("twr_nested_try_catch_finally", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    class Res implements AutoCloseable {
                        public void close() {}
                    }
                    Container root = new Container(BoxLayout.y());
                    try (Res in = new Res()) {
                        try {
                            root.add(new Label("inner"));
                        } catch (RuntimeException ex) {
                            root.add(new Label("catch"));
                        } finally {
                            root.add(new Label("finally"));
                        }
                    }
                    root;
                    """, ExpectedOutcome.SUCCESS, null));

            // Enhanced-for arrays.
            cases.add(new Case("enhanced_for_primitive_array", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    int sum = 0;
                    for (int v : new int[]{1,2,3}) {
                        sum += v;
                    }
                    root.add(new Label("sum=" + sum));
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("enhanced_for_object_array", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    String txt = "";
                    for (String v : new String[]{"a","b"}) {
                        txt += v;
                    }
                    root.add(new Label(txt));
                    root;
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("enhanced_for_null_array", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    int[] values = null;
                    for (int v : values) {
                        root.add(new Label("v=" + v));
                    }
                    root;
                    """, ExpectedOutcome.EVAL_ERROR, "Evaluation error:"));
            cases.add(new Case("enhanced_for_nested", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    int count = 0;
                    for (int row : new int[]{1,2}) {
                        for (int col : new int[]{3,4}) {
                            count += row + col;
                        }
                    }
                    root.add(new Label("count=" + count));
                    root;
                    """, ExpectedOutcome.SUCCESS, null));

            // Multiple classes / inner class variants.
            cases.add(new Case("multiple_top_level_classes", """
                    class A {}
                    class B {}
                    new A();
                    """, ExpectedOutcome.PARSE_ERROR, "Parse error:"));
            cases.add(new Case("inner_class_static_member", """
                    class Outer {
                        static class Inner {
                            String label() { return "ok"; }
                        }
                    }
                    new Outer.Inner().label();
                    """, ExpectedOutcome.SUCCESS, null));
            cases.add(new Case("inner_class_anonymous", """
                    import com.codename1.ui.*;
                    import com.codename1.ui.events.*;
                    import com.codename1.ui.layouts.*;
                    Container root = new Container(BoxLayout.y());
                    Button b = new Button("Go");
                    b.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {}
                    });
                    root.add(b);
                    root;
                    """, ExpectedOutcome.SUCCESS, null));

            int passed = 0;
            for (Case testCase : cases) {
                PlaygroundRunner.RunResult result = runSnippet(testCase.sourceSnippet);
                ExpectedOutcome actual = classify(result);
                if (actual != testCase.expectedOutcome) {
                    throw new IllegalStateException("Case failed: " + testCase.name
                            + " expected=" + testCase.expectedOutcome
                            + " actual=" + actual
                            + " messages=" + summarizeMessages(result));
                }
                if (testCase.expectedDiagnosticSubstring != null) {
                    String message = firstDiagnosticMessage(result);
                    if (message == null || message.indexOf(testCase.expectedDiagnosticSubstring) < 0) {
                        throw new IllegalStateException("Case failed: " + testCase.name
                                + " expected diagnostic containing='" + testCase.expectedDiagnosticSubstring
                                + "' actual='" + message + "'");
                    }
                }
                passed++;
            }

            System.out.println("Playground syntax matrix passed (" + passed + "/" + cases.size() + ").");
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    private static ExpectedOutcome classify(PlaygroundRunner.RunResult result) {
        if (result.getComponent() != null) {
            return ExpectedOutcome.SUCCESS;
        }
        String message = firstDiagnosticMessage(result);
        if (message != null && message.startsWith("Parse error:")) {
            return ExpectedOutcome.PARSE_ERROR;
        }
        return ExpectedOutcome.EVAL_ERROR;
    }

    private static String firstDiagnosticMessage(PlaygroundRunner.RunResult result) {
        List<PlaygroundRunner.Diagnostic> diagnostics = result.getDiagnostics();
        if (!diagnostics.isEmpty()) {
            return diagnostics.get(0).message;
        }
        List<PlaygroundRunner.InlineMessage> messages = result.getMessages();
        if (!messages.isEmpty()) {
            return messages.get(0).text;
        }
        return null;
    }

    private static PlaygroundRunner.RunResult runSnippet(String script) {
        Display.init(null);
        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        PlaygroundContext context = new PlaygroundContext(host, preview, null, new PlaygroundContext.Logger() {
            public void log(String message) {
            }
        });
        PlaygroundRunner runner = new PlaygroundRunner();
        return runner.run(script, context);
    }

    private static String summarizeMessages(PlaygroundRunner.RunResult result) {
        List<PlaygroundRunner.InlineMessage> messages = result.getMessages();
        if (messages.isEmpty()) {
            return "<no messages>";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                out.append(" | ");
            }
            out.append(messages.get(i).text);
        }
        return out.toString();
    }
}
