package com.codenameone.playground;

final class PlaygroundExamples {
    static final class Sample {
        final String title;
        final String script;

        Sample(String title, String script) {
            this.title = title;
            this.script = script;
        }
    }

    static final String DEFAULT_SCRIPT =
            "import com.codename1.ui.*;\n" +
            "import com.codename1.ui.layouts.*;\n" +
            "import com.codename1.components.*;\n" +
            "import com.codename1.ui.plaf.*;\n" +
            "\n" +
            "Container root = new Container(BoxLayout.y());\n" +
            "root.setScrollableY(true);\n" +
            "root.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);\n" +
            "root.getAllStyles().setPadding(3, 3, 3, 3);\n" +
            "\n" +
            "SpanLabel title = new SpanLabel(\"Codename One Playground\");\n" +
            "title.getAllStyles().setFgColor(0x1f3a5f);\n" +
            "root.add(title);\n" +
            "\n" +
            "Button button = new Button(\"Tap me\");\n" +
            "button.setText(\"Interactive controls can be added next\");\n" +
            "root.add(button);\n" +
            "\n" +
            "Label info = new Label(\"Rendered inside the preview panel\");\n" +
            "root.add(info);\n" +
            "\n" +
            "ctx.log(\"Preview built successfully\");\n" +
            "root;";

    static final String BUILD_METHOD_SCRIPT =
            "import com.codename1.ui.*;\n" +
            "import com.codename1.ui.layouts.*;\n" +
            "\n" +
            "Component build(PlaygroundContext ctx) {\n" +
            "    Container root = new Container(new BorderLayout());\n" +
            "    root.add(BorderLayout.CENTER, new Label(\"build(ctx) contract\"));\n" +
            "    ctx.log(\"build(ctx) executed\");\n" +
            "    return root;\n" +
            "}";

    static final String FORM_SCRIPT =
            "import com.codename1.ui.*;\n" +
            "import com.codename1.ui.layouts.*;\n" +
            "import com.codename1.components.*;\n" +
            "\n" +
            "Container root = new Container(BoxLayout.y());\n" +
            "root.setScrollableY(true);\n" +
            "root.add(new Label(\"Profile Card\"));\n" +
            "root.add(new SpanLabel(\"Use the side menu to load more samples or restore history.\"));\n" +
            "root.add(new TextField(\"Ada Lovelace\", \"Name\"));\n" +
            "root.add(new TextField(\"Mathematician\", \"Title\"));\n" +
            "root.add(new Button(\"Save\"));\n" +
            "root;";

    static final String LIST_SCRIPT =
            "import com.codename1.ui.*;\n" +
            "import com.codename1.ui.layouts.*;\n" +
            "import com.codename1.components.*;\n" +
            "\n" +
            "Container root = new Container(BoxLayout.y());\n" +
            "root.setScrollableY(true);\n" +
            "for (int i = 1; i <= 8; i++) {\n" +
            "    MultiButton row = new MultiButton(\"Menu Item \" + i);\n" +
            "    row.setTextLine2(\"Secondary line for item \" + i);\n" +
            "    root.add(row);\n" +
            "}\n" +
            "ctx.log(\"List sample loaded\");\n" +
            "root;";

    static final String TABS_SCRIPT =
            "import com.codename1.ui.*;\n" +
            "import com.codename1.ui.layouts.*;\n" +
            "\n" +
            "Tabs tabs = new Tabs();\n" +
            "tabs.addTab(\"News\", BoxLayout.encloseY(new Label(\"Latest updates\"), new Label(\"Deployment is green\")));\n" +
            "tabs.addTab(\"Stats\", BoxLayout.encloseY(new Label(\"Users: 42\"), new Label(\"Build time: 3m\")));\n" +
            "tabs.addTab(\"Notes\", BoxLayout.encloseY(new Label(\"Dark mode follows the website theme\")));\n" +
            "tabs;";

    static final Sample[] SAMPLES = new Sample[]{
            new Sample("Welcome", DEFAULT_SCRIPT),
            new Sample("build(ctx)", BUILD_METHOD_SCRIPT),
            new Sample("Profile Form", FORM_SCRIPT),
            new Sample("Menu List", LIST_SCRIPT),
            new Sample("Tabs", TABS_SCRIPT)
    };

    private PlaygroundExamples() {
    }
}
