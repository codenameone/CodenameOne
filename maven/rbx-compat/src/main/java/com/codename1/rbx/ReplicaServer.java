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

import com.codename1.luau.Coroutine;
import com.codename1.luau.LuaError;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The authoritative side of a multiplayer experience. It owns a
/// [Engine#SERVER] engine, which runs the place's Scripts; every client is a
/// [ReplicaClient] over some [Link] (a game socket, or a queue in a test).
///
/// What a client sees is the contents of [Wire#SERVICES], streamed as a
/// snapshot when it joins and as ops after every [#flush]: creations,
/// property and attribute changes, reparents and destroys, remote events and
/// remote function calls. Property values are read when the ops are written,
/// so ten changes to a part in one frame cost one op.
///
/// Each client drives its own character, as Roblox's network ownership does:
/// it sends the root part's CFrame every frame, the server takes it if the
/// move is physically plausible for the humanoid's speed, and corrects the
/// client when it is not. Nothing else a client sends changes the world
/// except through the place's own RemoteEvent and RemoteFunction handlers.
public final class ReplicaServer implements Network.Transport, Engine.EngineListener, Wire.Ids {
    /// Carries one message to the other side.
    public interface Link {
        void send(byte[] message);
    }

    private static final String[] MOTION = {"CFrame", "Position", "Orientation", "AssemblyLinearVelocity",
        "Velocity", "MoveDirection", "Jump"};
    private static final double FRAME = 1 / 60.0;

    /// Ops one message may carry; the rest of a longer message is ignored,
    /// as a malformed tail is. A real client sends a handful per frame.
    static final int MAX_OPS_PER_MESSAGE = 64;
    /// Ops one player may have applied per server frame, across messages.
    static final int MAX_OPS_PER_FRAME = 128;
    /// Chat lines per second a player may send, and the burst allowed.
    static final double CHAT_PER_SECOND = 2;
    static final double CHAT_BURST = 5;
    /// InvokeServer calls per second a player may make, and the burst.
    static final double INVOKE_PER_SECOND = 30;
    static final double INVOKE_BURST = 60;
    /// Longest chat line relayed, in characters, as Roblox caps it.
    static final int MAX_CHAT_LENGTH = 200;
    /// Longest channel name accepted.
    static final int MAX_CHANNEL_LENGTH = 50;

    private final Engine engine;
    private final Map<Instance, Integer> ids = new HashMap<Instance, Integer>();
    private final Map<Integer, Instance> byId = new HashMap<Integer, Instance>();
    private final List<Peer> peers = new ArrayList<Peer>();
    private final List<Object[]> ops = new ArrayList<Object[]>();
    private final Set<String> dirty = new HashSet<String>();
    private final Map<Integer, PendingCall> calls = new HashMap<Integer, PendingCall>();
    private int nextId = 1;
    private int nextCall = 1;
    private long seq;
    private int rejectedMoves;

    private static final class Peer {
        final Link link;
        final long joined;
        final Instance player;
        long lastMoveFrame;
        boolean correct;
        // Movement allowance, refilled once per server frame from the time
        // that passed and spent by every MOVE in it. A fresh allowance per
        // op let 500 MOVEs in one frame add up to a teleport.
        double walkBudget;
        double riseBudget;
        double fallBudget;
        // Flood limits: ops this frame, and token buckets for the two ops that
        // run server code or reach every client (refilled per frame).
        long opsFrame = -1;
        int opsThisFrame;
        long rateFrame;
        double chatTokens = CHAT_BURST;
        double invokeTokens = INVOKE_BURST;

        Peer(Link link, long joined, Instance player) {
            this.link = link;
            this.joined = joined;
            this.player = player;
            this.walkBudget = 1;
            this.riseBudget = 1;
            this.fallBudget = 1;
        }
    }

    /// An InvokeClient waiting on its answer, and the one peer allowed to
    /// give it: call ids are a server-wide counter, so any other client
    /// could otherwise answer (or forge) a call meant for someone else.
    private static final class PendingCall {
        final Coroutine co;
        final Peer peer;

        PendingCall(Coroutine co, Peer peer) {
            this.co = co;
            this.peer = peer;
        }
    }

