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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;

import java.util.ArrayList;
import java.util.List;

import static com.codenameone.playground.HarnessSupport.context;
import static com.codenameone.playground.HarnessSupport.require;
import static com.codenameone.playground.HarnessSupport.summarize;

/**
 * End-to-end checks of the Playground runner on the JVM: scripts and classes are
 * compiled by the in-tree compiler against the generated API stubs, loaded, run,
 * and their preview resolved.
 */
public final class PlaygroundSmokeHarness {
    private PlaygroundSmokeHarness() {
    }

    public static void main(String[] args) throws Exception {
        try {
            smokeApiIndex();
            smokeHostSetupFinishesBeforeReturning();
            smokeFormShowIsCaptured();
            smokeLifecycleWrapperScript();
            smokeLooseScriptListeners();
            smokeLooseScriptListSnippet();
            smokeLifecycleDemo();
            smokeRestScriptWithLambda();
            smokeStringMethods();
            smokeComponentTypeResolvesWithoutExplicitImport();
            smokeUIManagerClassImport();
            smokeCompileErrorNamesTheProblem();
            smokeUnknownTypeIsACompileError();
            smokeRuntimeErrorIsReported();
            smokeBuildMethodScript();
            smokeRecordsAndPatternsRun();
            System.out.println("Playground smoke tests passed.");
        } catch (Throwable failure) {
            failure.printStackTrace();
            // exec:java otherwise waits for JavaSE's non-daemon UI threads
            // after an assertion, hiding the actual failure behind a timeout.
            System.exit(1);
        }
        // Codename One/JavaSE initialization may leave non-daemon threads running.
        // Force a clean exit so CI jobs don't hang after successful completion.
        System.exit(0);
    }

    private static PlaygroundRunner.RunResult run(String script, PlaygroundContext context) {
        return new PlaygroundRunner().run(script, context);
    }

    private static void smokeApiIndex() {
        HarnessSupport.context();
        java.util.Set<String> names = new java.util.HashSet<String>(java.util.Arrays.asList(PlaygroundApi.classNames()));
        require(names.contains("com.codename1.ui.layouts.BoxLayout"), "API index should list BoxLayout");
        for (String cls : new String[]{"Button", "Container", "Dialog", "Display", "Form", "Label", "List", "TextField",
                "BrowserComponent", "CodeEditor", "RichTextArea", "Component"}) {
            require(names.contains("com.codename1.ui." + cls), "API index is missing com.codename1.ui." + cls);
        }
        // Internal classes stay out of completion (they are not public API).
        require(!names.contains("com.codename1.ui.Accessor") && !names.contains("com.codename1.io.IOAccessor"),
                "API index should not list internal accessor classes");
        java.util.List<String> rich = java.util.Arrays.asList(PlaygroundApi.methodSignatures("com.codename1.ui.RichTextArea"));
        for (String m : new String[]{"setContent(String,RichTextFormat)", "setMarkdown(String)", "setAsciiDoc(String)",
                "setRtf(String)"}) {
            require(rich.contains(m), "API index is missing editor API " + m + " in " + rich);
        }
        boolean setText = false;
        for (String m : PlaygroundApi.methodSignatures("com.codename1.ui.Label")) {
            setText |= "setText(String)".equals(m);
        }
        require(setText, "API index should list Label.setText(String)");
        boolean dips = false;
        for (String f : PlaygroundApi.fieldNames("com.codename1.ui.plaf.Style")) {
            dips |= "UNIT_TYPE_DIPS".equals(f);
        }
        require(dips, "API index should list Style.UNIT_TYPE_DIPS");
    }

    private static void smokeHostSetupFinishesBeforeReturning() {
        final boolean[] current = new boolean[1];
        Display.getInstance().callSeriallyAndWait(() -> {
            Display.getInstance().getCurrent().setTransitionOutAnimator(
                    com.codename1.ui.animations.CommonTransitions.createFade(10000));
            PlaygroundContext next = context();
            current[0] = Display.getInstance().getCurrent() == next.getHostForm();
        });
        require(current[0], "Host setup must finish before the preview is tested");
    }

