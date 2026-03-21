/*****************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one                *
 * or more contributor license agreements.  See the NOTICE file              *
 * distributed with this work for additional information                     *
 * regarding copyright ownership.  The ASF licenses this file                *
 * to you under the Apache License, Version 2.0 (the                         *
 * "License"); you may not use this file except in compliance                *
 * with the License.  You may obtain a copy of the License at                *
 *                                                                           *
 *     http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing,                *
 * software distributed under the License is distributed on an               *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY                    *
 * KIND, either express or implied.  See the License for the                 *
 * specific language governing permissions and limitations                   *
 * under the License.                                                        *
 *****************************************************************************/

package bsh;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * CN1-safe scripted object wrapper.
 *
 * This version intentionally drops Java SE proxy integration and dynamic class
 * generation support. The playground only needs namespace-backed scripted
 * method dispatch.
 */
public final class This implements java.io.Serializable, Runnable {
    private static final long serialVersionUID = 1L;
    public static final Map<String, NameSpace> contextStore = new Hashtable<String, NameSpace>();
    static final ThreadLocal<Map<String, Object[]>> CONTEXT_ARGS = new ThreadLocal<Map<String, Object[]>>() {
        @Override
        protected Map<String, Object[]> initialValue() {
            return new HashMap<String, Object[]>();
        }
    };

    enum Keys {
        BSHSTATIC { public String toString() { return "_bshStatic"; } },
        BSHTHIS { public String toString() { return "_bshThis"; } },
        BSHSUPER { public String toString() { return "_bshSuper"; } },
        BSHINIT { public String toString() { return "_bshInstanceInitializer"; } },
        BSHCONSTRUCTORS { public String toString() { return "_bshConstructors"; } },
        BSHCLASSMODIFIERS { public String toString() { return "_bshClassModifiers"; } }
    }

    final NameSpace namespace;
    transient Interpreter declaringInterpreter;

    static This getThis(NameSpace namespace, Interpreter declaringInterpreter) {
        return new This(namespace, declaringInterpreter);
    }

    This(NameSpace namespace, Interpreter declaringInterpreter) {
        this.namespace = namespace;
        this.declaringInterpreter = declaringInterpreter;
    }

    public NameSpace getNameSpace() {
        return namespace;
    }

    public Object getInterface(Class<?> clas) {
        throw new InterpreterError("Interface proxies are not supported in the Codename One BeanShell runtime.");
    }

    public Object getInterface(Class<?>[] ca) {
        throw new InterpreterError("Interface proxies are not supported in the Codename One BeanShell runtime.");
    }

    public String toString() {
        if (namespace == null) {
            return "'this' reference";
        }
        BshMethod toString = Reflect.getMethod(namespace, "toString", new Class<?>[0]);
        if (toString != null) {
            try {
                return String.valueOf(toString.invoke(new Object[0], declaringInterpreter));
            } catch (EvalError e) {
                // fall through to default representation
            }
        }
        return "'this' reference to Bsh object: " + namespace;
    }

    public void run() {
        try {
            invokeMethod("run", Reflect.ZERO_ARGS);
        } catch (EvalError e) {
            if (declaringInterpreter != null) {
                declaringInterpreter.error("Exception in runnable:" + e);
            }
        }
    }

    public Object invokeMethod(String name, Object[] args) throws EvalError {
        return invokeMethod(name, args, null, null, null, false);
    }

    public Object invokeMethod(String methodName, Object[] args, boolean declaredOnly)
            throws EvalError {
        CallStack callstack = new CallStack(namespace);
        Node node = namespace == null ? null : namespace.getNode();
        if (namespace != null) {
            namespace.setNode(null);
        }
        Object ret = invokeMethod(methodName, args, declaringInterpreter, callstack, node, declaredOnly);
        if (ret instanceof Primitive && ret != Primitive.VOID) {
            return ((Primitive) ret).getValue();
        }
        return ret;
    }

