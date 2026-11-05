/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Turns `@Command` classes (`com.codename1.cli.CommandLine.Command`) into a
/// command-line parser without reflection.
///
/// For every command it generates `<Command>Cn1Cli`, a `CommandBinding` beside
/// the command that builds its model, converts and stores each argument and runs
/// it; a private field is reached through the injection setter
/// [BackendWeaver] weaves, as an `@Autowired` field is. It also generates
/// `CliApplication`, whose `commands()` lists the top-level commands and whose
/// `main` runs them, and records it as the module's entry point.
///
/// It runs before [RestControllerAnnotationProcessor]. A module that also
/// serves HTTP gets that processor's `BackendApplication` as its entry point
/// instead, and that `main` hands the command line to the same commands with the
/// server as their `serve` sibling: [#CLI_APPLICATION_ATTRIBUTE] is how it
/// learns they exist. That is what makes the commands available in every
/// backend without an opt-in.
public final class CommandAnnotationProcessor extends AbstractAnnotationProcessor {
    /// The context attribute holding the binary name of the generated
    /// `CliApplication`, set when the module has commands.
    public static final String CLI_APPLICATION_ATTRIBUTE = "cn1.cli.application";

    /// The context attribute holding the application name the help prints.
    public static final String CLI_NAME_ATTRIBUTE = "cn1.cli.name";

    static final String COMMAND = "Lcom/codename1/cli/CommandLine$Command;";
    static final String OPTION = "Lcom/codename1/cli/CommandLine$Option;";
    static final String PARAMETERS = "Lcom/codename1/cli/CommandLine$Parameters;";
    static final String PARENT = "Lcom/codename1/cli/CommandLine$ParentCommand;";
    static final String SPEC = "Lcom/codename1/cli/CommandLine$Spec;";
    private static final String GENERATED = "Lcom/codename1/backend/annotations/Generated;";
    private static final String GENERATED_ANNOTATION = "@com.codename1.backend.annotations.Generated";

    /// What the Python transpiler generates for a module with Python sources.
    static final String PYTHON_MODULES = "com/codename1/generated/python/PythonModules";
    /// The entry point generated for a command-line tool written in Python.
    static final String PYTHON_APPLICATION = "com/codename1/generated/python/PythonCliApplication";

    private static final String RUNNABLE = "java/lang/Runnable";
    private static final String CALLABLE = "java/util/concurrent/Callable";
    private static final String EXIT_CODE = "com/codename1/cli/CommandLine$IExitCodeGenerator";

