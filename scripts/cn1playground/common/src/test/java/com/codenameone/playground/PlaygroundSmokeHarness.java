package com.codenameone.playground;

import bsh.cn1.GeneratedCN1Access;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.List;

public final class PlaygroundSmokeHarness {
    private PlaygroundSmokeHarness() {
    }

    public static void main(String[] args) throws Exception {
        smokeGeneratedRegistry();
        smokeLifecycleWrapperScript();
        smokeLooseScriptListeners();
        smokeLooseScriptListSnippet();
        smokeLifecycleDemo();
        smokeRestScriptWithLambda();
        smokeStringMethods();
        smokeComponentTypeResolvesWithoutExplicitImport();
        smokeUIManagerClassImportDoesNotCollideWithGlobals();
        System.out.println("Playground smoke tests passed.");
    }

    private static void smokeGeneratedRegistry() throws Exception {
        GeneratedCN1Access access = GeneratedCN1Access.INSTANCE;
        require(access.findClass("com.codename1.ui.layouts.BoxLayout") == BoxLayout.class,
                "BoxLayout class lookup failed");
        require(access.invokeStatic(BoxLayout.class, "y", new Object[0]) instanceof BoxLayout,
                "BoxLayout.y() dispatch failed");
        require(access.getStaticField(Style.class, "UNIT_TYPE_DIPS") instanceof Byte,
                "Style.UNIT_TYPE_DIPS dispatch failed");

        final List<String> log = new ArrayList<String>();
        PlaygroundContext context = new PlaygroundContext(null, null, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });
        access.invoke(context, "log", new Object[]{"hello"});
        require(log.size() == 1 && "hello".equals(log.get(0)), "PlaygroundContext.log dispatch failed");

