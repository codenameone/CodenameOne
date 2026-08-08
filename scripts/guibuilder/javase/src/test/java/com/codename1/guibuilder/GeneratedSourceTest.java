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

package com.codename1.guibuilder;

import com.codename1.guibuilder.model.GuiDocument;
import com.codename1.guibuilder.project.ProjectBinding;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedSourceTest {
    /**
     * Constructing the builder creates a UIManager, which needs a Display. Relying on another test
     * class to have initialized it first passes locally and fails wherever the runner happens to
     * order this class first.
     */
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void generatedFormAndBindingModelCompileTogether() throws Exception {
        CodenameOneGUIBuilder builder = builder("properties");

        String form = invoke(builder, "defaultCompanionSource");
        String model = invoke(builder, "generatedModelSource");
        assertTrue(form.contains("extends Form"));
        assertTrue(form.contains("new UiBinding().bind(model, this)"));
        assertTrue(model.contains("implements PropertyBusinessObject"));
        assertTrue(form.contains("// <gui-builder-generated>"));
        assertTrue(form.contains("// <gui-builder-user-code>"));

        compile(form, model);
    }

    @Test
    void bindablePojoStrategyGeneratesAnnotationBinderSource() throws Exception {
        CodenameOneGUIBuilder builder = builder("bindable");
        String form = invoke(builder, "defaultCompanionSource");
        String model = invoke(builder, "generatedModelSource");
        assertTrue(form.contains("Binders.bind(model, this)"));
        assertTrue(model.contains("@Bindable"));
        assertTrue(model.contains("@Bind(name = \"email\", attr = BindAttr.TEXT)"));
        assertTrue(model.contains("@Bind(name = \"remember\", attr = BindAttr.SELECTED)"));
        compile(form, model);
    }

    @Test
    void noBindingStrategyHasNoModelDependency() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        String form = invoke(builder, "defaultCompanionSource");
        assertFalse(form.contains("LoginFormModel model"));
        assertFalse(form.contains("UiBinding"));
        compile(form, null);
    }

    @Test
    void guidedConstraintsAndLaterSiblingReferencesGenerateCompilableSource() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"LayeredLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Button\" name=\"linked\" text=\"Linked\" layeredInsets=\"baseline -20px auto 20px\" guidedReferences=\"anchor|anchor|-|anchor\" guidedReferencePositions=\"0 0 0 0\" guidedHorizontalAnchor=\"0.5\"/>"
                + "<component type=\"Label\" name=\"anchor\" text=\"Anchor\" layeredInsets=\"30px auto auto 30px\" guidedPreferredWidth=\"140\"/>"
                + "</component>"));

        String form = invoke(builder, "defaultCompanionSource");

        assertTrue(form.contains("setReferenceComponents(linked, anchor, anchor, null, anchor)"));
        assertTrue(form.contains("size.setWidth(140)"));
        assertFalse(form.contains("setPreferredW"));
        assertFalse(form.contains("setPreferredH"));
        assertTrue(form.indexOf("anchor = new Label") < form.indexOf("setReferenceComponents(linked"),
                "all siblings must exist before a name-based relationship is installed");
        compile(form, null);
    }

    /**
     * The designer applies these through ComponentPreviewFactory, so a component the user
     * configured looked right on the canvas and came up with every default at runtime.
     */
    @Test
    void inspectorPropertiesReachTheGeneratedSource() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Label\" name=\"caption\" text=\"Caption\" gap=\"6\" alignment=\"center\""
                + " tickerEnabled=\"true\" enabled=\"false\" visible=\"false\" rtl=\"true\"/>"
                + "<component type=\"SpanLabel\" name=\"blurb\" text=\"Blurb\" gap=\"4\"/>"
                + "<component type=\"TextField\" name=\"email\" hint=\"Email\" columns=\"20\" maxSize=\"64\""
                + " editable=\"false\" growByContent=\"false\" constraint=\"EMAILADDR\" alignment=\"right\"/>"
                + "<component type=\"TextArea\" name=\"notes\" rows=\"5\"/>"
                + "<component type=\"CheckBox\" name=\"remember\" text=\"Remember\" selected=\"true\"/>"
                + "<component type=\"RadioButton\" name=\"choice\" text=\"Choice\" selected=\"true\"/>"
                + "<component type=\"Button\" name=\"submit\" text=\"Sign in\" toggle=\"true\"/>"
                + "<component type=\"Slider\" name=\"volume\" minValue=\"10\" maxValue=\"90\" progress=\"40\""
                + " editable=\"true\" infinite=\"true\"/>"
                + "<component type=\"Container\" name=\"scroller\" layout=\"BoxLayout\" scrollableX=\"true\" scrollableY=\"true\"/>"
                + "<component type=\"Tabs\" name=\"sections\" selectedIndex=\"1\" tabPlacement=\"bottom\">"
                + "<component type=\"Container\" name=\"first\" layout=\"BoxLayout\"/>"
                + "<component type=\"Container\" name=\"second\" layout=\"BoxLayout\"/>"
                + "</component>"
                + "</component>"));

        String form = invoke(builder, "defaultCompanionSource");

        assertTrue(form.contains("caption.setEnabled(false);"), form);
        assertTrue(form.contains("caption.setVisible(false);"));
        assertTrue(form.contains("caption.setRTL(true);"));
        assertTrue(form.contains("caption.setGap(6);"));
        assertTrue(form.contains("caption.setAlignment(Component.CENTER);"));
        assertTrue(form.contains("caption.setTickerEnabled(true);"));
        assertTrue(form.contains("blurb.setGap(4);"));
        assertFalse(form.contains("blurb.setAlignment"), "SpanLabel has no setAlignment");
        assertTrue(form.contains("email.setColumns(20);"));
        assertTrue(form.contains("email.setMaxSize(64);"));
        assertTrue(form.contains("email.setEditable(false);"));
        assertTrue(form.contains("email.setGrowByContent(false);"));
        assertTrue(form.contains("email.setConstraint(TextArea.EMAILADDR);"));
        assertTrue(form.contains("email.setAlignment(Component.RIGHT);"));
        assertTrue(form.contains("notes.setRows(5);"));
        assertTrue(form.contains("remember.setSelected(true);"));
        assertTrue(form.contains("choice.setSelected(true);"));
        assertTrue(form.contains("submit.setToggle(true);"));
        assertTrue(form.contains("volume.setMinValue(10);"));
        assertTrue(form.contains("volume.setProgress(40);"));
        assertTrue(form.contains("volume.setInfinite(true);"));
        assertTrue(form.contains("scroller.setScrollableX(true);"));
        assertTrue(form.contains("scroller.setScrollableY(true);"));
        assertTrue(form.contains("sections.setTabPlacement(Component.BOTTOM);"));
        assertTrue(form.contains("sections.setSelectedIndex(1, false);"));
        assertTrue(form.indexOf("sections.addTab(") < form.indexOf("sections.setSelectedIndex"),
                "selecting a tab before the tabs exist throws at runtime");
        assertFalse(form.contains("submit.setEnabled"), "untouched properties must stay out of the source");

        compile(form, null);
    }

    /**
     * An out of range index is worse than no index: the designer clamps it, the runtime throws.
     */
    @Test
    void anImpossibleTabSelectionIsNotGenerated() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Tabs\" name=\"sections\" selectedIndex=\"4\">"
                + "<component type=\"Container\" name=\"only\" layout=\"BoxLayout\"/>"
                + "</component></component>"));
        String form = invoke(builder, "defaultCompanionSource");
        assertFalse(form.contains("setSelectedIndex"), form);
        compile(form, null);
    }

    @Test
    void tableCellPercentagesReachTheGeneratedSource() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Container\" name=\"grid\" layout=\"TableLayout\" tableLayoutRows=\"1\" tableLayoutColumns=\"2\">"
                + "<component type=\"Label\" name=\"left\" text=\"Left\" tableRow=\"0\" tableColumn=\"0\" tableWidth=\"30\"/>"
                + "<component type=\"Label\" name=\"right\" text=\"Right\" tableRow=\"0\" tableColumn=\"1\" tableWidth=\"70\" tableHeight=\"50\"/>"
                + "</component></component>"));
        String form = invoke(builder, "defaultCompanionSource");
        assertTrue(form.contains(".widthPercentage(30)"), form);
        assertTrue(form.contains(".widthPercentage(70).heightPercentage(50)"), form);
        compile(form, null);
    }

    @Test
    void aContainerRootGeneratesAContainerAndADialogRootGeneratesADialog() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/Panel.gui",
                "<component type=\"Container\" name=\"Panel\" layout=\"BoxLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Label\" name=\"caption\" text=\"Caption\"/></component>"));
        String container = invoke(builder, "defaultCompanionSource");
        assertTrue(container.contains("public class Panel extends Container"), container);
        assertTrue(container.contains("super(BoxLayout.y());"), container);
        compile("Panel", container, null);

        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/Confirm.gui",
                "<component type=\"Dialog\" name=\"Confirm\" title=\"Confirm\" layout=\"BoxLayout\" bindingStrategy=\"none\">"
                + "<component type=\"Label\" name=\"caption\" text=\"Caption\"/></component>"));
        String dialog = invoke(builder, "defaultCompanionSource");
        assertTrue(dialog.contains("public class Confirm extends Dialog"), dialog);
        assertTrue(dialog.contains("super(\"Confirm\", BoxLayout.y());"), dialog);
        compile("Confirm", dialog, null);
    }

    /**
     * Projects scaffolded by cn1:create-gui-form before this editor existed carry the old markers.
     * Refusing to touch them meant designing a form, saving, and running an empty screen.
     */
    @Test
    void aLegacyScaffoldedCompanionIsMigratedAndKeepsUserCode() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "public class LoginForm extends com.codename1.ui.Form {\n"
                + "    public LoginForm() {\n"
                + "        this(com.codename1.ui.util.Resources.getGlobalResources());\n"
                + "    }\n"
                + "    \n"
                + "    public LoginForm(com.codename1.ui.util.Resources resourceObjectInstance) {\n"
                + "        initGuiBuilderComponents(resourceObjectInstance);\n"
                + "    }\n"
                + "    \n"
                + "    private void onSubmit(com.codename1.ui.events.ActionEvent event) {\n"
                + "        System.out.println(\"handled { } \");\n"
                + "    }\n"
                + "    \n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {\n"
                + "    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "}\n";
        String generated = invoke(builder, "defaultCompanionSource");
        String merged = merge(builder, legacy, generated);

        assertTrue(merged.contains("email = new TextField"), "the component tree was never generated:\n" + merged);
        assertTrue(merged.contains("private void onSubmit(com.codename1.ui.events.ActionEvent event)"),
                "the developer's own method was lost:\n" + merged);
        assertFalse(merged.contains("initGuiBuilderComponents"), "the legacy generated block survived:\n" + merged);
        assertFalse(merged.contains("DON'T EDIT"), merged);
        assertFalse(merged.contains("getGlobalResources"), "the legacy constructors survived:\n" + merged);
        compile(merged, null);
    }

    /**
     * Migration regenerates the header, so an import the developer added for their own method has
     * to be carried across or the project stops compiling on the first save in this editor.
     */
    @Test
    void migratingALegacyCompanionKeepsTheImportsItsUserCodeNeeds() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import java.util.ArrayList;\n"
                + "import java.util.List;\n"
                + "public class LoginForm extends com.codename1.ui.Form {\n"
                + "    private final List<String> attempts = new ArrayList<String>();\n"
                + "    public LoginForm() {\n"
                + "        initGuiBuilderComponents(null);\n"
                + "    }\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    private void initGuiBuilderComponents(com.codename1.ui.util.Resources r) {\n"
                + "    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "}\n";
        String generated = invoke(builder, "defaultCompanionSource");
        String merged = merge(builder, legacy, generated);

        assertTrue(merged.contains("import java.util.ArrayList;"),
                "the import the carried field needs was dropped:\n" + merged);
        assertTrue(merged.contains("import java.util.List;"), merged);
        assertTrue(merged.contains("attempts"), "the developer's field was lost:\n" + merged);
        compile(merged, null);
    }

    @Test
    void aHandWrittenCompanionIsLeftAlone() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        String handWritten = "package com.example;\npublic class LoginForm extends com.codename1.ui.Form {\n}\n";
        assertEquals(handWritten, merge(builder, handWritten, invoke(builder, "defaultCompanionSource")));
    }

    private static String merge(CodenameOneGUIBuilder builder, String existing, String generated) throws Exception {
        Method method = CodenameOneGUIBuilder.class.getDeclaredMethod("mergeGeneratedSource", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, existing, generated);
    }

    private static CodenameOneGUIBuilder builder(String strategy) throws Exception {
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        set(builder, "binding", ProjectBinding.parse(
                "projectDir=/tmp/project\nguiDir=/tmp/project/gui\nsourceDir=/tmp/project/java\ncssFile=/tmp/project/theme.css\n"));
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\"" + strategy + "\">"
                + "<component type=\"TextField\" name=\"email\" hint=\"Email\"/>"
                + "<component type=\"CheckBox\" name=\"remember\" text=\"Remember\"/>"
                + "<component type=\"Button\" name=\"submit\" text=\"Sign in\" actionEvent=\"onSubmit\"/>"
                + "</component>"));
        return builder;
    }

    private static void compile(String form, String model) throws Exception {
        compile("LoginForm", form, model);
    }

    /**
     * Compiles into the temporary directory rather than beside the sources: without {@code -d} the
     * class files land in the working directory the test suite runs from.
     */
    private static void compile(String className, String form, String model) throws Exception {
        Path root = Files.createTempDirectory("guibuilder-generated-source");
        Path classes = Files.createDirectories(root.resolve("classes"));
        Path pkg = Files.createDirectories(root.resolve("com/example"));
        Path formFile = pkg.resolve(className + ".java");
        Files.write(formFile, form.getBytes(StandardCharsets.UTF_8));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int result;
        if (model == null) {
            result = compiler.run(null, null, errors, "-d", classes.toString(),
                    "-classpath", System.getProperty("java.class.path"), formFile.toString());
        } else {
            Path modelFile = pkg.resolve(className + "Model.java");
            Files.write(modelFile, model.getBytes(StandardCharsets.UTF_8));
            result = compiler.run(null, null, errors, "-d", classes.toString(),
                    "-classpath", System.getProperty("java.class.path"),
                    formFile.toString(), modelFile.toString());
        }
        assertEquals(0, result, "the generated source did not compile:\n"
                + errors.toString("UTF-8") + "\n" + form);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = CodenameOneGUIBuilder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static String invoke(Object target, String name) throws Exception {
        Method method = CodenameOneGUIBuilder.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return (String) method.invoke(target);
    }
}
