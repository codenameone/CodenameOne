package bsh;

/**
 * Minimal CN1-safe invocable abstraction.
 */
public abstract class Invocable {
    public static Invocable get(Object ignored) {
        return null;
    }

    public abstract Class<?>[] getParameterTypes();

    public abstract Class<?> getReturnType();

    public abstract int getParameterCount();

    public boolean isInnerClass() {
        return false;
    }

    public boolean isVarArgs() {
        return false;
    }

    public Class<?> getVarArgsType() {
        return void.class;
    }

    public Class<?> getVarArgsComponentType() {
        return void.class;
    }

    public boolean isGetter() {
        return false;
    }

    public boolean isSetter() {
        return false;
    }

    public int getModifiers() {
        return 0;
    }

    public boolean isStatic() {
        return false;
    }

    public boolean isSynthetic() {
        return false;
    }

    protected int getLastParameterIndex() {
        return getParameterCount();
    }

    public Class<?> getDeclaringClass() {
        return Object.class;
    }

    public String getName() {
        return "<unsupported>";
    }

    public String getMethodDescriptor() {
        return "";
    }

    public String[] getParamTypeDescriptors() {
        return new String[0];
    }

    public String getReturnTypeDescriptor() {
        return "";
    }

    public Object invoke(Object base, Object... pars) {
        throw new UnsupportedOperationException(
                "Direct reflective invocation is not supported in the Codename One BeanShell runtime.");
    }
}
