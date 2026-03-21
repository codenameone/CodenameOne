package com.codenameone.playground;

final class PlaygroundExamples {
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

    private PlaygroundExamples() {
    }
}
