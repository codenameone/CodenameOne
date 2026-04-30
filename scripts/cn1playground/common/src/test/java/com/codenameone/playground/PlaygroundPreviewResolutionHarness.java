package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Harness covering the preview-component resolution rules for the
 * simplified syntax: trailing identifier wins, otherwise a Form passed
 * to show(), otherwise the first Form constructed during execution,
 * otherwise the first Component constructed during execution.
 *
 * Also asserts that the lifecycle and explicit-build paths still work
 * as before.
 */
public final class PlaygroundPreviewResolutionHarness {
    private PlaygroundPreviewResolutionHarness() {
    }

    private interface Check {
        String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext context);
    }

    private static final class Case {
        final String name;
        final String script;
        final boolean expectFailure;
        final Check check;

        Case(String name, String script, boolean expectFailure, Check check) {
            this.name = name;
            this.script = script;
            this.expectFailure = expectFailure;
            this.check = check;
        }
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            List<Case> cases = new ArrayList<Case>();
            addCases(cases);

            List<String> failures = new ArrayList<String>();
            int passed = 0;
            for (int i = 0; i < cases.size(); i++) {
                Case testCase = cases.get(i);
                Run run = runScript(testCase.script);
                String failure = evaluate(testCase, run);
                if (failure == null) {
                    passed++;
                } else {
                    failures.add("[" + testCase.name + "] " + failure);
                }
            }

            System.out.println("Playground preview resolution: "
                    + passed + "/" + cases.size() + " passed");
            if (!failures.isEmpty()) {
                System.out.println("Failures (" + failures.size() + "):");
                for (int i = 0; i < failures.size(); i++) {
                    System.out.println("  - " + failures.get(i));
                }
                throw new IllegalStateException("Preview resolution mismatches: "
                        + failures.size() + " of " + cases.size());
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    private static String evaluate(Case testCase, Run run) {
        boolean failed = run.result.getComponent() == null
                || hasErrorDiagnostic(run.result);
        if (testCase.expectFailure) {
            return failed ? null
                    : "expected failure but resolved to "
                            + describe(run.result.getComponent());
        }
        if (failed) {
            return "expected success but failed; component="
                    + describe(run.result.getComponent())
                    + ", diagnostic=" + firstDiagnostic(run.result);
        }
        if (testCase.check != null) {
            return testCase.check.evaluate(run.result, run.context);
        }
        return null;
    }

    private static boolean hasErrorDiagnostic(PlaygroundRunner.RunResult result) {
        List<PlaygroundRunner.Diagnostic> diags = result.getDiagnostics();
        if (diags == null) return false;
        for (int i = 0; i < diags.size(); i++) {
            if ("error".equals(diags.get(i).severity)) return true;
        }
        return false;
    }

    private static String firstDiagnostic(PlaygroundRunner.RunResult result) {
        List<PlaygroundRunner.Diagnostic> diags = result.getDiagnostics();
        if (diags != null && !diags.isEmpty()) {
            return diags.get(0).message;
        }
        List<PlaygroundRunner.InlineMessage> msgs = result.getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            return msgs.get(0).text;
        }
        return "<none>";
    }

    private static String describe(Component component) {
        if (component == null) return "null";
        return component.getClass().getSimpleName() + "@" + System.identityHashCode(component);
    }

    private static void addCases(List<Case> cases) {
        // 1. Trailing identifier (Container) wins.
        cases.add(new Case(
                "trailing-container-identifier",
                ""
                        + "Container c = new Container();\n"
                        + "c;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Container.class);
                    }
                }));

        // 2. Trailing identifier (Form) wins.
        cases.add(new Case(
                "trailing-form-identifier",
                ""
                        + "Form f = new Form(\"Hi\");\n"
                        + "f;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Form.class);
                    }
                }));

        // 3. Trailing identifier wins even when a different Form was created first.
        cases.add(new Case(
                "trailing-wins-over-earlier-form",
                ""
                        + "Form first = new Form(\"first\");\n"
                        + "Form second = new Form(\"second\");\n"
                        + "second;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form expected = (Form) ctx.getCreatedComponents().get(1);
                        return expectIdentity(result, expected, "second form");
                    }
                }));

        // 4. Trailing Container wins even when a Form was shown via show().
        cases.add(new Case(
                "trailing-container-wins-over-show",
                ""
                        + "Form a = new Form(\"a\");\n"
                        + "a.show();\n"
                        + "Container c = new Container();\n"
                        + "c;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Container.class);
                    }
                }));

        // 5. show() wins when there is no trailing identifier.
        cases.add(new Case(
                "show-without-trailing",
                ""
                        + "Form f = new Form(\"shown\");\n"
                        + "f.show();\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form shown = ctx.getShownForm();
                        if (shown == null) {
                            return "context did not capture shownForm";
                        }
                        return expectIdentity(result, shown, "shown form");
                    }
                }));

        // 6. show() wins over later-created components when no trailing identifier.
        cases.add(new Case(
                "show-wins-over-later-component",
                ""
                        + "Form f = new Form(\"shown\");\n"
                        + "f.show();\n"
                        + "Container ignored = new Container();\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form shown = ctx.getShownForm();
                        if (shown == null) {
                            return "context did not capture shownForm";
                        }
                        return expectIdentity(result, shown, "shown form");
                    }
                }));

        // 7. No show, no trailing identifier: first created Form wins, even
        //    when a Container was created earlier.
        cases.add(new Case(
                "first-created-form-wins-over-earlier-container",
                ""
                        + "Container early = new Container();\n"
                        + "Form mid = new Form(\"mid\");\n"
                        + "Container late = new Container();\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form expected = ctx.getFirstCreatedForm();
                        if (expected == null) {
                            return "no form recorded as first-created";
                        }
                        return expectIdentity(result, expected, "first-created form");
                    }
                }));

        // 8. Two forms, no show, no trailing: the first one wins.
        cases.add(new Case(
                "first-of-two-forms",
                ""
                        + "Form a = new Form(\"a\");\n"
                        + "Form b = new Form(\"b\");\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form a = (Form) ctx.getCreatedComponents().get(0);
                        return expectIdentity(result, a, "first form");
                    }
                }));

        // 9. No Form at all — first created Component wins.
        cases.add(new Case(
                "first-created-component",
                ""
                        + "Container outer = new Container();\n"
                        + "Container inner = new Container();\n"
                        + "outer.add(inner);\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Component first = ctx.getCreatedComponents().get(0);
                        return expectIdentity(result, first, "first-created component");
                    }
                }));

        // 10. Single component, no trailing: that single component wins.
        cases.add(new Case(
                "single-component-no-trailing",
                "Button b = new Button(\"hi\");\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Component first = ctx.getFirstCreatedComponent();
                        return expectIdentity(result, first, "the only created component");
                    }
                }));

        // 11. Empty body — no resolution possible, must fail.
        cases.add(new Case(
                "empty-body-fails",
                "",
                true,
                null));

        // 12. Imports only — no resolution possible.
        cases.add(new Case(
                "imports-only-fails",
                ""
                        + "import com.codename1.ui.Form;\n"
                        + "import com.codename1.ui.Container;\n",
                true,
                null));

        // 13. Trailing non-component expression with no fallback — must fail.
        cases.add(new Case(
                "trailing-non-component-no-fallback",
                ""
                        + "int x = 41;\n"
                        + "x + 1;\n",
                true,
                null));

        // 14. Trailing non-component expression but a Form was shown — show wins.
        cases.add(new Case(
                "trailing-non-component-falls-back-to-show",
                ""
                        + "Form f = new Form(\"shown\");\n"
                        + "f.show();\n"
                        + "1 + 2;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form shown = ctx.getShownForm();
                        if (shown == null) {
                            return "context did not capture shownForm";
                        }
                        return expectIdentity(result, shown, "shown form");
                    }
                }));

        // 15. Trailing non-component expression with no show, but a Form created — first form wins.
        cases.add(new Case(
                "trailing-non-component-falls-back-to-first-form",
                ""
                        + "Form f = new Form(\"f\");\n"
                        + "1 + 2;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        Form expected = ctx.getFirstCreatedForm();
                        return expectIdentity(result, expected, "first-created form");
                    }
                }));

        // 16. show() called on a Form that is not the host or preview.
        //     Make sure the host form does not leak into createdComponents.
        cases.add(new Case(
                "host-form-not-in-created-components",
                "Container c = new Container();\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        List<Component> created = ctx.getCreatedComponents();
                        for (int i = 0; i < created.size(); i++) {
                            if (created.get(i) == ctx.getHostForm()) {
                                return "hostForm leaked into createdComponents";
                            }
                            if (created.get(i) == ctx.getPreviewRoot()) {
                                return "previewRoot leaked into createdComponents";
                            }
                        }
                        return null;
                    }
                }));

        // 17. Existing build(ctx) path — user-defined build still drives
        //     the preview, fallback chain doesn't override it.
        cases.add(new Case(
                "user-defined-build-method",
                ""
                        + "Component build(PlaygroundContext ctx) {\n"
                        + "    Container c = new Container();\n"
                        + "    Form spurious = new Form(\"ignored\");\n"
                        + "    return c;\n"
                        + "}\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Container.class);
                    }
                }));

        // 18. Lifecycle path still works (init + start showing a Form).
        cases.add(new Case(
                "lifecycle-init-start",
                ""
                        + "void init(Object arg) {\n"
                        + "}\n"
                        + "void start() {\n"
                        + "    Form f = new Form(\"life\");\n"
                        + "    f.show();\n"
                        + "}\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Form.class);
                    }
                }));

        // 19. Lifecycle path: start() returning a Component.
        cases.add(new Case(
                "lifecycle-start-returns-component",
                ""
                        + "Component start() {\n"
                        + "    Container c = new Container();\n"
                        + "    return c;\n"
                        + "}\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Container.class);
                    }
                }));

        // 20. Trailing identifier referencing a component created inside a helper method.
        cases.add(new Case(
                "trailing-from-helper-method",
                ""
                        + "Container makeContainer() {\n"
                        + "    return new Container();\n"
                        + "}\n"
                        + "Container c = makeContainer();\n"
                        + "c;\n",
                false,
                new Check() {
                    public String evaluate(PlaygroundRunner.RunResult result, PlaygroundContext ctx) {
                        return expectKind(result, Container.class);
                    }
                }));
    }

    private static String expectKind(PlaygroundRunner.RunResult result, Class<?> kind) {
        Component c = result.getComponent();
        if (c == null) {
            return "expected " + kind.getSimpleName() + " but got null";
        }
        if (!kind.isInstance(c)) {
            return "expected " + kind.getSimpleName() + " but got " + c.getClass().getSimpleName();
        }
        return null;
    }

    private static String expectIdentity(PlaygroundRunner.RunResult result, Component expected, String label) {
        Component actual = result.getComponent();
        if (actual != expected) {
            return "expected " + label + " (" + describe(expected)
                    + ") but got " + describe(actual);
        }
        return null;
    }

    private static final class Run {
        final PlaygroundRunner.RunResult result;
        final PlaygroundContext context;

        Run(PlaygroundRunner.RunResult result, PlaygroundContext context) {
            this.result = result;
            this.context = context;
        }
    }

    private static Run runScript(String script) {
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
        PlaygroundRunner.RunResult result = runner.run(script, context);
        return new Run(result, context);
    }
}
