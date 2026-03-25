package bsh.cn1;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.Primitive;
import bsh.UtilEvalError;

public final class CN1LambdaSupport {
    private static final ThreadLocal<Interpreter> CURRENT_INTERPRETER = new ThreadLocal<Interpreter>();
    private static final ThreadLocal<NameSpace> CURRENT_NAMESPACE = new ThreadLocal<NameSpace>();

    private CN1LambdaSupport() {
    }

    public static void pushInterpreter(Interpreter interpreter) {
        CURRENT_INTERPRETER.set(interpreter);
    }

    public static void clearInterpreter() {
        CURRENT_INTERPRETER.remove();
    }

    public static void setCurrentNameSpace(NameSpace namespace) {
        CURRENT_NAMESPACE.set(namespace);
    }

    public static NameSpace getCurrentNameSpace() {
        return CURRENT_NAMESPACE.get();
    }

    public static void clearCurrentNameSpace() {
        CURRENT_NAMESPACE.remove();
    }

    public static LambdaValue lambda(String[] parameterNames, String bodySource) {
        Interpreter interpreter = CURRENT_INTERPRETER.get();
        if (interpreter == null) {
            throw new IllegalStateException("No active BeanShell interpreter available for lambda capture.");
        }
        NameSpace activeNs = CURRENT_NAMESPACE.get();
        NameSpace parentNs = activeNs != null ? activeNs : interpreter.getNameSpace();
        return new LambdaValue(interpreter, parentNs, sanitizeParams(parameterNames), bodySource == null ? "" : bodySource);
    }

    private static String[] sanitizeParams(String[] parameterNames) {
        if (parameterNames == null || parameterNames.length == 0) {
            return new String[0];
        }
        String[] copy = new String[parameterNames.length];
        for (int i = 0; i < parameterNames.length; i++) {
            copy[i] = parameterNames[i] == null ? "" : parameterNames[i].trim();
        }
        return copy;
    }

    public static Object coerceResult(Object value, Class<?> type) {
        if (type == null || type == Void.TYPE) {
            return null;
        }
        String typeName = type.getName();
        if (value == null || value == Primitive.NULL || value == Primitive.VOID) {
            return defaultValue(type);
        }
        value = Primitive.unwrap(value);
        if ("boolean".equals(typeName) || type == Boolean.class) {
            return value instanceof Boolean ? value : defaultValue(type);
        }
        if ("char".equals(typeName) || type == Character.class) {
            return value instanceof Character ? value : defaultValue(type);
        }
        if ("byte".equals(typeName) || type == Byte.class
                || "short".equals(typeName) || type == Short.class
                || "int".equals(typeName) || type == Integer.class
                || "long".equals(typeName) || type == Long.class
                || "float".equals(typeName) || type == Float.class
                || "double".equals(typeName) || type == Double.class) {
            return value instanceof Number ? value : defaultValue(type);
        }
        return value;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        String typeName = type.getName();
        if ("boolean".equals(typeName)) {
            return Boolean.FALSE;
        }
        if ("char".equals(typeName)) {
            return Character.valueOf((char) 0);
        }
        if ("byte".equals(typeName)) {
            return Byte.valueOf((byte) 0);
        }
        if ("short".equals(typeName)) {
            return Short.valueOf((short) 0);
        }
        if ("int".equals(typeName)) {
            return Integer.valueOf(0);
        }
        if ("long".equals(typeName)) {
            return Long.valueOf(0L);
        }
        if ("float".equals(typeName)) {
            return Float.valueOf(0f);
        }
        if ("double".equals(typeName)) {
            return Double.valueOf(0d);
        }
        return null;
    }

    public static final class LambdaValue {
        private final Interpreter interpreter;
        private final NameSpace parentNameSpace;
        private final String[] parameterNames;
        private final String bodySource;

        LambdaValue(Interpreter interpreter, NameSpace parentNameSpace, String[] parameterNames, String bodySource) {
            this.interpreter = interpreter;
            this.parentNameSpace = parentNameSpace;
            this.parameterNames = parameterNames;
            this.bodySource = bodySource;
        }

        public Object invoke(Object[] args) throws EvalError {
            NameSpace lambdaNs = new NameSpace(parentNameSpace, interpreter.getClassManager(), "lambda");
            Object[] safeArgs = args == null ? new Object[0] : args;
            int bindCount = Math.min(parameterNames.length, safeArgs.length);
            try {
                for (int i = 0; i < bindCount; i++) {
                    lambdaNs.setVariable(parameterNames[i], safeArgs[i], false);
                }
            } catch (UtilEvalError ex) {
                throw ex.toEvalError(null, null);
            }
            Interpreter prevInterpreter = CURRENT_INTERPRETER.get();
            NameSpace prevNamespace = CURRENT_NAMESPACE.get();
            try {
                CURRENT_INTERPRETER.set(interpreter);
                CURRENT_NAMESPACE.set(lambdaNs);
                synchronized (interpreter) {
                    return Primitive.unwrap(interpreter.eval(bodySource, lambdaNs));
                }
            } finally {
                if (prevInterpreter != null) {
                    CURRENT_INTERPRETER.set(prevInterpreter);
                } else {
                    CURRENT_INTERPRETER.remove();
                }
                if (prevNamespace != null) {
                    CURRENT_NAMESPACE.set(prevNamespace);
                } else {
                    CURRENT_NAMESPACE.remove();
                }
            }
        }
    }
}