    /// The command classes found, by internal name, in the order found.
    private final Map<String, AnnotatedClass> commands = new LinkedHashMap<String, AnnotatedClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        Set<String> s = new HashSet<String>();
        s.add(COMMAND);
        s.add(OPTION);
        s.add(PARAMETERS);
        return s;
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        commands.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        if (cls.isSynthetic()) {
            return;
        }
        boolean command = cls.getClassAnnotation(COMMAND) != null;
        if (!command) {
            for (FieldInfo f : cls.getFields()) {
                if (f.getAnnotation(OPTION) != null || f.getAnnotation(PARAMETERS) != null) {
                    if (BuildHintAnnotationProcessor.hasBackingSource(cls,
                            ctx.getCompileSourceRoots(), ctx.getSourceEncoding())) {
                        ctx.error(cls, cls.getSourceName() + "." + f.getName() + " is an "
                                + "@Option or @Parameters field, but " + cls.getSourceName()
                                + " is not annotated @Command. Annotate the class.");
                    }
                    return;
                }
            }
            return;
        }
        // A deleted command whose .class is still in target/classes must not keep
        // shipping: the same orphan rule every generator here follows.
        if (!BuildHintAnnotationProcessor.hasBackingSource(cls, ctx.getCompileSourceRoots(),
                ctx.getSourceEncoding())) {
            return;
        }
        commands.put(cls.getInternalName(), cls);
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (commands.isEmpty()) {
            generatePythonEntryPoint(ctx);
            return;
        }
        Map<String, Model> models = new LinkedHashMap<String, Model>();
        for (AnnotatedClass cls : commands.values()) {
            Model m = model(cls, ctx);
            if (m != null) {
                models.put(cls.getInternalName(), m);
            }
        }
        if (ctx.hasErrors()) {
            return;
        }
        Set<String> referenced = new HashSet<String>();
        for (Model m : models.values()) {
            for (String sub : m.subcommands) {
                if (!models.containsKey(sub)) {
                    ctx.error(m.cls, m.cls.getSourceName() + " lists "
                            + sub.replace('/', '.') + " as a subcommand, but it is not "
                            + "annotated @Command.");
                    continue;
                }
                if (!referenced.add(sub)) {
                    ctx.error(m.cls, sub.replace('/', '.') + " is a subcommand of more than "
                            + "one command. A command has one parent; declare a second class "
                            + "for the other place.");
                }
            }
        }
        List<Model> top = new ArrayList<Model>();
        for (Model m : models.values()) {
            if (!referenced.contains(m.cls.getInternalName())) {
                top.add(m);
            }
        }
        if (top.isEmpty() && !ctx.hasErrors()) {
            ctx.error("The @Command classes only list each other as subcommands, so none of "
                    + "them is the top level. Remove the cycle.");
        }
        if (ctx.hasErrors()) {
            return;
        }
        String appName = appName(ctx, top);
        String entryPackage = top.get(0).packageName;
        String application = qualify(entryPackage, "CliApplication");
        if (isNotOurOwnOutput(ctx, application)) {
            ctx.error(application + " already exists, and the entry point generated for the "
                    + "@Command classes would replace it. Rename that class.");
            return;
        }
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (Model m : models.values()) {
            String binding = m.bindingBinaryName();
            if (isNotOurOwnOutput(ctx, binding)) {
                ctx.error(m.cls, binding + " already exists, and the binding generated for "
                        + m.cls.getSourceName() + " would replace it. Rename that class.");
                return;
            }
            sources.put(binding, generateBinding(m, models));
        }
        sources.put(application, generateApplication(entryPackage, appName, top));
        try {
            for (Model m : models.values()) {
                if (!m.plan.isEmpty()) {
                    BackendWeaver.weave(ctx.getOutputClassDir(), m.plan);
                }
            }
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile the generated @Command sources: "
                    + ioe.getMessage(), ioe);
        }
        ctx.setAttribute(CLI_APPLICATION_ATTRIBUTE, application);
        ctx.setAttribute(CLI_NAME_ATTRIBUTE, appName);
        // The entry point of a module with commands and nothing else. A module
        // that also serves HTTP replaces this marker with its BackendApplication,
        // whose main runs these same commands.
        ctx.emitResource(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE, utf8(application));
        ctx.getLog().info("cn1: generated " + models.size() + " @Command binding(s) and "
                + application);
    }

    /// The name the help prints for a group of several top-level commands:
    /// `cn1.cli.name` from the module's `application.properties`, else the single
    /// command's own name, else the project directory's.
    ///
    /// Read from the file rather than from the context's project settings: those
    /// are `codenameone_settings.properties`, which a backend or a tool does not
    /// have, and the native packager hands this pass none at all. The file is the
    /// one both the JVM run and the packaged binary read, so the two agree.
    private static String appName(ProcessorContext ctx, List<Model> top) {
        File dir = moduleDir(ctx);
        File settings = dir == null ? null : new File(dir, "application.properties");
        if (settings != null && settings.isFile()) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.InputStream in = new java.io.FileInputStream(settings)) {
                p.load(in);
            } catch (java.io.IOException err) {
                ctx.getLog().warn("cn1: could not read " + settings + ": " + err.getMessage());
            }
            String n = p.getProperty("cn1.cli.name");
            if (n != null && n.trim().length() > 0) {
                return n.trim();
            }
        }
        if (top.size() == 1) {
            return top.get(0).name;
        }
        return dir == null ? "app" : dir.getName();
    }

    // ------------------------------------------------------------------ model

    /// One argument field of a command.
    static final class Arg {
        boolean option;
        int slot;
        FieldInfo field;
        Kind kind;
        /// For an array, list or set: the element kind; otherwise == kind.
        Kind element;
        String elementSource;
        String converter;
        AnnotationValues values;
        boolean flag;
        boolean multiple;
        String index;
    }

    /// What a field holds, as far as conversion and storage are concerned.
    enum Kind {
        BOOLEAN("boolean", "Boolean"), INT("int", "Integer"), LONG("long", "Long"),
        SHORT("short", "Short"), BYTE("byte", "Byte"), DOUBLE("double", "Double"),
        FLOAT("float", "Float"), CHAR("char", "Character"),
        BOXED_BOOLEAN(null, "Boolean"), BOXED_INT(null, "Integer"), BOXED_LONG(null, "Long"),
        BOXED_SHORT(null, "Short"), BOXED_BYTE(null, "Byte"), BOXED_DOUBLE(null, "Double"),
        BOXED_FLOAT(null, "Float"), BOXED_CHAR(null, "Character"),
        STRING(null, "String"), FILE(null, "java.io.File"), ENUM(null, null),
        CUSTOM(null, null), ARRAY(null, null), LIST(null, null), SET(null, null);

        final String primitive;
        final String boxed;

        Kind(String primitive, String boxed) {
            this.primitive = primitive;
            this.boxed = boxed;
        }

        boolean isCollection() {
            return this == ARRAY || this == LIST || this == SET;
        }
    }

    /// One command, ready to generate.
    final class Model {
        final AnnotatedClass cls;
        final String packageName;
        String name;
        String[] aliases;
        String[] description;
        String[] version;
        boolean standardHelp;
        final List<String> subcommands = new ArrayList<String>();
        final List<Arg> args = new ArrayList<Arg>();
        FieldInfo parentField;
        String parentSource;
        FieldInfo specField;
        boolean callable;
        boolean runnable;
        boolean exitCode;
        final BackendWeaver.Plan plan;

        Model(AnnotatedClass cls) {
            this.cls = cls;
            String bn = cls.getBinaryName();
            int dot = bn.lastIndexOf('.');
            this.packageName = dot < 0 ? "" : bn.substring(0, dot);
            this.plan = new BackendWeaver.Plan(cls.getInternalName(), null);
        }

        String bindingSimpleName() {
            String bn = cls.getBinaryName();
            return bn.substring(bn.lastIndexOf('.') + 1).replace('$', '_') + "Cn1Cli";
        }

        String bindingBinaryName() {
            return qualify(packageName, bindingSimpleName());
        }
    }

    private Model model(AnnotatedClass cls, ProcessorContext ctx) {
        String where = cls.getSourceName();
        if (cls.isAbstract() || cls.isInterface()) {
            ctx.error(cls, "@Command " + where + " is abstract. A command is instantiated "
                    + "for every run; make it a concrete class.");
            return null;
        }
        if (!cls.isNameableInPackage()) {
            ctx.error(cls, "@Command " + where + " is private, or nested in a private class, "
                    + "so the code generated beside it cannot create it. Make it at least "
                    + "package-private.");
            return null;
        }
        Model m = new Model(cls);
        AnnotationValues cmd = cls.getClassAnnotation(COMMAND);
        String simple = cls.getBinaryName().substring(cls.getBinaryName().lastIndexOf('.') + 1);
        simple = simple.substring(simple.lastIndexOf('$') + 1);
        m.name = cmd.getStringOrDefault("name", "").trim();
        if (m.name.length() == 0) {
            m.name = asciiLower(simple);
        }
        m.aliases = strings(cmd.get("aliases"));
        m.description = strings(cmd.get("description"));
        m.version = strings(cmd.get("version"));
        m.standardHelp = cmd.getBoolOrDefault("mixinStandardHelpOptions", false);
        Object subs = cmd.get("subcommands");
        if (subs instanceof List) {
            for (Object o : (List<?>) subs) {
                if (o instanceof Type) {
                    String sub = ((Type) o).getInternalName();
                    if (sub.equals(cls.getInternalName())) {
                        ctx.error(cls, where + " lists itself as a subcommand.");
                        return null;
                    }
                    m.subcommands.add(sub);
                }
            }
        }
        m.callable = implementsInterface(cls, CALLABLE, ctx);
        m.runnable = implementsInterface(cls, RUNNABLE, ctx);
        m.exitCode = implementsInterface(cls, EXIT_CODE, ctx);
        if (!m.callable && !m.runnable && m.subcommands.isEmpty()) {
            ctx.error(cls, "@Command " + where + " has nothing to run: implement Runnable or "
                    + "Callable<Integer>, or list subcommands.");
            return null;
        }
        boolean hasConstructor = false;
        for (MethodInfo method : cls.getMethods()) {
            if (method.isConstructor() && "()V".equals(method.getDescriptor())) {
                hasConstructor = true;
                if (method.isPrivate()) {
                    m.plan.constructors.add("()V");
                }
            }
        }
        if (!hasConstructor) {
            ctx.error(cls, "@Command " + where + " has no constructor without parameters. "
                    + "A command is created for every run; add one.");
            return null;
        }
        int slot = 0;
        Set<String> names = new HashSet<String>();
        int nextIndex = 0;
        boolean openPositional = false;
        List<int[]> ranges = new ArrayList<int[]>();
        for (FieldInfo f : cls.getFields()) {
            AnnotationValues option = f.getAnnotation(OPTION);
            AnnotationValues params = f.getAnnotation(PARAMETERS);
            boolean parent = f.getAnnotation(PARENT) != null;
            boolean spec = f.getAnnotation(SPEC) != null;
            int count = (option != null ? 1 : 0) + (params != null ? 1 : 0) + (parent ? 1 : 0)
                    + (spec ? 1 : 0);
            if (count == 0) {
                continue;
            }
            String fw = "field " + f.getName() + " of " + where;
            if (count > 1) {
                ctx.error(cls, fw + " has more than one of @Option, @Parameters, "
                        + "@ParentCommand and @Spec. Choose one.");
                continue;
            }
            if (f.isStatic()) {
                ctx.error(cls, fw + " is static. Arguments go to each run's own instance; "
                        + "make it an instance field.");
                continue;
            }
            if (f.isFinal()) {
                ctx.error(cls, fw + " is final, so the parser cannot set it. Drop the final.");
                continue;
            }
            if (f.isPrivate()) {
                m.plan.injectFields.put(f.getName(), f.getDescriptor());
            }
            if (spec) {
                if (!"Lcom/codename1/cli/CommandSpec;".equals(f.getDescriptor())) {
                    ctx.error(cls, fw + " is @Spec, so it must be a com.codename1.cli.CommandSpec.");
                    continue;
                }
                m.specField = f;
                continue;
            }
            if (parent) {
                Type t = Type.getType(f.getDescriptor());
                if (t.getSort() != Type.OBJECT) {
                    ctx.error(cls, fw + " is @ParentCommand, so it must have the type of the "
                            + "command that lists " + where + " as a subcommand.");
                    continue;
                }
                m.parentField = f;
                m.parentSource = sourceName(t.getInternalName(), ctx);
                continue;
            }
            Arg a = new Arg();
            a.option = option != null;
            a.values = option != null ? option : params;
            a.field = f;
            a.slot = slot++;
            Object conv = a.values.get("converter");
            if (conv instanceof Type && !"V".equals(((Type) conv).getDescriptor())) {
                AnnotatedClass c = ctx.lookupWithDependencies(((Type) conv).getInternalName());
                a.converter = c != null ? c.getSourceName()
                        : ((Type) conv).getClassName().replace('$', '.');
            }
            if (!classify(a, ctx, cls, fw)) {
                continue;
            }
            if (a.option) {
                String[] optionNames = strings(option.get("names"));
                if (optionNames.length == 0) {
                    ctx.error(cls, fw + " is an @Option with no names.");
                    continue;
                }
                for (String n : optionNames) {
                    if (!n.startsWith("-") || n.length() < 2 || n.indexOf('=') >= 0
                            || n.indexOf(' ') >= 0) {
                        ctx.error(cls, fw + " names the option '" + n + "'. An option name "
                                + "starts with '-' and holds no '=' or space.");
                    } else if (!names.add(n)) {
                        ctx.error(cls, "The option name '" + n + "' is used twice in " + where
                                + ".");
                    }
                }
                a.flag = !a.multiple && (a.kind == Kind.BOOLEAN || a.kind == Kind.BOXED_BOOLEAN)
                        && a.converter == null;
                if (option.getBoolOrDefault("negatable", false) && !a.flag) {
                    ctx.error(cls, fw + " is negatable, which only a boolean option can be.");
                }
            } else {
                String index = params.getStringOrDefault("index", "").trim();
                int from;
                int to;
                if (index.length() == 0) {
                    if (openPositional) {
                        ctx.error(cls, fw + " follows a @Parameters field that takes every "
                                + "remaining argument. Give it an index.");
                        continue;
                    }
                    from = nextIndex;
                    to = a.multiple ? Integer.MAX_VALUE : nextIndex;
                    index = a.multiple ? nextIndex + "..*" : String.valueOf(nextIndex);
                } else {
                    int[] r = parseRange(index);
                    if (r == null) {
                        ctx.error(cls, fw + " has the index '" + index + "'. Write a "
                                + "position, such as 0, or a range, such as 1..3 or 1..*.");
                        continue;
                    }
                    from = r[0];
                    to = r[1];
                    if (!a.multiple && from != to) {
                        ctx.error(cls, fw + " takes the range " + index + " but holds one "
                                + "value. Make it an array or a list, or give one position.");
                        continue;
                    }
                }
                for (int[] other : ranges) {
                    if (from <= other[1] && other[0] <= to) {
                        ctx.error(cls, fw + " takes positions that another @Parameters field "
                                + "of " + where + " also takes.");
                    }
                }
                ranges.add(new int[] {from, to});
                openPositional |= to == Integer.MAX_VALUE;
                nextIndex = to == Integer.MAX_VALUE ? nextIndex : to + 1;
                a.index = index;
            }
            String arity = a.values.getStringOrDefault("arity", "").trim();
            if (arity.length() > 0 && !validArity(arity)) {
                ctx.error(cls, fw + " has the arity '" + arity + "'. Write a count, such as 1, "
                        + "or a range, such as 0..1 or 1..*.");
            }
            m.args.add(a);
        }
        return m;
    }

    /// Works out the kind of `a`'s field. Answers false after reporting a type
    /// the parser cannot fill.
    private boolean classify(Arg a, ProcessorContext ctx, AnnotatedClass cls, String fw) {
        String desc = a.field.getDescriptor();
        if (desc.startsWith("[")) {
            if (desc.startsWith("[[")) {
                ctx.error(cls, fw + " is an array of arrays, which the parser cannot fill.");
                return false;
            }
            a.kind = Kind.ARRAY;
            a.multiple = true;
            a.element = scalar(desc.substring(1), ctx);
            a.elementSource = typeSource(desc.substring(1), ctx);
        } else if ("Ljava/util/List;".equals(desc) || "Ljava/util/Collection;".equals(desc)
                || "Ljava/util/Set;".equals(desc)) {
            a.kind = "Ljava/util/Set;".equals(desc) ? Kind.SET : Kind.LIST;
            a.multiple = true;
            String elementDesc = typeArgument(a.field.getSignature());
            if (elementDesc == null) {
                ctx.error(cls, fw + " is a raw collection. Give it an element type, such as "
                        + "List<String>.");
                return false;
            }
            a.element = scalar(elementDesc, ctx);
            a.elementSource = typeSource(elementDesc, ctx);
            if (a.element != null && a.element.primitive != null) {
                a.element = null;
            }
        } else {
            a.kind = scalar(desc, ctx);
            a.element = a.kind;
            a.elementSource = typeSource(desc, ctx);
        }
        if (a.converter != null) {
            a.element = Kind.CUSTOM;
            if (!a.multiple) {
                a.kind = Kind.CUSTOM;
            }
            return true;
        }
        if (a.element == null || a.element == Kind.CUSTOM) {
            ctx.error(cls, fw + " has a type the parser does not convert: "
                    + Type.getType(desc).getClassName() + ". Use a primitive, a wrapper, "
                    + "String, java.io.File, an enum, or an array or list of those; or "
                    + "name a converter.");
            return false;
        }
        return true;
    }

    private Kind scalar(String desc, ProcessorContext ctx) {
        switch (desc) {
            case "Z": return Kind.BOOLEAN;
            case "I": return Kind.INT;
            case "J": return Kind.LONG;
            case "S": return Kind.SHORT;
            case "B": return Kind.BYTE;
            case "D": return Kind.DOUBLE;
            case "F": return Kind.FLOAT;
            case "C": return Kind.CHAR;
            case "Ljava/lang/Boolean;": return Kind.BOXED_BOOLEAN;
            case "Ljava/lang/Integer;": return Kind.BOXED_INT;
            case "Ljava/lang/Long;": return Kind.BOXED_LONG;
            case "Ljava/lang/Short;": return Kind.BOXED_SHORT;
            case "Ljava/lang/Byte;": return Kind.BOXED_BYTE;
            case "Ljava/lang/Double;": return Kind.BOXED_DOUBLE;
            case "Ljava/lang/Float;": return Kind.BOXED_FLOAT;
            case "Ljava/lang/Character;": return Kind.BOXED_CHAR;
            case "Ljava/lang/String;": return Kind.STRING;
            case "Ljava/io/File;": return Kind.FILE;
            default:
                break;
        }
        if (desc.startsWith("L") && desc.endsWith(";")) {
            AnnotatedClass c = ctx.lookupWithDependencies(desc.substring(1, desc.length() - 1));
            if (c != null && c.isEnum()) {
                return Kind.ENUM;
            }
            return Kind.CUSTOM;
        }
        return null;
    }

    private String typeSource(String desc, ProcessorContext ctx) {
        Type t = Type.getType(desc);
        if (t.getSort() == Type.OBJECT) {
            return sourceName(t.getInternalName(), ctx);
        }
        return t.getClassName();
    }

    private static String sourceName(String internalName, ProcessorContext ctx) {
        AnnotatedClass c = ctx.lookupWithDependencies(internalName);
        return c != null ? c.getSourceName() : internalName.replace('/', '.').replace('$', '.');
    }

    /// The descriptor of the single type argument in a field signature such as
    /// `Ljava/util/List<Ljava/lang/String;>;`, or null.
    static String typeArgument(String signature) {
        if (signature == null) {
            return null;
        }
        int lt = signature.indexOf('<');
        int gt = signature.lastIndexOf('>');
        if (lt < 0 || gt < lt) {
            return null;
        }
        String inner = signature.substring(lt + 1, gt);
        if (inner.startsWith("+") || inner.startsWith("-")) {
            inner = inner.substring(1);
        }
        if (inner.indexOf('<') >= 0 || !inner.startsWith("L") || !inner.endsWith(";")
                || inner.indexOf(';') != inner.length() - 1) {
            return null;
        }
        return inner;
    }

    private static boolean implementsInterface(AnnotatedClass cls, String iface,
                                               ProcessorContext ctx) {
        AnnotatedClass at = cls;
        int guard = 0;
        while (at != null && guard++ < 64) {
            List<String> interfaces = at.getInterfaceInternalNames();
            if (interfaces != null) {
                for (String i : interfaces) {
                    if (i.equals(iface) || extendsInterface(i, iface, ctx, 0)) {
                        return true;
                    }
                }
            }
            String sup = at.getSuperInternalName();
            at = sup == null ? null : ctx.lookupWithDependencies(sup);
        }
        return false;
    }

    private static boolean extendsInterface(String name, String iface, ProcessorContext ctx,
                                            int depth) {
        if (depth > 16) {
            return false;
        }
        AnnotatedClass c = ctx.lookupWithDependencies(name);
        if (c == null || c.getInterfaceInternalNames() == null) {
            return false;
        }
        for (String i : c.getInterfaceInternalNames()) {
            if (i.equals(iface) || extendsInterface(i, iface, ctx, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    static int[] parseRange(String index) {
        try {
            int dots = index.indexOf("..");
            if (dots < 0) {
                int at = Integer.parseInt(index);
                return at < 0 ? null : new int[] {at, at};
            }
            int from = Integer.parseInt(index.substring(0, dots).trim());
            String to = index.substring(dots + 2).trim();
            int end = "*".equals(to) ? Integer.MAX_VALUE : Integer.parseInt(to);
            return from < 0 || end < from ? null : new int[] {from, end};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean validArity(String arity) {
        return "*".equals(arity) || parseRange(arity) != null;
    }

    // -------------------------------------------------------------- generation

    String generateBinding(Model m, Map<String, Model> models) {
        String self = m.cls.getSourceName();
        StringBuilder sb = new StringBuilder();
        if (m.packageName.length() > 0) {
            sb.append("package ").append(m.packageName).append(";\n\n");
        }
        sb.append("// Generated from the @Command ").append(self).append(". Do not edit.\n");
        sb.append(GENERATED_ANNOTATION).append('\n');
        sb.append("final class ").append(m.bindingSimpleName())
          .append(" implements com.codename1.cli.CommandBinding {\n\n");

        // spec()
        sb.append("    public com.codename1.cli.CommandSpec spec() {\n");
        sb.append("        com.codename1.cli.CommandSpec s = new com.codename1.cli.CommandSpec(")
          .append(quote(m.name)).append(", ").append(array(m.description)).append(", ")
          .append(array(m.version)).append(", ").append(m.standardHelp).append(", this);\n");
        if (m.aliases.length > 0) {
            sb.append("        s.aliases(").append(array(m.aliases)).append(");\n");
        }
        for (Arg a : m.args) {
            String description = join(strings(a.values.get("description")));
            String label = nullable(a.values.getStringOrDefault("paramLabel", ""));
            String arity = quote(a.values.getStringOrDefault("arity", "").trim());
            String def = nullable(a.values.getStringOrDefault("defaultValue", ""));
            String split = nullable(a.values.getStringOrDefault("split", ""));
            boolean hidden = a.values.getBoolOrDefault("hidden", false);
            if (a.option) {
                sb.append("        s.addOption(new com.codename1.cli.OptionSpec(").append(a.slot)
                  .append(", ").append(array(strings(a.values.get("names")))).append(", ")
                  .append(quote(description)).append(", ").append(label).append(", ")
                  .append(a.flag).append(", ")
                  .append(a.values.getBoolOrDefault("required", false)).append(", ")
                  .append(arity).append(", ").append(a.multiple).append(", ").append(def)
                  .append(", ").append(split).append(", ")
                  .append(a.values.getBoolOrDefault("negatable", false)).append(", ")
                  .append(hidden).append("));\n");
            } else {
                sb.append("        s.addPositional(new com.codename1.cli.PositionalParamSpec(")
                  .append(a.slot).append(", ").append(quote(a.index)).append(", ")
                  .append(quote(description)).append(", ").append(label).append(", ")
                  .append("false, ").append(arity).append(", ").append(a.multiple)
                  .append(", ").append(def).append(", ").append(split).append(", ")
                  .append(hidden).append("));\n");
            }
        }
        for (String sub : m.subcommands) {
            Model s = models.get(sub);
            sb.append("        s.addSubcommand(new ").append(qualify(s.packageName,
                    s.bindingSimpleName())).append("().spec());\n");
        }
        sb.append("        return s;\n    }\n\n");

        // create()
        sb.append("    public Object create() {\n");
        if (m.plan.constructors.contains("()V")) {
            sb.append("        return ").append(self).append('.')
              .append(BackendWeaver.NEW_BRIDGE).append("();\n");
        } else {
            sb.append("        return new ").append(self).append("();\n");
        }
        sb.append("    }\n\n");

        // convert()
        sb.append("    public Object convert(int slot, String text) throws Exception {\n");
        sb.append("        switch (slot) {\n");
        for (Arg a : m.args) {
            if (a.flag) {
                continue;
            }
            sb.append("            case ").append(a.slot).append(":\n");
            sb.append("                ").append(conversion(a)).append('\n');
        }
        sb.append("            default:\n");
        sb.append("                return text;\n");
        sb.append("        }\n    }\n\n");

        // set()
        sb.append("    public void set(Object command, int slot, Object value) {\n");
        sb.append("        ").append(self).append(" t = (").append(self).append(") command;\n");
        sb.append("        switch (slot) {\n");
        for (Arg a : m.args) {
            sb.append("            case ").append(a.slot).append(": {\n");
            storage(sb, m, a);
            sb.append("                break;\n            }\n");
        }
        sb.append("            default:\n                break;\n");
        sb.append("        }\n    }\n\n");

        // inject()
        sb.append("    public void inject(Object command, Object parent, "
                + "com.codename1.cli.CommandSpec spec) {\n");
        if (m.parentField != null || m.specField != null) {
            sb.append("        ").append(self).append(" t = (").append(self)
              .append(") command;\n");
        }
        if (m.parentField != null) {
            // instanceof, never a bare cast: ParparVM's CHECKCAST is unchecked, and
            // a command run on its own has no parent at all.
            sb.append("        if (parent instanceof ").append(m.parentSource).append(") {\n");
            sb.append("            ").append(assign(m, m.parentField, "(" + m.parentSource
                    + ") parent")).append('\n');
            sb.append("        }\n");
        }
        if (m.specField != null) {
            sb.append("        ").append(assign(m, m.specField, "spec")).append('\n');
        }
        sb.append("    }\n\n");

        // runnable() / call()
        boolean body = m.callable || m.runnable;
        sb.append("    public boolean runnable() {\n        return ").append(body)
          .append(";\n    }\n\n");
        sb.append("    public int call(Object command) throws Exception {\n");
        if (!body) {
            sb.append("        throw new IllegalStateException(\"").append(m.name)
              .append(" has no body\");\n");
        } else {
            sb.append("        ").append(self).append(" t = (").append(self)
              .append(") command;\n");
            if (m.callable) {
                sb.append("        Object result = t.call();\n");
                sb.append("        if (result instanceof Integer) {\n");
                sb.append("            return ((Integer) result).intValue();\n");
                sb.append("        }\n");
            } else {
                sb.append("        t.run();\n");
            }
            if (m.exitCode) {
                sb.append("        return t.getExitCode();\n");
            } else {
                sb.append("        return 0;\n");
            }
        }
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /// The `return` statement converting `text` for `a`.
    private static String conversion(Arg a) {
        Kind k = a.element;
        String target = a.elementSource;
        switch (k) {
            case STRING:
                return "return text;";
            case FILE:
                return "return new java.io.File(text);";
            case BOOLEAN:
            case BOXED_BOOLEAN:
                return "if (\"true\".equalsIgnoreCase(text)) return Boolean.TRUE; "
                        + "if (\"false\".equalsIgnoreCase(text)) return Boolean.FALSE; "
                        + "throw new IllegalArgumentException(\"expected true or false\");";
            case INT:
            case BOXED_INT:
                return "return Integer.valueOf(Integer.parseInt(text.trim()));";
            case LONG:
            case BOXED_LONG:
                return "return Long.valueOf(Long.parseLong(text.trim()));";
            case SHORT:
            case BOXED_SHORT:
                return "return Short.valueOf(Short.parseShort(text.trim()));";
            case BYTE:
            case BOXED_BYTE:
                return "return Byte.valueOf(Byte.parseByte(text.trim()));";
            case DOUBLE:
            case BOXED_DOUBLE:
                return "return Double.valueOf(Double.parseDouble(text.trim()));";
            case FLOAT:
            case BOXED_FLOAT:
                return "return Float.valueOf(Float.parseFloat(text.trim()));";
            case CHAR:
            case BOXED_CHAR:
                return "if (text.length() != 1) throw new IllegalArgumentException("
                        + "\"expected a single character\"); "
                        + "return Character.valueOf(text.charAt(0));";
            case ENUM:
                // A loop, not valueOf: its failure names every value the option
                // takes, which is the message a user needs.
                return "{ " + target + "[] all = " + target + ".values(); "
                        + "StringBuilder names = new StringBuilder(); "
                        + "for (int i = 0; i < all.length; i++) { "
                        + "if (all[i].name().equals(text)) return all[i]; "
                        + "names.append(i == 0 ? \"\" : \", \").append(all[i].name()); } "
                        + "throw new IllegalArgumentException(\"expected one of \" + names); }";
            case CUSTOM:
                return "return new " + a.converter + "().convert(text);";
            default:
                return "return text;";
        }
    }

    /// The statements storing `value` into `a`'s field.
    private static void storage(StringBuilder sb, Model m, Arg a) {
        String ind = "                ";
        if (!a.multiple) {
            sb.append(ind).append(assign(m, a.field, unbox(a.kind, a.elementSource, "value")))
              .append('\n');
            return;
        }
        sb.append(ind).append("java.util.List<?> l = (java.util.List<?>) value;\n");
        if (a.kind == Kind.ARRAY) {
            String elem = a.element.primitive != null ? a.element.primitive : a.elementSource;
            sb.append(ind).append(elem).append("[] a = new ").append(elem)
              .append("[l.size()];\n");
            sb.append(ind).append("for (int i = 0; i < a.length; i++) {\n");
            sb.append(ind).append("    a[i] = ").append(unbox(a.element, a.elementSource,
                    "l.get(i)")).append(";\n");
            sb.append(ind).append("}\n");
            sb.append(ind).append(assign(m, a.field, "a")).append('\n');
            return;
        }
        String impl = a.kind == Kind.SET ? "java.util.LinkedHashSet" : "java.util.ArrayList";
        String elem = boxedSource(a.element, a.elementSource);
        sb.append(ind).append("java.util.Collection<").append(elem).append("> c = new ")
          .append(impl).append("<").append(elem).append(">();\n");
        sb.append(ind).append("for (int i = 0; i < l.size(); i++) {\n");
        sb.append(ind).append("    c.add((").append(elem).append(") l.get(i));\n");
        sb.append(ind).append("}\n");
        String declared = a.kind == Kind.SET ? "java.util.Set" : "java.util.List";
        String cast = a.field.getDescriptor().equals("Ljava/util/Collection;")
                ? "c" : "(" + declared + "<" + elem + ">) c";
        sb.append(ind).append(assign(m, a.field, cast)).append('\n');
    }

    private static String boxedSource(Kind k, String source) {
        if (k.boxed != null && k != Kind.FILE) {
            return k.boxed;
        }
        return source;
    }

    /// `value` (an Object) as the field type of kind `k`.
    private static String unbox(Kind k, String source, String value) {
        if (k.primitive != null) {
            return "((" + k.boxed + ") " + value + ")." + k.primitive + "Value()";
        }
        return "(" + boxedSource(k, source) + ") " + value;
    }

    /// The statement storing `expr` into `field`: a plain assignment, or the
    /// injection setter woven into a private field's class.
    private static String assign(Model m, FieldInfo field, String expr) {
        if (m.plan.injectFields.containsKey(field.getName())) {
            return "t." + BackendWeaver.injectSetter(field.getName()) + "(" + expr + ");";
        }
        return "t." + field.getName() + " = " + expr + ";";
    }

    /// The entry point of a command-line tool written in Python: a module with
    /// Python sources, built against the backend runtime, and no `@Command`.
    ///
    /// Its `main` hands the arguments to `sys.argv` and exits with the status
    /// the program leaves, so `argparse`, `sys.exit` and an uncaught exception
    /// behave as they do under CPython. `sys.argv[0]` is the application name,
    /// which is what `argparse` prints as the program in its usage. A module
    /// that also serves HTTP from Java controllers keeps that processor's entry
    /// point, which replaces this marker; a Flask or FastAPI server written in
    /// Python is started by its own main module, so it takes this one.
    private void generatePythonEntryPoint(ProcessorContext ctx) throws ProcessingException {
        File modules = new File(ctx.getOutputClassDir(),
                PYTHON_MODULES.replace('/', File.separatorChar) + ".class");
        File projectDir = moduleDir(ctx);
        // Both: the transpiled program compiled into this tree, and the Python
        // sources it came from still in the project, so a class left in an
        // output directory by an earlier build is not mistaken for a program.
        if (!modules.isFile() || projectDir == null
                || !com.codename1.maven.PythonProjectBuilder.isPythonProject(
                        new File(projectDir, "src/main/python"))
                || !isBackendModule(ctx)) {
            return;
        }
        String application = PYTHON_APPLICATION.replace('/', '.');
        if (isNotOurOwnOutput(ctx, application)) {
            ctx.error(application + " already exists, and the entry point generated for the "
                    + "Python program would replace it. Rename that class.");
            return;
        }
        String appName = appName(ctx, new ArrayList<Model>());
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put(application, generatePythonApplication(appName));
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile the generated Python entry point: "
                    + ioe.getMessage(), ioe);
        }
        ctx.emitResource(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE, utf8(application));
        ctx.getLog().info("cn1: generated " + application + ", which runs the Python main module");
    }

    /// The module's own directory. The context's project directory is the one
    /// holding `codenameone_settings.properties`, which a Maven backend or tool
    /// does not have, so there it is null: the module is then found from the
    /// output directory, the first ancestor with a build file.
    static File moduleDir(ProcessorContext ctx) {
        if (ctx.getProjectDir() != null) {
            return ctx.getProjectDir();
        }
        File dir = ctx.getOutputClassDir();
        for (int i = 0; dir != null && i < 4; i++) {
            dir = dir.getParentFile();
            if (dir != null && (new File(dir, "pom.xml").isFile() || new File(dir, "build.gradle.kts").isFile()
                    || new File(dir, "build.gradle").isFile())) {
                return dir;
            }
        }
        return null;
    }

    static String generatePythonApplication(String appName) {
        StringBuilder sb = new StringBuilder();
        sb.append("package com.codename1.generated.python;\n\n");
        sb.append("// Generated for the Python program in this module. Do not edit.\n");
        sb.append(GENERATED_ANNOTATION).append('\n');
        sb.append("public final class PythonCliApplication {\n\n");
        sb.append("    private PythonCliApplication() {\n    }\n\n");
        sb.append("    /** The application's name, sys.argv[0] for the program. */\n");
        sb.append("    public static final String NAME = ").append(quote(appName)).append(";\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        com.codename1.python.runtime.Py.setArgv(NAME, args);\n");
        sb.append("        int code = PythonModules.runMain();\n");
        sb.append("        System.out.flush();\n");
        sb.append("        System.err.flush();\n");
        sb.append("        System.exit(code);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /// Whether the module builds against the backend runtime: on its compile
    /// classpath, or, in the native packager, compiled into the very tree being
    /// translated. An application has neither, and runs its Python from its
    /// Lifecycle instead.
    private static boolean isBackendModule(ProcessorContext ctx) {
        String entry = "com/codename1/cli/CommandLine.class";
        return new File(ctx.getOutputClassDir(), entry.replace('/', File.separatorChar)).isFile()
                || onCompileClasspath(ctx, entry);
    }

    /// Whether a class file is on the compile classpath, as a directory entry or
    /// a jar entry, without loading anything.
    private static boolean onCompileClasspath(ProcessorContext ctx, String entryName) {
        for (String element : ctx.getCompileClasspath()) {
            File file = new File(element);
            if (file.isDirectory()) {
                if (new File(file, entryName.replace('/', File.separatorChar)).isFile()) {
                    return true;
                }
            } else if (file.isFile()) {
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)) {
                    if (zip.getEntry(entryName) != null) {
                        return true;
                    }
                } catch (IOException err) {
                    ctx.getLog().warn("cn1: could not read " + file + ": " + err.getMessage());
                }
            }
        }
        return false;
    }

    String generateApplication(String packageName, String appName, List<Model> top) {
        StringBuilder sb = new StringBuilder();
        if (packageName.length() > 0) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        sb.append("// Generated from the @Command classes in this module. Do not edit.\n");
        sb.append(GENERATED_ANNOTATION).append('\n');
        sb.append("public final class CliApplication {\n\n");
        sb.append("    private CliApplication() {\n    }\n\n");
        sb.append("    /** The application's name, as the help prints it. */\n");
        sb.append("    public static final String NAME = ").append(quote(appName)).append(";\n\n");
        sb.append("    /** The top-level commands, freshly built. */\n");
        sb.append("    public static com.codename1.cli.CommandSpec[] commands() {\n");
        sb.append("        return new com.codename1.cli.CommandSpec[] {");
        for (int i = 0; i < top.size(); i++) {
            Model m = top.get(i);
            sb.append(i == 0 ? "\n            " : ",\n            ").append("new ")
              .append(qualify(m.packageName, m.bindingSimpleName())).append("().spec()");
        }
        sb.append("\n        };\n    }\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        int code = com.codename1.cli.CommandLine.run(NAME, commands(), args);\n");
        sb.append("        System.out.flush();\n");
        sb.append("        System.err.flush();\n");
        sb.append("        System.exit(code);\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---------------------------------------------------------------- helpers

    private static boolean isNotOurOwnOutput(ProcessorContext ctx, String binaryName) {
        AnnotatedClass existing = ctx.lookup(binaryName.replace('.', '/'));
        return existing != null && !existing.getClassAnnotations().containsKey(GENERATED);
    }

    static String qualify(String pkg, String simple) {
        return pkg.length() == 0 ? simple : pkg + "." + simple;
    }

    private static String[] strings(Object value) {
        if (value instanceof String) {
            return new String[] {(String) value};
        }
        if (!(value instanceof List)) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        for (Object o : (List<?>) value) {
            if (o instanceof String) {
                out.add((String) o);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static String join(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(l.trim());
        }
        return sb.toString();
    }

    private static String array(String[] values) {
        StringBuilder sb = new StringBuilder("new String[] {");
        for (int i = 0; i < values.length; i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(values[i]));
        }
        return sb.append('}').toString();
    }

    private static String nullable(String value) {
        return value == null || value.length() == 0 ? "null" : quote(value);
    }

    /// A Java string literal. Every character outside printable ASCII is
    /// written as an escape, because the generated source compiles as ASCII.
    static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c < 0x20 || c > 0x7e) {
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int p = hex.length(); p < 4; p++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String asciiLower(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException err) {
            throw new IllegalStateException(err);
        }
    }
}
