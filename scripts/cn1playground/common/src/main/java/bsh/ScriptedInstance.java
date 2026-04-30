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
            throw new EvalError(buildMissingMethodError(methodName, args),
                    callerInfo, callstack);
        }
        BshMethod bound = tpl.bind(instanceNameSpace);
        return bound.invoke(args, interpreter, callstack, callerInfo, false);
    }

    private String buildMissingMethodError(String methodName, Object[] args) {
        int arity = args == null ? 0 : args.length;
        StringBuilder msg = new StringBuilder();
        msg.append("No instance method ").append(scriptedClass.getName())
                .append('.').append(methodName).append('/').append(arity).append('.');
        java.util.List<String> known = scriptedClass.getInstanceMethodNames();
        String suggestion = closestMatch(known, methodName);
        if (suggestion != null) {
            msg.append(" Did you mean: ").append(suggestion).append('?');
        } else if (!known.isEmpty()) {
            msg.append(' ').append(scriptedClass.getName()).append(" declares: ");
            int n = Math.min(8, known.size());
            for (int i = 0; i < n; i++) {
                if (i > 0) msg.append(", ");
                msg.append(known.get(i));
            }
            if (known.size() > n) msg.append(", ...");
            msg.append('.');
        }
        return msg.toString();
    }

    private static String closestMatch(java.util.List<String> candidates, String requested) {
        if (candidates.isEmpty() || requested == null) return null;
        String lowerReq = requested.toLowerCase();
        for (String c : candidates) {
            if (c.toLowerCase().equals(lowerReq)) return c; // case only
        }
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        int threshold = Math.max(2, requested.length() / 3);
        for (String c : candidates) {
            int d = levenshtein(c.toLowerCase(), lowerReq);
            if (d < bestDist && d <= threshold) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private static int levenshtein(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (la == 0) return lb;
        if (lb == 0) return la;
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[lb];
    }

    @Override
    public String toString() {
        return scriptedClass.getName() + "@"
                + Integer.toHexString(System.identityHashCode(this));
    }
}
