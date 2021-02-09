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
package com.codename1.maven;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static com.codename1.maven.PathUtil.path;

/**
 * Simple tool to generate platform implementation stubs matching a
 * Native interface instance
 *
 * @author Shai Almog
 */
public class StubGenerator {
    private Class nativeInterface;
    private StubGenerator() {}
    private File androidFile;
    private File javaseFile;
    private File csFile;
    private File iosHFile;
    private File iosMFile;
    private File jsFile;
    private Log log;


    public static StubGenerator create(Log log, Class nativeInterface) {

        StubGenerator instance = new StubGenerator();
        instance.log = log;
        instance.nativeInterface = nativeInterface;
        return instance;
    }

    private boolean isSubinterfaceOfNativeInterface() {
        for(Class current : nativeInterface.getInterfaces()) {
            if(current.getName().equals("com.codename1.system.NativeInterface")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks that the native interface is valid and if not returns the error as a string
     * returns null for a valid interface.
     */
    public String verify() {
        if(!nativeInterface.isInterface()) {
            return "Not an interface! Native interfaces must be interfaces.";
        }

        if(!isSubinterfaceOfNativeInterface()) {
            return "The interface MUST implement NativeInterface!";
        }

        if((nativeInterface.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
            return "The interface must be a public interface and not an inner class";
        }

        if(nativeInterface.getEnclosingClass() != null) {
            return "The interface must be a public interface and not an inner class";
        }

        Method[] mtds = nativeInterface.getMethods();
        int offset = 0;
        for(Method m : mtds) {
            if(m.getExceptionTypes().length > 0) {
                return "Exceptions aren't supported when communicating with native interfaces, in the method " + m.getName();
            }

            if(m.getName().equalsIgnoreCase("init")) {
                return "init() is a reserved method in iOS (a constructor of sort) naming a method init will not work properly.";
            }

            if(!isValidType(m.getReturnType())) {
                return "Unsupported return type  " + m.getReturnType().getSimpleName() + " in the method " + m.getName();
            }

            if(!checkDuplicateName(mtds, offset)) {
                return "A method with the same name exists for the method " + m.getName() +
                        ", notice that duplicate names (even with different case) aren't supported!";
            }
            offset++;

            for(Class arg : m.getParameterTypes()) {
                if(!isValidType(arg)) {
                    return "Unsupported argument type  " + arg.getSimpleName() + " in the method " + m.getName();
                }
            }
        }

        return null;
    }

    private boolean checkDuplicateName(Method[] mtds, int offset) {
        String name = mtds[offset].getName();
        for(int iter = offset + 1 ; iter < mtds.length ; iter++) {
            if(mtds[iter].getName().equalsIgnoreCase(name)) {
                return false;
            }
        }
        return true;
    }

    private void initFileNames(File destination) {
        String className = nativeInterface.getName().replace('.', File.separatorChar) + "Impl.java";
        androidFile = new File(path(destination.getAbsolutePath(), "android", "src", "main", "java", className));
        androidFile.getParentFile().mkdirs();
        javaseFile = new File(path(destination.getAbsolutePath(), "javase", "src", "main", "java", className));
        javaseFile.getParentFile().mkdirs();
        csFile = new File(path(destination.getAbsolutePath(), "win", "src", "main", "csharp", nativeInterface.getName().replace('.', File.separatorChar) + "Impl.cs"));
        csFile.getParentFile().mkdirs();

        String iosFilename = nativeInterface.getName().replace('.', '_') + "Impl.";
        iosHFile = new File(path(destination.getAbsolutePath(), "src", "main", "objectivec", iosFilename+"h"));
        iosMFile = new File(path(destination.getAbsolutePath(), "src", "main", "objectivec", iosFilename+"m"));
        iosMFile.getParentFile().mkdirs();

        jsFile = new File(path(destination.getAbsolutePath(), "src", "main", "javascript", nativeInterface.getName().replace('.', '_') + ".js"));
        jsFile.getParentFile().mkdirs();
    }

    /**
     * Returns true if native files might need overwriting
     */
    public boolean isFilesExist(File destination) {
        initFileNames(destination);
        return androidFile.exists() || iosHFile.exists() || iosMFile.exists() ||
                csFile.exists() || javaseFile.exists() || jsFile.exists();
    }



    /**
     * Generates the code to the given base directory
     */
    public void generateCode(File destination, boolean overwrite) throws IOException {
        initFileNames(destination);
        if(overwrite || !androidFile.exists()) {
            log.info("Writing "+androidFile);
            generateJavaFile(androidFile, "android.view.View", false);
        } else {
            log.debug(androidFile+" already exists. Skipping");
        }
        if(overwrite || !javaseFile.exists()) {
            log.info("Writing "+javaseFile);
            generateJavaFile(javaseFile, "com.codename1.ui.PeerComponent", true);
        } else {
            log.debug(javaseFile+" already exists. Skipping");
        }

        if(overwrite || !csFile.exists()) {
            log.info("Writing "+csFile);
            generateCSFile(csFile, "FrameworkElement");
        } else {
            log.debug(csFile+" already exists. Skipping");
        }
        if(overwrite || !(iosHFile.exists() || iosMFile.exists())) {
            log.info("Writing "+iosHFile);
            log.info("Writing "+iosMFile);
            generateIOSFiles();
        } else {
            log.debug(iosHFile+" already exists. Skipping");
        }
        if(overwrite || !(jsFile.exists())) {
            log.info("Writing "+jsFile);
            generateJavaScriptFile();
        } else {
            log.debug(jsFile+" already exists. Skipping");
        }
    }

    private void generateIOSFiles() throws IOException {
        String h = "#import <Foundation/Foundation.h>\n\n"
                + "@interface " + nativeInterface.getName().replace('.', '_') + "Impl : NSObject {\n"
                + "}\n\n";

        Method[] mtds = nativeInterface.getMethods();
        for(Method m : mtds) {
            h += "-(" + javaTypeToObjectiveCType(m.getReturnType()) +")" + m.getName();
            Class[] params = m.getParameterTypes();
            if(params == null || params.length == 0) {
                h += ";\n";
                continue;
            }
            h += ":(" + javaTypeToObjectiveCType(params[0]) + ")param";
            if(params.length == 1) {
                h += ";\n";
                continue;
            }

            for(int iter = 1 ; iter < params.length ; iter++) {
                h += " param" + iter + ":(" + javaTypeToObjectiveCType(params[iter]) + ")param" + iter;
            }
            h += ";\n";
        }

        h += "@end\n";

        String m = "#import \"" + nativeInterface.getName().replace('.', '_') + "Impl.h\"\n\n"
                + "@implementation " + nativeInterface.getName().replace('.', '_') + "Impl\n\n";

        for(Method mtd : mtds) {
            Class returnType = mtd.getReturnType();
            m += "-(" + javaTypeToObjectiveCType(returnType) +")" + mtd.getName();
            Class[] params = mtd.getParameterTypes();
            if(params == null || params.length == 0) {
                m += "{\n";
            } else {
                m += ":(" + javaTypeToObjectiveCType(params[0]) + ")param";
                if(params.length == 1) {
                    m += "{\n";
                } else {
                    for(int iter = 1 ; iter < params.length ; iter++) {
                        m += " param" + iter + ":(" + javaTypeToObjectiveCType(params[iter]) + ")param" + iter;
                    }
                    m += "{\n";
                }
            }
            if(returnType != Void.TYPE && returnType != Void.class) {
                if(returnType == String.class || returnType.getName().equals("com.codename1.ui.PeerComponent") ||
                        returnType.isArray()) {
                    m += "    return nil;\n";
                } else {
                    if(returnType == Boolean.class || returnType == Boolean.TYPE) {
                        m += "    return NO;\n";
                    } else {
                        m += "    return 0;\n";
                    }
                }
            }
            m+= "}\n\n";
        }

        m += "@end\n";

        iosHFile.getParentFile().mkdirs();
        FileOutputStream fo = new FileOutputStream(iosHFile);
        fo.write(h.getBytes());
        fo.close();
        fo = new FileOutputStream(iosMFile);
        fo.write(m.getBytes());
        fo.close();
    }

    private String javaTypeToObjectiveCType(Class t) {
        if(t == String.class) {
            return "NSString*";
        }
        if(t.isArray()) {
            return "NSData*";
        }
        if(t == Integer.class || t == Integer.TYPE) {
            return "int";
        }
        if(Long.class == t || Long.TYPE == t) {
            return "long long";
        }
        if(Byte.class == t || Byte.TYPE == t) {
            return "char";
        }
        if(Short.class == t || Short.TYPE == t) {
            return "short";
        }
        if(Character.class == t || Character.TYPE == t) {
            return "int";
        }
        if(Boolean.class == t || Boolean.TYPE == t) {
            return "BOOL";
        }
        if(Float.class == t || Float.TYPE == t) {
            return "float";
        }
        if(Double.class == t || Double.TYPE == t) {
            return "double";
        }
        if(Void.class == t || Void.TYPE == t) {
            return "void";
        }
        return "void*";
    }


    private String javaTypeToCSharpType(Class t) {
        if(t.getName().equals("com.codename1.ui.PeerComponent")) {
            return "object";
        }
        if(t == String.class) {
            return "string ";
        }
        if(t == int.class || t == Integer.class || t == Integer.TYPE) {
            if(t.isArray()) {
                return "int[]";
            }
            return "int";
        }
        if(t == long.class || Long.class == t || Long.TYPE == t) {
            if(t.isArray()) {
                return "long[]";
            }
            return "long";
        }
        if(t == byte.class || Byte.class == t || Byte.TYPE == t) {
            if(t.isArray()) {
                return "byte[]";
            }
            return "byte";
        }
        if(t == short.class || Short.class == t || Short.TYPE == t) {
            if(t.isArray()) {
                return "short[]";
            }
            return "short";
        }
        if(t == char.class || Character.class == t || Character.TYPE == t) {
            if(t.isArray()) {
                return "char[]";
            }
            return "char";
        }
        if(t == boolean.class || Boolean.class == t || Boolean.TYPE == t) {
            if(t.isArray()) {
                return "bool[]";
            }
            return "bool";
        }
        if(t == float.class || Float.class == t || Float.TYPE == t) {
            if(t.isArray()) {
                return "float[]";
            }
            return "float";
        }
        if(t == double.class || Double.class == t || Double.TYPE == t) {
            if(t.isArray()) {
                return "double[]";
            }
            return "double";
        }
        if(Void.class == t || Void.TYPE == t) {
            return "void";
        }
        return t.getSimpleName();
    }

    private void generateJavaFile(File dest, String peerComponentType, boolean impl) throws IOException {
        String t = "package " + nativeInterface.getPackage().getName() + ";\n\n"
                + "public class " + nativeInterface.getSimpleName() + "Impl ";
        if(impl) {
            t += "implements " + nativeInterface.getName() + "{\n";
        } else {
            t += "{\n";
        }
        Method[] mtds = nativeInterface.getMethods();
        for(Method m : mtds) {
            t += "    public ";
            Class returnType = m.getReturnType();
            if(returnType.getName().equals("com.codename1.ui.PeerComponent")) {
                t += peerComponentType;
            } else {
                t += returnType.getSimpleName();
            }

            t += " " + m.getName() + "(";

            int offset = 0;
            for(Class arg : m.getParameterTypes()) {
                String s = arg.getSimpleName();
                if(arg.getName().equals("com.codename1.ui.PeerComponent")) {
                    s = peerComponentType;
                }
                if(offset == 0) {
                    t += s + " param";
                } else {
                    t += ", " + s + " param" + offset;
                }
                offset++;
            }

            t += ") {\n";

            if(returnType != Void.TYPE && returnType != Void.class) {
                if(returnType == String.class || returnType.getName().equals("com.codename1.ui.PeerComponent") ||
                        returnType.isArray()) {
                    t += "        return null;\n";
                } else {
                    if(returnType == Boolean.class || returnType == Boolean.TYPE) {
                        t += "        return false;\n";
                    } else {
                        if(returnType == Character.class || returnType == Character.TYPE) {
                            t += "        return (char)0;\n";
                        } else {
                            if(returnType == Byte.class || returnType == Byte.TYPE) {
                                t += "        return (byte)0;\n";
                            } else {
                                if(returnType == Short.class || returnType == Short.TYPE) {
                                    t += "        return (short)0;\n";
                                } else {
                                    t += "        return 0;\n";
                                }
                            }
                        }
                    }
                }
            }

            t += "    }\n\n";
        }
        t += "}\n";
        dest.getParentFile().mkdirs();
        FileOutputStream fo = new FileOutputStream(dest);
        fo.write(t.getBytes());
        fo.close();
    }

    private void generateCSFile(File dest, String peerComponentType) throws IOException {
        String t = "namespace " + nativeInterface.getPackage().getName() + "{\r\n\r\n" +
                "\r\n" +
                "public class " + nativeInterface.getSimpleName() + "Impl : I"+ nativeInterface.getSimpleName() + "Impl {\r\n";

        Method[] mtds = nativeInterface.getMethods();
        for(Method m : mtds) {
            t += "    public ";
            Class returnType = m.getReturnType();
            t += javaTypeToCSharpType(returnType);

            t += " " + m.getName() + "(";

            int offset = 0;
            for(Class arg : m.getParameterTypes()) {
                String s = arg.getSimpleName();
                if(arg.getName().equals("com.codename1.ui.PeerComponent")) {
                    s = "object";
                } else {
                    if(arg == boolean.class || arg == Boolean.class || arg == Boolean.TYPE) {
                        s = "bool";
                    }
                }
                if(offset == 0) {
                    t += s + " param";
                } else {
                    t += ", " + s + " param" + offset;
                }
                offset++;
            }

            t += ") {\n";

            if(returnType != Void.TYPE && returnType != Void.class) {
                if(returnType == String.class || returnType.getName().equals("com.codename1.ui.PeerComponent") ||
                        returnType.isArray()) {
                    t += "        return null;\n";
                } else {
                    if(returnType == Boolean.class || returnType == Boolean.TYPE) {
                        t += "        return false;\n";
                    } else {
                        if(returnType == Character.class || returnType == Character.TYPE) {
                            t += "        return (char)0;\n";
                        } else {
                            if(returnType == Byte.class || returnType == Byte.TYPE) {
                                t += "        return (byte)0;\n";
                            } else {
                                if(returnType == Short.class || returnType == Short.TYPE) {
                                    t += "        return (short)0;\n";
                                } else {
                                    t += "        return 0;\n";
                                }
                            }
                        }
                    }
                }
            }

            t += "    }\n\n";
        }
        t += "}\r\n}\r\n";
        dest.getParentFile().mkdirs();
        FileOutputStream fo = new FileOutputStream(dest);
        fo.write(t.getBytes());
        fo.close();
    }

    private boolean isValidType(Class cls) {
        if(cls.isPrimitive()) {
            return true;
        }
        if(cls.isArray()) {
            return cls.getComponentType().isPrimitive();
        }
        if(cls == String.class) {
            return true;
        }
        if(cls.getName().equals("com.codename1.ui.PeerComponent")) {
            return true;
        }
        return false;
    }

    private String typeToXMLVMJavaName(Class type) {
        if(type.isArray()) {
            return getSimpleNameWithJavaLang(type.getComponentType()).replace('.', '_') + "_1ARRAY";
        }
        return getSimpleNameWithJavaLang(type).replace('.', '_');
    }

    private String getSimpleNameWithJavaLang(Class c) {
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

    private void generateJavaScriptFile() throws IOException {
        String t = "(function(exports){\n\n" +
                "var o = {};\n\n";

        Method[] mtds = nativeInterface.getMethods();
        for(Method m : mtds) {
            t += "    o.";
            t += m.getName();
            t += "_";

            Class[] params = m.getParameterTypes();
            for(Class currentParam : params) {
                if(currentParam.getName().equals("com.codename1.ui.PeerComponent")) {
                    t += "_com_codename1_ui_PeerComponent";
                } else {
                    t += "_";
                    t += typeToXMLVMJavaName(currentParam);
                }
            }
            t += " = function(";
            if(params.length > 0) {
                t += "param1";
                for(int iter = 2 ; iter < params.length + 1 ; iter++) {
                    t += ", param" + iter;
                }
                t += ", callback) {\n";
            } else {
                t += "callback) {\n";
            }

            if(m.getName().equals("isSupported")) {
                t += "        callback.complete(false);\n";
            } else {
                t += "        callback.error(new Error(\"Not implemented yet\"));\n";
            }

            t += "    };\n\n";
        }
        t += "exports.";
        t += nativeInterface.getName().replace('.', '_');
        t += "= o;\n\n" +
                "})(cn1_get_native_interfaces());\n";
        FileOutputStream fo = new FileOutputStream(jsFile);
        fo.write(t.getBytes());
        fo.close();
    }
}