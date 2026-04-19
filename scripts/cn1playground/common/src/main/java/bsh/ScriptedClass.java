/*****************************************************************************
 * Codename One Playground BeanShell fork.                                    *
 *                                                                            *
 * Lightweight namespace-backed class runtime. Replaces the ASM/reflection    *
 * based class generator that was removed for CN1 safety. ScriptedClass       *
 * captures a class declaration as data; ScriptedInstance is an instance of   *
 * that class with its own NameSpace for fields.                              *
 *****************************************************************************/

package bsh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Descriptor for a script-declared class. Holds the methods, field
 * initializers, and ctors as parsed AST nodes so that instances can be
 * constructed without bytecode generation or reflection.
 *
 * <p>Scripted classes do not extend any Java type other than Object — they
 * are not assignable to user-declared interfaces at the Java type-system
 * level (we have no Proxy in CN1). Method dispatch on {@link
 * ScriptedInstance} goes through this descriptor's method table, not
 * through Java reflection.
 */
public final class ScriptedClass {
    private final String name;
    private final NameSpace declaringNameSpace;
    /** Method templates: paramNames/paramTypes/body captured once. Per
     * instance we synthesise fresh BshMethods bound to the instance
     * namespace so {@code this.x} and bare field references resolve. */
    private final List<MethodTemplate> instanceMethods;
    private final List<BshMethod> staticMethods;
    private final List<MethodTemplate> constructors;
    private final List<BSHTypedVariableDeclaration> instanceFieldDecls;
    private final NameSpace staticNameSpace;
    private final ScriptedClass parent;
    private boolean isInterface;
    private boolean isEnum;
    private List<ScriptedInstance> enumConstants;

    private ScriptedClass(
            String name,
            NameSpace declaringNameSpace,
            ScriptedClass parent,
            List<MethodTemplate> instanceMethods,
            List<BshMethod> staticMethods,
            List<MethodTemplate> constructors,
            List<BSHTypedVariableDeclaration> instanceFieldDecls,
            NameSpace staticNameSpace) {
        this.name = name;
        this.declaringNameSpace = declaringNameSpace;
        this.parent = parent;
        this.instanceMethods = instanceMethods;
        this.staticMethods = staticMethods;
        this.constructors = constructors;
        this.instanceFieldDecls = instanceFieldDecls;
        this.staticNameSpace = staticNameSpace;
    }

    public ScriptedClass getParent() {
        return parent;
    }

    /** Look up an instance method declared on the *parent* class only, used
     * by `super.foo()` dispatch to bypass this class's override. */
    MethodTemplate findParentInstanceMethodTemplate(String methodName, Object[] args) {
        if (parent == null) return null;
        return parent.findInstanceMethodTemplate(methodName, args);
    }

