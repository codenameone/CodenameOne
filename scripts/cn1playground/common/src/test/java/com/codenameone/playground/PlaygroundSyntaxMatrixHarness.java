package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Syntax regression matrix for Playground language support.
 *
 * <p>When adding new syntax support, update the expected outcome for the
 * relevant test case from FAILURE to SUCCESS.</p>
 */
public final class PlaygroundSyntaxMatrixHarness {
    private PlaygroundSyntaxMatrixHarness() {
    }

    private enum ExpectedOutcome {
        SUCCESS,
        FAILURE
    }

    private static final class Case {
        final String name;
        final ExpectedOutcome expected;
        final String script;

        Case(String name, ExpectedOutcome expected, String script) {
            this.name = name;
            this.expected = expected;
            this.script = script;
        }
    }

    public static void main(String[] args) {
        List<Case> cases = new ArrayList<Case>();
        // Control cases that should stay green.
        cases.add(new Case("lambda_listener", ExpectedOutcome.SUCCESS,
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "Button b = new Button(\"Go\");\n"
                        + "b.addActionListener(e -> {});\n"
                        + "root.add(b);\n"
                        + "root;\n"));
        cases.add(new Case("anonymous_listener", ExpectedOutcome.SUCCESS,
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.events.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "Button b = new Button(\"Go\");\n"
                        + "b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {} });\n"
                        + "root.add(b);\n"
                        + "root;\n"));

        // Known syntax gaps: flip these to SUCCESS as support lands.
        cases.add(new Case("method_reference", ExpectedOutcome.FAILURE,
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "Button b = new Button(\"Go\");\n"
                        + "b.addActionListener(System.out::println);\n"
                        + "root.add(b);\n"
                        + "root;\n"));
        cases.add(new Case("try_with_resources_multi", ExpectedOutcome.FAILURE,
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "import java.io.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "try (ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{1}); ByteArrayOutputStream out = new ByteArrayOutputStream()) {\n"
                        + "    out.write(in.read());\n"
                        + "}\n"
                        + "root;\n"));
        cases.add(new Case("enhanced_for_array", ExpectedOutcome.FAILURE,
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "int sum = 0;\n"
                        + "for (int v : new int[]{1,2,3}) {\n"
                        + "    sum += v;\n"
                        + "}\n"
                        + "root.add(new Label(\"sum=\" + sum));\n"
                        + "root;\n"));

        int passed = 0;
        for (Case testCase : cases) {
            PlaygroundRunner.RunResult result = runSnippet(testCase.script);
            boolean success = result.getComponent() instanceof Component;
            boolean casePassed = testCase.expected == ExpectedOutcome.SUCCESS ? success : !success;
            if (!casePassed) {
                throw new IllegalStateException("Case failed: " + testCase.name
                        + " expected=" + testCase.expected + " messages=" + summarizeMessages(result));
            }
            passed++;
        }

        System.out.println("Playground syntax matrix passed (" + passed + "/" + cases.size() + ").");
        System.exit(0);
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
