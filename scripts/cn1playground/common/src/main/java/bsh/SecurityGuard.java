package bsh;

/** It's the interface to implement a single SecurityGuard to be used by MainSecurityGuard */
public interface SecurityGuard {

    /**
     * Validate and return if you can create a instance
     * @param _class - It can be a Class or Interface ( To anonymous inner classes )
     * @param args - Args used to call the constructor
     */
    public default boolean canConstruct(Class<?> _class, Object[] args) {
        return true;
    }

    /** Validate and return if a specific static method of a specific class can be invoked */
    public default boolean canInvokeStaticMethod(Class<?> _class, String methodName, Object[] args) {
        return true;
    }

    /** Validate and return if a specific method of a specific object can be invoked. */
    public default boolean canInvokeMethod(Object thisArg, String methodName, Object[] args) {
        return true;
    }

    /** Validate and return if a specific local method ( aka commands ) can be invoked */
    public default boolean canInvokeLocalMethod(String methodName, Object[] args) {
        return true;
    }

    /** Validate and return if can call a method of super class */
    public default boolean canInvokeSuperMethod(Class<?> superClass, Object thisArg, String methodName, Object[] args) {
        return true;
    }

    /** Validate and return if can get a field of a specific object */
    public default boolean canGetField(Object thisArg, String fieldName) {
        return true;
    }

    /** Validate and return if can get a static field of a specific class */
    public default boolean canGetStaticField(Class<?> _class, String fieldName) {
        return true;
    }

    /** Validate and return if some class can extends {@link superClass} */
    public default boolean canExtends(Class<?> superClass) {
        return true;
    }

    /** Validate and return if some class can implements {@link _interface} */
    public default boolean canImplements(Class<?> _interface) {
        return true;
    }

}
