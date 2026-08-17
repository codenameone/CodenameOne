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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * An {@link InterpObjectFactory} for platforms with reflection, covering the
 * interface case with {@link Proxy}.
 *
 * <p>{@code Proxy} solves half the problem completely and for free: an
 * interpreted class implementing {@code ActionListener} becomes a real
 * {@code ActionListener} the framework can hold and call, with no code
 * generation, no dex loading, and nothing for App Review to object to.</p>
 *
 * <p>It solves none of the other half. {@code Proxy} cannot extend a class, so
 * an interpreted {@code class MyForm extends Form} still needs a per-platform
 * mechanism -- vtable synthesis on iOS, generated subclasses on Android. This
 * factory reports that honestly through {@link #canExtend} rather than
 * producing a peer that the framework would accept and then never dispatch
 * to.</p>
 *
 * @author Shai Almog
 */
public class ProxyInterpObjectFactory implements InterpObjectFactory {
    private final InterpLinker linker;
    private InterpRuntime runtime;

    public ProxyInterpObjectFactory(InterpLinker linker) {
        this.linker = linker;
    }

    /** Runtime used to dispatch calls that arrive on a proxy. */
    public void attach(InterpRuntime runtime) {
        this.runtime = runtime;
    }

    public String peerClassName(Object peer) {
        // The JVM reports this faithfully; only ParparVM does not.
        return peer == null ? null : peer.getClass().getName().replace('.', '/');
    }

    public boolean canExtend(String hostSuperclassName) {
        // java.lang.Object is not really "extending" anything -- every class
        // has it as an ancestor and no dispatch depends on it.
        return hostSuperclassName == null || "java/lang/Object".equals(hostSuperclassName);
    }

    public Object createPeer(final InterpObject object,
                             String hostSuperclassName,
                             String[] hostInterfaceNames,
                             String superConstructorDescriptor,
                             Object[] superConstructorArgs) throws Throwable {
        if (!canExtend(hostSuperclassName)) {
            throw new UnsupportedOperationException(
                    "this platform cannot produce an interpreted subclass of "
                            + hostSuperclassName.replace('/', '.')
                            + "; extending host classes needs the platform object factory "
                            + "(vtable synthesis on iOS, generated subclasses on Android)");
        }
        if (hostInterfaceNames == null || hostInterfaceNames.length == 0) {
            return null;
        }
        Class[] ifaces = new Class[hostInterfaceNames.length];
        for (int i = 0; i < hostInterfaceNames.length; i++) {
            ifaces[i] = Class.forName(hostInterfaceNames[i].replace('/', '.'));
        }
        return Proxy.newProxyInstance(getClass().getClassLoader(), ifaces,
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        String desc = descriptorOf(method);
                        InterpMethod m = object.getType().resolve(method.getName(), desc);
                        if (m == null) {
                            // Object's own methods reach a proxy too, and an
                            // interpreted class that does not override them
                            // should behave like any other object.
                            if ("toString".equals(method.getName()) && args == null) {
                                return object.toString();
                            }
                            if ("hashCode".equals(method.getName()) && args == null) {
                                return Integer.valueOf(System.identityHashCode(object));
                            }
                            if ("equals".equals(method.getName()) && args != null
                                    && args.length == 1) {
                                return Boolean.valueOf(proxy == args[0]);
                            }
                            throw new AbstractMethodError(
                                    object.getType().getName() + "." + method.getName());
                        }
                        return runtime.invoke(m, object, args);
                    }
                });
    }

    /** The JVM descriptor of a reflected method. */
    static String descriptorOf(Method m) {
        StringBuilder sb = new StringBuilder("(");
        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(descriptorOf(params[i]));
        }
        return sb.append(')').append(descriptorOf(m.getReturnType())).toString();
    }

    static String descriptorOf(Class c) {
        if (c == Void.TYPE) return "V";
        if (c == Boolean.TYPE) return "Z";
        if (c == Byte.TYPE) return "B";
        if (c == Character.TYPE) return "C";
        if (c == Short.TYPE) return "S";
        if (c == Integer.TYPE) return "I";
        if (c == Long.TYPE) return "J";
        if (c == Float.TYPE) return "F";
        if (c == Double.TYPE) return "D";
        if (c.isArray()) return c.getName().replace('.', '/');
        return "L" + c.getName().replace('.', '/') + ";";
    }
}
