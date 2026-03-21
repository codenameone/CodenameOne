package bsh;

/**
 * Reduced no-op security guard for the playground runtime.
 */
final class MainSecurityGuard {
    void add(SecurityGuard guard) {}
    boolean remove(SecurityGuard guard) { return false; }
    void canConstruct(Class<?> _class, Object[] args) throws SecurityError {}
    void canInvokeStaticMethod(Class<?> _class, String methodName, Object[] args) throws SecurityError {}
    void canInvokeMethod(Object thisArg, String methodName, Object[] args) throws SecurityError {}
    void canInvokeLocalMethod(String methodName, Object[] args) throws SecurityError {}
    void canInvokeSuperMethod(Class<?> superClass, Object thisArg, String methodName, Object[] args) throws SecurityError {}
    void canGetField(Object thisArg, String fieldName) throws SecurityError {}
    void canGetStaticField(Class<?> _class, String fieldName) throws SecurityError {}
    void canExtends(Class<?> superClass) throws SecurityError {}
    void canImplements(Class<?> _interface) throws SecurityError {}
}