    private static void smokeFormShowIsCaptured() {
        PlaygroundContext context = context();
        Form host = context.getHostForm();
        PlaygroundRunner.RunResult result = run(
                "Form f = new Form(\"Shown\", new BorderLayout());\n"
                + "f.add(BorderLayout.CENTER, new Label(\"x\"));\n"
                + "f.show();\n", context);
        require(result.getComponent() instanceof Form && "Shown".equals(((Form) result.getComponent()).getTitle()),
                "form.show() should become the preview: " + summarize(result));
        require(Display.getInstance().getCurrent() == host, "form.show() must not replace the Playground UI");
    }

    private static void smokeLifecycleWrapperScript() {
        List<String> log = new ArrayList<String>();
        PlaygroundContext context = context(log);
        PlaygroundRunner.RunResult result = run(
                "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "public class C {\n"
                + "public void init(Object o) {}\n"
                + "public void start() {\n"
                + "Form root = new Form(\"Test\", BoxLayout.y());\n"
                + "root.add(new Label(\"Hello\"));\n"
                + "ctx.log(\"Preview built successfully\");\n"
                + "root.show();\n"
                + "}\n"
                + "}\n",
                context);
        require(result.getComponent() instanceof Form, "Lifecycle wrapper script should return the shown Form: "
                + summarize(result));
        require(log.size() == 1 && "Preview built successfully".equals(log.get(0)),
                "Lifecycle wrapper script did not execute expected lifecycle body: " + log);
        require("Host".equals(context.getHostForm().getTitle()), "The host form title must not change");
        require("Test".equals(((Form) result.getComponent()).getTitle()), "Lifecycle script should preserve the shown form");
    }

    private static void smokeLooseScriptListeners() {
        PlaygroundContext context = context();
        PlaygroundRunner.RunResult lambdaResult = run(
                "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "Button save = new Button(\"Save\");\n"
                + "save.addActionListener(e -> {});\n"
                + "root.add(save);\n"
                + "root;\n",
                context);
        require(lambdaResult.getComponent() != null, "Loose script lambda listener should compile: " + summarize(lambdaResult));
        PlaygroundRunner.RunResult anonResult = run(
                "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.events.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "Button save = new Button(\"Save\");\n"
                + "save.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent evt) {} });\n"
                + "root.add(save);\n"
                + "root;\n",
                context);
        require(anonResult.getComponent() != null, "Loose script anonymous listener should compile: " + summarize(anonResult));
    }

    private static void smokeLooseScriptListSnippet() {
        List<String> log = new ArrayList<String>();
        PlaygroundContext context = context(log);
        PlaygroundRunner.RunResult result = run(
                "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "import com.codename1.components.*;\n"
                + "\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "root.setScrollableY(true);\n"
                + "for (int i = 1; i <= 8; i++) {\n"
                + "    MultiButton row = new MultiButton(\"Menu Item \" + i);\n"
                + "    row.addActionListener(e -> {});\n"
                + "    row.setTextLine2(\"Secondary line for item \" + i);\n"
                + "    root.add(row);\n"
                + "}\n"
                + "ctx.log(\"List sample loaded\");\n"
                + "root;\n",
                context);
        require(result.getComponent() instanceof Container, "List snippet should produce a Container: " + summarize(result));
        require(((Container) result.getComponent()).getComponentCount() == 8, "List snippet should add 8 rows");
        require(log.size() == 1 && "List sample loaded".equals(log.get(0)), "List snippet should log its completion");
    }

    private static void smokeLifecycleDemo() {
        PlaygroundContext context = context();
        PlaygroundRunner.RunResult result = run(PlaygroundExamples.LIFECYCLE_SCRIPT, context);
        require(result.getComponent() instanceof Form, "Lifecycle demo should produce a Form: " + summarize(result));
        Form f = (Form) result.getComponent();
        require("Lifecycle Demo".equals(f.getTitle()), "Lifecycle demo should preserve the form title");
        // The listener is real compiled code: fire it and see the label change.
        Button button = findButton(f.getContentPane());
        require(button != null, "Lifecycle demo should contain a button");
        final Button pressed = button;
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
                pressed.pressed();
                pressed.released();
            }
        });
        // The listener may itself be queued: let the EDT drain once more.
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            public void run() {
            }
        });
        Label status = findLabelStartingWith(f.getContentPane(), "Tapped at ");
        require(status != null, "Pressing the button should update the status label");
    }

    private static Button findButton(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            if (c.getComponentAt(i) instanceof Button) {
                return (Button) c.getComponentAt(i);
            }
        }
        return null;
    }

    private static Label findLabelStartingWith(Container c, String prefix) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            if (c.getComponentAt(i) instanceof Label && !(c.getComponentAt(i) instanceof Button)) {
                String t = ((Label) c.getComponentAt(i)).getText();
                if (t != null && t.startsWith(prefix)) {
                    return (Label) c.getComponentAt(i);
                }
            }
        }
        return null;
    }

    private static void smokeRestScriptWithLambda() {
        PlaygroundContext context = context();
        PlaygroundRunner.RunResult result = run(
                "import com.codename1.components.*;\n"
                + "import com.codename1.io.rest.*;\n"
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.events.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "root.setScrollableY(true);\n"
                + "SpanLabel output = new SpanLabel(\"Test\");\n"
                + "Button load = new Button(\"Load\");\n"
                + "load.addActionListener(e -> {\n"
                + "    String text = \"test data\";\n"
                + "    output.setText(text.length() > 10 ? text.substring(0, 10) : text);\n"
                + "    output.getParent().revalidate();\n"
                + "});\n"
                + "root.addAll(load, output);\n"
                + "root;\n",
                context);
        require(result.getComponent() instanceof Container, "REST script should produce a Container: " + summarize(result));
    }

    private static void smokeStringMethods() {
        List<String> log = new ArrayList<String>();
        PlaygroundContext context = context(log);
        PlaygroundRunner.RunResult result = run(
                "Container root = new Container(BoxLayout.y());\n"
                + "String text = \"Hello World\";\n"
                + "String sub = text.substring(0, 5);\n"
                + "String upper = text.toUpperCase();\n"
                + "int len = text.length();\n"
                + "ctx.log(\"substring: \" + sub);\n"
                + "ctx.log(\"upper: \" + upper);\n"
                + "ctx.log(\"len: \" + len);\n"
                + "root.add(new Label(sub));\n"
                + "root;\n",
                context);
        require(result.getComponent() != null, "String methods script should produce a component: " + summarize(result));
        require(log.size() == 3 && "upper: HELLO WORLD".equals(log.get(1)) && "len: 11".equals(log.get(2)),
                "String methods script should log results: " + log);
    }

    private static void smokeComponentTypeResolvesWithoutExplicitImport() {
        PlaygroundRunner.RunResult result = run("Component c = new Label(\"Implicit import works\");\nc;\n", context());
        require(result.getComponent() instanceof Label, "Component should resolve without an import: " + summarize(result));
    }

    private static void smokeUIManagerClassImport() {
        PlaygroundRunner.RunResult result = run(
                "import com.codename1.ui.plaf.Border;\n"
                + "import com.codename1.ui.plaf.UIManager;\n"
                + "Button top = new Button(\"Top\");\n"
                + "String cls = top.getClass().getName();\n"
                + "UIManager uim = UIManager.getInstance();\n"
                + "boolean hasArrow = uim.isThemeConstant(\"PopupDialogArrowBool\", false);\n"
                + "InteractionDialog it = new InteractionDialog();\n"
                + "it.setUIID(\"PopupDialog\");\n"
                + "Border b = it.getStyle().getBorder();\n"
                + "Container c = BorderLayout.north(top);\n"
                + "c.add(BorderLayout.CENTER, new Label(cls + \" \" + (hasArrow ? \"1\" : \"0\") + \" \" + (b == null ? \"null\" : b.getClass().getName())));\n"
                + "c;\n",
                context());
        require(result.getComponent() instanceof Container, "UIManager import snippet should produce a Container: "
                + summarize(result));
    }

    private static void smokeCompileErrorNamesTheProblem() {
        PlaygroundRunner.RunResult result = run(
                "import com.codename1.components.*;\n"
                + "MultiButton row = new MultiButton(\"Inbox\");\n"
                + "row.setMaterialIcon(FontImage.MATERIAL_ADD_CIRCLE);\n"
                + "row;\n",
                context());
        require(result.getComponent() == null, "A call with the wrong arguments must not run");
        require(!result.getDiagnostics().isEmpty() && result.getDiagnostics().get(0).line == 3,
                "The diagnostic should point at line 3: " + summarize(result));
        require(summarize(result).indexOf("setMaterialIcon") >= 0, "The diagnostic should name the method: " + summarize(result));
    }

    private static void smokeUnknownTypeIsACompileError() {
        PlaygroundRunner.RunResult result = run(
                "Runnable r = () -> DefinitelyMissingType.doStuff();\n"
                + "Label hi = new Label(\"OK\");\n"
                + "hi;\n",
                context());
        require(result.getComponent() == null, "An unknown type must be a compile error");
        require(result.getDiagnostics().get(0).line == 1 && summarize(result).indexOf("DefinitelyMissingType") >= 0,
                "The diagnostic should name the missing type on line 1: " + summarize(result));
    }

    private static void smokeRuntimeErrorIsReported() {
        PlaygroundRunner.RunResult result = run(
                "Label hi = new Label(\"OK\");\n"
                + "String s = null;\n"
                + "hi.setText(s.trim());\n"
                + "hi;\n",
                context());
        require(result.getComponent() == null && summarize(result).indexOf("Runtime error") >= 0
                && summarize(result).indexOf("NullPointerException") >= 0,
                "A runtime exception should be reported: " + summarize(result));
    }

    private static void smokeBuildMethodScript() {
        List<String> log = new ArrayList<String>();
        PlaygroundRunner.RunResult result = run(PlaygroundExamples.BUILD_METHOD_SCRIPT, context(log));
        require(result.getComponent() != null, "build(ctx) script should produce a component: " + summarize(result));
        require(log.contains("build(ctx) executed"), "build(ctx) should have run: " + log);
    }

    private static void smokeRecordsAndPatternsRun() {
        PlaygroundRunner.RunResult result = run(
                "sealed interface Shape permits Circle, Square {}\n"
                + "record Circle(double r) implements Shape {}\n"
                + "record Square(double s) implements Shape {}\n"
                + "double area(Shape s) {\n"
                + "    return switch (s) {\n"
                + "        case Circle c -> Math.PI * c.r() * c.r();\n"
                + "        case Square q -> q.s() * q.s();\n"
                + "    };\n"
                + "}\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + "for (Shape s : Arrays.asList(new Circle(1), new Square(2))) {\n"
                + "    root.add(new Label(s + \" \" + (int) area(s)));\n"
                + "}\n"
                + "root;\n",
                context());
        require(result.getComponent() instanceof Container, "Records and pattern switch should run: " + summarize(result));
        Container c = (Container) result.getComponent();
        require(c.getComponentCount() == 2 && "Square[s=2.0] 4".equals(((Label) c.getComponentAt(1)).getText()),
                "Unexpected labels: " + ((Label) c.getComponentAt(1)).getText());
    }

}
