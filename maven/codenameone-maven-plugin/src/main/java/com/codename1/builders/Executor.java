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
package com.codename1.builders;



import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

/**
 * This interface represents a build for a specific platform using the daemon,
 * this interface should be implemented to build to every platform type
 *
 * @author Shai Almog
 */
public abstract class Executor {
    public static final String BUILD_TARGET_XCODE_PROJECT = "ios-source";
    public static final String BUILD_TARGET_ANDROID_PROJECT = "android-source";
    public static final String BUILD_TARGET_MAC_NATIVE_PROJECT = "mac-source";
    // Native Windows (ParparVM -> clang-cl) target; distinct from the JVM-bundled
    // "windows-desktop" (javase) target.
    public static final String BUILD_TARGET_WINDOWS_NATIVE = "windows-device";
    public static final String BUILD_TARGET_WINDOWS_NATIVE_PROJECT = "windows-source";
    // Native Linux (ParparVM -> CMake/Ninja, GTK3/Cairo) target; distinct from a
    // JVM/JavaSE executable jar. The cloud target submits to the "linux" queue; the
    // local "local-linux-device" target builds on the developer's own machine.
    public static final String BUILD_TARGET_LINUX_NATIVE = "linux-device";
    // Native Mac (AppKit). The cloud target name; "mac-source" is the local
    // Xcode project. Both names are inherited from the Mac Catalyst target that
    // preceded this port, so an existing project keeps building without an edit
    // and simply gets the AppKit app instead.
    public static final String BUILD_TARGET_MAC_NATIVE = "mac-os-x-native";
    // The local counterpart, mirroring local-linux-device: builds the AppKit app
    // on the developer's own Mac rather than submitting it. Separate from the
    // cloud name for the same reason Windows and Linux keep the two apart --
    // "which machine compiled this" is not something a target string should
    // leave ambiguous.
    public static final String BUILD_TARGET_MAC_NATIVE_LOCAL = "local-mac-device";
    // Mac Catalyst has no target of its own, deliberately. It IS an iPhone
    // build: IPhoneBuilder switches to the Catalyst slice on the
    // macNative.enabled hint alone and has always done so, so an iOS target
    // plus that hint is the whole of it. Giving it a target name would add a
    // second spelling for something the hint already says, in the maven
    // targeting, the ant template and both builders.
    private String buildTarget;

    private static boolean disableDelete;
    public static final boolean is_windows = File.separatorChar == '\\';
    protected File tmpDir;
    StringBuilder message = new StringBuilder();
    private boolean canceled;
    private Class<?>[] nativeInterfaces;
    private boolean unitTestMode;
    static boolean IS_MAC;
    /**
     * The internal class name inside a JVM field descriptor.
     *
     * <p>{@code Lcom/foo/Bar;} carries one character of punctuation at
     * each end. Cutting two off the tail dropped the last letter of every
     * class name reported from a field or a local variable, so any check
     * comparing a whole name silently missed -- which is how a BLE-only
     * app declaring a {@code HealthSample} field was still classified as
     * using the health store.</p>
     */
    public static String descriptorToInternalName(String descriptor) {
        if (descriptor == null || descriptor.length() < 3
                || descriptor.charAt(0) != 'L') {
            return descriptor;
        }
        int end = descriptor.charAt(descriptor.length() - 1) == ';'
                ? descriptor.length() - 1 : descriptor.length();
        return descriptor.substring(1, end);
    }

    protected final Map<String,String> defaultEnvironment = new HashMap<String,String>();

    private Properties localBuilderProperties;


    protected File codenameOneJar;

    public void setCodenameOneJar(File codenameOneJar) {
        this.codenameOneJar = codenameOneJar;
    }
    public File getCodenameOneJar() {
        return codenameOneJar;
    }

    /**
     * The scratch directory where all the temporary build files are created.
     */
    private File buildDirectory;

    public void setBuildDirectory(File buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    public File getBuildDirectory() {
        return this.buildDirectory;
    }

    static {
        IS_MAC = System.getProperty("os.name").toLowerCase().indexOf("mac") > -1;
    }

    public void setId(String buildId) {
    }

    public void cleanup() {
        if(!disableDelete) {
            if (tmpDir != null) {
                delTree(tmpDir);
            }
        }

    }


    public static File createTempFile(String prefix, String suffix) throws IOException {

        return File.createTempFile(prefix, suffix);
    }


    public void setBuildTarget(String target) {
        this.buildTarget = target;
    }

    public String getBuildTarget() {
        return this.buildTarget;
    }


    public void replaceInFile(File sourceFile, String marker, String newValue) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        try(Writer fios = new OutputStreamWriter(Files.newOutputStream(sourceFile.toPath()), StandardCharsets.UTF_8)) {
            String str = new String(data, StandardCharsets.UTF_8);
            str = str.replace(marker, newValue);
            fios.write(str);
        }
    }

    public String readFileToString(File sourceFile) throws IOException {
        DataInputStream dis = new DataInputStream(Files.newInputStream(sourceFile.toPath()));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        return new String(data, StandardCharsets.UTF_8);
    }

