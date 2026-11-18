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
import com.codenameone.examples.brickyard.luau.LuaError;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// One player's side of a multiplayer experience: an [Engine#CLIENT] engine
/// that runs LocalScripts against the world a [ReplicaServer] streams to it.
/// Feed it every message from the server with [#receive], and call [#tick]
/// after each [Engine#step] to send the local character's movement.
///
/// The engine starts empty; the place file is not loaded on a client. Its
/// world arrives as the server's snapshot, which keeps server-only content --
/// ServerScriptService, ServerStorage and Script sources -- off the device.
public final class ReplicaClient implements Network.Transport, Engine.EngineListener, Wire.Ids {
    private final Engine engine;
    private final ReplicaServer.Link server;
    private final Map<Integer, Instance> byId = new HashMap<Integer, Instance>();
    private final Map<Instance, Integer> ids = new HashMap<Instance, Integer>();
    private final Map<Integer, Coroutine> calls = new HashMap<Integer, Coroutine>();
    private final Map<Integer, java.util.List<Object[]>> waiting = new HashMap<Integer, java.util.List<Object[]>>();
    private int missing;
    private int nextCall = 1;
    private boolean applying;
    private boolean joined;

    public ReplicaClient(Engine engine, ReplicaServer.Link server) {
        if (engine.getRole() != Engine.CLIENT) {
            throw new IllegalArgumentException("A replica client needs an Engine.CLIENT engine");
        }
        this.engine = engine;
        this.server = server;
        engine.setTransport(this);
        engine.addListener(this);
    }

    public Engine getEngine() {
        return engine;
    }

    /// Whether the server has admitted this player.
    public boolean isJoined() {
        return joined;
    }

    /// Applies one message from the server.
    public void receive(byte[] message) {
        DataInputStream in = Wire.reader(message); // NOPMD CloseResource - in-memory stream; nothing to close
        applying = true;
        try {
            while (in.available() > 0) {
                apply(in, in.readByte());
            }
        } catch (IOException bad) {
            engine.getScheduler().error(null, "Dropped a malformed message from the server: " + bad.getMessage());
        } finally {
            applying = false;
        }
    }

    private void apply(DataInputStream in, int op) throws IOException {
        switch (op) {
            case Wire.CREATE: {
                Instance parent = Wire.ref(in, engine, this);
                Instance i = readTree(in, parent);
                if (i.getParent() != parent) { // NOPMD CompareObjectsWithEquals - instance identity
                    i.setParent(parent);
                }
                break;
            }
            case Wire.PROP: {
                Instance i = instance(in.readInt());
                String key = in.readUTF();
                missing = 0;
                Object v = Wire.value(in, engine, this);
                if (i != null) {
                    set(i, key, v);
                    await(i, key);
                }
                break;
            }
            case Wire.ATTR: {
                Instance i = instance(in.readInt());
                String key = in.readUTF();
                Object v = Wire.value(in, engine, this);
                if (i != null) {
                    i.setAttribute(key, v);
                }
                break;
            }
            case Wire.PARENT: {
                Instance i = instance(in.readInt());
                Instance parent = Wire.ref(in, engine, this);
                if (i != null) {
                    i.setParent(parent);
                }
                break;
            }
            case Wire.DESTROY: {
                Instance i = instance(in.readInt());
                if (i != null) {
                    forget(i);
                    i.destroy();
                }
                break;
            }
            case Wire.EVENT: {
                Instance remote = Wire.ref(in, engine, this);
                Object[] args = Wire.values(in, engine, this);
                if (remote != null) {
                    Network.deliverToClient(remote, args);
                }
                break;
            }
            case Wire.INVOKE: {
                int call = in.readInt();
                Instance remote = Wire.ref(in, engine, this);
                Object[] args = Wire.values(in, engine, this);
                Object[] r = remote == null ? new Object[0] : Network.answerClient(remote, args);
                Wire.Writer w = new Wire.Writer();
                DataOutputStream out = w.op(Wire.RESULT); // NOPMD CloseResource - in-memory stream; nothing to close
                out.writeInt(call);
                Wire.values(out, engine, this, r);
                server.send(w.done());
                break;
            }
            case Wire.RESULT: {
                Coroutine co = calls.remove(Integer.valueOf(in.readInt()));
                Object[] r = Wire.values(in, engine, this);
                if (co != null) {
                    engine.getScheduler().resume(co, r);
                }
                break;
            }
            case Wire.LOCAL: {
                int id = in.readInt();
                long userId = in.readLong();
                String name = in.readUTF();
                String display = in.readUTF();
                Instance p = engine.addPlayer(userId, name, display);
                bind(id, p);
                String[] kids = {"Backpack", "PlayerGui", "StarterGear"};
                for (String k : kids) {
                    int kid = in.readInt();
                    Instance c = p.findFirstChild(k);
                    if (kid != 0 && c != null) {
                        bind(kid, c);
                    }
                }
                joined = true;
                break;
            }
            case Wire.CHAT: {
                Instance from = Wire.ref(in, engine, this);
                String text = in.readUTF();
                String channel = in.readUTF();
                // The local player's own line was shown when it was sent;
                // the server's relay of it is not a second message.
                if (from == null || from != engine.getLocalPlayer()) { // NOPMD CompareObjectsWithEquals - player identity
                    engine.chat(from, text, channel);
                }
                break;
            }
            case Wire.TAG: {
                Instance i = instance(in.readInt());
                String tag = in.readUTF();
                if (i != null) {
                    engine.addTag(i, tag);
                }
                break;
            }
            default:
                throw new IOException("Unknown op " + op);
        }
    }

