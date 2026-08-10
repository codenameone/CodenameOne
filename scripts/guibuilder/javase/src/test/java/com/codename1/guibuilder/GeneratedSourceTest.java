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
import com.codename1.guibuilder.ui.ComponentPreviewFactory;
import com.codename1.ui.Component;
import com.codename1.xml.Element;
import com.codename1.guibuilder.project.ProjectIO;
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

    @Test
    void legacyAliasesFollowTheOriginalNamesRatherThanTheDisambiguatedFields() throws Exception {
        // "email" and "Email" produce one pair of accessors, so assignJavaNames() makes the second
        // field Email2. The alias in the developer's code is still gui_Email -- that is what the
        // old builder declared -- so deriving the alias from the assigned name searched for
        // gui_Email2, found nothing, and left the migrated companion referring to gui_Email.
        CodenameOneGUIBuilder builder = builderFor("none",
                "<component type=\"TextField\" name=\"email\" hint=\"Email\"/>"
                + "<component type=\"TextField\" name=\"Email\" hint=\"Second\"/>");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    private com.codename1.ui.TextField gui_email;\n"
                + "    private com.codename1.ui.TextField gui_Email;\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public void describe() {\n"
                + "        gui_email.setText(gui_Email.getText());\n"
                + "    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertTrue(migrated.contains("email.setText(Email2.getText())"),
                "both aliases must be rewritten to the fields the class declares:\n" + migrated);
        assertFalse(migrated.contains("gui_"), "no legacy alias may survive:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void aCommentDelimiterInsideAStringLiteralDoesNotMaskTheCodeAfterIt() throws Exception {
        String source = "class A {\n"
                + "    String glob = \"/*\";\n"
                + "    String url = \"http://example.com\";\n"
                + "    char quote = '\\'';\n"
                + "    public void onSubmit(ActionEvent event) { }\n"
                + "}\n";
        String stripped = CodenameOneGUIBuilder.stripComments(source);

        assertEquals(source.length(), stripped.length(), "offsets must survive the masking");
        assertTrue(stripped.contains("public void onSubmit(ActionEvent event)"),
                "a comment delimiter inside a literal must not mask the declarations after it:\n" + stripped);
        assertTrue(stripped.contains("String url ="), "the line after a // inside a literal is code");
        assertFalse(stripped.contains("example.com"), "the literal body itself is not code");
        assertTrue(CodenameOneGUIBuilder.stripComments("int a; /* void onSubmit(ActionEvent e) */ int b;")
                .indexOf("onSubmit") < 0, "a real comment is still masked");
    }

    @Test
    void aStringLiteralHoldingACommentDelimiterDoesNotDuplicateAnExistingHandler() throws Exception {
        // The end of that story: the masked declaration made declaresActionHandler() report the
        // handler missing, and the migration then carried the developer's method over next to a
        // freshly generated stub with the same signature.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "import com.codename1.ui.events.ActionEvent;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    private String glob = \"/*\";\n"
                + "    public void onSubmit(ActionEvent event) {\n"
                + "        setTitle(glob);\n"
                + "    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        int first = migrated.indexOf("void onSubmit(");
        assertTrue(first >= 0, "the developer's handler must be carried over:\n" + migrated);
        assertEquals(-1, migrated.indexOf("void onSubmit(", first + 1),
                "the stub must be dropped rather than declared a second time:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void aHandlerDeclaringAThrowsClauseIsNotDeclaredATwiceOver() throws Exception {
        // The scanner wanted the body brace immediately after the parameter list, so a throws
        // clause between them read as "no such handler" and Save appended a second method with the
        // same signature -- a companion that does not compile.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "import com.codename1.ui.events.ActionEvent;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public void onSubmit(ActionEvent event) throws RuntimeException {\n"
                + "        setTitle(\"submitted\");\n"
                + "    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        int first = migrated.indexOf("void onSubmit(");
        assertTrue(first >= 0, "the developer's handler must be carried over:\n" + migrated);
        assertEquals(-1, migrated.indexOf("void onSubmit(", first + 1),
                "a throws clause does not make it a different method:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void aThrowsLikeIdentifierIsNotMistakenForAClause() throws Exception {
        // The guard has to be narrow: "throwsSomething" is an identifier, and a call rather than a
        // declaration must still not count.
        CodenameOneGUIBuilder builder = builder("none");
        assertFalse(declares(builder, "helper.onSubmit(new ActionEvent(this)); {", "onSubmit"),
                "a receiver-qualified call is not a declaration");
        assertFalse(declares(builder, "void onSubmit(ActionEvent event) throwsRuntimeException {}", "onSubmit"),
                "throwsRuntimeException is one identifier, not a clause and a type");
        assertTrue(declares(builder, "void onSubmit(ActionEvent event) throws java.io.IOException, IllegalStateException {}", "onSubmit"),
                "a qualified, multi-exception clause is still a declaration");
        assertFalse(declares(builder, "abstract void onSubmit(ActionEvent event) throws RuntimeException;", "onSubmit"),
                "a declaration with no body is not a stub that can be dropped");
    }

    private static boolean declares(CodenameOneGUIBuilder builder, String source, String handler) throws Exception {
        Method method = CodenameOneGUIBuilder.class.getDeclaredMethod("declaresActionHandler", String.class, String.class);
        method.setAccessible(true);
        return ((Boolean) method.invoke(builder, source, handler)).booleanValue();
    }

    @Test
    void theCodePaneSaveRegeneratesAModelTheStrategyChangeAskedFor() throws Exception {
        // "Regenerate on save" is recorded against the document, not against whichever pane saves
        // it. Honouring it only in the toolbar save committed this companion against the previous
        // strategy's model and left the project uncompilable until something else saved the form.
        // No hyphen in the name: the package the companion declares is derived from this path.
        Path project = Files.createTempDirectory("guibuilderModelRegen");
        Path java = Files.createDirectories(project.resolve("src/main/java/com/example"));
        Path gui = Files.createDirectories(project.resolve("src/main/guibuilder/com/example"));
        Path model = java.resolve("LoginFormModel.java");
        Files.write(model, "package com.example;\n// the previous strategy's model\n".getBytes(StandardCharsets.UTF_8));

        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        set(builder, "binding", ProjectBinding.parse("projectDir=" + project + "\nguiDir="
                + project.resolve("src/main/guibuilder") + "\nsourceDir=" + project.resolve("src/main/java")
                + "\ncssFile=" + project.resolve("theme.css") + "\n"));
        set(builder, "document", GuiDocument.parse(gui.resolve("LoginForm.gui").toString(),
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\"bindable\">"
                + "<component type=\"TextField\" name=\"email\" hint=\"Email\"/></component>"));
        set(builder, "regenerateModelFor", field(builder, "document"));

        Method save = CodenameOneGUIBuilder.class.getDeclaredMethod("saveSourceAndModel", String.class, String.class);
        save.setAccessible(true);
        save.invoke(builder, java.resolve("LoginForm.java").toString(), invoke(builder, "defaultCompanionSource"));

        String written = new String(Files.readAllBytes(model), StandardCharsets.UTF_8);
        assertFalse(written.contains("the previous strategy's model"),
                "the pending regeneration must be part of this save:\n" + written);
        assertTrue(written.contains("@Bindable"), "the model must follow the strategy now set:\n" + written);
        assertNull(field(builder, "regenerateModelFor"), "the request is spent once the model is on disk");
        compile("LoginForm", new String(Files.readAllBytes(java.resolve("LoginForm.java")), StandardCharsets.UTF_8), written);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void migrationKeepsTheDevelopersOwnConstructors() throws Exception {
        // The scaffold's constructors go because the generated class declares its own; an overload
        // the developer added is theirs, and deleting it silently on the first save broke callers
        // that had nothing to do with the GUI builder.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    public LoginForm() {\n        initGuiBuilderComponents();\n    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public LoginForm(String heading) {\n"
                + "        this();\n        setTitle(heading);\n    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertTrue(migrated.contains("LoginForm(String heading)"),
                "the developer's overload must survive migration:\n" + migrated);
        assertFalse(migrated.contains("initGuiBuilderComponents"),
                "the scaffolded constructor must not:\n" + migrated);
        int noArg = migrated.indexOf("public LoginForm() {");
        assertTrue(noArg >= 0 && migrated.indexOf("public LoginForm() {", noArg + 1) < 0,
                "exactly one no-arg constructor:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void migrationCarriesInterfacesAndRefusesAnUnknownBaseClass() throws Exception {
        CodenameOneGUIBuilder builder = builder("none");
        String head = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "import java.util.Observer;\nimport java.util.Observable;\n";
        String body = "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public void update(Observable o, Object arg) {\n    }\n"
                + "}\n";

        String migrated = migrate(builder,
                head + "public class LoginForm extends Form implements Observer {\n" + body,
                invoke(builder, "defaultCompanionSource"));
        assertTrue(migrated.contains("implements Observer"),
                "dropping the interface changes what the class is to every caller:\n" + migrated);
        compile("LoginForm", migrated, null);

        // A base class this generator cannot reproduce -- it emits super(title, layout), which is
        // not inherited -- is refused rather than silently replaced.
        String custom = head + "public class LoginForm extends BaseForm implements Observer {\n" + body;
        assertNull(migrate(builder, custom, invoke(builder, "defaultCompanionSource")),
                "a custom base class must stop the migration, leaving the file untouched");
    }

    @Test
    void aCustomResourcesOverloadIsNotMistakenForTheScaffoldsOwn() throws Exception {
        // Exactly one Resources parameter is the scaffold's overload, and the only one
        // legacyResourcesConstructor() puts back. A list that merely contains the word took the
        // developer's two-argument constructor with it and replaced it with a different signature.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "import com.codename1.ui.util.Resources;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    public LoginForm(Resources res) {\n        initGuiBuilderComponents();\n    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public LoginForm(Resources res, String heading) {\n"
                + "        this();\n        setTitle(heading);\n    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertTrue(migrated.contains("LoginForm(Resources res, String heading)"),
                "the developer's overload must survive:\n" + migrated);
        assertTrue(migrated.contains("Resources resourceObjectInstance"),
                "the scaffold's one-argument overload is still replaced by the delegate:\n" + migrated);
        assertFalse(migrated.contains("initGuiBuilderComponents"), migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void aRefusedMigrationFailsTheSaveInsteadOfReportingSuccess() throws Exception {
        // mergeGeneratedSource() reads a refusal as "keep the existing companion", which is right
        // for a hand-written file. Here it meant the .gui was written and the document marked
        // clean while the Java the application runs never changed.
        Path project = Files.createTempDirectory("guibuilderRefusedSave");
        Path java = Files.createDirectories(project.resolve("src/main/java/com/example"));
        Path gui = Files.createDirectories(project.resolve("src/main/guibuilder/com/example"));
        Path guiFile = gui.resolve("LoginForm.gui");
        String originalXml = "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\">"
                + "<component type=\"TextField\" name=\"email\" hint=\"Email\"/></component>";
        Files.write(guiFile, originalXml.getBytes(StandardCharsets.UTF_8));
        String legacy = "package com.example;\n"
                + "public class LoginForm extends BaseForm {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "}\n";
        Path companion = java.resolve("LoginForm.java");
        Files.write(companion, legacy.getBytes(StandardCharsets.UTF_8));

        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        set(builder, "binding", ProjectBinding.parse("projectDir=" + project + "\nguiDir="
                + project.resolve("src/main/guibuilder") + "\nsourceDir=" + project.resolve("src/main/java")
                + "\ncssFile=" + project.resolve("theme.css") + "\n"));
        // The fsUrl form, which is what findGuiFiles() hands the builder. A plain path makes
        // companionSourcePath() strip the wrong prefix length and look for a companion elsewhere.
        GuiDocument document = GuiDocument.parse(ProjectIO.fsUrl(guiFile.toString()), originalXml);
        set(builder, "document", document);
        document.select(document.components().get(1));
        document.setAttribute("text", "changed");
        assertTrue(document.isModified(), "fixture: the form must have something to save");

        Method save = CodenameOneGUIBuilder.class.getDeclaredMethod("save");
        save.setAccessible(true);
        assertFalse(((Boolean) save.invoke(builder)).booleanValue(), "the save must report failure");

        assertEquals(legacy, new String(Files.readAllBytes(companion), StandardCharsets.UTF_8),
                "the companion is untouched, which is the whole point of the refusal");
        assertEquals(originalXml, new String(Files.readAllBytes(guiFile), StandardCharsets.UTF_8),
                "the .gui must not be written against Java that will never match it");
        assertTrue(document.isModified(), "the document still holds unsaved changes");
    }

    @Test
    void anAliasShapedStringOrCommentIsLeftAloneWhileTheFieldIsRenamed() throws Exception {
        // The alias rewrite used to be textual, so an annotation value or an analytics key that
        // merely looks like a field reference was rewritten too -- the first save quietly changed
        // behaviour that has nothing to do with the generated field.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    private String key = \"gui_submit\";\n"
                + "    // gui_submit is described here\n"
                + "    public void describe() {\n"
                + "        gui_submit.setText(key);\n"
                + "        String label = \"tap gui_submit now\";\n"
                + "        setTitle(label);\n"
                + "    }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertTrue(migrated.contains("submit.setText(key)"),
                "the real field reference must still be migrated:\n" + migrated);
        assertTrue(migrated.contains("\"gui_submit\""), "a string literal is data, not a reference:\n" + migrated);
        assertTrue(migrated.contains("tap gui_submit now"), "and so is text inside one:\n" + migrated);
        assertTrue(migrated.contains("// gui_submit is described here"), "a comment is not a reference:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void aCustomisedScaffoldConstructorStopsTheMigrationInsteadOfBeingDeleted() throws Exception {
        // Nothing replaces this signature and the call in its body is gone, so it can neither be
        // kept nor deleted. Deleting it took the developer's own logic with it.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "import com.codename1.ui.util.Resources;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public LoginForm(String title) {\n"
                + "        initGuiBuilderComponents(Resources.getGlobalResources());\n"
                + "        setTitle(title);\n"
                + "    }\n"
                + "}\n";

        assertNull(migrate(builder, legacy, invoke(builder, "defaultCompanionSource")),
                "the migration must refuse rather than delete the constructor");
    }

    @Test
    void theInspectorReportsTheUiidTheRuntimeActuallyUses() throws Exception {
        // Every non-root type: what the CSS selector field claims has to be what the preview -- and
        // therefore the generated application -- resolves styles from, or a rule written against
        // the designer's answer applies to nothing.
        String[] types = {"Button", "Label", "SpanLabel", "TextField", "TextArea", "CheckBox",
            "RadioButton", "Slider", "Tabs", "Accordion", "Container"};
        for (String type : types) {
            GuiDocument document = GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                    "<component type=\"Form\" name=\"LoginForm\" layout=\"BoxLayout\">"
                    + "<component type=\"" + type + "\" name=\"x\"/></component>");
            Element element = document.components().get(1);
            Component preview = ComponentPreviewFactory.create(element, null, null);
            assertEquals(preview.getUIID(), document.effectiveUiid(element),
                    "the inspector and the runtime disagree about " + type);
        }

        // The root is the exception the preview cannot answer: create() builds the content pane,
        // while the field describes the Form, which is what setUIID() sets on the generated class.
        GuiDocument form = GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" layout=\"BoxLayout\"/>");
        assertEquals("Form", form.effectiveUiid(form.root()));
        GuiDocument dialog = GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Dialog\" name=\"LoginForm\" layout=\"BoxLayout\"/>");
        assertEquals("Dialog", dialog.effectiveUiid(dialog.root()));
    }

    @Test
    void aQualifiedParameterAnnotationDoesNotHideTheHandler() throws Exception {
        // The annotation name stopped at the first dot, so the argument list was never recognised
        // and the comma inside it read as a second parameter. The declaration was missed and Save
        // appended a second method with the same signature.
        CodenameOneGUIBuilder builder = builder("none");
        assertTrue(declares(builder,
                "void onSubmit(@com.example.Foo(a = 1, b = 2) ActionEvent event) {}", "onSubmit"),
                "a qualified annotation carrying arguments is still just one ActionEvent parameter");
        assertTrue(declares(builder, "void onSubmit(@Foo ActionEvent event) {}", "onSubmit"));
        assertFalse(declares(builder,
                "void onSubmit(@com.example.Foo(a = 1) String other, ActionEvent event) {}", "onSubmit"),
                "two parameters are still two parameters");
    }

    @Test
    void adamagedUserCodeMarkerRefusesRatherThanWipingTheUserRegion() throws Exception {
        // The source pane leaves the generated regions editable on purpose, so a stray keystroke on
        // a marker line is easy. Reading that as "the user region is empty" replaced the file with
        // the template and every method the developer had written went with it.
        CodenameOneGUIBuilder builder = builder("none");
        String generated = invoke(builder, "defaultCompanionSource");
        String withUserCode = generated.replace("// <gui-builder-user-code>",
                "// <gui-builder-user-code>\n    public void mine() { }");
        String damaged = withUserCode.replace("// </gui-builder-user-code>", "// </gui-builder-user-codeX>");

        String merged = merge(builder, damaged, generated);

        assertTrue(merged.contains("public void mine() { }"),
                "the developer's method must survive a damaged marker:\n" + merged);
        assertEquals(damaged, merged, "nothing is rewritten while the markers are broken");
    }

    @Test
    void aScaffoldSignatureWithExtraSetupStopsTheMigration() throws Exception {
        // Same signature the scaffold used, but the developer added to it, and neither the
        // generated constructor nor the delegate puts those statements back.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public LoginForm() {\n"
                + "        initGuiBuilderComponents();\n"
                + "        setTitle(loadTitle());\n"
                + "    }\n"
                + "    private String loadTitle() { return \"Login\"; }\n"
                + "}\n";

        assertNull(migrate(builder, legacy, invoke(builder, "defaultCompanionSource")),
                "the migration must refuse rather than drop setTitle(loadTitle())");

        // The untouched scaffold is still migrated, or every legacy project would be refused.
        String plain = legacy.replace("        setTitle(loadTitle());\n", "");
        assertNotNull(migrate(builder, plain, invoke(builder, "defaultCompanionSource")),
                "a constructor that only does what the scaffold did is still removable");
    }

    @Test
    void aBraceInACommentDoesNotCutAConstructorInHalf() throws Exception {
        // matchingBrace() ignores braces in literals but was reading the raw text, so "// }" ended
        // the constructor early: the prefix was removed and its remaining statements were left
        // loose in the class body.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "    public LoginForm() {\n"
                + "        // the next brace is a decoy }\n"
                + "        initGuiBuilderComponents();\n"
                + "    }\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public void mine() { }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertNotNull(migrated, "the constructor is a plain scaffold and must migrate:\n" + legacy);
        assertFalse(migrated.contains("initGuiBuilderComponents"),
                "the whole constructor goes, not just its opening:\n" + migrated);
        assertFalse(migrated.contains("decoy"), "and its comment with it:\n" + migrated);
        assertTrue(migrated.contains("public void mine()"), "the developer's method stays:\n" + migrated);
        compile("LoginForm", migrated, null);
    }

    @Test
    void backspaceDeletesAWholeCodePoint() {
        // caret - 1 splits a surrogate pair and leaves half an emoji, which becomes a replacement
        // character the moment the text is stored.
        String emoji = "ab\uD83D\uDE00";
        assertEquals(2, CodenameOneGUIBuilder.previousCodePointStart(emoji, emoji.length()),
                "an emoji is one character to delete");
        assertEquals(1, CodenameOneGUIBuilder.previousCodePointStart("abc", 2));
        assertEquals(0, CodenameOneGUIBuilder.previousCodePointStart("abc", 0));
        assertEquals(2, CodenameOneGUIBuilder.previousCodePointStart("abc", 99),
                "a caret past the end is clamped, then steps back one character");
    }

    @Test
    void migrationCarriesTheClassAnnotationsTheHeaderRebuildWouldDrop() throws Exception {
        // The members all survive while the annotation that makes the class visible to a framework
        // does not -- the kind of change nobody looks for in a diff of generated code.
        CodenameOneGUIBuilder builder = builder("none");
        String legacy = "package com.example;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Deprecated\n"
                + "@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n"
                + "public class LoginForm extends Form {\n"
                + "//-- DON'T EDIT BELOW THIS LINE!!!\n"
                + "//-- DON'T EDIT ABOVE THIS LINE!!!\n"
                + "    public void mine() { }\n"
                + "}\n";

        String migrated = migrate(builder, legacy, invoke(builder, "defaultCompanionSource"));

        assertTrue(migrated.contains("@Deprecated"), "a class annotation must survive:\n" + migrated);
        assertTrue(migrated.contains("@SuppressWarnings({\"unchecked\", \"rawtypes\"})"),
                "including its arguments, commas and all:\n" + migrated);
        assertTrue(migrated.indexOf("@Deprecated") < migrated.indexOf("public class LoginForm"),
                "and must precede the declaration:\n" + migrated);
        compile("LoginForm", migrated, null);

        // An annotation on a member is not the class's.
        String memberAnnotated = legacy.replace("    public void mine() { }",
                "    @Deprecated\n    public void mine() { }");
        String second = migrate(builder, memberAnnotated, invoke(builder, "defaultCompanionSource"));
        assertEquals(1, countOccurrences(second.substring(0, second.indexOf("public class LoginForm")), "@Deprecated"),
                "the class keeps one @Deprecated, not the member's as well:\n" + second);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int at = text.indexOf(needle);
        while (at >= 0) {
            count++;
            at = text.indexOf(needle, at + needle.length());
        }
        return count;
    }

    private static String migrate(CodenameOneGUIBuilder builder, String existing, String generated) throws Exception {
        Method method = CodenameOneGUIBuilder.class.getDeclaredMethod("migrateLegacySource", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(builder, existing, generated);
    }

    /** A builder over a document with the given children, for the cases the fixed one cannot show. */
    private static CodenameOneGUIBuilder builderFor(String strategy, String children) throws Exception {
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        set(builder, "binding", ProjectBinding.parse(
                "projectDir=/tmp/project\nguiDir=/tmp/project/gui\nsourceDir=/tmp/project/java\ncssFile=/tmp/project/theme.css\n"));
        set(builder, "document", GuiDocument.parse("/tmp/project/gui/com/example/LoginForm.gui",
                "<component type=\"Form\" name=\"LoginForm\" title=\"Login\" layout=\"BoxLayout\" bindingStrategy=\""
                + strategy + "\">" + children + "</component>"));
        return builder;
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
