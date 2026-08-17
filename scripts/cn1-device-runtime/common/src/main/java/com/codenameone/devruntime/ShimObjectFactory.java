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
package com.codenameone.devruntime;

import com.codename1.interp.InterpObject;
import com.codename1.interp.InterpObjectFactory;
import com.codename1.interp.InterpRuntime;
import com.codenameone.devruntime.gen.InterpShimRegistry;

/**
 * Produces the host-visible object for an interpreted class, from shims
 * generated before the app shipped.
 *
 * <p>This is the same code on iOS and Android, which was not the original plan.
 * Android could cover interfaces with {@link java.lang.reflect.Proxy} and needs
 * generation only for classes; iOS has no {@code Proxy}, because {@code Proxy}
 * is {@code defineClass} in a trenchcoat and ParparVM has no such thing. Rather
 * than keep two factories that fail differently, both platforms use generated
 * shims for both cases -- a pushed program that works on one now works on the
 * other, and a program that does not gets the same error naming the same
 * missing entry in the same curated list.</p>
 *
 * <p>The cost is that a class implementing several host interfaces at once has
 * no shim, since a shim covers one supertype. That is rare enough to be worth
 * the symmetry, and it fails loudly.</p>
 *
 * @author Shai Almog
 */
public class ShimObjectFactory implements InterpObjectFactory {
    private InterpRuntime runtime;

    /** The runtime used to dispatch calls arriving on a peer. */
    public void attach(InterpRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * The peer's class name, from the registry rather than from reflection.
     *
     * <p>{@code peer.getClass().getName()} is wrong on iOS for exactly the
     * classes this factory produces: ParparVM reconstructs the name from the
     * mangled C symbol, where the package separator and an underscore are the
     * same character, so {@code Interp_Form} returns as {@code Interp/Form}.
     * The registry knows the real name because it generated it.</p>
     */
    public String peerClassName(Object peer) {
        return peer == null ? null : InterpShimRegistry.nameOf(peer);
    }

    public boolean canExtend(String hostSuperclassName) {
        if (hostSuperclassName == null || "java/lang/Object".equals(hostSuperclassName)) {
            return true;
        }
        return InterpShimRegistry.canExtend(hostSuperclassName);
    }

    public Object createPeer(InterpObject object,
                             String hostSuperclassName,
                             String[] hostInterfaceNames,
                             String superConstructorDescriptor,
                             Object[] superConstructorArgs) throws Throwable {
        if (hostSuperclassName != null && !"java/lang/Object".equals(hostSuperclassName)) {
            if (hostInterfaceNames != null && hostInterfaceNames.length > 0) {
                // A shim extends one class and implements nothing else, so the
                // interfaces would be silently dropped: the framework would
                // accept the peer as a Form and then never recognise it as an
                // ActionListener, and the missing callbacks would look like an
                // interpreter bug rather than a missing shim.
                throw new UnsupportedOperationException(
                        object.getType().getName().replace('/', '.') + " extends "
                        + hostSuperclassName.replace('/', '.')
                        + " and also implements a host interface; the device runtime "
                        + "generates a shim per supertype and has none for that combination");
            }
            Object peer = InterpShimRegistry.create(hostSuperclassName, runtime, object,
                    superConstructorDescriptor, superConstructorArgs);
            if (peer == null) {
                throw new UnsupportedOperationException(
                        "no generated shim for " + hostSuperclassName.replace('/', '.')
                        + "; regenerate the shims with scripts/generate-interp-shims.sh and rebuild "
                        + "the device runtime app");
            }
            return peer;
        }
        if (hostInterfaceNames == null || hostInterfaceNames.length == 0) {
            return null;
        }
        if (hostInterfaceNames.length > 1) {
            throw new UnsupportedOperationException(
                    object.getType().getName().replace('/', '.') + " implements "
                    + hostInterfaceNames.length + " host interfaces; the device runtime "
                    + "generates a shim per interface and has none for that combination");
        }
        Object peer = InterpShimRegistry.createInterface(hostInterfaceNames[0], runtime, object);
        if (peer == null) {
            throw new UnsupportedOperationException(
                    "no generated shim implementing " + hostInterfaceNames[0].replace('/', '.')
                    + "; regenerate the shims with scripts/generate-interp-shims.sh and rebuild "
                    + "the device runtime app");
        }
        return peer;
    }
}