        Display.init(null);
        Form host = new Form("Host", new BorderLayout());
        PlaygroundContext showContext = new PlaygroundContext(host, new Container(new BorderLayout()), null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                    }
                });
        Form shown = new Form("Shown", new BorderLayout());
        PlaygroundContext.pushCurrent(showContext);
        try {
            access.invoke(shown, "show", new Object[0]);
        } finally {
            PlaygroundContext.clearCurrent();
        }
        require(showContext.getShownForm() == shown, "Form.show() dispatch should capture the shown form");
        require(Display.getInstance().getCurrent() != shown, "Form.show() interception should not replace the host UI");
    }

    private static void smokeLifecycleWrapperScript() {
        Display.init(null);

        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        final List<String> log = new ArrayList<String>();
        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
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

        require(result.getComponent() != null,
                "Lifecycle wrapper script did not produce a preview component: " + summarizeMessages(result));
        require(result.getComponent() instanceof Form, "Lifecycle wrapper script should return the shown Form");
        require(containsMessage(result, "Preview updated."),
                "Lifecycle wrapper script returned unexpected messages: " + summarizeMessages(result));
        require(log.size() == 1 && "Preview built successfully".equals(log.get(0)),
                "Lifecycle wrapper script did not execute expected lifecycle body");
        require("Host".equals(host.getTitle()), "Lifecycle wrapper script should not replace the host form title");
        require("Test".equals(((Form) result.getComponent()).getTitle()),
                "Lifecycle wrapper script should preserve the shown form");
    }

    private static void smokeLooseScriptListeners() {
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

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult lambdaResult = runner.run(
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "Button save = new Button(\"Save\");\n"
                        + "save.addActionListener(e -> {});\n"
                        + "root.add(save);\n"
                        + "root;\n",
                context);
        require(lambdaResult.getComponent() != null,
                "Loose script lambda listener should compile: " + summarizeMessages(lambdaResult));

        PlaygroundRunner.RunResult anonResult = runner.run(
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.events.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "Container root = new Container(BoxLayout.y());\n"
                        + "Button save = new Button(\"Save\");\n"
                        + "save.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent evt) {} });\n"
                        + "root.add(save);\n"
                        + "root;\n",
                context);
        require(anonResult.getComponent() != null,
                "Loose script anonymous listener should compile: " + summarizeMessages(anonResult));
    }

    private static void smokeLooseScriptListSnippet() {
        Display.init(null);

        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        final List<String> log = new ArrayList<String>();
        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
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

        require(result.getComponent() instanceof Container,
                "Loose script list snippet should produce a Container: " + summarizeMessages(result));
        require(log.size() == 1 && "List sample loaded".equals(log.get(0)),
                "Loose script list snippet should log its completion");
    }

    private static void smokeComponentTypeResolvesWithoutExplicitImport() {
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

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
                "Component c = new Label(\"Implicit import works\");\n"
                        + "c;\n",
                context);

        require(result.getComponent() instanceof Label,
                "Component type should resolve without explicit import: " + summarizeMessages(result));
    }


    private static void smokeUIManagerClassImportDoesNotCollideWithGlobals() {
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

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
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
                context);

        require(result.getComponent() instanceof Container,
                "UIManager import snippet should produce a Container: " + summarizeMessages(result));
    }
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static boolean containsMessage(PlaygroundRunner.RunResult result, String expected) {
        List<PlaygroundRunner.InlineMessage> messages = result.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            PlaygroundRunner.InlineMessage message = messages.get(i);
            if (expected.equals(message.text)) {
                return true;
            }
        }
        return false;
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

    private static void smokeLifecycleDemo() {
        Display.init(null);

        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        final List<String> log = new ArrayList<String>();
        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.events.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "\n"
                        + "public class DemoApp {\n"
                        + "    private Label status;\n"
                        + "\n"
                        + "    public void init(Object context) {}\n"
                        + "\n"
                        + "    public void start() {\n"
                        + "        Form form = new Form(\"Lifecycle Demo\", BoxLayout.y());\n"
                        + "        status = new Label(\"Ready\");\n"
                        + "        Button button = new Button(\"Tap me\");\n"
                        + "        button.addActionListener(new ActionListener() {\n"
                        + "            public void actionPerformed(ActionEvent evt) {\n"
                        + "                status.setText(\"Tapped at \" + System.currentTimeMillis());\n"
                        + "                status.getParent().revalidate();\n"
                        + "            }\n"
                        + "        });\n"
                        + "        form.addAll(new Label(\"Lifecycle-style scripts are the easiest place to test listeners.\"), button, status);\n"
                        + "        form.show();\n"
                        + "    }\n"
                        + "}\n",
                context);

        require(result.getComponent() != null,
                "Lifecycle demo script should produce a component: " + summarizeMessages(result));
        require(result.getComponent() instanceof Form,
                "Lifecycle demo script should produce a Form");
        require("Lifecycle Demo".equals(((Form) result.getComponent()).getTitle()),
                "Lifecycle demo script should preserve form title");
    }

    private static void smokeRestScriptWithLambda() {
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

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
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
                        + "load.addActionListener(() -> {\n"
                        + "    String text = \"test data\";\n"
                        + "    output.setText(text.length() > 10 ? text.substring(0, 10) : text);\n"
                        + "    output.getParent().revalidate();\n"
                        + "});\n"
                        + "root.addAll(load, output);\n"
                        + "root;\n",
                context);

        require(result.getComponent() != null,
                "REST script with lambda should compile: " + summarizeMessages(result));
        require(result.getComponent() instanceof Container,
                "REST script should produce a Container");
    }

    private static void smokeStringMethods() {
        Display.init(null);

        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        final List<String> log = new ArrayList<String>();
        PlaygroundContext context = new PlaygroundContext(host, preview, null,
                new PlaygroundContext.Logger() {
                    public void log(String message) {
                        log.add(message);
                    }
                });

        PlaygroundRunner runner = new PlaygroundRunner();
        PlaygroundRunner.RunResult result = runner.run(
                "import com.codename1.ui.*;\n"
                        + "import com.codename1.ui.layouts.*;\n"
                        + "\n"
                        + "Container root = new Container(BoxLayout.y());\n"
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

        require(result.getComponent() != null,
                "String methods script should produce a component: " + summarizeMessages(result));
        require(log.size() >= 3,
                "String methods script should log results");
        require(containsMessage(result, "Preview updated."),
                "String methods script should complete successfully");
    }
}
