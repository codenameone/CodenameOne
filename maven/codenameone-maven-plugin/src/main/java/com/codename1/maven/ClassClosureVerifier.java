package com.codename1.maven;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Verifies that the application classes staged for a build-server submission are
 * self-consistent: every class in the application's own package space that is
 * referenced from another application class must actually be present in the
 * staged jar (or in an artifact the build server provides, such as
 * codenameone-core / java-runtime / kotlin-stdlib).
 *
 * Stale build output produces exactly this inconsistency -- e.g. an IDE deletes
 * the class files of removed sources but a dependent class compiled against them
 * survives in target/classes, and an incremental Maven compile then ships it.
 * The server-side VM translators only log a warning for the dangling reference
 * and the build later dies with an obscure native compiler error (a missing
 * generated header on iOS), so this check exists to fail fast with a clear
 * message before anything is uploaded.
 *
 * The check is deliberately scoped to the project's own packages (the packages
 * that contain classes in the project's output directories). Third-party
 * libraries routinely reference optional dependencies that are legitimately
 * absent; those references are outside the project package space and are
 * ignored here, matching the translators' tolerance for them.
 */
final class ClassClosureVerifier {

    private ClassClosureVerifier() {
    }

    /**
     * Scans the staged jar and returns the dangling project-package references.
     *
     * @param stagedJar the merged jar-with-dependencies that will be uploaded
     * @param projectClassRoots the project's own compiled classes -- output
     *                          directories and/or module jars (e.g. the app's
     *                          common jar); their contents define the project
     *                          package space
     * @param providedJars jars stripped from the upload that the build server
     *                     re-supplies; classes found in them are not missing
     * @return map of missing class internal name to the internal names of the
     *         classes that reference it, empty when the closure is consistent
     */
    static Map<String, Set<String>> findMissingProjectReferences(File stagedJar, List<File> projectClassRoots, List<File> providedJars) throws IOException {
        Set<String> projectPackages = new HashSet<String>();
        for (File root : projectClassRoots) {
            if (root.isDirectory()) {
                collectPackages(root, "", projectPackages);
            } else if (root.isFile()) {
                Set<String> classNames = new HashSet<String>();
                collectClassEntryNames(root, classNames);
                for (String className : classNames) {
                    projectPackages.add(packageOf(className));
                }
            }
        }
        if (projectPackages.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> containedClasses = new HashSet<String>();
        Map<String, Set<String>> referencingClassesByReference = new HashMap<String, Set<String>>();
        InputStream in = new BufferedInputStream(new FileInputStream(stagedJar));
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !isRelevantClassEntry(entry.getName())) {
                    continue;
                }
                String internalName = entry.getName().substring(0, entry.getName().length() - ".class".length());
                containedClasses.add(internalName);
                if (projectPackages.contains(packageOf(internalName))) {
                    collectReferences(internalName, readAllBytes(zip), referencingClassesByReference);
                }
            }
        } finally {
            in.close();
        }

        Set<String> providedClasses = new HashSet<String>();
        for (File jar : providedJars) {
            collectClassEntryNames(jar, providedClasses);
        }

        Map<String, Set<String>> missing = new TreeMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> e : referencingClassesByReference.entrySet()) {
            String referenced = e.getKey();
            if (containedClasses.contains(referenced) || providedClasses.contains(referenced)) {
                continue;
            }
            if (!projectPackages.contains(packageOf(referenced))) {
                continue;
            }
            missing.put(referenced, new TreeSet<String>(e.getValue()));
        }
        return missing;
    }

    private static boolean isRelevantClassEntry(String entryName) {
        if (!entryName.endsWith(".class")) {
            return false;
        }
        if (entryName.startsWith("META-INF/")) {
            return false;
        }
        return !entryName.endsWith("module-info.class");
    }

    private static String packageOf(String internalName) {
        int slash = internalName.lastIndexOf('/');
        return slash < 0 ? "" : internalName.substring(0, slash);
    }

    private static void collectPackages(File dir, String packagePath, Set<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectPackages(child, packagePath.isEmpty() ? child.getName() : packagePath + "/" + child.getName(), out);
            } else if (child.getName().endsWith(".class")) {
                out.add(packagePath);
            }
        }
    }

    private static void collectClassEntryNames(File jar, Set<String> out) throws IOException {
        if (jar == null || !jar.isFile()) {
            return;
        }
        InputStream in = new BufferedInputStream(new FileInputStream(jar));
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && isRelevantClassEntry(entry.getName())) {
                    out.add(entry.getName().substring(0, entry.getName().length() - ".class".length()));
                }
            }
        } finally {
            in.close();
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = input.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    /**
     * Collects the classes referenced from the given class's supertypes, member
     * descriptors and method bodies -- the references the VM translators render
     * and therefore the ones whose absence breaks a build. Metadata-only
     * references (annotations, generic signatures, the InnerClasses attribute)
     * are deliberately not collected: the translators tolerate their absence.
     */
    private static void collectReferences(final String sourceClass, byte[] classBytes, final Map<String, Set<String>> out) {
        final ReferenceSink sink = new ReferenceSink(sourceClass, out);
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                sink.addInternalName(superName);
                if (interfaces != null) {
                    for (String iface : interfaces) {
                        sink.addInternalName(iface);
                    }
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                sink.addType(Type.getType(descriptor));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                sink.addMethodDescriptor(descriptor);
                if (exceptions != null) {
                    for (String exception : exceptions) {
                        sink.addInternalName(exception);
                    }
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        sink.addInternalName(type);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        sink.addInternalName(owner);
                        sink.addMethodDescriptor(methodDescriptor);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        sink.addInternalName(owner);
                        sink.addType(Type.getType(fieldDescriptor));
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type) {
                            sink.addType((Type) value);
                        } else if (value instanceof Handle) {
                            sink.addHandle((Handle) value);
                        }
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String indyName, String indyDescriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        sink.addMethodDescriptor(indyDescriptor);
                        sink.addHandle(bootstrapMethodHandle);
                        for (Object argument : bootstrapMethodArguments) {
                            if (argument instanceof Type) {
                                sink.addType((Type) argument);
                            } else if (argument instanceof Handle) {
                                sink.addHandle((Handle) argument);
                            }
                        }
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String arrayDescriptor, int numDimensions) {
                        sink.addType(Type.getType(arrayDescriptor));
                    }

                    @Override
                    public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end, org.objectweb.asm.Label handler, String type) {
                        sink.addInternalName(type);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static final class ReferenceSink {
        private final String sourceClass;
        private final Map<String, Set<String>> out;

        private ReferenceSink(String sourceClass, Map<String, Set<String>> out) {
            this.sourceClass = sourceClass;
            this.out = out;
        }

        private void addInternalName(String internalName) {
            if (internalName == null) {
                return;
            }
            if (internalName.charAt(0) == '[') {
                addType(Type.getType(internalName));
                return;
            }
            Set<String> referencing = out.get(internalName);
            if (referencing == null) {
                referencing = new HashSet<String>();
                out.put(internalName, referencing);
            }
            referencing.add(sourceClass);
        }

        private void addType(Type type) {
            switch (type.getSort()) {
                case Type.ARRAY:
                    addType(type.getElementType());
                    break;
                case Type.OBJECT:
                    addInternalName(type.getInternalName());
                    break;
                case Type.METHOD:
                    addMethodDescriptor(type.getDescriptor());
                    break;
                default:
                    break;
            }
        }

        private void addMethodDescriptor(String descriptor) {
            for (Type argumentType : Type.getArgumentTypes(descriptor)) {
                addType(argumentType);
            }
            addType(Type.getReturnType(descriptor));
        }

        private void addHandle(Handle handle) {
            addInternalName(handle.getOwner());
            String descriptor = handle.getDesc();
            if (descriptor.startsWith("(")) {
                addMethodDescriptor(descriptor);
            } else {
                addType(Type.getType(descriptor));
            }
        }
    }
}
