package bsh.cn1;

/**
 * Indirection point for generated access code.
 *
 * The generated implementation is expected to replace the fallback during
 * the build/bootstrap workflow.
 */
public final class CN1AccessRegistry {
    private static CN1Access instance = GeneratedCN1Access.INSTANCE;

    private CN1AccessRegistry() {
    }

    public static CN1Access getInstance() {
        return instance;
    }

    public static void setInstance(CN1Access access) {
        instance = access == null ? GeneratedCN1Access.INSTANCE : access;
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
