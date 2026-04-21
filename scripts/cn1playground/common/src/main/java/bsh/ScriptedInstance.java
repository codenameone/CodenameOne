/*****************************************************************************
 * Codename One Playground BeanShell fork.                                    *
 *****************************************************************************/

package bsh;

/**
 * Runtime instance of a {@link ScriptedClass}. Backed by a NameSpace that
 * holds the instance's fields. Method dispatch goes through the parent
 * ScriptedClass's method table; field access reads/writes the namespace
 * directly.
 *
 * <p>This is a regular Java object (subclass of Object) — it is not
 * assignable to user-declared interfaces at the Java type system level.
 * Code that needs to pass a ScriptedInstance to a CN1 API expecting a
 * specific interface will fail at the cast site with a clear message;
 * use the listener-bridge factories for those cases.
 */
public final class ScriptedInstance {
    private final ScriptedClass scriptedClass;
    private final NameSpace instanceNameSpace;
    /** Delegate Java instance when the ScriptedClass extends a real Java
     * class (e.g. RuntimeException). Constructed by super(args); method
     * calls on this ScriptedInstance that don't resolve to a scripted
     * method fall through to this delegate. */
    private Object javaDelegate;
    /** Non-null when this is an instance of a non-static inner class —
     * holds the outer instance so inner methods can walk to outer
     * fields and methods. */
    private ScriptedInstance enclosingInstance;

    ScriptedInstance(ScriptedClass scriptedClass, NameSpace instanceNameSpace) {
        this.scriptedClass = scriptedClass;
        this.instanceNameSpace = instanceNameSpace;
    }

    public ScriptedClass getScriptedClass() {
        return scriptedClass;
    }

    public NameSpace getInstanceNameSpace() {
        return instanceNameSpace;
    }

    public Object getJavaDelegate() {
        return javaDelegate;
    }

    public void setJavaDelegate(Object javaDelegate) {
        this.javaDelegate = javaDelegate;
    }

    public ScriptedInstance getEnclosingInstance() {
        return enclosingInstance;
    }

    public void setEnclosingInstance(ScriptedInstance enclosingInstance) {
        this.enclosingInstance = enclosingInstance;
    }

    /** Read an instance field by name. Returns Primitive.VOID if not bound. */
    public Object getField(String name) {
        try {
            return instanceNameSpace.getVariable(name);
        } catch (UtilEvalError ex) {
            return Primitive.VOID;
        }
    }

    /** Write an instance field by name. */
    public void setField(String name, Object value) throws EvalError {
        try {
            instanceNameSpace.setVariable(name, value, false);
        } catch (UtilEvalError ex) {
            throw ex.toEvalError(null, null);
        }
    }

    /** Invoke an instance method by name with the given args. */
    public Object invokeMethod(String methodName, Object[] args,
            Interpreter interpreter, CallStack callstack, Node callerInfo)
            throws EvalError {
        ScriptedClass.MethodTemplate tpl =
                scriptedClass.findInstanceMethodTemplate(methodName, args);
        if (tpl == null) {
            // Allow Object methods to fall through to the JVM defaults.
            if ("toString".equals(methodName) && (args == null || args.length == 0)) {
                return toString();
            }
            if ("hashCode".equals(methodName) && (args == null || args.length == 0)) {
                return Integer.valueOf(System.identityHashCode(this));
            }
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return Boolean.valueOf(this == args[0]);
            }
            if ("getClass".equals(methodName) && (args == null || args.length == 0)) {
                return ScriptedInstance.class;
            }
            // When this scripted class extends a real Java class, fall
            // through to a delegate Java instance constructed by super(...)
            // for methods the scripted class doesn't override (e.g.
            // getMessage / getCause on scripted Exception subclasses).
            if (javaDelegate != null) {
                try {
                    return bsh.Reflect.invokeObjectMethod(
                            javaDelegate, methodName, args == null ? new Object[0] : args,
                            interpreter, callstack, callerInfo);
                } catch (bsh.EvalError ex) {
                    // fall through to the diagnostic below if the delegate
                    // doesn't have the method either
                }
            }
            // Enum-constant built-ins.
            if (scriptedClass.isEnum()
                    && "name".equals(methodName)
                    && (args == null || args.length == 0)) {
                Object v = getField("__enumName__");
                if (v != Primitive.VOID) return v;
            }
            if (scriptedClass.isEnum()
                    && "ordinal".equals(methodName)
                    && (args == null || args.length == 0)) {
                java.util.List<ScriptedInstance> all = scriptedClass.getEnumConstants();
                return Integer.valueOf(all == null ? -1 : all.indexOf(this));
            }
            throw new EvalError("No instance method " + scriptedClass.getName()
                    + "." + methodName + "/"
                    + (args == null ? 0 : args.length),
                    callerInfo, callstack);
        }
        BshMethod bound = tpl.bind(instanceNameSpace);
        return bound.invoke(args, interpreter, callstack, callerInfo, false);
    }

    @Override
    public String toString() {
        return scriptedClass.getName() + "@"
                + Integer.toHexString(System.identityHashCode(this));
    }
}
