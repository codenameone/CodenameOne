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

import com.codename1.io.SocketConnection;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * One accepted push connection.
 *
 * <p>Public, with a public no-argument constructor, because
 * {@code Socket.listenLoopback} instantiates it reflectively per connection.
 * That is one of the few reflective calls ParparVM does support -- the
 * translator emits a default-constructor function pointer into every
 * {@code struct clazz} -- so the framework's own listener API works unchanged on
 * iOS.</p>
 *
 * @author Shai Almog
 */
public class DeviceRuntimeConnection extends SocketConnection {
    public void connectionEstablished(InputStream is, OutputStream os) {
        // An accepted connection came in over the network, so it has to pair;
        // only a connection this device dialled to loopback is trusted on the
        // strength of the cable.
        DeviceRuntimeService.getInstance().handleAccepted(is, os);
    }

    public void connectionError(int errorCode, String message) {
        // Not fatal: the listener stays bound and the next push is accepted.
        com.codename1.io.Log.p("device runtime connection error " + errorCode + ": " + message);
    }
}
