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
    private Class<?> javaParent;
    /** When set, this is a non-static inner class — new instances must
     * be supplied with an enclosing instance of this outer type so
     * their namespace chain reaches the outer instance's fields. */
    private ScriptedClass enclosingClass;

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

    public Class<?> getJavaParent() {
        return javaParent;
    }

    void setJavaParent(Class<?> javaParent) {
        this.javaParent = javaParent;
    }

    public ScriptedClass getEnclosingClass() {
        return enclosingClass;
    }

    void setEnclosingClass(ScriptedClass enclosingClass) {
        this.enclosingClass = enclosingClass;
    }

    /** Look up an instance method declared on the *parent* class only, used
     * by `super.foo()` dispatch to bypass this class's override. */
    MethodTemplate findParentInstanceMethodTemplate(String methodName, Object[] args) {
        if (parent == null) return null;
        return parent.findInstanceMethodTemplate(methodName, args);
    }

    /** Stack of classes currently being dispatched through, per thread.
     * When we enter a method via super.foo(), we push the parent class here
     * so that a nested super.foo() inside that method resolves relative to
     * the PARENT's parent (i.e. grandparent), not the actual instance's
     * parent. Without this, multi-level inheritance super chains loop. */
    private static final ThreadLocal<java.util.ArrayDeque<ScriptedClass>> DISPATCH_STACK =
            new ThreadLocal<java.util.ArrayDeque<ScriptedClass>>() {
                @Override protected java.util.ArrayDeque<ScriptedClass> initialValue() {
                    return new java.util.ArrayDeque<ScriptedClass>();
                }
            };

    public static void pushDispatchClass(ScriptedClass c) {
        DISPATCH_STACK.get().push(c);
    }

    public static void popDispatchClass() {
        java.util.ArrayDeque<ScriptedClass> s = DISPATCH_STACK.get();
        if (!s.isEmpty()) s.pop();
    }

    /** Class whose super we should consult for a super.x() call in the
     * current dispatch frame, if any. */
    public static ScriptedClass currentDispatchClass() {
        java.util.ArrayDeque<ScriptedClass> s = DISPATCH_STACK.get();
        return s.isEmpty() ? null : s.peek();
    }

    /** Convenience for call sites that have no interfaces to merge. */
    static ScriptedClass build(
            String name,
            NameSpace declaringNameSpace,
            BSHBlock body,
            ScriptedClass parent,
            CallStack callstack,
            Interpreter interpreter) throws EvalError {
        return build(name, declaringNameSpace, body, parent,
                java.util.Collections.<ScriptedClass>emptyList(), false,
                callstack, interpreter);
    }

    static ScriptedClass build(
            String name,
            NameSpace declaringNameSpace,
            BSHBlock body,
            ScriptedClass parent,
            java.util.List<ScriptedClass> implementedInterfaces,
            CallStack callstack,
            Interpreter interpreter) throws EvalError {
        return build(name, declaringNameSpace, body, parent,
                implementedInterfaces, false, callstack, interpreter);
    }

    /** Build a ScriptedClass from a class-declaration body, evaluating
     * static initializers eagerly. The optional {@code parent} is a
     * previously-declared ScriptedClass whose instance methods and field
     * declarations are inherited (overridden by name+arity in this class).
     * {@code implementedInterfaces} is a list of scripted interfaces whose
     * default methods are also merged. When {@code treatFieldsAsStatic}
     * is true (declarations of interfaces) every field is evaluated into
     * the static namespace, matching the implicit `public static final`
     * semantics of interface fields. */
    static ScriptedClass build(
            String name,
            NameSpace declaringNameSpace,
            BSHBlock body,
            ScriptedClass parent,
            java.util.List<ScriptedClass> implementedInterfaces,
            boolean treatFieldsAsStatic,
            CallStack callstack,
            Interpreter interpreter) throws EvalError {

        NameSpace staticNs = new NameSpace(declaringNameSpace,
                interpreter.getClassManager(), name + "(static)");
        List<MethodTemplate> instanceMethods = new ArrayList<MethodTemplate>();
        List<BshMethod> staticMethods = new ArrayList<BshMethod>();
        List<MethodTemplate> ctors = new ArrayList<MethodTemplate>();
        List<BSHTypedVariableDeclaration> fieldDecls = new ArrayList<BSHTypedVariableDeclaration>();
        // Non-static nested classes discovered during the body pass; we
        // back-fill their enclosingClass reference once the outer's
        // ScriptedClass is fully constructed below.
        List<ScriptedClass> nestedEnclosings = new ArrayList<ScriptedClass>();

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
                    // Nested class declaration — static or inner. Both
                    // are built eagerly and bound in the parent's
                    // static namespace so `Outer.Inner` resolution
                    // finds them. Non-static inner classes also get an
                    // enclosingClass reference so their instances can
                    // walk the outer instance's fields.
                    BSHClassDeclaration nested = (BSHClassDeclaration) child;
                    boolean isStaticNested = nested.modifiers != null
                            && nested.modifiers.hasModifier("static");
                    callstack.push(staticNs);
                    try {
                        Object nestedResult = child.eval(callstack, interpreter);
                        if (!isStaticNested && nestedResult instanceof ScriptedClass) {
                            nestedEnclosings.add((ScriptedClass) nestedResult);
                        }
                    } finally {
                        callstack.pop();
                    }
                } else if (child instanceof BSHTypedVariableDeclaration) {
                    BSHTypedVariableDeclaration fdecl = (BSHTypedVariableDeclaration) child;
                    boolean isStatic = treatFieldsAsStatic
                            || (fdecl.modifiers != null
                                && fdecl.modifiers.hasModifier("static"));
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
            mergeNonShadowed(instanceMethods, parent.instanceMethods);
        }
        // Merge default methods from implemented interfaces. Classes in the
        // chain before interfaces win (parent > interface); within the
        // interface list, earlier-declared interfaces win.
        // Also copy the interfaces' static fields into this class's static
        // namespace so unqualified references (e.g. `DEFAULT_NAME` inside
        // an implementor's method body) resolve.
        if (implementedInterfaces != null) {
            for (ScriptedClass iface : implementedInterfaces) {
                if (iface == null) continue;
                mergeNonShadowed(instanceMethods, iface.instanceMethods);
                copyStaticVariables(iface.staticNameSpace, staticNs);
            }
        }

        ScriptedClass built = new ScriptedClass(name, declaringNameSpace, parent, instanceMethods,
                staticMethods, ctors, fieldDecls, staticNs);
        for (ScriptedClass nested : nestedEnclosings) {
            nested.setEnclosingClass(built);
        }
        return built;
    }

    private static void copyStaticVariables(NameSpace source, NameSpace target) {
        if (source == null || target == null) return;
        String[] names = source.getVariableNames();
        if (names == null) return;
        for (String n : names) {
            if ("this".equals(n)) continue;
            try {
                if (target.getVariable(n) != Primitive.VOID) continue;
                Object v = source.getVariable(n);
                if (v != Primitive.VOID) target.setVariable(n, v, false);
            } catch (UtilEvalError ignore) {
                // skip if either namespace rejects
            }
        }
    }

    private static void mergeNonShadowed(java.util.List<MethodTemplate> target,
            java.util.List<MethodTemplate> source) {
        for (MethodTemplate candidate : source) {
            boolean shadowed = false;
            for (MethodTemplate own : target) {
                if (own.name.equals(candidate.name)
                        && own.paramCount == candidate.paramCount) {
                    shadowed = true;
                    break;
                }
            }
            if (!shadowed) target.add(candidate);
        }
    }

    public String getName() {
        return name;
    }

    /** Unique instance method names declared on this class (including
     * those merged in from superclasses and implemented interfaces).
     * Used by diagnostics to help the user spot typos. */
    public java.util.List<String> getInstanceMethodNames() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        for (MethodTemplate mt : instanceMethods) out.add(mt.name);
        return new java.util.ArrayList<String>(out);
    }

    /** Instance methods declared on this class only (does NOT include
     * inherited or interface-default methods). Used to detect
     * unimplemented abstract interface methods. */
    public java.util.List<String> getDeclaredInstanceMethodSignatures() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (MethodTemplate mt : instanceMethods) {
            out.add(mt.name + "/" + mt.paramCount);
        }
        return out;
    }

    /** Names of abstract methods this ScriptedClass (if it's an
     * interface) declares. Used by concrete class declarations to
     * verify they implement every abstract method of each
     * implemented scripted interface. */
    public java.util.List<String> getAbstractInstanceMethodNames() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        for (MethodTemplate mt : instanceMethods) {
            if (mt.isAbstract()) out.add(mt.name);
        }
        return new java.util.ArrayList<String>(out);
    }

    /** Concrete (non-abstract) instance method names this class
     * supplies — includes own declarations, inherited methods from
     * the extends chain, and default methods merged from
     * implemented scripted interfaces. The interface-method
     * enforcement uses this to distinguish real implementations
     * from just-inherited-abstract stubs. */
    public java.util.Set<String> getConcreteInstanceMethodNames() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        for (MethodTemplate mt : instanceMethods) {
            if (!mt.isAbstract()) out.add(mt.name);
        }
        return out;
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
     * ScriptedInstance constructed via the standard newInstance flow so
     * field initializers and the ctor (if any) run normally. The constant
     * is bound in the static namespace under its declared name and added to
     * {@link #getEnumConstants()} for {@code values()} dispatch.
     *
     * <p>Constants with a body block (per-constant method bodies) are not
     * yet supported — those would need a per-constant subclass.
     */
    void populateEnumConstants(BSHBlock body, CallStack callstack,
            Interpreter interpreter) throws EvalError {
        enumConstants = new ArrayList<ScriptedInstance>();
        if (body == null) return;
        int n = body.jjtGetNumChildren();
        for (int i = 0; i < n; i++) {
            Node child = body.jjtGetChild(i);
            if (!(child instanceof BSHEnumConstant)) continue;
            BSHEnumConstant ec = (BSHEnumConstant) child;
            String constantName = ec.getName();
            Object[] args = ec.hasArguments(callstack, interpreter)
                    ? ec.getArguments(callstack, interpreter)
                    : new Object[0];
            // If the constant has a body block (per-constant method
            // overrides like `ADD { public int apply(...) {...} }`), build
            // an anonymous subclass with this enum as parent and instantiate
            // it; otherwise instantiate the enum class directly.
            BSHBlock constBody = findConstantBody(ec);
            ScriptedInstance constant;
            if (constBody != null) {
                // Declare the anon subclass against this enum's static
                // namespace so the anon's methods can refer to sibling
                // constants (e.g. `return CLOSED;` from OPEN's body).
                ScriptedClass anon = ScriptedClass.build(
                        name + "$" + constantName,
                        staticNameSpace, constBody, this,
                        callstack, interpreter);
                // Propagate the enum marker so built-in name()/ordinal()
                // dispatch still works on per-constant anonymous subclasses.
                anon.markEnum(true);
                constant = anon.newInstance(args, callstack, interpreter);
            } else {
                constant = newInstance(args, callstack, interpreter);
            }
            try {
                constant.getInstanceNameSpace().setVariable("__enumName__",
                        constantName, false);
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

    private static BSHBlock findConstantBody(BSHEnumConstant ec) {
        for (int i = 0; i < ec.jjtGetNumChildren(); i++) {
            Node c = ec.jjtGetChild(i);
            if (c instanceof BSHBlock) return (BSHBlock) c;
        }
        return null;
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
        return newInstance(args, null, callstack, interpreter);
    }

    /** Construct a new instance, optionally linking it to an enclosing
     * instance so that non-static inner classes can reach the outer
     * instance's fields through their namespace chain. */
    public ScriptedInstance newInstance(Object[] args, ScriptedInstance enclosingInstance,
            CallStack callstack, Interpreter interpreter) throws EvalError {
        // For non-static inner classes, chain instanceNs through the
        // enclosing instance's namespace so outer fields resolve. When
        // no enclosing is provided for an inner class, fall back to the
        // static namespace (outer static fields still visible).
        NameSpace parentNs = staticNameSpace;
        if (enclosingClass != null && enclosingInstance != null) {
            parentNs = enclosingInstance.getInstanceNameSpace();
        }
        NameSpace instanceNs = new NameSpace(parentNs,
                interpreter.getClassManager(), name + "(instance)");
        ScriptedInstance instance = new ScriptedInstance(this, instanceNs);
        if (enclosingClass != null && enclosingInstance != null) {
            instance.setEnclosingInstance(enclosingInstance);
        }
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

    /** Look up an instance method template by name with simple overload
     * resolution. Same-arity templates are scored against the actual
     * argument types — exact type match scores highest, then assignable,
     * then Object (fallback). The template with the highest score wins;
     * ties resolve to the first declared. The caller binds the template
     * to its instance namespace. */
    MethodTemplate findInstanceMethodTemplate(String methodName, Object[] args) {
        int arity = args == null ? 0 : args.length;
        MethodTemplate best = null;
        int bestScore = Integer.MIN_VALUE;
        MethodTemplate varArgsMatch = null;
        for (MethodTemplate t : instanceMethods) {
            if (!t.name.equals(methodName)) continue;
            if (t.paramCount == arity) {
                int score = scoreMatch(t, args);
                if (score > bestScore) {
                    bestScore = score;
                    best = t;
                }
            } else if (t.isVarArgs && t.paramCount - 1 <= arity) {
                varArgsMatch = t;
            }
        }
        return best != null ? best : varArgsMatch;
    }

    private static int scoreMatch(MethodTemplate t, Object[] args) {
        if (args == null || args.length == 0) return 0;
        Class<?>[] paramTypes = t.paramTypes();
        int score = 0;
        for (int i = 0; i < args.length; i++) {
            Class<?> declared = paramTypes != null && i < paramTypes.length ? paramTypes[i] : null;
            if (declared == null) {
                // loose-typed parameter — neutral score
                continue;
            }
            Object arg = args[i];
            if (arg == null) {
                score += declared.isPrimitive() ? -10 : 1;
            } else if (declared == arg.getClass()) {
                score += 3;
            } else if (declared.isInstance(arg)) {
                score += 2;
            } else if (declared.isPrimitive() && isCompatiblePrimitive(declared, arg)) {
                score += 2;
            } else if (declared == Object.class) {
                score += 1;
            } else {
                score -= 5;
            }
        }
        return score;
    }

    private static boolean isCompatiblePrimitive(Class<?> primitive, Object value) {
        // CN1 doesn't have Boolean.TYPE / Character.TYPE static fields, so
        // we identify primitive variants by class name.
        String n = primitive.getName();
        if (value instanceof Number && !"boolean".equals(n) && !"char".equals(n)) return true;
        if (value instanceof Character && "char".equals(n)) return true;
        if (value instanceof Boolean && "boolean".equals(n)) return true;
        return false;
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
        return findConstructorTemplate(args);
    }

    /** Public lookup so super(args) dispatch can reach a parent ctor. */
    public MethodTemplate findConstructorTemplate(Object[] args) {
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

        boolean isAbstract() {
            // insureNodesParsed sets blockNode only when the source has
            // a body. A missing body (`void foo();`) leaves blockNode
            // null. An explicit empty body (`void foo() {}`) produces a
            // BSHBlock with zero children and must NOT count as
            // abstract — the user wrote a concrete no-op.
            return decl.blockNode == null;
        }

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

        Class<?>[] paramTypes() {
            return decl.paramsNode == null ? null : decl.paramsNode.paramTypes;
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
