package com.codenameone.playground;

import bsh.cn1.GeneratedCN1Access;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
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
}
