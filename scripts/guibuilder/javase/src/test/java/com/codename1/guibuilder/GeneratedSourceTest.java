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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedSourceTest {
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
        Path root = Files.createTempDirectory("guibuilder-generated-source");
        Path pkg = Files.createDirectories(root.resolve("com/example"));
        Path formFile = pkg.resolve("LoginForm.java");
        Files.write(formFile, form.getBytes(StandardCharsets.UTF_8));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        int result;
        if (model == null) {
            result = compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"), formFile.toString());
        } else {
            Path modelFile = pkg.resolve("LoginFormModel.java");
            Files.write(modelFile, model.getBytes(StandardCharsets.UTF_8));
            result = compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                    formFile.toString(), modelFile.toString());
        }
        assertEquals(0, result);
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
