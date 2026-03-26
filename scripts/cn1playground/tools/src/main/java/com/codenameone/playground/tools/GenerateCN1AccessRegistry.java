package com.codenameone.playground.tools;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Generates the CN1-safe dispatch registry from CN1 source trees instead of jars.
 */
public final class GenerateCN1AccessRegistry {
    private static final String ROOT_PACKAGE = "bsh.cn1";
    private static final String HELPER_PACKAGE = "bsh.cn1.gen";
    private static final String ROOT_CLASS_NAME = "GeneratedCN1Access";
    private static final String HELPER_CLASS_PREFIX = "GeneratedAccess_";
    private static final int FIND_CLASS_CHUNK_SIZE = 64;
    private static final char MEMBER_SEPARATOR = '\u001f';

    private static final String[] INDEX_PACKAGE_PREFIXES = new String[]{
            "com.codename1.",
            "com.codenameone.playground.",
            "java."
    };

    private GenerateCN1AccessRegistry() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output path");
        }
        File rootOutput = new File(args[0]);
        File projectRoot = projectRoot(rootOutput);
        File cn1Root = locateCn1Root(projectRoot);
        Discovery discovery = discover(projectRoot, cn1Root);
        File helperDir = new File(rootOutput.getParentFile(), "gen");
        recreateDir(helperDir);
        writeRootRegistry(rootOutput, discovery);
        writePackageHelpers(helperDir, discovery);
    }

    private static Discovery discover(File projectRoot, File cn1Root) throws Exception {
        List<File> sourceRoots = new ArrayList<File>();
        sourceRoots.add(new File(cn1Root, "CodenameOne/src"));
        sourceRoots.add(new File(cn1Root, "Ports/CLDC11/src"));
        sourceRoots.add(new File(projectRoot, "common/src/main/java"));

        List<File> sourceFiles = new ArrayList<File>();
        for (File sourceRoot : sourceRoots) {
            collectJavaFiles(sourceRoot, sourceFiles);
        }

        List<SourceUnit> units = parseSourceUnits(sourceFiles);
        LinkedHashMap<String, SourceClass> indexedClasses = new LinkedHashMap<String, SourceClass>();
        for (SourceUnit unit : units) {
            for (SourceClass sourceClass : unit.topLevelClasses) {
                if (matchesPrefix(sourceClass.qualifiedName, INDEX_PACKAGE_PREFIXES)) {
                    indexedClasses.put(sourceClass.qualifiedName, sourceClass);
                }
            }
        }

        LinkedHashSet<String> knownTypes = new LinkedHashSet<String>(indexedClasses.keySet());
        for (SourceClass sourceClass : indexedClasses.values()) {
            knownTypes.addAll(sourceClass.nestedTypes.values());
        }

        List<ApiClass> apiClasses = new ArrayList<ApiClass>();
        for (SourceClass sourceClass : indexedClasses.values()) {
            ApiClass apiClass = buildApiClass(sourceClass, knownTypes);
            apiClass = validateAgainstRuntime(apiClass);
            if (apiClass != null) {
                apiClasses.add(apiClass);
            }
        }
        apiClasses = resolveInheritedMembers(apiClasses);
        apiClasses = validateInheritedAgainstRuntime(apiClasses);
        Collections.sort(apiClasses, new Comparator<ApiClass>() {
            public int compare(ApiClass a, ApiClass b) {
                return a.qualifiedName.compareTo(b.qualifiedName);
            }
        });

        List<String> indexedClassNames = new ArrayList<String>(knownTypes);
        Collections.sort(indexedClassNames);

        LinkedHashMap<String, List<ApiClass>> classesByPackage = new LinkedHashMap<String, List<ApiClass>>();
        for (ApiClass apiClass : apiClasses) {
            if (!isDispatchClass(apiClass)) {
                continue;
            }
            List<ApiClass> classes = classesByPackage.get(apiClass.packageName);
            if (classes == null) {
                classes = new ArrayList<ApiClass>();
                classesByPackage.put(apiClass.packageName, classes);
            }
            classes.add(apiClass);
        }

        List<GeneratedPackage> packages = new ArrayList<GeneratedPackage>();
        for (Map.Entry<String, List<ApiClass>> entry : classesByPackage.entrySet()) {
            List<ApiClass> classes = entry.getValue();
            Collections.sort(classes, new Comparator<ApiClass>() {
                public int compare(ApiClass a, ApiClass b) {
                    return a.qualifiedName.compareTo(b.qualifiedName);
                }
            });
            packages.add(new GeneratedPackage(entry.getKey(), helperClassName(entry.getKey()), classes));
        }

        return new Discovery(indexedClassNames, packages);
    }

    private static List<SourceUnit> parseSourceUnits(List<File> sourceFiles) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler not available");
        }
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, Charset.defaultCharset());
        try {
            Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null,
                    Arrays.asList("-proc:none", "-implicit:none"), null, fileObjects);
            Iterable<? extends CompilationUnitTree> parsed = task.parse();
            List<SourceUnit> result = new ArrayList<SourceUnit>();
            for (CompilationUnitTree unit : parsed) {
                SourceUnit sourceUnit = buildSourceUnit(unit);
                if (sourceUnit != null && !sourceUnit.topLevelClasses.isEmpty()) {
                    result.add(sourceUnit);
                }
            }
            return result;
        } finally {
            fileManager.close();
        }
    }

    private static SourceUnit buildSourceUnit(CompilationUnitTree unit) {
        String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
        Map<String, String> explicitImports = new LinkedHashMap<String, String>();
        List<String> wildcardImports = new ArrayList<String>();
        for (ImportTree importTree : unit.getImports()) {
            if (importTree.isStatic()) {
                continue;
            }
            String name = importTree.getQualifiedIdentifier().toString();
            if (name.endsWith(".*")) {
                wildcardImports.add(name.substring(0, name.length() - 2));
            } else {
                explicitImports.put(simpleName(name), name);
            }
        }

        List<SourceClass> topLevelClasses = new ArrayList<SourceClass>();
        for (Tree typeDecl : unit.getTypeDecls()) {
            if (!(typeDecl instanceof ClassTree)) {
                continue;
            }
            ClassTree classTree = (ClassTree) typeDecl;
            if (!isTopLevelPublicType(classTree)) {
                continue;
            }
            String simpleName = classTree.getSimpleName().toString();
            String qualifiedName = qualify(packageName, simpleName);
            Map<String, String> nestedTypes = new LinkedHashMap<String, String>();
            collectNestedTypes(classTree, qualifiedName, nestedTypes);
            Map<String, String> typeParameterBounds = resolveTypeParameterBounds(packageName, simpleName, qualifiedName,
                    classTree, explicitImports, wildcardImports, nestedTypes);
            SourceClass unresolved = new SourceClass(packageName, simpleName, qualifiedName, classTree,
                    explicitImports, wildcardImports, nestedTypes, typeParameterBounds, Collections.<String>emptyList());
            List<String> superTypes = resolveSuperTypes(unresolved);
            topLevelClasses.add(new SourceClass(packageName, simpleName, qualifiedName, classTree,
                    explicitImports, wildcardImports, nestedTypes, typeParameterBounds, superTypes));
        }
        return new SourceUnit(packageName, topLevelClasses);
    }

    private static Map<String, String> resolveTypeParameterBounds(String packageName, String simpleName, String qualifiedName,
            ClassTree classTree, Map<String, String> explicitImports, List<String> wildcardImports,
            Map<String, String> nestedTypes) {
        if (classTree.getTypeParameters().isEmpty()) {
            return Collections.emptyMap();
        }
        SourceClass sourceClass = new SourceClass(packageName, simpleName, qualifiedName, classTree,
                explicitImports, wildcardImports, nestedTypes, Collections.<String, String>emptyMap(),
                Collections.<String>emptyList());
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (TypeParameterTree typeParameter : classTree.getTypeParameters()) {
            String bound = "java.lang.Object";
            if (!typeParameter.getBounds().isEmpty()) {
                String resolved = resolveHierarchyTypeName(sourceClass, typeParameter.getBounds().get(0).toString());
                if (resolved != null) {
                    bound = resolved;
                }
            }
            out.put(typeParameter.getName().toString(), bound);
        }
        return out;
    }

    private static List<String> resolveSuperTypes(SourceClass sourceClass) {
        List<String> out = new ArrayList<String>();
        if (sourceClass.classTree.getExtendsClause() != null) {
            String extendsType = resolveHierarchyTypeName(sourceClass, sourceClass.classTree.getExtendsClause().toString());
            if (extendsType != null) {
                out.add(extendsType);
            }
        }
        for (Tree implementedInterface : sourceClass.classTree.getImplementsClause()) {
            String implementedType = resolveHierarchyTypeName(sourceClass, implementedInterface.toString());
            if (implementedType != null) {
                out.add(implementedType);
            }
        }
        return out;
    }

    private static String resolveHierarchyTypeName(SourceClass sourceClass, String rawType) {
        if (rawType == null) {
            return null;
        }
        String cleaned = stripAnnotations(rawType).trim();
        cleaned = stripGenerics(cleaned).trim();
        cleaned = cleaned.replace("? extends ", "").replace("? super ", "").trim();
        while (cleaned.endsWith("[]")) {
            cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
        }
        if (cleaned.endsWith("...")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        if (cleaned.length() == 0 || ApiType.PRIMITIVES.contains(cleaned) || "void".equals(cleaned)) {
            return null;
        }
        if (cleaned.indexOf('.') >= 0) {
            String[] parts = cleaned.split("\\.");
            if (parts.length > 0 && Character.isLowerCase(parts[0].charAt(0))) {
                return cleaned;
            }
        }
        if (sourceClass.simpleName.equals(cleaned)) {
            return sourceClass.qualifiedName;
        }
        String nested = sourceClass.nestedTypes.get(cleaned);
        if (nested != null) {
            return nested;
        }
        String explicit = sourceClass.explicitImports.get(cleaned);
        if (explicit != null) {
            return explicit;
        }
        String samePackage = qualify(sourceClass.packageName, cleaned);
        if (sourceClass.packageName.length() > 0) {
            return samePackage;
        }
        String javaLang = Resolver.JAVA_LANG_BUILTINS.get(cleaned);
        if (javaLang != null) {
            return javaLang;
        }
        for (String wildcardImport : sourceClass.wildcardImports) {
            return wildcardImport + "." + cleaned;
        }
        return cleaned;
    }

    private static void collectNestedTypes(ClassTree classTree, String ownerQualifiedName, Map<String, String> nestedTypes) {
        for (Tree member : classTree.getMembers()) {
            if (!(member instanceof ClassTree)) {
                continue;
            }
            ClassTree nested = (ClassTree) member;
            if (!isNestedPublicType(nested, classTree)) {
                continue;
            }
            String nestedName = nested.getSimpleName().toString();
            String qualifiedName = ownerQualifiedName + "." + nestedName;
            nestedTypes.put(nestedName, qualifiedName);
            collectNestedTypes(nested, qualifiedName, nestedTypes);
        }
    }

    private static ApiClass buildApiClass(SourceClass sourceClass, Set<String> knownTypes) {
        Resolver resolver = new Resolver(sourceClass, knownTypes);
        List<ApiConstructor> constructors = new ArrayList<ApiConstructor>();
        List<ApiMethod> staticMethods = new ArrayList<ApiMethod>();
        List<ApiMethod> instanceMethods = new ArrayList<ApiMethod>();
        List<ApiField> staticFields = new ArrayList<ApiField>();
        List<ApiField> instanceFields = new ArrayList<ApiField>();

        boolean isInterface = sourceClass.classTree.getKind() == Tree.Kind.INTERFACE
                || sourceClass.classTree.getKind() == Tree.Kind.ANNOTATION_TYPE;
        boolean isEnum = sourceClass.classTree.getKind() == Tree.Kind.ENUM;
        boolean isAbstract = isInterface || sourceClass.classTree.getModifiers().getFlags().contains(Modifier.ABSTRACT);

        for (Tree member : sourceClass.classTree.getMembers()) {
            if (member instanceof MethodTree) {
                MethodTree methodTree = (MethodTree) member;
                if (methodTree.getReturnType() == null) {
                    ApiConstructor constructor = parseConstructor(sourceClass, methodTree, resolver);
                    if (constructor != null) {
                        constructors.add(constructor);
                    }
                } else {
                    ApiMethod method = parseMethod(sourceClass, methodTree, resolver, isInterface);
                    if (method == null) {
                        continue;
                    }
                    if (method.isStatic) {
                        staticMethods.add(method);
                    } else {
                        instanceMethods.add(method);
                    }
                }
                continue;
            }
            if (member instanceof VariableTree) {
                ApiField field = parseField(sourceClass, (VariableTree) member, resolver, isInterface, isEnum);
                if (field == null) {
                    continue;
                }
                if (field.isStatic) {
                    staticFields.add(field);
                } else {
                    instanceFields.add(field);
                }
            }
        }

        sortConstructors(constructors);
        sortMethods(staticMethods);
        sortMethods(instanceMethods);
        sortFields(staticFields);
        sortFields(instanceFields);

        return new ApiClass(sourceClass.packageName, sourceClass.simpleName, sourceClass.qualifiedName,
                sourceClass.superTypes, isInterface, isAbstract, constructors, staticMethods, instanceMethods,
                staticFields, instanceFields);
    }

    private static ApiClass validateAgainstRuntime(ApiClass apiClass) {
        if (apiClass == null || !apiClass.packageName.startsWith("java.")) {
            return apiClass;
        }
        if (!isSupportedJavaClass(apiClass.qualifiedName)) {
            return null;
        }
        Class<?> runtimeClass = loadRuntimeClass(apiClass.qualifiedName);
        if (runtimeClass == null || !java.lang.reflect.Modifier.isPublic(runtimeClass.getModifiers())) {
            return null;
        }

        List<ApiConstructor> constructors = new ArrayList<ApiConstructor>();
        for (ApiConstructor constructor : apiClass.constructors) {
            if (hasRuntimeConstructor(runtimeClass, constructor)) {
                constructors.add(constructor);
            }
        }

        List<ApiMethod> staticMethods = new ArrayList<ApiMethod>();
        for (ApiMethod method : apiClass.staticMethods) {
            if (hasRuntimeMethod(runtimeClass, method)) {
                staticMethods.add(method);
            }
        }

        List<ApiMethod> instanceMethods = new ArrayList<ApiMethod>();
        for (ApiMethod method : apiClass.instanceMethods) {
            if (hasRuntimeMethod(runtimeClass, method)) {
                instanceMethods.add(method);
            }
        }

        List<ApiField> staticFields = new ArrayList<ApiField>();
        for (ApiField field : apiClass.staticFields) {
            if (hasRuntimeField(runtimeClass, field)) {
                staticFields.add(field);
            }
        }

        List<ApiField> instanceFields = new ArrayList<ApiField>();
        for (ApiField field : apiClass.instanceFields) {
            if (hasRuntimeField(runtimeClass, field)) {
                instanceFields.add(field);
            }
        }

        return new ApiClass(apiClass.packageName, apiClass.simpleName, apiClass.qualifiedName, apiClass.superTypes,
                apiClass.isInterface, apiClass.isAbstract, constructors, staticMethods, instanceMethods, staticFields,
                instanceFields);
    }

    private static List<ApiClass> validateInheritedAgainstRuntime(List<ApiClass> apiClasses) {
        List<ApiClass> result = new ArrayList<ApiClass>();
        for (ApiClass apiClass : apiClasses) {
            result.add(validateMethodsAgainstRuntime(apiClass));
        }
        return result;
    }

    private static ApiClass validateMethodsAgainstRuntime(ApiClass apiClass) {
        Class<?> runtimeClass = loadRuntimeClass(apiClass.qualifiedName);
        if (runtimeClass == null) {
            return apiClass;
        }
        
        List<ApiMethod> staticMethods = new ArrayList<ApiMethod>();
        for (ApiMethod method : apiClass.staticMethods) {
            if (hasRuntimeMethod(runtimeClass, method)) {
                staticMethods.add(method);
            }
        }

        List<ApiMethod> instanceMethods = new ArrayList<ApiMethod>();
        for (ApiMethod method : apiClass.instanceMethods) {
            if (hasRuntimeMethod(runtimeClass, method)) {
                instanceMethods.add(method);
            }
        }

        return new ApiClass(apiClass.packageName, apiClass.simpleName, apiClass.qualifiedName, apiClass.superTypes,
                apiClass.isInterface, apiClass.isAbstract, apiClass.constructors, staticMethods, instanceMethods,
                apiClass.staticFields, apiClass.instanceFields);
    }

    private static List<ApiClass> resolveInheritedMembers(List<ApiClass> apiClasses) {
        Map<String, ApiClass> classIndex = new LinkedHashMap<String, ApiClass>();
        for (ApiClass apiClass : apiClasses) {
            classIndex.put(apiClass.qualifiedName, apiClass);
        }
        Map<String, List<String>> typeHierarchy = new LinkedHashMap<String, List<String>>();
        for (ApiClass apiClass : apiClasses) {
            typeHierarchy.put(apiClass.qualifiedName, apiClass.superTypes);
        }
        Map<String, ApiClass> resolved = new LinkedHashMap<String, ApiClass>();
        for (ApiClass apiClass : apiClasses) {
            resolveInheritedMembers(apiClass, classIndex, resolved, new LinkedHashSet<String>(), typeHierarchy);
        }
        return new ArrayList<ApiClass>(resolved.values());
    }

    private static ApiClass resolveInheritedMembers(ApiClass apiClass, Map<String, ApiClass> classIndex,
            Map<String, ApiClass> resolved, Set<String> visiting, Map<String, List<String>> typeHierarchy) {
        ApiClass existing = resolved.get(apiClass.qualifiedName);
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(apiClass.qualifiedName)) {
            return apiClass;
        }

        LinkedHashMap<String, ApiMethod> inheritedMethods = new LinkedHashMap<String, ApiMethod>();
        LinkedHashMap<String, ApiField> inheritedFields = new LinkedHashMap<String, ApiField>();
        for (String superType : apiClass.superTypes) {
            ApiClass superClass = classIndex.get(superType);
            if (superClass == null) {
                continue;
            }
            ApiClass resolvedSuper = resolveInheritedMembers(superClass, classIndex, resolved, visiting, typeHierarchy);
            mergeInheritedMethods(inheritedMethods, resolvedSuper.instanceMethods);
            mergeInheritedFields(inheritedFields, resolvedSuper.instanceFields);
        }

        mergeDeclaredMethods(inheritedMethods, apiClass.instanceMethods);
        mergeDeclaredFields(inheritedFields, apiClass.instanceFields);

        List<ApiMethod> instanceMethods = filterBridgeLikeMethods(new ArrayList<ApiMethod>(inheritedMethods.values()), typeHierarchy);
        instanceMethods = filterGenericInheritedMethods(apiClass.qualifiedName, instanceMethods);
        List<ApiField> instanceFields = new ArrayList<ApiField>(inheritedFields.values());
        sortMethods(instanceMethods);
        sortFields(instanceFields);

        ApiClass merged = new ApiClass(apiClass.packageName, apiClass.simpleName, apiClass.qualifiedName, apiClass.superTypes,
                apiClass.isInterface, apiClass.isAbstract, apiClass.constructors, apiClass.staticMethods, instanceMethods,
                apiClass.staticFields, instanceFields);
        visiting.remove(apiClass.qualifiedName);
        resolved.put(apiClass.qualifiedName, merged);
        return merged;
    }

    private static void mergeInheritedMethods(Map<String, ApiMethod> target, List<ApiMethod> methods) {
        for (ApiMethod method : methods) {
            String key = methodKey(method);
            if (!target.containsKey(key)) {
                target.put(key, method);
            }
        }
    }

    private static void mergeDeclaredMethods(Map<String, ApiMethod> target, List<ApiMethod> methods) {
        for (ApiMethod method : methods) {
            target.put(methodKey(method), method);
        }
    }

    private static void mergeInheritedFields(Map<String, ApiField> target, List<ApiField> fields) {
        for (ApiField field : fields) {
            if (!target.containsKey(field.name)) {
                target.put(field.name, field);
            }
        }
    }

    private static void mergeDeclaredFields(Map<String, ApiField> target, List<ApiField> fields) {
        for (ApiField field : fields) {
            target.put(field.name, field);
        }
    }

    private static String methodKey(ApiMethod method) {
        return method.name + "#" + signatureKey(method.paramTypes);
    }

    private static String signatureKey(List<ApiType> paramTypes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(paramTypes.get(i).canonicalName());
        }
        return sb.toString();
    }