    /** Build a ScriptedClass from a class-declaration body, evaluating
     * static initializers eagerly. The optional {@code parent} is a
     * previously-declared ScriptedClass whose instance methods and field
     * declarations are inherited (overridden by name+arity in this class). */
    static ScriptedClass build(
            String name,
            NameSpace declaringNameSpace,
            BSHBlock body,
            ScriptedClass parent,
            CallStack callstack,
            Interpreter interpreter) throws EvalError {

        NameSpace staticNs = new NameSpace(declaringNameSpace,
                interpreter.getClassManager(), name + "(static)");
        List<MethodTemplate> instanceMethods = new ArrayList<MethodTemplate>();
        List<BshMethod> staticMethods = new ArrayList<BshMethod>();
        List<MethodTemplate> ctors = new ArrayList<MethodTemplate>();
        List<BSHTypedVariableDeclaration> fieldDecls = new ArrayList<BSHTypedVariableDeclaration>();

        // Inherit parent fields first so subclass field decls can shadow them.
        if (parent != null) {
            fieldDecls.addAll(parent.instanceFieldDecls);
        }

        if (body != null) {
            int n = body.jjtGetNumChildren();
            for (int i = 0; i < n; i++) {
                Node child = body.jjtGetChild(i);
                if (child instanceof BSHMethodDeclaration) {
                    BSHMethodDeclaration mdecl = (BSHMethodDeclaration) child;
                    mdecl.insureNodesParsed();
                    // Evaluate parameter types now so paramTypes are populated;
                    // insureNodesParsed alone does not compute Class<?>[].
                    mdecl.paramsNode.eval(callstack, interpreter);
                    boolean isStatic = mdecl.modifiers != null
                            && mdecl.modifiers.hasModifier("static");
                    boolean isCtor = mdecl.name.equals(name);
                    if (isCtor) {
                        ctors.add(new MethodTemplate(mdecl));
                    } else if (isStatic) {
                        BshMethod bm = new BshMethod(mdecl, staticNs,
                                mdecl.modifiers, false);
                        staticMethods.add(bm);
                        staticNs.setMethod(bm);
                    } else {
                        instanceMethods.add(new MethodTemplate(mdecl));
                    }
                } else if (child instanceof BSHClassDeclaration) {
                    // Nested static class declaration. We build it eagerly
                    // and bind it in the parent's static namespace so that
                    // `Outer.Inner` resolution finds it. Only `static` nested
                    // classes are supported — non-static inner classes need
                    // an enclosing-instance reference which we don't model.
                    BSHClassDeclaration nested = (BSHClassDeclaration) child;
                    if (nested.modifiers != null
                            && nested.modifiers.hasModifier("static")) {
                        callstack.push(staticNs);
                        try {
                            child.eval(callstack, interpreter);
                        } finally {
                            callstack.pop();
                        }
                    }
                } else if (child instanceof BSHTypedVariableDeclaration) {
                    BSHTypedVariableDeclaration fdecl = (BSHTypedVariableDeclaration) child;
                    boolean isStatic = fdecl.modifiers != null
                            && fdecl.modifiers.hasModifier("static");
                    if (isStatic) {
                        // Static field: evaluate now, bind into static namespace.
                        callstack.push(staticNs);
                        try {
                            child.eval(callstack, interpreter);
                        } finally {
                            callstack.pop();
                        }
                    } else {
                        fieldDecls.add(fdecl);
                    }
                }
            }
        }

        // Merge parent instance methods that aren't shadowed (same name+arity)
        // by this class's own declarations.
        if (parent != null) {
            for (MethodTemplate pm : parent.instanceMethods) {
                boolean shadowed = false;
                for (MethodTemplate own : instanceMethods) {
                    if (own.name.equals(pm.name) && own.paramCount == pm.paramCount) {
                        shadowed = true;
                        break;
                    }
                }
                if (!shadowed) instanceMethods.add(pm);
            }
        }

        return new ScriptedClass(name, declaringNameSpace, parent, instanceMethods,
                staticMethods, ctors, fieldDecls, staticNs);
    }

    public String getName() {
        return name;
    }

    public boolean isInterface() {
        return isInterface;
    }

    void markInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    void markEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    /** Populate enum constants from the class body. Each constant becomes a
     * ScriptedInstance with a single internal {@code __enumName__} field; the
     * constant is bound as a static variable and added to {@link
     * #getEnumConstants()} for {@code values()} dispatch. */
    void populateEnumConstants(BSHBlock body, CallStack callstack,
            Interpreter interpreter) throws EvalError {
        enumConstants = new ArrayList<ScriptedInstance>();
        if (body == null) return;
        int n = body.jjtGetNumChildren();
        for (int i = 0; i < n; i++) {
            Node child = body.jjtGetChild(i);
            if (!(child instanceof BSHEnumConstant)) continue;
            String constantName = ((BSHEnumConstant) child).getName();
            NameSpace constNs = new NameSpace(staticNameSpace,
                    interpreter.getClassManager(), name + "." + constantName);
            ScriptedInstance constant = new ScriptedInstance(this, constNs);
            try {
                constNs.setVariable("this", constant, false);
                constNs.setVariable("__enumName__", constantName, false);
                staticNameSpace.setVariable(constantName, constant, false);
            } catch (UtilEvalError ex) {
                throw ex.toEvalError(child, callstack);
            }
            enumConstants.add(constant);
        }
    }

    public List<ScriptedInstance> getEnumConstants() {
        return enumConstants;
    }

    NameSpace getDeclaringNameSpace() {
        return declaringNameSpace;
    }

    NameSpace getStaticNameSpace() {
        return staticNameSpace;
    }

