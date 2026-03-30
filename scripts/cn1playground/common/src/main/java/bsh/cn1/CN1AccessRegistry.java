package bsh.cn1;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Indirection point for generated access code.
 *
 * The generated implementation is expected to replace the fallback during
 * the build/bootstrap workflow.
 */
public final class CN1AccessRegistry {
    private static CN1Access instance = GeneratedCN1Access.INSTANCE;
    private static volatile Map<String, Class<?>> generatedSimpleClassIndex;

    private CN1AccessRegistry() {
    }

    public static CN1Access getInstance() {
        return instance;
    }

    public static void setInstance(CN1Access access) {
        instance = access == null ? GeneratedCN1Access.INSTANCE : access;
        generatedSimpleClassIndex = null;
    }

    public static Class<?> findClass(String name) {
        if (name == null) {
            return null;
        }
        String normalized = normalizeClassName(name);
        Class<?> found = instance.findClass(normalized);
        if (found != null) {
            return found;
        }
        int lastDot = normalized.lastIndexOf('.');
        String simpleName = lastDot < 0 ? normalized : normalized.substring(lastDot + 1);
        found = instance.findClass(simpleName);
        if (found != null) {
            return found;
        }
        if (instance instanceof GeneratedCN1Access) {
            return findBySimpleNameFromGeneratedIndex((GeneratedCN1Access) instance, simpleName);
        }
        return null;
    }

    private static Class<?> findBySimpleNameFromGeneratedIndex(GeneratedCN1Access access, String simpleName) {
        Map<String, Class<?>> index = generatedSimpleClassIndex;
        if (index == null) {
            synchronized (CN1AccessRegistry.class) {
                index = generatedSimpleClassIndex;
                if (index == null) {
                    index = buildSimpleClassIndex(access);
                    generatedSimpleClassIndex = index;
                }
            }
        }
        return index.get(simpleName);
    }

    private static Map<String, Class<?>> buildSimpleClassIndex(GeneratedCN1Access access) {
        Map<String, Class<?>> index = new LinkedHashMap<String, Class<?>>();
        String[] indexed = access.getIndexedClassNames();
        for (int i = 0; i < indexed.length; i++) {
            String qualified = indexed[i];
            int lastDot = qualified.lastIndexOf('.');
            String simple = lastDot < 0 ? qualified : qualified.substring(lastDot + 1);
            if (!index.containsKey(simple)) {
                Class<?> resolved = access.findClass(qualified);
                if (resolved != null) {
                    index.put(simple, resolved);
                }
            }
        }
        return index;
    }

    private static String normalizeClassName(String name) {
        String normalized = name.trim();
        if (normalized.startsWith("class ")) {
            normalized = normalized.substring("class ".length()).trim();
        }
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - ".class".length());
        }
        return normalized;
    }

    private static final class UnsupportedCN1Access implements CN1Access {
        private CN1AccessException unsupported(String operation) {
            return new CN1AccessException(
                    "CN1 access registry not generated: " + operation);
        }

        @Override
        public Class<?> findClass(String name) {
            return null;
        }

        @Override
        public Object construct(Class<?> type, Object[] args) throws Exception {
            throw unsupported("construct " + type);
        }

        @Override
        public Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
            throw unsupported("invoke static " + type + "." + name);
        }

        @Override
        public Object invoke(Object target, String name, Object[] args) throws Exception {
            throw unsupported("invoke " + target.getClass().getName() + "." + name);
        }

        @Override
        public Object getStaticField(Class<?> type, String name) throws Exception {
            throw unsupported("get static field " + type + "." + name);
        }

        @Override
        public Object getField(Object target, String name) throws Exception {
            throw unsupported("get field " + target.getClass().getName() + "." + name);
        }

        @Override
        public void setStaticField(Class<?> type, String name, Object value) throws Exception {
            throw unsupported("set static field " + type + "." + name);
        }

        @Override
        public void setField(Object target, String name, Object value) throws Exception {
            throw unsupported("set field " + target.getClass().getName() + "." + name);
        }
    }
}
