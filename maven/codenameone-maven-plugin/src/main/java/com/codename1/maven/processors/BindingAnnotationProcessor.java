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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.FieldInfo;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.MethodInfo;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Build-time `@Bindable` processor. Generates one `XxxBinder` Java class per
/// `@Bindable` type plus a `BindersIndex` that lazily registers them all with
/// `com.codename1.binding.Binders` on first use.
public final class BindingAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String BINDABLE_DESC = "Lcom/codename1/annotations/Bindable;";
    public static final String BIND_DESC = "Lcom/codename1/annotations/Bind;";

    static final String GENERATED_PACKAGE = "com.codename1.binding.generated";

    private static final Set<String> DESCRIPTORS;
    static {
        Set<String> s = new LinkedHashSet<String>();
        s.add(BINDABLE_DESC);
        DESCRIPTORS = Collections.unmodifiableSet(s);
    }

    private final TreeMap<String, BindableClass> accepted = new TreeMap<String, BindableClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) return;
        if (cls.getClassAnnotation(BINDABLE_DESC) == null) return;
        if (cls.isAbstract() || cls.isInterface()) {
            ctx.error(cls, "@Bindable requires a concrete class; " + cls.getBinaryName()
                    + " is abstract or an interface");
            return;
        }

        BindableClass bc = new BindableClass();
        bc.binaryName = cls.getBinaryName();
        bc.simpleName = simpleName(cls.getBinaryName());
        bc.binderSimpleName = bc.simpleName + "Binder";
        bc.binderBinaryName = GENERATED_PACKAGE + "." + bc.binderSimpleName;

        for (FieldInfo f : cls.getFields()) {
            if (f.isStatic()) continue;
            AnnotationValues bind = f.getAnnotation(BIND_DESC);
            if (bind == null) continue;
            if (!f.isPublic()) {
                ctx.error(cls, "@Bind on " + bc.binaryName + "." + f.getName()
                        + " requires a public field");
                continue;
            }
            String compName = bind.getString("name");
            if (compName == null || compName.length() == 0) {
                ctx.error(cls, "@Bind on " + bc.binaryName + "." + f.getName()
                        + " requires name() to identify the target component");
                continue;
            }
            BoundField bf = new BoundField();
            bf.fieldName = f.getName();
            bf.componentName = compName;
            bf.attr = readAttr(bind);
            bf.twoWay = bind.getBoolOrDefault("twoWay", true);
            bf.kind = PropertyTypeKind.of(f);
            if (bf.kind.kind == PropertyTypeKind.Kind.UNSUPPORTED) {
                ctx.error(cls, "@Bind field " + bc.binaryName + "." + f.getName()
                        + " has an unsupported type (descriptor " + f.getDescriptor() + ")");
                continue;
            }
            bc.fields.add(bf);
        }
        // An @Bindable class with no @Bind fields is still accepted -- the
        // generated binder is a no-op, but it stays in the index so the user
        // gets a registration hit even if they remove every @Bind later.
        accepted.put(bc.binaryName, bc);
    }

    private static BindAttrName readAttr(AnnotationValues bind) {
        Object v = bind.get("attr");
        // ASM gives enums back as `String[] { internalDescriptor, valueName }`.
        if (v instanceof String[]) {
            String name = ((String[]) v)[1];
            for (BindAttrName candidate : BindAttrName.values()) {
                if (candidate.name().equals(name)) return candidate;
            }
        }
        return BindAttrName.TEXT;
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) return;
        if (accepted.isEmpty()) return;

        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (BindableClass bc : accepted.values()) {
            sources.put(bc.binderBinaryName, generateBinderSource(bc));
        }
        sources.put(GENERATED_PACKAGE + ".BindersIndex", generateIndexSource(accepted.values()));
        try {
            java.util.List<java.io.File> cp = new java.util.ArrayList<java.io.File>();
            cp.add(ctx.getOutputClassDir());
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile generated binder sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + accepted.size()
                + " @Bindable binder(s) under " + GENERATED_PACKAGE);
    }

    // ---------------------------------------------------------------
    // Source generation
    // ---------------------------------------------------------------

    private static String generateBinderSource(BindableClass bc) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("package ").append(GENERATED_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(bc.binderSimpleName)
                .append(" implements com.codename1.binding.Binder<").append(bc.binaryName).append("> {\n\n");

        sb.append("    public Class<").append(bc.binaryName).append("> type() {\n");
        sb.append("        return ").append(bc.binaryName).append(".class;\n");
        sb.append("    }\n\n");

        sb.append("    public com.codename1.binding.Binding bind(final ").append(bc.binaryName)
                .append(" model, final com.codename1.ui.Container container) {\n");
        sb.append("        final java.util.ArrayList<com.codename1.ui.events.ActionListener> _disposers = new java.util.ArrayList<com.codename1.ui.events.ActionListener>();\n");

        // Resolve every component up front so refresh() / commit() / disconnect()
        // never re-walk the container.
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            sb.append("        final com.codename1.ui.Component _c").append(i)
                    .append(" = _findByName(container, \"").append(escape(f.componentName)).append("\");\n");
        }

        // Initial push from model -> components.
        sb.append("        final Runnable _refresh = new Runnable() { public void run() {\n");
        for (int i = 0; i < bc.fields.size(); i++) {
            emitRefreshOne(sb, bc.fields.get(i), i);
        }
        sb.append("        }};\n");
        sb.append("        _refresh.run();\n");

        // Two-way listeners.
        sb.append("        final Runnable _commit = new Runnable() { public void run() {\n");
        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            if (!f.twoWay) continue;
            emitCommitOne(sb, f, i);
        }
        sb.append("        }};\n");

        for (int i = 0; i < bc.fields.size(); i++) {
            BoundField f = bc.fields.get(i);
            if (!f.twoWay) continue;
            emitListenerInstall(sb, f, i);
        }

        sb.append("        return new com.codename1.binding.Binding() {\n");
        sb.append("            public void refresh() { _refresh.run(); }\n");
        sb.append("            public void commit() { _commit.run(); }\n");
        sb.append("            public void disconnect() {\n");
        sb.append("                for (com.codename1.ui.events.ActionListener _d : _disposers) _d.actionPerformed(null);\n");
        sb.append("                _disposers.clear();\n");
        sb.append("            }\n");
        sb.append("        };\n");
        sb.append("    }\n\n");

        // Recursive component-by-name lookup. cn1 doesn't ship one as a
        // public Container API, so we inline it -- keeps each binder
        // self-contained with no shared runtime dependency.
        sb.append("    private static com.codename1.ui.Component _findByName(com.codename1.ui.Container c, String name) {\n");
        sb.append("        if (c == null || name == null) return null;\n");
        sb.append("        if (name.equals(c.getName())) return c;\n");
        sb.append("        int n = c.getComponentCount();\n");
        sb.append("        for (int i = 0; i < n; i++) {\n");
        sb.append("            com.codename1.ui.Component child = c.getComponentAt(i);\n");
        sb.append("            if (name.equals(child.getName())) return child;\n");
        sb.append("            if (child instanceof com.codename1.ui.Container) {\n");
        sb.append("                com.codename1.ui.Component found = _findByName((com.codename1.ui.Container) child, name);\n");
        sb.append("                if (found != null) return found;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        return null;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String generateIndexSource(Iterable<BindableClass> classes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package ").append(GENERATED_PACKAGE).append(";\n\n");
        sb.append("// Auto-generated by cn1:process-annotations. Do not edit.\n");
        sb.append("public final class BindersIndex {\n");
        sb.append("    public BindersIndex() {\n");
        for (BindableClass bc : classes) {
            sb.append("        com.codename1.binding.Binders.register(new ").append(bc.binderSimpleName).append("());\n");
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Per-attribute code generation
    // ---------------------------------------------------------------

    private static void emitRefreshOne(StringBuilder sb, BoundField f, int i) {
        sb.append("            if (_c").append(i).append(" != null) {\n");
        String modelExpr = readModelExpr(f);
        switch (f.attr) {
            case TEXT:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? \"\" : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) ((com.codename1.ui.TextArea) _c").append(i).append(").setText(_v);\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.Label) ((com.codename1.ui.Label) _c").append(i).append(").setText(_v);\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.Button) ((com.codename1.ui.Button) _c").append(i).append(").setText(_v);\n");
                break;
            case UIID:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? \"\" : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                _c").append(i).append(".setUIID(_v);\n");
                break;
            case HIDDEN:
                sb.append("                _c").append(i).append(".setHidden(").append(booleanModelExpr(f, modelExpr)).append(");\n");
                break;
            case VISIBLE:
                sb.append("                _c").append(i).append(".setVisible(").append(booleanModelExpr(f, modelExpr)).append(");\n");
                break;
            case ENABLED:
                sb.append("                _c").append(i).append(".setEnabled(").append(booleanModelExpr(f, modelExpr)).append(");\n");
                break;
            case SELECTED:
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.RadioButton) ((com.codename1.ui.RadioButton) _c").append(i).append(").setSelected(").append(booleanModelExpr(f, modelExpr)).append(");\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.CheckBox) ((com.codename1.ui.CheckBox) _c").append(i).append(").setSelected(").append(booleanModelExpr(f, modelExpr)).append(");\n");
                break;
            case ICON_NAME:
                sb.append("                String _v = ").append(modelExpr).append(" == null ? null : String.valueOf(").append(modelExpr).append(");\n");
                sb.append("                if (_v != null && _c").append(i).append(" instanceof com.codename1.ui.Label) {\n");
                sb.append("                    com.codename1.ui.util.Resources _r = com.codename1.ui.util.Resources.getGlobalResources();\n");
                sb.append("                    if (_r != null) ((com.codename1.ui.Label) _c").append(i).append(").setIcon(_r.getImage(_v));\n");
                sb.append("                }\n");
                break;
            case NAME:
                sb.append("                _c").append(i).append(".setName(String.valueOf(").append(modelExpr).append("));\n");
                break;
        }
        sb.append("            }\n");
    }

    private static void emitCommitOne(StringBuilder sb, BoundField f, int i) {
        sb.append("            if (_c").append(i).append(" != null) {\n");
        switch (f.attr) {
            case TEXT:
                sb.append("                String _v = null;\n");
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) _v = ((com.codename1.ui.TextArea) _c").append(i).append(").getText();\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.Label) _v = ((com.codename1.ui.Label) _c").append(i).append(").getText();\n");
                sb.append("                if (_v != null) {\n");
                emitWriteModelFromString(sb, f, "_v");
                sb.append("                }\n");
                break;
            case SELECTED:
                sb.append("                boolean _v = false;\n");
                sb.append("                if (_c").append(i).append(" instanceof com.codename1.ui.RadioButton) _v = ((com.codename1.ui.RadioButton) _c").append(i).append(").isSelected();\n");
                sb.append("                else if (_c").append(i).append(" instanceof com.codename1.ui.CheckBox) _v = ((com.codename1.ui.CheckBox) _c").append(i).append(").isSelected();\n");
                emitWriteModelFromBoolean(sb, f, "_v");
                break;
            default:
                break;
        }
        sb.append("            }\n");
    }

    private static void emitListenerInstall(StringBuilder sb, BoundField f, int i) {
        // Action listener captures the model field by reference; we listen on
        // both TextArea (DataChanged via DataChangedListener -> ActionListener
        // bridge isn't free, so we use addDataChangedListener) and CheckBox /
        // RadioButton.
        if (f.attr == BindAttrName.TEXT) {
            sb.append("        if (_c").append(i).append(" instanceof com.codename1.ui.TextArea) {\n");
            sb.append("            final com.codename1.ui.TextArea _ta = (com.codename1.ui.TextArea) _c").append(i).append(";\n");
            sb.append("            final com.codename1.ui.events.DataChangedListener _l = new com.codename1.ui.events.DataChangedListener() {\n");
            sb.append("                public void dataChanged(int type, int index) {\n");
            sb.append("                    String _v = _ta.getText();\n");
            emitWriteModelFromString(sb, f, "_v");
            sb.append("                }\n");
            sb.append("            };\n");
            sb.append("            _ta.addDataChangedListener(_l);\n");
            sb.append("            _disposers.add(new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) { _ta.removeDataChangedListener(_l); }\n");
            sb.append("            });\n");
            sb.append("        }\n");
        } else if (f.attr == BindAttrName.SELECTED) {
            sb.append("        if (_c").append(i).append(" instanceof com.codename1.ui.Button) {\n");
            sb.append("            final com.codename1.ui.Button _b = (com.codename1.ui.Button) _c").append(i).append(";\n");
            sb.append("            final com.codename1.ui.events.ActionListener _l = new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) {\n");
            sb.append("                    boolean _v = false;\n");
            sb.append("                    if (_b instanceof com.codename1.ui.RadioButton) _v = ((com.codename1.ui.RadioButton) _b).isSelected();\n");
            sb.append("                    else if (_b instanceof com.codename1.ui.CheckBox) _v = ((com.codename1.ui.CheckBox) _b).isSelected();\n");
            emitWriteModelFromBoolean(sb, f, "_v");
            sb.append("                }\n");
            sb.append("            };\n");
            sb.append("            _b.addActionListener(_l);\n");
            sb.append("            _disposers.add(new com.codename1.ui.events.ActionListener() {\n");
            sb.append("                public void actionPerformed(com.codename1.ui.events.ActionEvent _e) { _b.removeActionListener(_l); }\n");
            sb.append("            });\n");
            sb.append("        }\n");
        }
    }

    private static String readModelExpr(BoundField f) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
            return "model." + f.fieldName + ".get()";
        }
        return "model." + f.fieldName;
    }

    private static String booleanModelExpr(BoundField f, String modelExpr) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY
                && "java.lang.Boolean".equals(f.kind.elementBinaryName)) {
            return "Boolean.TRUE.equals(" + modelExpr + ")";
        }
        if (f.kind.kind == PropertyTypeKind.Kind.BOOLEAN) {
            return modelExpr;
        }
        return modelExpr + " != null && Boolean.parseBoolean(String.valueOf(" + modelExpr + "))";
    }

    private static void emitWriteModelFromString(StringBuilder sb, BoundField f, String src) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY) {
            String elem = f.kind.elementBinaryName;
            if ("java.lang.String".equals(elem)) {
                sb.append("                    model.").append(f.fieldName).append(".set(").append(src).append(");\n");
            } else if ("java.lang.Integer".equals(elem)) {
                sb.append("                    try { model.").append(f.fieldName).append(".set(Integer.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else if ("java.lang.Long".equals(elem)) {
                sb.append("                    try { model.").append(f.fieldName).append(".set(Long.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else if ("java.lang.Double".equals(elem)) {
                sb.append("                    try { model.").append(f.fieldName).append(".set(Double.valueOf(").append(src).append(")); } catch (NumberFormatException _nfe) {}\n");
            } else {
                sb.append("                    model.").append(f.fieldName).append(".set((").append(elem).append(") ").append(src).append(");\n");
            }
        } else if (f.kind.kind == PropertyTypeKind.Kind.STRING) {
            sb.append("                    model.").append(f.fieldName).append(" = ").append(src).append(";\n");
        } else if (f.kind.kind == PropertyTypeKind.Kind.INT) {
            sb.append("                    try { model.").append(f.fieldName).append(" = Integer.parseInt(").append(src).append("); } catch (NumberFormatException _nfe) {}\n");
        } else if (f.kind.kind == PropertyTypeKind.Kind.LONG) {
            sb.append("                    try { model.").append(f.fieldName).append(" = Long.parseLong(").append(src).append("); } catch (NumberFormatException _nfe) {}\n");
        } else if (f.kind.kind == PropertyTypeKind.Kind.DOUBLE) {
            sb.append("                    try { model.").append(f.fieldName).append(" = Double.parseDouble(").append(src).append("); } catch (NumberFormatException _nfe) {}\n");
        } else if (f.kind.kind == PropertyTypeKind.Kind.FLOAT) {
            sb.append("                    try { model.").append(f.fieldName).append(" = Float.parseFloat(").append(src).append("); } catch (NumberFormatException _nfe) {}\n");
        }
    }

    private static void emitWriteModelFromBoolean(StringBuilder sb, BoundField f, String src) {
        if (f.kind.kind == PropertyTypeKind.Kind.PROPERTY
                && "java.lang.Boolean".equals(f.kind.elementBinaryName)) {
            sb.append("                model.").append(f.fieldName).append(".set(Boolean.valueOf(").append(src).append("));\n");
        } else if (f.kind.kind == PropertyTypeKind.Kind.BOOLEAN) {
            sb.append("                model.").append(f.fieldName).append(" = ").append(src).append(";\n");
        }
    }

    private static String simpleName(String binary) {
        int dot = binary.lastIndexOf('.');
        return dot < 0 ? binary : binary.substring(dot + 1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    // ---------------------------------------------------------------
    // Accumulator types
    // ---------------------------------------------------------------

    /// Mirror of `com.codename1.binding.BindAttr` -- can't reference the enum
    /// directly here because the plugin module doesn't depend on cn1-core.
    enum BindAttrName { TEXT, UIID, HIDDEN, VISIBLE, ENABLED, SELECTED, ICON_NAME, NAME }

    static final class BindableClass {
        String binaryName;
        String simpleName;
        String binderBinaryName;
        String binderSimpleName;
        final List<BoundField> fields = new ArrayList<BoundField>();
    }

    static final class BoundField {
        String fieldName;
        String componentName;
        BindAttrName attr;
        boolean twoWay;
        PropertyTypeKind kind;
    }
}
