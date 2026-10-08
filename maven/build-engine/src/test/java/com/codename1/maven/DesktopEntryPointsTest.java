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
package com.codename1.maven;

import com.codename1.builders.BuildException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The main class the remap step generates for a desktop application, end
/// to end: a fixture is compiled by javac against the layers' real runtime
/// jars, remapped with an entry record, and the generated class is loaded
/// and made to start it. The same directory then has to pass the compliance
/// check, as a project's build would run it next.
public class DesktopEntryPointsTest {

    private static final String SWING_MAIN = "package com.acme.swingapp;\n"
            + "public class Main {\n"
            + "    public static int ran = -1;\n"
            + "    static javax.swing.JLabel label;\n"
            + "    public static void main(String[] args) { ran = args.length; }\n"
            + "}\n";

    private static final String FX_APP = "package com.acme.fxapp;\n"
            + "public class Shop extends javafx.application.Application {\n"
            + "    public static void main(String[] args) { launch(args); }\n"
            + "    public void start(javafx.stage.Stage stage) { }\n"
            + "}\n";

    private static final String FX_LAUNCHER = "package com.acme.fxapp;\n"
            + "public class Launcher {\n"
            + "    public static void main(String[] args) { Shop.main(args); }\n"
            + "}\n";

    /// Calls of JDK members the device's classes lack. `pure()` is the part
    /// that needs no running framework, so the test can compare what it
    /// answers before the remap, on the JDK, with what it answers after.
    private static final String GAPS = "package com.acme.swingapp;\n"
            + "import java.util.*;\n"
            + "import java.util.logging.*;\n"
            + "import java.util.prefs.Preferences;\n"
            + "public class Gaps {\n"
            + "    static class Audit extends Level { Audit() { super(\"AUDIT\", 850); } }\n"
            + "    public static String pure() throws Exception {\n"
            + "        StringBuilder o = new StringBuilder();\n"
            + "        StringBuilder sb = new StringBuilder(\"hello world\");\n"
            + "        StringBuffer sf = new StringBuffer(\"hello world\");\n"
            + "        o.append(String.join(\",\", \"a\", \"b\")).append(String.join(\"-\", Arrays.asList(\"x\", \"y\")));\n"
            + "        o.append(String.valueOf(new char[] {'q', 'r'})).append(\"ab12\".matches(\"[a-z]+[0-9]+\"));\n"
            + "        o.append(\"ab12x\".matches(\"[a-z]+[0-9]+\")).append(\"MiXed\".toLowerCase(Locale.ROOT));\n"
            + "        o.append(String.format(Locale.US, \"%d-%s\", 4, \"z\"));\n"
            + "        o.append(sb.indexOf(\"o\")).append(sb.indexOf(\"o\", 5)).append(sb.lastIndexOf(\"o\"));\n"
            + "        o.append(sb.substring(6)).append(sb.substring(0, 5)).append(sb.replace(0, 5, \"bye\"));\n"
            + "        o.append(sb.insert(0, new char[] {'>', ' '}));\n"
            + "        o.append(sf.indexOf(\"w\")).append(sf.substring(2, 4)).append(sf.replace(6, 99, \"all\"));\n"
            + "        o.append(Character.toString('c')).append(Character.isLetter('x')).append(Character.isLetter('1'));\n"
            + "        o.append(Character.isLetterOrDigit('_')).append(Character.forDigit(11, 16));\n"
            + "        o.append(Character.isISOControl('\\n')).append(Character.getNumericValue('7'));\n"
            + "        o.append(Character.isDigit((int) '5')).append(Character.toUpperCase((int) 'k'));\n"
            + "        o.append(Integer.max(3, 9)).append(Integer.min(3, 9)).append(Integer.sum(3, 9));\n"
            + "        o.append(Integer.bitCount(0xff0f)).append(Integer.highestOneBit(100));\n"
            + "        o.append(Integer.lowestOneBit(100)).append(Integer.numberOfTrailingZeros(64));\n"
            + "        o.append(Integer.reverse(1)).append(Integer.rotateLeft(1, 33)).append(Integer.rotateRight(1, 1));\n"
            + "        o.append(Integer.decode(\"-0x1F\")).append(Integer.decode(\"010\")).append(Integer.decode(\"#ff\"));\n"
            + "        o.append(Long.valueOf(\"-77\")).append(Long.valueOf(\"zz\", 36)).append(Long.decode(\"0x10\"));\n"
            + "        o.append(Long.toHexString(-2L)).append(Long.toBinaryString(5L)).append(Long.toOctalString(64L));\n"
            + "        o.append(Long.signum(-5L)).append(Long.bitCount(-1L)).append(Long.numberOfLeadingZeros(1L));\n"
            + "        o.append(Long.numberOfTrailingZeros(1L << 40)).append(Long.highestOneBit(1000L));\n"
            + "        o.append(Long.reverse(1L)).append(Long.rotateLeft(1L, 65)).append(Long.max(4L, 5L));\n"
            + "        o.append(Short.valueOf(\"12\")).append(Byte.valueOf(\"-3\")).append(Short.decode(\"0x7f\"));\n"
            + "        o.append(Byte.decode(\"7\")).append(Short.toString((short) 5)).append(Byte.toString((byte) 6));\n"
            + "        o.append(Double.isFinite(1 / 0.0)).append(Float.isFinite(2f)).append(Double.max(1, 2));\n"
            + "        o.append(Float.sum(1f, 2f)).append(Boolean.toString(true)).append(Boolean.logicalXor(true, false));\n"
            + "        o.append(Math.signum(-3.5)).append(Math.hypot(3, 4)).append(Math.rint(2.5)).append(Math.rint(3.5));\n"
            + "        o.append(Math.rint(-0.2)).append(Math.floorDiv(-7, 2)).append(Math.floorMod(-7, 2));\n"
            + "        o.append(Math.floorMod(-7L, 3L)).append(Math.addExact(2, 3)).append(Math.toIntExact(9L));\n"
            + "        o.append(Math.multiplyExact(1L << 20, 1L << 20)).append(Math.negateExact(4));\n"
            + "        o.append(Math.round(Math.pow(2, 10))).append(Math.round(Math.cbrt(27) * 1000));\n"
            + "        o.append(Math.round(Math.exp(1) * 1000)).append(Math.round(Math.atan2(1, 1) * 1000));\n"
            + "        o.append(Math.round(Math.sinh(1) * 1000)).append(Math.round(Math.tanh(0.5) * 1000));\n"
            + "        o.append(Math.round(StrictMath.log10(1000)));\n"
            + "        double r = Math.random();\n"
            + "        o.append(r >= 0 && r < 1);\n"
            + "        try { Math.addExact(Integer.MAX_VALUE, 1); } catch (ArithmeticException e) { o.append(\"ovf\"); }\n"
            + "        try { Math.floorDiv(1, 0); } catch (ArithmeticException e) { o.append(\"div\"); }\n"
            + "        try {\n"
            + "            throw new java.lang.reflect.InvocationTargetException(new IllegalStateException(\"t\"));\n"
            + "        } catch (java.lang.reflect.InvocationTargetException e) {\n"
            + "            o.append(e.getCause().getMessage()).append(e.getTargetException() == e.getCause());\n"
            + "        }\n"
            + "        Level audit = new Audit();\n"
            + "        o.append(audit.getName()).append(audit.intValue()).append(Level.parse(\"FINE\").intValue());\n"
            + "        o.append(Level.WARNING).append(Level.parse(\"800\") == Level.INFO);\n"
            + "        Thread t = new Thread();\n"
            + "        t.setDaemon(true);\n"
            + "        o.append(t.isInterrupted()).append(Thread.interrupted());\n"
            + "        o.append(System.lineSeparator().length()).append(System.getenv(\"NO_SUCH_VARIABLE_HERE\"));\n"
            + "        return o.toString();\n"
            + "    }\n"
            + "    public static void framework() throws Exception {\n"
            + "        Logger log = Logger.getLogger(\"acme\");\n"
            + "        log.setLevel(Level.FINE);\n"
            + "        log.log(Level.SEVERE, \"failed {0}\", \"x\");\n"
            + "        log.log(Level.WARNING, \"failed\", new RuntimeException());\n"
            + "        log.info(\"started\");\n"
            + "        log.fine(() -> \"lazily\");\n"
            + "        Preferences prefs = Preferences.userNodeForPackage(Gaps.class).node(\"ui\");\n"
            + "        prefs.putInt(\"width\", prefs.getInt(\"width\", 100) + 1);\n"
            + "        prefs.put(\"name\", prefs.get(\"name\", \"\"));\n"
            + "        prefs.putBoolean(\"shown\", !prefs.getBoolean(\"shown\", false));\n"
            + "        prefs.remove(\"name\");\n"
            + "        prefs.flush();\n"
            + "        String home = System.getProperty(\"user.home\") + System.getProperty(\"os.name\", \"?\");\n"
            + "        System.setProperty(\"acme.home\", home);\n"
            + "        Runtime.getRuntime().addShutdownHook(new Thread());\n"
            + "        int cpus = Runtime.getRuntime().availableProcessors();\n"
            + "        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> error.printStackTrace());\n"
            + "        Thread.UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();\n"
            + "        new Thread().join(cpus);\n"
            + "        if (h == null) { System.exit(1); }\n"
            + "    }\n"
            + "}\n";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    private List<File> classpath() throws Exception {
        if (scratch == null) {
            scratch = tmp.newFolder("jars");
        }
        return Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch));
    }

    private File compile(String... nameThenSource) throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compileAgainst(classpath(), tmp.newFolder(), classes, nameThenSource);
        return classes;
    }

    private File record(String... lines) throws Exception {
        File dir = tmp.newFolder();
        File record = DesktopSources.entryRecord(dir);
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }
        Files.write(record.toPath(), text.toString().getBytes("UTF-8"));
        return record;
    }

    private CompatRemapper remapper(File classes, File record, String main) throws Exception {
        return new CompatRemapper(classes, classpath(), null, CompatRemapperTest.LOG)
                .withDesktopEntryRecord(record).withApplicationMain(main);
    }

    /// Loads a class of the remapped directory. The parent supplies the
    /// framework; whatever the remap relocated or generated is only here.
    private static Class<?> load(File classes, String name) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()},
                DesktopEntryPointsTest.class.getClassLoader());
        return loader.loadClass(name);
    }

    private static Object call(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private void assertCompliant(File classes) throws Exception {
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
    }

    private static void assertFails(CompatRemapper remapper, String... expected) throws Exception {
        try {
            remapper.run();
            fail("The remap must fail");
        } catch (BuildException e) {
            for (String text : expected) {
                assertTrue(e.getMessage(), e.getMessage().contains(text));
            }
        }
    }

    @Test
    public void aSwingApplicationIsStartedThroughItsMain() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN);
        assertTrue(remapper(classes, record("mainClass=com.acme.swingapp.Main", "kind=swing"), "com.acme.MyApp")
                .run());

        Class<?> main = load(classes, "com.acme.MyApp");
        assertEquals("com.codename1.desktopcompat.rt.DesktopLifecycle", main.getSuperclass().getName());
        Object lifecycle = main.getConstructor().newInstance();
        call(lifecycle, "runMain");
        Class<?> fixture = main.getClassLoader().loadClass("com.acme.swingapp.Main");
        assertEquals("main(new String[0]) was called", 0, fixture.getField("ran").getInt(null));
        // Before the application: its resources are registered first.
        assertTrue(CompatFixtures.members(Files.readAllBytes(new File(classes, "com/acme/MyApp.class").toPath()))
                .contains("com/codename1/compat/jdk/CompatBoot.cn1Init"));
        assertFalse("Only the layer the application uses ships",
                new File(classes, "com/codename1/fxcompat").exists());
        assertCompliant(classes);
    }

    /// What a desktop application calls on `String`, the wrappers, `Math`,
    /// `System` and `Thread` beyond the device's class library, and the
    /// logging and preferences classes: each has to end up as something the
    /// device has, and to answer what the JDK answers.
    @Test
    public void theMembersTheDeviceLacksAreRedirectedAndAnswerAsTheJdkDoes() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/swingapp/Gaps.java", GAPS);
        File untouched = compile("com/acme/swingapp/Gaps.java", GAPS);
        Object expected = load(untouched, "com.acme.swingapp.Gaps").getMethod("pure").invoke(null);

        assertTrue(remapper(classes, record("mainClass=com.acme.swingapp.Main", "kind=swing"), "com.acme.MyApp")
                .run());
        assertCompliant(classes);
        java.util.Set<String> members = CompatFixtures.members(
                Files.readAllBytes(new File(classes, "com/acme/swingapp/Gaps.class").toPath()));
        assertTrue(members.toString(), members.contains("com/codename1/compat/jdk/JdkNumbers.decodeInteger"));
        assertTrue(members.toString(), members.contains("com/codename1/util/MathUtil.pow"));
        assertTrue(members.toString(), members.contains("com/codename1/compat/jdk/JdkSystem.getProperty"));
        assertEquals(expected, load(classes, "com.acme.swingapp.Gaps").getMethod("pure").invoke(null));
    }

    @Test
    public void aJavaFxApplicationIsCreatedAndItsMainIsNeverCalled() throws Exception {
        File classes = compile("com/acme/fxapp/Shop.java", FX_APP);
        assertTrue(remapper(classes, record("mainClass=com.acme.fxapp.Shop", "kind=javafx"), "com.acme.MyApp")
                .run());

        Class<?> main = load(classes, "com.acme.MyApp");
        assertEquals("com.codename1.fxcompat.rt.FxLifecycle", main.getSuperclass().getName());
        Object application = call(main.getConstructor().newInstance(), "createApplication");
        assertEquals("com.acme.fxapp.Shop", application.getClass().getName());
        assertEquals("com.codename1.fxcompat.javafx.application.Application",
                application.getClass().getSuperclass().getName());
        assertFalse(CompatFixtures.members(Files.readAllBytes(new File(classes, "com/acme/MyApp.class").toPath()))
                .contains("com/acme/fxapp/Shop.main"));
        // launch(args) in the application's own main compiles and does nothing.
        application.getClass().getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        // The runtime's own events extend java.util.EventObject, which the
        // device lacks: the copy that ships extends the shared class.
        assertEquals("com.codename1.compat.jdk.EventObject", application.getClass().getClassLoader()
                .loadClass("com.codename1.fxcompat.javafx.event.Event").getSuperclass().getName());
        assertCompliant(classes);
    }

    @Test
    public void theKindIsInferredFromTheClass() throws Exception {
        File swing = compile("com/acme/swingapp/Main.java", SWING_MAIN);
        assertTrue(remapper(swing, record("mainClass=com.acme.swingapp.Main"), "com.acme.MyApp").run());
        assertEquals("com.codename1.desktopcompat.rt.DesktopLifecycle",
                load(swing, "com.acme.MyApp").getSuperclass().getName());

        File fx = compile("com/acme/fxapp/Shop.java", FX_APP);
        assertTrue(remapper(fx, record("mainClass=com.acme.fxapp.Shop"), "com.acme.MyApp").run());
        assertEquals("com.codename1.fxcompat.rt.FxLifecycle", load(fx, "com.acme.MyApp").getSuperclass().getName());
    }

    @Test
    public void aLauncherClassMeansTheApplicationItLaunches() throws Exception {
        File classes = compile("com/acme/fxapp/Shop.java", FX_APP, "com/acme/fxapp/Launcher.java", FX_LAUNCHER);
        assertTrue(remapper(classes, record("mainClass=com.acme.fxapp.Launcher"), "com.acme.MyApp").run());
        Object application = call(load(classes, "com.acme.MyApp").getConstructor().newInstance(),
                "createApplication");
        assertEquals("com.acme.fxapp.Shop", application.getClass().getName());
    }

    @Test
    public void withoutARecordTheOnlyCandidateIsUsed() throws Exception {
        File swing = compile("com/acme/swingapp/Main.java", SWING_MAIN);
        assertTrue(remapper(swing, null, "com.acme.MyApp").run());
        assertEquals("com.codename1.desktopcompat.rt.DesktopLifecycle",
                load(swing, "com.acme.MyApp").getSuperclass().getName());

        // An Application subclass is the candidate even beside its launcher.
        File fx = compile("com/acme/fxapp/Shop.java", FX_APP, "com/acme/fxapp/Launcher.java", FX_LAUNCHER);
        assertTrue(remapper(fx, new File(tmp.getRoot(), "absent.properties"), "com.acme.MyApp").run());
        assertEquals("com.codename1.fxcompat.rt.FxLifecycle", load(fx, "com.acme.MyApp").getSuperclass().getName());
    }

    @Test
    public void withoutARecordSeveralCandidatesFailTheBuild() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/swingapp/Other.java",
                SWING_MAIN.replace("class Main", "class Other"));
        assertFails(remapper(classes, null, "com.acme.MyApp"), "several classes", DesktopSources.ENTRY_RECORD,
                "mainClass=", "com.acme.swingapp.Main (swing)", "com.acme.swingapp.Other (swing)");
    }

    @Test
    public void withoutAMainClassNameTheDefaultNameIsUsedOnlyForARecord() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN);
        assertTrue(remapper(classes, null, null).run());
        assertFalse(new File(classes, DesktopEntryPoints.DEFAULT_MAIN + ".class").exists());
        assertTrue(remapper(classes, record("mainClass=com.acme.swingapp.Main"), null).run());
        assertTrue(new File(classes, DesktopEntryPoints.DEFAULT_MAIN + ".class").isFile());
    }

    @Test
    public void aMainClassThatDoesNotExistListsTheOnesThatDo() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN);
        assertFails(remapper(classes, record("mainClass=com.acme.swingapp.Mian", "kind=swing"), "com.acme.MyApp"),
                "mainClass=com.acme.swingapp.Mian", "not among the application's compiled classes",
                "com.acme.swingapp.Main (swing)");
    }

    @Test
    public void aSwingMainClassWithoutMainFailsTheBuild() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/swingapp/Panel.java",
                "package com.acme.swingapp;\npublic class Panel extends javax.swing.JPanel {\n"
                        + "    public static void main(String args) { }\n}\n");
        assertFails(remapper(classes, record("mainClass=com.acme.swingapp.Panel", "kind=swing"), "com.acme.MyApp"),
                "com.acme.swingapp.Panel does not declare public static void main(String[])",
                "com.acme.swingapp.Main (swing)");
    }

    @Test
    public void aJavaFxMainClassMustBeAnInstantiableApplication() throws Exception {
        File classes = compile("com/acme/fxapp/Shop.java", FX_APP, "com/acme/fxapp/Launcher.java", FX_LAUNCHER,
                "com/acme/fxapp/Base.java", "package com.acme.fxapp;\n"
                        + "public class Base extends javafx.application.Application {\n"
                        + "    public Base(String name) { }\n"
                        + "    public void start(javafx.stage.Stage stage) { }\n}\n");
        assertFails(remapper(classes, record("mainClass=com.acme.fxapp.Launcher", "kind=javafx"), "com.acme.MyApp"),
                "com.acme.fxapp.Launcher does not extend javafx.application.Application",
                "com.acme.fxapp.Shop (javafx)");
        assertFails(remapper(classes, record("mainClass=com.acme.fxapp.Base", "kind=javafx"), "com.acme.MyApp"),
                "com.acme.fxapp.Base has no public constructor without parameters");
        assertFails(remapper(classes, record("mainClass=com.acme.fxapp.Shop", "kind=gtk"), "com.acme.MyApp"),
                "kind=gtk");
    }

    @Test
    public void aMainClassThatIsNotPublicIsCalledFromItsOwnPackage() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN.replace("public class Main", "class Main"));
        assertTrue(remapper(classes, record("mainClass=com.acme.swingapp.Main"), "com.acme.MyApp").run());
        Class<?> main = load(classes, "com.acme.MyApp");
        call(main.getConstructor().newInstance(), "runMain");
        java.lang.reflect.Field ran = main.getClassLoader().loadClass("com.acme.swingapp.Main").getDeclaredField("ran");
        ran.setAccessible(true);
        assertEquals(0, ran.getInt(null));
        // The bridge is not a candidate of a later run without a record.
        assertTrue(remapper(classes, null, "com.acme.MyApp").run());
        assertCompliant(classes);
    }

    @Test
    public void aSecondRunChangesNothingAndAChangedRecordRegenerates() throws Exception {
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/swingapp/Other.java",
                SWING_MAIN.replace("class Main", "class Other"));
        File record = record("mainClass=com.acme.swingapp.Main");
        assertTrue(remapper(classes, record, "com.acme.MyApp").run());
        File generated = new File(classes, "com/acme/MyApp.class");
        byte[] first = Files.readAllBytes(generated.toPath());
        long written = generated.lastModified();
        assertTrue(remapper(classes, record, "com.acme.MyApp").run());
        assertEquals(written, generated.lastModified());
        assertTrue(Arrays.equals(first, Files.readAllBytes(generated.toPath())));

        Files.write(record.toPath(), "mainClass=com.acme.swingapp.Other\n".getBytes("UTF-8"));
        assertTrue(remapper(classes, record, "com.acme.MyApp").run());
        assertTrue(CompatFixtures.members(Files.readAllBytes(generated.toPath()))
                .contains("com/acme/swingapp/Other.main"));
    }

    @Test
    public void aMainClassOfTheApplicationsOwnIsNeverReplaced() throws Exception {
        // A lifecycle the developer wrote: kept, record or not.
        String own = "package com.acme;\n"
                + "public class MyApp extends com.codename1.desktopcompat.rt.DesktopLifecycle {\n"
                + "    protected void runMain() { com.acme.swingapp.Main.main(new String[] {\"own\"}); }\n}\n";
        File classes = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/MyApp.java", own);
        assertTrue(remapper(classes, record("mainClass=com.acme.swingapp.Main"), "com.acme.MyApp").run());
        Class<?> main = load(classes, "com.acme.MyApp");
        call(main.getConstructor().newInstance(), "runMain");
        assertEquals(1, main.getClassLoader().loadClass("com.acme.swingapp.Main").getField("ran").getInt(null));

        // An ordinary Codename One main class: kept without a record, and a
        // build error with one, since only one of the two can start the app.
        String plain = "package com.acme;\npublic class MyApp extends com.codename1.system.Lifecycle {\n"
                + "    public void runApp() { }\n}\n";
        File other = compile("com/acme/swingapp/Main.java", SWING_MAIN, "com/acme/MyApp.java", plain);
        assertTrue(remapper(other, null, "com.acme.MyApp").run());
        assertEquals("com.codename1.system.Lifecycle", load(other, "com.acme.MyApp").getSuperclass().getName());
        assertFails(remapper(other, record("mainClass=com.acme.swingapp.Main"), "com.acme.MyApp"),
                "com.acme.MyApp is this project's main class", "Delete the source of com.acme.MyApp");
    }

    @Test
    public void theRecordIsReadAsTheImporterWritesIt() throws Exception {
        assertNotNull(DesktopEntryPoints.read(record("# comment", "mainClass = a.B ", "kind=swing")));
        assertEquals("a.B", DesktopEntryPoints.read(record("mainClass = a.B ")).getProperty("mainClass").trim());
        assertEquals(null, DesktopEntryPoints.read(new File(tmp.getRoot(), "none")));
    }
}