    public Object invokeMethod(String methodName, Object[] args,
            Interpreter interpreter, CallStack callstack, Node callerInfo,
            boolean declaredOnly) throws EvalError {
        if (namespace == null) {
            throw new EvalError("Uninitialized This reference.", callerInfo, callstack);
        }
        if (args == null) {
            args = Reflect.ZERO_ARGS;
        }
        if (interpreter == null) {
            interpreter = declaringInterpreter;
        }
        if (interpreter == null) {
            interpreter = new Interpreter(namespace);
        }
        if (interpreter.getNameSpace() == null) {
            interpreter.setNameSpace(namespace);
        }
        if (callstack == null) {
            callstack = new CallStack(namespace);
        }
        if (callerInfo == null) {
            callerInfo = Node.JAVACODE;
        }

        Class<?>[] types = Types.getTypes(args);
        BshMethod bshMethod = Reflect.getMethod(namespace, methodName, types, declaredOnly);
        if (bshMethod != null) {
            return bshMethod.invoke(args, interpreter, callstack, callerInfo);
        }

        if ("getClass".equals(methodName) && args.length == 0) {
            return getClass();
        }
        if ("toString".equals(methodName) && args.length == 0) {
            return toString();
        }
        if ("hashCode".equals(methodName) && args.length == 0) {
            return Integer.valueOf(hashCode());
        }
        if ("equals".equals(methodName) && args.length == 1) {
            return this == args[0] ? Boolean.TRUE : Boolean.FALSE;
        }
        if ("clone".equals(methodName) && args.length == 0) {
            return cloneMethodImpl(callerInfo, callstack);
        }

        boolean[] outHasMethod = new boolean[1];
        Object result = namespace.invokeDefaultInvokeMethod(methodName, args,
                interpreter, callstack, callerInfo, outHasMethod);
        if (outHasMethod[0]) {
            return result;
        }

        try {
            return namespace.invokeCommand(methodName, args, interpreter, callstack, callerInfo, true);
        } catch (EvalError e) {
            throw new EvalException("Method "
                    + StringUtil.methodString(methodName, types)
                    + " not found in bsh scripted object: " + namespace.getName(),
                    callerInfo, callstack, e);
        }
    }

    Object cloneMethodImpl(Node callerInfo, CallStack callstack) throws EvalError {
        NameSpace copy = namespace.copy();
        return copy.getThis(declaringInterpreter);
    }

    Object cloneMethodImpl(Node callerInfo, CallStack callstack, Object clonedInstance)
            throws EvalError {
        return cloneMethodImpl(callerInfo, callstack);
    }

    public Object[] enumValues() {
        return new Object[0];
    }

    public static void bind(This ths, NameSpace namespace, Interpreter declaringInterpreter) {
        ths.namespace.setParent(namespace);
        ths.declaringInterpreter = declaringInterpreter;
    }

    static boolean isExposedThisMethod(String name) {
        return name.equals("invokeMethod")
                || name.equals("getInterface")
                || name.equals("wait")
                || name.equals("notify")
                || name.equals("notifyAll");
    }

    public static ConstructorArgs getConstructorArgs(Class<?> superClass, This classStaticThis,
            Object[] consArgs, int index) {
        throw new InterpreterError("Scripted class generation is not supported in the Codename One BeanShell runtime.");
    }

    public static void initInstance(GeneratedClass instance, String className, Object[] args) {
        throw new InterpreterError("Scripted class generation is not supported in the Codename One BeanShell runtime.");
    }

    static void registerConstructorContext(CallStack callstack, Interpreter interpreter) {
    }

    public static void initStatic(Class<?> genClass) throws UtilEvalError {
        throw new UtilEvalError("Scripted class generation is not supported in the Codename One BeanShell runtime.");
    }

    public static This pullBshStatic(String uuid) {
        throw new InterpreterError("Scripted class generation is not supported in the Codename One BeanShell runtime.");
    }

    public static class ConstructorArgs {
        public static final ConstructorArgs DEFAULT = new ConstructorArgs();
        public int selector = 0;
        Object[] args = Reflect.ZERO_ARGS;
        int arg;

        ConstructorArgs() {
        }

        ConstructorArgs(int selector, Object[] args) {
            this.selector = selector;
            this.args = args == null ? Reflect.ZERO_ARGS : args;
        }

        Object next() {
            return args[arg++];
        }

        public boolean getBoolean() {
            return ((Boolean) next()).booleanValue();
        }

        public byte getByte() {
            return ((Number) next()).byteValue();
        }

        public char getChar() {
            return ((Character) next()).charValue();
        }

        public short getShort() {
            return ((Number) next()).shortValue();
        }

        public int getInt() {
            return ((Number) next()).intValue();
        }

        public long getLong() {
            return ((Number) next()).longValue();
        }

        public float getFloat() {
            return ((Number) next()).floatValue();
        }

        public double getDouble() {
            return ((Number) next()).doubleValue();
        }

        public Object getObject() {
            return next();
        }
    }
}
