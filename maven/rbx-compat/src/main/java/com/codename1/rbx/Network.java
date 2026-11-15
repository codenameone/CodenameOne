/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx;

import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;

/// Remote events and functions. A solo engine delivers them in process; a
/// client or server engine hands them to its [Transport], which the
/// multiplayer layer implements over a socket.
public final class Network {
    private Network() {
    }

    /// Carries remote traffic between engines.
    public interface Transport {
        /// Sends an event. `player` is the target on a server, null on a client.
        void fire(Engine from, Instance remote, Instance player, Object[] args);

        /// Invokes a remote function and answers its results. Called on a
        /// script thread; an implementation may yield through the scheduler.
        Object[] invoke(Engine from, Instance remote, Instance player, Object[] args);
    }

    static boolean solo(Engine e) {
        return e.getRole() == Engine.SOLO || e.getTransport() == null;
    }

    static void fireServer(Engine e, Instance remote, Object[] args) {
        if (solo(e)) {
            deliverToServer(remote, e.getLocalPlayer(), args);
        } else {
            e.getTransport().fire(e, remote, null, args);
        }
    }

    static void fireClient(Engine e, Instance remote, Instance player, Object[] args) {
        if (solo(e)) {
            if (player == e.getLocalPlayer()) { // NOPMD CompareObjectsWithEquals - reference identity
                deliverToClient(remote, args);
            }
        } else {
            e.getTransport().fire(e, remote, player, args);
        }
    }

    static void fireAllClients(Engine e, Instance remote, Object[] args) {
        if (solo(e)) {
            deliverToClient(remote, args);
        } else {
            e.getTransport().fire(e, remote, null, args);
        }
    }

    /// Runs `OnServerEvent` for a message that came from `player`.
    public static void deliverToServer(Instance remote, Instance player, Object[] args) {
        Object[] full = new Object[args.length + 1];
        full[0] = player;
        System.arraycopy(args, 0, full, 1, args.length);
        remote.event("OnServerEvent").fire(full);
    }

    /// Runs `OnClientEvent` for a message from the server.
    public static void deliverToClient(Instance remote, Object[] args) {
        remote.event("OnClientEvent").fire(args);
    }

    static Object[] invokeServer(Engine e, Instance remote, Object[] args) {
        if (!solo(e)) {
            return e.getTransport().invoke(e, remote, null, args);
        }
        return answerServer(remote, e.getLocalPlayer(), args);
    }

    static Object[] invokeClient(Engine e, Instance remote, Instance player, Object[] args) {
        if (!solo(e)) {
            return e.getTransport().invoke(e, remote, player, args);
        }
        return answerClient(remote, args);
    }

    /// Runs the server callback for an invocation from `player`.
    public static Object[] answerServer(Instance remote, Instance player, Object[] args) {
        Object cb = remote.prop("OnServerInvoke");
        if (!(cb instanceof LuaFunction)) {
            throw LuaError.rt("RemoteFunction " + remote.getName() + " has no OnServerInvoke callback");
        }
        Object[] full = new Object[args.length + 1];
        full[0] = player;
        System.arraycopy(args, 0, full, 1, args.length);
        return remote.getEngine().invoke((LuaFunction) cb, full);
    }

    /// Runs the client callback for an invocation from the server.
    public static Object[] answerClient(Instance remote, Object[] args) {
        Object cb = remote.prop("OnClientInvoke");
        if (!(cb instanceof LuaFunction)) {
            throw LuaError.rt("RemoteFunction " + remote.getName() + " has no OnClientInvoke callback");
        }
        return remote.getEngine().invoke((LuaFunction) cb, args);
    }
}
