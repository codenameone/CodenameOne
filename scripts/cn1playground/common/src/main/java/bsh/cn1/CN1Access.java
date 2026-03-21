package bsh.cn1;

/**
 * CN1-safe access surface used instead of direct JDK reflection.
 */
public interface CN1Access {
    Class<?> findClass(String name);

    Object construct(Class<?> type, Object[] args) throws Exception;

    Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception;

    Object invoke(Object target, String name, Object[] args) throws Exception;

    Object getStaticField(Class<?> type, String name) throws Exception;

    Object getField(Object target, String name) throws Exception;

    void setStaticField(Class<?> type, String name, Object value) throws Exception;

    void setField(Object target, String name, Object value) throws Exception;
}
