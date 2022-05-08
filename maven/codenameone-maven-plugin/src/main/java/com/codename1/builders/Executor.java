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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
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
    private String buildTarget;

    private static boolean disableDelete;
    public static final boolean is_windows = File.separatorChar == '\\';
    protected File tmpDir;
    StringBuilder message = new StringBuilder();
    private String buildId;
    private boolean canceled;
    private Class[] nativeInterfaces;
    private String buildKey;
    private boolean unitTestMode;
    private String platform;
    static boolean IS_MAC;
    protected final Map<String,String> defaultEnvironment = new HashMap<String,String>();




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
        this.buildId = buildId;
    }

    public void setPlatform(String p) {
        this.platform = p;
    }

    public static void disableDelete() {
        disableDelete = true;
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
        FileWriter fios = new FileWriter(sourceFile);
        String str = new String(data);
        str = str.replace(marker, newValue);
        fios.write(str);
        fios.close();
    }

    public String readFileToString(File sourceFile) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        //FileWriter fios = new FileWriter(sourceFile);
        String str = new String(data);
        return str;
    }

    public boolean findInFile(File sourceFile, String marker) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        //FileWriter fios = new FileWriter(sourceFile);
        String str = new String(data);
        return str.contains(marker);
        //str = str.replace(marker, newValue);
        //fios.write(str);
        //fios.close();
    }

    public void replaceAllInFile(File sourceFile, String marker, String newValue) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(sourceFile));
        byte[] data = new byte[(int) sourceFile.length()];
        dis.readFully(data);
        dis.close();
        FileWriter fios = new FileWriter(sourceFile);
        String str = new String(data);
        str = str.replaceAll(marker, newValue);
        fios.write(str);
        fios.close();
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

                        @Override
                        public void visit(int i, int i1, String string, String string1, String superName, String[] interfaces) {
                            scanner.usesClass(superName);
                            for (String s : interfaces) {
                                scanner.usesClass(s);
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
                        }

                        @Override
                        public FieldVisitor visitField(int i, String string, String type, String string2, Object o) {
                            if (type.startsWith("L")) {
                                scanner.usesClass(type.substring(1, type.length() - 2));
                            }
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int i, final String methodName, String string1, String string2, String[] strings) {
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
                                }

                                @Override
                                public void visitInsn(int i) {
                                }

                                @Override
                                public void visitIntInsn(int i, int i1) {
                                }

                                @Override
                                public void visitVarInsn(int i, int i1) {
                                }

                                @Override
                                public void visitTypeInsn(int i, String string) {
                                    scanner.usesClass(string);
                                }

                                @Override
                                public void visitFieldInsn(int i, String string, String string1, String string2) {
                                }

                                @Override
                                public void visitMethodInsn(int i, String owner, String name, String string2) {
                                    scanner.usesClass(owner);
                                    if (name != null && !name.equals("<init>")) {
                                        scanner.usesClassMethod(owner, name);
                                    }
                                }

                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    scanner.usesClass(owner);
                                    if (name != null && !name.equals("<init>")) {
                                        scanner.usesClassMethod(owner, name);
                                    }
                                }

                                @Override
                                public void visitJumpInsn(int i, Label label) {
                                }

                                @Override
                                public void visitLabel(Label label) {
                                }

                                @Override
                                public void visitLdcInsn(Object o) {
                                    if (o instanceof Type) {
                                        scanner.usesClass(((Type) o).getClassName());
                                    }
                                }

                                @Override
                                public void visitIincInsn(int i, int i1) {
                                }

                                @Override
                                public void visitTableSwitchInsn(int i, int i1, Label label, Label[] labels) {
                                }

                                @Override
                                public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
                                }

                                @Override
                                public void visitMultiANewArrayInsn(String string, int i) {
                                }

                                @Override
                                public void visitTryCatchBlock(Label label, Label label1, Label label2, String string) {
                                }

                                @Override
                                public void visitLocalVariable(String string, String classType, String string2, Label label, Label label1, int i) {
                                    if (classType.startsWith("L")) {
                                        scanner.usesClass(classType.substring(1, classType.length() - 2));
                                    }
                                }

                                @Override
                                public void visitLineNumber(int i, Label label) {
                                }

                                @Override
                                public void visitMaxs(int i, int i1) {
                                }

                                @Override
                                public void visitEnd() {
                                }
                            };
                        }

                        @Override
                        public void visitEnd() {
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
                out.write(javaImplSourceFile.getBytes());
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
                if (fileName.endsWith(".java") || fileName.endsWith(".m") || fileName.endsWith(".h") || fileName.endsWith(".cs")) {
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

                if(!"html.tar".equals(entryName)&& (entryName.startsWith("html") || entryName.startsWith("/html"))) {
                    if(entry.isDirectory()) {
                        continue;
                    }

                    if(tos == null) {
                        tos = new TarOutputStream(new FileOutputStream(new File(resDir, "html.tar")));
                    }
                    entryName = entryName.substring(5);
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
                    if(podspecTos == null) {
                        podspecTos = new TarOutputStream(new FileOutputStream(new File(resDir, "podspecs.tar")));
                    }
                    entryName = entryName.substring(podSpecsPrefix);
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
                    if(libTos == null) {
                        libTos = new TarOutputStream(new FileOutputStream(new File(resDir, "javase.lib.tar")));
                    }
                    entryName = entryName.substring(libPrefix);
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

                if (entry.isDirectory()) {
                    File dir = new File(classesDir, entryName);
                    dir.mkdirs();
                    dir = new File(resDir, entryName);
                    dir.mkdirs();
                    dir = new File(sourceDir, entryName);
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
                        destFile = new File(classesDir, entryName);
                    }
                } else {
                    if (entryName.endsWith(".java") || entryName.endsWith(".m") || entryName.endsWith(".h") || entryName.endsWith(".cs")) {
                        destFile = new File(sourceDir, entryName);
                    } else {
                        if (entryName.endsWith(".jar") || entryName.endsWith(".a") || entryName.endsWith(".dylib") || entryName.endsWith(".andlib") || entryName.endsWith(".aar") || entryName.endsWith(dll)) {
                            destFile = new File(libsDir, entryName);
                        } else {
                            if (useXMLDir() && entryName.endsWith(".xml")) {
                                destFile = placeXMLFile(entry, xmlDir, resDir);
                            } else {
                                if(entryName.equals("codenameone_settings.properties")) {
                                    destFile = new File(sourceDir.getParentFile(), entryName);
                                } else {
                                    destFile = new File(resDir, entryName);
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
                    File dir = new File(destDir, entryName);
                    dir.mkdirs();
                    continue;
                }

                int count;
                byte[] data = new byte[8192];

                // write the files to the disk
                File destFile = filter.destFile(currentDir, entryName);
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

    protected File placeXMLFile(ZipEntry entry, File xmlDir, File resDir) {
        boolean putInXMLDir = false;
        String name = entry.getName();
        if (name.contains("/")) {
            name = name.substring(0, name.lastIndexOf("/"));
            if (name.contains("/")) {
                name = name.substring(name.lastIndexOf("/"));
                if (name.equalsIgnoreCase("xml")) {
                    putInXMLDir = true;
                }
            } else {
                if (name.equalsIgnoreCase("xml")) {
                    putInXMLDir = true;
                }
            }
            name = entry.getName();
            name = name.substring(name.lastIndexOf("/") + 1, name.length());
        }
        if (putInXMLDir) {
            return new File(xmlDir, name);
        } else {
            return new File(resDir, entry.getName());
        }
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
            new Thread() {
                public void run() {
                    try {
                        byte[] buffer = new byte[8192];
                        int i = stream.read(buffer);
                        while (i > -1) {
                            String str = new String(buffer, 0, i);
                            log(str, false);
                            outputMessage.append(str);
                            i = stream.read(buffer);
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        outputMessage.append("Exception on appending to log: " + ex);
                    }
                }
            }.start();
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
            if (destroyed[0]) {
                log("Process timed out");
                return 1;
            }
            running[0] = false;
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
        byte[] tmpBuf = new byte[8192];

        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                boolean found = false;
                if (exclude != null) {
                    List<String> excludeNames = new ArrayList<String>();

                    for (String ex : exclude) {
                        if (ex.indexOf("/") > -1) {
                            // We only check for excludes at this level
                        } else {
                            excludeNames.add(ex);
                        }
                    }
                    for (String ex : excludeNames) {
                        if (files[i].getName().equalsIgnoreCase(ex)) {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    List<String> newExcludes = new ArrayList<String>();
                    if (exclude != null) {
                        for (String ex : exclude) {
                            if (ex.indexOf("/") > -1) {
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
        BufferedImage ret = (BufferedImage) img;
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

    /**
     * @return the buildKey
     */
    public String getBuildKey() {
        return buildKey;
    }

    /**
     * @param buildKey the buildKey to set
     */
    public void setBuildKey(String buildKey) {
        this.buildKey = buildKey;
    }



    protected boolean deriveGlobalInstrumentClasspath() {
        return false;
    }

    private void instrument(String classpath, File directory, File root, List<String> methodNames) throws Exception {
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                instrument(classpath, f, root, methodNames);
                continue;
            }
            if (f.getName().endsWith(".class") && !f.getName().endsWith("CodenameOneThread.class")) {
                ClassPool pool = new ClassPool(deriveGlobalInstrumentClasspath());
                pool.appendClassPath(root.getAbsolutePath());
                if (classpath != null) {
                    pool.appendClassPath(classpath);
                }
                String name = f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
                name = name.substring(0, name.length() - 6);
                name = name.replace(File.separatorChar, '.');//.replace('$', '.');
                DataInputStream fi = new DataInputStream(new FileInputStream(f));
                CtClass cls = pool.makeClass(fi);//pool.get(name);
                fi.close();
                CtClass runtimeException = pool.get("java.lang.RuntimeException");

                methodNames.add(name);
                CtMethod[] mtds = cls.getDeclaredMethods();
                for (CtMethod mtd : mtds) {
                    if (!mtd.isEmpty()) {
                        if (mtd.getMethodInfo().getCodeAttribute() != null) {
                            methodNames.add(mtd.getName());
                            int mid = methodNames.size();
                            mtd.insertBefore("{ com.codename1.impl.CodenameOneThread.push(" + mid + "); }");
                            mtd.insertAfter("{ com.codename1.impl.CodenameOneThread.pop(); }", true);
                            mtd.addCatch("{ com.codename1.impl.CodenameOneThread.storeStack($e, " + mid + "); throw $e; }", runtimeException);
                            for (CtClass ex : mtd.getExceptionTypes()) {
                                mtd.addCatch("{ com.codename1.impl.CodenameOneThread.storeStack($e, " + mid + "); throw $e; }", ex);
                            }
                        }
                    }
                }
                CtConstructor[] cons = cls.getDeclaredConstructors();
                for (CtConstructor con : cons) {
                    if (!con.isEmpty()) {
                        if (con.getMethodInfo().getCodeAttribute() != null) {
                            methodNames.add(con.getName());
                            int mid = methodNames.size();
                            con.insertBefore("{ com.codename1.impl.CodenameOneThread.push(" + mid + "); }");
                            con.insertAfter("{ com.codename1.impl.CodenameOneThread.pop(); }", true);
                            con.addCatch("{ com.codename1.impl.CodenameOneThread.storeStack($e, " + mid + "); throw $e; }", runtimeException);
                            for (CtClass ex : con.getExceptionTypes()) {
                                con.addCatch("{ com.codename1.impl.CodenameOneThread.storeStack($e, " + mid + "); throw $e; }", ex);
                            }
                        }
                    }
                }
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(cls.toBytecode());
                fo.close();
            }
        }
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


        InputStream s = Executor.class.getResourceAsStream(res);
        if(s != null) {
            return s;
        }

        return null;
    }

    /**
     * Gets a potentially versioned file
     */
    public File getFileObject(File f) {
        return f;

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
                    + "}\n").getBytes());
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
        try {

            if(s == null) {
                return null;
            }
            byte[] dat = s.getBytes("UTF-8");
            for(int iter = 0 ; iter < dat.length ; iter++) {
                dat[iter] = (byte)(dat[iter] ^ (iter % 254 + 1));
            }
            return Base64.encodeNoNewline(dat);
        } catch(UnsupportedEncodingException err) {
            // will never happen damn stupid exception
            err.printStackTrace();
            return null;
        }
    }




    private ClassLoader getCodenameOneJarClassLoader() throws IOException {
        if (codenameOneJar == null) {
            throw new IllegalStateException("Must set codenameOneJar in Executor");
        }
        return new URLClassLoader(new URL[]{codenameOneJar.toURI().toURL()});
    }


}
