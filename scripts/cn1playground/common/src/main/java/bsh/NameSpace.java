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
 *                                                                           *
 *                                                                           *
 * This file is part of the BeanShell Java Scripting distribution.           *
 * Documentation and updates may be found at http://www.beanshell.org/       *
 * Patrick Niemeyer (pat@pat.net)                                            *
 * Author of Learning Java, O'Reilly & Associates                            *
 *                                                                           *
 *****************************************************************************/
package bsh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A namespace in which methods, variables, and imports (class names) live.
 * This is package public because it is used in the implementation of some bsh
 * commands. However for normal use you should be using methods on
 * bsh.Interpreter to interact with your scripts.
 * <p>
 * A bsh.This object is a thin layer over a NameSpace that associates it with an
 * Interpreter instance. Together they comprise a Bsh scripted object context.
 * <p>
 * Note: Dropping support for JDK 1.1. in favour of Collections
 * Note: This class has gotten too big. It should be broken down a bit. */
public class NameSpace
        implements Serializable, BshClassManager.Listener, NameSource, Cloneable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;
    /** The Constant JAVACODE. */
    public static final NameSpace JAVACODE =
            new NameSpace(null, null, "Called from compiled Java code.");
    static {
        JAVACODE.isMethod = true;
    }
    // Begin instance data
    // Note: if we add something here we should reset it in the clear() method.
    /** The name of this namespace. If the namespace is a method body namespace
     * then this is the name of the method. If it's a class or class instance
     * then it's the name of the class. */
    private String nsName;
    /** The parent. */
    private NameSpace parent;
    /** The variables. */
    private Map<String, Variable> variables = new HashMap<>();
    /** The methods. */
    private Map<String, List<BshMethod>> methods = new HashMap<>();
    /** The imported classes. */
    protected Map<String, String> importedClasses = new HashMap<>();
    /** The imported packages. */
    private List<String> importedPackages = new ArrayList<>();
    /** The imported commands. */
    private List<String> importedCommands = new ArrayList<>();
    /** The imported objects. */
    private List<Object> importedObjects = new ArrayList<>();
    /** The imported static. */
    private List<Class<?>> importedStatic = new ArrayList<>();
    /** The name source listeners. */
    private List<NameSource.Listener> nameSourceListeners = new ArrayList<>();
    /** The package name. */
    private String packageName;
    /** The class manager. */
    private transient BshClassManager classManager;
    /** The this reference. */
    // See notes in getThis()
    private This thisReference;
    /** Name resolver objects. */
    private Map<String, Name> names = new HashMap<>();
    /** The node associated with the creation of this namespace. This is used
     * support getInvocationLine() and getInvocationText(). */
    Node callerInfoNode;
    /** Note that the namespace is a method body namespace. This is used for
     * printing stack traces in exceptions. */
    boolean isMethod;
    /** Note that the namespace is a class body or class instance namespace.
     * This is used for controlling static/object import precedence, etc. Note:
     * We can move this class related behavior out to a subclass of NameSpace,
     * but we'll start here. */
    boolean isClass;
    boolean isInterface;
    boolean isEnum;
    /** The class static. */
    Class<?> classStatic;
    /** The class instance. */
    Object classInstance;
    /** Local class cache for classes resolved through this namespace using
     * getClass() (taking into account imports). Only unqualified class names
     * are cached here (those which might be imported). Qualified names are
     * always absolute and are cached by BshClassManager. */
    private transient Map<String, Class<?>> classCache = new HashMap<>();

    /** Sets the class static.
     * @param clas the new class static */
    void setClassStatic(final Class<?> clas) {
        this.classStatic = clas;
        this.importStatic(clas);
    }

    /** Sets the class instance.
     * @param instance the new class instance */
    void setClassInstance(final Object instance) {
        this.classInstance = instance;
        this.importObject(instance);
    }

    /** Gets the class instance.
     * @return the class instance
     * @throws UtilEvalError the util eval error */
    Object getClassInstance() throws UtilEvalError {
        if (this.classInstance != null)
            return this.classInstance;
        if (this.classStatic != null)
            throw new UtilEvalError(
                "Can't refer to class instance from static context.");
        else
            throw new InterpreterError(
                "Can't resolve class instance 'this' in: " + this);
    }
    // End instance data

    // Begin constructors
    /** Instantiates a new name space.
     * @param parent the parent
     * @param name the name
     * @parent the parent namespace of this namespace. Child namespaces inherit
     *         all variables and methods of their parent and can (of course)
     *         override / shadow them. */
    public NameSpace(final NameSpace parent, final String name) {
        // Note: in this case parent must have a class manager.
        this(parent, null, name);
    }

    /** Instantiates a new name space for name.
     * @param name the name */
    public NameSpace(final String name) {
        this(null, null, name);
    }

    /** Instantiates a new name space for name with class manager.
     * @param name the name
     * @param classManager the class manager */
    public NameSpace(final String name, final BshClassManager classManager) {
        this(null, classManager, name);
    }

    /** Instantiates a new name space.
     * @param parent the parent
     * @param classManager the class manager
     * @param name the name */
    public NameSpace(final NameSpace parent, final BshClassManager classManager,
            final String name) {
        // We might want to do this here rather than explicitly in Interpreter
        // for global (see also prune())
        // if (classManager == null && (parent == null))
        // create our own class manager?
        this.setName(name);
        this.setParent(parent);
        this.setClassManager(classManager);
        // Register for notification of classloader change
        this.getClassManager().addListener(this);
    }

    /** Sets the name.
     * @param name the new name */
    // End constructors
    public void setName(final String name) {
        this.nsName = name;
    }

    /** The name of this namespace. If the namespace is a method body namespace
     * then this is the name of the method. If it's a class or class instance
     * then it's the name of the class.
     * @return the name */
    public String getName() {
        return this.nsName;
    }

    /** Set the node associated with the creation of this namespace. This is
     * used in debugging and to support the getInvocationLine() and
     * getInvocationText() methods.
     * @param node the new node */
    void setNode(final Node node) {
        this.callerInfoNode = node;
    }

    /** Gets the node.
     * @return the node */
    Node getNode() {
        if (this.callerInfoNode != null)
            return this.callerInfoNode;
        if (this.parent != null)
            return this.parent.getNode();
        else
            return null;
    }

    /** Resolve name to an object through this namespace.
     * @param name the name
     * @param interpreter the interpreter
     * @return the object
     * @throws UtilEvalError the util eval error */
    public Object get(final String name, final Interpreter interpreter)
            throws UtilEvalError {
        final CallStack callstack = new CallStack(this);
        return this.getNameResolver(name).toObject(callstack, interpreter);
    }

    /** Set the variable through this namespace.
     * <p>
     * Note: this method is primarily intended for use internally. If you use
     * this method outside of the bsh package and wish to set variables with
     * primitive values you will have to wrap them using bsh.Primitive.
     * </p>
     * @param name the name
     * @param value the value
     * @param strictJava specifies whether strict java rules are applied.
     * @throws UtilEvalError the util eval error
     * @see bsh.Primitive
     *      <p>
     *      Setting a new variable (which didn't exist before) or removing a
     *      variable causes a namespace change. </p>*/
    public void setVariable(final String name, final Object value,
            final boolean strictJava) throws UtilEvalError {
        this.setVariable(name, value, strictJava, true);
    }

    /** Set a variable explicitly in the local scope.
     * @param name the name
     * @param value the value
     * @param strictJava the strict java
     * @return the variable
     * @throws UtilEvalError the util eval error */
    public Variable setLocalVariable(final String name, final Object value,
            final boolean strictJava) throws UtilEvalError {
        return this.setVariable(name, value, strictJava, false/* recurse */);
    }

    /** Set the value of a the variable 'name' through this namespace. The
     * variable may be an existing or non-existing variable. It may live in this
     * namespace or in a parent namespace if recurse is true.
     * <p>
     * Note: this method is primarily intended for use internally. If you use
     * this method outside of the bsh package and wish to set variables with
     * primitive values you will have to wrap them using bsh.Primitive.
     * </p>
     * @param name the name
     * @param value the value
     * @param strictJava specifies whether strict java rules are applied.
     * @param recurse determines whether we will search for the variable in our
     *        parent's scope before assigning locally.
     * @return the variable
     * @throws UtilEvalError the util eval error
     * @see bsh.Primitive
     *      <p>
     *      Setting a new variable (which didn't exist before) or removing a
     *      variable causes a namespace change. </p>*/
    Variable setVariable(final String name, Object value,
            final boolean strictJava, final boolean recurse)
            throws UtilEvalError {
        // primitives should have been wrapped
        if (value == null)
            value = Primitive.NULL; // So then wrap it
        // Locate the variable definition if it exists.
        final Variable existing = this.getVariableImpl(name, recurse);
        // Found an existing variable here (or above if recurse allowed)
        if ( existing != null ) {
            existing.setValue( value, Variable.ASSIGNMENT );
            return existing;
        } else {
            // No previous variable definition found here (or above if recurse)
            if (strictJava)
                throw new UtilEvalError(
                        "(Strict Java mode) Assignment to undeclared variable: "
                                + name);
            // If recurse, set global untyped var, else set it here.
            // NameSpace varScope = recurse ? getGlobal() : this;
            // This modification makes default allocation local
            // NameSpace varScope = this;
            final Variable var = this.createVariable(name, value,
                    null/* modifiers */);
            this.variables.put(name, var);
            this.nameSpaceChanged();
            return var;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * <p>
     * Sets a variable or property. See "setVariable" for rules regarding
     * scoping.
     * </p>
     * <p>
     * We first check for the existence of the variable. If it exists, we set
     * it. If the variable does not exist we look for a property. If the
     * property exists and is writable we set it. Finally, if neither the
     * variable or the property exist, we create a new variable.
     * </p>
     * @param name the name
     * @param value the value
     * @param strictJava specifies whether strict java rules are applied.
     * @throws UtilEvalError the util eval error */
    public void setVariableOrProperty(final String name, final Object value,
            final boolean strictJava) throws UtilEvalError {
        this.setVariableOrProperty(name, value, strictJava, true);
    }

    /** Set a variable or property explicitly in the local scope.
     * <p>
     * Sets a variable or property. See "setLocalVariable" for rules regarding
     * scoping.
     * </p>
     * <p>
     * We first check for the existence of the variable. If it exists, we set
     * it. If the variable does not exist we look for a property. If the
     * property exists and is writable we set it. Finally, if neither the
     * variable or the property exist, we create a new variable.
     * </p>
     * @param name the name
     * @param value the value
     * @param strictJava the strict java
     * @throws UtilEvalError the util eval error */
    void setLocalVariableOrProperty(final String name, final Object value,
            final boolean strictJava) throws UtilEvalError {
        this.setVariableOrProperty(name, value, strictJava, false/* recurse */);
    }

    /** Set the value of a the variable or property 'name' through this
     * namespace.
     * <p>
     * Sets a variable or property. See "setVariableOrProperty" for rules
     * regarding scope.
     * </p>
     * <p>
     * We first check for the existence of the variable. If it exists, we set
     * it. If the variable does not exist we look for a property. If the
     * property exists and is writable we set it. Finally, if neither the
     * variable or the property exist, we create a new variable.
     * </p>
     * @param name the name
     * @param value the value
     * @param strictJava specifies whether strict java rules are applied.
     * @param recurse determines whether we will search for the variable in our
     *        parent's scope before assigning locally.
     * @throws UtilEvalError the util eval error */
    void setVariableOrProperty(final String name, Object value,
            final boolean strictJava, final boolean recurse)
            throws UtilEvalError {
        // primitives should have been wrapped
        if (value == null)
            value = Primitive.NULL;
        // Locate the variable definition if it exists.
        final Variable existing = this.getVariableImpl(name, recurse);
        // Found an existing variable here (or above if recurse allowed)
        if (existing != null)
            try {
                existing.setValue(value, Variable.ASSIGNMENT);
            } catch (final UtilEvalError e) {
                throw new UtilEvalError(
                        "Variable assignment: " + name + ": " + e.getMessage(), e);
            }
        else {
            // No previous variable definition found here (or above if recurse)
            if (strictJava)
                throw new UtilEvalError(
                        "(Strict Java mode) Assignment to undeclared variable: "
                                + name);
            final boolean setProp = this.attemptSetPropertyValue(name, value,
                    null != thisReference ? thisReference.declaringInterpreter
                    : null);
            if (setProp)
                return;
            // If recurse, set global untyped var, else set it here.
            // NameSpace varScope = recurse ? getGlobal() : this;
            // This modification makes default allocation local
            final NameSpace varScope = this;
            varScope.variables.put(name,
                    this.createVariable(name, value, null/* modifiers */));
            this.nameSpaceChanged();
        }
    }

    /** Creates the variable.
     * @param name the name
     * @param value the value
     * @param mods the mods
     * @return the variable
     * @throws UtilEvalError the util eval error */
    protected Variable createVariable(final String name, final Object value,
            final Modifiers mods) throws UtilEvalError {
        return this.createVariable(name, null/* type */, value, mods);
    }

    /** Creates the variable.
     * @param name the name
     * @param type the type
     * @param value the value
     * @param mods the mods
     * @return the variable
     * @throws UtilEvalError the util eval error */
    protected Variable createVariable(final String name, final Class<?> type,
            final Object value, final Modifiers mods) throws UtilEvalError {
        return new Variable(name, type, value, mods);
    }

    /** Creates the variable.
     * @param name the name
     * @param type the type
     * @param lhs the lhs
     * @return the variable
     * @throws UtilEvalError the util eval error */
    protected Variable createVariable(final String name, final Class<?> type,
            final LHS lhs) throws UtilEvalError {
        return new Variable(name, type, lhs);
    }

    /** Remove the variable from the namespace.
     * @param name the name */
    public void unsetVariable(final String name) {
        this.variables.remove(name);
        this.nameSpaceChanged();
    }

    /**
        Get the names of variables defined in this namespace.
        (This does not show variables in parent namespaces).
    */
    public String [] getVariableNames() {
        return this.variables.keySet().toArray(new String[this.variables.size()]);
    }

    /**
        Get the variables defined in this namespace.
        (This does not show variables in parent namespaces).
    */
    public Variable [] getVariables() {
        return this.variables.values().toArray(new Variable[this.variables.size()]);
    }

    /**
        Get the names of methods declared in this namespace.
        (This does not include methods in parent namespaces).
    */
    public String [] getMethodNames()
    {
        return this.methods.keySet().toArray(new String[this.methods.size()]);
    }

    /** Get the methods defined in this namespace. (This does not show methods
     * in parent namespaces). Note: This will probably be renamed
     * getDeclaredMethods()
     * @return the methods */
    public BshMethod[] getMethods() {
        int count = 0;
        for (List<BshMethod> list : this.methods.values()) {
            count += list.size();
        }
        BshMethod[] out = new BshMethod[count];
        int index = 0;
        for (List<BshMethod> list : this.methods.values()) {
            for (BshMethod method : list) {
                out[index++] = method;
            }
        }
        return out;
    }

    /** Get the parent namespace. Note: this isn't quite the same as getSuper().
     * getSuper() returns 'this' if we are at the root namespace.
     * @return the parent */
    public NameSpace getParent() {
        return this.parent;
    }

    /** Check if this namespace is a child of the given namespace.
     * @param parent the possible parent
     * @return true if this is child of parent */
    public boolean isChildOf(NameSpace parent) {
        return null != this.getParent() && (
                this.getParent().equals(parent)
             || this.getParent().isChildOf(parent));
    }

    /** Get the parent namespace' This reference or this namespace' This
     * reference if we are the top.
     * @param declaringInterpreter the declaring interpreter
     * @return the super */
    public This getSuper(final Interpreter declaringInterpreter) {
        if (this.parent != null) {
            if (this.parent.isClass)
                return this.parent.getSuper(declaringInterpreter);
            return this.parent.getThis(declaringInterpreter);
        }
        return this.getThis(declaringInterpreter);
    }

    /** Get the top level namespace or this namespace if we are the top. Note:
     * this method should probably return type bsh.This to be consistent with
     * getThis();
     * @param declaringInterpreter the declaring interpreter
     * @return the global */
    public This getGlobal(final Interpreter declaringInterpreter) {
        if (this.parent != null)
            return this.parent.getGlobal(declaringInterpreter);
        else
            return this.getThis(declaringInterpreter);
    }

    /** A This object is a thin layer over a namespace, comprising a bsh object
     * context. It handles things like the interface types the bsh object
     * supports and aspects of method invocation on it.
     * <p>
     * The declaringInterpreter is here to support callbacks from Java through
     * generated proxies. The scripted object "remembers" who created it for
     * things like printing messages and other per-interpreter phenomenon when
     * called externally from Java.
     * @param declaringInterpreter the declaring interpreter
     * @return the this Note: we need a singleton here so that things like 'this
     *         == this' work (and probably a good idea for speed). Caching a
     *         single instance here seems technically incorrect, considering the
     *         declaringInterpreter could be different under some circumstances.
     *         (Case: a child interpreter running a source() / eval() command).
     *         However the effect is just that the main interpreter that
     *         executes your script should be the one involved in call-backs
     *         from Java. I do not know if there are corner cases where a child
     *         interpreter would be the first to use a This reference in a
     *         namespace or if that would even cause any problems if it did...
     *         We could do some experiments to find out... and if necessary we
     *         could cache on a per interpreter basis if we had weak
     *         references... We might also look at skipping over child
     *         interpreters and going to the parent for the declaring
     *         interpreter, so we'd be sure to get the top interpreter. */
    public This getThis(final Interpreter declaringInterpreter) {
        if (this.thisReference == null)
            this.thisReference = This.getThis(this, declaringInterpreter);
        return this.thisReference;
    }

    /** Gets the class manager.
     * @return the class manager */
    public BshClassManager getClassManager() {
        if (this.classManager != null)
            return this.classManager;
        if (this.parent != null && this.parent != JAVACODE)
            return this.parent.getClassManager();
        this.setClassManager(BshClassManager
                .createClassManager(null/* interp */));
        // Interpreter.debug("No class manager namespace:" +this);
        return this.classManager;
    }

    /** Sets the class manager.
     * @param classManager the new class manager */
    void setClassManager(final BshClassManager classManager) {
        this.classManager = classManager;
    }

    /** Used for serialization. */
    public void prune() {
        this.getClassManager();
        this.setParent(null);
    }

    /** Sets the parent.
     * @param parent the new parent */
    public void setParent(final NameSpace parent) {
        this.parent = parent;
        // If we are disconnected from root we need to handle the def imports
        if (parent == null)
            this.loadDefaultImports();
    }

    /**
     * <p>
     * Get the specified variable or property in this namespace or a parent
     * namespace.
     * </p>
     * <p>
     * We first search for a variable name, and then a property.
     * </p>
     * @param name the name
     * @param interp the interp
     * @return The variable or property value or Primitive.VOID if neither is
     *         defined.
     * @throws UtilEvalError the util eval error */
    public Object getVariableOrProperty(final String name,
            final Interpreter interp) throws UtilEvalError {
        final Object val = this.getVariable(name, true);
        return val == Primitive.VOID
                ? this.getPropertyValue(name, interp)
                : val;
    }

    /** Get the specified variable in this namespace or a parent namespace.
     * <p>
     * Note: this method is primarily intended for use internally. If you use
     * this method outside of the bsh package you will have to use
     * Primitive.unwrap() to get primitive values.
     * @param name the name
     * @return The variable value or Primitive.VOID if it is not defined.
     * @throws UtilEvalError the util eval error
     * @see Primitive#unwrap(Object) */
    public Object getVariable(final String name) throws UtilEvalError {
        return this.getVariable(name, true);
    }

    /** Get the specified variable in this namespace.
     * @param name the name
     * @param recurse If recurse is true then we recursively search through
     *        parent namespaces for the variable.
     *        <p>
     *        Note: this method is primarily intended for use internally. If you
     *        use this method outside of the bsh package you will have to use
     *        Primitive.unwrap() to get primitive values.
     * @return The variable value or Primitive.VOID if it is not defined.
     * @throws UtilEvalError the util eval error
     * @see Primitive#unwrap(Object) */
    public Object getVariable(final String name, final boolean recurse)
            throws UtilEvalError {
        final Variable var = this.getVariableImpl(name, recurse);
        Interpreter.debug("Get variable: ", name, " = ", var);
        return this.unwrapVariable(var);
    }

    /** Locate a variable and return the Variable object with optional recursion
     * through parent name spaces.
     * <p/>
     * If this namespace is static, return only static variables.
     * @param name the name
     * @param recurse the recurse
     * @return the Variable value or null if it is not defined
     * @throws UtilEvalError the util eval error */
    protected Variable getVariableImpl(final String name, final boolean recurse)
            throws UtilEvalError {
        Variable var = null;
        if (this.variables.containsKey(name))
            return this.variables.get(name);
        else
            var = this.getImportedVar(name);
        // try parent
        if (recurse && var == null && this.parent != null)
            var = this.parent.getVariableImpl(name, recurse);
        return var;
    }

    protected void setVariableImpl(Variable var) {
        if (!this.variables.containsKey(var.getName()))
            this.variables.put(var.getName(), var);
    }
    /*
        Get variables declared in this namespace.
    */
    public Variable [] getDeclaredVariables()
    {
        return this.variables.values().toArray(new Variable[this.variables.size()]);
    }

    /** Unwrap a variable to its value.
     * @param var the var
     * @return return the variable value. A null var is mapped to Primitive.VOID
     * @throws UtilEvalError the util eval error */
    protected Object unwrapVariable(final Variable var) throws UtilEvalError {
        return var == null ? Primitive.VOID : var.getValue();
    }

    /** Sets the typed variable.
     * @param name the name
     * @param type the type
     * @param value the value
     * @param isFinal the is final
     * @throws UtilEvalError the util eval error
     * @deprecated See #setTypedVariable(String, Class, Object, Modifiers) */
    @Deprecated
    public void setTypedVariable(final String name, final Class<?> type,
            final Object value, final boolean isFinal) throws UtilEvalError {
        final Modifiers modifiers = new Modifiers(Modifiers.FIELD);
        if (isFinal)
            modifiers.addModifier("final");
        this.setTypedVariable(name, type, value, modifiers);
    }

    /** Declare a variable in the local scope and set its initial value. Value
     * may be null to indicate that we would like the default value for the
     * variable type. (e.g. 0 for integer types, null for object types). An
     * existing typed variable may only be set to the same type. If an untyped
     * variable of the same name exists it will be overridden with the new typed
     * var. The set will perform a Types.getAssignableForm() on the value if
     * necessary.
     * <p>
     * Note: this method is primarily intended for use internally. If you use
     * this method outside of the bsh package and wish to set variables with
     * primitive values you will have to wrap them using bsh.Primitive.
     * @param name the name
     * @param type the type
     * @param value If value is null, you'll get the default value for the type
     * @param modifiers may be null
     * @throws UtilEvalError the util eval error
     * @see bsh.Primitive */
    public void setTypedVariable(final String name, final Class<?> type,
            final Object value, final Modifiers modifiers)
            throws UtilEvalError {
        // Setting a typed variable is always a local operation.
        final Variable existing = this.getVariableImpl(name,
                false/* recurse */);
        // Null value is just a declaration
        // Note: we might want to keep any existing value here instead of reset
        // does the variable already exist? Is it typed?
        if (existing != null && existing.getType() != null)
            // If it had a different type throw error.
            // This allows declaring the same var again, but not with
            // a different (even if assignable) type.
            if (existing.getType() != type)
                throw new UtilEvalError("Typed variable: " + name
                        + " was previously declared with type: "
                        + existing.getType());
            else {
                if (existing.modifiers == null)
                    existing.modifiers = modifiers;
                // else set it and return
                existing.setValue(value, Variable.DECLARATION);
                return;
            }
        // Add the new typed var
        this.variables.put(name,
                this.createVariable(name, type, value, modifiers));
    }

    /** Dissallow static vars outside of a class.
     * @param name is here just to allow the error message to use it protected
     *        void checkVariableModifiers(String name, Modifiers modifiers)
     *        throws UtilEvalError { if (modifiers!=null &&
     *        modifiers.hasModifier("static")) throw new UtilEvalError("Can't
     *        declare static variable outside of class: "+name); }
     * @param method the method
     * @throws UtilEvalError the util eval error Note: this is primarily for
     *         internal use.
     * @see Interpreter#source(String)
     * @see Interpreter#eval(String) */
    public void setMethod(BshMethod method) {
        String name = method.getName();
        if (!this.methods.containsKey(name))
            this.methods.put(name, new ArrayList<BshMethod>(1));
        this.methods.get(name).remove(method);
        this.methods.get(name).add(0, method);
    }

    /** Gets the method.
     * @param name the name
     * @param sig the sig
     * @return the method
     * @throws UtilEvalError the util eval error
     * @see #getMethod(String, Class [], boolean)
     * @see #getMethod(String, Class []) */
    public BshMethod getMethod(final String name, final Class<?>[] sig)
            throws UtilEvalError {
        return this.getMethod(name, sig, false/* declaredOnly */);
    }

    /** Get the bsh method matching the specified signature declared in this
     * name space or a parent.
     * <p>
     * Note: this method is primarily intended for use internally. If you use
     * this method outside of the bsh package you will have to be familiar with
     * BeanShell's use of the Primitive wrapper class.
     * @param name the name
     * @param sig the sig
     * @param declaredOnly if true then only methods declared directly in this
     *        namespace will be found and no inherited or imported methods will
     *        be visible.
     * @return the BshMethod or null if not found
     * @throws UtilEvalError the util eval error
     * @see bsh.Primitive */
    public BshMethod getMethod(final String name, final Class<?>[] sig,
            final boolean declaredOnly) throws UtilEvalError {
        BshMethod method = null;
        Interpreter.debug("Get method: ", name, " ", this );
        // Change import precedence if we are a class body/instance
        // Get import first. Enum blocks may override class methods.
        if (this.isClass && !this.isEnum && !declaredOnly)
            method = this.getImportedMethod(name, sig);
        if (method == null && this.methods.containsKey(name))
            method = Reflect.findMostSpecificBshMethod(sig, methods.get(name));
        if (method == null && !this.isClass && !declaredOnly)
            method = this.getImportedMethod(name, sig);
        // try parent
        if (method == null && !declaredOnly && this.parent != null)
            return this.parent.getMethod(name, sig);
        return method;
    }

    /** Import a class name. Subsequent imports override earlier ones
     * @param name the name */
    public void importClass(final String name) {
        this.importedClasses.put(Name.suffix(name, 1), name);
        this.nameSpaceChanged();
    }

    /** subsequent imports override earlier ones.
     * @param name the name */
    public void importPackage(final String name) {
        this.importedPackages.remove(name);
        this.importedPackages.add(0, name);
        this.nameSpaceChanged();
    }

    /** Import scripted or compiled BeanShell commands in the following package
     * in the classpath. You may use either "/" path or "." package notation.
     * e.g. importCommands("/bsh/commands") or importCommands("bsh.commands")
     * are equivalent. If a relative path style specifier is used then it is
     * made into an absolute path by prepending "/".
     * @param name the name */
    public void importCommands(String name) {
        // dots to slashes
        name = name.replace('.', '/');
        // absolute
        if (!name.startsWith("/"))
            name = "/" + name;
        // remove trailing (but preserve case of simple "/")
        if (name.length() > 1 && name.endsWith("/"))
            name = name.substring(0, name.length() - 1);
        this.importedCommands.remove(name);
        this.importedCommands.add(0, name);
        this.nameSpaceChanged();
    }

    /** A command is a scripted method or compiled command class implementing a
     * specified method signature. Commands are loaded from the classpath and
     * may be imported using the importCommands() method.
     * <p/>
     * This method searches the imported commands packages for a script or
     * command object corresponding to the name of the method. If it is a script
     * the script is sourced into this namespace and the BshMethod for the
     * requested signature is returned. If it is a compiled class the class is
     * returned. (Compiled command classes implement static invoke() methods).
     * <p/>
     * The imported packages are searched in reverse order, so that later
     * imports take priority. Currently only the first object (script or class)
     * with the appropriate name is checked. If another, overloaded form, is
     * located in another package it will not currently be found. This could be
     * fixed.
     * <p/>
     * @param name is the name of the desired command method
     * @param argTypes is the signature of the desired command method.
     * @param interpreter the interpreter
     * @return a BshMethod, Class, or null if no such command is found.
     * @throws UtilEvalError if loadScriptedCommand throws UtilEvalError i.e. on
     *         errors loading a script that was found */
    public Object getCommand(final String name, final Class<?>[] argTypes,
            final Interpreter interpreter) throws UtilEvalError {
        return this.parent != null ? this.parent.getCommand(name, argTypes, interpreter) : null;
    }

    /** Gets the imported method.
     * @param name the name
     * @param sig the sig
     * @return the imported method
     * @throws UtilEvalError the util eval error */
    protected BshMethod getImportedMethod(final String name, final Class<?>[] sig)
            throws UtilEvalError {
        // Try object imports
        for (final Object object : this.importedObjects) {
            final Invocable method = Reflect.resolveJavaMethod(
                   object.getClass(), name, sig, false/* onlyStatic */);
            if (method != null)
                return new BshMethod(method, object);
        }
        // Try static imports
        for (final Class<?> stat : this.importedStatic) {
            final Invocable method = Reflect.resolveJavaMethod(
                    stat, name, sig, true/* onlyStatic */);
            if (method != null)
                return new BshMethod(method, null/* object */);
        }
        return null;
    }

    /** Gets the imported var.
     * @param name the name
     * @return the imported var
     * @throws UtilEvalError the util eval error */
    protected Variable getImportedVar(final String name) throws UtilEvalError {
        Variable var = null;
        // Try object imports
        for (final Object object : this.importedObjects) {
            final Invocable field = Reflect.resolveJavaField(object.getClass(),
                    name, false/* onlyStatic */);
            if (field != null)
                var = this.createVariable(name, field.getReturnType(), new LHS(object, field));
            if (null != var) {
                this.variables.put(name, var);
                return var;
            }
        }
        // Try static imports
        for (final Class<?> stat : this.importedStatic) {
            final Invocable field = Reflect.resolveJavaField(stat,
                    name, true/* onlyStatic */);
            if (field != null) {
                var = this.createVariable(name, field.getReturnType(),
                        new LHS(field));
                this.variables.put(name, var);
                return var;
            }
        }
        return null;
    }

    /** Load a command script from the input stream and find the BshMethod in
     * the target namespace.
     * @param in the in
     * @param name the name
     * @param argTypes the arg types
     * @param resourcePath the resource path
     * @param interpreter the interpreter
     * @return the bsh method
     * @throws UtilEvalError on error in parsing the script or if the the method
     *         is not found after parsing the script. If we want to support
     *         multiple commands in the command path we need to change this to
     *         not throw the exception. */
    private BshMethod loadScriptedCommand(final Object in,
            final String name, final Class<?>[] argTypes,
            final String resourcePath, final Interpreter interpreter)
            throws UtilEvalError {
        return null;
    }

    /** Helper that caches class.
     * @param name the name
     * @param c the c */
    void cacheClass(final String name, final Class<?> c) {
        this.classCache.put(name, c);
    }

    /** Load a class through this namespace taking into account imports. The
     * class search will proceed through the parent namespaces if necessary.
     * @param name the name
     * @return null if not found.
     * @throws UtilEvalError the util eval error */
    public Class<?> getClass(final String name) throws UtilEvalError {
        final Class<?> c = this.getClassImpl(name);
        if (c != null)
            return c;
        if (this.parent != null)
            return this.parent.getClass(name);
        return null;
    }

    /** Implementation of getClass() Load a class through this namespace taking
     * into account imports.
     * <p>
     * Check the cache first. If an unqualified name look for imported class or
     * package. Else try to load absolute name.
     * <p>
     * This method implements caching of unqualified names (normally imports).
     * Qualified names are cached by the BshClassManager. Unqualified absolute
     * class names (e.g. unpackaged Foo) are cached too so that we don't go
     * searching through the imports for them each time.
     * @param name the name
     * @return null if not found.
     * @throws UtilEvalError the util eval error */
    private Class<?> getClassImpl(final String name) throws UtilEvalError {
        Class<?> c = null;
        // Check the cache
        if (this.classCache.containsKey(name)) {
            return this.classCache.get(name);
        }
        // Unqualified (simple, non-compound) name
        final boolean unqualifiedName = !Name.isCompound(name);
        // Unqualified name check imported
        if (unqualifiedName) {
            c = this.getImportedClassImpl(name);
            // if found as imported also cache it
            if (c == null)
                c = this.classForName(name);
            if (c != null) {
                this.cacheClass(name, c);
                return c;
            }
        }
        // Try absolute
        c = this.classForName(name);
        if (c != null) {
            return c;
        }
        // Not found
        Interpreter.debug("getClass(): ", name, " not found in ", this);
        return null;
    }

    /** Try to make the name into an imported class. This method takes into
     * account only imports (class or package) found directly in this NameSpace
     * (no parent chain).
     * @param name the name
     * @return the imported class impl
     * @throws UtilEvalError the util eval error */
    private Class<?> getImportedClassImpl(final String name) throws UtilEvalError {
        // Try explicitly imported class, e.g. import foo.Bar;
        String fullname = this.importedClasses.get(name);
        // not sure if we should really recurse here for explicitly imported
        // class in parent...
        if (fullname != null) {
            /* Found the full name in imported classes. */
            // Try to make the full imported name
            Class<?> clas = this.classForName(fullname);

            if ( clas != null )
                return clas;

            // Handle imported inner class case
            // Imported full name wasn't found as an absolute class
            // If it is compound, try to resolve to an inner class.
            // (maybe this should happen in the BshClassManager?)
            if (Name.isCompound(fullname))
                try {
                    clas = this.getNameResolver(fullname).toClass();
                } catch (final ClassNotFoundException e) { /* not a class */ }
            Interpreter.debug(
                        "imported unpackaged name not found:", fullname);
            // If found cache the full name in the BshClassManager
            if (clas != null) {
                // (should we cache info in not a class case too?)
                this.getClassManager().cacheClassInfo(fullname, clas);
                return clas;
            }
            // It was explicitly imported, but we don't know what it is.
            // should we throw an error here??
            return null;
        }
        /* Try imported packages, e.g. "import foo.bar.*;" in reverse order of
         * import... (give later imports precedence...) */
        for (final String s: this.importedPackages) {
            final Class<?> c = this.classForName(s + "." + name);
            if (c != null)
                return c;
        }
        final BshClassManager bcm = this.getClassManager();
        /* Try super import if available Note: we do this last to allow
         * explicitly imported classes and packages to take priority. This
         * method will also throw an error indicating ambiguity if it
         * exists... */
        if (bcm.hasSuperImport()) {
            final String s = bcm.getClassNameByUnqName(name);
            if (s != null)
                return this.classForName(s);
        }
        return null;
    }

    /** Class<?> for name.
     * @param name the name
     * @return the class */
    private Class<?> classForName(final String name) {
        return this.getClassManager().classForName(name);
    }

    /** Implements NameSource.
     * @return all variable and method names in this and all parent
     *         namespaces */
    public String[] getAllNames() {
        final List<String> vec = new ArrayList<String>();
        this.getAllNamesAux(vec);
        return vec.toArray(new String[vec.size()]);
    }

    /** Helper for implementing NameSource.
     * @param vec the vec */
    protected void getAllNamesAux(final List<String> vec) {
        vec.addAll(this.variables.keySet());
        if ( methods != null )
            vec.addAll(this.methods.keySet());
        if (this.parent != null)
            this.parent.getAllNamesAux(vec);
    }

    /** Implements NameSource Add a listener who is notified upon changes to
     * names in this space.
     * @param listener the listener */
    public void addNameSourceListener(final NameSource.Listener listener) {
        this.nameSourceListeners.add(listener);
    }

    /**
     * Perform "import *;" causing the entire classpath to be mapped.
     * This can take a while.
     * <p>
     * Super imports are different than regular imports.
     * They are done in the context of the ClassManager
     * rather than the NameSpace and thus their effects
     * may persist after the NameSpace is released.
     *
     * It seems irregular that named imports are 'local' to the
     * namespace but super imports are 'global' to the class manager
     * but I suppose for performance reasons it is a good idea
     * to scan the super class path only once and store the results.
     * @throws UtilEvalError the util eval error
     */
    public void doSuperImport() throws UtilEvalError {
        this.getClassManager().doSuperImport();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "NameSpace: "
                + (this.nsName == null
                    ? super.toString()
                    : this.nsName + " (" + super.toString() + ")")
                + (this.isClass ? " (class) " : "")
                + (this.isInterface ? " (interface) " : "")
                + (this.isEnum ? " (enum) " : "")
                + (this.isMethod ? " (method) " : "")
                + (this.classStatic != null ? " (class static) " : "")
                + (this.classInstance != null ? " (class instance) " : "");
    }

    /** Write object.
     * @param s the s
     * @throws IOException Signals that an I/O exception has occurred. For
     *         serialization. Don't serialize non-serializable objects. */
    private synchronized void writeObject(final ObjectOutputStream s)
            throws IOException {
        // clear name resolvers... don't know if this is necessary.
        this.names.clear();
        s.defaultWriteObject();
    }
    /** Re-initialize transient members.
     * @param in the serializer
     * @throws IOException mandatory throwing exception
     * @throws ClassNotFoundException mandatory throwing exception */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        this.classCache = new HashMap<>();
    }
    /** Invoke a method in this namespace with the specified args and
     * interpreter reference. No caller information or call stack is required.
     * The method will appear as if called externally from Java.
     * <p>
     * @param methodName the method name
     * @param args the args
     * @param interpreter the interpreter
     * @return the object
     * @throws EvalError the eval error
     * @see bsh.This.invokeMethod(String methodName, Object [] args,
     *      Interpreter interpreter, CallStack callstack, Node callerInfo,
     *      boolean) */
    public Object invokeMethod(final String methodName, final Object[] args,
            final Interpreter interpreter) throws EvalError {
        return this.invokeMethod(methodName, args, interpreter, null, null);
    }

    /** This method simply delegates to This.invokeMethod();.
     * <p>
     * @param methodName the method name
     * @param args the args
     * @param interpreter the interpreter
     * @param callstack the callstack
     * @param callerInfo the caller info
     * @return the object
     * @throws EvalError the eval error
     * @see bsh.This.invokeMethod(String methodName, Object [] args,
     *      Interpreter interpreter, CallStack callstack, Node
     *      callerInfo) */
    public Object invokeMethod(final String methodName, final Object[] args,
            final Interpreter interpreter, final CallStack callstack,
            final Node callerInfo) throws EvalError {
        return this.getThis(interpreter).invokeMethod(methodName, args,
                interpreter, callstack, callerInfo, false/* declaredOnly */);
    }


    /** Invoke an imported command scoped by namespace hierarchy.
     * When no commands were found attempt to invoke the default invoke
     * method if it is present in the namespace.
     * @param commandName the command name
     * @param args the args
     * @param interpreter the interpreter
     * @param callstack the callstack
     * @param callerInfo the caller info
     * @return the method return data object
     * @throws EvalError the eval error */
    protected Object invokeCommand(final String commandName, final Object[] args,
            final Interpreter interpreter, final CallStack callstack,
            final Node callerInfo) throws EvalError {
        return invokeCommand(commandName, args, interpreter, callstack, callerInfo, false);
    }

    /** Invoke an imported command scoped by namespace hierarchy.
     * Unless ignore invoke is true, when no commands were found attempt to
     * invoke the default invoke method if it is present in the namespace.
     * @param commandName the command name
     * @param args the args
     * @param interpreter the interpreter
     * @param callstack the callstack
     * @param callerInfo the caller info
     * @param ignoreInvoke do not call default invoke if no commands found
     * @return the method return data object
     * @throws EvalError the eval error */
    protected Object invokeCommand(final String commandName, final Object[] args,
            final Interpreter interpreter, final CallStack callstack,
            final Node callerInfo, final boolean ignoreInvoke) throws EvalError {
        Class<?>[] argTypes = Types.getTypes( args );
        Object commandObject;
        try {
            commandObject = getCommand(
                commandName, argTypes, interpreter );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError("Error loading command: ",
                callerInfo, callstack );
        }

        // should try to print usage here if nothing found
        if ( null == commandObject )
        {
            if ( !ignoreInvoke ) {
                // Look for a default invoke() handler method in the namespace
                boolean[] outHasMethod = new boolean[1];
                Object result = invokeDefaultInvokeMethod(commandName, args, interpreter,
                        callstack, callerInfo, outHasMethod);
                if ( outHasMethod[0] )
                    return result;
            }
            throw new EvalException( "Command not found: " +
                StringUtil.methodString( commandName, argTypes ),
                callerInfo, callstack );
        }

        if ( commandObject instanceof BshMethod )
            return ((BshMethod)commandObject).invoke(
                args, interpreter, callstack, callerInfo );

        try {
            return Reflect.invokeCompiledCommand(
                ((Class<?>)commandObject), args, interpreter, callstack, callerInfo );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError("Error invoking compiled command: ",
                    callerInfo, callstack );
        }
    }

    /** Invoke the default invoke method if found in the namespace,
     * Updates the outHasMethod array index 0 by reference, if false then method was
     * not found and the return result is meaningless.
     * @param commandName the command name
     * @param args the args
     * @param interpreter the interpreter
     * @param callstack the callstack
     * @param callerInfo the caller info
     * @param outHasMethod update array by reference if method found
     * @return the method return data object
     * @throws EvalError the eval error */
    @SuppressWarnings("LogicalAssignment")
    protected Object invokeDefaultInvokeMethod(final String commandName, final Object[] args,
            final Interpreter interpreter, final CallStack callstack, final Node callerInfo,
            final boolean[] outHasMethod) throws EvalError {

        BshMethod invokeMethod = null;
        try {
            invokeMethod = getMethod(
                "invoke", new Class [] { null, null } );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(
                "Local method invocation", callerInfo, callstack );
        }

        if ( outHasMethod[0] = ( invokeMethod != null ))
            return invokeMethod.invoke(
                new Object [] { commandName, args },
                interpreter, callstack, callerInfo );

        return null;
    }
    /** Clear all cached classes and names. */
    public void classLoaderChanged() {
        this.nameSpaceChanged();
    }

    /** Clear all cached classes and names. */
    public void nameSpaceChanged() {
        this.classCache.clear();
        this.names.clear();
    }

    /** Import standard packages. Currently:
     *
     * <pre>
     * importClass("bsh.EvalError");
     * importClass("bsh.Interpreter");
     * importClass("bsh.Capabilities");
     * importPackage("java.net");
     * importClass("java.util.Map.Entry");
     * importPackage("java.util.function");
     * importPackage("java.util.stream");
     * importPackage("java.util.regex");
     * importPackage("java.util");
     * importPackage("java.io");
     * importPackage("java.lang");
     * importClass("bsh.FileReader");
     * importPackage("java.math");
     * importCommands("/bsh/commands");
     * </pre>
     */
    public void loadDefaultImports() {
        /** Note: the resolver looks through these in reverse order, per
         * precedence rules... so for max efficiency put the most common ones
         * later. */
        this.importClass("bsh.EvalError");
        this.importClass("bsh.Interpreter");
        this.importClass("bsh.Capabilities");
        this.importClass("java.util.Map.Entry");
        this.importPackage("java.util");
        this.importPackage("java.io");
        this.importPackage("java.lang");
        this.importCommands("/bsh/commands");
    }

    /** This is the factory for Name objects which resolve names within this
     * namespace (e.g. toObject(), toClass(), toLHS()).
     * <p>
     * This was intended to support name resolver caching, allowing Name objects
     * to cache info about the resolution of names for performance reasons.
     * However this not proven useful yet.
     * <p>
     * We'll leave the caching as it will at least minimize Name object
     * creation.
     * <p>
     * (This method would be called getName() if it weren't already used for the
     * simple name of the NameSpace)
     * <p>
     * This method was public for a time, which was a mistake. Use get()
     * instead.
     * @param ambigname the ambigname
     * @return the name resolver */
    Name getNameResolver(final String ambigname) {
        if (!this.names.containsKey(ambigname))
            this.names.put(ambigname, new Name(this, ambigname));
        return this.names.get(ambigname);
    }

    /** Gets the invocation line.
     * @return the invocation line */
    public int getInvocationLine() {
        final Node node = this.getNode();
        if (node != null)
            return node.getLineNumber();
        else
            return -1;
    }

    /** Gets the invocation text.
     * @return the invocation text */
    public String getInvocationText() {
        final Node node = this.getNode();
        if (node != null)
            return node.getText();
        else
            return "<invoked from Java code>";
    }

    /** This is a helper method for working inside of bsh scripts and commands.
     * In that context it is impossible to see a ClassIdentifier object for what
     * it is. Attempting to access a method on a ClassIdentifier will look like
     * a static method invocation. This method is in NameSpace for convenience
     * (you don't have to import bsh.ClassIdentifier to use it);
     * @param ci the ci
     * @return the class */
    public static Class<?> identifierToClass(final ClassIdentifier ci) {
        return ci.getTargetClass();
    }

    /** Clear all variables, methods, and imports from this namespace. If this
     * namespace is the root, it will be reset to the default imports.
     * @see #loadDefaultImports() */
    public void clear() {
        this.variables.clear();
        this.methods.clear();
        this.importedClasses.clear();
        this.importedPackages.clear();
        this.importedCommands.clear();
        this.importedObjects.clear();
        if (this.parent == null)
            this.loadDefaultImports();
        this.classCache.clear();
        this.names.clear();
    }

    /** Import a compiled Java object's methods and variables into this
     * namespace. When no scripted method / command or variable is found locally
     * in this namespace method / fields of the object will be checked. Objects
     * are checked in the order of import with later imports taking precedence.
     * <p/>
     * @param obj the obj Note: this impor pattern is becoming common... could
     *        factor it out into an importedObject List<String> class. */
    public void importObject(final Object obj) {
        this.importedObjects.remove(obj);
        this.importedObjects.add(0, obj);
        this.nameSpaceChanged();
    }

    /** Import static.
     * @param clas the clas */
    public void importStatic(final Class<?> clas) {
        this.importedStatic.remove(clas);
        this.importedStatic.add(0, clas);
        this.nameSpaceChanged();
    }

    /** Set the package name for classes defined in this namespace. Subsequent
     * sets override the package.
     * @param packageName the new package */
    void setPackage(final String packageName) {
        this.packageName = packageName;
    }

    /** Gets the package.
     * @return the package */
    String getPackage() {
        if (this.packageName != null)
            return this.packageName;
        if (this.parent != null)
            return this.parent.getPackage();
        return null;
    }

    /** If a writable property exists for the given name, set it and return
     * true, otherwise do nothing and return false.
     * @param propName the prop name
     * @param value the value
     * @param interp the interp
     * @return true, if successful
     * @throws UtilEvalError the util eval error */
    boolean attemptSetPropertyValue(final String propName, final Object value,
            final Interpreter interp) throws UtilEvalError {
        final String accessorName = Reflect.accessorName(Reflect.SET_PREFIX, propName);
        Object val = Primitive.unwrap(value);
        final Class<?>[] classArray = new Class<?>[] {
                val == null ? null : val.getClass()};
        final BshMethod m = this.getMethod(accessorName, classArray);
        if (m != null)
            try {
                this.invokeMethod(accessorName, new Object[] {value}, interp);
                // m.invoke(new Object[] {value}, interp);
                return true;
            } catch (final EvalError ee) {
                throw new UtilEvalError(
                        "'This' property accessor threw exception: "
                                + ee.getMessage(), ee);
            }
        return false;
    }

    /** Get a property from a scripted object or Primitive.VOID if no such
     * property exists.
     * @param propName the prop name
     * @param interp the interp
     * @return the property value
     * @throws UtilEvalError the util eval error */
    Object getPropertyValue(final String propName, final Interpreter interp)
            throws UtilEvalError {
        String accessorName = Reflect.accessorName(Reflect.GET_PREFIX, propName);
        final Class<?>[] classArray = Reflect.ZERO_TYPES;
        BshMethod m = this.getMethod(accessorName, classArray);
        try {
            if (m != null)
                return m.invoke((Object[]) null, interp);
            accessorName = Reflect.accessorName(Reflect.IS_PREFIX, propName);
            m = this.getMethod(accessorName, classArray);
            if (m != null && m.getReturnType() == Boolean.class)
                return m.invoke((Object[]) null, interp);
            return Primitive.VOID;
        } catch (final EvalError ee) {
            throw new UtilEvalError("'This' property accessor threw exception: "
                    + ee.getMessage(), ee);
        }
    }


    NameSpace copy() {
        try {
            final NameSpace clone = (NameSpace) clone();
            clone.thisReference = null;
            clone.variables = clone(variables);
            clone.methods = clone(methods);
            clone.importedClasses = clone(importedClasses);
            clone.importedPackages = clone(importedPackages);
            clone.importedCommands = clone(importedCommands);
            clone.importedObjects = clone(importedObjects);
            clone.importedStatic = clone(importedStatic);
            clone.names = clone(names);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }


    private <K,V> Map<K,V> clone(final Map<K,V> map) {
        if (map == null) {
            return null;
        }
        return new HashMap<K,V>(map);
    }


    private <T> List<T> clone(final List<T> list) {
        if (list == null) {
            return null;
        }
        return new ArrayList<T>(list);
    }

}
