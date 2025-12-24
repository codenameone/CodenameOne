package com.codename1.apichecker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class APIChecker {

    public static void main(String[] args) throws IOException {
        String subjectPath = null;
        String referencePath = null;
        String reportPath = null;

        for (int i = 0; i < args.length; i++) {
            if ("--subject".equals(args[i])) {
                subjectPath = args[++i];
            } else if ("--reference".equals(args[i])) {
                referencePath = args[++i];
            } else if ("--report".equals(args[i])) {
                reportPath = args[++i];
            }
        }

        if (subjectPath == null || referencePath == null) {
            System.err.println("Usage: APIChecker --subject <path> --reference <path> [--report <path>]");
            System.exit(1);
        }

        System.out.println("Loading subject classes from: " + subjectPath);
        Map<String, ClassNode> subjectClasses = loadClasses(subjectPath);

        System.out.println("Loading reference classes from: " + referencePath);
        Map<String, ClassNode> referenceClasses;
        if ("java11".equals(referencePath)) {
            referenceClasses = loadJava11Classes();
        } else {
            referenceClasses = loadClasses(referencePath);
        }

        List<String> errors = new ArrayList<>();
        Map<String, List<String>> extraApis = new TreeMap<>();

        // Pre-calculate subject packages for faster lookup
        Set<String> subjectPackages = new HashSet<>();
        for (String className : subjectClasses.keySet()) {
            int lastSlash = className.lastIndexOf('/');
            if (lastSlash != -1) {
                subjectPackages.add(className.substring(0, lastSlash));
            }
        }

        // Check Subject vs Reference (must be subset)
        for (ClassNode subjectClass : subjectClasses.values()) {
            if (isModuleInfo(subjectClass.name)) continue;
            if (!isPublicOrProtected(subjectClass.access)) continue;

            // Only check standard packages if comparing against Java 11
            if ("java11".equals(referencePath) && !isStandardPackage(subjectClass.name)) {
                continue;
            }

            ClassNode referenceClass = referenceClasses.get(subjectClass.name);
            if (referenceClass == null) {
                errors.add("Class " + subjectClass.name + " is present in Subject but missing in Reference.");
                continue;
            }

            // Check superclass
            if (!Objects.equals(subjectClass.superName, referenceClass.superName)) {
                if (subjectClass.superName != null || referenceClass.superName != null) {
                     errors.add("Class " + subjectClass.name + " extends " + subjectClass.superName + " but Reference extends " + referenceClass.superName);
                }
            }

            // Check interfaces
            Set<String> refInterfaces = new HashSet<>(referenceClass.interfaces);
            for (String iface : subjectClass.interfaces) {
                if (!refInterfaces.contains(iface)) {
                     errors.add("Class " + subjectClass.name + " implements " + iface + " which is missing in Reference.");
                }
            }

            // Check fields
            for (FieldNode subjectField : subjectClass.fields) {
                if (!isPublicOrProtected(subjectField.access)) continue;
                boolean found = false;
                for (FieldNode refField : referenceClass.fields) {
                    if (refField.name.equals(subjectField.name) && refField.desc.equals(subjectField.desc)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                     errors.add("Field " + subjectClass.name + "." + subjectField.name + " " + subjectField.desc + " is missing in Reference.");
                }
            }

            // Check methods
            for (MethodNode subjectMethod : subjectClass.methods) {
                if (!isPublicOrProtected(subjectMethod.access)) continue;
                if ("<clinit>".equals(subjectMethod.name)) continue;

                boolean found = false;
                for (MethodNode refMethod : referenceClass.methods) {
                    if (refMethod.name.equals(subjectMethod.name) && refMethod.desc.equals(subjectMethod.desc)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                     errors.add("Method " + subjectClass.name + "." + subjectMethod.name + subjectMethod.desc + " is missing in Reference.");
                }
            }
        }

        // Check Reference vs Subject (for report)
        if (reportPath != null) {
            for (ClassNode referenceClass : referenceClasses.values()) {
                if (isModuleInfo(referenceClass.name)) continue;
                if (!isPublicOrProtected(referenceClass.access)) continue;

                ClassNode subjectClass = subjectClasses.get(referenceClass.name);

                if (subjectClass == null) {
                    // Check if the package exists in Subject to reduce noise (optional, but good for library comparison)
                    // If reference is java11, we only care if CLDC11 *should* have it.
                    // But if strict report is required: "classes/methods that are in vm/JavaAPI but not in CLDC11".
                    // I'll stick to packages present in Subject to avoid reporting unrelated libs if any.
                    // But for vm/JavaAPI, it should be mostly matching.
                    // Let's report if package matches or if we are not checking against java11 (where we expect huge diffs).

                    boolean relevant = !"java11".equals(referencePath) || isPackageInSubject(referenceClass.name, subjectPackages);
                    if (relevant) {
                        addExtra(extraApis, referenceClass.name, "Class " + referenceClass.name + " is missing in Subject.");
                    }
                } else {
                    // Class exists, check members
                    for (FieldNode refField : referenceClass.fields) {
                        if (!isPublicOrProtected(refField.access)) continue;
                        boolean found = false;
                        for (FieldNode subjectField : subjectClass.fields) {
                            if (subjectField.name.equals(refField.name) && subjectField.desc.equals(refField.desc)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            addExtra(extraApis, referenceClass.name, "Field " + refField.name + " " + refField.desc);
                        }
                    }

                    for (MethodNode refMethod : referenceClass.methods) {
                        if (!isPublicOrProtected(refMethod.access)) continue;
                        if ("<clinit>".equals(refMethod.name)) continue;
                        boolean found = false;
                        for (MethodNode subjectMethod : subjectClass.methods) {
                            if (subjectMethod.name.equals(refMethod.name) && subjectMethod.desc.equals(refMethod.desc)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            addExtra(extraApis, referenceClass.name, "Method " + refMethod.name + refMethod.desc);
                        }
                    }
                }
            }

            // Write report
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(reportPath), extraApis);
            System.out.println("Report written to " + reportPath);
        }

        if (!errors.isEmpty()) {
            System.err.println("Compatibility Errors Found:");
            for (String error : errors) {
                System.err.println(error);
            }
            System.exit(1);
        } else {
            System.out.println("Compatibility Check Passed.");
        }
    }

    private static void addExtra(Map<String, List<String>> report, String className, String item) {
        report.computeIfAbsent(className, k -> new ArrayList<>()).add(item);
    }

    private static boolean isPackageInSubject(String className, Set<String> subjectPackages) {
        int lastSlash = className.lastIndexOf('/');
        if (lastSlash == -1) return false;
        String pkg = className.substring(0, lastSlash);
        // Direct lookup or parent lookup? Usually direct.
        return subjectPackages.contains(pkg);
    }

    private static boolean isStandardPackage(String className) {
        return className.startsWith("java/") || className.startsWith("javax/");
    }

    private static boolean isModuleInfo(String className) {
        return className.endsWith("module-info");
    }

    private static boolean isPublicOrProtected(int access) {
        return (access & Opcodes.ACC_PUBLIC) != 0 || (access & Opcodes.ACC_PROTECTED) != 0;
    }

    private static Map<String, ClassNode> loadClasses(String path) throws IOException {
        Map<String, ClassNode> classes = new HashMap<>();
        File file = new File(path);
        if (file.isDirectory()) {
             Files.walk(file.toPath())
                  .filter(p -> p.toString().endsWith(".class"))
                  .forEach(p -> {
                      try {
                          ClassNode cn = readClass(Files.readAllBytes(p));
                          if (!isModuleInfo(cn.name)) {
                              classes.put(cn.name, cn);
                          }
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  });
        } else if (path.endsWith(".jar")) {
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        ClassNode cn = readClass(jar.getInputStream(entry).readAllBytes());
                        if (!isModuleInfo(cn.name)) {
                            classes.put(cn.name, cn);
                        }
                    }
                }
            }
        }
        return classes;
    }

    private static Map<String, ClassNode> loadJava11Classes() throws IOException {
        Map<String, ClassNode> classes = new HashMap<>();
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path modules = fs.getPath("modules");
        Files.walk(modules)
             .filter(p -> p.toString().endsWith(".class"))
             .forEach(p -> {
                 try {
                     if (p.getFileName().toString().equals("module-info.class")) return;
                     ClassNode cn = readClass(Files.readAllBytes(p));
                     classes.put(cn.name, cn);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             });
        return classes;
    }

    private static ClassNode readClass(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cn;
    }
}