    private static void set(Instance i, String key, Object v) {
        if ("Name".equals(key)) {
            i.setName(String.valueOf(v));
        } else {
            i.put(key, v);
        }
    }

    /// Builds a replicated subtree, unparented. A class this runtime does not
    /// know arrives as a Folder, so its children still have a place to live.
    /// An instance every engine makes for itself (a service's
    /// StarterPlayerScripts, the Workspace camera) is not made twice: the
    /// server's copy is merged into the existing one.
    private Instance readTree(DataInputStream in, Instance parent) throws IOException {
        int id = in.readInt();
        String cls = in.readUTF();
        String name = in.readUTF();
        Instance i = parent == null ? null : parent.findFirstChild(name);
        if (i != null && (ids.containsKey(i) || !cls.equals(i.getClassName()))) {
            i = null;
        }
        if (i == null) {
            try {
                i = engine.create(cls);
            } catch (LuaError unknown) {
                engine.getUnsupported().add(cls);
                i = engine.create("Folder");
            }
            i.setName(name);
        }
        bind(id, i);
        int n = in.readInt();
        for (int k = 0; k < n; k++) {
            String key = in.readUTF();
            missing = 0;
            Object v = Wire.value(in, engine, this);
            if (v != null && i.hasProp(key)) {
                i.put(key, v);
            }
            await(i, key);
        }
        n = in.readInt();
        for (int k = 0; k < n; k++) {
            String key = in.readUTF();
            i.setAttribute(key, Wire.value(in, engine, this));
        }
        n = in.readInt();
        for (int k = 0; k < n; k++) {
            engine.addTag(i, in.readUTF());
        }
        n = in.readInt();
        for (int k = 0; k < n; k++) {
            Instance c = readTree(in, i);
            if (c.getParent() != i) { // NOPMD CompareObjectsWithEquals - instance identity
                c.setParent(i);
            }
        }
        return i;
    }

    /// A ref to an instance whose creation is later in the stream (a
    /// player's Character before the model, a Humanoid's RootPart before the
    /// part) is set when that instance arrives.
    private void await(Instance i, String key) {
        if (missing != 0) {
            Integer k = Integer.valueOf(missing);
            java.util.List<Object[]> l = waiting.get(k);
            if (l == null) {
                l = new java.util.ArrayList<Object[]>();
                waiting.put(k, l);
            }
            l.add(new Object[] {i, key});
            missing = 0;
        }
    }

    private void bind(int id, Instance i) {
        Integer key = Integer.valueOf(id);
        java.util.List<Object[]> l = waiting.remove(key);
        if (l != null) {
            for (Object[] w : l) {
                set((Instance) w[0], (String) w[1], i);
            }
        }
        Instance old = byId.put(key, i);
        if (old != null) {
            ids.remove(old);
        }
        ids.put(i, key);
    }

    private void forget(Instance i) {
        Integer id = ids.remove(i);
        if (id != null) {
            byId.remove(id);
        }
        for (Instance c : i.getChildren()) {
            forget(c);
        }
    }

