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
package com.codename1.impl.interp;

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
public class ReflectionInterpLinker implements InterpLinker {
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

    public ReflectionInterpLinker() {
        this(ReflectionInterpLinker.class.getClassLoader());
    }

    public ReflectionInterpLinker(ClassLoader loader) {
        this.loader = loader;
    }

    public void initializeDefaultBearingInterfaces(String internalName) throws Throwable {
        Object c = findClass(internalName);
        if (c instanceof Class) {
            initializeDefaultBearing((Class<?>) c, 0);
        }
    }

    private void initializeDefaultBearing(Class<?> iface, int depth) throws Throwable {
        if (!iface.isInterface() || depth > 16) {
            return;
        }
        for (Class<?> parent : iface.getInterfaces()) {
            initializeDefaultBearing(parent, depth + 1);
        }
        for (java.lang.reflect.Method m : iface.getDeclaredMethods()) {
            if (m.isDefault()) {
                Class.forName(iface.getName(), true,
                        ReflectionInterpLinker.class.getClassLoader());
                return;
            }
        }
    }

    public void initializeClass(String internalName) throws Throwable {
        Object c = findClass(internalName);
        if (c instanceof Class) {
            Class.forName(((Class) c).getName(), true,
                    ReflectionInterpLinker.class.getClassLoader());
        }
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
        String[] descs = InterpValues.argumentTypes(descriptor);
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
        NoSuchMethodException last = null;
        // Walk up rather than relying on getMethod: the method may be public on
        // a package-private class, or declared on a supertype, and
        // getDeclaredMethod alone would miss inherited declarations.
        for (Class k = c; k != null; k = k.getSuperclass()) {
            try {
                m = k.getDeclaredMethod(name, types);
                break;
            } catch (NoSuchMethodException e) {
                last = e;
            }
        }
        if (m == null) {
            m = findInInterfaces(c, name, types);
        }
        if (m == null) {
            throw last != null ? last : new NoSuchMethodException(key);
        }
        m.setAccessible(true);
        methodCache.put(key, m);
        return m;
    }

    private Method findInInterfaces(Class c, String name, Class[] types) {
        if (c == null) {
            return null;
        }
        Class[] ifaces = c.getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            try {
                return ifaces[i].getDeclaredMethod(name, types);
            } catch (NoSuchMethodException ignore) {
                Method m = findInInterfaces(ifaces[i], name, types);
                if (m != null) {
                    return m;
                }
            }
        }
        return findInInterfaces(c.getSuperclass(), name, types);
    }

    private Field lookupField(String owner, String name)
            throws ClassNotFoundException, NoSuchFieldException {
        String key = owner + '#' + name;
        Field f = fieldCache.get(key);
        if (f != null) {
            return f;
        }
        Class c = resolve(owner);
        NoSuchFieldException last = null;
        for (Class k = c; k != null; k = k.getSuperclass()) {
            try {
                f = k.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                last = e;
            }
        }
        if (f == null) {
            f = findFieldInInterfaces(c, name);
        }
        if (f == null) {
            throw last != null ? last : new NoSuchFieldException(key);
        }
        f.setAccessible(true);
        fieldCache.put(key, f);
        return f;
    }

    private Field findFieldInInterfaces(Class c, String name) {
        if (c == null) {
            return null;
        }
        Class[] ifaces = c.getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            try {
                return ifaces[i].getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                Field f = findFieldInInterfaces(ifaces[i], name);
                if (f != null) {
                    return f;
                }
            }
        }
        return findFieldInInterfaces(c.getSuperclass(), name);
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
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Object invokeSpecial(Object target, String owner, String name, String descriptor,
                                Object[] args) throws Throwable {
        Method m = lookupMethod(owner, name, descriptor);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
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
        return lookupField(owner, name).get(null);
    }

    public void setStatic(String owner, String name, String descriptor, Object value)
            throws Throwable {
        lookupField(owner, name).set(null, value);
    }

    public Object getField(Object target, String owner, String name, String descriptor)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        return lookupField(owner, name).get(target);
    }

    public void setField(Object target, String owner, String name, String descriptor, Object value)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        lookupField(owner, name).set(target, value);
    }

    public boolean isInstance(Object hostClass, Object value) {
        return hostClass != null && ((Class) hostClass).isInstance(value);
    }

    public Object cloneArray(Object source) {
        Class<?> component = source.getClass().getComponentType();
        if (component == null) {
            return null;
        }
        return java.lang.reflect.Array.newInstance(component, java.lang.reflect.Array.getLength(source));
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

    public Object classObject(Object hostClass) {
        return hostClass;
    }
}
