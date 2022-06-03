package com.codename1.maven;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static com.codename1.maven.PathUtil.path;

/**
 * Generates native interface stubs for all native interfaces in the app. Will not overwrite existing stubs.
 */
@Mojo(name="generate-native-interfaces")
@Execute(phase= LifecyclePhase.COMPILE)
public class GenerateNativeInterfaces extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            // This should only be run in the CN1 project directory.
            getLog().debug("generate-native-interfaces skipped in directory "+project.getBasedir()+" because itis not a codeame one project directory");
            return;
        }
        File classes = new File(project.getBuild().getOutputDirectory());
        final List<Exception> failures = new ArrayList<>();
        try {
            scanClasses(classes, new ClassScanner() {

                @Override
                public void usesClass(String cls) {

                }

                @Override
                public void usesClassMethod(String cls, String method) {

                }

                @Override
                public void extendsNativeInterface(String cls) {
                    try {
                        generateNativeInterface(cls);
                    } catch (Exception ex) {
                        getLog().error("Problem occurred while generating native interface for class "+cls);
                        getLog().error(ex);
                        failures.add(ex);
                    }
                }
            });
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to generate native interfaces", ex);
        }
        if (!failures.isEmpty()) {
            throw new MojoExecutionException("There were "+failures.size()+" failures while generating native interface stubs. Check the log for details.");
        }
    }


    private void generateNativeInterface(String relativePath) throws Exception {
        if (!relativePath.endsWith(".class")) {
            relativePath += ".class";
        }
        File classFile = new File(path(project.getBuild().getOutputDirectory(), relativePath.replace(".java", ".class")));
        Class c = null;

        if (classFile.exists()) {

            File cn1CoreJar = getJar("com.codenameone", "codenameone-core");
            URLClassLoader cl = new URLClassLoader(new URL[]{
                    new File(project.getBuild().getOutputDirectory()).toURI().toURL(),
                    cn1CoreJar.toURI().toURL()});
            String classPath = relativePath.replace(File.separator, ".");
            classPath = classPath.substring(0, classPath.lastIndexOf("."));
            c = cl.loadClass(classPath);

        } else {
            throw new IllegalStateException("Project needs to be compiled first");
        }

        StubGenerator g = StubGenerator.create(getLog(), c);
        String s = g.verify();
        if (s != null) {
            throw new RuntimeException("Generation Failed: " + s);
        }


        g.generateCode(project.getBasedir().getCanonicalFile().getParentFile(), false);


    }

    private static interface ClassScanner {

        public void usesClass(String cls);

        public void usesClassMethod(String cls, String method);

        public void extendsNativeInterface(String cls);
    }

    private void scanClasses(File directory, final ClassScanner scanner) throws IOException {
        File[] list = directory.listFiles();
        for (File current : list) {
            if (current.isDirectory()) {
                scanClasses(current, scanner);
            } else {
                if (current.getName().endsWith(".class")) {
                    InputStream is = new FileInputStream(current);
                    ClassReader r = null;
                    try {
                        r = new ClassReader(is);
                    } catch(RuntimeException re) {

                        getLog().error("Problem reading class "+current, re);

                        throw re;
                    }
                    is.close();
                    ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9) {

                        @Override
                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                            scanner.usesClass(superName);
                            for (String s : interfaces) {
                                if ("com/codename1/system/NativeInterface".equals(s)) {
                                    scanner.extendsNativeInterface(name);
                                }
                                scanner.usesClass(s);
                            }
                            if ("com/codename1/system/NativeInterface".equals(superName)) {
                                scanner.extendsNativeInterface(name);
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
                        getLog().error("Error encountered while parsing class "+current.getName(), re);

                        throw re;
                    }
                }
            }
        }
    }
}