    public ReplicaServer(Engine engine) {
        if (engine.getRole() != Engine.SERVER) {
            throw new IllegalArgumentException("A replica server needs an Engine.SERVER engine");
        }
        this.engine = engine;
        engine.setTransport(this);
        engine.addListener(this);
        for (String s : Wire.SERVICES) {
            for (Instance c : engine.service(s).getChildren()) {
                assign(c);
            }
        }
    }

    public Engine getEngine() {
        return engine;
    }

    /// Moves the server refused as implausible, for the anti-cheat panel.
    public int getRejectedMoves() {
        return rejectedMoves;
    }

    /// Players connected now.
    public int getPlayerCount() {
        // A disconnect nulls its slot rather than shifting the handles, so
        // the list's size counted everyone who ever joined.
        int n = 0;
        for (Peer p : peers) {
            if (p != null) {
                n++;
            }
        }
        return n;
    }

    /// Admits a player: sends the snapshot over `link`, then adds the player,
    /// which every client learns of at the next flush. Answers the handle
    /// [#receive] and [#disconnect] take.
    public int connect(Link link, long userId, String name, String displayName) {
        return connect(link, userId, name, displayName, null);
    }

    /// [#connect(Link, long, String, String)] with the player's platform
    /// attributes, as [Engine#addPlayer(long, String, String, java.util.Map)]
    /// takes them; `attributes` may be null.
    public int connect(Link link, long userId, String name, String displayName, Map<String, Object> attributes) {
        long joined = seq;
        Wire.Writer w = new Wire.Writer();
        Set<Integer> sent = new HashSet<Integer>();
        try {
            for (String s : Wire.SERVICES) {
                for (Instance c : engine.service(s).getChildren()) {
                    if (ids.containsKey(c)) {
                        writeCreate(w, null, c, sent);
                    }
                }
            }
            // A service's own properties -- Lighting's ClockTime, Brightness
            // and Ambient -- belong to no replicated child, so without these
            // every client drew the place at the default noon.
            for (String s : Wire.SERVICES) {
                writeServiceProps(w, engine.service(s));
            }
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible.getMessage(), impossible);
        }
        link.send(w.done());
        Peer p = new Peer(link, joined, engine.addPlayer(userId, name, displayName, attributes));
        p.lastMoveFrame = engine.getFrames();
        peers.add(p);
        return peers.size() - 1;
    }

    /// Removes a player, as leaving does.
    public void disconnect(int handle) {
        Peer p = peer(handle);
        if (p == null) {
            return;
        }
        peers.set(handle, null);
        // Calls waiting on this player would stay parked forever. Roblox
        // raises an error in the caller when the player leaves; the
        // interpreter cannot resume a thread with an error from outside, so
        // the caller gets nil back and the leave is reported.
        List<Coroutine> orphaned = new ArrayList<Coroutine>();
        for (java.util.Iterator<PendingCall> it = calls.values().iterator(); it.hasNext();) {
            PendingCall c = it.next();
            if (c.peer == p) { // NOPMD CompareObjectsWithEquals - peer identity
                orphaned.add(c.co);
                it.remove();
            }
        }
        for (Coroutine co : orphaned) {
            engine.getScheduler().error(co.getOwner(), "InvokeClient: player " + p.player.getName() + " left");
            engine.getScheduler().resume(co, new Object[0]);
        }
        Instance ch = p.player.ref("Character");
        if (ch != null) {
            ch.destroy();
        }
        p.player.destroy();
    }

    /// The engine's Player for client `handle`, or null once it has left.
    public Instance player(int handle) {
        Peer p = peer(handle);
        return p == null ? null : p.player;
    }

    private Peer peer(int handle) {
        return handle >= 0 && handle < peers.size() ? peers.get(handle) : null;
    }

    /// Applies one message from client `handle`. A malformed message drops
    /// what is left of it; it cannot reach anything but the remotes.
    public void receive(int handle, byte[] message) {
        Peer p = peer(handle);
        if (p == null) {
            return;
        }
        DataInputStream in = Wire.reader(message); // NOPMD CloseResource - in-memory stream; nothing to close
        long now = engine.getFrames();
        refill(p, now);
        if (p.opsFrame != now) {
            p.opsFrame = now;
            p.opsThisFrame = 0;
        }
        int opsInMessage = 0;
        try {
            while (in.available() > 0) {
                // Excess ops are ignored rather than queued: a flood costs the
                // server a counter, never a remote handler or a relay per op.
                if (++opsInMessage > MAX_OPS_PER_MESSAGE || ++p.opsThisFrame > MAX_OPS_PER_FRAME) { // NOPMD AssignmentInOperand - op counters
                    return;
                }
                int op = in.readByte();
                switch (op) {
                    case Wire.EVENT: {
                        Instance remote = Wire.ref(in, engine, this);
                        Object[] args = Wire.values(in, engine, this);
                        if (remote != null && remote.isA("RemoteEvent")) {
                            Network.deliverToServer(remote, p.player, args);
                        }
                        break;
                    }
                    case Wire.INVOKE: {
                        int call = in.readInt();
                        Instance remote = Wire.ref(in, engine, this);
                        Object[] args = Wire.values(in, engine, this);
                        Object[] r = new Object[0];
                        // Past the rate the call is answered empty, so the
                        // client's coroutine resumes instead of waiting forever.
                        boolean allowed = p.invokeTokens >= 1;
                        if (allowed) {
                            p.invokeTokens--;
                        }
                        if (allowed && remote != null && remote.isA("RemoteFunction")) {
                            r = Network.answerServer(remote, p.player, args);
                        }
                        queue(Wire.RESULT, null, Integer.valueOf(call), p.player, r);
                        break;
                    }
                    case Wire.RESULT: {
                        int call = in.readInt();
                        Object[] r = Wire.values(in, engine, this);
                        Integer key = Integer.valueOf(call);
                        PendingCall pc = calls.get(key);
                        if (pc != null && pc.peer == p) { // NOPMD CompareObjectsWithEquals - peer identity
                            calls.remove(key);
                            engine.getScheduler().resume(pc.co, r);
                        }
                        break;
                    }
                    case Wire.MOVE: {
                        Object cf = Wire.value(in, engine, this);
                        Object dir = Wire.value(in, engine, this);
                        boolean jump = in.readBoolean();
                        if (cf instanceof CFrame) {
                            move(p, (CFrame) cf, dir instanceof Vector3 ? (Vector3) dir : null, jump);
                        }
                        break;
                    }
                    case Wire.CHAT: {
                        String text = in.readUTF();
                        String channel = in.readUTF();
                        if (!validChannel(channel) || p.chatTokens < 1) {
                            break;
                        }
                        p.chatTokens--;
                        if (text.length() > MAX_CHAT_LENGTH) {
                            text = text.substring(0, MAX_CHAT_LENGTH);
                        }
                        // The server filters what it relays (a client's own
                        // filter is not trusted) and its scripts hear it.
                        String clean = engine.getHost().filter(text, (long) p.player.num("UserId"));
                        engine.chat(p.player, clean, channel);
                        p.player.fireEvent("Chatted", clean);
                        break;
                    }
                    default:
                        return;
                }
            }
        } catch (IOException bad) {
            // Only reading is guarded: a cast that failed would not throw on
            // iOS, so every value is tested with instanceof instead. A script
            // error in a remote handler is the scheduler's to report.
            engine.getScheduler().error(null, "Dropped a malformed message from " + p.player.getName()
                    + ": " + bad.getMessage());
        }
    }

    /// Tops up `p`'s chat and invoke allowances for the frames since the last
    /// message, up to each burst.
    private static void refill(Peer p, long now) {
        if (now != p.rateFrame) {
            double dt = Math.max(0, now - p.rateFrame) * FRAME;
            p.rateFrame = now;
            p.chatTokens = Math.min(CHAT_BURST, p.chatTokens + dt * CHAT_PER_SECOND);
            p.invokeTokens = Math.min(INVOKE_BURST, p.invokeTokens + dt * INVOKE_PER_SECOND);
        }
    }

    /// A channel is a TextChannel's name: short, letters, digits and `_`.
    /// Anything else is not a channel any client could have, so the line is
    /// dropped rather than relayed to every listener under a forged name.
    static boolean validChannel(String channel) {
        if (channel.isEmpty() || channel.length() > MAX_CHANNEL_LENGTH) {
            return false;
        }
        for (int i = 0; i < channel.length(); i++) {
            char c = channel.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private void move(Peer p, CFrame cf, Vector3 dir, boolean jump) {
        Instance ch = p.player.ref("Character");
        Instance root = ch == null ? null : ch.findFirstChild("HumanoidRootPart");
        Instance hum = ch == null ? null : ch.findFirstChildOfClass("Humanoid", false);
        if (root == null || hum == null || cf == null) {
            return;
        }
        double speed = hum.num("WalkSpeed") * 1.5 + 2;
        double gravity = engine.getWorkspace().num("Gravity");
        double rise = Math.max(hum.num("JumpPower"), 50) + gravity;
        // Nothing stops a fall but the ground, so the bound is a generous
        // terminal velocity: three seconds of free fall.
        double fall = Math.max(gravity, 0) * 3 + 50;
        long now = engine.getFrames();
        if (now != p.lastMoveFrame) {
            double dt = Math.min(1, Math.max(1, now - p.lastMoveFrame) * FRAME);
            p.lastMoveFrame = now;
            // Refill by the time that passed, capped at one second's worth
            // so idling does not bank a teleport.
            p.walkBudget = Math.min(p.walkBudget + speed * dt + 1, speed + 1);
            p.riseBudget = Math.min(p.riseBudget + rise * dt + 1, rise + 1);
            p.fallBudget = Math.min(p.fallBudget + fall * dt + 1, fall + 1);
        }
        CFrame at = (CFrame) root.prop("CFrame");
        double dx = cf.x - at.x;
        double dy = cf.y - at.y;
        double dz = cf.z - at.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // NaN and Infinity fail every comparison, so they are tested first,
        // and the rotation has to be one: a skewed or scaled matrix would
        // replicate to every client.
        if (!validFrame(cf) || horizontal > p.walkBudget || dy > p.riseBudget || -dy > p.fallBudget) {
            rejectedMoves++;
            p.correct = true;
            touch(root, "CFrame");
            return;
        }
        p.walkBudget -= horizontal;
        if (dy > 0) {
            p.riseBudget -= dy;
        } else {
            p.fallBudget += dy;
        }
        boolean dirOk = dir != null && isFinite(dir.x) && isFinite(dir.y) && isFinite(dir.z);
        root.put("CFrame", cf);
        if (dirOk) {
            hum.put("MoveDirection", dir);
        }
        if (jump) {
            hum.put("Jump", Boolean.TRUE);
        }
    }

    private static boolean isFinite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }

    /// True when every component is finite and the rotation is orthonormal
    /// with determinant +1, within what float drift on a client produces.
    static boolean validFrame(CFrame c) {
        double[] m = {c.x, c.y, c.z, c.r00, c.r01, c.r02, c.r10, c.r11, c.r12, c.r20, c.r21, c.r22};
        for (double d : m) {
            if (!isFinite(d)) {
                return false;
            }
        }
        double eps = 1e-3;
        double n0 = c.r00 * c.r00 + c.r01 * c.r01 + c.r02 * c.r02;
        double n1 = c.r10 * c.r10 + c.r11 * c.r11 + c.r12 * c.r12;
        double n2 = c.r20 * c.r20 + c.r21 * c.r21 + c.r22 * c.r22;
        double d01 = c.r00 * c.r10 + c.r01 * c.r11 + c.r02 * c.r12;
        double d02 = c.r00 * c.r20 + c.r01 * c.r21 + c.r02 * c.r22;
        double d12 = c.r10 * c.r20 + c.r11 * c.r21 + c.r12 * c.r22;
        double det = c.r00 * (c.r11 * c.r22 - c.r12 * c.r21) - c.r01 * (c.r10 * c.r22 - c.r12 * c.r20)
                + c.r02 * (c.r10 * c.r21 - c.r11 * c.r20);
        return Math.abs(n0 - 1) < eps && Math.abs(n1 - 1) < eps && Math.abs(n2 - 1) < eps
                && Math.abs(d01) < eps && Math.abs(d02) < eps && Math.abs(d12) < eps && Math.abs(det - 1) < eps;
    }

    /// Sends every op queued since the last flush. Call it after each
    /// [Engine#step].
    public void flush() {
        if (ops.isEmpty()) {
            return;
        }
        List<Object[]> batch = new ArrayList<Object[]>(ops);
        ops.clear();
        dirty.clear();
        for (int k = 0; k < peers.size(); k++) { // NOPMD ForLoopCanBeForeach - a disconnect nulls a slot while walked
            Peer p = peers.get(k);
            if (p == null) {
                continue;
            }
            Wire.Writer w = new Wire.Writer();
            Set<Integer> sent = new HashSet<Integer>();
            try {
                for (Object[] op : batch) {
                    if (((Long) op[5]).longValue() >= p.joined) {
                        write(w, p, op, sent);
                    }
                }
            } catch (IOException impossible) {
                throw new IllegalStateException(impossible.getMessage(), impossible);
            }
            p.correct = false;
            if (w.ops > 0) {
                p.link.send(w.done());
            }
        }
    }

    private void write(Wire.Writer w, Peer p, Object[] op, Set<Integer> sent) throws IOException {
        int code = ((Integer) op[0]).intValue();
        Instance i = (Instance) op[1];
        switch (code) {
            case Wire.CREATE:
                if (!i.isDestroyed() && ids.containsKey(i) && !sent.contains(ids.get(i))) {
                    writeCreate(w, p, i, sent);
                }
                break;
            case Wire.PROP: {
                String key = (String) op[2];
                Integer id = ids.get(i);
                int service = Wire.serviceRef(engine, i);
                if (id == null && service != 0) {
                    DataOutputStream out = w.op(Wire.PROP); // NOPMD CloseResource - in-memory stream; nothing to close
                    out.writeInt(service);
                    out.writeUTF(key);
                    Wire.value(out, engine, this, i.prop(key));
                    break;
                }
                if (id == null || sent.contains(id) || i.isDestroyed() || ownMotion(p, i, key)) {
                    return;
                }
                DataOutputStream out = w.op(Wire.PROP); // NOPMD CloseResource - in-memory stream; nothing to close
                out.writeInt(id.intValue());
                out.writeUTF(key);
                Wire.value(out, engine, this, "Name".equals(key) ? i.getName() : i.prop(key));
                break;
            }
            case Wire.ATTR: {
                Integer id = ids.get(i);
                if (id == null || sent.contains(id)) {
                    return;
                }
                String key = (String) op[2];
                DataOutputStream out = w.op(Wire.ATTR); // NOPMD CloseResource - in-memory stream; nothing to close
                out.writeInt(id.intValue());
                out.writeUTF(key);
                Wire.value(out, engine, this, i.getAttribute(key));
                break;
            }
            case Wire.PARENT: {
                Integer id = ids.get(i);
                if (id == null || sent.contains(id) || i.isDestroyed()) {
                    return;
                }
                DataOutputStream out = w.op(Wire.PARENT); // NOPMD CloseResource - in-memory stream; nothing to close
                out.writeInt(id.intValue());
                Wire.ref(out, engine, this, i.getParent());
                break;
            }
            case Wire.DESTROY:
                w.op(Wire.DESTROY).writeInt(((Integer) op[2]).intValue());
                break;
            case Wire.EVENT: {
                Instance target = (Instance) op[3];
                if (target != null && target != p.player) { // NOPMD CompareObjectsWithEquals - player identity
                    return;
                }
                DataOutputStream out = w.op(Wire.EVENT); // NOPMD CloseResource - in-memory stream; nothing to close
                Wire.ref(out, engine, this, i);
                Wire.values(out, engine, this, (Object[]) op[4]);
                break;
            }
            case Wire.INVOKE:
            case Wire.RESULT: {
                if (op[3] != p.player) { // NOPMD CompareObjectsWithEquals - player identity
                    return;
                }
                DataOutputStream out = w.op(code); // NOPMD CloseResource - in-memory stream; nothing to close
                out.writeInt(((Integer) op[2]).intValue());
                if (code == Wire.INVOKE) {
                    Wire.ref(out, engine, this, i);
                }
                Wire.values(out, engine, this, (Object[]) op[4]);
                break;
            }
            case Wire.CHAT: {
                DataOutputStream out = w.op(Wire.CHAT); // NOPMD CloseResource - in-memory stream; nothing to close
                Wire.ref(out, engine, this, i);
                out.writeUTF((String) op[2]);
                out.writeUTF((String) op[3]);
                break;
            }
            case Wire.TAG: {
                Integer id = ids.get(i);
                if (id != null && !sent.contains(id)) {
                    DataOutputStream out = w.op(Wire.TAG); // NOPMD CloseResource - in-memory stream; nothing to close
                    out.writeInt(id.intValue());
                    out.writeUTF((String) op[2]);
                }
                break;
            }
            default:
                break;
        }
    }

    /// The owner of a character is not told where its own character is: it
    /// knows better. A rejected move is the exception.
    private boolean ownMotion(Peer p, Instance i, String key) {
        if (p.player == null || p.correct) {
            return false;
        }
        Instance ch = p.player.ref("Character");
        if (ch == null || !(i == ch || i.isDescendantOf(ch))) { // NOPMD CompareObjectsWithEquals - instance identity
            return false;
        }
        for (String m : MOTION) {
            if (m.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private void writeCreate(Wire.Writer w, Peer p, Instance i, Set<Integer> sent) throws IOException {
        if (p != null && i == p.player) { // NOPMD CompareObjectsWithEquals - the joining player is created on its own client
            writeLocal(w, i);
            return;
        }
        DataOutputStream out = w.op(Wire.CREATE); // NOPMD CloseResource - in-memory stream; nothing to close
        Wire.ref(out, engine, this, i.getParent());
        writeTree(out, i, sent);
    }

    private void writeLocal(Wire.Writer w, Instance player) throws IOException {
        DataOutputStream out = w.op(Wire.LOCAL); // NOPMD CloseResource - in-memory stream; nothing to close
        out.writeInt(id(player));
        out.writeLong((long) player.num("UserId"));
        out.writeUTF(player.getName());
        out.writeUTF(player.str("DisplayName"));
        String[] kids = {"Backpack", "PlayerGui", "StarterGear"};
        for (String k : kids) {
            Instance c = player.findFirstChild(k);
            out.writeInt(c == null ? 0 : id(c));
        }
        // Not marked sent: LOCAL carries no properties, so the ones queued
        // with it (Character, Team) must still follow.
    }

    /// Whether a service property is sent: a plain value. An instance ref
    /// (Workspace.CurrentCamera) names something of the server's own that
    /// the client has its own of, and sending it would clear the client's.
    private static boolean serviceProp(String k, Object v) {
        return !"Parent".equals(k) && !"Name".equals(k) && !k.startsWith("@") && !(v instanceof Instance)
                && Wire.encodable(v);
    }

    private void writeServiceProps(Wire.Writer w, Instance service) throws IOException {
        int ref = Wire.serviceRef(engine, service);
        for (Map.Entry<String, Object> e : service.storedProps().entrySet()) {
            String k = e.getKey();
            if (!serviceProp(k, e.getValue())) {
                continue;
            }
            DataOutputStream out = w.op(Wire.PROP); // NOPMD CloseResource - in-memory stream; nothing to close
            out.writeInt(ref);
            out.writeUTF(k);
            Wire.value(out, engine, this, e.getValue());
        }
    }

    private void writeTree(DataOutputStream out, Instance i, Set<Integer> sent) throws IOException {
        Integer id = ids.get(i);
        sent.add(id);
        out.writeInt(id.intValue());
        out.writeUTF(i.getClassName());
        out.writeUTF(i.getName());
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, Object> e : i.storedProps().entrySet()) {
            String k = e.getKey();
            if ("Parent".equals(k) || "Name".equals(k) || !Wire.encodable(e.getValue())
                    || "Source".equals(k) && "Script".equals(i.getClassName())) {
                continue;
            }
            keys.add(k);
        }
        out.writeInt(keys.size());
        for (String k : keys) {
            out.writeUTF(k);
            Wire.value(out, engine, this, i.prop(k));
        }
        Map<String, Object> attrs = i.attributes();
        out.writeInt(attrs.size());
        for (Map.Entry<String, Object> a : attrs.entrySet()) {
            out.writeUTF(a.getKey());
            Wire.value(out, engine, this, a.getValue());
        }
        List<String> tags = i.tags();
        out.writeInt(tags.size());
        for (String t : tags) {
            out.writeUTF(t);
        }
        List<Instance> kids = new ArrayList<Instance>();
        for (Instance c : i.getChildren()) {
            if (ids.containsKey(c) && !sent.contains(ids.get(c))) {
                kids.add(c);
            }
        }
        out.writeInt(kids.size());
        for (Instance c : kids) {
            writeTree(out, c, sent);
        }
    }

    // ---- ids ----

    @Override
    public int id(Instance i) {
        Integer id = ids.get(i);
        return id == null ? 0 : id.intValue();
    }

    @Override
    public Instance instance(int id) {
        return byId.get(Integer.valueOf(id));
    }

    private void assign(Instance i) {
        if (!ids.containsKey(i)) {
            Integer id = Integer.valueOf(nextId++);
            ids.put(i, id);
            byId.put(id, i);
        }
        for (Instance c : i.getChildren()) {
            assign(c);
        }
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

    private boolean replicated(Instance i) {
        if (i.isDestroyed()) {
            return false;
        }
        Instance top = i;
        while (top.getParent() != null && top.getParent() != engine.getGame()) { // NOPMD CompareObjectsWithEquals - the data model root
            top = top.getParent();
        }
        return top != i && top.getParent() != null && Wire.serviceRef(engine, top) != 0; // NOPMD CompareObjectsWithEquals - instance identity
    }

    private void queue(int code, Instance i, Object a, Object b, Object c) {
        ops.add(new Object[] {Integer.valueOf(code), i, a, b, c, Long.valueOf(seq++)});
    }

    private void touch(Instance i, String key) {
        if (dirty.add(id(i) + ":" + key)) {
            queue(Wire.PROP, i, key, null, null);
        }
    }

    // ---- EngineListener ----

    @Override
    public void propertyChanged(Instance i, String prop) {
        if (!ids.containsKey(i) && serviceProp(prop, i.prop(prop)) && Wire.serviceRef(engine, i) != 0) {
            // a service's own property: a day/night script turning ClockTime
            queue(Wire.PROP, i, prop, null, null);
            return;
        }
        if (!ids.containsKey(i) || "Parent".equals(prop)) {
            return;
        }
        if (prop.startsWith("@")) {
            queue(Wire.ATTR, i, prop.substring(1), null, null);
        } else if (!"Source".equals(prop) || !"Script".equals(i.getClassName())) {
            touch(i, prop);
        }
    }

    @Override
    public void ancestryChanged(Instance i) {
        boolean rep = replicated(i);
        Integer id = ids.get(i);
        if (rep && id == null) {
            assign(i);
            queue(Wire.CREATE, i, null, null, null);
        } else if (rep) {
            queue(Wire.PARENT, i, null, null, null);
        } else if (id != null) {
            queue(Wire.DESTROY, i, id, null, null);
            forget(i);
        }
    }

    @Override
    public void destroyed(Instance i) {
        Integer id = ids.get(i);
        if (id != null) {
            queue(Wire.DESTROY, i, id, null, null);
            forget(i);
        }
    }

    @Override
    public void chat(Instance from, String text, String channel) {
        queue(Wire.CHAT, from, text, channel, null);
    }

    // ---- Network.Transport ----

    @Override
    public void fire(Engine from, Instance remote, Instance player, Object[] args) {
        queue(Wire.EVENT, remote, null, player, args);
    }

    @Override
    public Object[] invoke(Engine from, Instance remote, Instance player, Object[] args) {
        Coroutine co = engine.getInterp().currentThread();
        if (co == null || player == null) {
            throw LuaError.rt("InvokeClient must be called from a script thread, with a player");
        }
        Peer target = null;
        for (Peer p : peers) {
            if (p != null && p.player == player) { // NOPMD CompareObjectsWithEquals - instance identity
                target = p;
                break;
            }
        }
        if (target == null) {
            throw LuaError.rt("InvokeClient: player is not connected");
        }
        Integer call = Integer.valueOf(nextCall++);
        calls.put(call, new PendingCall(co, target));
        queue(Wire.INVOKE, remote, call, player, args);
        return engine.getInterp().yield(new Object[0]);
    }
}