    /** Construct a new instance of this scripted class with the given
     * constructor arguments. */
    public ScriptedInstance newInstance(Object[] args, CallStack callstack,
            Interpreter interpreter) throws EvalError {
        NameSpace instanceNs = new NameSpace(staticNameSpace,
                interpreter.getClassManager(), name + "(instance)");
        ScriptedInstance instance = new ScriptedInstance(this, instanceNs);
        try {
            instanceNs.setVariable("this", instance, false);
        } catch (UtilEvalError ex) {
            throw ex.toEvalError(null, callstack);
        }

        // Install instance methods so this.foo() and bare foo() in a method
        // body resolve via the instance namespace. Each method is bound to
        // instanceNs as its declaringNameSpace, which is what makes the
        // method body see fields and other instance methods.
        for (MethodTemplate mt : instanceMethods) {
            instanceNs.setMethod(mt.bind(instanceNs));
        }

        // Initialise instance fields against the instance namespace.
        for (BSHTypedVariableDeclaration fdecl : instanceFieldDecls) {
            callstack.push(instanceNs);
            try {
                fdecl.eval(callstack, interpreter);
            } finally {
                callstack.pop();
            }
        }

        // Run a matching constructor if one was declared.
        MethodTemplate ctorTpl = pickConstructor(args);
        if (ctorTpl == null) {
            if (args != null && args.length != 0 && !constructors.isEmpty()) {
                throw new EvalError("No matching constructor for "
                        + name + "(" + describeArgs(args) + ")",
                        null, callstack);
            }
        } else {
            BshMethod ctorBound = ctorTpl.bind(instanceNs);
            ctorBound.invoke(args, interpreter, callstack, null, false);
        }

        return instance;
    }

    /** Look up an instance method template by name. Overload resolution is
     * arity-only for now (matching BSH's loose-typed dispatch).
     * The caller binds the template to its instance namespace. */
    MethodTemplate findInstanceMethodTemplate(String methodName, Object[] args) {
        int arity = args == null ? 0 : args.length;
        MethodTemplate varArgsMatch = null;
        for (MethodTemplate t : instanceMethods) {
            if (!t.name.equals(methodName)) continue;
            if (t.paramCount == arity) return t;
            if (t.isVarArgs && t.paramCount - 1 <= arity) varArgsMatch = t;
        }
        return varArgsMatch;
    }

    BshMethod findStaticMethod(String methodName, Object[] args) {
        int arity = args == null ? 0 : args.length;
        for (BshMethod m : staticMethods) {
            if (m.getName().equals(methodName) && m.getParameterCount() == arity) {
                return m;
            }
        }
        return null;
    }

    /** Built-in static dispatch for enum {@code values()} that doesn't have a
     * declared method. Returns null when no built-in applies. */
    Object invokeEnumBuiltinStatic(String methodName, Object[] args) {
        if (!isEnum) return null;
        int arity = args == null ? 0 : args.length;
        if ("values".equals(methodName) && arity == 0) {
            if (enumConstants == null) return new ScriptedInstance[0];
            return enumConstants.toArray(new ScriptedInstance[enumConstants.size()]);
        }
        if ("valueOf".equals(methodName) && arity == 1 && args[0] instanceof String) {
            if (enumConstants != null) {
                for (ScriptedInstance c : enumConstants) {
                    Object n = c.getField("__enumName__");
                    if (args[0].equals(n)) return c;
                }
            }
            throw new IllegalArgumentException("No enum constant " + name + "." + args[0]);
        }
        return null;
    }

    private MethodTemplate pickConstructor(Object[] args) {
        int arity = args == null ? 0 : args.length;
        for (MethodTemplate c : constructors) {
            if (c.paramCount == arity) return c;
        }
        return null;
    }

    /** A captured class-body method/ctor declaration. We keep the AST
     * pieces and (re)bind them to a per-instance namespace at invocation
     * time so that BshMethod's declaringNameSpace points at the instance,
     * giving the body access to the instance's fields and sibling methods. */
    static final class MethodTemplate {
        final String name;
        final int paramCount;
        final boolean isVarArgs;
        private final BSHMethodDeclaration decl;

        MethodTemplate(BSHMethodDeclaration decl) {
            this.decl = decl;
            this.name = decl.name;
            this.paramCount = decl.paramsNode.getParamNames() == null ? 0
                    : decl.paramsNode.getParamNames().length;
            this.isVarArgs = decl.isVarArgs;
        }

        BshMethod bind(NameSpace declaringNameSpace) {
            return new BshMethod(decl, declaringNameSpace,
                    decl.modifiers, false);
        }
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i] == null ? "null" : args[i].getClass().getSimpleName());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ScriptedClass[" + name + "]";
    }
}