    @Override
    public int id(Instance i) {
        Integer id = ids.get(i);
        return id == null ? 0 : id.intValue();
    }

    @Override
    public Instance instance(int id) {
        if (id < 0) {
            return Wire.service(engine, id);
        }
        Instance i = byId.get(Integer.valueOf(id));
        if (i == null && id > 0) {
            missing = id;
        }
        return i;
    }

    /// Points the camera at the local character, as the client's camera
    /// script does in Roblox. A solo engine does it when it spawns the
    /// character; here the character is replicated from the server, so
    /// nothing else would, and the camera stayed where the place left it
    /// while the player walked off into the distance.
    /// Only a new character takes the camera, as a spawn does in a solo
    /// engine, so a LocalScript that points it elsewhere keeps it.
    private void follow(Instance hum) {
        if (hum == followed) { // NOPMD CompareObjectsWithEquals - reference identity
            return;
        }
        Object c = engine.getWorkspace().prop("CurrentCamera");
        if (c instanceof Instance) {
            ((Instance) c).put("CameraSubject", hum);
            followed = hum;
        }
    }

    /// The humanoid [#follow] last gave the camera.
    private Instance followed;

    /// Sends the local character's root CFrame, move direction and jump.
    public void tick() {
        Instance lp = engine.getLocalPlayer();
        Instance ch = lp == null ? null : lp.ref("Character");
        Instance root = ch == null ? null : ch.findFirstChild("HumanoidRootPart");
        Instance hum = ch == null ? null : ch.findFirstChildOfClass("Humanoid", false);
        if (root == null || hum == null) {
            return;
        }
        follow(hum);
        Wire.Writer w = new Wire.Writer();
        try {
            DataOutputStream out = w.op(Wire.MOVE); // NOPMD CloseResource - in-memory stream; nothing to close
            Wire.value(out, engine, this, root.prop("CFrame"));
            Wire.value(out, engine, this, hum.prop("MoveDirection"));
            out.writeBoolean(hum.bool("Jump"));
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible.getMessage(), impossible);
        }
        server.send(w.done());
    }

    // ---- Network.Transport ----

    @Override
    public void fire(Engine from, Instance remote, Instance player, Object[] args) {
        Wire.Writer w = new Wire.Writer();
        try {
            DataOutputStream out = w.op(Wire.EVENT); // NOPMD CloseResource - in-memory stream; nothing to close
            Wire.ref(out, engine, this, remote);
            Wire.values(out, engine, this, args);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible.getMessage(), impossible);
        }
        server.send(w.done());
    }

    @Override
    public Object[] invoke(Engine from, Instance remote, Instance player, Object[] args) {
        Coroutine co = engine.getInterp().currentThread();
        if (co == null) {
            throw LuaError.rt("InvokeServer must be called from a script thread");
        }
        Integer call = Integer.valueOf(nextCall++);
        calls.put(call, co);
        Wire.Writer w = new Wire.Writer();
        try {
            DataOutputStream out = w.op(Wire.INVOKE); // NOPMD CloseResource - in-memory stream; nothing to close
            out.writeInt(call.intValue());
            Wire.ref(out, engine, this, remote);
            Wire.values(out, engine, this, args);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible.getMessage(), impossible);
        }
        server.send(w.done());
        return engine.getInterp().yield(new Object[0]);
    }

    // ---- EngineListener ----

    @Override
    public void propertyChanged(Instance i, String prop) {
        // Client changes stay on the client, as with FilteringEnabled.
    }

    @Override
    public void ancestryChanged(Instance i) {
        // Client-made instances are local.
    }

    @Override
    public void destroyed(Instance i) {
        forget(i);
    }

    @Override
    public void chat(Instance from, String text, String channel) {
        if (applying || from == null || from != engine.getLocalPlayer()) { // NOPMD CompareObjectsWithEquals - player identity
            return;
        }
        Wire.Writer w = new Wire.Writer();
        try {
            DataOutputStream out = w.op(Wire.CHAT); // NOPMD CloseResource - in-memory stream; nothing to close
            out.writeUTF(text);
            out.writeUTF(channel == null ? "All" : channel);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible.getMessage(), impossible);
        }
        server.send(w.done());
    }
}
