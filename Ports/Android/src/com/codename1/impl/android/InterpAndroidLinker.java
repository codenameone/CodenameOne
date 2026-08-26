/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.impl.android;

import com.codename1.impl.interp.InterpLinker;
import com.codename1.impl.interp.InterpValuesAccess;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * The reflection-backed {@link InterpLinker}, for platforms that have
 * reflection: the JavaSE simulator and Android.
 *
 * <p>iOS needs a different backend entirely -- ParparVM has no
 * {@code Method.invoke} -- which binds through the translator's per-method
 * invoke thunks and symbol table instead. Both sit behind the same interface so
 * the interpreter never branches on platform.</p>
 *
 * <p>Lookups are memoised on (owner, name, descriptor). Reflection's own
 * {@code getMethod} walks the hierarchy on every call and allocates a
 * {@code Class[]} to do it; a pushed program calls the same handful of
 * framework methods in a loop, so caching the resolved {@link Method} is the
 * difference between "usable" and "visibly slow".</p>
 *
 * @author Shai Almog
 */
public class InterpAndroidLinker implements InterpLinker {
    private final ClassLoader loader;
    // Concurrent, not plain HashMap: the interpreter is entered from every thread
    // the pushed program touches, and a resolution cache is exactly the shared
    // state that gets hit from all of them at once. An unsynchronised HashMap
    // under concurrent put does not merely lose an entry -- it can return null
    // for a key that is present, which surfaces as NoClassDefFoundError for a
    // class that plainly exists.
    private final Map<String, Class> classCache =
            java.util.Collections.synchronizedMap(new HashMap<String, Class>());
    private final Map<String, Method> methodCache =
            java.util.Collections.synchronizedMap(new HashMap<String, Method>());
    private final Map<String, Constructor> ctorCache =
            java.util.Collections.synchronizedMap(new HashMap<String, Constructor>());
    private final Map<String, Field> fieldCache =
            java.util.Collections.synchronizedMap(new HashMap<String, Field>());

    public InterpAndroidLinker() {
        this(InterpAndroidLinker.class.getClassLoader());
    }

    public InterpAndroidLinker(ClassLoader loader) {
        this.loader = loader;
    }

    public Object findClass(String internalName) {
        Class c = classCache.get(internalName);
        if (c != null) {
            return c;
        }
        try {
            c = resolve(internalName);
        } catch (ClassNotFoundException e) {
            return null;
        }
        classCache.put(internalName, c);
        return c;
    }

    public void initializeClass(String internalName) throws Throwable {
        // findClass resolves with initialize=false on purpose; this is the one
        // caller that wants the initializer to have run.
        Object c = findClass(internalName);
        if (c instanceof Class) {
            Class.forName(((Class)c).getName(), true, loader);
        }
    }

    public void initializeDefaultBearingInterfaces(String internalName) throws Throwable {
        Object c = findClass(internalName);
        if (c instanceof Class) {
            initializeDefaultBearing((Class)c, new java.util.HashSet());
        }
    }

