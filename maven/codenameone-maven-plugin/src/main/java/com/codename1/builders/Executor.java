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
    // Native Mac (ParparVM Catalyst slice, rides the iOS pipeline with
    // macNative.enabled). The cloud target name; "mac-source" is the local project.
    public static final String BUILD_TARGET_MAC_NATIVE = "mac-os-x-native";
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
    /// `Database.setLegacyBehavior` is a plain static and needs no display, so a launcher that
    /// only hands control to the bootstrap can still apply the switch. Referencing `Database`
    /// pins the class, which is why this is emitted only for an application that already uses it.
    ///
    /// #### Parameters
    ///
    /// - `request`: the build request
    /// - `usesDatabase`: whether the scanned classes reference `com.codename1.db`
    ///
    /// #### Returns
    ///
    /// the source line to insert, or an empty string
    protected String databaseLegacyStubCall(BuildRequest request, boolean usesDatabase) {
        if (!isDatabaseLegacyMode(request, usesDatabase)) {
            return "";
        }
        return "        com.codename1.db.Database.setLegacyBehavior(true);\n";
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
                        File s = sourceZip;
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

    public boolean exec(File dir, File javaHome, int timeout, Map<String, String> env, String... varArgs) throws Exception {
        log("Executing: ");
        message.append("Executing: ");
        StringBuilder logSb = new StringBuilder();
        for (String s : varArgs) {
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
