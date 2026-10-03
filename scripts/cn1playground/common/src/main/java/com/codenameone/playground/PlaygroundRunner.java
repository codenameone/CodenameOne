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

import com.codename1.system.NativeLookup;
import com.codename1.tools.javac.ClassLibrary;
import com.codename1.tools.javac.Diagnostic;
import com.codename1.tools.javac.JavaCompiler;
import com.codename1.tools.javac.ScriptSpec;
import com.codename1.tools.javac.StubLibrary;
import com.codename1.tools.translator.JavascriptIncremental;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs Playground code: real Java, compiled in-process by the Codename One Java
 * compiler against the API the running VM contains ({@link PlaygroundApi}).
 *
 * <p>A script may mix type declarations, methods and statements at the top level
 * (the compiler's script mode wraps the methods and statements into
 * {@value #SCRIPT_CLASS}); it may also be a complete class with lifecycle methods
 * ({@code init}/{@code start}) or a {@code build(PlaygroundContext)} method, for
 * which a small launcher class is compiled in a second step. The compiled classes
 * are loaded into the running application -- translated to JavaScript and
 * evaluated in the VM worker on the web, defined by a class loader on the JavaSE
 * simulator -- and run on the EDT.
 *
 * <p>The component to preview is, in order: what a lifecycle {@code start()} or a
 * {@code build} returned, the script's trailing expression, a form the code
 * showed ({@code form.show()} is compiled to {@link PlaygroundContext#showForm}),
 * and finally the first form, then the first component, among the script's
 * top-level variables.
 */
final class PlaygroundRunner {
    static final String SCRIPT_CLASS = "PlaygroundScript";
    static final String LAUNCHER_CLASS = "PlaygroundLauncher";
    private static final String CONTEXT = "com.codenameone.playground.PlaygroundContext";
    private static final String CONTEXT_DESC = "Lcom/codenameone/playground/PlaygroundContext;";

    /** What every script sees without importing it (the user's own imports take precedence). */
    static final String[] DEFAULT_IMPORTS = {
        "java.util.*", "java.io.*",
        "com.codename1.ui.*", "com.codename1.ui.layouts.*", "com.codename1.components.*",
        "com.codename1.ui.geom.*", "com.codename1.ui.events.*",
        "com.codename1.ui.plaf.Style", "com.codename1.ui.plaf.UIManager", "com.codename1.ui.util.Resources",
        CONTEXT, "com.codenameone.playground.GameScripting", "com.codenameone.playground.GpuScripting",
        "static com.codenameone.playground.PlaygroundGlobals.*"
    };

    static final class Diagnostic {
        final int line;
        final int column;
        final int endLine;
        final int endColumn;
        final String message;
        final String severity;

        Diagnostic(int line, int column, int endLine, int endColumn, String message, String severity) {
            this.line = line;
            this.column = column;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.message = message;
            this.severity = severity;
        }
    }

    static final class InlineMessage {
        final int line;
        final String text;
        final String kind;

        InlineMessage(int line, String text, String kind) {
            this.line = line;
            this.text = text;
            this.kind = kind;
        }
    }

    static final class RunResult {
        private final Component component;
        private final List<Diagnostic> diagnostics;
        private final List<InlineMessage> messages;

        RunResult(Component component, List<Diagnostic> diagnostics, List<InlineMessage> messages) {
            this.component = component;
            this.diagnostics = diagnostics;
            this.messages = messages;
        }

        Component getComponent() {
            return component;
        }

        List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        List<InlineMessage> getMessages() {
            return messages;
        }
    }

    /** Compiled classes ready to run: the class files and the entry class (dotted name). */
    static final class Compiled {
        final Map<String, byte[]> classes;
        final String mainClass;

        Compiled(Map<String, byte[]> classes, String mainClass) {
            this.classes = classes;
            this.mainClass = mainClass;
        }
    }

    /** Compile errors, as diagnostics. */
    static final class CompileFailure extends Exception {
        final List<Diagnostic> diagnostics;
        /** The source parsed; the errors are semantic. */
        final boolean wellFormed;

        CompileFailure(List<Diagnostic> diagnostics, boolean wellFormed) {
            super(diagnostics.isEmpty() ? "compile error" : diagnostics.get(0).message);
            this.diagnostics = diagnostics;
            this.wellFormed = wellFormed;
        }
    }

    private static boolean definerLookedUp;

    RunResult run(String script, PlaygroundContext context) {
        RunResult r = runImpl(script, context);
        // One console line per run (the browser console on the web): what happened and where the time went.
        StringBuilder line = new StringBuilder("[playground] ");
        if (r.getComponent() != null) {
            line.append("preview updated");
        } else {
            Diagnostic d = r.getDiagnostics().isEmpty() ? null : r.getDiagnostics().get(0);
            line.append("failed: ").append(d == null ? "?" : d.line + ":" + d.column + " " + d.message);
        }
        line.append(" (compile ").append(lastCompileMillis).append("ms, load ").append(lastLoadMillis)
                .append("ms, run ").append(lastRunMillis).append("ms)");
        System.out.println(line.toString());
        return r;
    }

    private long lastCompileMillis;
    private long lastLoadMillis;
    private long lastRunMillis;

    private RunResult runImpl(String script, PlaygroundContext context) {
        List<InlineMessage> messages = new ArrayList<InlineMessage>();
        lastCompileMillis = 0;
        lastLoadMillis = 0;
        lastRunMillis = 0;
        Compiled compiled;
        long t0 = System.currentTimeMillis();
        try {
            compiled = compile(script);
        } catch (CompileFailure f) {
            lastCompileMillis = System.currentTimeMillis() - t0;
            for (Diagnostic d : f.diagnostics) {
                messages.add(new InlineMessage(d.line, d.message, "error"));
            }
            return new RunResult(null, f.diagnostics, messages);
        } catch (Throwable t) {
            return failure("Compiler error: " + describe(t), messages);
        }
        lastCompileMillis = System.currentTimeMillis() - t0;
        context.clearCreatedComponents();
        context.clearShownForm();
        PlaygroundContext.activate(context);
        PlaygroundGlobals.ctx = context;
        Object value;
        long t1 = System.currentTimeMillis();
        try {
            PlaygroundEntry entry = (PlaygroundEntry) instantiate(compiled);
            lastLoadMillis = System.currentTimeMillis() - t1;
            long t2 = System.currentTimeMillis();
            try {
                value = entry.run(context);
            } finally {
                lastRunMillis = System.currentTimeMillis() - t2;
            }
        } catch (Throwable t) {
            context.markRunFinished();
            return failure("Runtime error: " + describe(t), messages);
        }
        context.markRunFinished();
        Object result = value;
        List<Object> locals = new ArrayList<Object>();
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= 2 && PlaygroundEntry.SCRIPT_VALUES.equals(arr[0])) {
                result = arr[1];
                for (int i = 2; i < arr.length; i++) {
                    locals.add(arr[i]);
                }
            }
        }
        Component component = resolveComponent(result, locals, context);
        if (component == null) {
            if (result != null && !(result instanceof Component)) {
                return failure("Script produced " + result.getClass().getName()
                        + " instead of a previewable Component.", messages);
            }
            return failure("Script must return a com.codename1.ui.Component, define build(ctx), or define lifecycle "
                    + "methods such as init(Object) and start().", messages);
        }
        messages.add(new InlineMessage(0, "Preview updated.", "success"));
        return new RunResult(component, new ArrayList<Diagnostic>(), messages);
    }

    private static Component resolveComponent(Object result, List<Object> locals, PlaygroundContext context) {
        // The script's top-level components, in declaration order, are its "created" ones.
        for (Object o : locals) {
            if (o instanceof Component) {
                context.recordCreatedComponent((Component) o);
            }
        }
        if (result instanceof Component) {
            return (Component) result;
        }
        if (context.getShownForm() != null) {
            return context.getShownForm();
        }
        if (context.getFirstCreatedForm() != null) {
            return context.getFirstCreatedForm();
        }
        return context.getFirstCreatedComponent();
    }

    private static RunResult failure(String message, List<InlineMessage> messages) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(new Diagnostic(1, 1, 1, 2, message, "error"));
        messages.add(new InlineMessage(0, message, "error"));
        return new RunResult(null, diagnostics, messages);
    }

    private static String describe(Throwable t) {
        String m = t.getMessage();
        String name = t.getClass().getName();
        return m == null || m.length() == 0 ? name : name + ": " + m;
    }

    // ------------------------------------------------------------------ compiling

    /** Compiles a script (and, for a class-shaped entry, its launcher). */
    static Compiled compile(String script) throws Exception {
        final StubLibrary api = PlaygroundApi.library();
        JavaCompiler jc = configure(new JavaCompiler(api));
        ScriptSpec spec = new ScriptSpec();
        spec.className = SCRIPT_CLASS;
        spec.interfaceName = "com.codenameone.playground.PlaygroundEntry";
        spec.methodName = "run";
        spec.paramType = CONTEXT;
        spec.paramName = "ctx";
        spec.valuesMarker = PlaygroundEntry.SCRIPT_VALUES;
        jc.addScript("Playground.java", script, spec);
        JavaCompiler.Result r = jc.compile();
        if (!r.isSuccess()) {
            throw new CompileFailure(toDiagnostics(r.getDiagnostics()), r.isWellFormed());
        }
        final Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>(r.getClasses());
        String launcher = launcherSource(r.getClassInfos());
        if (launcher == null) {
            return new Compiled(classes, SCRIPT_CLASS);
        }
        JavaCompiler lc = configure(new JavaCompiler(new ClassLibrary() {
            @Override
            public byte[] classBytes(String internalName) {
                byte[] b = classes.get(internalName);
                return b != null ? b : api.classBytes(internalName);
            }
        }));
        lc.addSource("PlaygroundLauncher.java", launcher);
        JavaCompiler.Result lr = lc.compile();
        if (!lr.isSuccess()) {
            throw new CompileFailure(toDiagnostics(lr.getDiagnostics()), lr.isWellFormed());
        }
        classes.putAll(lr.getClasses());
        return new Compiled(classes, LAUNCHER_CLASS);
    }

    private static JavaCompiler configure(JavaCompiler jc) {
        for (String d : DEFAULT_IMPORTS) {
            jc.addDefaultImport(d);
        }
        // form.show() / showBack() become the preview rather than replacing the Playground.
        jc.redirectCall("com/codename1/ui/Form", "show", "()V", "com/codenameone/playground/PlaygroundContext", "showForm");
        jc.redirectCall("com/codename1/ui/Form", "showBack", "()V", "com/codenameone/playground/PlaygroundContext", "showForm");
        return jc;
    }

    private static List<Diagnostic> toDiagnostics(List<com.codename1.tools.javac.Diagnostic> in) {
        List<Diagnostic> out = new ArrayList<Diagnostic>();
        for (com.codename1.tools.javac.Diagnostic d : in) {
            if (!d.error) {
                continue;
            }
            int line = Math.max(1, d.line);
            int col = Math.max(1, d.column);
            out.add(new Diagnostic(line, col, line, col + 1, d.message, "error"));
        }
        return out;
    }

    /**
     * A launcher for a class-shaped entry, or null when the script's statements are
     * the entry. Looks, in order, for a top-level {@code build(PlaygroundContext)} (or
     * {@code build(Object)}) in the script, lifecycle methods in the script, then a
     * class declaring {@code start()} (with an optional {@code init}), then a class
     * declaring {@code build}.
     */
    static String launcherSource(List<JavaCompiler.ClassInfo> infos) {
        JavaCompiler.ClassInfo script = null;
        for (JavaCompiler.ClassInfo ci : infos) {
            if (ci.getName().equals(SCRIPT_CLASS)) {
                script = ci;
            }
        }
        StringBuilder b = new StringBuilder();
        b.append("public class ").append(LAUNCHER_CLASS).append(" implements com.codenameone.playground.PlaygroundEntry {\n");
        b.append("    public Object run(").append(CONTEXT).append(" ctx) throws Throwable {\n");
        b.append("        ").append(SCRIPT_CLASS).append(" script = new ").append(SCRIPT_CLASS).append("();\n");
        b.append("        Object values = script.run(ctx);\n");
        String call = null;
        if (script != null) {
            call = buildCall(script, "script", false);
            if (call == null && declaresStart(script)) {
                call = lifecycleCall(script, "script");
            }
        }
        if (call == null) {
            for (JavaCompiler.ClassInfo ci : infos) {
                if (isCandidate(ci) && declaresStart(ci)) {
                    String target = sourceName(ci.getName());
                    b.append("        ").append(target).append(" app = new ").append(target).append("();\n");
                    call = lifecycleCall(ci, "app");
                    break;
                }
            }
        }
        if (call == null) {
            for (JavaCompiler.ClassInfo ci : infos) {
                if (isCandidate(ci)) {
                    String target = sourceName(ci.getName());
                    String c = buildCall(ci, target, true);
                    if (c == null) {
                        c = buildCall(ci, "app", false);
                        if (c != null) {
                            b.append("        ").append(target).append(" app = new ").append(target).append("();\n");
                        }
                    }
                    if (c != null) {
                        call = c;
                        break;
                    }
                }
            }
        }
        if (call == null) {
            return null;
        }
        b.append(call);
        b.append("    }\n}\n");
        return b.toString();
    }

    private static boolean isCandidate(JavaCompiler.ClassInfo ci) {
        return ci.isTopLevel() && !ci.isAbstract() && !ci.getName().equals(SCRIPT_CLASS)
                && !ci.getName().equals(LAUNCHER_CLASS) && ci.declares("<init>", "()V", false);
    }

    private static boolean declaresStart(JavaCompiler.ClassInfo ci) {
        return ci.declares("start", "()V", false) || ci.declares("start", "()Lcom/codename1/ui/Component;", false)
                || ci.declares("start", "()Lcom/codename1/ui/Form;", false)
                || ci.declares("start", "()Ljava/lang/Object;", false);
    }

    private static String lifecycleCall(JavaCompiler.ClassInfo ci, String target) {
        StringBuilder b = new StringBuilder();
        if (ci.declares("init", "(" + CONTEXT_DESC + ")V", false)) {
            b.append("        ").append(target).append(".init(ctx);\n");
        } else if (ci.declares("init", "(Ljava/lang/Object;)V", false)) {
            b.append("        ").append(target).append(".init((Object) ctx);\n");
        } else if (ci.declares("init", "()V", false)) {
            b.append("        ").append(target).append(".init();\n");
        }
        if (ci.declares("start", "()V", false)) {
            b.append("        ").append(target).append(".start();\n");
            b.append("        return null;\n");
        } else {
            b.append("        return ").append(target).append(".start();\n");
        }
        return b.toString();
    }

    /** {@code return target.build(ctx);} for whichever build overload the class declares. */
    private static String buildCall(JavaCompiler.ClassInfo ci, String target, boolean isStatic) {
        String[] returns = {"Ljava/lang/Object;", "Lcom/codename1/ui/Component;", "Lcom/codename1/ui/Form;",
            "Lcom/codename1/ui/Container;", "V"};
        for (String ret : returns) {
            String arg = null;
            if (ci.declares("build", "(" + CONTEXT_DESC + ")" + ret, isStatic)) {
                arg = "ctx";
            } else if (ci.declares("build", "(Ljava/lang/Object;)" + ret, isStatic)) {
                arg = "(Object) ctx";
            } else if (ci.declares("build", "()" + ret, isStatic)) {
                arg = "";
            }
            if (arg != null) {
                return "V".equals(ret) ? "        " + target + ".build(" + arg + ");\n        return null;\n"
                        : "        return " + target + ".build(" + arg + ");\n";
            }
        }
        return null;
    }

    private static String sourceName(String internalName) {
        return internalName.replace('/', '.').replace('$', '.');
    }

    // ------------------------------------------------------------------ loading

    /** Loads the compiled classes into this VM and instantiates the entry class. */
    static Object instantiate(final Compiled compiled) throws Exception {
        if ("HTML5".equals(Display.getInstance().getPlatformName())) {
            final StubLibrary api = PlaygroundApi.library();
            String js = JavascriptIncremental.translate(compiled.classes, new JavascriptIncremental.ClassSource() {
                @Override
                public byte[] classBytes(String internalName) {
                    return api.classBytes(internalName);
                }
            });
            PlaygroundJs.loadClasses(js);
            return Class.forName(compiled.mainClass).newInstance();
        }
        PlaygroundClassDefiner definer = PlaygroundClassDefiner.Registry.get();
        if (definer == null && !definerLookedUp) {
            definerLookedUp = true;
            PlaygroundLoaderNative n = NativeLookup.create(PlaygroundLoaderNative.class);
            if (n != null && n.isSupported()) {
                n.registerDefiner();
            }
            definer = PlaygroundClassDefiner.Registry.get();
        }
        if (definer == null) {
            throw new IllegalStateException("Running compiled code is not supported on "
                    + Display.getInstance().getPlatformName());
        }
        return definer.defineAndInstantiate(StubLibrary.pack(compiled.classes), compiled.mainClass);
    }
}