    public boolean findInFile(File sourceFile, String marker) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        String str = new String(data, StandardCharsets.UTF_8);
        return str.contains(marker);
    }

    public void replaceAllInFile(File sourceFile, String marker, String newValue) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        try(Writer fios = new OutputStreamWriter(Files.newOutputStream(sourceFile.toPath()), StandardCharsets.UTF_8)) {
            String str = new String(data, StandardCharsets.UTF_8);
            str = str.replaceAll(marker, newValue);
            fios.write(str);
        }
    }

    File includeSources(BuildRequest request) throws Exception {
        return null;
    }



    protected File retrolambdaDontRename(File userDir, BuildRequest request, File classDir) throws Exception {
        return retrolambda(userDir, request, classDir, false);
    }

    protected String defaultJavaVersion() {
        return "8";
    }

    protected boolean retrolambda(File userDir, BuildRequest request, File classDir) throws Exception {
        return retrolambda(userDir, request, classDir, true) != null;
    }

    private File retrolambda(File userDir, BuildRequest request, File classDir, boolean rename) throws Exception {


        File output = new File(classDir.getParentFile(), classDir.getName()+"_retrolamda");
        output.mkdir();


        HashMap<String, String> env = new HashMap<String, String>();


        String retrolambda = System.getProperty("retrolambdaJarPath", null);
        if (retrolambda == null) {
            getResourceAsFile("/com/codename1/builder/retrolambda.jar", ".jar").getAbsolutePath();
        }


        if (codenameOneJar == null) {
            throw new IllegalStateException("CodenameOne jar is not set");
        }
        if (!codenameOneJar.exists()) {
            throw new IOException("Cannot find codename one jar at "+ codenameOneJar);
        }
        String codenameOneJarPath = codenameOneJar.getAbsolutePath();
        File java8Home = new File(System.getProperty("java.home"));
        String java = new File(java8Home, "bin" + File.separator + "java").getAbsolutePath();
        String defaultMethods = "-Dretrolambda.defaultMethods=true";;


        if (!exec(userDir, env, java,
                "-Dretrolambda.inputDir="+classDir.getAbsolutePath(),
                //"-Dretrolambda.classpath="+classDir.getAbsolutePath()+":src/iOSPort.jar:JavaAPI.jar",
                "-Dretrolambda.classpath="+classDir.getAbsolutePath()+File.pathSeparator+codenameOneJarPath,
                "-Dretrolambda.outputDir="+output.getAbsolutePath(),
                "-Dretrolambda.bytecodeVersion=49", defaultMethods,
                "-jar", retrolambda

        )
        ) {
            return null;
        }
        // Remove stale references to java/lang/invoke classes.
        stripInvokeClassConstantsRecursive(output);
        if(rename) {
            delTree(classDir, true);
            if(is_windows) {
                Files.move(output.toPath(), classDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                output.renameTo(classDir);
            }
            remapClasses(classDir, getDefaultClassMapping());
        } else {
            remapClasses(output, getDefaultClassMapping());
        }

        return output;
    }


    /**
     * Retrolambda seems to leave class constants for java/lang/invoke classes
     * in the constant pool even though they aren't used.  Strips these
     * constants out.
     * @param dir Directory containing classes to be converted.  Recursively.
     * @throws IOException
     */
    private void stripInvokeClassConstantsRecursive(File dir) throws IOException {
        if (dir.isFile() && dir.getName().endsWith(".class")) {
            stripInvokeClassConstants(dir);
        } else if (dir.isDirectory()){
            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(".")) {
                    stripInvokeClassConstantsRecursive(f);
                }
            }
        }
    }

    /**
     * Retrolambda seems to leave class constants for java/lang/invoke classes
     * in the constant pool even though they aren't used.  This will strip
     * them out.
     * @param classFile
     * @throws IOException
     */
    private void stripInvokeClassConstants(File classFile) throws IOException {
        FileInputStream fis = null;
        try {
            final boolean[] found = new boolean[1];
            fis = new FileInputStream(classFile);
            ClassReader r = new ClassReader(fis) {

            };
            ClassVisitor v = new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                }
                @Override
                public void visitInnerClass(String name, String outerName, String innerName, int access) {//(String string, String string1, String string2, int i) {
                    if (!name.startsWith("java/lang/invoke")) {
                        super.visitInnerClass(name, outerName, innerName, access);
                    } else {
                        found[0] = true;
                    }
                }
            };

            ClassWriter w = new ClassWriter(r, ClassWriter.COMPUTE_MAXS);
            r.accept(v, 0);

            if (!found[0]) {
                // If nothing was stripped, we don't need to write the file.
                return;
            }
            File out = //new File(classFile.getParentFile(), classFile.getName()+".stripped");
                    classFile;
            createFile(out, w.toByteArray());

        } finally {
            if (fis != null) {
                try { fis.close();} catch(Throwable t){}
            }
        }
    }

    protected String createStartInvocation(BuildRequest request, String mainObject) {
        return createStartInvocation(request, mainObject, true);
    }

    protected String createStartInvocation(BuildRequest request, String mainObject, boolean includeVserv) {
        String zone = request.getArg("vserv.zone", null);
        if (includeVserv && zone != null && zone.length() > 0) {
            String transition = request.getArg("vserv.transition", "300000");
            String countryCode = request.getArg("vserv.countryCode", "null");
            String networkCode = request.getArg("vserv.networkCode", "null");
            String locale = request.getArg("vserv.locale", "en_US");
            String category = request.getArg("vserv.category", "29");
            try {
                URL u = new URL("http://admin.vserv.mobi/partner/zone-add.php?partnerid=1&zoneid=" + zone);
                InputStream i = u.openStream();
                i.read();
                i.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String scaleMode = request.getArg("vserv.scaleMode", "false");
            String allowSkipping = request.getArg("vserv.allowSkipping", "true");
            return "        com.codename1.impl.VServAds v = new com.codename1.impl.VServAds();\n"
                    + "        v.setCountryCode(\"" + countryCode + "\");\n"
                    + "        v.setNetworkCode(\"" + networkCode + "\");\n"
                    + "        v.setLocale(\"" + locale + "\");\n"
                    + "        v.setZoneId(\"" + zone + "\");\n"
                    + "        v.setCategory(" + category + ");\n"
                    + "        v.setScaleMode(" + scaleMode + ");\n"
                    + "        v.setAllowSkipping(" + allowSkipping + ");\n"
                    + "        v.showWelcomeAd();\n"
                    + "        v.bindTransitionAd(" + transition + ");\n"
                    + "        " + mainObject + ".start();\n";


        }
        return mainObject + ".start();\n";
    }


    private Log logger;

    public void setLogger(Log log) {
        this.logger = log;
    }

    public static interface ClassScanner {

        public void usesClass(String cls);

        public void usesClassMethod(String cls, String method);

        /**
         * Reports that {@code cls} declares {@code iface} among its
         * implemented interfaces.
         *
         * <p>{@link #usesClass(String)} is also called for the interface,
         * but it only says that the interface was referenced somewhere --
         * it drops which class did the implementing. Builders that need to
         * generate a binding to an app-supplied callback need the
         * implementor, so that a direct constructor call can be emitted
         * instead of resolving a name reflectively at runtime.</p>
         */
        public void implementsInterface(String cls, String iface);

        /**
         * Reports that {@code cls} is a member of {@code outer}, as the
         * class file's own {@code InnerClasses} attribute states it.
         *
         * <p>Nesting cannot be inferred from the binary name: a dollar is
         * a legal Java identifier character, so a top-level
         * {@code app.Step$Listener} is a class somebody may really have
         * written and a generated {@code new app.Step.Listener()} would
         * not compile. Only the attribute knows which dollars separate
         * anything.</p>
         */
        public default void declaresEnclosedBy(String cls, String outer) {
        }

        /**
         * Reports a type, its superclass, and whether the generated
         * bindings could build it with {@code new X()} -- that is, it is
         * a public, non-abstract, non-interface class with a public
         * no-argument constructor.
         *
         * <p>Needed because the class that <em>declares</em> a listener
         * interface is often not the one to bind: an abstract base cannot
         * be constructed, and the concrete subclass never names the
         * interface itself.</p>
         */
        public default void declaresType(String cls, String superName,
                boolean isConstructible) {
        }

        /** Reports that {@code cls} is public. */
        public default void declaresPublicType(String cls) {
        }
        /**
         * Reports that {@code cls} is a concrete class -- neither
         * abstract nor an interface -- whatever its constructors look
         * like.
         *
         * <p>{@link #declaresType(String, String, boolean)} answers
         * whether the generated bindings can build it, which folds two
         * different situations into one false: an abstract base, which is
         * meant to have a concrete subclass do the work, and a concrete
         * class with no public no-argument constructor, which nothing can
         * restore even though an app can register it. The second is a
         * silent failure and the first is not, so the two have to be
         * distinguishable.</p>
         */
        public default void declaresConcreteType(String cls) {
        }

        /**
         * Reports a call whose last argument was pushed as a literal
         * {@code boolean}, or null when it was not.
         *
         * <p>{@link #usesClassMethod(String, String)} fires for the same
         * call and says nothing about the arguments, which is enough for
         * "this class is used" but not for a setter that switches a
         * feature on or off: {@code setWriteToStore(false)} is the app
         * saying it wants no store at all, and treating it like
         * {@code true} dragged a Bluetooth-only build into Health
         * Connect, a privacy policy and a Play health review.</p>
         *
         * <p>{@code value} is null unless the constant is the instruction
         * immediately before the call, so anything computed, loaded from
         * a variable or reached through a branch reads as unknown. The
         * caller must treat unknown as the feature being on: the cost of
         * over-declaring is a permission the app does not need, and the
         * cost of under-declaring is a SecurityException on a user's
         * device.</p>
         */
        public default void usesClassMethodWithBooleanArgument(String cls,
                String method, Boolean value) {
        }


        /**
         * Reports a call together with the descriptor of the method it
         * resolves to.
         *
         * <p>{@link #usesClassMethod(String, String)} reports the name
         * alone, which cannot separate overloads. That is not a detail
         * where the overloads do different things:
         * {@code MediaManager.createMedia(String,boolean)} plays a URI
         * that may point at the MediaStore, while
         * {@code createMedia(InputStream,String)} copies the stream into
         * app-private storage and reads no shared media at all. Keyed on
         * the name, an app doing the second was built asking for
         * {@code READ_MEDIA_VIDEO}, which costs its author a Play Console
         * Photo and Video Permissions declaration for a permission the
         * app never uses.</p>
         *
         * <p>{@code descriptor} is the call site's descriptor, so it is
         * what the compiler resolved rather than what runs -- close
         * enough for overload selection, which is all it is for. It is
         * null when the scan could not recover one; a caller must treat
         * null as "may be the overload that needs the permission",
         * because under-declaring costs a SecurityException on a user's
         * device.</p>
         */
        public default void usesClassMethodWithDescriptor(String cls,
                String method, String descriptor) {
        }
    }

    public static interface InternalClassRemapper {
        public String remapClass(String cls);

    }

    protected Map<String,String> getDefaultClassMapping() {
        Map<String,String> out = new HashMap<String,String>();
        out.put("java/util/Objects", "com/codename1/compat/java/util/Objects");
        return out;
    }

    protected void remapClasses(File directory, Map<String,String> mapping) throws IOException {
        remapClasses(directory, new SimpleRemapper(mapping));
    }

    private void remapClasses(File directory, final SimpleRemapper remapper) throws IOException {
        File[] list = directory.listFiles();
        for (File current : list) {
            if (current.isDirectory()) {
                remapClasses(current, remapper);
            } else {
                if (current.getName().endsWith(".class")) {
                    InputStream is = new FileInputStream(current);
                    ClassReader r = null;
                    ClassWriter cw = new ClassWriter(0);
                    ClassRemapper remappingClassAdapter = new ClassRemapper(cw, remapper);
                    try {
                        r = new ClassReader(is);
                    } catch(RuntimeException re) {
                        message.append(getCustomStackTrace(re));
                        message.append("Error encountered while parsing the class ");
                        message.append(current.getName());
                        throw re;
                    }
                    is.close();

                    try {
                        r.accept(remappingClassAdapter, ClassReader.EXPAND_FRAMES);
                        is = new ByteArrayInputStream(cw.toByteArray());
                        FileOutputStream fos = new FileOutputStream(current);
                        copy(is, fos);
                    } catch(RuntimeException re) {
                        message.append(getCustomStackTrace(re));
                        message.append("Error encountered while parsing the class ");
                        message.append(current.getName());
                        throw re;
                    }
                }
            }
        }
    }

    /// Reports whether this build came from a Maven project.
    ///
    /// The Maven plugin stamps its own version into the settings it hands over, and the legacy
    /// Ant build client never has, so the presence of that argument is the discriminator. It has
    /// been written since 2021, well before the Maven transition finished, so an ordinary Maven
    /// build is not going to be missing it.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build request
    ///
    /// #### Returns
    ///
    /// true if the request was produced by the Maven plugin
    protected boolean isMavenBuild(BuildRequest request) {
        return request.getArg("maven.codenameone-maven-plugin", null) != null
                || request.getArg("maven.codenameone-core.version", null) != null;
    }

    /// Decides whether the database compatibility switch should be on for this build.
    ///
    /// An explicit `db.legacy` always wins, in either direction. Failing that, an Ant project is
    /// defaulted to the old behaviour, because it predates the portable contract and its author
    /// has no reason to expect a rebuild to change how their queries behave. A Maven project
    /// gets the portable contract, which is the documented default.
    ///
    /// The default only applies when the application actually uses the database. Turning it on
    /// for an app with no database is harmless but misleading, and it would show up in the build
    /// log of every Ant project ever built.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build request
    /// - `usesDatabase`: whether the scanned classes reference `com.codename1.db`
    ///
    /// #### Returns
    ///
    /// true if the generated stub should switch the database into legacy mode
    protected boolean isDatabaseLegacyMode(BuildRequest request, boolean usesDatabase) {
        String explicit = request.getArg("db.legacy", null);
        if (explicit != null) {
            return "true".equalsIgnoreCase(explicit);
        }
        if (!usesDatabase || isMavenBuild(request)) {
            return false;
        }
        debug("This is an Ant project that uses com.codename1.db, so the database is being built "
                + "in compatibility mode: it keeps the behaviour this project was written "
                + "against rather than the portable contract. Set the db.legacy build hint to "
                + "false to opt in to the portable contract, or to true to pin compatibility "
                + "mode explicitly.");
        return true;
    }

    /// The line a generated application stub needs in order to apply the decision above.
    ///
    /// Empty when the switch is off, so the stub is unchanged for the common case.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build request
    /// - `usesDatabase`: whether the scanned classes reference `com.codename1.db`
    ///
    /// #### Returns
    ///
    /// the source line to insert, or an empty string
    protected String databaseLegacyStubProperty(BuildRequest request, boolean usesDatabase) {
        if (!isDatabaseLegacyMode(request, usesDatabase)) {
            return "";
        }
        return "        Display.getInstance().setProperty(\"db.legacy\", \"true\");\n";
    }

    /// The same decision, for a stub that runs before `Display` exists.
    ///
    /// A **direct** call, not reflection: ParparVM's dead-code elimination does not keep a member
    /// reached only reflectively, so a reflective form would be culled and the lookup would fail
    /// at runtime, silently leaving the application on the new behaviour. The launcher calls
    /// `SVGRegistry.installGlobal` and `NativeLookup.register` directly for the same reason.
    ///
    /// It compiles everywhere because it is emitted only when the staged core actually has the
    /// switch. A core that predates it has none to set, and its behaviour is already the old
    /// behaviour, so emitting nothing there reaches the same result.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build request
    /// - `usesDatabase`: whether the scanned classes reference `com.codename1.db`
    ///
    /// #### Returns
    ///
    /// the source line to insert, or an empty string
    protected String databaseLegacyStubCall(BuildRequest request, boolean usesDatabase,
            boolean coreHasLegacySwitch) {
        if (!coreHasLegacySwitch || !isDatabaseLegacyMode(request, usesDatabase)) {
            return "";
        }
        return "        com.codename1.db.Database.setLegacyBehavior(true);\n";
    }

    /// Whether the staged framework carries the database compatibility switch.
    ///
    /// `DatabaseConfig` arrived with the portable database contract, so its presence is what
    /// distinguishes a core that has `setLegacyBehavior` from one that predates the feature.
    ///
    /// #### Parameters
    ///
    /// - `stageClasses`: the staged class tree, or null
    ///
    /// #### Returns
    ///
    /// true if the switch is available to call directly
    protected boolean coreHasLegacySwitch(File stageClasses) {
        return stageClasses != null
                && new File(stageClasses, "com/codename1/db/DatabaseConfig.class").exists();
    }

    /// What an application's own classes say about its use of the database API.
    ///
    /// Two independent answers, because two different payloads hang off them: any reference to
    /// `com.codename1.db` means the SQLite engine has to ship, and a reference to `DatabaseConfig`
    /// additionally means the cipher does.
    public static final class DatabaseUsage {

        private final boolean database;

        private final boolean cipher;

        DatabaseUsage(boolean database, boolean cipher) {
            this.database = database;
            this.cipher = cipher;
        }

        /// The answer from two roots, since a build can stage classes and libraries separately.
        ///
        /// #### Parameters
        ///
        /// - `other`: the usage found under another root
        ///
        /// #### Returns
        ///
        /// a usage that is true wherever either is
        public DatabaseUsage merge(DatabaseUsage other) {
            if (other == null) {
                return this;
            }
            return new DatabaseUsage(database || other.database, cipher || other.cipher);
        }

        /// Whether anything outside the framework references `com.codename1.db`.
        public boolean usesDatabase() {
            return database;
        }

        /// Whether anything outside the framework references `com.codename1.db.DatabaseConfig`.
        public boolean usesDatabaseCipher() {
            return cipher;
        }
    }

    /// The database API itself. Every class in it names the package, so none of them says
    /// anything about whether the application does.
    private static final String DATABASE_PACKAGE = "com/codename1/db";

    /// Framework classes whose reference to the database package is their own.
    ///
    /// Named one by one rather than by package, because a package is not a reliable statement
    /// about who wrote a class: skipping `com/codename1/ui` wholesale would also skip an
    /// application class under it, and a direct `DatabaseConfig` reference there would then go
    /// unseen and the engine would be left out of a build that needs it. There are sixteen of
    /// these in the framework and they are enumerated.
    private static final String[] FRAMEWORK_DATABASE_CLASSES = {
        "com/codename1/impl/AbstractDBCursor",
        "com/codename1/impl/CodenameOneImplementation",
        "com/codename1/orm/Dao",
        "com/codename1/orm/EntityManager",
        "com/codename1/properties/SQLMap",
        // Not framework code any more -- it lives under tests/ and is compiled only into the
        // conformance harness -- but the harness does carry it, and it is our check on the ports
        // rather than that application's own use of the database. Named here so it keeps counting
        // for nothing, and so an application that shipped against an older core, which did carry
        // it, is unaffected.
        "com/codename1/testing/DatabaseConformanceSuite",
        "com/codename1/ui/Display",
        // The database package itself. Listed by name rather than skipped as a directory: the
        // package is the framework's by convention, not by ownership, and an application or a
        // library is free to put a class in it -- which used to make that class invisible to this
        // scan, so a helper there could configure encryption and the build would drop the cipher.
        "com/codename1/db/Cursor",
        "com/codename1/db/CursorExt",
        "com/codename1/db/Database",
        "com/codename1/db/DatabaseConfig",
        "com/codename1/db/DatabaseEncryptionException",
        "com/codename1/db/ManagedKeys",
        "com/codename1/db/Row",
        "com/codename1/db/RowExt",
        "com/codename1/db/ThreadSafeDatabase",
        "com/codename1/db/package-info"
    };

    /// The class that turns encryption on.
    private static final String DATABASE_CONFIG_CLASS = "com/codename1/db/DatabaseConfig";

    /// Framework classes that are a database by another name.
    ///
    /// An application can use one of these and never mention `com.codename1.db` itself --
    /// `EntityManager` and `SQLMap` exist so that it does not have to -- so a reference to one is
    /// a reference to the database. Without this the engine would be left out of exactly the
    /// applications that took the framework's advice, and they would fail at runtime rather than
    /// at build time.
    private static final String[] DATABASE_FACADE_PREFIXES = {
        "com/codename1/orm/",
        "com/codename1/properties/SQLMap"
    };

    /// Reports whether the application, as opposed to the framework, uses the database.
    ///
    /// Reads each class file directly rather than going through
    /// `#scanClassesForPermissions(File,ClassScanner)`, which reports the class being scanned only
    /// from `visitEnd` -- after its references have already been delivered -- so a reference cannot
    /// be attributed to the class that made it. Walking one file at a time, and skipping the
    /// framework by path, is what makes the distinction possible.
    ///
    /// The test is a constant-pool search for the package name, which is how every reference to a
    /// class in it is stored, including the descriptor of a framework method that merely returns
    /// one. A class mentioning the string for some other reason counts too, which errs towards
    /// treating the application as a database user -- the safe direction, since the cost is bytes
    /// rather than a missing engine.
    ///
    /// #### Parameters
    ///
    /// - `classesDir`: the staged class tree, application and framework together
    ///
    /// #### Returns
    ///
    /// what the application's own classes reference, never null
    protected DatabaseUsage scanForDatabaseUsage(File classesDir) throws IOException {
        boolean[] found = {false, false};
        if (classesDir != null && classesDir.isDirectory()) {
            scanForDatabaseUsage(classesDir, "", found);
        }
        return new DatabaseUsage(found[0], found[1]);
    }

    private void scanForDatabaseUsage(File dir, String relativePath, boolean[] found)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (int iter = 0; iter < children.length; iter++) {
            if (found[0] && found[1]) {
                return;
            }
            File child = children[iter];
            String childPath = relativePath.length() == 0
                    ? child.getName() : relativePath + "/" + child.getName();
            if (child.isDirectory()) {
                scanForDatabaseUsage(child, childPath, found);
            } else if (child.getName().endsWith(".aar")) {
                // An Android archive carries its bytecode in a nested classes.jar, and the
                // generated gradle links it like any other dependency, so encryption configured
                // inside one has to count exactly as a plain jar's does.
                scanArchiveForDatabaseUsage(child, found);
            } else if (child.getName().endsWith(".jar")) {
                // A library can be the only thing that touches the database: the application calls
                // the library, and Android stages the jar into libs and links it through the
                // generated fileTree. Reading loose class files alone reported no database use and
                // dropped the engine out from under code that runs it.
                scanArchiveForDatabaseUsage(child, found);
            } else if (child.getName().endsWith(".class")
                    && !isFrameworkDatabaseClass(childPath)) {
                inspectClassForDatabaseUsage(readAllBytes(child), found);
            }
        }
    }

    /// Reads the class entries of a library archive, which carry the same weight as loose ones.
    private void scanArchiveForDatabaseUsage(File archive, boolean[] found) {
        java.util.zip.ZipFile zip = null;
        try {
            zip = new java.util.zip.ZipFile(archive);
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements() && !(found[0] && found[1])) {
                java.util.zip.ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.endsWith(".jar")) {
                    // An Android archive keeps its bytecode in a nested classes.jar, so the
                    // entries that matter are one level further in.
                    //
                    // Caught per entry, because an entry that cannot be read says nothing about
                    // the entries after it. A name ending in .jar need not be an archive at all --
                    // a truncated or opaque resource carries that name perfectly well -- and
                    // letting it out of here abandoned the rest of a library that was otherwise
                    // fine. The classes it dropped are the ones this scan exists to find, and the
                    // consequence is silent: the cipher implementation is pruned from an
                    // application that turns out to need it, and encryption fails on the device.
                    try {
                        java.io.InputStream nested = zip.getInputStream(entry);
                        try {
                            scanNestedArchiveForDatabaseUsage(nested, found);
                        } finally {
                            nested.close();
                        }
                    } catch (IOException cannotReadEntry) {
                        log("WARNING: could not read " + name + " inside " + archive
                                + " while looking for database use; the rest of the archive was "
                                + "still read");
                    }
                    continue;
                }
                if (!name.endsWith(".class") || isFrameworkDatabaseClass(name)) {
                    continue;
                }
                // Per entry for the same reason: one unreadable class is not a reason to stop
                // reading the ones after it.
                try {
                    java.io.InputStream in = zip.getInputStream(entry);
                    try {
                        inspectClassForDatabaseUsage(readAllBytes(in), found);
                    } finally {
                        in.close();
                    }
                } catch (IOException cannotReadEntry) {
                    log("WARNING: could not read " + name + " inside " + archive
                            + " while looking for database use; the rest of the archive was still "
                            + "read");
                }
            }
        } catch (IOException cannotRead) {
            // An archive that cannot be opened says nothing either way, and refusing to build over
            // it would fail every application carrying a jar this cannot parse.
            log("WARNING: could not read " + archive + " while looking for database use");
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                    // Nothing left to do with it.
                }
            }
        }
    }

    /// Reads the class entries of an archive inside an archive, which is where an AAR keeps them.
    private void scanNestedArchiveForDatabaseUsage(java.io.InputStream nested, boolean[] found)
            throws IOException {
        java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(nested);
        java.util.zip.ZipEntry entry = in.getNextEntry();
        while (entry != null && !(found[0] && found[1])) {
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class")
                    && !isFrameworkDatabaseClass(name)) {
                inspectClassForDatabaseUsage(readAllBytes(in), found);
            }
            entry = in.getNextEntry();
        }
    }

    /// Applies both questions to one class file, through ASM.
    ///
    /// Read as bytecode rather than searched as bytes. Every name a class refers to sits in its
    /// constant pool, so a substring search cannot tell a *reference* to com.codename1.db from a
    /// string literal that happens to contain those characters -- and it cannot tell
    /// DatabaseConfig.plain(), which says a database is not encrypted, from
    /// DatabaseConfig.passphrase(), because the class name is identical in both. Asking the
    /// bytecode asks the question that was meant.
    ///
    /// The same reader and visitor style scanClassesForPermissions already uses, on the same
    /// files, in the same builds -- which is where this should have started rather than in a
    /// hand written constant pool walk.
    ///
    /// A class this cannot read counts as using both: the alternative is an application that
    /// encrypts shipping without a cipher.
    ///
    /// #### Parameters
    ///
    /// - `bytes`: one class file
    /// - `found`: the two answers so far, updated in place
    private void inspectClassForDatabaseUsage(byte[] bytes, boolean[] found) {
        if (found[0] && found[1]) {
            return;
        }
        boolean[] hit = {false, false};
        try {
            new ClassReader(bytes).accept(new DatabaseUsageVisitor(hit),
                    ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (RuntimeException cannotRead) {
            // Truncated or obfuscated past recognition, and not a reason to decide the application
            // does not encrypt. This stays a rare case only while the ASM here keeps up with the
            // bytecode the toolchain emits: read no class file at all and every application would
            // be charged the cipher, and on Android an API 23 floor. The daemon, pinned to an ASM
            // that stops at Java 8, reads the constant pool directly instead for that reason.
            hit[0] = true;
            hit[1] = true;
        }
        found[0] = found[0] || hit[0];
        found[1] = found[1] || hit[1];
    }

    /// Answers "does this class use the database" and "does it configure encryption".
    private static final class DatabaseUsageVisitor extends ClassVisitor {

        private final boolean[] hit;

        DatabaseUsageVisitor(boolean[] hit) {
            super(Opcodes.ASM9);
            this.hit = hit;
        }

        /// Any mention of a database type, in a name, a descriptor or a signature.
        private void note(String name) {
            if (name == null) {
                return;
            }
            if (name.indexOf(DATABASE_PACKAGE + "/") >= 0) {
                hit[0] = true;
                return;
            }
            for (int iter = 0; iter < DATABASE_FACADE_PREFIXES.length; iter++) {
                // An application can use SQLMap or the ORM and never mention com.codename1.db
                // itself; those exist so that it does not have to.
                if (name.indexOf(DATABASE_FACADE_PREFIXES[iter]) >= 0) {
                    hit[0] = true;
                    return;
                }
            }
        }

        /// A constant that can name a type or a method: a class literal, a method handle, or --
        /// on a class file new enough to have them -- a dynamic constant whose own bootstrap
        /// arguments carry more of the same.
        private void noteConstant(Object value) {
            if (value instanceof Type) {
                // The descriptor rather than the internal name, because a Type here can describe a
                // method, and an internal name is not a question that can be asked of one.
                note(((Type) value).getDescriptor());
                return;
            }
            if (value instanceof Handle) {
                Handle handle = (Handle) value;
                note(handle.getOwner());
                note(handle.getDesc());
                if (DATABASE_CONFIG_CLASS.equals(handle.getOwner())
                        && isEncryptingFactory(handle.getName())) {
                    hit[1] = true;
                }
                return;
            }
            if (value instanceof ConstantDynamic) {
                ConstantDynamic constant = (ConstantDynamic) value;
                note(constant.getDescriptor());
                noteConstant(constant.getBootstrapMethod());
                for (int iter = 0; iter < constant.getBootstrapMethodArgumentCount(); iter++) {
                    noteConstant(constant.getBootstrapMethodArgument(iter));
                }
            }
        }

        private void noteAll(String[] names) {
            if (names != null) {
                for (int iter = 0; iter < names.length; iter++) {
                    note(names[iter]);
                }
            }
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                String superName, String[] interfaces) {
            note(superName);
            note(signature);
            noteAll(interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                String signature, Object value) {
            note(descriptor);
            note(signature);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            note(descriptor);
            note(signature);
            noteAll(exceptions);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName,
                        String methodDescriptor, boolean isInterface) {
                    note(owner);
                    note(methodDescriptor);
                    if (DATABASE_CONFIG_CLASS.equals(owner) && isEncryptingFactory(methodName)) {
                        // The call, not the class. plain() is the documented way to say a
                        // database is not encrypted, and reading it as encryption costs that
                        // application the cipher library and, on Android, every device below 23.
                        hit[1] = true;
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String fieldName,
                        String fieldDescriptor) {
                    note(owner);
                    note(fieldDescriptor);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    note(type);
                }

                @Override
                public void visitLdcInsn(Object value) {
                    noteConstant(value);
                }

                /// A lambda or a method reference, which is not a call instruction.
                ///
                /// `DatabaseConfig::passphrase` compiles to an invokedynamic whose bootstrap
                /// arguments carry the factory as a method handle, so nothing reaches
                /// visitMethodInsn above. Missing it left an application that encrypts being
                /// built without a cipher -- the one direction this scan must never get wrong.
                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor,
                        Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    note(descriptor);
                    noteConstant(bootstrapMethodHandle);
                    if (bootstrapMethodArguments != null) {
                        for (int iter = 0; iter < bootstrapMethodArguments.length; iter++) {
                            noteConstant(bootstrapMethodArguments[iter]);
                        }
                    }
                }
            };
        }
    }

    /// Whether a DatabaseConfig factory is one that configures a key.
    private static boolean isEncryptingFactory(String name) {
        for (int iter = 0; iter < ENCRYPTING_CONFIG_FACTORIES.length; iter++) {
            if (ENCRYPTING_CONFIG_FACTORIES[iter].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /// The DatabaseConfig factories that mean a database is encrypted.
    ///
    /// `plain()` is deliberately absent: it is the documented way to say a database is *not*
    /// encrypted, and treating a reference to it as encryption costs that application the cipher
    /// library and, on Android, every device below API 23.
    private static final String[] ENCRYPTING_CONFIG_FACTORIES = {
        "passphrase", "rawKey", "managed"
    };

    /// Whether this class file is one of the framework's own, by exact name.
    ///
    /// The `$` test catches a nested class, which belongs to the class that declares it --
    /// `SQLMap$SqlType$8` is `SQLMap`.
    private static boolean isFrameworkDatabaseClass(String path) {
        String name = path.substring(0, path.length() - ".class".length());
        for (int iter = 0; iter < FRAMEWORK_DATABASE_CLASSES.length; iter++) {
            String framework = FRAMEWORK_DATABASE_CLASSES[iter];
            if (name.equals(framework) || name.startsWith(framework + "$")) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readAllBytes(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        InputStream in = new FileInputStream(f);
        try {
            int read = 0;
            while (read < bytes.length) {
                int step = in.read(bytes, read, bytes.length - read);
                if (step < 0) {
                    break;
                }
                read += step;
            }
        } finally {
            in.close();
        }
        return bytes;
    }

    /// Reads a stream whose length is not known in advance, which is every archive entry.
    private static byte[] readAllBytes(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int step = in.read(buffer);
        while (step > 0) {
            out.write(buffer, 0, step);
            step = in.read(buffer);
        }
        return out.toByteArray();
    }
    protected void scanClassesForPermissions(File directory, final ClassScanner scanner) throws IOException {
        File[] list = directory.listFiles();
        for (final File current : list) {
            if (current.isDirectory()) {
                scanClassesForPermissions(current, scanner);
            } else {
                if (current.getName().endsWith(".class")) {
                    InputStream is = new FileInputStream(current);
                    ClassReader r = null;
                    try {
                        r = new ClassReader(is);
                    } catch(RuntimeException re) {
                        message.append(getCustomStackTrace(re));
                        message.append("Error encountered while parsing the class ");
                        message.append(current.getName());
                        throw re;
                    }
                    is.close();
                    ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9) {

                        private String scannedName;
                        private String scannedSuper;
                        private boolean scannedPublic;
                        private boolean scannedConcrete;
                        private boolean scannedNonAbstract;
                        private boolean scannedHasPublicNoArgCtor;

                        @Override
                        public void visit(int i, int accessFlags, String string, String string1, String superName, String[] interfaces) {
                            scannedName = string;
                            scannedSuper = superName;
                            // ACC_PUBLIC 0x0001, ACC_INTERFACE 0x0200,
                            // ACC_ABSTRACT 0x0400. A class the generated
                            // bindings construct from another package has
                            // to be public and buildable; the constructor
                            // is checked in visitMethod.
                            scannedPublic = (accessFlags & 0x0001) != 0;
                            // Two different questions. Concrete is
                            // about the class itself -- neither abstract
                            // nor an interface -- and stays true for a
                            // package-private one, which an app can
                            // still register and which the factory still
                            // cannot name. Constructible additionally
                            // requires the accessibility the generated
                            // binding needs.
                            scannedNonAbstract = (accessFlags & 0x0200) == 0
                                    && (accessFlags & 0x0400) == 0;
                            scannedConcrete = scannedNonAbstract
                                    && scannedPublic;
                            scannedHasPublicNoArgCtor = false;
                            scanner.usesClass(superName);
                            for (String s : interfaces) {
                                scanner.usesClass(s);
                                scanner.implementsInterface(string, s);
                            }
                        }

                        @Override
                        public void visitEnd() {
                            // Reported here rather than in visit(): the
                            // InnerClasses attribute arrives in between
                            // and can still rule the class out.
                            if (scannedName != null) {
                                if (scannedPublic) {
                                    scanner.declaresPublicType(scannedName);
                                }
                                if (scannedNonAbstract) {
                                    scanner.declaresConcreteType(scannedName);
                                }
                                scanner.declaresType(scannedName,
                                        scannedSuper, scannedConcrete
                                                && scannedHasPublicNoArgCtor);
                            }
                        }

                        @Override
                        public void visitSource(String string, String string1) {
                        }

                        @Override
                        public void visitOuterClass(String string, String string1, String string2) {
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String string, boolean bln) {
                            return null;
                        }

                        @Override
                        public void visitAttribute(Attribute atrbt) {
                        }

                        @Override
                        public void visitInnerClass(String string, String string1, String string2, int i) {
                            // Only the entry naming this very class, and
                            // only when it has an outer -- a null one is an
                            // anonymous or local class, which is neither
                            // nameable nor a member of anything.
                            if (string != null && string.equals(scannedName)
                                    && string1 != null) {
                                scanner.declaresEnclosedBy(string, string1);
                                // ACC_STATIC 0x0008. A non-static member
                                // class needs an enclosing instance, so
                                // `new Outer.Inner()` would not compile.
                                if ((i & 0x0008) == 0) {
                                    scannedConcrete = false;
                                }
                            }
                        }

                        @Override
                        public FieldVisitor visitField(int i, String string, String type, String string2, Object o) {
                            if (type.startsWith("L")) {
                                scanner.usesClass(descriptorToInternalName(type));
                            }
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int i, final String methodName, String string1, String string2, String[] strings) {
                            // ACC_PUBLIC 0x0001. The generated bindings
                            // call `new Listener()` from another package,
                            // so anything less than a public no-argument
                            // constructor produces source that does not
                            // compile.
                            if ("<init>".equals(methodName)
                                    && "()V".equals(string1)
                                    && (i & 0x0001) != 0) {
                                scannedHasPublicNoArgCtor = true;
                            }
                            return new MethodVisitor(Opcodes.ASM9) {
                                @Override
                                public AnnotationVisitor visitAnnotationDefault() {
                                    return null;
                                }

                                @Override
                                public AnnotationVisitor visitAnnotation(String string, boolean bln) {
                                    return null;
                                }

                                @Override
                                public AnnotationVisitor visitParameterAnnotation(int i, String string, boolean bln) {
                                    return null;
                                }

                                @Override
                                public void visitAttribute(Attribute atrbt) {
                                }

                                @Override
                                public void visitCode() {
                                }

                                @Override
                                public void visitFrame(int i, int i1, Object[] os, int i2, Object[] os1) {
                                    pushedBoolean = null;
                                }

                                /**
                                  * The literal pushed immediately before
                                  * the next call, or null. Cleared by
                                  * every other visit below -- including
                                  * labels and frames, since a constant
                                  * that is last before a merge point is
                                  * not a straight-line argument -- so
                                  * "unknown" is what a missed case
                                  * degrades to.
                                  */
                                private Boolean pushedBoolean;

                                @Override
                                public void visitInsn(int i) {
                                    pushedBoolean = i == Opcodes.ICONST_0
                                            ? Boolean.FALSE
                                            : i == Opcodes.ICONST_1
                                                    ? Boolean.TRUE : null;
                                }

                                @Override
                                public void visitIntInsn(int i, int i1) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitVarInsn(int i, int i1) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitTypeInsn(int i, String string) {
                                    pushedBoolean = null;
                                    scanner.usesClass(string);
                                }

                                @Override
                                public void visitFieldInsn(int i, String string, String string1, String string2) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitMethodInsn(int i, String owner, String name, String descriptor) {
                                    Boolean arg = pushedBoolean;
                                    pushedBoolean = null;
                                    scanner.usesClass(owner);
                                    if (name != null && !name.equals("<init>")) {
                                        scanner.usesClassMethod(owner, name);
                                        scanner.usesClassMethodWithBooleanArgument(
                                                owner, name, arg);
                                        scanner.usesClassMethodWithDescriptor(
                                                owner, name, descriptor);
                                    }
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    Boolean arg = pushedBoolean;
                                    pushedBoolean = null;
                                    scanner.usesClass(owner);
                                    if (name != null && !name.equals("<init>")) {
                                        scanner.usesClassMethod(owner, name);
                                        scanner.usesClassMethodWithBooleanArgument(
                                                owner, name, arg);
                                        scanner.usesClassMethodWithDescriptor(
                                                owner, name, descriptor);
                                    }
                                }

                                @Override
                                public void visitInvokeDynamicInsn(String name,
                                        String descriptor, Handle bootstrap,
                                        Object... args) {
                                    pushedBoolean = null;
                                    // A method reference -- store::readSamples,
                                    // or a lambda body -- is an invokedynamic
                                    // whose real target sits in the bootstrap
                                    // arguments as a Handle. visitMethodInsn
                                    // never sees it, so every such call was
                                    // invisible to feature detection: obtaining
                                    // the store registered, and the read it was
                                    // obtained for did not.
                                    if (args == null) {
                                        return;
                                    }
                                    for (Object a : args) {
                                        if (!(a instanceof Handle)) {
                                            continue;
                                        }
                                        Handle h = (Handle) a;
                                        if (h.getOwner() == null) {
                                            continue;
                                        }
                                        scanner.usesClass(h.getOwner());
                                        if (h.getName() != null
                                                && !"<init>".equals(h.getName())
                                                && !"<clinit>".equals(
                                                        h.getName())) {
                                            scanner.usesClassMethod(
                                                    h.getOwner(), h.getName());
                                            // Unknown, never absent. The
                                            // arguments of a method
                                            // reference are supplied
                                            // wherever it is later called,
                                            // which this insn cannot see --
                                            // and a consumer that decides
                                            // on the argument alone would
                                            // never hear about the call at
                                            // all, so options::setWriteTo
                                            // Store silently built an app
                                            // with no health stack.
                                            scanner.usesClassMethodWithBooleanArgument(
                                                    h.getOwner(), h.getName(),
                                                    null);
                                            // The Handle does carry the
                                            // descriptor of the exact
                                            // overload the reference was
                                            // resolved against, so a
                                            // MediaManager::createMedia
                                            // reference is as selectable
                                            // as a direct call.
                                            scanner.usesClassMethodWithDescriptor(
                                                    h.getOwner(), h.getName(),
                                                    h.getDesc());
                                        }
                                    }
                                }

                                @Override
                                public void visitJumpInsn(int i, Label label) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitLabel(Label label) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitLdcInsn(Object o) {
                                    pushedBoolean = null;
                                    if (o instanceof Type) {
                                        scanner.usesClass(((Type) o).getClassName());
                                    }
                                }

                                @Override
                                public void visitIincInsn(int i, int i1) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitMultiANewArrayInsn(String string, int i) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitTryCatchBlock(Label label, Label label1, Label label2, String string) {
                                }

                                @Override
                                public void visitLocalVariable(String string, String classType, String string2, Label label, Label label1, int i) {
                                    if (classType.startsWith("L")) {
                                        scanner.usesClass(descriptorToInternalName(classType));
                                    }
                                }

                                @Override
                                public void visitLineNumber(int i, Label label) {
                                    pushedBoolean = null;
                                }

                                @Override
                                public void visitMaxs(int i, int i1) {
                                }

                                @Override
                                public void visitEnd() {
                                }
                            };
                        }

                    };
                    try {
                        r.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    } catch(RuntimeException re) {
                        message.append(getCustomStackTrace(re));
                        message.append("Error encountered while parsing the class ");
                        message.append(current.getName());
                        throw new RuntimeException("Failed to parse class file "+current, re);

                    }
                }
            }
        }
    }

    protected abstract String getDeviceIdCode();



    protected void findFiles(List<File> result, File directory, final String filter) {
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(filter);
            }
        });
        for (File f : files) {
            if (f.isDirectory()) {
                findFiles(result, f, filter);
            } else {
                result.add(f);
            }
        }
    }

    /**
     * The {@code -source} / {@code -target} pair to compile the generated
     * application stub with.
     *
     * <p>Shared by both Apple builders, which generate the same shape of stub.
     * The default is 1.6 because ParparVM targets Java 5 with Java 8 syntax via
     * retrolambda; a JDK 9 or later javac refuses 1.6 outright, so those compile
     * the stub as 8 instead.</p>
     */
    /**
     * The leading integer of a version string, or {@code defaultVal} when it has
     * none.
     */
    protected int getMajorVersionInt(String versionStr, int defaultVal) {
        if (versionStr == null) {
            return defaultVal;
        }
        int pos = versionStr.indexOf(".");
        try {
            return Integer.parseInt(pos != -1 ? versionStr.substring(0, pos) : versionStr);
        } catch (Throwable ex) {
            return defaultVal;
        }
    }

    protected String[] getStubCompileSourceTarget(String javacPath) {
        String source = "1.6";
        String target = "1.6";
        int major = -1;
        String version = null;
        try {
            String versionOutput = execString(getBuildDirectory() != null ? getBuildDirectory() : new File("."), javacPath, "-version");
            if (versionOutput != null && versionOutput.trim().length() > 0) {
                String[] parts = versionOutput.trim().split("\\s+");
                version = parts[parts.length - 1];
                major = getMajorVersionInt(version, -1);
            }
        } catch (Exception ex) {
            debug("Failed to resolve the javac version for the stub compile: " + ex.getMessage());
        }
        if (major < 0) {
            version = System.getProperty("java.version");
            major = getMajorVersionInt(version, -1);
        }
        if (major >= 9) {
            source = "8";
            target = "8";
            log("JDK " + version + " does not support -source/-target 1.6. Compiling the stubs with -source/-target 8.");
        }
        return new String[]{source, target};
    }

    /**
     * The framework headers the generated native-interface bridge imports.
     *
     * <p>The bridge itself is platform neutral -- it moves a native peer pointer
     * across the ParparVM boundary -- but the file it lands in is compiled
     * alongside the port's own Objective-C, so it has to see the same UI
     * framework.</p>
     */
    /*
     * Type-name mapping for the generated native-interface bridge. Shared by
     * both Apple builders because the mangling is ParparVM's, not any one
     * platform's.
     */

    protected String convertToJavaMethod(Class type) {
        if(type.isArray()) {
            type = type.getComponentType();
            if(Integer.class == type || Integer.TYPE == type) {
                return "nsDataToIntArray(";
            }
            if(Long.class == type || Long.TYPE == type) {
                return "nsDataToLongArray(";
            }
            if(Byte.class == type || Byte.TYPE == type) {
                return "nsDataToByteArr(";
            }
            if(Short.class == type || Short.TYPE == type) {
                return "nsDataToShortArray(";
            }
            if(Character.class == type || Character.TYPE == type) {
                return "nsDataToCharArray(";
            }
            if(Boolean.class == type || Boolean.TYPE == type) {
                return "nsDataToBooleanArray(";
            }
            if(Float.class == type || Float.TYPE == type) {
                return "nsDataToFloatArray(";
            }
            if(Double.class == type || Double.TYPE == type) {
                return "nsDataToDoubleArray(";
            }
        }
        if(String.class == type) {
            return "fromNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }

    protected String getSimpleNameWithJavaLang(Class c) {
        if(c.isPrimitive()) {
            return c.getSimpleName();
        }
        if(c.isArray()) {
            return getSimpleNameWithJavaLang(c.getComponentType()) + "[]";
        }
        if(c.getClass().getName().startsWith("java.lang.")) {
            return c.getName();
        }
        return c.getSimpleName();
    }

    protected String typeToXMLVMJavaName(Class type) {
        if(type.isArray()) {
            return getSimpleNameWithJavaLang(type.getComponentType()).replace('.', '_') + "_1ARRAY";
        }
        return getSimpleNameWithJavaLang(type).replace('.', '_');
    }

    protected String typeToXMLVMName(Class type) {
        if(type.getName().equals("com.codename1.ui.PeerComponent")) {
            return "JAVA_LONG";
        }
        if(Integer.class == type || Integer.TYPE == type) {
            return "JAVA_INT";
        }
        if(Long.class == type || Long.TYPE == type) {
            return "JAVA_LONG";
        }
        if(Byte.class == type || Byte.TYPE == type) {
            return "JAVA_BYTE";
        }
        if(Short.class == type || Short.TYPE == type) {
            return "JAVA_SHORT";
        }
        if(Character.class == type || Character.TYPE == type) {
            return "JAVA_CHAR";
        }
        if(Boolean.class == type || Boolean.TYPE == type) {
            return "JAVA_BOOLEAN";
        }
        if(Void.class == type || Void.TYPE == type) {
            return "void";
        }
        if(Float.class == type || Float.TYPE == type) {
            return "JAVA_FLOAT";
        }
        if(Double.class == type || Double.TYPE == type) {
            return "JAVA_DOUBLE";
        }
        // array/string
        return "JAVA_OBJECT";
    }

    protected String convertToObjectiveCMethod(Class type) {
        if(type.isArray()) {
            return "arrayToData(";
        }
        if(String.class == type) {
            return "toNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }

    protected String convertToClosing(Class type) {
        if(type.isArray()) {
            return ")";
        }
        if(String.class == type) {
            return ")";
        }
        return "";
    }

    /**
     * The Objective-C type the generated bridge sends and receives for a Java
     * type.
     *
     * <p>Derived the same way the bridge body is: an array crosses as
     * {@code NSData}, a String as {@code NSString}, a PeerComponent as the
     * native handle, and everything else keeps its ParparVM C typedef.</p>
     */
    /// The marker every generated placeholder header carries, so a later build
    /// can tell its own file from one the developer wrote.
    private static final String PLACEHOLDER_MARKER = "Auto-generated placeholder: the native interface";

    private static boolean isGeneratedPlaceholderHeader(File header) {
        try {
            String body = new String(java.nio.file.Files.readAllBytes(header.toPath()), StandardCharsets.UTF_8);
            return body.contains(PLACEHOLDER_MARKER);
        } catch (IOException ex) {
            // Unreadable means "not ours": overwriting a file we cannot inspect
            // would be the one outcome worth avoiding here.
            return false;
        }
    }

    protected String objectiveCTypeFor(Class type) {
        if (type.isArray()) {
            return "NSData*";
        }
        if (String.class == type) {
            return "NSString*";
        }
        return typeToXMLVMName(type);
    }

    protected String nativeInterfaceFrameworkImports() {
        return "#import \"CodenameOne_GLViewController.h\"\n"
                + "#import <UIKit/UIKit.h>\n";
    }

    /**
     * Generates the Java and Objective-C halves of every {@code @NativeInterface}
     * binding the application declares.
     *
     * <p>Shared by both Apple builders. The Java class carries the native methods
     * ParparVM turns into C functions; the generated {@code .m} defines those
     * functions and forwards each to the developer's own Objective-C class,
     * reached through a peer pointer. A native interface with no implementation
     * in the project gets a placeholder header rather than a build failure, and
     * calls into it no-op -- which is what the runtime already does when the peer
     * comes back nil.</p>
     */
    protected void generateNativeInterfaceBindings(File stubSource, File resDir) throws BuildException {
            Class[] nativeInterfaces = getNativeInterfaces();
            if(nativeInterfaces != null && nativeInterfaces.length > 0) {
                for(Class currentNative : nativeInterfaces) {
                    File folder = new File(stubSource, currentNative.getPackage().getName().replace('.', File.separatorChar));
                    folder.mkdirs();
                    File javaFile = new File(folder, currentNative.getSimpleName() + getImplSuffix() + ".java");
                
                    String javaImplSourceFile = "package " + currentNative.getPackage().getName() + ";\n\n"
                            + "import com.codename1.ui.PeerComponent;\n\n"
                            + "public class " + currentNative.getSimpleName() + getImplSuffix() + " {\n"
                            + "    private long nativePeer;\n\n"
                            + "    public " + currentNative.getSimpleName() + getImplSuffix() + "() {\n"
                            + "        nativePeer = initializeNativePeer();\n"
                            + "    }\n\n"
                            + "    public void finalize() {\n"
                            + "        releaseNativePeerInstance(nativePeer);\n"
                            + "    }\n\n"
                            + "    private static native long initializeNativePeer();\n\n"
                            + "    private static native void releaseNativePeerInstance(long peer);\n\n";
                
                    String prefixForNewVM = "";
                    String postfixForNewVM = "";
                    String prefix2ForNewVM = "";
                    String newVMEnterNativeCode = "";
                    String newVMExitNativeCode = "";
                    String newVMInclude = "";

                    newVMInclude = "\n#include \"cn1_globals.h\"\n";
                    newVMEnterNativeCode = "    POOL_BEGIN();\n    enteringNativeAllocations();\n";
                    newVMExitNativeCode = "    finishedNativeAllocations();\n    POOL_END();\n";
                    prefixForNewVM = "CODENAME_ONE_THREAD_STATE";
                    prefix2ForNewVM = "CODENAME_ONE_THREAD_STATE, ";
                    postfixForNewVM = "_R_long";

                    String classNameWithUnderscores = currentNative.getName().replace('.', '_');
                    String mSourceFile = "#include \"xmlvm.h\"\n"
                            + "#include \"java_lang_String.h\"\n"
                            + "#include <stdlib.h>\n"
                            + nativeInterfaceFrameworkImports()
                            + "#import <objc/runtime.h>\n"
                            + "#import \"" + classNameWithUnderscores + "Impl.h\"\n"
                            + newVMInclude
                            + "#include \"" + classNameWithUnderscores + getImplSuffix() + ".h\"\n\n"
                            + "static id cn1_createNativeInterfacePeer(NSString* className) {\n"
                            + "    NSMutableArray* candidates = [NSMutableArray arrayWithObject:className];\n"
                            + "    NSString* executableName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"CFBundleExecutable\"];\n"
                            + "    NSString* bundleName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"CFBundleName\"];\n"
                            + "    NSArray* moduleNames = @[executableName ?: @\"\", bundleName ?: @\"\"];\n"
                            + "    for(NSString* moduleName in moduleNames) {\n"
                            + "        if(moduleName.length == 0) {\n"
                            + "            continue;\n"
                            + "        }\n"
                            + "        NSString* sanitized = [[moduleName stringByReplacingOccurrencesOfString:@\"-\" withString:@\"_\"] stringByReplacingOccurrencesOfString:@\" \" withString:@\"_\"];\n"
                            + "        [candidates addObject:[sanitized stringByAppendingFormat:@\".%@\", className]];\n"
                            + "        if(![sanitized isEqualToString:moduleName]) {\n"
                            + "            [candidates addObject:[moduleName stringByAppendingFormat:@\".%@\", className]];\n"
                            + "        }\n"
                            + "    }\n"
                            + "    Class cls = Nil;\n"
                            + "    for(NSString* candidate in candidates) {\n"
                            + "        cls = NSClassFromString(candidate);\n"
                            + "        if(cls != Nil) {\n"
                            + "            break;\n"
                            + "        }\n"
                            + "    }\n"
                            + "    if(cls == Nil) {\n"
                            + "        unsigned int classCount = 0;\n"
                            + "        Class *classList = objc_copyClassList(&classCount);\n"
                            + "        NSString* dottedSuffix = [@\".\" stringByAppendingString:className];\n"
                            + "        for(unsigned int i = 0; i < classCount; i++) {\n"
                            + "            NSString* runtimeName = [NSString stringWithUTF8String:class_getName(classList[i])];\n"
                            + "            if([runtimeName isEqualToString:className] || [runtimeName hasSuffix:dottedSuffix] || [runtimeName hasSuffix:className]) {\n"
                            + "                cls = classList[i];\n"
                            + "                NSLog(@\"[CN1] Resolved native interface class %@ via runtime scan as %@\", className, runtimeName);\n"
                            + "                break;\n"
                            + "            }\n"
                            + "        }\n"
                            + "        if(classList != NULL) {\n"
                            + "            free(classList);\n"
                            + "        }\n"
                            + "    }\n"
                            + "    if(cls == Nil) {\n"
                            + "        NSLog(@\"[CN1] Failed to find native interface class %@. Tried: %@\", className, candidates);\n"
                            + "        return nil;\n"
                            + "    }\n"
                            + "    return [[cls alloc] init];\n"
                            + "}\n\n"
                            + "JAVA_LONG " + classNameWithUnderscores + getImplSuffix() + "_initializeNativePeer__" + postfixForNewVM + "(" + prefixForNewVM + ") {\n"
                            + "    id i = cn1_createNativeInterfacePeer(@\"" + classNameWithUnderscores + "Impl\");\n"
                            + "    return i;\n"
                            + "}\n\n"
                            + "void " + classNameWithUnderscores + getImplSuffix() + "_releaseNativePeerInstance___long(" + prefix2ForNewVM + "JAVA_LONG l) {\n"
                            + "    id i = (id)l;\n"
                            + "    [i release];\n"
                            + "}\n\n"
                            + "extern NSData* arrayToData(JAVA_OBJECT arr);\n"
                            + "extern NSString* toNSString(" + prefix2ForNewVM + "JAVA_OBJECT str);\n"
                            + "extern JAVA_OBJECT nsDataToByteArr(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToBooleanArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToCharArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToShortArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToIntArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToLongArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToFloatArray(NSData *data);\n"
                            + "extern JAVA_OBJECT nsDataToDoubleArray(NSData *data);\n\n"
                            + "void xmlvm_init_native_"+ classNameWithUnderscores + getImplSuffix() + "() {}\n\n";

                    for(Method m : currentNative.getMethods()) {
                        String name = m.getName();
                        if(name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                            continue;
                        }
                    
                        Class returnType = m.getReturnType();
                    
                        mSourceFile += typeToXMLVMName(returnType) + " " + currentNative.getName().replace('.', '_') + getImplSuffix() + "_" + 
                                name + "__";
                        String mFileArgs;
                        String mFileBody;

                        mFileArgs = "(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me";
                        mFileBody = "    id ptr = (id)get_field_" + classNameWithUnderscores + getImplSuffix() + "_nativePeer(me);\n";

                    
                        if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                            mFileBody += "    " + typeToXMLVMName(returnType) + " returnValue = " + convertToJavaMethod(returnType);
                        }
                        mFileBody += "[((" + classNameWithUnderscores + "Impl*)ptr) " + name;
                    
                        if(returnType.getName().equals("com.codename1.ui.PeerComponent")) {
                            javaImplSourceFile += "    public native long " + name + "(";
                        } else {
                            javaImplSourceFile += "    public native " + getSimpleNameWithJavaLang(returnType) + " " + name + "(";
                        }
                        Class[] params = m.getParameterTypes();
                        if(params != null && params.length > 0) {
                            for(int iter = 0 ; iter < params.length ; iter++) {
                                if(params[iter].getName().equals("com.codename1.ui.PeerComponent")) {
                                    params[iter] = Long.TYPE;
                                }
                            }
                            javaImplSourceFile += getSimpleNameWithJavaLang(params[0]) + " param0";
                            for(int iter = 1 ; iter < params.length ; iter++) {
                                javaImplSourceFile += ", " + getSimpleNameWithJavaLang(params[iter]) + " param" + iter;
                            }
                                                
                            for(int iter = 0 ; iter < params.length ; iter++) {
                                mSourceFile += "_" + typeToXMLVMJavaName(params[iter]);
                                mFileArgs += ", " + typeToXMLVMName(params[iter]) + " param" + iter;
                                if(iter == 0) {
                                    mFileBody += ":" + convertToObjectiveCMethod(params[iter]) + "param0" + convertToClosing(params[iter]); 
                                } else {
                                    mFileBody += " param" + iter + ":" + convertToObjectiveCMethod(params[iter]) + "param" + iter + convertToClosing(params[iter]); 
                                }
                            }
                        }

                        if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                            if(returnType.getName().endsWith("PeerComponent")) {
                                mSourceFile += "_R_long";
                            } else {
                                mSourceFile += "_R_" + typeToXMLVMJavaName(returnType);
                            }
                        }

                        if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                            mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                    mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                    + "    return returnValue;\n}\n\n";                        
                        } else {
                            mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                    mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                    + "}\n\n";                        
                        }
                        javaImplSourceFile += ");\n";
                    }
                
                    javaImplSourceFile += "}\n";
                
                
                    try (FileOutputStream out = new FileOutputStream(javaFile)) {
                        out.write(javaImplSourceFile.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                    }
                    File mFile = new File(resDir, "native_" + currentNative.getName().replace('.', '_') + getImplSuffix() + ".m");

                    try (FileOutputStream out = new FileOutputStream(mFile)) {
                        out.write(mSourceFile.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                    }

                    // The generated .m imports "<X>Impl.h" -- the Objective-C
                    // class the user is expected to provide as their native
                    // implementation. When no such class exists for this app
                    // (native interfaces pulled in transitively from a CN1
                    // library, the app never instantiates them), the build
                    // still needs an @interface in scope so the .m compiles.
                    // Generate a tiny placeholder iff the user hasn't dropped
                    // their own copy alongside the project sources. The peer
                    // class itself stays absent at runtime, which is fine: any
                    // call into this native interface from Java would have
                    // failed to resolve a peer regardless.
                    File implHeader = new File(resDir, classNameWithUnderscores + "Impl.h");
                    // Rewritten when the file on disk is one of ours, not only
                    // when it is missing. The build directory survives between
                    // runs, so a placeholder written by an earlier build would
                    // otherwise be kept forever -- including after the interface
                    // gained a method, which is a stale declaration set standing
                    // in front of the real one.
                    if (!implHeader.exists() || isGeneratedPlaceholderHeader(implHeader)) {
                        String guard = classNameWithUnderscores.toUpperCase() + "_IMPL_H";
                        // The methods are declared, not just the class. The
                        // generated bridge sends them to a receiver typed as this
                        // class, so an empty @interface makes every one of those
                        // an unchecked send -- and a build that treats an
                        // unchecked send as an error, which the macOS one does
                        // because that is how a UIKit-to-AppKit port crashes,
                        // cannot tell this apart from a real mistake.
                        StringBuilder decls = new StringBuilder();
                        for (Method placeholderMethod : currentNative.getMethods()) {
                            String placeholderName = placeholderMethod.getName();
                            if (placeholderName.equals("hashCode") || placeholderName.equals("equals")
                                    || placeholderName.equals("toString")) {
                                continue;
                            }
                            decls.append("- (")
                                 .append(objectiveCTypeFor(placeholderMethod.getReturnType()))
                                 .append(")").append(placeholderName);
                            Class[] placeholderParams = placeholderMethod.getParameterTypes();
                            for (int iter = 0; iter < placeholderParams.length; iter++) {
                                if (iter > 0) {
                                    decls.append(" param").append(iter);
                                }
                                decls.append(":(").append(objectiveCTypeFor(placeholderParams[iter]))
                                     .append(")param").append(iter);
                            }
                            decls.append(";\n");
                        }
                        String hStub = "#ifndef " + guard + "\n"
                                + "#define " + guard + "\n"
                                + "// " + PLACEHOLDER_MARKER + " "
                                + currentNative.getName() + " has no user-provided\n"
                                + "// Objective-C implementation in this project. The CN1\n"
                                + "// runtime returns nil from cn1_createNativeInterfacePeer\n"
                                + "// in that case; calls into the peer no-op silently.\n"
                                + "#include \"cn1_globals.h\"\n"
                                + "#import <Foundation/Foundation.h>\n"
                                + "@interface " + classNameWithUnderscores + "Impl : NSObject\n"
                                + decls
                                + "@end\n"
                                + "#endif\n";
                        try (FileOutputStream out = new FileOutputStream(implHeader)) {
                            out.write(hStub.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException ex) {
                            throw new BuildException("Error while generating placeholder header for "+currentNative, ex);
                        }
                    }
                }
            }
    }

    public Class[] getNativeInterfaces() {
        return nativeInterfaces;
    }

    protected String getImplSuffix() {
        return "Impl";
    }

    protected String registerNativeImplementationsAndCreateStubs(ClassLoader parentClassLoader, File stubDir, File... classesDirectory) throws MalformedURLException, IOException {
        nativeInterfaces = findNativeInterfaces(parentClassLoader, classesDirectory);
        String registerNativeFunctions = "";
        if (nativeInterfaces != null && nativeInterfaces.length > 0) {
            for (Class n : nativeInterfaces) {
                registerNativeFunctions += "        NativeLookup.register(" + n.getName() + ".class, "
                        + n.getName() + "Stub.class" + ");\n";
            }
        }

        if (nativeInterfaces != null && nativeInterfaces.length > 0) {
            for (Class currentNative : nativeInterfaces) {
                File folder = new File(stubDir, currentNative.getPackage().getName().replace('.', File.separatorChar));
                folder.mkdirs();
                File javaFile = new File(folder, currentNative.getSimpleName() + "Stub.java");

                String javaImplSourceFile = "package " + currentNative.getPackage().getName() + ";\n\n"
                        + "import com.codename1.ui.PeerComponent;\n\n"
                        + "public class " + currentNative.getSimpleName() + "Stub implements " + currentNative.getSimpleName() + "{\n"
                        + "    private " + currentNative.getSimpleName() + getImplSuffix() + " impl = new " + currentNative.getSimpleName() + getImplSuffix() + "();\n\n";

                for (Method m : currentNative.getMethods()) {
                    String name = m.getName();
                    if (name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                        continue;
                    }

                    Class returnType = m.getReturnType();

                    javaImplSourceFile += "    public " + returnType.getSimpleName() + " " + name + "(";
                    Class[] params = m.getParameterTypes();
                    String args = "";
                    if (params != null && params.length > 0) {
                        for (int iter = 0; iter < params.length; iter++) {
                            if (iter > 0) {
                                javaImplSourceFile += ", ";
                                args += ", ";
                            }
                            javaImplSourceFile += params[iter].getSimpleName() + " param" + iter;
                            if (params[iter].getName().equals("com.codename1.ui.PeerComponent")) {
                                args += convertPeerComponentToNative("param" + iter);
                            } else {
                                args += "param" + iter;
                            }
                        }
                    }
                    javaImplSourceFile += ") {\n";
                    if (Void.class == returnType || Void.TYPE == returnType) {
                        javaImplSourceFile += "        impl." + name + "(" + args + ");\n    }\n\n";
                    } else {
                        if (returnType.getName().equals("com.codename1.ui.PeerComponent")) {
                            javaImplSourceFile += "        return " + generatePeerComponentCreationCode("impl." + name + "(" + args + ")") + ";\n    }\n\n";
                        } else {
                            javaImplSourceFile += "        return impl." + name + "(" + args + ");\n    }\n\n";
                        }
                    }
                }

                javaImplSourceFile += "}\n";

                FileOutputStream out = new FileOutputStream(javaFile);
                out.write(javaImplSourceFile.getBytes(StandardCharsets.UTF_8));
                out.close();
            }
        }

        return registerNativeFunctions;
    }

    protected abstract String generatePeerComponentCreationCode(String methodCallString);

    protected abstract String convertPeerComponentToNative(String param);

    protected boolean execWithFiles(File dir, File filesDir, String filter, String... varArgs) throws Exception {
        List<File> fileList = new ArrayList<File>();
        findFiles(fileList, filesDir, filter);
        String[] args = new String[fileList.size() + varArgs.length];
        System.arraycopy(varArgs, 0, args, 0, varArgs.length);
        for (int iter = 0; iter < fileList.size(); iter++) {
            args[varArgs.length + iter] = fileList.get(iter).getAbsolutePath();
        }
        return exec(dir, args);
    }

    protected Class[] findNativeInterfaces(ClassLoader parentClassLoader, File... classesDirectories) throws MalformedURLException, IOException {
        URL[] urls = new URL[classesDirectories.length];
        for (int iter = 0; iter < urls.length; iter++) {
            urls[iter] = classesDirectories[iter].toURI().toURL();
        }
        URLClassLoader cl = new URLClassLoader(urls, parentClassLoader);

        // first directory is assumed to be the user classes directory
        List<Class> classList = new ArrayList<Class>();
        for (File userClassesDirectory : classesDirectories) {


            findNativeClassesInDir(userClassesDirectory.getAbsolutePath(), userClassesDirectory, cl, classList);

        }
        Class[] arr = new Class[classList.size()];
        classList.toArray(arr);
        return arr;
    }

    private void findNativeClassesInDir(String baseDir, File directory, URLClassLoader cl, List<Class> classList) throws IOException {
        File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || (file.getName().endsWith(".class") && file.getName().indexOf('$') < 0)
                        || file.getName().endsWith(".jar");
            }
        });
        for (File f : files) {
            if (f.isDirectory()) {
                findNativeClassesInDir(baseDir, f, cl, classList);
            } else {
                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".jar")) {
                    FileInputStream zipFile = new FileInputStream(fileName);
                    ZipInputStream zip = new ZipInputStream(zipFile);
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {

                        if (entry.isDirectory()) {
                            continue;
                        }

                        String entryName = entry.getName();
                        if (entryName.endsWith(".class") && entryName.indexOf('$') < 0) {
                            String className = entryName.substring(baseDir.length() + 1, entryName.length() - 6);
                            className = className.replace('/', '.');
                            isNativeInterface(cl, className, classList);
                        }
                    }
                    zip.close();
                } else {
                    String className = fileName.substring(baseDir.length() + 1, fileName.length() - 6);
                    className = className.replace(File.separatorChar, '.');
                    isNativeInterface(cl, className, classList);
                }
            }
        }
    }

    private void isNativeInterface(ClassLoader cl, String className, List<Class> classList) {
        try {
            Class cls = cl.loadClass(className);
            if (cls.isInterface()) {
                for (Class current : cls.getInterfaces()) {
                    if (current.getName().equals("com.codename1.system.NativeInterface")) {
                        debug(className + " is a native interface");
                        classList.add(cls);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            warn("Evaluated " + className + " it is not a native interface " + t, t);
        }
    }

    protected File createTmpDir() throws IOException {
        tmpDir = createTempFile("build", "xxx");
        tmpDir.delete();

        tmpDir.mkdirs();
        return tmpDir;
    }

    public static void delTree(File f){
        delTree(f, false);
    }
    public static void delTree(File f, boolean force) {
        if (!force && disableDelete) {
            return;
        }
        if (f != null && f.isDirectory()) {
            for (String current : f.list()) {
                File ff = new File(f, current);
                if (ff.isDirectory()) {
                    delTree(ff, force);
                }
                ff.setWritable(true);
                ff.delete();
            }
        }
    }



    protected long getTimeoutValue() {
        return 25 * 60 * 60 * 1000;
    }

    private static void verifyCN1Install() throws IOException {
        File cn1Home = new File(System.getProperty("user.home"), ".codenameone");
        File UpdateCodenameOneJar = new File(cn1Home, "UpdateCodenameOne.jar");
        if(!cn1Home.exists() || !UpdateCodenameOneJar.exists()) {
            cn1Home.mkdirs();
            URL update = new URL("https://www.codenameone.com/files/updates/UpdateCodenameOne.jar");
            InputStream is = update.openStream();
            OutputStream os = new FileOutputStream(UpdateCodenameOneJar);
            copy(is, os);
        }
    }

    private void updateProjectLibs(BuildRequest r, File path) throws Exception {
        File cn1Home = new File(System.getProperty("user.home"), ".codenameone");
        File updateJar = new File(cn1Home, "UpdateCodenameOne.jar");
        File java8Home = new File(System.getProperty("java.home"));
        String java = new File(java8Home + "bin" + File.separator + "java").getAbsolutePath();
        if(is_windows) {
            java += ".exe";
        }
        HashMap<String, String> env = new HashMap<String, String>();
        exec(path, env, java, "-jar", updateJar.getAbsolutePath(), path.getAbsolutePath());
    }



    private byte[] fileToByteArray(File certFileO) throws IOException {
        if(certFileO.exists()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(certFileO));
            byte[] data = new byte[(int)certFileO.length()];
            dis.readFully(data);
            dis.close();
            return data;
        }
        return null;
    }

    public boolean buildNoException(final File sourceZip, final BuildRequest request) {
        try {
            if (isCanceled()) {
                return false;
            }
            final boolean[] result = new boolean[1];
            final boolean[] alive = new boolean[]{true};
            final Object LOCK = new Object();
            Thread t = new Thread() {
                public void run() {
                    try {
                        File s = hardenSourceJar(sourceZip, request);
                        result[0] = build(s, request);

                    } catch (Throwable err) {
                        err.printStackTrace();
                        if (err.getCause() != null) {
                            err.getCause().printStackTrace();
                            debug(err.getCause().toString());
                            message.append(getCustomStackTrace(err.getCause()));
                        }
                        message.append(getCustomStackTrace(err));
                    }
                    synchronized (LOCK) {
                        alive[0] = false;
                        LOCK.notify();
                    }
                }
            };
            t.start();

            long time = System.currentTimeMillis() + getTimeoutValue();
            synchronized (LOCK) {
                LOCK.wait(3000);
                while (alive[0]) {
                    if (isCanceled()) {
                        t.stop();
                        return false;
                    }
                    LOCK.wait(3000);
                    if (System.currentTimeMillis() > time) {
                        canceled = true;
                        t.stop();
                        return false;
                    }
                }
            }

            return result[0];
        } catch (Exception err) {
            err.printStackTrace();
            message.append(getCustomStackTrace(err));
        }
        return false;
    }

    protected String getDebugCertificateFile() {
        return "ios_debug.p12";
    }

    protected String getReleaseCertificateFile() {
        return "ios_release.p12";
    }

    protected String getDebugCertificatePasswordKey() {
        return "codename1.android.keystorePassword";
    }

    protected String getReleaseCertificatePasswordKey() {
        return getDebugCertificatePasswordKey();
    }

    protected boolean isCanceled() {
        return canceled;


    }

    public static String getCustomStackTrace(Throwable aThrowable) {
        //add the class name and any message passed to constructor
        final StringBuilder result = new StringBuilder("Exception: ");
        result.append(aThrowable.toString());
        final String NEW_LINE = System.getProperty("line.separator");
        result.append(NEW_LINE);

        //add each element of the stack trace
        for (StackTraceElement element : aThrowable.getStackTrace()) {
            result.append(element);
            result.append(NEW_LINE);
        }
        return result.toString();
    }

    public abstract boolean build(File sourceZip, BuildRequest request) throws BuildException;

    public String getErrorMessage() {
        return message.toString();
    }

    protected void createIconFile(File f, BufferedImage icon, int w, int h) throws IOException {
        ImageIO.write(getScaledInstance(icon, w, h), "png", f);
    }



    protected void createUnevenIconFile(File f, BufferedImage icon, int w, int h) throws IOException {
        ImageIO.write(getScaledUnevenInstance(icon, w, h), "png", f);
    }

    public String getMimetypeFor(File f) {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".ipa")) {
            return "application/octet-stream";
        }
        if (name.endsWith(".png") || name.equals("iTunesArtwork")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.equals("jpeg")) {
            return "image/jpg";
        }
        if (name.endsWith(".bz2")) {
            return "application/bzip2";
        }
        if (name.endsWith(".zip")) {
            return "application/zip";
        }
        if (name.endsWith(".jad")) {
            return "text/vnd.sun.j2me.app-descriptor";
        }
        if (name.endsWith(".jar")) {
            return "application/java-archive";
        }
        if (name.endsWith(".cod")) {
            return "application/vnd.rim.cod";
        }
        if (name.endsWith(".cod")) {
            return "application/vnd.rim.cod";
        }
        if (name.endsWith(".apk")) {
            return "application/vnd.android.package-archive";
        }
        if (name.endsWith(".txt")) {
            return "plain/text";
        }
        if (name.endsWith(".p12")) {
            return "application/x-pkcs12";
        }
        if (name.endsWith(".xap")) {
            return "application/x-silverlight-app";
        }
        if (name.endsWith(".cer")) {
            return "application/x-x509-ca-cert";
        }
        if (name.endsWith(".dmg")) {
            return "application/x-apple-diskimage";
        }
        if (name.endsWith(".msi")) {
            return "application/x-msi";
        }
        if (name.endsWith(".exe")) {
            return "application/octet-stream";
        }
        if (name.endsWith(".war")){
            return "application/java-archive";
        }
        if (name.endsWith(".html")){
            return "text/html";
        }

        return "application/unknown";
    }


    private void copyDir(File dir, File classesDir, File resDir, File sourceDir, File libsDir) throws IOException {
        for (File currentFile : dir.listFiles()) {
            String fileName = currentFile.getName();
            if (currentFile.isDirectory()) {
                File newClassesDir = new File(classesDir, fileName);
                newClassesDir.mkdirs();
                File newresDir = new File(resDir, fileName);
                newresDir.mkdirs();
                File newsourceDir = new File(sourceDir, fileName);
                newsourceDir.mkdirs();
                File newlibsDir = new File(libsDir, fileName);
                newlibsDir.mkdirs();
                copyDir(currentFile, newClassesDir, newresDir, newsourceDir, newlibsDir);
                continue;
            }
            File destFile;
            if (fileName.endsWith(".class")) {
                if (fileName.equals("module-info.class")) {
                    continue;
                } else {
                    destFile = new File(classesDir, fileName);
                }
            } else {
                if (fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".swift") || fileName.endsWith(".m") || fileName.endsWith(".h") || fileName.endsWith(".cs")) {
                    destFile = new File(sourceDir, fileName);
                } else {
                    if (fileName.endsWith(".jar") || fileName.endsWith(".a") || fileName.endsWith(".dylib")) {
                        destFile = new File(libsDir, fileName);
                    } else {
                        destFile = new File(resDir, fileName);
                    }
                }
            }
            destFile.getParentFile().mkdirs();
            DataInputStream di = new DataInputStream(new FileInputStream(currentFile));
            byte[] data = new byte[(int) currentFile.length()];
            di.readFully(data);
            di.close();

            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(data);
            fos.close();
        }
    }

    public static void copy(File source, File dest) throws IOException {
        copy(new FileInputStream(source), new FileOutputStream(dest));
    }

    public static void copyDirectory(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            dest.mkdir();
            for (File child : source.listFiles()) {
                if (child.isDirectory()) {
                    copyDirectory(child, new File(dest, child.getName()));
                } else {
                    copy(child, new File(dest, child.getName()));
                }
            }
        } else {
            copy(source, dest);
        }
    }

    /**
     * Copy the input stream into the output stream, closes both streams when
     * finishing or in a case of an exception
     *
     * @param i source
     * @param o destination
     */
    public static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, closes both streams when
     * finishing or in a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2
     * large enoguh
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while (size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            cleanup(o);
            cleanup(i);
        }
    }

    /**
     * Closes the object (connection, stream etc.) without throwing any
     * exception, even if the object is null
     *
     * @param o Connection, Stream or other closeable object
     */
    public static void cleanup(Object o) {
        try {
            if (o instanceof OutputStream) {
                ((OutputStream) o).close();
                return;
            }
            if (o instanceof InputStream) {
                ((InputStream) o).close();
                return;
            }
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    public void unzip(File source, File classesDir, File resDir, File sourceDir) throws IOException {
        unzip(source, classesDir, resDir, sourceDir, resDir);
    }

    public void unzip(File source, File classesDir, File resDir, File sourceDir, File libsDir) throws IOException {
        unzip(source, classesDir, resDir, sourceDir, libsDir, resDir);
    }

    public void unzip(File source, File classesDir, File resDir, File sourceDir, File libsDir, File xmlDir) throws IOException {
        if (source.isDirectory()) {
            copyDir(source, classesDir, resDir, sourceDir, libsDir);
            return;
        }
        FileInputStream fi = new FileInputStream(source);
        unzip(fi, classesDir, resDir, sourceDir, libsDir, xmlDir);
    }

    public void unzip(InputStream source, File classesDir, File resDir, File sourceDir) throws IOException {
        unzip(source, classesDir, resDir, sourceDir, resDir, resDir);
    }

    public void unzip(InputStream source, File classesDir, File resDir, File sourceDir, File libsDir) throws IOException {
        unzip(source, classesDir, resDir, sourceDir, libsDir, resDir);
    }

    protected boolean useXMLDir() {
        return false;
    }

    protected boolean isDllResource() {
        return false;
    }

    public void unzip(InputStream source, File classesDir, File resDir, File sourceDir, File libsDir, File xmlDir) throws IOException {
        try {
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(source);
            ZipEntry entry;
            TarOutputStream tos = null;
            TarOutputStream podspecTos = null;
            TarOutputStream libTos = null;
            String dll = ".dll";
            if(isDllResource()) {
                dll = ".this isn't a valid extension";
            }
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.startsWith("html/")
                        || entryName.startsWith("/html/")) {
                    if(entry.isDirectory()) {
                        continue;
                    }
                    int htmlPrefix = entryName.startsWith("html/")
                            ? "html/".length() : "/html/".length();
                    entryName = entryName.substring(htmlPrefix);
                    // The stripped name becomes a member of another archive,
                    // so validate it independently against that archive's root.
                    resolveArchiveEntry(resDir, entryName);
                    if(tos == null) {
                        tos = new TarOutputStream(new FileOutputStream(new File(resDir, "html.tar")));
                    }
                    TarEntry tEntry = new TarEntry(new File(entryName), entryName);
                    tEntry.setSize(entry.getSize());
                    debug("Packaging entry " + entryName + " size: " + entry.getSize());
                    tos.putNextEntry(tEntry);
                    int count;
                    byte[] data = new byte[8192];
                    while ((count = zis.read(data, 0, data.length)) != -1) {
                        tos.write(data, 0, count);
                    }
                    continue;
                }
                if(entryName.startsWith("podspecs/") || entryName.startsWith("/podspecs/")) {
                    if(entry.isDirectory()) {
                        continue;
                    }
                    int podSpecsPrefix = entryName.startsWith("podspecs/") ? "podspecs/".length() : "/podspecs/".length();
                    entryName = entryName.substring(podSpecsPrefix);
                    resolveArchiveEntry(resDir, entryName);
                    if(podspecTos == null) {
                        podspecTos = new TarOutputStream(new FileOutputStream(new File(resDir, "podspecs.tar")));
                    }
                    TarEntry tEntry = new TarEntry(new File(entryName), entryName);
                    tEntry.setSize(entry.getSize());
                    debug("Packaging entry " + entryName + " size: " + entry.getSize());
                    podspecTos.putNextEntry(tEntry);
                    int count;
                    byte[] data = entry.getSize() >=819200 ? new byte[819200] : new byte[8192];
                    while ((count = zis.read(data, 0, data.length)) != -1) {
                        podspecTos.write(data, 0, count);
                    }
                    continue;
                }


                if(entryName.startsWith("javase.lib/") || entryName.startsWith("/javase.lib/")) {
                    if(entry.isDirectory()) {
                        continue;
                    }
                    int libPrefix = entryName.startsWith("javase.lib/") ? "javase.lib/".length() : "/javase.lib/".length();
                    entryName = entryName.substring(libPrefix);
                    resolveArchiveEntry(resDir, entryName);
                    if(libTos == null) {
                        libTos = new TarOutputStream(new FileOutputStream(new File(resDir, "javase.lib.tar")));
                    }
                    TarEntry tEntry = new TarEntry(new File(entryName), entryName);
                    tEntry.setSize(entry.getSize());
                    debug("Packaging entry " + entryName + " size: " + entry.getSize());
                    libTos.putNextEntry(tEntry);
                    int count;
                    byte[] data = entry.getSize() >=819200 ? new byte[819200] : new byte[8192];
                    while ((count = zis.read(data, 0, data.length)) != -1) {
                        libTos.write(data, 0, count);
                    }
                    continue;
                }

                // Entries outside the supported virtual cn1lib namespaces
                // retain their original ZIP name, which must be relative and
                // remain inside the extraction root.
                resolveArchiveEntry(resDir, entryName);

                if (entry.isDirectory()) {
                    File dir = resolveArchiveEntry(classesDir, entryName);
                    dir.mkdirs();
                    dir = resolveArchiveEntry(resDir, entryName);
                    dir.mkdirs();
                    dir = resolveArchiveEntry(sourceDir, entryName);
                    dir.mkdirs();
                    continue;
                }

                int count;
                byte[] data = new byte[8192];

                // write the files to the disk
                File destFile;
                if (entryName.endsWith(".class")) {
                    if (entryName.endsWith("module-info.class")) {
                        log("!!!!Skipping "+entryName);
                        continue;
                    } else {
                        destFile = resolveArchiveEntry(classesDir, entryName);
                    }
                } else {
                    if (entryName.endsWith(".java") || entryName.endsWith(".kt") || entryName.endsWith(".swift") || entryName.endsWith(".m") || entryName.endsWith(".h") || entryName.endsWith(".cs")) {
                        destFile = resolveArchiveEntry(sourceDir, entryName);
                    } else {
                        if (entryName.endsWith(".jar") || entryName.endsWith(".a") || entryName.endsWith(".dylib") || entryName.endsWith(".andlib") || entryName.endsWith(".aar") || entryName.endsWith(dll)) {
                            destFile = resolveArchiveEntry(libsDir, entryName);
                        } else {
                            if (useXMLDir() && entryName.endsWith(".xml")) {
                                destFile = placeXMLFile(entry, xmlDir, resDir);
                            } else {
                                if(entryName.equals("codenameone_settings.properties")) {
                                    destFile = resolveArchiveEntry(sourceDir.getParentFile(), entryName);
                                } else {
                                    destFile = resolveArchiveEntry(resDir, entryName);
                                }
                            }
                        }
                    }
                }
                destFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(destFile);
                dest = new BufferedOutputStream(fos, data.length);
                while ((count = zis.read(data, 0, data.length)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            if(tos != null) {
                tos.close();
            }
            if (podspecTos != null) {
                podspecTos.close();
            }
            if (libTos != null) {
                libTos.close();
            }
            zis.close();
            source.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ExtractionFilter {
        public File destFile(String path, String fileName);
    }

    public void extractZip(InputStream source, File destDir, ExtractionFilter filter) throws IOException {
        try {
            BufferedOutputStream dest = null;
            ZipInputStream zis = new ZipInputStream(source);
            ZipEntry entry;
            String currentDir = null;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                debug("Extracting "+entryName);

                if (entry.isDirectory()) {
                    currentDir = entryName;
                    File dir = resolveArchiveEntry(destDir, entryName);
                    dir.mkdirs();
                    continue;
                }

                int count;
                byte[] data = new byte[8192];

                // Validate the untrusted archive name before the trusted
                // filter sees it. The filter deliberately owns final routing
                // and may select a sibling (for example, project settings),
                // so its canonical result is not constrained to destDir.
                resolveArchiveEntry(destDir, entryName);
                File destFile = filter.destFile(currentDir, entryName);
                if (destFile == null) {
                    throw new IOException("Extraction filter returned no destination for "
                            + entryName);
                }
                destFile = destFile.getCanonicalFile();
                destFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(destFile);
                dest = new BufferedOutputStream(fos, data.length);
                while ((count = zis.read(data, 0, data.length)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            source.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected File placeXMLFile(ZipEntry entry, File xmlDir, File resDir) throws IOException {
        boolean putInXMLDir = false;
        String name = entry.getName();
        if (name.contains("/")) {
            name = name.substring(0, name.lastIndexOf("/"));
            if (name.contains("/")) {
                name = name.substring(name.lastIndexOf("/"));
            }
            if (name.equalsIgnoreCase("xml")) {
                putInXMLDir = true;
            }
            name = entry.getName();
            name = name.substring(name.lastIndexOf("/") + 1, name.length());
        }
        if (putInXMLDir) {
            return resolveArchiveEntry(xmlDir, name);
        } else {
            return resolveArchiveEntry(resDir, entry.getName());
        }
    }

    /**
     * Resolves an archive entry beneath its intended extraction directory.
     * Entries that are absolute or escape through {@code ..} components are
     * rejected before any directory or file is created.
     *
     * @param destinationDirectory extraction root
     * @param entryName untrusted name read from the archive
     * @return canonical destination contained by {@code destinationDirectory}
     * @throws IOException if the entry escapes the extraction root
     */
    protected static File resolveArchiveEntry(File destinationDirectory,
            String entryName) throws IOException {
        if (new File(entryName).isAbsolute()) {
            throw new IOException("Archive entry is absolute: " + entryName);
        }
        return requireArchiveDestination(destinationDirectory,
                new File(destinationDirectory, entryName), entryName);
    }

    private static File requireArchiveDestination(File destinationDirectory,
            File destinationFile, String entryName) throws IOException {
        File canonicalDirectory = destinationDirectory.getCanonicalFile();
        File canonicalDestination = destinationFile.getCanonicalFile();
        String directoryPath = canonicalDirectory.getPath();
        String directoryPrefix = directoryPath.endsWith(File.separator)
                ? directoryPath : directoryPath + File.separator;
        if (!canonicalDestination.getPath().startsWith(directoryPrefix)) {
            throw new IOException("Archive entry escapes destination directory: "
                    + entryName);
        }
        return canonicalDestination;
    }

    public int executeProcess(ProcessBuilder pb) throws Exception {
        return executeProcess(pb, -1);
    }

    public boolean exec(File dir, String... varArgs) throws Exception {
        return exec(dir, -1, varArgs);
    }

    public String execStringWithThrow(boolean withThrow, File dir, String... varArgs) throws Exception {
        message.append("Executing: ");
        for (String s : varArgs) {
            message.append(s);
            message.append(" ");
        }

        if (is_windows && varArgs[0].indexOf('.') < 0) {
            varArgs[0] += ".exe";
        }

        StringBuilder response = new StringBuilder();
        ProcessBuilder p = new ProcessBuilder(varArgs).directory(dir);
        p.environment().putAll(defaultEnvironment);
        int val = executeProcess(p, -1, response);
        if (val != 0) {
            if (withThrow) {
                throw new IOException("Exec failed with response code "+val);
            }
            return "";
        }
        return response.toString();
    }

    public String execString(File dir, String... varArgs) throws Exception {
        return execStringWithThrow(false, dir, varArgs);
    }

    public boolean exec(File dir, Map<String, String> env, String... varArgs) throws Exception {
        return exec(dir, (File) null, -1, env, varArgs);
    }
    public boolean exec(File dir, Map<String, String> env, int timeout, String... varArgs) throws Exception {
        return exec(dir, (File) null, timeout, env, varArgs);
    }

    public boolean exec(File dir, int timeout, String... varArgs) throws Exception {
        return exec(dir, (File) null, timeout, varArgs);
    }

    protected boolean logToSystemOut;


    protected synchronized void log(String s) {
        log(s, true);
    }

    protected synchronized void debug(String s) {
        if (logger != null) {
            logger.debug(s);
            return;
        }
    }

    protected synchronized void warn(String s) {
        if (logger != null) {
            logger.warn(s);
            return;
        }
    }

    protected synchronized void warn(String s, Throwable ex) {
        if (logger != null) {
            logger.warn(s, ex);
            return;
        }
    }

    protected synchronized void error(String s, Throwable ex) {
        if (logger != null) {
            logger.error(s, ex);
            return;
        }
    }

    protected synchronized void log(String s, boolean ln) {
        if (logger != null) {
            logger.info(s);
            return;
        }
        if (logToSystemOut) {

            if (ln) {
                System.out.println(s);
            } else {
                System.out.print(s);
            }
        }
        message.append(s);
        if (ln) message.append('\n');


    }

    public boolean exec(File dir, File javaHome, int timeout, String... varArgs) throws Exception {
        return exec(dir, javaHome, timeout, (Map<String, String>) null, varArgs);
    }


    /**
     * Argument positions the next {@link #exec} must not print, or null.
     *
     * <p>An instance field rather than a parameter because the logging happens
     * in one place at the bottom of five overloads, and threading a set through
     * all of them to serve two call sites would be worse than this. Always set
     * and cleared around a single call -- see {@link #execRedacted}.</p>
     */
    private java.util.Set<Integer> redactedArgIndices;

    /**
     * Runs a command whose arguments at {@code secretIndices} are credentials.
     *
     * <p>{@code exec} appends every argument to the build message that is handed
     * back to the customer AND to the daemon's stdout, so a password on the
     * command line is retained twice over: once in an error log the customer
     * reads, once in host logging that outlives the build. This is the only way
     * to run {@code security import} or {@code notarytool submit} without doing
     * that.</p>
     *
     * <p>The process still receives the real argument -- this redacts the log,
     * not the command. A command line is visible to other processes on the host
     * while it runs, which is a smaller and much shorter-lived exposure than a
     * log file, and neither tool accepts the secret any other way.</p>
     */
    public boolean execRedacted(File dir, int timeout, int[] secretIndices, String... varArgs)
            throws Exception {
        java.util.Set<Integer> secrets = new java.util.HashSet<Integer>();
        if (secretIndices != null) {
            for (int i : secretIndices) {
                secrets.add(Integer.valueOf(i));
            }
        }
        redactedArgIndices = secrets;
        try {
            return exec(dir, timeout, varArgs);
        } finally {
            redactedArgIndices = null;
        }
    }

    public boolean exec(File dir, File javaHome, int timeout, Map<String, String> env, String... varArgs) throws Exception {
        log("Executing: ");
        message.append("Executing: ");
        StringBuilder logSb = new StringBuilder();
        for (int argIdx = 0; argIdx < varArgs.length; argIdx++) {
            // Redacted arguments are replaced, not omitted, so the command still
            // reads as the shape it was. Both sinks matter: `message` is returned
            // to the customer in the build log, and log() goes to the daemon's
            // own stdout, so a credential printed here outlives the build in
            // operational logging as well.
            String s = redactedArgIndices != null && redactedArgIndices.contains(Integer.valueOf(argIdx))
                    ? "***" : varArgs[argIdx];
            logSb.append(s + " ");
            message.append(s);
            message.append(" ");
        }
        log(logSb.toString());

        if (is_windows && varArgs[0].indexOf('.') < 0) {
            varArgs[0] += ".exe";
        }

        ProcessBuilder p = new ProcessBuilder(varArgs).directory(dir);
        p.environment().putAll(defaultEnvironment);
        if (env != null) {
            p.environment().putAll(env);
        }
        if (javaHome != null) {
            p.environment().put("JAVA_HOME", javaHome.getAbsolutePath());
            p.environment().put("java.home", javaHome.getAbsolutePath());
        }
        int val = executeProcess(p, timeout);
        return val == 0;
    }

    public int executeProcess(ProcessBuilder pb, final int timeout) throws Exception {
        return executeProcess(pb, timeout, message);
    }

    private boolean hasCloning(String str) {
        Pattern p = Pattern.compile("Cloning spec repo `.*` from `*`");
        Matcher m = p.matcher(str);

        return m.find();
    }

    private boolean hasGitFetch(String str) {
        Pattern p = Pattern.compile("git -C .* fetch origin --progress");
        Matcher m = p.matcher(str);

        return m.find();
    }



    private int executeProcess(ProcessBuilder pb, final int timeout, final StringBuilder outputMessage) throws Exception {
        log("Executing with timeout "+timeout);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final boolean[] destroyed = new boolean[]{false};
        final InputStream stream = p.getInputStream();
        final boolean[] running = new boolean[]{true};

        try {
            Thread reader = new Thread() {
                public void run() {
                    try {
                        byte[] buffer = new byte[8192];
                        int i = stream.read(buffer);
                        while (i > -1) {
                            String str = new String(buffer, 0, i, StandardCharsets.UTF_8);
                            log(str, false);
                            outputMessage.append(str);
                            i = stream.read(buffer);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        outputMessage.append("Exception on appending to log: " + ex);
                    }
                }
            };
            reader.start();
            if (timeout > -1) {
                new Thread() {
                    public void run() {
                        long t = System.currentTimeMillis();
                        while (running[0] && !destroyed[0]) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                            }
                            if (System.currentTimeMillis() - t > timeout) {
                                log("Timeout reached.  Destroying process");
                                destroyed[0] = true;

                                p.destroyForcibly();
                            }
                        }
                    }
                }.start();
            }
            int val = p.waitFor();
            // Stop the timeout watcher FIRST. The process has already exited, so
            // nothing it could do from here is useful -- and the join below can
            // hold us here for a while, during which a watcher still counting
            // would cross the deadline and flag a completed run as timed out.
            running[0] = false;
            boolean timedOut = destroyed[0];
            // waitFor returns as soon as the process exits, but the reader thread
            // may still be draining what is left in the pipe. Callers that inspect
            // outputMessage after this returns (the translator's out-of-memory
            // check, for one) would otherwise race the reader and read a partial
            // tail -- and the tail is exactly where a JVM prints its
            // OutOfMemoryError. Bounded so a wedged reader cannot hang the build;
            // the process has already exited, so the stream reaches EOF promptly.
            reader.join(30000);
            if (timedOut) {
                log("Process timed out");
                return 1;
            }
            log("Process return code is "+val);
            return val;
        } finally {
            running[0] = false;
        }
    }

    public void createFile(File f, byte[] b) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        out.write(b);
        out.close();
    }

    public File findFile(File rootFolder, String filename) {
        for (File f : rootFolder.listFiles()) {
            if (f.isDirectory()) {
                File c = findFile(f, filename);
                if (c != null) {
                    return c;
                }
            }
            if (f.getName().equalsIgnoreCase(filename)) {
                return f;
            }
        }
        return null;
    }

    public File findFileType(File rootFolder, String fileExtension) {
        fileExtension = fileExtension.toLowerCase();
        for (File f : rootFolder.listFiles()) {
            if (f.getName().toLowerCase().endsWith(fileExtension)) {
                return f;
            }


        }
        for (File f : rootFolder.listFiles()) {
            if (f.isDirectory()) {
                File c = findFileType(f, fileExtension);
                if (c != null) {
                    return c;
                }
            }

        }
        return null;
    }

    public File findFileTypeNoRecursion(File rootFolder, String fileExtension) {
        return findFileTypeNoRecursion(rootFolder, fileExtension, false);
    }
    public File findFileTypeNoRecursion(File rootFolder, String fileExtension, boolean allowDirectories) {
        fileExtension = fileExtension.toLowerCase();
        for (File f : rootFolder.listFiles()) {
            if (!allowDirectories && f.isDirectory()) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(fileExtension)) {
                return f;
            }
        }
        return null;
    }

    public void createFile(File f, InputStream i) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        byte[] buffer = new byte[8192];
        int size = i.read(buffer);
        while (size > -1) {
            out.write(buffer, 0, size);
            size = i.read(buffer);
        }
        out.close();
        i.close();
    }

    public static void zipDir(String zipFileName, String dir) throws Exception {
        File dirObj = new File(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));

        addDir(dirObj, dirObj, out);
        out.close();
    }

    public static void zipDir(String zipFileName, String dir, String... exclude) throws Exception {
        File dirObj = new File(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));

        addDir(dirObj, dirObj, out, exclude);
        out.close();
    }

    static void addDir(File baseDir, File dirObj, ZipOutputStream out) throws IOException {
        addDir(baseDir, dirObj, out, null);
    }

    static void addDir(File baseDir, File dirObj, ZipOutputStream out, String... exclude) throws IOException {
        File[] files = dirObj.listFiles();
        if(files == null) {
            return;
        }
        byte[] tmpBuf = new byte[8192];

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                boolean found = isFound(exclude, files, i);
                if (!found) {
                    List<String> newExcludes = new ArrayList<>();
                    if (exclude != null) {
                        for (String ex : exclude) {
                            if (ex.contains("/")) {
                                newExcludes.add(ex.substring(ex.indexOf("/")+1));
                            }
                        }
                    }
                    addDir(baseDir, files[i], out, newExcludes.toArray(new String[newExcludes.size()]));
                }
                continue;
            }
            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());

            out.putNextEntry(new ZipEntry(files[i].getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1).replace('\\', '/')));
            int len;
            while ((len = in.read(tmpBuf)) >= 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }

    private static boolean isFound(String[] exclude, File[] files, int i) {
        if (exclude != null) {
            List<String> excludeNames = new ArrayList<>();

            for (String ex : exclude) {
                if (!ex.contains("/")) {
                    // We only check for excludes at this level
                    excludeNames.add(ex);
                }
            }
            for (String ex : excludeNames) {
                if (files[i].getName().equalsIgnoreCase(ex)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected BufferedImage getScaledUnevenInstance(BufferedImage img,
                                                    int targetWidth,
                                                    int targetHeight) {
        int ar;
        int imageX, imageY;
        Rectangle rec1, rec2;
        if (targetWidth < targetHeight) {
            ar = targetWidth;
            imageX = 0;
            imageY = targetHeight / 2 - targetWidth / 2;
            rec1 = new Rectangle(0, 0, targetWidth, imageY);
            rec2 = new Rectangle(0, targetHeight, targetWidth, imageY);
        } else {
            ar = targetHeight;
            imageY = 0;
            imageX = targetWidth / 2 - targetHeight / 2;
            rec1 = new Rectangle(0, 0, imageX, targetHeight);
            rec2 = new Rectangle(imageX + targetWidth, 0, imageX, targetHeight);
        }
        BufferedImage bi = getScaledInstance(img, ar, ar);
        BufferedImage b2 = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        int array = bi.getRGB(0, 0);
        Graphics2D g2d = b2.createGraphics();
        if ((array & 0xff000000) != 0) {
            g2d.setColor(new Color(array, true));
            g2d.fill(rec2);
            g2d.fill(rec1);
        }
        g2d.drawImage(bi, imageX, imageY, null);
        g2d.dispose();

        return b2;
    }

    protected BufferedImage getScaledInstance(BufferedImage img,
                                              int targetWidth,
                                              int targetHeight) {
        BufferedImage ret = img;
        int w, h;
        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        w = img.getWidth();
        h = img.getHeight();

        if (w < targetWidth && h < targetHeight) {
            BufferedImage b = new BufferedImage(targetWidth, targetHeight, img.getType());
            Graphics2D g2d = b.createGraphics();
            g2d.drawImage(img, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
            return b;
        }

        do {
            if (w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }


    public File getResourceAsFile(String res, String extension) throws IOException {

        File tmp = File.createTempFile("temp", extension);
        tmp.deleteOnExit();
        InputStream is = getResourceAsStream(res);
        if (is == null) {
            throw new IOException("Resource not found: "+res);
        }
        FileOutputStream o = new FileOutputStream(tmp);
        copy(is, o);
        return tmp;
    }

    /**
     * Gets a potentially versioned resource
     */
    public InputStream getResourceAsStream(String res) {


        return Executor.class.getResourceAsStream(res);
    }

    protected boolean isUnitTestMode() {
        return unitTestMode;
    }

    public void setUnitTestMode(boolean unitTestMode) {
        this.unitTestMode = unitTestMode;
    }

    protected void generateUnitTestFiles(BuildRequest req, File stubDir) throws IOException {
        if (unitTestMode) {
            String actualMainClass = req.getMainClass();
            req.putArgument("j2me.obfuscation", "false");
            req.setMainClass("CodenameOneUnitTestExecutor");
            String testLogger = req.getArg("build.testReporter", null);
            String testReporter = "";
            if (testLogger != null) {
                testReporter = "        TestReporting.setInstance(new " + testLogger + "());\n";
            }
            File outputFile = new File(stubDir, req.getPackageName().replace('.', File.separatorChar) + File.separatorChar + "CodenameOneUnitTestExecutor.java");
            outputFile.getParentFile().mkdirs();
            FileOutputStream fo = new FileOutputStream(outputFile);
            fo.write(("package " + req.getPackageName() + ";\n\n"
                    + "import com.codename1.testing.DeviceRunner;\n"
                    + "import com.codename1.testing.TestReporting;\n\n"
                    + "public class CodenameOneUnitTestExecutor extends DeviceRunner {\n"
                    + "    private " + actualMainClass + " instance;\n"
                    + "    private Object context;\n\n"
                    + "    protected void startApplicationInstance() {\n"
                    + "        instance = new " + actualMainClass + "();\n"
                    + "        instance.init(context);\n"
                    + "        instance.start();\n"
                    + "    }\n\n\n"
                    + "    protected void stopApplicationInstance() {\n"
                    + "        instance.stop();\n"
                    + "        instance.destroy();\n"
                    + "        instance = null;\n"
                    + "    }\n\n\n"
                    + "    public void init(Object ctx) {\n"
                    + "        context = ctx;\n"
                    + testReporter
                    + "    }\n\n\n"
                    + "    public void start() {\n"
                    + "        runTests();\n"
                    + "    }\n\n\n"
                    + "    public void stop() {\n"
                    + "    }\n\n\n"
                    + "    public void destroy() {\n"
                    + "    }\n\n\n"
                    + "}\n").getBytes(StandardCharsets.UTF_8));
            fo.close();
        }
    }

    public String decodeFunction() {

        debug("Using xorDecode function");
        return "    public String d(String s) {\n"
                + "        return com.codename1.io.Util.xorDecode(s);\n"
                + "    }\n\n";
    }

    public String xorEncode(String s) {

        if(s == null) {
            return null;
        }
        byte[] dat = s.getBytes(StandardCharsets.UTF_8);
        for(int iter = 0 ; iter < dat.length ; iter++) {
            dat[iter] = (byte)(dat[iter] ^ (iter % 254 + 1));
        }
        return Base64.encodeNoNewline(dat);
    }

    /**
     * The platform id this builder targets, for the hardening engine ({@code ios}, {@code and},
     * {@code javascript}, {@code win}, {@code linux}, {@code mac}, ...). Subclasses override.
     */
    protected String hardeningPlatform(BuildRequest request) {
        return "unknown";
    }

    /**
     * Extra fully-qualified class names to keep from renaming, beyond the main class, for a slice whose
     * runtime resolves a class by its ORIGINAL name in generated native code (which the input-jar
     * scanner cannot discover). Empty by default; the iOS builder adds the watch lifecycle entry.
     */
    protected java.util.List<String> extraKeepClasses(BuildRequest request) {
        return java.util.Collections.emptyList();
    }

    /**
     * Java source that stamps the hardening runtime properties ({@code cn1.mappingId},
     * {@code cn1.hardened}, {@code cn1.hardenLevel}) into {@code Display}, so every port's stub can
     * emit them the same way. These back {@code Hardening.isHardened()} and the crash report's
     * mapping id / level. The values are controlled build outputs (a hex id and a fixed level),
     * so string concatenation into the stub is safe.
     */
    protected String hardeningRuntimeProperties(BuildRequest request) {
        return "        Display.getInstance().setProperty(\"cn1.mappingId\", \"" + resolveMappingId(request) + "\");\n"
                + "        Display.getInstance().setProperty(\"cn1.hardened\", \"" + request.getArg("cn1.hardened", "false") + "\");\n"
                + "        Display.getInstance().setProperty(\"cn1.hardenLevel\", \"" + request.getArg("cn1.hardenLevel", "off") + "\");\n";
    }

    /**
     * Whether the hardening engine should rename for this platform. Android returns false: R8
     * remains the sole renamer there, and the engine only encrypts strings and exports keep rules.
     */
    protected boolean hardeningRenameSupported() {
        return true;
    }

    /**
     * Library jars the hardening engine passes to ProGuard so it can see inherited framework APIs
     * and not rename an application method that overrides a framework method (which would break
     * dispatch at runtime). The caller supplies the compile/platform classpath in the
     * {@code cn1.hardening.libraryJars} request argument (path-separated); subclasses may add more.
     */
    /**
     * True for the ParparVM-to-C targets (iOS/mac/watch/tv/win/linux), whose app links against the
     * {@code parparvm-java-api.jar} runtime. A compile-time literal there is a constant-pool object that
     * ParparVM never interns, so an encrypted app copy of the same value would not be reference-equal to
     * it; the runtime's literals are therefore excluded from encryption on these targets.
     */
    protected boolean isParparVMCPlatform(String platform) {
        return "ios".equals(platform) || "mac".equals(platform) || "watch".equals(platform)
                || "tv".equals(platform) || "win".equals(platform) || "linux".equals(platform);
    }

    /**
     * Whether this builder's artifact links against the un-interned ParparVM Java runtime, so that
     * runtime's literals must be excluded from encryption to preserve reference equality. Defaults to the
     * ParparVM-C platform test, but the platform tag alone is insufficient for the JavaScript port: the
     * ParparVM-to-JS builder reports {@code javascript} yet unpacks {@code parparvm-java-api.jar} into the
     * translated app exactly like the native ParparVM-C targets, so {@code JavaScriptBuilder} overrides
     * this to true. (A TeaVM-based JS build would not link that runtime, but the plugin's local JS builder
     * is ParparVM-only.)
     */
    protected boolean stagesParparVMRuntime(BuildRequest request) {
        return isParparVMCPlatform(hardeningPlatform(request));
    }

    /**
     * The platform tags this one build ships from the SAME hardened jar. The default builder emits a
     * single slice ({@link #hardeningPlatform(BuildRequest)}); the Apple builder widens it to the iOS app
     * plus any native-Mac, watch or tv slice. A shared jar cannot be hardened per-slice, so its
     * {@code harden.<slice>.enabled} opt-outs are combined: hardening runs unless EVERY shipped slice is
     * opted out (see {@link #writeHardeningConfig}). Kept consistent with the daemon builder.
     */
    protected java.util.List<String> effectiveHardeningPlatforms(BuildRequest request) {
        return java.util.Collections.singletonList(hardeningPlatform(request));
    }

    /**
     * True when a {@code harden.*} boolean reads as disabled, matching the engine's tri-state parsing:
     * {@code false}, {@code 0} and {@code off} all mean off, so a per-slice opt-out is recognized
     * consistently rather than only as the literal {@code false}.
     */
    protected static boolean hardenDisabled(String value) {
        if (value == null) {
            return false;
        }
        String t = value.trim().toLowerCase();
        return "false".equals(t) || "0".equals(t) || "off".equals(t);
    }

    /**
     * True when at least one of the shipped {@code slices} still wants hardening, i.e. NOT every slice
     * opted out via {@code harden.<slice>.enabled}. A shared jar is hardened as a whole, so the combined
     * decision is this OR: hardening runs unless every slice is opted out.
     */
    static boolean anySliceHardeningEnabled(java.util.List<String> slices, BuildRequest request) {
        for (String slice : slices) {
            if (!hardenDisabled(request.getArg("harden." + slice + ".enabled", "true"))) {
                return true;
            }
        }
        return false;
    }

    protected java.util.List<File> hardeningLibraryJars(BuildRequest request) {
        java.util.List<File> jars = new java.util.ArrayList<File>();
        // Always include the Codename One framework jar: every builder receives it, and it carries
        // the framework superclasses ProGuard must see so it never renames an application override
        // (e.g. a custom Component.paint) apart from the fixed framework method -- which would break
        // virtual dispatch. cn1.hardening.libraryJars (below) is only set on the CN1BuildMojo entry
        // and is absent when hardening runs through buildNoException, so it can't be relied on alone.
        if (codenameOneJar != null && codenameOneJar.exists()) {
            jars.add(codenameOneJar);
        }
        // On a ParparVM-C target the app also links against the ParparVM Java runtime
        // (parparvm-java-api.jar -- java.lang.Boolean, java.lang.String, ...), which the builder stages
        // later and never hardens. Its literals must reach the engine's library-literal exclusion scan,
        // or an app value like "true" (encrypted then interned) would compare != to a runtime-returned
        // copy such as Boolean.toString() -- a constant-pool literal ParparVM never interns -- breaking a
        // reference comparison that held before hardening. It doubles as a -libraryjars entry for ProGuard.
        if (stagesParparVMRuntime(request)) {
            try {
                File runtime = getResourceAsFile("/parparvm-java-api.jar", ".jar");
                if (runtime != null && runtime.exists() && !jars.contains(runtime)) {
                    jars.add(runtime);
                }
            } catch (IOException ex) {
                // Best-effort: without the runtime jar the scan simply misses its literals (a rare == edge).
            }
        }
        String raw = request.getArg("cn1.hardening.libraryJars", "");
        if (raw == null || raw.length() == 0) {
            // Fallback: the maven plugin publishes the compile classpath here (a single injection
            // point rather than threading it through every local-build request).
            raw = System.getProperty("cn1.hardening.libraryJars", "");
        }
        if (raw != null && raw.length() > 0) {
            for (String p : raw.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (p != null && p.trim().length() > 0) {
                    File f = new File(p.trim());
                    if (f.exists() && !jars.contains(f)) {
                        jars.add(f);
                    }
                }
            }
        }
        return jars;
    }

    private boolean hardeningRanThisBuild;
    private File lastHardeningMapping;
    private String lastHardeningMappingId = "";
    private String lastHardeningR8Keep = "";

    /** The cross-platform obfuscation mapping produced by the last {@link #hardenSourceJar} call, or null. */
    public File getLastHardeningMapping() {
        return lastHardeningMapping;
    }

    /**
     * The keep rules the engine derived from the input jar (reflective {@code Class.forName}
     * targets, service providers, name-bound property objects, the app's {@code harden.keep}),
     * for a downstream renamer the engine does not drive itself -- specifically R8 on Android.
     * Empty when hardening did not run or emitted no rules.
     */
    public String getLastHardeningR8Keep() {
        return lastHardeningR8Keep;
    }

    /** The mapping id produced by the last {@link #hardenSourceJar} call, or empty. */
    public String getLastHardeningMappingId() {
        return lastHardeningMappingId;
    }

    /**
     * Runs the build with hardening applied first: {@code build(hardenSourceJar(sourceZip, request),
     * request)}. Callers that bypass {@link #buildNoException} (the local build paths in the maven
     * plugin) invoke this instead of {@code build} directly, so hardening reaches every path.
     */
    public boolean runBuild(File sourceZip, BuildRequest request) throws BuildException {
        return build(hardenSourceJar(sourceZip, request), request);
    }

    /**
     * Reads a {@code harden.*} boolean argument with the same tri-state rules the engine's
     * {@code HardeningConfig.boolTri} applies: {@code true/1/2/3/on} are true, {@code false/0/off}
     * are false, and anything else (including unset/blank) falls back to {@code def}. Builders must
     * use this rather than a bare {@code "false".equals(...)} so a documented alias like
     * {@code harden.rename=off} is not silently misread.
     */
    protected boolean hardenBoolArg(BuildRequest request, String key, boolean def) {
        String v = request.getArg(key, null);
        if (v == null) {
            return def;
        }
        String t = v.trim().toLowerCase();
        if (t.length() == 0) {
            return def;
        }
        if ("true".equals(t) || "1".equals(t) || "2".equals(t) || "3".equals(t) || "on".equals(t)) {
            return true;
        }
        if ("false".equals(t) || "0".equals(t) || "off".equals(t)) {
            return false;
        }
        return def;
    }

    /**
     * Applies the app-hardening transform to the merged application jar and returns the jar the
     * build should proceed with. When hardening is not requested (or already applied, or declined
     * by the engine) the input jar is returned unchanged; when the engine reports the build is not
     * entitled, the build fails. The engine runs as a forked process so it is single-sourced across
     * the plugin and the cloud daemon and never shares a classloader with the caller.
     */
    public File hardenSourceJar(File sourceZip, BuildRequest request) throws BuildException {
        // Idempotence FIRST, via a non-forgeable per-instance flag (not an input-jar marker: a
        // spurious META-INF/CN1-HARDENED resource must not skip the transform). On a second call in
        // this build -- a nested/delegated invocation -- the verified cn1.hardened / cn1.hardenLevel /
        // cn1.mappingId from the first run are already in the request, so return WITHOUT touching them.
        if (hardeningRanThisBuild) {
            log("cn1-hardening: already hardened in this build; skipping");
            return sourceZip;
        }
        // cn1.hardened / cn1.hardenLevel / cn1.mappingId are engine OUTPUTS, never inputs. Clear any
        // supplied values so the stubs never stamp a hardened state the engine didn't actually
        // produce; they are set again below only from a verified hardening run.
        request.putArgument("cn1.hardened", "false");
        request.putArgument("cn1.hardenLevel", "off");
        request.putArgument("cn1.mappingId", "");
        String level = request.getArg("harden.level", "off");
        if (level == null || level.trim().length() == 0 || "off".equalsIgnoreCase(level.trim())) {
            return sourceZip;
        }
        // The client-side pre-flight (Check 1) sets this when a local/source target opted into an
        // unhardened build via harden.allowUnhardenedLocalBuild; honor it as a single point. Prefer the
        // per-build request arg (the Mojo injects its instance decision there) over the process-wide
        // System property, which is racy under concurrent module builds -- another platform's build could
        // clear it between this build's pre-flight and this read. The System property stays as a fallback
        // for any caller that has not migrated to the request arg.
        if ("true".equals(request.getArg("cn1.harden.forceOff", null))
                || "true".equals(System.getProperty("cn1.harden.forceOff"))) {
            log("cn1-hardening: forced off for this local build; building unhardened");
            return sourceZip;
        }
        try {
            File engine = getResourceAsFile("/cn1-hardening.jar", ".jar");
            File workDir = new File(sourceZip.getParentFile(), "cn1-harden-work");
            workDir.mkdirs();
            File hardened = new File(workDir, "hardened.jar");
            File mapping = new File(workDir, "cn1-mapping.txt");
            File report = new File(workDir, "cn1-harden-report.json");
            File r8Keep = new File(workDir, "cn1-r8-keep.pro");
            File config = new File(workDir, "config.properties");
            writeHardeningConfig(config, request);

            String javaBin = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
            java.util.List<String> cmd = new java.util.ArrayList<String>();
            cmd.add(javaBin);
            cmd.add("-jar");
            cmd.add(engine.getAbsolutePath());
            cmd.add("harden");
            cmd.add("--in");
            cmd.add(sourceZip.getAbsolutePath());
            cmd.add("--out");
            cmd.add(hardened.getAbsolutePath());
            cmd.add("--mapping");
            cmd.add(mapping.getAbsolutePath());
            cmd.add("--report");
            cmd.add(report.getAbsolutePath());
            cmd.add("--r8keep");
            cmd.add(r8Keep.getAbsolutePath());
            cmd.add("--config");
            cmd.add(config.getAbsolutePath());

            int exit = runForked(cmd, workDir);
            if (exit == 0) {
                lastHardeningMapping = mapping.isFile() ? mapping : null;
                lastHardeningMappingId = readMappingId(mapping);
                lastHardeningR8Keep = r8Keep.isFile() ? readFileToString(r8Keep) : "";
                // On Android the engine does not rename (R8 is the sole renamer), so its mapping --
                // and thus its mapping id -- is empty. Derive a per-build id regardless of
                // harden.rename: R8 still renames whenever minification is on (independent of the
                // engine's rename), and when minification is off an identity map is uploaded -- both
                // need a non-empty id to key the mapping the app carries. An empty id would leave
                // hardened Android crashes unretraceable.
                if ((lastHardeningMappingId == null || lastHardeningMappingId.length() == 0)
                        && !hardeningRenameSupported()) {
                    lastHardeningMappingId = downstreamMappingId(request, hardened);
                }
                // Propagate the mapping id / hardened flag / level into the request BEFORE the
                // builder generates its stubs, so the stubs stamp them as runtime properties
                // (Hardening.isHardened(), the crash report's mappingId/hardenLevel).
                request.putArgument("cn1.mappingId", lastHardeningMappingId);
                request.putArgument("cn1.hardened", "true");
                request.putArgument("cn1.hardenLevel", level.trim().toLowerCase());
                hardeningRanThisBuild = true;
                log("cn1-hardening: applied, mappingId=" + lastHardeningMappingId);
                return hardened;
            }
            if (exit == 4) {
                throw new BuildException("App hardening is an Enterprise feature and this build "
                        + "is not entitled. Upgrade at https://www.codenameone.com/pricing.html "
                        + "or set codename1.arg.harden.level=off.");
            }
            if (exit == 3) {
                log("cn1-hardening: declined by engine; building unhardened");
                return sourceZip;
            }
            throw new BuildException("App hardening failed (engine exit code " + exit
                    + "). This build has been stopped rather than shipping a partially hardened binary.");
        } catch (BuildException be) {
            throw be;
        } catch (Exception e) {
            throw new BuildException("App hardening failed: " + e.getMessage());
        }
    }

    private void writeHardeningConfig(File config, BuildRequest request) throws IOException {
        java.util.Properties p = new java.util.Properties();
        for (String key : request.getArgs()) {
            if (key.startsWith("harden.")) {
                p.setProperty(key, request.getArg(key, ""));
            }
        }
        p.setProperty("cn1.platform", hardeningPlatform(request));
        // A build can ship several slices from this ONE shared hardened jar (the iOS app plus a
        // native-Mac/watch/tv slice). The engine reads a single harden.<cn1.platform>.enabled, so a
        // combined build would otherwise honor only one slice's opt-out and silently ignore the others'.
        // Coalesce them: hardening runs unless EVERY shipped slice is opted out. A shared jar cannot be
        // hardened one way for one slice and another for the other, so this is the honest combination.
        p.setProperty("harden." + hardeningPlatform(request) + ".enabled",
                Boolean.toString(anySliceHardeningEnabled(effectiveHardeningPlatforms(request), request)));
        // The keep rule must name the FULLY QUALIFIED main class: getMainClass() is the simple name
        // (the stubs combine it with getPackageName()), so passing it bare would keep a default-package
        // class and let ProGuard rename the real application class out from under the generated stub.
        p.setProperty("cn1.mainClass", fullyQualifiedMainClass(request));
        // Keep any class a slice resolves by its original name in generated native code (e.g. the
        // watch lifecycle entry embedded in cn1_watch_runtime_start), which the input-jar scanner
        // cannot see. Appended as ProGuard -keep rules to harden.keep, the caller-keep channel.
        java.util.List<String> extraKeeps = extraKeepClasses(request);
        if (extraKeeps != null && !extraKeeps.isEmpty()) {
            StringBuilder kb = new StringBuilder(p.getProperty("harden.keep", ""));
            for (String cls : extraKeeps) {
                if (cls == null || cls.trim().length() == 0) {
                    continue;
                }
                if (kb.length() > 0) {
                    kb.append('\n');
                }
                kb.append("-keep class ").append(cls.trim()).append(" { *; }");
            }
            p.setProperty("harden.keep", kb.toString());
        }
        p.setProperty("cn1.renameSupported", Boolean.toString(hardeningRenameSupported()));
        // Local plugin builds are ungated: the engine is open source and a developer must be able
        // to reproduce a cloud failure locally. The cloud daemon sets this from the account tier.
        p.setProperty("cn1.entitled", request.getArg("cn1.entitled", "true"));
        p.setProperty("cn1.buildKey", resolveBuildKey(request));
        StringBuilder libs = new StringBuilder();
        for (File lib : hardeningLibraryJars(request)) {
            if (lib != null && lib.exists()) {
                if (libs.length() > 0) {
                    libs.append(File.pathSeparator);
                }
                libs.append(lib.getAbsolutePath());
            }
        }
        p.setProperty("cn1.libraryJars", libs.toString());
        FileOutputStream fo = new FileOutputStream(config);
        try {
            p.store(fo, "Codename One hardening configuration");
        } finally {
            fo.close();
        }
    }

    /** The fully qualified main class: {@code getPackageName().getMainClass()} unless already qualified. */
    private String fullyQualifiedMainClass(BuildRequest request) {
        String main = request.getMainClass();
        if (main == null || main.trim().length() == 0) {
            return "";
        }
        main = main.trim();
        if (main.indexOf('.') >= 0) {
            return main;
        }
        String pkg = request.getPackageName();
        if (pkg == null || pkg.trim().length() == 0) {
            return main;
        }
        return pkg.trim() + "." + main;
    }

    private int runForked(java.util.List<String> cmd, File workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            log(line);
        }
        return proc.waitFor();
    }

    private boolean isAlreadyHardened(File jar) {
        if (jar == null || !jar.isFile()) {
            return false;
        }
        java.util.zip.ZipFile zf = null;
        try {
            zf = new java.util.zip.ZipFile(jar);
            return zf.getEntry("META-INF/CN1-HARDENED") != null;
        } catch (IOException e) {
            return false;
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException ignore) {
                    // best effort
                }
            }
        }
    }

    private String readMappingId(File mapping) {
        if (mapping == null || !mapping.isFile()) {
            return "";
        }
        java.io.BufferedReader r = null;
        try {
            r = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new FileInputStream(mapping), StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("# mappingId:")) {
                    return line.substring("# mappingId:".length()).trim();
                }
            }
        } catch (IOException e) {
            return "";
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignore) {
                    // best effort
                }
            }
        }
        return "";
    }

    /**
     * The per-build key the cloud stamps into the app and that crash reports echo back so the
     * server can match a report to its uploaded symbol bundle. The cloud passes it in the
     * {@code cn1.buildKey} argument; when it is absent (local builds) we fall back to the
     * literal {@code LOCAL_BUILD}. Historically Android hard-coded {@code "LOCAL_BUILD"} as the
     * <em>encoded</em> constant and then ran it through {@code d()} / {@code Util.xorDecode},
     * which is not valid Base64 and decoded to junk -- always encode through this pair.
     */
    public String resolveBuildKey(BuildRequest request) {
        String bk = request.getArg("cn1.buildKey", null);
        if(bk == null || bk.length() == 0) {
            bk = "LOCAL_BUILD";
        }
        return bk;
    }

    /**
     * The {@link #resolveBuildKey(BuildRequest) build key} in the {@code d()}-decodable encoded
     * form the generated stubs embed, i.e. what a stub assigns to its {@code BUILD_KEY} constant
     * before stamping {@code Display.setProperty("build_key", d(BUILD_KEY))} at runtime.
     */
    public String buildKeyEncoded(BuildRequest request) {
        return xorEncode(resolveBuildKey(request));
    }

    /**
     * Identifier of the obfuscation mapping this build was hardened with, stamped alongside the
     * build key so a crash report can be tied to the exact mapping even if a rebuilt app reused
     * the build key. Empty for unhardened builds. Passed by the cloud in {@code cn1.mappingId}.
     */
    public String resolveMappingId(BuildRequest request) {
        return request.getArg("cn1.mappingId", "");
    }

    /**
     * A stable mapping id for a build whose rename is produced by a downstream tool (R8 on Android)
     * rather than the engine, so the engine's own mapping id is empty. Derived from the build key
     * and platform as a SHA-256 hex string, matching the engine mapping id's format, so a hardened
     * crash report can be tied to the R8 mapping.txt uploaded for this build+platform.
     */
    // The id is necessarily fixed BEFORE R8 runs -- the app carries it as a compile-time constant,
    // and R8's own mapping.txt does not exist until after the app is compiled, so the id cannot be a
    // hash of that mapping. It is a per-BUILD nonce (build key + platform + hardened jar bytes + a
    // unique run stamp), so every build -- even a byte-identical rebuild, or two builds that reuse a
    // build key but produce different R8 mappings because android.proguardKeep or the R8 version
    // changed -- gets a distinct id. The daemon uploads that build's R8 mapping.txt keyed by THIS id,
    // so a crash report's id selects the exact mapping that build shipped and no upload overwrites
    // another build's mapping in the same slot.
    private String downstreamMappingId(BuildRequest request, File hardenedJar) {
        String seed = resolveBuildKey(request) + ":" + hardeningPlatform(request)
                + ":" + System.nanoTime() + ":" + System.identityHashCode(hardenedJar);
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(seed.getBytes("UTF-8"));
            // Fold in the hardened application jar's bytes so two builds that reuse a build key but
            // differ in code get distinct ids -- resolveMappingId promises to distinguish a rebuilt
            // app that reused a build key. A byte-identical rebuild keeps the same id, matching its
            // identical R8 mapping.
            if (hardenedJar != null && hardenedJar.isFile()) {
                java.io.InputStream in = new java.io.FileInputStream(hardenedJar);
                try {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        md.update(buf, 0, n);
                    }
                } finally {
                    in.close();
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            // No SHA-256 (impossible on a supported JDK) -- fall back to a non-empty encoded key.
            return buildKeyEncoded(request);
        }
    }

    /**
     * Loads global local builder properties from user's home directory.
     */
    protected Properties getLocalBuilderProperties() {
        if (localBuilderProperties == null) {
            String sep = File.separator;
            File propertiesFile = new File(
                    System.getProperty("user.home") + sep + ".codenameone" + sep + "local.properties"
            );
            localBuilderProperties = new Properties();
            if (!propertiesFile.exists()) {
                return localBuilderProperties;

            }
            try {
                try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                    localBuilderProperties.load(fis);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to load local properties", ex);
            }
        }

        return localBuilderProperties;

    }

    /// Returns true when the project's `jar-with-dependencies` (the
    /// `sourceZip` passed to `build(...)`) contains the build-time
    /// generated `com.codename1.router.generated.Routes` class.
    protected static boolean projectHasRouteDispatcher(File sourceZip) {
        if (sourceZip == null || !sourceZip.isFile()) {
            return false;
        }
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(sourceZip)) {
            return zf.getEntry("com/codename1/router/generated/Routes.class") != null;
        } catch (IOException e) {
            return false;
        }
    }

    /// Stub-source fragment to splice into a generated application stub
    /// right before `Display.init(...)` to install the build-time
    /// generated `@Route` dispatcher. Empty when the project ships no
    /// Routes class, so legacy apps without the annotation Mojo still
    /// produce a clean stub. The dispatcher's no-arg constructor self-
    /// registers via `Navigation#setDispatcher` -- direct symbol
    /// reference, not `Class.forName`, so ParparVM / R8 obfuscation
    /// rewrites the call site and the generated class together and the
    /// binding survives in shipped builds. `indent` is the leading
    /// whitespace that matches the surrounding stub source.
    protected static String routeDispatcherInstallSource(File sourceZip, String indent) {
        if (!projectHasRouteDispatcher(sourceZip)) {
            return "";
        }
        return indent + "new com.codename1.router.generated.Routes();\n";
    }

    /// Stub-source fragment to splice into a generated application stub
    /// right before `Display.init(...)` to install the build-time-generated
    /// JSON / XML mapper index, the component binder index, and the SQLite
    /// dao index -- but only when the project actually uses each feature.
    ///
    /// The annotation processor emits `cn1app.MapperBootstrap` /
    /// `BinderBootstrap` / `DaoBootstrap` only when there are `@Mapped` /
    /// `@Bindable` / `@Entity` classes to register. Each bootstrap's
    /// constructor references every generated per-class mapper / binder /
    /// dao by direct symbol (`new com.example.UserCn1Mapper();` etc.), so
    /// ParparVM iOS / R8 Android rename the call sites and the generated
    /// classes together.
    ///
    /// We probe the project zip for each bootstrap and emit the
    /// instantiation only when the class is present, so a project that
    /// uses only `@Mapped` (no `@Bindable`, no `@Entity`) gets just the
    /// mapper bootstrap line and nothing else. cn1-core does not ship a
    /// stub: an absent feature leaves the registries empty.
    protected static String annotationFrameworksInstallSource(File sourceZip, String indent) {
        StringBuilder sb = new StringBuilder();
        if (projectHasBootstrap(sourceZip, "cn1app/MapperBootstrap.class")) {
            sb.append(indent).append("new cn1app.MapperBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/BinderBootstrap.class")) {
            sb.append(indent).append("new cn1app.BinderBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/DaoBootstrap.class")) {
            sb.append(indent).append("new cn1app.DaoBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/RestClientBootstrap.class")) {
            sb.append(indent).append("new cn1app.RestClientBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/ProtoBootstrap.class")) {
            sb.append(indent).append("new cn1app.ProtoBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/GrpcClientBootstrap.class")) {
            sb.append(indent).append("new cn1app.GrpcClientBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/GraphQLClientBootstrap.class")) {
            sb.append(indent).append("new cn1app.GraphQLClientBootstrap();\n");
        }
        if (projectHasBootstrap(sourceZip, "cn1app/IntentBootstrap.class")) {
            sb.append(indent).append("new cn1app.IntentBootstrap();\n");
        }
        return sb.toString();
    }

    /// Returns true when `sourceZip` (the project's
    /// `jar-with-dependencies`) contains `entryPath`. Used to gate the
    /// per-feature bootstrap install lines so projects that don't use
    /// every annotation framework still produce a clean stub.
    protected static boolean projectHasBootstrap(File sourceZip, String entryPath) {
        if (sourceZip == null || !sourceZip.isFile()) {
            return false;
        }
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(sourceZip)) {
            return zf.getEntry(entryPath) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