    /// Superinterfaces first, then the interface itself if it declares a
    /// default method -- the order JLS 12.4.1 gives, applied to whatever depth
    /// the app's own hierarchy has.
    ///
    /// Bounded by what has already been seen rather than by a depth number.
    /// The walk has to terminate because a diamond visits the same interface
    /// twice, not because a hierarchy is too deep -- and a cap of sixteen
    /// edges silently skipped a legal ancestor, leaving the default methods it
    /// declares uninitialized when a pushed class implemented it.
    private void initializeDefaultBearing(Class iface, java.util.Set visited) throws Throwable {
        if (!iface.isInterface() || !visited.add(iface)) {
            return;
        }
        Class[] parents = iface.getInterfaces();
        for (int i = 0; i < parents.length; i++) {
            initializeDefaultBearing(parents[i], visited);
        }
        java.lang.reflect.Method[] methods = iface.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].isDefault()) {
                Class.forName(iface.getName(), true, loader);
                return;
            }
        }
    }

    private Class resolve(String internalName) throws ClassNotFoundException {
        if (internalName.length() == 1) {
            switch (internalName.charAt(0)) {
                case 'Z': return Boolean.TYPE;
                case 'B': return Byte.TYPE;
                case 'C': return Character.TYPE;
                case 'S': return Short.TYPE;
                case 'I': return Integer.TYPE;
                case 'J': return Long.TYPE;
                case 'F': return Float.TYPE;
                case 'D': return Double.TYPE;
                case 'V': return Void.TYPE;
                default: break;
            }
        }
        if (internalName.startsWith("[")) {
            return Class.forName(internalName.replace('/', '.'), false, loader);
        }
        if (internalName.startsWith("L") && internalName.endsWith(";")) {
            return Class.forName(
                    internalName.substring(1, internalName.length() - 1).replace('/', '.'),
                    false, loader);
        }
        return Class.forName(internalName.replace('/', '.'), false, loader);
    }

    private Class[] paramTypes(String descriptor) throws ClassNotFoundException {
        String[] descs = InterpValuesAccess.argumentTypes(descriptor);
        Class[] types = new Class[descs.length];
        for (int i = 0; i < descs.length; i++) {
            types[i] = resolve(descs[i]);
        }
        return types;
    }

    private Method lookupMethod(String owner, String name, String descriptor)
            throws ClassNotFoundException, NoSuchMethodException {
        String key = owner + '.' + name + descriptor;
        Method m = methodCache.get(key);
        if (m != null) {
            return m;
        }
        Class c = resolve(owner);
        Class[] types = paramTypes(descriptor);
        // The bytecode's descriptor names the return type too; getDeclaredMethod
        // ignores it, so a host build where `String value()` became
        // `Object value()` would otherwise bind and hand the caller an object
        // of the wrong static type. Skip a candidate whose return type differs
        // -- it is a NoSuchMethodError to the pushed code by JVMS 5.4.3.3.
        Class expectedReturn = resolve(InterpValuesAccess.returnType(descriptor));
        NoSuchMethodException last = null;
        // Walk up rather than relying on getMethod: the method may be public on
        // a package-private class, or declared on a supertype, and
        // getDeclaredMethod alone would miss inherited declarations.
        // Private methods are not inherited: for
        // `class A { private void m() {} } class B extends A implements I {}`
        // where `I` declares `m`, `invokevirtual B.m` on the JVM raises
        // IllegalAccessError, not a call to A.m. `setAccessible(true)`
        // would otherwise let this run the private method the JVM refuses.
        for (Class k = c; k != null; k = k.getSuperclass()) {
            try {
                Method candidate = k.getDeclaredMethod(name, types);
                if (!candidate.getReturnType().equals(expectedReturn)) {
                    last = new NoSuchMethodException(
                            k.getName() + "." + name + descriptor
                            + " (return type " + candidate.getReturnType().getName()
                            + " does not match)");
                    continue;
                }
                if (k != c && Modifier.isPrivate(candidate.getModifiers())) {
                    throw new IllegalAccessError(k.getName() + "." + name + descriptor
                            + " is private and not inherited by "
                            + owner.replace('/', '.'));
                }
                m = candidate;
                break;
            } catch (NoSuchMethodException e) {
                last = e;
            }
        }
        if (m == null) {
            m = findInInterfaces(c, name, types, expectedReturn);
        }
        if (m == null) {
            throw last != null ? last : new NoSuchMethodException(key);
        }
        m.setAccessible(true);
        methodCache.put(key, m);
        return m;
    }

    private Method findInInterfaces(Class c, String name, Class[] types, Class expectedReturn) {
        // Collect every inheritable interface declaration reachable through
        // this class or any of its superclasses, then pick the maximally
        // specific per JLS 5.4.3.3. Returning the first depth-first hit
        // instead would pick I.m over J.m on a class declaring
        // `implements I, J` where `J extends I` overrides the default -- and
        // the same problem happens when the more-specific interface is
        // inherited through a superclass, which is why the pool is drawn from
        // the whole chain, not just this class's own interfaces.
        java.util.ArrayList<Method> candidates = new java.util.ArrayList<Method>();
        java.util.HashSet<Class> visited = new java.util.HashSet<Class>();
        for (Class k = c; k != null; k = k.getSuperclass()) {
            for (Class iface : k.getInterfaces()) {
                collectInterfaceCandidates(iface, name, types, expectedReturn, candidates, visited);
            }
        }
        int count = candidates.size();
        if (count == 0) {
            return null;
        }
        // Prefer concrete defaults over abstract declarations. Abstracts are
        // still kept in the collection because `invokevirtual A.m` on
        // `abstract class A implements I {}` resolves to I.m even when I.m
        // is abstract -- reflection's virtual dispatch runs the concrete
        // override on the receiver -- but a concrete sibling default beats
        // any abstract per JVMS 5.4.3.3.
        java.util.ArrayList<Method> concrete = new java.util.ArrayList<Method>();
        for (int i = 0; i < count; i++) {
            if (!Modifier.isAbstract(candidates.get(i).getModifiers())) {
                concrete.add(candidates.get(i));
            }
        }
        java.util.List<Method> pool = concrete.isEmpty() ? candidates : concrete;
        int psz = pool.size();
        if (psz == 1) {
            return pool.get(0);
        }
        // Collect maximally specific candidates: keep only those that no
        // other candidate's declaring interface subtypes. Also collapse
        // duplicates by declaring class -- the same interface's default only
        // needs to appear once in the set.
        java.util.ArrayList<Method> maximal = new java.util.ArrayList<Method>();
        java.util.HashSet<Class> seenDeclaring = new java.util.HashSet<Class>();
        for (int i = 0; i < psz; i++) {
            Method mi = pool.get(i);
            Class declaringA = mi.getDeclaringClass();
            if (!seenDeclaring.add(declaringA)) {
                continue;
            }
            boolean dominated = false;
            for (int j = 0; j < psz; j++) {
                if (i == j) {
                    continue;
                }
                Class declaringB = pool.get(j).getDeclaringClass();
                // Some other candidate is on a proper subinterface of this
                // one, so it is more specific and this one is not maximal.
                if (!declaringA.equals(declaringB)
                        && declaringA.isAssignableFrom(declaringB)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                maximal.add(mi);
            }
        }
        if (maximal.size() == 1) {
            return maximal.get(0);
        }
        // Multiple non-abstract maximally specific methods that do not
        // dominate each other is IncompatibleClassChangeError per JVMS
        // 5.4.3.3 -- possible after binary-compatible interface evolution
        // (an interface adds a default that a sibling interface also
        // declares). Silently picking one would run an arbitrary body the
        // JVM refuses. All-abstract pools do not conflict this way: every
        // abstract candidate dispatches through the receiver, so any
        // maximal one is a valid resolution target.
        if (maximal.size() > 1 && !concrete.isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < maximal.size(); i++) {
                if (i > 0) {
                    message.append(", ");
                }
                message.append(maximal.get(i).getDeclaringClass().getName());
            }
            throw new IncompatibleClassChangeError("conflicting default methods for "
                    + name + " on " + c.getName() + ": " + message);
        }
        return pool.get(0);
    }

    private void collectInterfaceCandidates(Class iface, String name, Class[] types,
                                            Class expectedReturn,
                                            java.util.ArrayList<Method> candidates,
                                            java.util.HashSet<Class> visited) {
        if (!visited.add(iface)) {
            return;
        }
        try {
            Method m = iface.getDeclaredMethod(name, types);
            int mods = m.getModifiers();
            // Static and private declarations are never inheritable, so
            // they cannot serve as interface defaults. Abstract methods do
            // stay in the pool: `invokevirtual A.m` on `abstract A
            // implements I` resolves to I.m even when it is abstract, and
            // reflection's virtual dispatch runs the concrete override on
            // the receiver. `findInInterfaces` prefers concrete siblings
            // over abstracts when both are present.
            // A candidate whose return type differs from the descriptor is
            // discarded for the same reason lookupMethod skips them on the
            // superclass chain: the caller was compiled against a different
            // shape and reflection would otherwise silently bind it.
            if (!Modifier.isStatic(mods) && !Modifier.isPrivate(mods)
                    && m.getReturnType().equals(expectedReturn)) {
                candidates.add(m);
            }
        } catch (NoSuchMethodException ignore) {
            // Not declared here -- keep walking; a superinterface may declare it.
        }
        for (Class parent : iface.getInterfaces()) {
            collectInterfaceCandidates(parent, name, types, expectedReturn, candidates, visited);
        }
    }

    private Field lookupField(String owner, String name, String descriptor)
            throws ClassNotFoundException, NoSuchFieldException {
        String key = owner + '#' + name + '#' + descriptor;
        Field f = fieldCache.get(key);
        if (f != null) {
            return f;
        }
        Class c = resolve(owner);
        // Name alone is not enough: a bundle compiled against `String x` would
        // otherwise bind to a rebuilt host's `Object x` and the caller would
        // read the wrong slot -- for a primitive/reference change that means
        // reading a long as an object reference. Match on the descriptor's
        // Class too, so a type change becomes NoSuchFieldError.
        Class expected = resolve(descriptor);
        f = resolveField(c, name, expected);
        if (f == null) {
            throw new NoSuchFieldException(key);
        }
        f.setAccessible(true);
        fieldCache.put(key, f);
        return f;
    }

    // JVMS 5.4.3.2: at each level, declared field wins over a superinterface's,
    // and a superinterface's wins over the superclass's -- so a private field in
    // A never masks a public field the subclass exposes via an interface.
    private Field resolveField(Class c, String name, Class expected) {
        if (c == null) {
            return null;
        }
        try {
            Field candidate = c.getDeclaredField(name);
            if (candidate.getType().equals(expected)) {
                return candidate;
            }
        } catch (NoSuchFieldException ignore) {
        }
        for (Class iface : c.getInterfaces()) {
            Field f = resolveField(iface, name, expected);
            if (f != null) {
                return f;
            }
        }
        return resolveField(c.getSuperclass(), name, expected);
    }

    public Object construct(Object hostClass, String descriptor, Object[] args) throws Throwable {
        Class c = (Class) hostClass;
        String key = c.getName() + "<init>" + descriptor;
        Constructor ctor = ctorCache.get(key);
        if (ctor == null) {
            ctor = c.getDeclaredConstructor(paramTypes(descriptor));
            ctor.setAccessible(true);
            ctorCache.put(key, ctor);
        }
        try {
            return ctor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Object invokeVirtual(Object target, String owner, String name, String descriptor,
                                Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        // Resolve against the *declared* owner, not the receiver's concrete
        // class. Method.invoke already dispatches virtually, so the override is
        // still reached -- and resolving on the concrete class would often land
        // on a non-public implementation type (ArrayList$Itr for an iterator),
        // where setAccessible now throws InaccessibleObjectException because
        // java.base does not open java.util to an unnamed module.
        Method m = lookupMethod(owner, name, descriptor);
        // A host method that changed from instance to static between the
        // bundle's compile and the installed framework must not bind here:
        // Method.invoke would ignore the receiver and run it as a static,
        // silently executing a different API shape. The JVM raises
        // IncompatibleClassChangeError; mirror that.
        if (Modifier.isStatic(m.getModifiers())) {
            throw new IncompatibleClassChangeError(owner + "." + name + " is static");
        }
        // Same shape for a public/protected method the installed framework
        // has since made private: setAccessible(true) would otherwise call
        // the private declaration silently, and the JVM raises
        // IllegalAccessError for invokevirtual on a private method (nestmate
        // access requires invokespecial). lookupMethod already refuses a
        // private declaration inherited from a superclass; this covers the
        // directly-declared case that walk permits.
        if (Modifier.isPrivate(m.getModifiers())) {
            throw new IllegalAccessError(owner + "." + name + " is private");
        }
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Object invokeSpecial(Object target, String owner, String name, String descriptor,
                                Object[] args) throws Throwable {
        Method m = lookupMethod(owner, name, descriptor);
        if (Modifier.isStatic(m.getModifiers())) {
            throw new IncompatibleClassChangeError(owner + "." + name + " is static");
        }
        // No private check here: invokespecial's whole role -- super, init,
        // nestmate -- is to reach methods invokevirtual cannot, and a private
        // one on the declared owner is a legitimate target.
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public boolean hasMethod(String owner, String name, String descriptor) {
        try {
            // The call is the question; lookupMethod either answers or throws.
            lookupMethod(owner, name, descriptor);
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }

    public Object invokeStatic(String owner, String name, String descriptor, Object[] args)
            throws Throwable {
        Method m = lookupMethod(owner, name, descriptor);
        if (!Modifier.isStatic(m.getModifiers())) {
            throw new IncompatibleClassChangeError(owner + "." + name + " is not static");
        }
        try {
            return m.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Object getStatic(String owner, String name, String descriptor) throws Throwable {
        Field f = lookupField(owner, name, descriptor);
        requireStaticField(f, owner, name);
        return f.get(null);
    }

    public void setStatic(String owner, String name, String descriptor, Object value)
            throws Throwable {
        Field f = lookupField(owner, name, descriptor);
        requireStaticField(f, owner, name);
        f.set(null, value);
    }

    public Object getField(Object target, String owner, String name, String descriptor)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        Field f = lookupField(owner, name, descriptor);
        requireInstanceField(f, owner, name);
        return f.get(target);
    }

    public void setField(Object target, String owner, String name, String descriptor, Object value)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        Field f = lookupField(owner, name, descriptor);
        requireInstanceField(f, owner, name);
        f.set(target, value);
    }

    // A host field that changed instance/static between the bundle's compile
    // and the installed framework must not bind through the opposite access
    // opcode: Field.get(target) silently ignores the receiver on a static
    // slot, and Field.get(null) on an instance slot throws NPE from deep in
    // reflection with no context. The JVM raises IncompatibleClassChangeError;
    // mirror that here for GETFIELD/PUTFIELD/GETSTATIC/PUTSTATIC.
    private void requireStaticField(Field f, String owner, String name) {
        if (!Modifier.isStatic(f.getModifiers())) {
            throw new IncompatibleClassChangeError(owner + "." + name + " is not static");
        }
    }

    private void requireInstanceField(Field f, String owner, String name) {
        if (Modifier.isStatic(f.getModifiers())) {
            throw new IncompatibleClassChangeError(owner + "." + name + " is static");
        }
    }

    public boolean isInstance(Object hostClass, Object value) {
        return hostClass != null && ((Class) hostClass).isInstance(value);
    }

    public Object newArray(String componentDescriptor, int length) throws Throwable {
        return Array.newInstance(resolve(componentDescriptor), length);
    }

    public Object newMultiArray(String arrayDescriptor, int[] dimensions) throws Throwable {
        // The descriptor names the whole array type; strip one '[' per
        // dimension being allocated to get the component Array.newInstance
        // wants.
        String component = arrayDescriptor.substring(dimensions.length);
        return Array.newInstance(resolve(component), dimensions);
    }

    public Object cloneArray(Object source) {
        Class component = source.getClass().getComponentType();
        if (component == null) {
            return null;
        }
        return Array.newInstance(component, Array.getLength(source));
    }

    public Object classObject(Object hostClass) {
        return hostClass;
    }
}