private static List<ApiMethod> filterBridgeLikeMethods(List<ApiMethod> methods, Map<String, List<String>> typeHierarchy) {
        Map<String, List<ApiMethod>> byName = new LinkedHashMap<String, List<ApiMethod>>();
        for (ApiMethod method : methods) {
            List<ApiMethod> list = byName.get(method.name);
            if (list == null) {
                list = new ArrayList<ApiMethod>();
                byName.put(method.name, list);
            }
            list.add(method);
        }
        List<ApiMethod> result = new ArrayList<ApiMethod>();
        for (Map.Entry<String, List<ApiMethod>> entry : byName.entrySet()) {
            List<ApiMethod> overloads = entry.getValue();
            if (overloads.size() == 1) {
                result.add(overloads.get(0));
                continue;
            }
            Set<ApiMethod> bridgeLike = new LinkedHashSet<ApiMethod>();
            for (int i = 0; i < overloads.size(); i++) {
                ApiMethod m1 = overloads.get(i);
                for (int j = 0; j < overloads.size(); j++) {
                    if (i == j) continue;
                    ApiMethod m2 = overloads.get(j);
                    if (isBridgeLikeOverload(m1, m2, typeHierarchy)) {
                        bridgeLike.add(m1);
                    }
                }
            }
            for (ApiMethod method : overloads) {
                if (!bridgeLike.contains(method)) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    private static boolean isBridgeLikeOverload(ApiMethod bridgeCandidate, ApiMethod other, Map<String, List<String>> typeHierarchy) {
        if (bridgeCandidate.paramTypes.size() != other.paramTypes.size()) {
            return false;
        }
        if (isArrayOverload(bridgeCandidate, other)) {
            return false;
        }
        boolean hasLessSpecificParam = false;
        for (int i = 0; i < bridgeCandidate.paramTypes.size(); i++) {
            String bridgeParam = bridgeCandidate.paramTypes.get(i).baseName;
            String otherParam = other.paramTypes.get(i).baseName;
            if (!bridgeParam.equals(otherParam)) {
                if ("java.lang.Object".equals(bridgeParam)) {
                    hasLessSpecificParam = true;
                } else if (isSubtype(otherParam, bridgeParam, typeHierarchy)) {
                    hasLessSpecificParam = true;
                } else {
                    return false;
                }
            }
        }
        return hasLessSpecificParam;
    }

    private static boolean isArrayOverload(ApiMethod m1, ApiMethod m2) {
        if (m1.paramTypes.size() != m2.paramTypes.size()) {
            return false;
        }
        for (int i = 0; i < m1.paramTypes.size(); i++) {
            ApiType p1 = m1.paramTypes.get(i);
            ApiType p2 = m2.paramTypes.get(i);
            if (p1.arrayDepth > 0 && p2.arrayDepth > 0 && !p1.baseName.equals(p2.baseName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSubtype(String subTypeName, String superTypeName, Map<String, List<String>> typeHierarchy) {
        if (subTypeName.equals(superTypeName)) {
            return false;
        }
        return isSubtype(subTypeName, superTypeName, typeHierarchy, new LinkedHashSet<String>());
    }

    private static boolean isSubtype(String subTypeName, String superTypeName, Map<String, List<String>> typeHierarchy, Set<String> visited) {
        if (subTypeName.equals(superTypeName)) {
            return true;
        }
        if (visited.contains(subTypeName)) {
            return false;
        }
        visited.add(subTypeName);
        List<String> superTypes = typeHierarchy.get(subTypeName);
        if (superTypes == null) {
            return false;
        }
        for (String superType : superTypes) {
            if (superType.equals(superTypeName)) {
                return true;
            }
            if (isSubtype(superType, superTypeName, typeHierarchy, visited)) {
                return true;
            }
        }
        return false;
    }

    private static List<ApiMethod> filterGenericInheritedMethods(String qualifiedName, List<ApiMethod> methods) {
        List<ApiMethod> result = new ArrayList<ApiMethod>();
        for (ApiMethod method : methods) {
            if (isGenericInheritedMethod(qualifiedName, method)) {
                continue;
            }
            result.add(method);
        }
        return result;
    }

    private static boolean isGenericInheritedMethod(String qualifiedName, ApiMethod method) {
        if (method.paramTypes.size() == 1 && "java.lang.Object".equals(method.paramTypes.get(0).baseName)) {
            if ("setPluginEventResponse".equals(method.name)) {
                return qualifiedName.startsWith("com.codename1.plugin.event.");
            }
        }
        if (method.paramTypes.size() == 1 && "com.codename1.ui.events.ActionEvent".equals(method.paramTypes.get(0).baseName)) {
            if ("actionPerformed".equals(method.name)) {
                return "com.codename1.plugin.Plugin".equals(qualifiedName);
            }
        }
        return false;
    }

    private static boolean hasRuntimeConstructor(Class<?> runtimeClass, ApiConstructor constructor) {
        Class<?>[] paramTypes = runtimeTypes(constructor.paramTypes);
        if (paramTypes == null) {
            return false;
        }
        if (isUnsupportedConstructor(runtimeClass, paramTypes)) {
            return false;
        }
        try {
            Constructor<?> runtimeConstructor = runtimeClass.getConstructor(paramTypes);
            return java.lang.reflect.Modifier.isPublic(runtimeConstructor.getModifiers())
                    && throwsOnlySupportedExceptions(runtimeConstructor.getExceptionTypes());
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static boolean isUnsupportedConstructor(Class<?> runtimeClass, Class<?>[] paramTypes) {
        String className = runtimeClass.getName();
        if ("java.lang.String".equals(className) && paramTypes.length == 1 && paramTypes[0] == StringBuffer.class) {
            return true;
        }
        if ("java.lang.String".equals(className) && paramTypes.length == 1 && paramTypes[0] == StringBuilder.class) {
            return true;
        }
        return false;
    }

    private static boolean hasRuntimeMethod(Class<?> runtimeClass, ApiMethod method) {
        if (!isSupportedJavaMethod(runtimeClass.getName(), method)) {
            return false;
        }
        Class<?>[] paramTypes = runtimeTypes(method.paramTypes);
        Class<?> returnType = runtimeType(method.returnType);
        if (paramTypes == null || (returnType == null && !method.returnType.isVoid())) {
            return false;
        }
        try {
            Method runtimeMethod = runtimeClass.getMethod(method.name, paramTypes);
            if (java.lang.reflect.Modifier.isStatic(runtimeMethod.getModifiers()) != method.isStatic) {
                return false;
            }
            if (runtimeMethod.isBridge()) {
                return false;
            }
            if (hasGenericTypeParameters(runtimeMethod, paramTypes)) {
                return false;
            }
            return (returnType == null || runtimeMethod.getReturnType() == returnType)
                    && throwsOnlySupportedExceptions(runtimeMethod.getExceptionTypes());
        } catch (NoSuchMethodException ex) {
            return false;
        }
    }

    private static boolean hasGenericTypeParameters(Method method, Class<?>[] paramTypes) {
        java.lang.reflect.Type[] genericParamTypes = method.getGenericParameterTypes();
        if (genericParamTypes.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i] == Object.class && genericParamTypes[i] instanceof java.lang.reflect.TypeVariable) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRuntimeField(Class<?> runtimeClass, ApiField field) {
        Class<?> fieldType = runtimeType(field.type);
        if (fieldType == null) {
            return false;
        }
        try {
            Field runtimeField = runtimeClass.getField(field.name);
            return java.lang.reflect.Modifier.isStatic(runtimeField.getModifiers()) == field.isStatic
                    && runtimeField.getType() == fieldType;
        } catch (NoSuchFieldException ex) {
            return false;
        }
    }

    private static Class<?>[] runtimeTypes(List<ApiType> types) {
        Class<?>[] out = new Class<?>[types.size()];
        for (int i = 0; i < types.size(); i++) {
            out[i] = runtimeType(types.get(i));
            if (out[i] == null) {
                return null;
            }
        }
        return out;
    }

    private static Class<?> runtimeType(ApiType type) {
        if (type == null) {
            return null;
        }
        if (type.isVoid()) {
            return Void.TYPE;
        }
        if (type.arrayDepth == 0) {
            return runtimeBaseType(type.baseName);
        }
        Class<?> componentType = runtimeBaseType(type.baseName);
        if (componentType == null) {
            return null;
        }
        int[] dimensions = new int[type.arrayDepth];
        return Array.newInstance(componentType, dimensions).getClass();
    }

    private static Class<?> runtimeBaseType(String name) {
        if ("boolean".equals(name)) {
            return Boolean.TYPE;
        }
        if ("byte".equals(name)) {
            return Byte.TYPE;
        }
        if ("char".equals(name)) {
            return Character.TYPE;
        }
        if ("short".equals(name)) {
            return Short.TYPE;
        }
        if ("int".equals(name)) {
            return Integer.TYPE;
        }
        if ("long".equals(name)) {
            return Long.TYPE;
        }
        if ("float".equals(name)) {
            return Float.TYPE;
        }
        if ("double".equals(name)) {
            return Double.TYPE;
        }
        return loadRuntimeClass(name);
    }

    private static boolean throwsOnlySupportedExceptions(Class<?>[] exceptionTypes) {
        for (Class<?> exceptionType : exceptionTypes) {
            if (Throwable.class == exceptionType) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSupportedJavaClass(String qualifiedName) {
        return !"java.lang.Record".equals(qualifiedName)
                && !"java.lang.AbstractMethodError".equals(qualifiedName)
                && !"java.lang.IllegalThreadStateException".equals(qualifiedName)
                && !"java.lang.SuppressWarnings".equals(qualifiedName)
                && !"java.util.EventListenerProxy".equals(qualifiedName);
    }

    private static boolean isSupportedJavaMethod(String qualifiedName, ApiMethod method) {
        return !("java.lang.Class".equals(qualifiedName) && "isRecord".equals(method.name) && method.paramTypes.isEmpty())
                && !("java.lang.Class".equals(qualifiedName)
                && ("isAnnotation".equals(method.name) || "isAnonymousClass".equals(method.name))
                && method.paramTypes.isEmpty())
                && !("java.lang.System".equals(qualifiedName) && "exit".equals(method.name) && method.isStatic
                && method.paramTypes.size() == 1 && "int".equals(method.paramTypes.get(0).baseName))
                && !("java.lang.reflect.Array".equals(qualifiedName) && "newInstance".equals(method.name) && method.isStatic
                && method.paramTypes.size() == 2 && "java.lang.Class".equals(method.paramTypes.get(0).baseName)
                && "int".equals(method.paramTypes.get(1).baseName) && method.paramTypes.get(1).arrayDepth == 1)
                && !("java.util.Collections".equals(qualifiedName)
                && ("asLifoQueue".equals(method.name)
                || "synchronizedSortedMap".equals(method.name)
                || "synchronizedSortedSet".equals(method.name)
                || "unmodifiableSortedMap".equals(method.name)
                || "unmodifiableSortedSet".equals(method.name)))
                && !("java.util.TimerTask".equals(qualifiedName) && "scheduledExecutionTime".equals(method.name)
                && method.paramTypes.isEmpty())
                && !("java.io.PrintStream".equals(qualifiedName) && "print".equals(method.name)
                && method.paramTypes.size() == 1
                && ("boolean".equals(method.paramTypes.get(0).baseName)
                || "float".equals(method.paramTypes.get(0).baseName)))
                && !("java.util.TimeZone".equals(qualifiedName) && "setDefault".equals(method.name) && method.isStatic
                && method.paramTypes.size() == 1 && "java.util.TimeZone".equals(method.paramTypes.get(0).baseName));
    }

    private static Class<?> loadRuntimeClass(String canonicalName) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        Class<?> loaded = tryLoadRuntimeClass(contextLoader, canonicalName);
        if (loaded != null) {
            return loaded;
        }
        loaded = tryLoadRuntimeClass(GenerateCN1AccessRegistry.class.getClassLoader(), canonicalName);
        if (loaded != null) {
            return loaded;
        }
        return tryLoadRuntimeClass(null, canonicalName);
    }

    private static Class<?> tryLoadRuntimeClass(ClassLoader loader, String canonicalName) {
        try {
            return loader == null ? Class.forName(canonicalName) : Class.forName(canonicalName, false, loader);
        } catch (Throwable ex) {
            String attempt = canonicalName;
            int dot = attempt.lastIndexOf('.');
            while (dot > 0) {
                attempt = attempt.substring(0, dot) + "$" + attempt.substring(dot + 1);
                try {
                    return loader == null ? Class.forName(attempt) : Class.forName(attempt, false, loader);
                } catch (Throwable ignore) {
                    dot = attempt.lastIndexOf('.', dot - 1);
                }
            }
            return null;
        }
    }

    private static ApiConstructor parseConstructor(SourceClass sourceClass, MethodTree methodTree, Resolver resolver) {
        if (!methodTree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
            return null;
        }
        ApiSignature signature = resolveSignature(methodTree.getParameters(), resolver);
        return signature == null ? null : new ApiConstructor(signature.paramTypes, signature.varArgs);
    }

    private static ApiMethod parseMethod(SourceClass sourceClass, MethodTree methodTree, Resolver resolver, boolean enclosingInterface) {
        if (!isPublicMethod(methodTree, enclosingInterface) || isBlacklistedMethod(methodTree.getName().toString())) {
            return null;
        }
        ApiType returnType = resolver.resolveType(methodTree.getReturnType().toString());
        if (!isSupportedType(returnType)) {
            return null;
        }
        ApiSignature signature = resolveSignature(methodTree.getParameters(), resolver);
        if (signature == null) {
            return null;
        }
        boolean isStatic = methodTree.getModifiers().getFlags().contains(Modifier.STATIC);
        return new ApiMethod(methodTree.getName().toString(), returnType, signature.paramTypes, signature.varArgs, isStatic);
    }

    private static ApiField parseField(SourceClass sourceClass, VariableTree variableTree, Resolver resolver,
            boolean enclosingInterface, boolean enclosingEnum) {
        Set<Modifier> modifiers = variableTree.getModifiers().getFlags();
        boolean isEnumConstant = enclosingEnum && variableTree.getType() == null;
        boolean isPublic = modifiers.contains(Modifier.PUBLIC) || (enclosingInterface && !modifiers.contains(Modifier.PRIVATE));
        if (!isPublic && !isEnumConstant) {
            return null;
        }
        ApiType type = isEnumConstant
                ? new ApiType(sourceClass.qualifiedName, 0, false)
                : resolver.resolveType(variableTree.getType().toString());
        if (!isSupportedType(type)) {
            return null;
        }
        boolean isStatic = isEnumConstant || modifiers.contains(Modifier.STATIC) || enclosingInterface;
        boolean writable = !isEnumConstant && !modifiers.contains(Modifier.FINAL) && !enclosingInterface;
        if ("TYPE".equals(variableTree.getName().toString())) {
            return null;
        }
        return new ApiField(variableTree.getName().toString(), type, isStatic, writable);
    }

    private static ApiSignature resolveSignature(List<? extends VariableTree> parameters, Resolver resolver) {
        List<ApiType> paramTypes = new ArrayList<ApiType>();
        boolean varArgs = false;
        for (int i = 0; i < parameters.size(); i++) {
            VariableTree parameter = parameters.get(i);
            ApiType type = resolver.resolveParameter(parameter.toString());
            if (!isSupportedType(type)) {
                return null;
            }
            if (type.varArgs) {
                if (i != parameters.size() - 1) {
                    return null;
                }
                varArgs = true;
            }
            paramTypes.add(type);
        }
        return new ApiSignature(paramTypes, varArgs);
    }

    private static boolean isTopLevelPublicType(ClassTree classTree) {
        return isNamedType(classTree) && classTree.getModifiers().getFlags().contains(Modifier.PUBLIC);
    }

    private static boolean isNestedPublicType(ClassTree nested, ClassTree enclosing) {
        if (!isNamedType(nested)) {
            return false;
        }
        if (nested.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
            return true;
        }
        return enclosing.getKind() == Tree.Kind.INTERFACE || enclosing.getKind() == Tree.Kind.ANNOTATION_TYPE;
    }

    private static boolean isNamedType(ClassTree classTree) {
        String name = classTree.getSimpleName().toString();
        return name != null && name.length() > 0;
    }

    private static boolean isPublicMethod(MethodTree methodTree, boolean enclosingInterface) {
        Set<Modifier> modifiers = methodTree.getModifiers().getFlags();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED)) {
            return false;
        }
        return modifiers.contains(Modifier.PUBLIC) || enclosingInterface;
    }

    private static boolean isDispatchClass(ApiClass apiClass) {
        return apiClass.packageName.startsWith("com.codename1.")
                || isSupportedJavaDispatchPackage(apiClass.packageName)
                || "com.codenameone.playground".equals(apiClass.packageName)
                || apiClass.packageName.startsWith("com.codenameone.playground.");
    }

    private static boolean isSupportedJavaDispatchPackage(String packageName) {
        return packageName.startsWith("java.")
                && !isPackageOrChild(packageName, "java.lang.annotation")
                && !isPackageOrChild(packageName, "java.lang.invoke")
                && !isPackageOrChild(packageName, "java.time")
                && !isPackageOrChild(packageName, "java.util.function")
                && !isPackageOrChild(packageName, "java.util.stream");
    }

    private static boolean isSupportedType(ApiType type) {
        if (type == null) {
            return false;
        }
        if (type.isPrimitive()) {
            return true;
        }
        String name = type.baseName;
        if ("void".equals(name)) {
            return true;
        }
        return !name.startsWith("java.lang.reflect.")
                && !name.startsWith("java.lang.annotation.")
                && !name.startsWith("java.lang.invoke.")
                && !name.startsWith("java.lang.constant.")
                && !isPackageOrChild(name, "java.time")
                && !name.startsWith("java.io.")
                && !name.startsWith("java.net.")
                && !name.startsWith("java.nio.")
                && !name.startsWith("java.security.")
                && !name.startsWith("java.util.concurrent.")
                && !name.startsWith("java.util.function.")
                && !name.startsWith("java.util.stream.");
    }

    private static boolean isPackageOrChild(String name, String packageName) {
        return name.equals(packageName) || name.startsWith(packageName + ".");
    }

    private static void sortConstructors(List<ApiConstructor> constructors) {
        Collections.sort(constructors, new Comparator<ApiConstructor>() {
            public int compare(ApiConstructor a, ApiConstructor b) {
                return compareSignatures(a.paramTypes, b.paramTypes);
            }
        });
    }

    private static void sortMethods(List<ApiMethod> methods) {
        Collections.sort(methods, new Comparator<ApiMethod>() {
            public int compare(ApiMethod a, ApiMethod b) {
                int name = a.name.compareTo(b.name);
                return name != 0 ? name : compareSignatures(a.paramTypes, b.paramTypes);
            }
        });
    }

    private static void sortFields(List<ApiField> fields) {
        Collections.sort(fields, new Comparator<ApiField>() {
            public int compare(ApiField a, ApiField b) {
                return a.name.compareTo(b.name);
            }
        });
    }

    private static List<ApiClass> orderInstanceDispatchClasses(List<ApiClass> classes) {
        List<ApiClass> ordered = new ArrayList<ApiClass>(classes);
        final Map<String, ApiClass> classIndex = new LinkedHashMap<String, ApiClass>();
        for (ApiClass apiClass : classes) {
            classIndex.put(apiClass.qualifiedName, apiClass);
        }
        Collections.sort(ordered, new Comparator<ApiClass>() {
            public int compare(ApiClass a, ApiClass b) {
                if (a.isInterface != b.isInterface) {
                    return a.isInterface ? 1 : -1;
                }
                int depthCompare = hierarchyDepth(b, classIndex, new LinkedHashSet<String>())
                        - hierarchyDepth(a, classIndex, new LinkedHashSet<String>());
                if (depthCompare != 0) {
                    return depthCompare;
                }
                return a.qualifiedName.compareTo(b.qualifiedName);
            }
        });
        return ordered;
    }

    private static int hierarchyDepth(ApiClass type, Map<String, ApiClass> classIndex, Set<String> visited) {
        int best = 0;
        for (String superType : type.superTypes) {
            if (!visited.add(superType)) {
                continue;
            }
            ApiClass superClass = classIndex.get(superType);
            if (superClass != null && !superClass.isInterface) {
                best = Math.max(best, 1 + hierarchyDepth(superClass, classIndex, visited));
            }
        }
        return best;
    }

    private static boolean isSubtypeOf(ApiClass type, String candidateSupertype, Map<String, ApiClass> classIndex,
            Set<String> visited) {
        for (String superType : type.superTypes) {
            if (!visited.add(superType)) {
                continue;
            }
            if (candidateSupertype.equals(superType)) {
                return true;
            }
            ApiClass superClass = classIndex.get(superType);
            if (superClass != null && isSubtypeOf(superClass, candidateSupertype, classIndex, visited)) {
                return true;
            }
        }
        return false;
    }

    private static int compareSignatures(List<ApiType> a, List<ApiType> b) {
        if (a.size() != b.size()) {
            return a.size() - b.size();
        }
        for (int i = 0; i < a.size(); i++) {
            int compare = compareTypesBySpecificity(a.get(i), b.get(i));
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

    private static int compareTypesBySpecificity(ApiType a, ApiType b) {
        if (a.arrayDepth != b.arrayDepth) {
            return a.arrayDepth - b.arrayDepth;
        }
        if (a.isPrimitive() || b.isPrimitive()) {
            if (a.isPrimitive() && b.isPrimitive()) {
                return a.baseName.compareTo(b.baseName);
            }
            return a.isPrimitive() ? -1 : 1;
        }
        Class<?> classA = loadRuntimeClass(a.canonicalName());
        Class<?> classB = loadRuntimeClass(b.canonicalName());
        if (classA != null && classB != null) {
            if (classA.isAssignableFrom(classB)) {
                return 1;
            }
            if (classB.isAssignableFrom(classA)) {
                return -1;
            }
        }
        String nameA = a.canonicalName();
        String nameB = b.canonicalName();
        if ("java.lang.Object".equals(nameA)) {
            return 1;
        }
        if ("java.lang.Object".equals(nameB)) {
            return -1;
        }
        return nameA.compareTo(nameB);
    }

    private static void writeRootRegistry(File output, Discovery discovery) throws IOException {
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create " + parent);
        }
        try (Writer writer = new FileWriter(output)) {
            writer.write("package " + ROOT_PACKAGE + ";\n\n");
            writer.write("import com.codename1.ui.Form;\n");
            writer.write("import com.codenameone.playground.PlaygroundContext;\n");
            writer.write("import java.util.LinkedHashMap;\n");
            writer.write("import java.util.Map;\n");
            for (GeneratedPackage generatedPackage : discovery.packages) {
                writer.write("import " + HELPER_PACKAGE + "." + generatedPackage.helperClassName + ";\n");
            }
            if (!discovery.packages.isEmpty()) {
                writer.write("\n");
            }
            writer.write("/**\n");
            writer.write(" * Generated registry. Re-run tools/generate-cn1-access-registry.sh after updating the CN1 sources.\n");
            writer.write(" */\n");
            writer.write("public final class " + ROOT_CLASS_NAME + " implements CN1Access {\n");
            writer.write("    public static final " + ROOT_CLASS_NAME + " INSTANCE = new " + ROOT_CLASS_NAME + "();\n\n");
            writeRootClassIndex(writer, discovery.packages);
            writer.write("    private " + ROOT_CLASS_NAME + "() {\n");
            writer.write("    }\n\n");
            writeRootFindClass(writer, discovery.packages);
            writeRootConstruct(writer, discovery.packages);
            writeRootInvokeStatic(writer, discovery.packages);
            writeRootInvoke(writer, discovery.packages);
            writeRootGetStaticField(writer, discovery.packages);
            writeRootGetField(writer, discovery.packages);
            writeRootSetStaticField(writer, discovery.packages);
            writeRootSetField(writer, discovery.packages);
            writeRootHelpers(writer);
            writer.write("}\n");
        }
    }

    private static void writePackageHelpers(File helperDir, Discovery discovery) throws IOException {
        for (GeneratedPackage generatedPackage : discovery.packages) {
            File output = new File(helperDir, generatedPackage.helperClassName + ".java");
            try (Writer writer = new FileWriter(output)) {
                writer.write("package " + HELPER_PACKAGE + ";\n\n");
                writer.write("import bsh.cn1.CN1AccessException;\n\n");
                writer.write("public final class " + generatedPackage.helperClassName + " {\n");
                writer.write("    private " + generatedPackage.helperClassName + "() {\n");
                writer.write("    }\n\n");
                writeFindClass(writer, generatedPackage.classes);
                writeConstruct(writer, generatedPackage.classes);
                writeInvokeStatic(writer, generatedPackage.classes);
                writeInvoke(writer, generatedPackage.classes);
                writeGetStaticField(writer, generatedPackage.classes);
                writeGetField(writer, generatedPackage.classes);
                writeSetStaticField(writer, generatedPackage.classes);
                writeSetField(writer, generatedPackage.classes);
                writeHelpers(writer, collectSamInterfaces(discovery));
                writer.write("}\n");
            }
        }
    }

    private static void writeRootFindClass(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Class<?> findClass(String name) {\n");
        writer.write("        if (name == null) {\n");
        writer.write("            return null;\n");
        writer.write("        }\n");
        writer.write("        return CLASS_INDEX.get(name);\n");
        writer.write("    }\n\n");
        writer.write("    public String[] getIndexedClassNames() {\n");
        writer.write("        return INDEXED_CLASS_NAMES.clone();\n");
        writer.write("    }\n\n");
        writer.write("    public String[] getMethodSignatures(String name) {\n");
        writer.write("        return copyStrings(METHOD_INDEX.get(name));\n");
        writer.write("    }\n\n");
        writer.write("    public String[] getFieldNames(String name) {\n");
        writer.write("        return copyStrings(FIELD_INDEX.get(name));\n");
        writer.write("    }\n\n");
    }

    private static void writeRootClassIndex(Writer writer, List<GeneratedPackage> packages) throws IOException {
        List<ApiClass> classes = new ArrayList<ApiClass>();
        for (GeneratedPackage generatedPackage : packages) {
            classes.addAll(generatedPackage.classes);
        }
        writer.write("    private static final String[] EMPTY_STRINGS = new String[0];\n\n");
        writer.write("    private static final String[] INDEXED_CLASS_NAMES = new String[]{\n");
        for (int i = 0; i < classes.size(); i++) {
            ApiClass apiClass = classes.get(i);
            writer.write("        \"" + apiClass.qualifiedName + "\"");
            writer.write(i + 1 < classes.size() ? ",\n" : "\n");
        }
        writer.write("    };\n\n");
        writer.write("    private static final Map<String, Class<?>> CLASS_INDEX = buildClassIndex();\n\n");
        writer.write("    private static final Map<String, String[]> METHOD_INDEX = buildMethodIndex();\n\n");
        writer.write("    private static final Map<String, String[]> FIELD_INDEX = buildFieldIndex();\n\n");
        writer.write("    private static Map<String, Class<?>> buildClassIndex() {\n");
        writer.write("        Map<String, Class<?>> index = new LinkedHashMap<String, Class<?>>();\n");
        int chunkCount = (classes.size() + FIND_CLASS_CHUNK_SIZE - 1) / FIND_CLASS_CHUNK_SIZE;
        for (int i = 0; i < chunkCount; i++) {
            writer.write("        fillClassIndex" + i + "(index);\n");
        }
        writer.write("        return index;\n");
        writer.write("    }\n");
        for (int i = 0; i < chunkCount; i++) {
            int fromIndex = i * FIND_CLASS_CHUNK_SIZE;
            int toIndex = Math.min(classes.size(), fromIndex + FIND_CLASS_CHUNK_SIZE);
            writer.write("\n");
            writer.write("    private static void fillClassIndex" + i + "(Map<String, Class<?>> index) {\n");
            for (int j = fromIndex; j < toIndex; j++) {
                ApiClass apiClass = classes.get(j);
                writer.write("        index.put(\"" + apiClass.qualifiedName + "\", " + typeLiteral(apiClass.qualifiedName) + ");\n");
            }
            writer.write("    }\n");
        }
        writer.write("\n");
        writer.write("    private static Map<String, String[]> buildMethodIndex() {\n");
        writer.write("        Map<String, String[]> index = new LinkedHashMap<String, String[]>();\n");
        for (int i = 0; i < chunkCount; i++) {
            writer.write("        fillMethodIndex" + i + "(index);\n");
        }
        writer.write("        return index;\n");
        writer.write("    }\n");
        for (int i = 0; i < chunkCount; i++) {
            int fromIndex = i * FIND_CLASS_CHUNK_SIZE;
            int toIndex = Math.min(classes.size(), fromIndex + FIND_CLASS_CHUNK_SIZE);
            writer.write("\n");
            writer.write("    private static void fillMethodIndex" + i + "(Map<String, String[]> index) {\n");
            for (int j = fromIndex; j < toIndex; j++) {
                ApiClass apiClass = classes.get(j);
                List<String> methods = collectEditorMethodSignatures(apiClass);
                writer.write("        index.put(\"" + apiClass.qualifiedName + "\", splitMembers(\""
                        + escape(joinMembers(methods)) + "\"));\n");
            }
            writer.write("    }\n");
        }
        writer.write("\n");
        writer.write("    private static Map<String, String[]> buildFieldIndex() {\n");
        writer.write("        Map<String, String[]> index = new LinkedHashMap<String, String[]>();\n");
        for (int i = 0; i < chunkCount; i++) {
            writer.write("        fillFieldIndex" + i + "(index);\n");
        }
        writer.write("        return index;\n");
        writer.write("    }\n");
        for (int i = 0; i < chunkCount; i++) {
            int fromIndex = i * FIND_CLASS_CHUNK_SIZE;
            int toIndex = Math.min(classes.size(), fromIndex + FIND_CLASS_CHUNK_SIZE);
            writer.write("\n");
            writer.write("    private static void fillFieldIndex" + i + "(Map<String, String[]> index) {\n");
            for (int j = fromIndex; j < toIndex; j++) {
                ApiClass apiClass = classes.get(j);
                List<String> fields = collectEditorFields(apiClass);
                writer.write("        index.put(\"" + apiClass.qualifiedName + "\", splitMembers(\""
                        + escape(joinMembers(fields)) + "\"));\n");
            }
            writer.write("    }\n");
        }
        writer.write("\n");
    }

    private static void writeRootConstruct(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Object construct(Class<?> type, Object[] args) throws Exception {\n");
        writer.write("        String packageName = packageName(type);\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        if (\"" + generatedPackage.packageName + "\".equals(packageName)) {\n");
            writer.write("            return " + generatedPackage.helperClassName + ".construct(type, args);\n");
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedConstruct(type, args);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootInvokeStatic(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {\n");
        writer.write("        String packageName = packageName(type);\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        if (\"" + generatedPackage.packageName + "\".equals(packageName)) {\n");
            writer.write("            return " + generatedPackage.helperClassName + ".invokeStatic(type, name, args);\n");
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedStatic(type, name, args);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootInvoke(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Object invoke(Object target, String name, Object[] args) throws Exception {\n");
        writer.write("        if (interceptShownForm(target, name, args)) {\n");
        writer.write("            return null;\n");
        writer.write("        }\n");
        writer.write("        CN1AccessException unsupported = null;\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        try {\n");
            writer.write("            return " + generatedPackage.helperClassName + ".invoke(target, name, args);\n");
            writer.write("        } catch (CN1AccessException ex) {\n");
            writer.write("            unsupported = ex;\n");
            writer.write("        }\n");
        }
        writer.write("        if (unsupported != null) {\n");
        writer.write("            throw unsupported;\n");
        writer.write("        }\n");
        writer.write("        throw unsupportedInstance(target, name, args);\n");
        writer.write("    }\n\n");
        writer.write("    private static boolean interceptShownForm(Object target, String name, Object[] args) {\n");
        writer.write("        PlaygroundContext context = PlaygroundContext.getCurrent();\n");
        writer.write("        if (context == null || !(target instanceof Form) || !\"show\".equals(name)) {\n");
        writer.write("            return false;\n");
        writer.write("        }\n");
        writer.write("        if (args != null && args.length != 0) {\n");
        writer.write("            return false;\n");
        writer.write("        }\n");
        writer.write("        context.captureShownForm((Form) target);\n");
        writer.write("        return true;\n");
        writer.write("    }\n\n");
    }

    private static void writeRootGetStaticField(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Object getStaticField(Class<?> type, String name) throws Exception {\n");
        writer.write("        String packageName = packageName(type);\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        if (\"" + generatedPackage.packageName + "\".equals(packageName)) {\n");
            writer.write("            return " + generatedPackage.helperClassName + ".getStaticField(type, name);\n");
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedStaticField(type, name);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootGetField(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public Object getField(Object target, String name) throws Exception {\n");
        writer.write("        CN1AccessException unsupported = null;\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        try {\n");
            writer.write("            return " + generatedPackage.helperClassName + ".getField(target, name);\n");
            writer.write("        } catch (CN1AccessException ex) {\n");
            writer.write("            unsupported = ex;\n");
            writer.write("        }\n");
        }
        writer.write("        if (unsupported != null) {\n");
        writer.write("            throw unsupported;\n");
        writer.write("        }\n");
        writer.write("        throw unsupportedField(target, name);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootSetStaticField(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public void setStaticField(Class<?> type, String name, Object value) throws Exception {\n");
        writer.write("        String packageName = packageName(type);\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        if (\"" + generatedPackage.packageName + "\".equals(packageName)) {\n");
            writer.write("            " + generatedPackage.helperClassName + ".setStaticField(type, name, value);\n");
            writer.write("            return;\n");
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedStaticFieldWrite(type, name, value);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootSetField(Writer writer, List<GeneratedPackage> packages) throws IOException {
        writer.write("    @Override\n");
        writer.write("    public void setField(Object target, String name, Object value) throws Exception {\n");
        writer.write("        CN1AccessException unsupported = null;\n");
        for (GeneratedPackage generatedPackage : packages) {
            writer.write("        try {\n");
            writer.write("            " + generatedPackage.helperClassName + ".setField(target, name, value);\n");
            writer.write("            return;\n");
            writer.write("        } catch (CN1AccessException ex) {\n");
            writer.write("            unsupported = ex;\n");
            writer.write("        }\n");
        }
        writer.write("        if (unsupported != null) {\n");
        writer.write("            throw unsupported;\n");
        writer.write("        }\n");
        writer.write("        throw unsupportedFieldWrite(target, name, value);\n");
        writer.write("    }\n\n");
    }

    private static void writeRootHelpers(Writer writer) throws IOException {
        writer.write("    private static String packageName(Class<?> type) {\n");
        writer.write("        String name = type.getName();\n");
        writer.write("        int lastDot = name.lastIndexOf('.');\n");
        writer.write("        return lastDot < 0 ? \"\" : name.substring(0, lastDot);\n");
        writer.write("    }\n\n");
        writer.write("    private static String packageName(String name) {\n");
        writer.write("        if (name == null) {\n");
        writer.write("            return null;\n");
        writer.write("        }\n");
        writer.write("        int lastDot = name.lastIndexOf('.');\n");
        writer.write("        return lastDot < 0 ? null : name.substring(0, lastDot);\n");
        writer.write("    }\n\n");
        writer.write("    private static String simpleName(String name) {\n");
        writer.write("        if (name == null) {\n");
        writer.write("            return null;\n");
        writer.write("        }\n");
        writer.write("        int lastDot = name.lastIndexOf('.');\n");
        writer.write("        return lastDot < 0 || lastDot == name.length() - 1 ? null : name.substring(lastDot + 1);\n");
        writer.write("    }\n\n");
        writer.write("    private static String[] copyStrings(String[] values) {\n");
        writer.write("        return values == null ? EMPTY_STRINGS : values.clone();\n");
        writer.write("    }\n\n");
        writer.write("    private static String[] splitMembers(String data) {\n");
        writer.write("        if (data == null || data.length() == 0) {\n");
        writer.write("            return EMPTY_STRINGS;\n");
        writer.write("        }\n");
        writer.write("        int count = 1;\n");
        writer.write("        for (int i = 0; i < data.length(); i++) {\n");
        writer.write("            if (data.charAt(i) == '\\u001f') {\n");
        writer.write("                count++;\n");
        writer.write("            }\n");
        writer.write("        }\n");
        writer.write("        String[] out = new String[count];\n");
        writer.write("        int start = 0;\n");
        writer.write("        int index = 0;\n");
        writer.write("        for (int i = 0; i <= data.length(); i++) {\n");
        writer.write("            if (i == data.length() || data.charAt(i) == '\\u001f') {\n");
        writer.write("                out[index++] = data.substring(start, i);\n");
        writer.write("                start = i + 1;\n");
        writer.write("            }\n");
        writer.write("        }\n");
        writer.write("        return out;\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated constructor dispatch not implemented for \" + type.getName() + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated static dispatch not implemented for \" + type.getName() + \".\" + name + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated instance dispatch not implemented for \" + target.getClass().getName() + \".\" + name + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {\n");
        writer.write("        return new CN1AccessException(\"Generated static field access not implemented for \" + type.getName() + \".\" + name);\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedField(Object target, String name) {\n");
        writer.write("        return new CN1AccessException(\"Generated field access not implemented for \" + target.getClass().getName() + \".\" + name);\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {\n");
        writer.write("        return new CN1AccessException(\"Generated static field write not implemented for \" + type.getName() + \".\" + name + \" value=\" + describeValue(value));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {\n");
        writer.write("        return new CN1AccessException(\"Generated field write not implemented for \" + target.getClass().getName() + \".\" + name + \" value=\" + describeValue(value));\n");
        writer.write("    }\n\n");
        writeDescribeHelpers(writer);
    }

    private static void writeFindClass(Writer writer, List<ApiClass> classes) throws IOException {
        writer.write("    public static Class<?> findClass(String name) {\n");
        writer.write("        int lastDot = name == null ? -1 : name.lastIndexOf('.');\n");
        writer.write("        if (lastDot < 0 || lastDot == name.length() - 1) {\n");
        writer.write("            return null;\n");
        writer.write("        }\n");
        writer.write("        return findClassBySimpleName(name.substring(lastDot + 1));\n");
        writer.write("    }\n\n");
        writer.write("    public static Class<?> findClassBySimpleName(String simpleName) {\n");
        writeFindClassStatements(writer, classes, "findClassChunk", "        ", "simpleName");
        writer.write("        return null;\n");
        writer.write("    }\n\n");
        writeFindClassChunkMethods(writer, classes, "findClassChunk", "    ");
    }

    private static void writeFindClassStatements(Writer writer, List<ApiClass> classes, String methodPrefix, String indent, String nameVar)
            throws IOException {
        int chunkCount = (classes.size() + FIND_CLASS_CHUNK_SIZE - 1) / FIND_CLASS_CHUNK_SIZE;
        for (int i = 0; i < chunkCount; i++) {
            writer.write(indent + "Class<?> found" + i + " = " + methodPrefix + i + "(" + nameVar + ");\n");
            writer.write(indent + "if (found" + i + " != null) {\n");
            writer.write(indent + "    return found" + i + ";\n");
            writer.write(indent + "}\n");
        }
    }

    private static void writeFindClassChunkMethods(Writer writer, List<ApiClass> classes, String methodPrefix, String indent)
            throws IOException {
        int chunkCount = (classes.size() + FIND_CLASS_CHUNK_SIZE - 1) / FIND_CLASS_CHUNK_SIZE;
        for (int i = 0; i < chunkCount; i++) {
            int fromIndex = i * FIND_CLASS_CHUNK_SIZE;
            int toIndex = Math.min(classes.size(), fromIndex + FIND_CLASS_CHUNK_SIZE);
            writer.write("\n");
            writer.write(indent + "private static Class<?> " + methodPrefix + i + "(String simpleName) {\n");
            for (int j = fromIndex; j < toIndex; j++) {
                ApiClass apiClass = classes.get(j);
                writer.write(indent + "    if (\"" + apiClass.simpleName + "\".equals(simpleName)) {\n");
                writer.write(indent + "        return " + typeLiteral(apiClass.qualifiedName) + ";\n");
                writer.write(indent + "    }\n");
            }
            writer.write(indent + "    return null;\n");
            writer.write(indent + "}\n");
        }
    }

    private static void writeConstruct(Writer writer, List<ApiClass> classes) throws IOException {
        writer.write("    public static Object construct(Class<?> type, Object[] args) throws Exception {\n");
        writer.write("        Object[] safeArgs = safeArgs(args);\n");
        for (ApiClass apiClass : classes) {
            if (apiClass.isLookupOnly() || apiClass.isInterface || apiClass.isAbstract) {
                continue;
            }
            if (apiClass.constructors.isEmpty()) {
                continue;
            }
            writer.write("        if (type == " + typeLiteral(apiClass.qualifiedName) + ") {\n");
            for (ApiConstructor constructor : apiClass.constructors) {
                writer.write("            if (matches(safeArgs, " + classArrayLiteral(constructor.paramTypes) + ", "
                        + constructor.varArgs + ")) {\n");
                writer.write("                Object[] adaptedArgs = adaptArgs(safeArgs, " + classArrayLiteral(constructor.paramTypes)
                        + ", " + constructor.varArgs + ");\n");
                if (constructor.varArgs && !constructor.paramTypes.isEmpty()) {
                    writeVarArgsInvocation(writer, "                ", constructor.paramTypes,
                            constructorCall(apiClass, constructor));
                } else {
                    writer.write("                " + constructorCall(apiClass, constructor) + "\n");
                }
                writer.write("            }\n");
            }
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedConstruct(type, safeArgs);\n");
        writer.write("    }\n\n");
    }

    private static List<String> collectEditorMethodSignatures(ApiClass apiClass) {
        if (!isEditorDetailedClass(apiClass)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> signatures = new LinkedHashSet<String>();
        for (ApiMethod method : apiClass.instanceMethods) {
            signatures.add(editorMethodSignature(method));
        }
        for (ApiMethod method : apiClass.staticMethods) {
            signatures.add(editorMethodSignature(method));
        }
        return new ArrayList<String>(signatures);
    }

    private static List<String> collectEditorFields(ApiClass apiClass) {
        if (!isEditorDetailedClass(apiClass)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (ApiField field : apiClass.instanceFields) {
            names.add(field.name);
        }
        for (ApiField field : apiClass.staticFields) {
            names.add(field.name);
        }
        return new ArrayList<String>(names);
    }

    private static String editorMethodSignature(ApiMethod method) {
        return method.paramTypes.isEmpty() && !method.varArgs ? method.name + "()" : method.name + "(...)";
    }

    private static String joinMembers(List<String> members) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                out.append(MEMBER_SEPARATOR);
            }
            out.append(members.get(i));
        }
        return out.toString();
    }

    private static boolean isEditorDetailedClass(ApiClass apiClass) {
        return apiClass.packageName.startsWith("com.codename1.ui")
                || apiClass.packageName.startsWith("com.codename1.components")
                || apiClass.packageName.startsWith("com.codenameone.playground");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeInvokeStatic(Writer writer, List<ApiClass> classes) throws IOException {
        writer.write("    public static Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {\n");
        writer.write("        Object[] safeArgs = safeArgs(args);\n");
        int helperIndex = 0;
        for (ApiClass apiClass : classes) {
            if (apiClass.isLookupOnly() || apiClass.staticMethods.isEmpty()) {
                continue;
            }
            writer.write("        if (type == " + typeLiteral(apiClass.qualifiedName) + ") return invokeStatic"
                    + helperIndex + "(name, safeArgs);\n");
            helperIndex++;
        }
        writer.write("        throw unsupportedStatic(type, name, safeArgs);\n");
        writer.write("    }\n\n");

        helperIndex = 0;
        for (ApiClass apiClass : classes) {
            if (apiClass.isLookupOnly() || apiClass.staticMethods.isEmpty()) {
                continue;
            }
            writer.write("    private static Object invokeStatic" + helperIndex + "(String name, Object[] safeArgs) throws Exception {\n");
            LinkedHashMap<String, List<ApiMethod>> methodsByName = groupMethodsByName(apiClass.staticMethods);
            for (Map.Entry<String, List<ApiMethod>> entry : methodsByName.entrySet()) {
                writer.write("        if (\"" + entry.getKey() + "\".equals(name)) {\n");
                for (ApiMethod method : entry.getValue()) {
                    writeExecutableCase(writer, "            ", method.paramTypes, method.varArgs, staticMethodCall(apiClass, method));
                }
                writer.write("        }\n");
            }
            writer.write("        throw unsupportedStatic(" + typeLiteral(apiClass.qualifiedName) + ", name, safeArgs);\n");
            writer.write("    }\n\n");
            helperIndex++;
        }
    }

    private static LinkedHashMap<String, List<ApiMethod>> groupMethodsByName(List<ApiMethod> methods) {
        LinkedHashMap<String, List<ApiMethod>> result = new LinkedHashMap<String, List<ApiMethod>>();
        for (ApiMethod method : methods) {
            List<ApiMethod> list = result.get(method.name);
            if (list == null) {
                list = new ArrayList<ApiMethod>();
                result.put(method.name, list);
            }
            list.add(method);
        }
        return result;
    }

    private static void writeInvoke(Writer writer, List<ApiClass> classes) throws IOException {
        List<ApiClass> dispatchClasses = orderInstanceDispatchClasses(classes);
        writer.write("    public static Object invoke(Object target, String name, Object[] args) throws Exception {\n");
        writer.write("        Object[] safeArgs = safeArgs(args);\n");
        writer.write("        CN1AccessException unsupported = null;\n");
        int helperIndex = 0;
        for (ApiClass apiClass : dispatchClasses) {
            if (apiClass.isLookupOnly() || apiClass.instanceMethods.isEmpty()) {
                continue;
            }
            writer.write("        if (target instanceof " + apiClass.qualifiedName + ") {\n");
            writer.write("            try {\n");
            writer.write("                return invoke" + helperIndex + "((" + apiClass.qualifiedName + ") target, name, safeArgs);\n");
            writer.write("            } catch (CN1AccessException ex) {\n");
            writer.write("                unsupported = ex;\n");
            writer.write("            }\n");
            writer.write("        }\n");
            helperIndex++;
        }
        writer.write("        if (unsupported != null) {\n");
        writer.write("            throw unsupported;\n");
        writer.write("        }\n");
        writer.write("        throw unsupportedInstance(target, name, safeArgs);\n");
        writer.write("    }\n\n");

        helperIndex = 0;
        for (ApiClass apiClass : dispatchClasses) {
            if (apiClass.isLookupOnly() || apiClass.instanceMethods.isEmpty()) {
                continue;
            }
            writer.write("    private static Object invoke" + helperIndex + "(" + apiClass.qualifiedName
                    + " typedTarget, String name, Object[] safeArgs) throws Exception {\n");
            LinkedHashMap<String, List<ApiMethod>> methodsByName = groupMethodsByName(apiClass.instanceMethods);
            for (Map.Entry<String, List<ApiMethod>> entry : methodsByName.entrySet()) {
                writer.write("        if (\"" + entry.getKey() + "\".equals(name)) {\n");
                for (ApiMethod method : entry.getValue()) {
                    writeExecutableCase(writer, "            ", method.paramTypes, method.varArgs, instanceMethodCall(method));
                }
                writer.write("        }\n");
            }
            writer.write("        throw unsupportedInstance(typedTarget, name, safeArgs);\n");
            writer.write("    }\n\n");
            helperIndex++;
        }
    }

    private static void writeGetStaticField(Writer writer, List<ApiClass> classes) throws IOException {
        writer.write("    public static Object getStaticField(Class<?> type, String name) throws Exception {\n");
        for (ApiClass apiClass : classes) {
            if (apiClass.isLookupOnly() || apiClass.staticFields.isEmpty()) {
                continue;
            }
            writer.write("        if (type == " + typeLiteral(apiClass.qualifiedName) + ") {\n");
            for (ApiField field : apiClass.staticFields) {
                writer.write("            if (\"" + field.name + "\".equals(name)) return "
                        + apiClass.qualifiedName + "." + field.name + ";\n");
            }
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedStaticField(type, name);\n");
        writer.write("    }\n\n");
    }

    private static void writeGetField(Writer writer, List<ApiClass> classes) throws IOException {
        List<ApiClass> dispatchClasses = orderInstanceDispatchClasses(classes);
        writer.write("    public static Object getField(Object target, String name) throws Exception {\n");
        for (ApiClass apiClass : dispatchClasses) {
            if (apiClass.isLookupOnly() || apiClass.instanceFields.isEmpty()) {
                continue;
            }
            writer.write("        if (target instanceof " + apiClass.qualifiedName + ") {\n");
            writer.write("            " + apiClass.qualifiedName + " typedTarget = (" + apiClass.qualifiedName + ") target;\n");
            for (ApiField field : apiClass.instanceFields) {
                writer.write("            if (\"" + field.name + "\".equals(name)) return typedTarget." + field.name + ";\n");
            }
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedField(target, name);\n");
        writer.write("    }\n\n");
    }

    private static void writeSetStaticField(Writer writer, List<ApiClass> classes) throws IOException {
        writer.write("    public static void setStaticField(Class<?> type, String name, Object value) throws Exception {\n");
        for (ApiClass apiClass : classes) {
            if (apiClass.isLookupOnly()) {
                continue;
            }
            List<ApiField> writableFields = writableStaticFields(apiClass.staticFields);
            if (writableFields.isEmpty()) {
                continue;
            }
            writer.write("        if (type == " + typeLiteral(apiClass.qualifiedName) + ") {\n");
            for (ApiField field : writableFields) {
                writer.write("            if (\"" + field.name + "\".equals(name)) {\n");
                writer.write("                " + apiClass.qualifiedName + "." + field.name + " = "
                        + castValue("value", field.type) + ";\n");
                writer.write("                return;\n");
                writer.write("            }\n");
            }
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedStaticFieldWrite(type, name, value);\n");
        writer.write("    }\n\n");
    }

    private static void writeSetField(Writer writer, List<ApiClass> classes) throws IOException {
        List<ApiClass> dispatchClasses = orderInstanceDispatchClasses(classes);
        writer.write("    public static void setField(Object target, String name, Object value) throws Exception {\n");
        for (ApiClass apiClass : dispatchClasses) {
            if (apiClass.isLookupOnly()) {
                continue;
            }
            List<ApiField> writableFields = writableInstanceFields(apiClass.instanceFields);
            if (writableFields.isEmpty()) {
                continue;
            }
            writer.write("        if (target instanceof " + apiClass.qualifiedName + ") {\n");
            writer.write("            " + apiClass.qualifiedName + " typedTarget = (" + apiClass.qualifiedName + ") target;\n");
            for (ApiField field : writableFields) {
                writer.write("            if (\"" + field.name + "\".equals(name)) {\n");
                writer.write("                typedTarget." + field.name + " = " + castValue("value", field.type) + ";\n");
                writer.write("                return;\n");
                writer.write("            }\n");
            }
            writer.write("        }\n");
        }
        writer.write("        throw unsupportedFieldWrite(target, name, value);\n");
        writer.write("    }\n\n");
    }

    private static List<ApiField> writableStaticFields(List<ApiField> fields) {
        List<ApiField> out = new ArrayList<ApiField>();
        for (ApiField field : fields) {
            if (field.writable) {
                out.add(field);
            }
        }
        return out;
    }

    private static List<ApiField> writableInstanceFields(List<ApiField> fields) {
        List<ApiField> out = new ArrayList<ApiField>();
        for (ApiField field : fields) {
            if (field.writable) {
                out.add(field);
            }
        }
        return out;
    }

    private static void writeHelpers(Writer writer, List<SamInterfaceAdapter> samInterfaces) throws IOException {
        writer.write("    private static Object[] safeArgs(Object[] args) {\n");
        writer.write("        return args == null ? new Object[0] : args;\n");
        writer.write("    }\n\n");
        writer.write("    private static Object[] adaptArgs(Object[] args, Class<?>[] paramTypes, boolean varArgs) {\n");
        writer.write("        if (args == null || args.length == 0) {\n");
        writer.write("            return args == null ? new Object[0] : args;\n");
        writer.write("        }\n");
        writer.write("        Object[] adapted = args.clone();\n");
        writer.write("        if (!varArgs) {\n");
        writer.write("            for (int i = 0; i < Math.min(adapted.length, paramTypes.length); i++) {\n");
        writer.write("                adapted[i] = adaptValue(adapted[i], paramTypes[i]);\n");
        writer.write("            }\n");
        writer.write("            return adapted;\n");
        writer.write("        }\n");
        writer.write("        if (paramTypes.length == 0) {\n");
        writer.write("            return adapted;\n");
        writer.write("        }\n");
        writer.write("        int fixedCount = paramTypes.length - 1;\n");
        writer.write("        for (int i = 0; i < Math.min(fixedCount, adapted.length); i++) {\n");
        writer.write("            adapted[i] = adaptValue(adapted[i], paramTypes[i]);\n");
        writer.write("        }\n");
        writer.write("        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();\n");
        writer.write("        for (int i = fixedCount; i < adapted.length; i++) {\n");
        writer.write("            adapted[i] = adaptValue(adapted[i], componentType);\n");
        writer.write("        }\n");
        writer.write("        return adapted;\n");
        writer.write("    }\n\n");
        writeSamAdaptHelpers(writer, samInterfaces);
        writer.write("    private static Object adaptValue(Object value, Class<?> type) {\n");
        writer.write("        if (!(value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue)) {\n");
        writer.write("            return value;\n");
        writer.write("        }\n");
        writer.write("        return adaptLambdaValue((bsh.cn1.CN1LambdaSupport.LambdaValue) value, type);\n");
        writer.write("    }\n\n");
        writer.write("    private static boolean matches(Object[] args, Class<?>[] paramTypes, boolean varArgs) {\n");
        writer.write("        if (!varArgs) {\n");
        writer.write("            if (args.length != paramTypes.length) {\n");
        writer.write("                return false;\n");
        writer.write("            }\n");
        writer.write("            for (int i = 0; i < paramTypes.length; i++) {\n");
        writer.write("                if (!matchesType(args[i], paramTypes[i])) {\n");
        writer.write("                    return false;\n");
        writer.write("                }\n");
        writer.write("            }\n");
        writer.write("            return true;\n");
        writer.write("        }\n");
        writer.write("        if (paramTypes.length == 0) {\n");
        writer.write("            return true;\n");
        writer.write("        }\n");
        writer.write("        int fixedCount = paramTypes.length - 1;\n");
        writer.write("        if (args.length < fixedCount) {\n");
        writer.write("            return false;\n");
        writer.write("        }\n");
        writer.write("        for (int i = 0; i < fixedCount; i++) {\n");
        writer.write("            if (!matchesType(args[i], paramTypes[i])) {\n");
        writer.write("                return false;\n");
        writer.write("            }\n");
        writer.write("        }\n");
        writer.write("        Class<?> componentType = paramTypes[paramTypes.length - 1].getComponentType();\n");
        writer.write("        for (int i = fixedCount; i < args.length; i++) {\n");
        writer.write("            if (!matchesType(args[i], componentType)) {\n");
        writer.write("                return false;\n");
        writer.write("            }\n");
        writer.write("        }\n");
        writer.write("        return true;\n");
        writer.write("    }\n\n");
        writer.write("    private static boolean matchesType(Object value, Class<?> type) {\n");
        writer.write("        if (type == Object.class) {\n");
        writer.write("            return true;\n");
        writer.write("        }\n");
        writer.write("        if (value == null) {\n");
        writer.write("            return !type.isPrimitive();\n");
        writer.write("        }\n");
        writer.write("        if (type.isArray()) {\n");
        writer.write("            return type.isInstance(value);\n");
        writer.write("        }\n");
        writer.write("        if (\"boolean\".equals(type.getName()) || type == Boolean.class) {\n");
        writer.write("            return value instanceof Boolean;\n");
        writer.write("        }\n");
        writer.write("        if (\"char\".equals(type.getName()) || type == Character.class) {\n");
        writer.write("            return value instanceof Character;\n");
        writer.write("        }\n");
        writer.write("        if (\"byte\".equals(type.getName()) || type == Byte.class || \"short\".equals(type.getName()) || type == Short.class\n");
        writer.write("                || \"int\".equals(type.getName()) || type == Integer.class || \"long\".equals(type.getName()) || type == Long.class\n");
        writer.write("                || \"float\".equals(type.getName()) || type == Float.class || \"double\".equals(type.getName()) || type == Double.class) {\n");
        writer.write("            return value instanceof Number;\n");
        writer.write("        }\n");
        writer.write("        if (value instanceof bsh.cn1.CN1LambdaSupport.LambdaValue) {\n");
        writer.write("            return isSamInterface(type);\n");
        writer.write("        }\n");
        writer.write("        return type.isInstance(value);\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated constructor dispatch not implemented for \" + type.getName() + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated static dispatch not implemented for \" + type.getName() + \".\" + name + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {\n");
        writer.write("        return new CN1AccessException(\"Generated instance dispatch not implemented for \" + target.getClass().getName() + \".\" + name + describeArgs(args));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {\n");
        writer.write("        return new CN1AccessException(\"Generated static field access not implemented for \" + type.getName() + \".\" + name);\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedField(Object target, String name) {\n");
        writer.write("        return new CN1AccessException(\"Generated field access not implemented for \" + target.getClass().getName() + \".\" + name);\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {\n");
        writer.write("        return new CN1AccessException(\"Generated static field write not implemented for \" + type.getName() + \".\" + name + \" value=\" + describeValue(value));\n");
        writer.write("    }\n\n");
        writer.write("    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {\n");
        writer.write("        return new CN1AccessException(\"Generated field write not implemented for \" + target.getClass().getName() + \".\" + name + \" value=\" + describeValue(value));\n");
        writer.write("    }\n\n");
        writeDescribeHelpers(writer);
    }

    private static void writeSamAdaptHelpers(Writer writer, List<SamInterfaceAdapter> samInterfaces) throws IOException {
        writer.write("    private static boolean isSamInterface(Class<?> type) {\n");
        for (SamInterfaceAdapter adapter : samInterfaces) {
            writer.write("        if (type == " + typeLiteral(adapter.apiClass.qualifiedName) + ") {\n");
            writer.write("            return true;\n");
            writer.write("        }\n");
        }
        writer.write("        return false;\n");
        writer.write("    }\n\n");
        writer.write("    private static Object adaptLambdaValue(final bsh.cn1.CN1LambdaSupport.LambdaValue lambda, Class<?> type) {\n");
        for (SamInterfaceAdapter adapter : samInterfaces) {
            writer.write("        if (type == " + typeLiteral(adapter.apiClass.qualifiedName) + ") {\n");
            writer.write("            return new " + adapter.apiClass.qualifiedName + "() {\n");
            writer.write("                public " + adapter.method.returnType.canonicalName() + " " + adapter.method.name + "("
                    + samParameterList(adapter.method.paramTypes) + ") {\n");
            String invokeExpr = "lambda.invoke(" + samArgumentArray(adapter.method.paramTypes) + ")";
            if (adapter.method.returnType.isVoid()) {
                writer.write("                    try {\n");
                writer.write("                        " + invokeExpr + ";\n");
                writer.write("                    } catch (bsh.EvalError ex) {\n");
                writer.write("                        throw new RuntimeException(ex);\n");
                writer.write("                    }\n");
            } else {
                writer.write("                    try {\n");
                writer.write("                        return " + samReturnExpression(adapter.method.returnType, invokeExpr) + ";\n");
                writer.write("                    } catch (bsh.EvalError ex) {\n");
                writer.write("                        throw new RuntimeException(ex);\n");
                writer.write("                    }\n");
            }
            writer.write("                }\n");
            writer.write("            };\n");
            writer.write("        }\n");
        }
        writer.write("        return lambda;\n");
        writer.write("    }\n\n");
    }

    private static List<SamInterfaceAdapter> collectSamInterfaces(Discovery discovery) {
        LinkedHashMap<String, SamInterfaceAdapter> adapters = new LinkedHashMap<String, SamInterfaceAdapter>();
        String[] knownSamInterfaces = {
            "com.codename1.util.OnComplete",
            "com.codename1.util.SuccessCallback",
            "com.codename1.util.FailureCallback",
            "com.codename1.util.Callback",
            "com.codename1.ui.events.ActionListener",
            "java.lang.Runnable",
            "com.codename1.ui.events.DataChangedListener",
            "com.codename1.ui.events.SelectionListener"
        };
        Map<String, ApiClass> byName = new HashMap<String, ApiClass>();
        for (GeneratedPackage pkg : discovery.packages) {
            for (ApiClass apiClass : pkg.classes) {
                byName.put(apiClass.qualifiedName, apiClass);
            }
        }
        for (String samInterface : knownSamInterfaces) {
            ApiClass apiClass = byName.get(samInterface);
            if (apiClass == null || !apiClass.isInterface) {
                continue;
            }
            Class<?> runtimeClass = loadRuntimeClass(samInterface);
            if (runtimeClass != null) {
                Method samMethod = findRuntimeSamMethod(runtimeClass);
                if (samMethod == null) {
                    continue;
                }
                ApiMethod apiMethod = findApiMethod(apiClass, samMethod);
                if (apiMethod != null) {
                    adapters.put(samInterface, new SamInterfaceAdapter(apiClass, apiMethod));
                }
            } else {
                ApiMethod apiMethod = findSamMethodFromApiClass(apiClass);
                if (apiMethod != null) {
                    adapters.put(samInterface, new SamInterfaceAdapter(apiClass, apiMethod));
                }
            }
        }
        return new ArrayList<SamInterfaceAdapter>(adapters.values());
    }

    private static ApiMethod findSamMethodFromApiClass(ApiClass apiClass) {
        ApiMethod candidate = null;
        for (ApiMethod method : apiClass.instanceMethods) {
            if (method.isStatic) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = method;
        }
        return candidate;
    }

    private static void collectSamInterfaces(List<?> executables, Map<String, ApiClass> byName,
            LinkedHashMap<String, SamInterfaceAdapter> adapters) {
        for (Object executable : executables) {
            List<ApiType> paramTypes;
            if (executable instanceof ApiConstructor) {
                paramTypes = ((ApiConstructor) executable).paramTypes;
            } else {
                paramTypes = ((ApiMethod) executable).paramTypes;
            }
            for (ApiType paramType : paramTypes) {
                ApiType effectiveType = paramType.varArgs ? paramType.componentType() : paramType;
                if (effectiveType.arrayDepth != 0) {
                    continue;
                }
                ApiClass candidate = byName.get(effectiveType.baseName);
                if (candidate == null || !candidate.isInterface) {
                    continue;
                }
                ApiMethod sam = findSamMethod(candidate);
                if (sam != null) {
                    adapters.put(candidate.qualifiedName, new SamInterfaceAdapter(candidate, sam));
                }
            }
        }
    }

    private static ApiMethod findSamMethod(ApiClass apiClass) {
        Class<?> runtimeClass = loadRuntimeClass(apiClass.qualifiedName);
        if (runtimeClass != null && runtimeClass.isInterface()) {
            Method samMethod = findRuntimeSamMethod(runtimeClass);
            if (samMethod == null) {
                return null;
            }
            return findApiMethod(apiClass, samMethod);
        }
        if (!apiClass.isInterface) {
            return null;
        }
        ApiMethod candidate = null;
        for (ApiMethod method : apiClass.instanceMethods) {
            if (method.isStatic) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = method;
        }
        return candidate;
    }

    private static Method findRuntimeSamMethod(Class<?> runtimeClass) {
        Method found = null;
        for (Method method : runtimeClass.getMethods()) {
            int modifiers = method.getModifiers();
            if (!java.lang.reflect.Modifier.isAbstract(modifiers)
                    || java.lang.reflect.Modifier.isStatic(modifiers)
                    || method.isDefault()
                    || method.isSynthetic()
                    || isObjectMethod(method)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = method;
        }
        return found;
    }

    private static ApiMethod findApiMethod(ApiClass apiClass, Method runtimeMethod) {
        for (ApiMethod method : apiClass.instanceMethods) {
            if (matchesRuntimeMethod(method, runtimeMethod)) {
                return method;
            }
        }
        return null;
    }

    private static boolean matchesRuntimeMethod(ApiMethod method, Method runtimeMethod) {
        if (!method.name.equals(runtimeMethod.getName())) {
            return false;
        }
        if (!matchesRuntimeType(method.returnType, runtimeMethod.getReturnType())) {
            return false;
        }
        Class<?>[] runtimeParamTypes = runtimeMethod.getParameterTypes();
        if (method.paramTypes.size() != runtimeParamTypes.length) {
            return false;
        }
        for (int i = 0; i < runtimeParamTypes.length; i++) {
            if (!matchesRuntimeType(method.paramTypes.get(i), runtimeParamTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesRuntimeType(ApiType apiType, Class<?> runtimeType) {
        return apiType.canonicalName().equals(runtimeCanonicalName(runtimeType));
    }

    private static String runtimeCanonicalName(Class<?> runtimeType) {
        if (runtimeType.isArray()) {
            return runtimeCanonicalName(runtimeType.getComponentType()) + "[]";
        }
        return runtimeType.getName();
    }

    private static boolean isObjectMethod(ApiMethod method) {
        return ("toString".equals(method.name) && method.paramTypes.isEmpty())
                || ("hashCode".equals(method.name) && method.paramTypes.isEmpty())
                || ("equals".equals(method.name) && method.paramTypes.size() == 1
                && "java.lang.Object".equals(method.paramTypes.get(0).baseName)
                && method.paramTypes.get(0).arrayDepth == 0);
    }

    private static boolean isObjectMethod(Method method) {
        return ("toString".equals(method.getName()) && method.getParameterCount() == 0)
                || ("hashCode".equals(method.getName()) && method.getParameterCount() == 0)
                || ("equals".equals(method.getName()) && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == Object.class);
    }

    private static String samParameterList(List<ApiType> paramTypes) {
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < paramTypes.size(); i++) {
            parts.add(paramTypes.get(i).canonicalName() + " arg" + i);
        }
        return join(parts, ", ");
    }

    private static String samArgumentArray(List<ApiType> paramTypes) {
        if (paramTypes.isEmpty()) {
            return "new Object[0]";
        }
        List<String> parts = new ArrayList<String>();
        for (int i = 0; i < paramTypes.size(); i++) {
            parts.add("arg" + i);
        }
        return "new Object[]{" + join(parts, ", ") + "}";
    }

    private static String samReturnExpression(ApiType returnType, String invocation) {
        return castValue("bsh.cn1.CN1LambdaSupport.coerceResult(" + invocation + ", " + matchTypeLiteral(returnType) + ")",
                returnType);
    }

    private static void writeDescribeHelpers(Writer writer) throws IOException {
        writer.write("    private static String describeArgs(Object[] args) {\n");
        writer.write("        if (args == null || args.length == 0) {\n");
        writer.write("            return \"()\";\n");
        writer.write("        }\n");
        writer.write("        StringBuilder sb = new StringBuilder(\"(\");\n");
        writer.write("        for (int i = 0; i < args.length; i++) {\n");
        writer.write("            if (i > 0) {\n");
        writer.write("                sb.append(\", \");\n");
        writer.write("            }\n");
        writer.write("            sb.append(describeValue(args[i]));\n");
        writer.write("        }\n");
        writer.write("        sb.append(')');\n");
        writer.write("        return sb.toString();\n");
        writer.write("    }\n\n");
        writer.write("    private static String describeValue(Object value) {\n");
        writer.write("        return value == null ? \"null\" : value.getClass().getName();\n");
        writer.write("    }\n");
    }

    private static void writeExecutableCase(Writer writer, String indent, List<ApiType> paramTypes,
            boolean varArgs, String invocation) throws IOException {
        boolean emptyArgs = paramTypes.isEmpty() && !varArgs;
        if (emptyArgs) {
            writer.write(indent + "if (safeArgs.length == 0) {\n");
            writer.write(indent + "    " + invocation + "\n");
            writer.write(indent + "}\n");
        } else {
            writer.write(indent + "if (matches(safeArgs, " + classArrayLiteral(paramTypes) + ", " + varArgs + ")) {\n");
            writer.write(indent + "    Object[] adaptedArgs = adaptArgs(safeArgs, " + classArrayLiteral(paramTypes) + ", " + varArgs + ");\n");
            if (varArgs && !paramTypes.isEmpty()) {
                writeVarArgsInvocation(writer, indent + "    ", paramTypes, invocation);
            } else {
                writer.write(indent + "    " + invocation + "\n");
            }
            writer.write(indent + "}\n");
        }
    }

    private static void writeVarArgsInvocation(Writer writer, String indent, List<ApiType> paramTypes, String invocation)
            throws IOException {
        int fixedCount = paramTypes.size() - 1;
        ApiType componentType = paramTypes.get(paramTypes.size() - 1).componentType();
        writer.write(indent + componentType.canonicalName() + "[] varArgs = new " + componentType.canonicalName()
                + "[adaptedArgs.length - " + fixedCount + "];\n");
        writer.write(indent + "for (int i = " + fixedCount + "; i < adaptedArgs.length; i++) {\n");
        writer.write(indent + "    varArgs[i - " + fixedCount + "] = " + castValue("adaptedArgs[i]", componentType) + ";\n");
        writer.write(indent + "}\n");
        writer.write(indent + invocation.replace("__VARARGS__", "varArgs") + "\n");
    }

    private static String constructorCall(ApiClass apiClass, ApiConstructor constructor) {
        return "return new " + apiClass.qualifiedName + "(" + joinInvocationArgs(constructor.paramTypes, constructor.varArgs) + ");";
    }

    private static String staticMethodCall(ApiClass apiClass, ApiMethod method) {
        String call = apiClass.qualifiedName + "." + method.name + "(" + joinInvocationArgs(method.paramTypes, method.varArgs) + ")";
        return method.returnType.isVoid() ? call + "; return null;" : "return " + call + ";";
    }

    private static String instanceMethodCall(ApiMethod method) {
        String call = "typedTarget." + method.name + "(" + joinInvocationArgs(method.paramTypes, method.varArgs) + ")";
        return method.returnType.isVoid() ? call + "; return null;" : "return " + call + ";";
    }

    private static String joinInvocationArgs(List<ApiType> paramTypes, boolean varArgs) {
        if (paramTypes.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<String>();
        int fixedCount = varArgs ? paramTypes.size() - 1 : paramTypes.size();
        for (int i = 0; i < fixedCount; i++) {
            parts.add(castValue("adaptedArgs[" + i + "]", paramTypes.get(i)));
        }
        if (varArgs) {
            parts.add("__VARARGS__");
        }
        return join(parts, ", ");
    }

    private static String classArrayLiteral(List<ApiType> paramTypes) {
        if (paramTypes.isEmpty()) {
            return "new Class<?>[0]";
        }
        List<String> values = new ArrayList<String>();
        for (ApiType type : paramTypes) {
            values.add(matchTypeLiteral(type));
        }
        return "new Class<?>[]{" + join(values, ", ") + "}";
    }

    private static String castValue(String expression, ApiType type) {
        if (type.arrayDepth > 0) {
            return "(" + type.canonicalName() + ") " + expression;
        }
        if ("boolean".equals(type.baseName)) {
            return "((Boolean) " + expression + ").booleanValue()";
        }
        if ("java.lang.Boolean".equals(type.baseName)) {
            return "Boolean.valueOf(((Boolean) " + expression + ").booleanValue())";
        }
        if ("char".equals(type.baseName)) {
            return "((Character) " + expression + ").charValue()";
        }
        if ("java.lang.Character".equals(type.baseName)) {
            return "Character.valueOf(((Character) " + expression + ").charValue())";
        }
        if ("byte".equals(type.baseName)) {
            return "((Number) " + expression + ").byteValue()";
        }
        if ("java.lang.Byte".equals(type.baseName)) {
            return "Byte.valueOf(((Number) " + expression + ").byteValue())";
        }
        if ("short".equals(type.baseName)) {
            return "((Number) " + expression + ").shortValue()";
        }
        if ("java.lang.Short".equals(type.baseName)) {
            return "Short.valueOf(((Number) " + expression + ").shortValue())";
        }
        if ("int".equals(type.baseName)) {
            return "((Number) " + expression + ").intValue()";
        }
        if ("java.lang.Integer".equals(type.baseName)) {
            return "Integer.valueOf(((Number) " + expression + ").intValue())";
        }
        if ("long".equals(type.baseName)) {
            return "((Number) " + expression + ").longValue()";
        }
        if ("java.lang.Long".equals(type.baseName)) {
            return "Long.valueOf(((Number) " + expression + ").longValue())";
        }
        if ("float".equals(type.baseName)) {
            return "((Number) " + expression + ").floatValue()";
        }
        if ("java.lang.Float".equals(type.baseName)) {
            return "Float.valueOf(((Number) " + expression + ").floatValue())";
        }
        if ("double".equals(type.baseName)) {
            return "((Number) " + expression + ").doubleValue()";
        }
        if ("java.lang.Double".equals(type.baseName)) {
            return "Double.valueOf(((Number) " + expression + ").doubleValue())";
        }
        return "(" + type.canonicalName() + ") " + expression;
    }

    private static String typeLiteral(String qualifiedName) {
        return qualifiedName + ".class";
    }

    private static String matchTypeLiteral(ApiType type) {
        if (type.arrayDepth > 0) {
            return type.canonicalName() + ".class";
        }
        if (!type.isPrimitive()) {
            return type.canonicalName() + ".class";
        }
        if ("boolean".equals(type.baseName)) {
            return "java.lang.Boolean.class";
        }
        if ("char".equals(type.baseName)) {
            return "java.lang.Character.class";
        }
        if ("byte".equals(type.baseName)) {
            return "java.lang.Byte.class";
        }
        if ("short".equals(type.baseName)) {
            return "java.lang.Short.class";
        }
        if ("int".equals(type.baseName)) {
            return "java.lang.Integer.class";
        }
        if ("long".equals(type.baseName)) {
            return "java.lang.Long.class";
        }
        if ("float".equals(type.baseName)) {
            return "java.lang.Float.class";
        }
        if ("double".equals(type.baseName)) {
            return "java.lang.Double.class";
        }
        return type.baseName + ".class";
    }

    private static boolean isBlacklistedMethod(String name) {
        return "wait".equals(name)
                || "notify".equals(name)
                || "notifyAll".equals(name)
                || "getClass".equals(name)
                || "clone".equals(name)
                || "finalize".equals(name);
    }

    private static void collectJavaFiles(File root, List<File> out) {
        if (root == null || !root.exists()) {
            return;
        }
        if (root.isDirectory()) {
            if ("target".equals(root.getName()) || ".git".equals(root.getName()) || "build".equals(root.getName())) {
                return;
            }
            File[] files = root.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                collectJavaFiles(file, out);
            }
            return;
        }
        if (root.getName().endsWith(".java")) {
            out.add(root);
        }
    }

    private static boolean matchesPrefix(String value, String[] prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String qualify(String packageName, String simpleName) {
        return packageName.length() == 0 ? simpleName : packageName + "." + simpleName;
    }

    private static String simpleName(String qualifiedName) {
        int pos = qualifiedName.lastIndexOf('.');
        return pos < 0 ? qualifiedName : qualifiedName.substring(pos + 1);
    }

    private static String helperClassName(String packageName) {
        StringBuilder out = new StringBuilder(HELPER_CLASS_PREFIX);
        for (int i = 0; i < packageName.length(); i++) {
            char c = packageName.charAt(i);
            out.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
        return out.toString();
    }

    private static void recreateDir(File dir) throws IOException {
        deleteRecursively(dir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create " + dir);
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file);
        }
    }

    private static File projectRoot(File output) {
        File current = output.getParentFile();
        while (current != null) {
            if (new File(current, "common").isDirectory() && new File(current, "tools").isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Failed to locate project root from " + output);
    }

    private static File locateCn1Root(File projectRoot) {
        List<File> candidates = new ArrayList<File>();

        String configuredRoot = System.getenv("CN1_REPO_ROOT");
        if (configuredRoot != null && configuredRoot.trim().length() > 0) {
            candidates.add(new File(configuredRoot.trim()));
        }

        String githubWorkspace = System.getenv("GITHUB_WORKSPACE");
        if (githubWorkspace != null && githubWorkspace.trim().length() > 0) {
            candidates.add(new File(githubWorkspace.trim()));
        }

        File current = projectRoot;
        while (current != null) {
            candidates.add(current);
            current = current.getParentFile();
        }

        candidates.add(new File(System.getProperty("user.home"), "dev/cn1"));

        for (File candidate : candidates) {
            if (looksLikeCn1Root(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException(
                "Failed to locate Codename One source tree. Checked CN1_REPO_ROOT, GITHUB_WORKSPACE, "
                        + "ancestors of " + projectRoot + ", and ~/dev/cn1");
    }

    private static boolean looksLikeCn1Root(File candidate) {
        return candidate != null
                && new File(candidate, "CodenameOne/src").isDirectory()
                && new File(candidate, "Ports/CLDC11/src").isDirectory();
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(delimiter);
            }
            out.append(values.get(i));
        }
        return out.toString();
    }

    private static final class Resolver {
        private static final Map<String, String> JAVA_LANG_BUILTINS = createJavaLangBuiltins();

        private final SourceClass sourceClass;
        private final Set<String> knownTypes;

        Resolver(SourceClass sourceClass, Set<String> knownTypes) {
            this.sourceClass = sourceClass;
            this.knownTypes = knownTypes;
        }

        ApiType resolveParameter(String declaration) {
            String cleaned = stripAnnotations(declaration);
            int eqPos = cleaned.indexOf('=');
            if (eqPos >= 0) {
                cleaned = cleaned.substring(0, eqPos).trim();
            }
            int lastSpace = findLastWhitespaceOutsideArrays(cleaned);
            if (lastSpace < 0) {
                return resolveType(cleaned);
            }
            return resolveType(cleaned.substring(0, lastSpace).trim());
        }

        ApiType resolveType(String rawType) {
            if (rawType == null) {
                return null;
            }
            String cleaned = stripAnnotations(rawType).trim();
            cleaned = stripGenerics(cleaned).trim();
            cleaned = cleaned.replace("? extends ", "").replace("? super ", "").trim();
            if ("?".equals(cleaned) || cleaned.length() == 0) {
                return null;
            }

            boolean varArgs = false;
            int arrayDepth = 0;
            while (cleaned.endsWith("[]")) {
                arrayDepth++;
                cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
            }
            if (cleaned.endsWith("...")) {
                varArgs = true;
                arrayDepth++;
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }

            while (cleaned.endsWith("[]")) {
                arrayDepth++;
                cleaned = cleaned.substring(0, cleaned.length() - 2).trim();
            }

            String resolved = resolveBaseType(cleaned);
            return resolved == null ? null : new ApiType(resolved, arrayDepth, varArgs);
        }

        private String resolveBaseType(String name) {
            if (ApiType.PRIMITIVES.contains(name) || "void".equals(name)) {
                return name;
            }
            if (name.indexOf('.') >= 0) {
                String[] parts = name.split("\\.");
                if (parts.length > 0 && Character.isLowerCase(parts[0].charAt(0))) {
                    return name;
                }
                String first = resolveSimpleName(parts[0]);
                if (first == null) {
                    return null;
                }
                StringBuilder out = new StringBuilder(first);
                for (int i = 1; i < parts.length; i++) {
                    out.append('.').append(parts[i]);
                }
                String resolved = out.toString();
                return knownTypes.contains(resolved) ? resolved : null;
            }
            return resolveSimpleName(name);
        }

        private String resolveSimpleName(String simpleName) {
            if (sourceClass.simpleName.equals(simpleName)) {
                return sourceClass.qualifiedName;
            }
            String typeParamBound = sourceClass.typeParameterBounds.get(simpleName);
            if (typeParamBound != null) {
                return typeParamBound;
            }
            String nested = sourceClass.nestedTypes.get(simpleName);
            if (nested != null) {
                return nested;
            }
            String explicit = sourceClass.explicitImports.get(simpleName);
            if (explicit != null) {
                return explicit;
            }
            String samePackage = qualify(sourceClass.packageName, simpleName);
            if (knownTypes.contains(samePackage)) {
                return samePackage;
            }
            String javaLang = JAVA_LANG_BUILTINS.get(simpleName);
            if (javaLang != null) {
                return javaLang;
            }
            String wildcard = resolveWildcard(simpleName);
            if (wildcard != null) {
                return wildcard;
            }
            return null;
        }

        private String resolveWildcard(String simpleName) {
            String found = null;
            for (String wildcardImport : sourceClass.wildcardImports) {
                String candidate = wildcardImport + "." + simpleName;
                if (!knownTypes.contains(candidate)) {
                    continue;
                }
                if (found != null && !found.equals(candidate)) {
                    return null;
                }
                found = candidate;
            }
            return found;
        }

        private static Map<String, String> createJavaLangBuiltins() {
            Map<String, String> out = new HashMap<String, String>();
            String[] names = new String[]{
                    "Appendable", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
                    "AssertionError", "AutoCloseable", "Boolean", "Byte", "CharSequence", "Character", "Class",
                    "ClassCastException", "ClassLoader", "ClassNotFoundException", "CloneNotSupportedException",
                    "Cloneable", "Comparable", "Deprecated", "Double", "Enum", "Error", "Exception", "Float",
                    "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException",
                    "IllegalStateException", "IllegalThreadStateException", "IncompatibleClassChangeError",
                    "IndexOutOfBoundsException", "InstantiationException", "Integer", "Iterable", "LinkageError",
                    "Long", "Math", "NegativeArraySizeException", "NoClassDefFoundError", "NoSuchFieldException",
                    "NoSuchMethodException", "NullPointerException", "Number", "NumberFormatException", "Object",
                    "OutOfMemoryError", "Override", "Readable", "ReflectiveOperationException", "Runnable",
                    "RuntimeException", "SecurityException", "Short", "StackTraceElement", "String",
                    "StringBuffer", "StringBuilder", "SuppressWarnings", "System", "Thread", "Throwable",
                    "UnsupportedOperationException", "Void"
            };
            for (String name : names) {
                out.put(name, "java.lang." + name);
            }
            return out;
        }
    }

    private static String stripAnnotations(String value) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            char ch = value.charAt(i);
            if (ch == '@') {
                i++;
                while (i < value.length() && (Character.isJavaIdentifierPart(value.charAt(i)) || value.charAt(i) == '.')) {
                    i++;
                }
                if (i < value.length() && value.charAt(i) == '(') {
                    int depth = 1;
                    i++;
                    while (i < value.length() && depth > 0) {
                        char c = value.charAt(i++);
                        if (c == '(') {
                            depth++;
                        } else if (c == ')') {
                            depth--;
                        }
                    }
                }
                while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
                    i++;
                }
                continue;
            }
            out.append(ch);
            i++;
        }
        return out.toString().replaceAll("\\bfinal\\b", "").trim().replaceAll("\\s+", " ");
    }

    private static String stripGenerics(String value) {
        StringBuilder out = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '<') {
                depth++;
                continue;
            }
            if (ch == '>') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }
            if (depth == 0) {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static int findLastWhitespaceOutsideArrays(String value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static final class Discovery {
        final List<String> indexedClassNames;
        final List<GeneratedPackage> packages;

        Discovery(List<String> indexedClassNames, List<GeneratedPackage> packages) {
            this.indexedClassNames = indexedClassNames;
            this.packages = packages;
        }
    }

    private static final class GeneratedPackage {
        final String packageName;
        final String helperClassName;
        final List<ApiClass> classes;

        GeneratedPackage(String packageName, String helperClassName, List<ApiClass> classes) {
            this.packageName = packageName;
            this.helperClassName = helperClassName;
            this.classes = classes;
        }
    }

    private static final class SourceUnit {
        final String packageName;
        final List<SourceClass> topLevelClasses;

        SourceUnit(String packageName, List<SourceClass> topLevelClasses) {
            this.packageName = packageName;
            this.topLevelClasses = topLevelClasses;
        }
    }

    private static final class SourceClass {
        final String packageName;
        final String simpleName;
        final String qualifiedName;
        final ClassTree classTree;
        final Map<String, String> explicitImports;
        final List<String> wildcardImports;
        final Map<String, String> nestedTypes;
        final Map<String, String> typeParameterBounds;
        final List<String> superTypes;

        SourceClass(String packageName, String simpleName, String qualifiedName, ClassTree classTree,
                Map<String, String> explicitImports, List<String> wildcardImports, Map<String, String> nestedTypes,
                Map<String, String> typeParameterBounds, List<String> superTypes) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.classTree = classTree;
            this.explicitImports = explicitImports;
            this.wildcardImports = wildcardImports;
            this.nestedTypes = nestedTypes;
            this.typeParameterBounds = typeParameterBounds;
            this.superTypes = superTypes;
        }
    }

    private static final class ApiSignature {
        final List<ApiType> paramTypes;
        final boolean varArgs;

        ApiSignature(List<ApiType> paramTypes, boolean varArgs) {
            this.paramTypes = paramTypes;
            this.varArgs = varArgs;
        }
    }

    private static final class ApiClass {
        final String packageName;
        final String simpleName;
        final String qualifiedName;
        final List<String> superTypes;
        final boolean isInterface;
        final boolean isAbstract;
        final List<ApiConstructor> constructors;
        final List<ApiMethod> staticMethods;
        final List<ApiMethod> instanceMethods;
        final List<ApiField> staticFields;
        final List<ApiField> instanceFields;

        ApiClass(String packageName, String simpleName, String qualifiedName, List<String> superTypes,
                boolean isInterface, boolean isAbstract,
                List<ApiConstructor> constructors, List<ApiMethod> staticMethods, List<ApiMethod> instanceMethods,
                List<ApiField> staticFields, List<ApiField> instanceFields) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.superTypes = superTypes;
            this.isInterface = isInterface;
            this.isAbstract = isAbstract;
            this.constructors = constructors;
            this.staticMethods = staticMethods;
            this.instanceMethods = instanceMethods;
            this.staticFields = staticFields;
            this.instanceFields = instanceFields;
        }

        boolean isLookupOnly() {
            return "java.lang.StringBuilder".equals(qualifiedName);
        }
    }

    private static final class ApiConstructor {
        final List<ApiType> paramTypes;
        final boolean varArgs;

        ApiConstructor(List<ApiType> paramTypes, boolean varArgs) {
            this.paramTypes = paramTypes;
            this.varArgs = varArgs;
        }
    }

    private static final class ApiMethod {
        final String name;
        final ApiType returnType;
        final List<ApiType> paramTypes;
        final boolean varArgs;
        final boolean isStatic;

        ApiMethod(String name, ApiType returnType, List<ApiType> paramTypes, boolean varArgs, boolean isStatic) {
            this.name = name;
            this.returnType = returnType;
            this.paramTypes = paramTypes;
            this.varArgs = varArgs;
            this.isStatic = isStatic;
        }
    }

    private static final class ApiField {
        final String name;
        final ApiType type;
        final boolean isStatic;
        final boolean writable;

        ApiField(String name, ApiType type, boolean isStatic, boolean writable) {
            this.name = name;
            this.type = type;
            this.isStatic = isStatic;
            this.writable = writable;
        }
    }

    private static final class SamInterfaceAdapter {
        final ApiClass apiClass;
        final ApiMethod method;

        SamInterfaceAdapter(ApiClass apiClass, ApiMethod method) {
            this.apiClass = apiClass;
            this.method = method;
        }
    }

    private static final class ApiType {
        static final Set<String> PRIMITIVES = new LinkedHashSet<String>(Arrays.asList(
                "boolean", "byte", "char", "short", "int", "long", "float", "double"));

        final String baseName;
        final int arrayDepth;
        final boolean varArgs;

        ApiType(String baseName, int arrayDepth, boolean varArgs) {
            this.baseName = baseName;
            this.arrayDepth = arrayDepth;
            this.varArgs = varArgs;
        }

        boolean isPrimitive() {
            return arrayDepth == 0 && PRIMITIVES.contains(baseName);
        }

        boolean isVoid() {
            return arrayDepth == 0 && "void".equals(baseName);
        }

        String canonicalName() {
            StringBuilder out = new StringBuilder(baseName);
            for (int i = 0; i < arrayDepth; i++) {
                out.append("[]");
            }
            return out.toString();
        }

        ApiType componentType() {
            return new ApiType(baseName, arrayDepth - 1, false);
        }
    }
}
