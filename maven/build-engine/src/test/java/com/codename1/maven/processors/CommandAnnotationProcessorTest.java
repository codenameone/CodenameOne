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

import com.codename1.build.SystemStreamLog;
import com.codename1.cli.CommandLine;
import com.codename1.cli.CommandSpec;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationProcessor;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// `@Command` classes: the bindings the processor generates, compiled and run
/// against the real `com.codename1.cli` runtime, and the declarations it refuses.
public class CommandAnnotationProcessorTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String IMPORTS = "package com.example;\n"
            + "import com.codename1.cli.CommandLine;\n"
            + "import com.codename1.cli.CommandLine.Command;\n"
            + "import com.codename1.cli.CommandLine.Option;\n"
            + "import com.codename1.cli.CommandLine.Parameters;\n"
            + "import com.codename1.cli.CommandLine.ParentCommand;\n"
            + "import com.codename1.cli.CommandLine.Spec;\n"
            + "import com.codename1.cli.CommandSpec;\n"
            + "import java.util.List;\n";

    /// A command using most of the surface: private fields, a flag cluster, an
    /// enum, a split list, a File, a default from the environment, positionals
    /// and a subcommand reaching its parent.
    private static final String GREET = IMPORTS
            + "@Command(name = \"greet\", mixinStandardHelpOptions = true, version = \"greet 1.0\",\n"
            + "        description = \"Prints a greeting.\", subcommands = Shout.class)\n"
            + "public class Greet implements Runnable {\n"
            + "    public static String last;\n"
            + "    public enum Mode { PLAIN, FANCY }\n"
            + "    @Option(names = {\"-n\", \"--count\"}, description = \"How many times.\", defaultValue = \"1\")\n"
            + "    private int count;\n"
            + "    @Option(names = {\"-v\", \"--verbose\"}) private boolean verbose;\n"
            + "    @Option(names = \"-q\") boolean quiet;\n"
            + "    @Option(names = \"--mode\", defaultValue = \"PLAIN\") private Mode mode;\n"
            + "    @Option(names = \"--tag\", split = \",\") private List<String> tags;\n"
            + "    @Option(names = \"--out\") private java.io.File out;\n"
            + "    @Option(names = \"--color\", negatable = true, defaultValue = \"true\") private boolean color;\n"
            + "    @Option(names = \"--home\", defaultValue = \"${env:CN1_CLI_TEST_UNSET:-none}\") String home;\n"
            + "    @Parameters(paramLabel = \"<name>\", description = \"Who to greet.\") private String name;\n"
            + "    @Parameters(paramLabel = \"<extra>\") private int[] extra;\n"
            + "    @Spec CommandSpec spec;\n"
            + "    public String describe() { return name; }\n"
            + "    public void run() {\n"
            + "        StringBuilder sb = new StringBuilder();\n"
            + "        sb.append(spec.name()).append(' ').append(name).append(' ').append(count)\n"
            + "          .append(' ').append(verbose).append(' ').append(quiet).append(' ').append(mode)\n"
            + "          .append(' ').append(tags).append(' ').append(out).append(' ').append(color)\n"
            + "          .append(' ').append(home).append(' ').append(java.util.Arrays.toString(extra));\n"
            + "        last = sb.toString();\n"
            + "    }\n"
            + "}\n";

    private static final String SHOUT = IMPORTS
            + "@Command(name = \"shout\", description = \"Shouts instead.\")\n"
            + "class Shout implements java.util.concurrent.Callable<Integer> {\n"
            + "    @ParentCommand private Greet parent;\n"
            + "    @Option(names = \"--times\", required = true) private Integer times;\n"
            + "    public Integer call() {\n"
            + "        Greet.last = \"shout \" + times + \" \" + (parent != null);\n"
            + "        return 7;\n"
            + "    }\n"
            + "}\n";

    @Test
    public void bindsEveryKindOfArgument() throws Exception {
        Run r = runGreet("-vq", "-n", "3", "--mode=FANCY", "--tag", "a,b", "--tag=c",
                "--out", "x.txt", "--no-color", "bob", "1", "2");
        assertEquals(r.err, 0, r.code);
        assertEquals("greet bob 3 true true FANCY [a, b, c] x.txt false none [1, 2]",
                lastLine(r));
    }

    @Test
    public void appliesDefaults() throws Exception {
        Run r = runGreet("alice");
        assertEquals(r.err, 0, r.code);
        assertEquals("greet alice 1 false false PLAIN null null true none null", lastLine(r));
    }

    @Test
    public void attachedShortValueAndEndOfOptions() throws Exception {
        Run r = runGreet("-n5", "--", "-dash");
        assertEquals(r.err, 0, r.code);
        assertTrue(lastLine(r), lastLine(r).startsWith("greet -dash 5 "));
    }

    @Test
    public void runsSubcommandWithItsParentAndExitCode() throws Exception {
        Run r = runGreet("shout", "--times", "4");
        assertEquals(r.err, 7, r.code);
        assertEquals("shout 4 true", lastLine(r));
    }

    @Test
    public void helpAndVersion() throws Exception {
        Run help = runGreet("--help");
        assertEquals(0, help.code);
        assertTrue(help.out, help.out.startsWith("Usage: greet [-hqvV]"));
        assertTrue(help.out, help.out.contains("<name>"));
        assertTrue(help.out, help.out.contains("-n, --count=<count>"));
        assertTrue(help.out, help.out.contains("How many times. Default: 1"));
        assertTrue(help.out, help.out.contains("--[no-]color"));
        assertTrue(help.out, help.out.contains("Commands:"));
        assertTrue(help.out, help.out.contains("shout"));
        Run version = runGreet("-V");
        assertEquals(0, version.code);
        assertEquals("greet 1.0", version.out.trim());
        Run subHelp = runGreet("shout", "--help");
        assertEquals(2, subHelp.code);
    }

    @Test
    public void usageErrorsExitWithTwo() throws Exception {
        assertUsageError(runGreet("--nope", "x"), "Unknown option: '--nope'");
        assertUsageError(runGreet(), "Missing required parameter: '<name>'");
        assertUsageError(runGreet("-n", "abc", "x"), "Invalid value for option '--count': 'abc'");
        assertUsageError(runGreet("--mode", "LOUD", "x"), "expected one of PLAIN, FANCY");
        assertUsageError(runGreet("-n", "1", "-n", "2", "x"), "should be specified only once");
        assertUsageError(runGreet("shout"), "Missing required option: '--times=<times>'");
        assertUsageError(runGreet("x", "1", "y"), "Invalid value for positional parameter");
    }

    @Test
    public void argumentFilesExpand() throws Exception {
        File args = tmp.newFile("args.txt");
        Files.write(args.toPath(), "# options\n-n 2 \"bob smith\"\n".getBytes(StandardCharsets.UTF_8));
        Run r = runGreet("@" + args.getAbsolutePath());
        assertEquals(r.err, 0, r.code);
        assertTrue(lastLine(r), lastLine(r).startsWith("greet bob smith 2 "));
    }

    @Test
    public void severalTopLevelCommandsBecomeOneGroup() throws Exception {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Alpha", IMPORTS
                + "@Command(name = \"alpha\") public class Alpha implements Runnable {\n"
                + "  public static String last; public void run() { last = \"alpha\"; } }\n");
        sources.put("com.example.Beta", IMPORTS
                + "@Command(name = \"beta\") public class Beta implements Runnable {\n"
                + "  public void run() { Alpha.last = \"beta\"; } }\n");
        File classes = compile(sources);
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor());
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        ClassLoader loader = loader(classes);
        CommandSpec[] top = commands(loader);
        assertEquals(2, top.length);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(err, true));
        int bare;
        try {
            bare = CommandLine.run("tool", commands(loader), new String[0]);
        } finally {
            System.setErr(oldErr);
        }
        assertEquals(2, bare);
        assertTrue(err.toString(), err.toString().contains("Usage: tool [-h] [COMMAND]"));
        assertEquals(0, CommandLine.run("tool", commands(loader), new String[] {"beta"}));
        assertEquals("beta", loader.loadClass("com.example.Alpha").getField("last").get(null));
    }

    /// The group is named by `cn1.cli.name` in the module's application.properties,
    /// which the JVM run and the native packager both hand this pass as the project
    /// directory, else after that directory.
    @Test
    public void groupNameComesFromApplicationProperties() throws Exception {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Alpha", IMPORTS
                + "@Command(name = \"alpha\") public class Alpha implements Runnable { public void run() { } }\n");
        sources.put("com.example.Beta", IMPORTS
                + "@Command(name = \"beta\") public class Beta implements Runnable { public void run() { } }\n");
        File project = tmp.newFolder("mytool");
        assertEquals("mytool", groupName(sources, project));
        java.nio.file.Files.write(new File(project, "application.properties").toPath(),
                "# the tool\ncn1.cli.name = notes\n".getBytes("UTF-8"));
        assertEquals("notes", groupName(sources, project));
    }

    private String groupName(Map<String, String> sources, File project) throws Exception {
        File classes = compile(sources);
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), ClassScanner.scan(classes),
                new SystemStreamLog(), project, null, null, null, "UTF-8", null);
        CommandAnnotationProcessor proc = new CommandAnnotationProcessor();
        proc.start(ctx);
        for (AnnotatedClass cls : ctx.getClassIndex().values()) {
            proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        return (String) loader(classes).loadClass("com.example.CliApplication").getField("NAME").get(null);
    }

    @Test
    public void pythonProgramBecomesTheEntryPoint() throws Exception {
        // What the transpiler and the Python runtime provide, reduced to the two
        // members the generated main calls.
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.codename1.python.runtime.Py", "package com.codename1.python.runtime;\n"
                + "public class Py { public static String[] argv;\n"
                + "  public static void setArgv(String p, String[] a) { argv = a; } }\n");
        sources.put("com.codename1.generated.python.PythonModules", "package com.codename1.generated.python;\n"
                + "public class PythonModules { public static int runMain() { return 0; } }\n");
        File classes = compile(sources);
        File project = tmp.newFolder("pytool");
        List<String> cp = new ArrayList<String>();
        for (File f : classpath()) {
            cp.add(f.getPath());
        }
        // No Python sources in the project: a class left by an earlier build is not a program.
        assertNull(pythonEntryPoint(classes, project, cp));
        File python = new File(project, "src/main/python");
        assertTrue(python.mkdirs());
        java.nio.file.Files.write(new File(python, "main.py").toPath(), "print(1)\n".getBytes("UTF-8"));
        // Not a backend module: an application runs its Python from its Lifecycle.
        assertNull(pythonEntryPoint(classes, project, new ArrayList<String>()));
        assertEquals("com.codename1.generated.python.PythonCliApplication",
                pythonEntryPoint(classes, project, cp));
        Class<?> app = loader(classes).loadClass("com.codename1.generated.python.PythonCliApplication");
        assertEquals("pytool", app.getField("NAME").get(null));
        String source = CommandAnnotationProcessor.generatePythonApplication("pytool");
        assertTrue(source, source.contains("Py.setArgv(NAME, args)"));
        assertTrue(source, source.contains("System.exit(code)"));
    }

    private String pythonEntryPoint(File classes, File project, List<String> cp) throws Exception {
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), ClassScanner.scan(classes),
                new SystemStreamLog(), project, null, null, null, "UTF-8", cp);
        CommandAnnotationProcessor proc = new CommandAnnotationProcessor();
        proc.start(ctx);
        proc.finish(ctx);
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        byte[] marker = ctx.getEmittedResources().get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        return marker == null ? null : new String(marker, "UTF-8");
    }

    @Test
    public void backendServesWithNoArgumentsAndRunsCommandsOtherwise() throws Exception {
        final int[] served = {0};
        CommandLine.Server server = new CommandLine.Server() {
            @Override
            public void serve() {
                served[0]++;
            }
        };
        File classes = compile(greetSources());
        process(classes, new CommandAnnotationProcessor());
        ClassLoader loader = loader(classes);
        assertEquals(CommandLine.SERVED, CommandLine.serve("app", commands(loader), server, new String[0]));
        assertEquals(1, served[0]);
        // One top-level command: the server joins it as its "serve" subcommand.
        assertEquals(CommandLine.SERVED, CommandLine.serve("app", commands(loader), server,
                new String[] {"serve"}));
        assertEquals(2, served[0]);
        assertEquals(0, CommandLine.serve("app", commands(loader), server,
                new String[] {"bob"}));
        assertEquals(2, served[0]);
        // A backend with no commands keeps ignoring what it is passed.
        assertEquals(CommandLine.SERVED, CommandLine.serve("app", null, server, new String[] {"--port=1"}));
        assertEquals(3, served[0]);
    }

    @Test
    public void backendEntryPointHandsTheCommandLineToTheCommands() throws Exception {
        Map<String, String> sources = greetSources();
        sources.put("com.example.Hello", "package com.example;\n"
                + "import com.codename1.backend.annotations.GetMapping;\n"
                + "import com.codename1.backend.annotations.RestController;\n"
                + "@RestController public class Hello {\n"
                + "  @GetMapping(\"/hello\") public String hello() { return \"hi\"; } }\n");
        File classes = compile(sources);
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor(),
                new RestControllerAnnotationProcessor());
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertEquals("com.example.BackendApplication", new String(ctx.getEmittedResources()
                .get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE), "UTF-8"));
        assertTrue(new File(classes, "com/example/BackendApplication$Cn1Server.class").isFile());
        assertTrue(new File(classes, "com/example/CliApplication.class").isFile());
    }

    @Test
    public void commandOnlyModuleGetsItsOwnEntryPoint() throws Exception {
        File classes = compile(greetSources());
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor(),
                new RestControllerAnnotationProcessor());
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        assertEquals("com.example.CliApplication", new String(ctx.getEmittedResources()
                .get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE), "UTF-8"));
        assertFalse(new File(classes, "com/example/BackendApplication.class").exists());
    }

    @Test
    public void refusesWhatItCannotBind() throws Exception {
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"-x\") static int x; public void run() {} }", "is static");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"-x\") final int x = 1; public void run() {} }", "is final");
        assertRefused("@Command public class Bad { }", "has nothing to run");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"-x\") int a; @Option(names = \"-x\") int b;\n"
                + "  public void run() {} }", "used twice");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"-x\") Object a; public void run() {} }",
                "does not convert");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"x\") int a; public void run() {} }", "starts with '-'");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Parameters(index = \"0\") String a; @Parameters(index = \"0\") String b;\n"
                + "  public void run() {} }", "also takes");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  Bad(int x) {} public void run() {} }", "no constructor without");
        assertRefused("public class Bad {\n"
                + "  @Option(names = \"-x\") int a; }", "not a concrete @Command class");
    }

    @Test
    public void refusesDeclarationsThatCannotWork() throws Exception {
        assertRefused("@Command(mixinStandardHelpOptions = true) public class Bad implements Runnable {\n"
                + "  @Option(names = \"-V\") boolean verbose; public void run() {} }",
                "mixinStandardHelpOptions already gives");
        assertRefused("@Command public class Bad implements Runnable {\n"
                + "  @Option(names = \"--x\", split = \",\") String x; public void run() {} }",
                "holds one value");
        assertRefused("@Command(subcommands = {A.class, B.class}) public class Bad {}\n"
                + "@Command(name = \"add\") class A implements Runnable { public void run() {} }\n"
                + "@Command(name = \"put\", aliases = \"add\") class B implements Runnable {\n"
                + "  public void run() {} }", "only one of them can be reached");
        assertRefused("abstract class Base { @Option(names = \"-v\") private boolean v; }\n"
                + "@Command public class Bad extends Base implements Runnable {\n"
                + "  public void run() {} }", "is private");
    }

    /// The fixes a red-team review asked for, each run through the generated code.
    @Test
    public void parsesTheEdgesLikePicocli() throws Exception {
        String body = "abstract class Base { @Option(names = \"-l\") boolean loud; }\n"
                + "class Conv implements CommandLine.ITypeConverter<Object> {\n"
                + "  public Object convert(String s) { return s.startsWith(\"#\") ? s : (Object) Integer.valueOf(s); } }\n"
                + "@Command(name = \"t\", mixinStandardHelpOptions = true)\n"
                + "public class Edge extends Base implements java.util.concurrent.Callable<Integer> {\n"
                + "  public static String last;\n"
                + "  @Option(names = {\"-c\", \"--count\"}) int count;\n"
                + "  @Option(names = \"--port\", converter = Conv.class) Integer port;\n"
                + "  @Option(names = \"--x\", arity = \"1\") boolean x;\n"
                + "  @Parameters(arity = \"0..1\", paramLabel = \"<name>\") String name;\n"
                + "  public Integer call() { last = loud + \" \" + count + \" \" + port + \" \" + x;\n"
                + "    return count == 9 ? -1 : 0; } }";
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Edge", IMPORTS + body + "\n");
        File classes = compile(sources);
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor());
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        ClassLoader loader = loader(classes);
        // An inherited option, and = after a cluster.
        assertEquals(0, CommandLine.run("t", commands(loader), new String[] {"-lc=2", "--port", "80"}));
        assertEquals("true 2 80 false", loader.loadClass("com.example.Edge").getField("last").get(null));
        assertEquals(0, CommandLine.run("t", commands(loader), new String[] {"--x", "true"}));
        assertEquals("false 0 null true", loader.loadClass("com.example.Edge").getField("last").get(null));
        // A negative exit code is the command's to give.
        assertEquals(-1, CommandLine.run("t", commands(loader), new String[] {"-c", "9"}));
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err, true));
        System.setOut(new PrintStream(out, true));
        int wrongType;
        int bareX;
        int badX;
        try {
            wrongType = CommandLine.run("t", commands(loader), new String[] {"--port", "#80"});
            bareX = CommandLine.run("t", commands(loader), new String[] {"--x"});
            badX = CommandLine.run("t", commands(loader), new String[] {"--x", "yes"});
            CommandLine.run("t", commands(loader), new String[] {"--help"});
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
        }
        // A converter answering the wrong type is an invalid value, never a cast.
        assertEquals(err.toString(), 2, wrongType);
        assertTrue(err.toString(), err.toString().contains("returned a java.lang.String, not a java.lang.Integer"));
        assertEquals(err.toString(), 2, bareX);
        assertEquals(err.toString(), 2, badX);
        assertTrue(out.toString(), out.toString().contains("[<name>]"));
    }

    @Test
    public void typeArgumentReadsTheElementDescriptor() {
        assertEquals("Ljava/lang/String;",
                CommandAnnotationProcessor.typeArgument("Ljava/util/List<Ljava/lang/String;>;"));
        assertEquals(null, CommandAnnotationProcessor.typeArgument("Ljava/util/List;"));
        assertEquals(null, CommandAnnotationProcessor.typeArgument(
                "Ljava/util/List<Ljava/util/List<Ljava/lang/String;>;>;"));
    }

    // ---------------------------------------------------------------- helpers

    /// The developer guide's snippets, read from the guide's own demo tree, so a
    /// chapter cannot show a command this processor would refuse.
    @Test
    public void developerGuideSnippetsBuildAndRun() throws Exception {
        File dir = new File("../../docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/cli");
        assertTrue(dir.getAbsolutePath(), dir.isDirectory());
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (String name : new String[] {"Greet", "Notes", "Ports"}) {
            sources.put("com.codenameone.developerguide.backend.cli." + name, new String(
                    java.nio.file.Files.readAllBytes(new File(dir, name + ".java").toPath()), "UTF-8"));
        }
        File classes = compile(sources);
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor());
        assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
        ClassLoader loader = loader(classes);
        CommandSpec[] top = (CommandSpec[]) loader
                .loadClass("com.codenameone.developerguide.backend.cli.CliApplication")
                .getMethod("commands").invoke(null);
        assertEquals(3, top.length);
        assertEquals("ports 8000 to 8010\n", runIn(top, "app", "listen", "--ports", "8000-8010").out);
        assertEquals("adding to n.txt: buy milk\n",
                runIn(top, "app", "notes", "-f", "n.txt", "add", "--tag", "a,b", "buy", "milk").out);
        assertEquals("app notes show sorted OLDEST\n", runIn(top, "app", "notes", "ls", "--sort", "OLDEST").out);
        assertEquals("HELLO, Ada!\nHELLO, Ada!\n", runIn(top, "app", "greet", "-lc", "2", "Ada").out);
        Run bad = runIn(top, "app", "listen", "--ports", "x");
        assertEquals(bad.err, 2, bad.code);
    }

    private static Run runIn(CommandSpec[] top, String app, String... args) throws Exception {
        java.io.PrintStream oldOut = System.out;
        java.io.PrintStream oldErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Run r = new Run();
        try {
            System.setOut(new PrintStream(out, true, "UTF-8"));
            System.setErr(new PrintStream(err, true, "UTF-8"));
            r.code = CommandLine.run(app, top, args);
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
        r.out = out.toString("UTF-8");
        r.err = err.toString("UTF-8");
        return r;
    }

    private static final class Run {
        int code;
        String out;
        String err;
        ClassLoader loader;
    }

    private Map<String, String> greetSources() {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Greet", GREET);
        sources.put("com.example.Shout", SHOUT);
        return sources;
    }

    private ClassLoader greetLoader;

    private Run runGreet(String... args) throws Exception {
        if (greetLoader == null) {
            File classes = compile(greetSources());
            ProcessorContext ctx = process(classes, new CommandAnnotationProcessor());
            assertFalse(String.valueOf(ctx.getErrors()), ctx.hasErrors());
            greetLoader = loader(classes);
        }
        greetLoader.loadClass("com.example.Greet").getField("last").set(null, null);
        CommandSpec[] top = commands(greetLoader);
        assertEquals(1, top.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Run r = new Run();
        r.code = new CommandLine(top[0]).setOut(new PrintStream(out, true, "UTF-8"))
                .setErr(new PrintStream(err, true, "UTF-8")).execute(args);
        r.out = out.toString("UTF-8");
        r.err = err.toString("UTF-8");
        r.loader = greetLoader;
        return r;
    }

    private static String lastLine(Run r) throws Exception {
        return String.valueOf(r.loader.loadClass("com.example.Greet").getField("last").get(null));
    }

    private static void assertUsageError(Run r, String message) {
        assertEquals(r.err, 2, r.code);
        assertTrue("expected '" + message + "' in:\n" + r.err, r.err.contains(message));
        assertTrue(r.err, r.err.contains("Usage: "));
    }

    private void assertRefused(String body, String message) throws Exception {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put("com.example.Bad", IMPORTS + body + "\n");
        File classes = compile(sources);
        ProcessorContext ctx = process(classes, new CommandAnnotationProcessor());
        assertTrue("accepted: " + body, ctx.hasErrors());
        assertTrue("expected '" + message + "' in " + ctx.getErrors(),
                String.valueOf(ctx.getErrors()).contains(message));
        assertFalse(new File(classes, "com/example/CliApplication.class").exists());
    }

    private File compile(Map<String, String> sources) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(sources, classes, classpath());
        return classes;
    }

    private ProcessorContext process(File classes, AnnotationProcessor... processors)
            throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog());
        for (AnnotationProcessor proc : processors) {
            proc.start(ctx);
            for (AnnotatedClass cls : index.values()) {
                proc.processClass(cls, ctx);
            }
            proc.finish(ctx);
        }
        return ctx;
    }

    private static ClassLoader loader(File classes) throws Exception {
        return new URLClassLoader(new URL[] {classes.toURI().toURL()},
                CommandAnnotationProcessorTest.class.getClassLoader());
    }

    private static CommandSpec[] commands(ClassLoader loader) throws Exception {
        Object specs = loader.loadClass("com.example.CliApplication").getMethod("commands")
                .invoke(null);
        return (CommandSpec[]) specs;
    }

    private static List<File> classpath() {
        List<File> cp = new ArrayList<File>();
        for (String element : System.getProperty("java.class.path").split(File.pathSeparator)) {
            cp.add(new File(element));
        }
        return cp;
    }
}
