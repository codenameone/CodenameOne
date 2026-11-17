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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Coroutine;
import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaFunction;
import java.util.ArrayList;
import java.util.List;

/// An event (`RBXScriptSignal`). Script handlers run on their own thread,
/// so a handler may wait; Java listeners run inline.
public final class Signal extends RbxType {
    /// A Java-side subscriber.
    public interface Listener {
        void fired(Object[] args);
    }

    private final String name;
    private final Scheduler sched;
    private final List<Connection> connections = new ArrayList<Connection>();
    private final List<Coroutine> waiters = new ArrayList<Coroutine>();

    public Signal(String name, Scheduler sched) {
        this.name = name;
        this.sched = sched;
    }

    @Override
    public String typeName() {
        return "RBXScriptSignal";
    }

    public String getName() {
        return name;
    }

    public boolean hasListeners() {
        return !connections.isEmpty() || !waiters.isEmpty();
    }

    public Connection connect(LuaFunction fn, boolean once) {
        Connection c = new Connection(this, fn, null, once);
        connections.add(c);
        return c;
    }

    public Connection connect(Listener l) {
        Connection c = new Connection(this, null, l, false);
        connections.add(c);
        return c;
    }

    void remove(Connection c) {
        connections.remove(c);
    }

    /// Drops every connection, as a destroyed instance does.
    public void disconnectAll() {
        for (Connection c : new ArrayList<Connection>(connections)) {
            c.connected = false;
        }
        connections.clear();
        waiters.clear();
    }

    public void fire(Object... args) {
        if (connections.isEmpty() && waiters.isEmpty()) {
            return;
        }
        Connection[] snap = connections.toArray(new Connection[0]);
        for (Connection c : snap) {
            if (!c.connected) {
                continue;
            }
            if (c.once) {
                c.disconnect();
            }
            if (c.listener != null) {
                c.listener.fired(args);
            } else {
                sched.spawn(c.fn, args);
            }
        }
        if (!waiters.isEmpty()) {
            Coroutine[] w = waiters.toArray(new Coroutine[0]);
            waiters.clear();
            for (Coroutine co : w) {
                sched.resume(co, args);
            }
        }
    }

    private static final Method CONNECT = new Method("Connect", 0);
    private static final Method ONCE = new Method("Once", 1);
    private static final Method WAIT = new Method("Wait", 2);
    private static final Method PARALLEL = new Method("ConnectParallel", 3);

    @Override
    public Object get(Interp in, String k) {
        switch (k) {
            case "Connect":
            case "connect":
                return CONNECT;
            case "Once":
                return ONCE;
            case "Wait":
            case "wait":
                return WAIT;
            case "ConnectParallel":
                return PARALLEL;
            default:
                return super.get(in, k);
        }
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        if (id == 2) {
            waiters.add(in.currentThread());
            return in.yield(new Object[0]);
        }
        return ret(connect(Args.fn(a, 1, id == 1 ? "Once" : "Connect"), id == 1));
    }

    @Override
    public String str() {
        return "Signal " + name;
    }

    /// `RBXScriptConnection`.
    public static final class Connection extends RbxType {
        private final Signal signal;
        final LuaFunction fn;
        final Listener listener;
        final boolean once;
        boolean connected = true;

        Connection(Signal signal, LuaFunction fn, Listener listener, boolean once) {
            this.signal = signal;
            this.fn = fn;
            this.listener = listener;
            this.once = once;
        }

        public void disconnect() {
            connected = false;
            signal.remove(this);
        }

        public boolean isConnected() {
            return connected;
        }

        @Override
        public String typeName() {
            return "RBXScriptConnection";
        }

        private static final Method DISCONNECT = new Method("Disconnect", 0);

        @Override
        public Object get(Interp in, String k) {
            if ("Connected".equals(k)) {
                return Boolean.valueOf(connected);
            }
            if ("Disconnect".equals(k) || "disconnect".equals(k)) {
                return DISCONNECT;
            }
            return super.get(in, k);
        }

        @Override
        protected Object[] invoke(int id, Interp in, Object[] a) {
            disconnect();
            return null;
        }

        @Override
        public String str() {
            return "Connection";
        }
    }
}
