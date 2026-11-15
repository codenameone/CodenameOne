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

import com.codename1.luau.Interp;
import com.codename1.luau.LuaClosure;
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// One running experience: an interpreter, the scheduler, the data model
/// and the hooks a host (renderer, physics, network, backend) plugs into.
///
/// An engine runs as a server, a client or solo -- both at once, which is
/// how the studio's Play and offline play work. Scripts start by where they
/// sit in the tree: a `Script` under Workspace or ServerScriptService on the
/// server, a `LocalScript` under a player's scripts, GUI, backpack or
/// character on the client, exactly as an exported place expects.
public final class Engine {
    public static final int SOLO = 0;
    public static final int SERVER = 1;
    public static final int CLIENT = 2;

    /// Host services the engine calls out to. Every method has a sensible
    /// offline default, so a bare engine runs a place on its own.
    public interface Host {
        /// Persistent key/value storage behind DataStoreService. Values are
        /// JSON strings; null means absent.
        String dataGet(String store, String scope, String key);

        void dataSet(String store, String scope, String key, String json);

        /// Every key of a store with its JSON value, for ListKeysAsync and
        /// OrderedDataStore:GetSortedAsync.
        List<String[]> dataList(String store, String scope);

        /// A message to every server of this experience (MessagingService).
        /// The offline host delivers it straight back.
        void publish(String topic, String json);

        /// A purchase prompt. The host shows its own UI and later calls
        /// [Engine#purchaseFinished].
        void promptPurchase(Instance player, String kind, long id);

        /// Whether `userId` owns game pass `passId`.
        boolean ownsPass(long userId, long passId);

        /// A badge award; answers whether it was newly awarded.
        boolean awardBadge(long userId, long badgeId);

        boolean hasBadge(long userId, long badgeId);

        /// Analytics from AnalyticsService and the platform's own events.
        void analytics(String event, Map<String, Object> fields);

        /// Text filtering for chat and user text.
        String filter(String text, long fromUserId);

        /// A teleport to another experience on this platform.
        void teleport(Instance player, long placeId, Object data);

        /// A leaderboard write from leaderstats or BrickyardService.
        void leaderstat(long userId, String stat, double value);

        /// Top entries of a platform leaderboard: {userId, name, value} rows.
        /// `period` is "daily", "weekly" or "alltime".
        List<Object[]> leaderboard(String stat, String period, int count);

        /// The player's platform level.
        int level(long userId);

        /// Shows the platform's invite sheet; `message` may be null.
        void invite(Instance player, String message);

        /// Whether chat is allowed for the user (age and parental settings).
        boolean canChat(long userId);

        /// Whether the user is under the platform's age threshold.
        boolean isMinor(long userId);
    }

    final Interp in;
    final Scheduler sched;
    final Instance game;
    private final int role;
    private int ids;
    private Host host = new OfflineHost();
    private final Map<String, Instance> services = new LinkedHashMap<String, Instance>();
    private final Map<String, Set<Instance>> tagged = new HashMap<String, Set<Instance>>();
    private final Set<String> unsupported = new LinkedHashSet<String>();
    private final List<Object[]> timers = new ArrayList<Object[]>();
    private final Set<Instance> running = new LinkedHashSet<Instance>();
    private final List<EngineListener> listeners = new ArrayList<EngineListener>();
    private Instance localPlayer;
    /// `function(cb, done, ...) done(pcall(cb, ...)) end`, from the prelude.
    com.codename1.luau.LuaFunction invoker;
    private PhysicsWorld physics;
    private double frameTime;
    private long frames;

    /// Observes the tree: renderers, replication, the studio.
    public interface EngineListener {
        void propertyChanged(Instance i, String prop);

        void ancestryChanged(Instance i);

        void destroyed(Instance i);

        /// A chat line was sent; `from` is null for a system message.
        void chat(Instance from, String text, String channel);
    }

    /// The simulation the engine steps every frame (set by the 3D layer).
    public interface PhysicsWorld {
        void step(Engine e, double dt);

        /// The first part hit by the ray, as {part, position, normal,
        /// material}; null when nothing is hit. `filter` decides which parts
        /// are candidates.
        Object[] raycast(Engine e, Vector3 origin, Vector3 dir, RaycastFilter filter);

        /// The parts whose bounds touch the box.
        List<Instance> partsInBox(Engine e, CFrame cf, Vector3 size, RaycastFilter filter);
    }

    /// Which parts a query considers.
    public interface RaycastFilter {
        boolean accepts(Instance part);
    }

    public Engine(int role) {
        this.role = role;
        in = new Interp();
        sched = new Scheduler(in);
        game = new Instance(this, ClassDb.get("DataModel"));
        game.setName("Game");
        for (String s : new String[] {"Workspace", "Players", "Lighting", "ReplicatedStorage", "ReplicatedFirst",
                "ServerScriptService", "ServerStorage", "StarterGui", "StarterPack", "StarterPlayer", "SoundService",
                "Teams", "Chat", "TextChatService", "RunService", "UserInputService", "TweenService", "Debris",
                "CollectionService", "HttpService", "DataStoreService", "MarketplaceService", "BadgeService",
                "MessagingService", "TeleportService", "PathfindingService", "PhysicsService", "ContextActionService",
                "GuiService", "TextService", "SocialService", "AnalyticsService", "PolicyService",
                "LocalizationService", "GroupService", "InsertService", "AssetService", "ProximityPromptService",
                "MemoryStoreService", "MaterialService", "BrickyardService"}) {
            service(s);
        }
        Instance sp = services.get("StarterPlayer");
        Instance sps = create("StarterPlayerScripts");
        sps.setName("StarterPlayerScripts");
        sps.setParent(sp);
        Instance scs = create("StarterCharacterScripts");
        scs.setName("StarterCharacterScripts");
        scs.setParent(sp);
        Instance cam = create("Camera");
        cam.setName("Camera");
        cam.setParent(services.get("Workspace"));
        services.get("Workspace").putQuiet("CurrentCamera", cam);
        Globals.install(this);
        if (host instanceof OfflineHost) {
            ((OfflineHost) host).setOwner(this);
        }
    }

    public int getRole() {
        return role;
    }

    public boolean isServer() {
        return role != CLIENT;
    }

    public boolean isClient() {
        return role != SERVER;
    }

    public Interp getInterp() {
        return in;
    }

    public Scheduler getScheduler() {
        return sched;
    }

    public Instance getGame() {
        return game;
    }

    public Instance getWorkspace() {
        return services.get("Workspace");
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host h) {
        host = h;
    }

    /// Receives visual and audio effects the scene asks for (a sound played,
    /// particles emitted, an explosion) for a renderer to show.
    public interface EffectSink {
        /// `kind` names the effect, `arg` is its parameter or null.
        void effect(Instance source, String kind, Object arg);
    }

    private EffectSink effects;
    private Network.Transport transport;
    private final Map<String, Object> extras = new HashMap<String, Object>();

    /// Per-engine state of the behaviour classes, by key.
    public Object extra(String key) {
        return extras.get(key);
    }

    public void extra(String key, Object v) {
        extras.put(key, v);
    }

    private boolean studio;
    private String platform = "None";

    /// True while the studio plays the place (RunService:IsStudio).
    public boolean isStudio() {
        return studio;
    }

    public void setStudio(boolean s) {
        studio = s;
    }

    /// The `Enum.Platform` item name the device reports.
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String p) {
        platform = p;
    }

    /// A chat line, for the host's chat window and moderation log.
    void chat(Instance from, String text, String channel) {
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            listeners.get(i).chat(from, text, channel);
        }
    }

    public Network.Transport getTransport() {
        return transport;
    }

    public void setTransport(Network.Transport t) {
        transport = t;
    }

    public void setEffectSink(EffectSink s) {
        effects = s;
    }

    void effect(Instance source, String kind, Object arg) {
        if (effects != null) {
            effects.effect(source, kind, arg);
        }
    }

    public PhysicsWorld getPhysics() {
        return physics;
    }

    public void setPhysics(PhysicsWorld p) {
        physics = p;
    }

    public void addListener(EngineListener l) {
        listeners.add(l);
    }

    public void removeListener(EngineListener l) {
        listeners.remove(l);
    }

    public Instance getLocalPlayer() {
        return localPlayer;
    }

    public long getFrames() {
        return frames;
    }

    /// Wall time spent in the last [#step], in milliseconds.
    public double getFrameTime() {
        return frameTime;
    }

    /// Members scripts asked for that this engine does not implement,
    /// "Class:Member". The importer and the studio list these.
    public Set<String> getUnsupported() {
        return unsupported;
    }

    void unsupported(String cls, String member) {
        unsupported.add(cls + ":" + member);
    }

    int nextId() {
        return ++ids;
    }

    /// `game:GetService(name)`, creating a service on first request.
    public Instance service(String name) {
        Instance s = services.get(name);
        if (s != null) {
            return s;
        }
        ClassDb.Desc d = ClassDb.get(name);
        if (d == null || !d.isService()) {
            throw LuaError.rt("'" + name + "' is not a valid Service name");
        }
        s = new Instance(this, d);
        s.setName(name);
        services.put(name, s);
        s.setParent(game);
        return s;
    }

    public Instance findService(String name) {
        return services.get(name);
    }

    /// `Instance.new(className)`.
    public Instance create(String cls) {
        ClassDb.Desc d = ClassDb.get(cls);
        if (d == null) {
            throw LuaError.rt("Unable to create an Instance of type \"" + cls + "\"");
        }
        Instance i = new Instance(this, d);
        for (ClassDb.Desc c = d; c != null; c = c.parent) {
            if (c.behavior != null) {
                c.behavior.created(i);
            }
        }
        return i;
    }

    /// Creates a creatable class only, as scripts are allowed to.
    public Instance scriptCreate(String cls) {
        ClassDb.Desc d = ClassDb.get(cls);
        if (d == null || !d.isCreatable()) {
            throw LuaError.rt("Unable to create an Instance of type \"" + cls + "\"");
        }
        return create(cls);
    }

    // ------------------------------------------------------------ tree hooks

    void propertyChanged(Instance i, String prop) {
        for (int k = 0; k < listeners.size(); k++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            listeners.get(k).propertyChanged(i, prop);
        }
        if (("Disabled".equals(prop) || "Enabled".equals(prop)) && i.isA("BaseScript")) {
            scriptStateChanged(i);
        }
        if ("Value".equals(prop) && i.getParent() != null && "leaderstats".equals(i.getParent().getName())) {
            Instance player = i.getParent().getParent();
            if (player != null && player.isA("Player")) {
                Object v = i.prop("Value");
                if (v instanceof Double) {
                    host.leaderstat((long) player.num("UserId"), i.getName(), ((Double) v).doubleValue());
                }
            }
        }
    }

    void ancestryChanged(Instance i) {
        for (int k = 0; k < listeners.size(); k++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            listeners.get(k).ancestryChanged(i);
        }
        if (i.isA("BaseScript")) {
            scriptStateChanged(i);
        }
    }

    void destroyed(Instance i) {
        for (int k = 0; k < listeners.size(); k++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            listeners.get(k).destroyed(i);
        }
        if (i.tags().size() > 0) {
            for (String t : new ArrayList<String>(i.tags())) {
                removeTag(i, t);
            }
        }
    }

    void attributeChanged(Instance i, String n) {
        for (int k = 0; k < listeners.size(); k++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            listeners.get(k).propertyChanged(i, "@" + n);
        }
    }

    public void addTag(Instance i, String t) {
        if (i.hasTag(t)) {
            return;
        }
        i.tags().add(t);
        Set<Instance> s = tagged.get(t);
        if (s == null) {
            s = new LinkedHashSet<Instance>();
            tagged.put(t, s);
        }
        s.add(i);
        Instance cs = services.get("CollectionService");
        cs.fireEvent("TagAdded", t);
        cs.fireEvent("+" + t, i);
    }

    void removeTag(Instance i, String t) {
        if (!i.hasTag(t)) {
            return;
        }
        i.tags().remove(t);
        Set<Instance> s = tagged.get(t);
        if (s != null) {
            s.remove(i);
        }
        Instance cs = services.get("CollectionService");
        cs.fireEvent("TagRemoved", t);
        cs.fireEvent("-" + t, i);
    }

    public List<Instance> tagged(String t) {
        Set<Instance> s = tagged.get(t);
        return s == null ? new ArrayList<Instance>() : new ArrayList<Instance>(s);
    }

    public Set<String> allTags() {
        return tagged.keySet();
    }

    // ------------------------------------------------------------ scripts

    private boolean shouldRun(Instance s) {
        if (s.isDestroyed() || s.bool("Disabled") || !s.bool("Enabled")) {
            return false;
        }
        if (s.isA("LocalScript")) {
            if (!isClient()) {
                return false;
            }
            Instance ps = s.findFirstAncestorOfClass("PlayerScripts", false);
            Instance pg = s.findFirstAncestorOfClass("PlayerGui", false);
            Instance bp = s.findFirstAncestorOfClass("Backpack", false);
            Instance rf = s.findFirstAncestorOfClass("ReplicatedFirst", false);
            boolean inChar = false;
            if (localPlayer != null && localPlayer.ref("Character") != null) {
                inChar = s.isDescendantOf(localPlayer.ref("Character"));
            }
            return ps != null || pg != null || bp != null || rf != null || inChar;
        }
        if (s.isA("Script")) {
            if (!isServer()) {
                return false;
            }
            return s.isDescendantOf(services.get("Workspace")) || s.isDescendantOf(services.get("ServerScriptService"));
        }
        return false;
    }

    private void scriptStateChanged(Instance s) {
        boolean should = shouldRun(s);
        if (should && !running.contains(s)) {
            running.add(s);
            final Instance script = s;
            // Scripts start on the next timer pass, as a place expects:
            // every sibling has been parented by then.
            later(0, new Runnable() {
                @Override
                public void run() {
                    startScript(script);
                }
            });
        } else if (!should && running.contains(s)) {
            running.remove(s);
            sched.cancelOwnedBy(s);
        }
    }

    /// The environment a script's code sees: the globals plus `script`.
    LuaTable scriptEnv(Instance script) {
        LuaTable env = new LuaTable();
        env.rawset("script", script);
        LuaTable mt = new LuaTable();
        mt.rawset("__index", in.getGlobals());
        env.setMetatable(mt);
        return env;
    }

    void startScript(Instance s) {
        if (!running.contains(s) || s.isDestroyed()) {
            return;
        }
        String src = s.str("Source");
        LuaClosure fn;
        try {
            fn = in.load(com.codename1.luau.Utf8.fromJava(src), s.fullName(), scriptEnv(s));
        } catch (LuaError e) {
            sched.error(s, e.getMessage());
            return;
        }
        sched.spawnOwned(fn, s, new Object[0]);
    }

    /// Compiles a ModuleScript for `require`.
    LuaClosure loadModule(Instance m) {
        return in.load(com.codename1.luau.Utf8.fromJava(m.str("Source")), m.fullName(), scriptEnv(m));
    }

    public int runningScripts() {
        return running.size();
    }

    // ------------------------------------------------------------ players

    /// Adds a player. On a client or solo engine the first one becomes
    /// `Players.LocalPlayer`.
    public Instance addPlayer(long userId, String name, String displayName) {
        return addPlayer(userId, name, displayName, null);
    }

    /// [#addPlayer(long, String, String)] with attributes the player carries
    /// from the platform -- its chosen `AvatarBody` -- set before PlayerAdded
    /// fires and its character loads, so neither scripts nor the rig see the
    /// player without them. `attributes` may be null.
    public Instance addPlayer(long userId, String name, String displayName, Map<String, Object> attributes) {
        Instance players = services.get("Players");
        Instance p = create("Player");
        p.setName(name);
        if (attributes != null) {
            for (Map.Entry<String, Object> a : attributes.entrySet()) {
                p.setAttribute(a.getKey(), a.getValue());
            }
        }
        p.putQuiet("UserId", Args.n(userId));
        p.putQuiet("DisplayName", displayName == null ? name : displayName);
        Instance bp = create("Backpack");
        bp.setName("Backpack");
        bp.setParent(p);
        Instance pg = create("PlayerGui");
        pg.setName("PlayerGui");
        pg.setParent(p);
        Instance sg = create("StarterGear");
        sg.setName("StarterGear");
        sg.setParent(p);
        boolean local = isClient() && localPlayer == null;
        if (local) {
            localPlayer = p;
            players.putQuiet("LocalPlayer", p);
        }
        p.setParent(players);
        if (local) {
            Instance ps = create("PlayerScripts");
            ps.setName("PlayerScripts");
            Instance starter = services.get("StarterPlayer").findFirstChild("StarterPlayerScripts");
            if (starter != null) {
                copyChildren(starter, ps);
            }
            ps.setParent(p);
            copyChildren(services.get("StarterGui"), pg);
        }
        players.fireEvent("PlayerAdded", p);
        if (services.get("Players").bool("CharacterAutoLoads") && isServer()) {
            Characters.load(this, p);
        }
        return p;
    }

    public void removePlayer(Instance p) {
        Instance players = services.get("Players");
        players.fireEvent("PlayerRemoving", p);
        Instance ch = p.ref("Character");
        if (ch != null) {
            ch.destroy();
        }
        p.destroy();
    }

    static void copyChildren(Instance from, Instance to) {
        for (Instance c : new ArrayList<Instance>(from.getChildren())) {
            Instance k = c.cloneTree();
            if (k != null) {
                k.setParent(to);
            }
        }
    }

    // ------------------------------------------------------------ frame

    /// Runs `r` after `secs` of simulated time.
    public void later(double secs, Runnable r) {
        timers.add(new Object[] {Double.valueOf(sched.now() + secs), r});
    }

    /// Advances the experience by one frame of `dt` seconds.
    public void step(double dt) {
        long t0 = System.currentTimeMillis();
        frames++;
        Instance rs = services.get("RunService");
        double now = sched.now();
        rs.fireEvent("PreSimulation", Args.n(dt));
        rs.fireEvent("Stepped", Args.n(now), Args.n(dt));
        if (physics != null) {
            physics.step(this, dt);
        }
        rs.fireEvent("PostSimulation", Args.n(dt));
        rs.fireEvent("Heartbeat", Args.n(dt));
        if (isClient()) {
            Behaviors.renderStep(this, dt);
            rs.fireEvent("PreRender", Args.n(dt));
            rs.fireEvent("RenderStepped", Args.n(dt));
        }
        Behaviors.tweens(this, dt);
        Instance ws = services.get("Workspace");
        ws.putQuiet("DistributedGameTime", Args.n(now + dt));
        sched.step(dt);
        runTimers();
        frameTime = System.currentTimeMillis() - t0;
    }

    private void runTimers() {
        if (timers.isEmpty()) {
            return;
        }
        double now = sched.now();
        List<Object[]> due = new ArrayList<Object[]>();
        for (int i = timers.size() - 1; i >= 0; i--) {
            Object[] t = timers.get(i);
            if (((Double) t[0]).doubleValue() <= now + 1e-9) {
                due.add(0, t);
                timers.remove(i);
            }
        }
        for (Object[] t : due) {
            ((Runnable) t[1]).run();
        }
        sched.runDeferred();
    }

    /// Calls a script callback (OnServerInvoke, OnInvoke, ProcessReceipt)
    /// on a thread of its own and hands its results to the calling thread,
    /// waiting for them if the callback yields. Errors in the callback are
    /// rethrown in the caller.
    public Object[] invoke(final com.codename1.luau.LuaFunction cb, Object[] args) {
        final com.codename1.luau.Coroutine caller = in.currentThread();
        final Object[][] result = new Object[1][];
        final boolean[] waiting = new boolean[1];
        com.codename1.luau.JavaFunction done = new com.codename1.luau.JavaFunction("invokeDone") {
            @Override
            public Object[] call(Interp interp, Object[] a) {
                result[0] = a;
                if (waiting[0]) {
                    Object[] r = unpackResult(a, true);
                    sched.resume(caller, r);
                }
                return null;
            }
        };
        Object[] all = new Object[args.length + 2];
        all[0] = cb;
        all[1] = done;
        System.arraycopy(args, 0, all, 2, args.length);
        sched.spawn(invoker, all);
        if (result[0] != null) {
            return unpackResult(result[0], false);
        }
        waiting[0] = true;
        return in.yield(new Object[0]);
    }

    private Object[] unpackResult(Object[] a, boolean resumed) {
        boolean ok = a.length > 0 && Boolean.TRUE.equals(a[0]);
        if (!ok) {
            Object err = a.length > 1 ? a[1] : null;
            if (resumed) {
                sched.error(null, in.tostring(err));
                return new Object[0];
            }
            throw new LuaError(err);
        }
        Object[] r = new Object[Math.max(0, a.length - 1)];
        System.arraycopy(a, 1, r, 0, r.length);
        return r;
    }

    /// A purchase prompt finished (called by the host's purchase UI).
    public void purchaseFinished(Instance player, String kind, long id, boolean purchased) {
        Behaviors.purchaseFinished(this, player, kind, id, purchased);
    }

    /// Runs a chunk of code in the server context (studio command bar,
    /// agent tools). Returns the printed or returned text.
    public Object[] runCommand(String source) {
        LuaClosure fn = in.load(com.codename1.luau.Utf8.fromJava(source), "command", in.getGlobals());
        return in.call(fn);
    }

    /// The default [Host]: in-memory data, everything owned, nothing sent.
    public static final class OfflineHost implements Host {
        private final Map<String, String> data = new HashMap<String, String>();
        private final Set<String> badges = new LinkedHashSet<String>();

        @Override
        public String dataGet(String store, String scope, String key) {
            return data.get(store + "/" + scope + "/" + key);
        }

        @Override
        public void dataSet(String store, String scope, String key, String json) {
            if (json == null) {
                data.remove(store + "/" + scope + "/" + key);
            } else {
                data.put(store + "/" + scope + "/" + key, json);
            }
        }

        @Override
        public List<String[]> dataList(String store, String scope) {
            String pre = store + "/" + scope + "/";
            List<String[]> out = new ArrayList<String[]>();
            for (Map.Entry<String, String> en : data.entrySet()) {
                if (en.getKey().startsWith(pre)) {
                    out.add(new String[] {en.getKey().substring(pre.length()), en.getValue()});
                }
            }
            return out;
        }

        private Engine owner;

        /// The engine MessagingService loops back into.
        public void setOwner(Engine e) {
            owner = e;
        }

        @Override
        public void publish(String topic, String json) {
            if (owner != null) {
                ServiceBehaviors.deliverMessage(owner, topic, json);
            }
        }

        @Override
        public void promptPurchase(Instance player, String kind, long id) {
            Engine e = player.getEngine();
            e.purchaseFinished(player, kind, id, true);
        }

        @Override
        public boolean ownsPass(long userId, long passId) {
            return false;
        }

        @Override
        public boolean awardBadge(long userId, long badgeId) {
            return badges.add(userId + "/" + badgeId);
        }

        @Override
        public boolean hasBadge(long userId, long badgeId) {
            return badges.contains(userId + "/" + badgeId);
        }

        @Override
        public void analytics(String event, Map<String, Object> fields) {
        }

        @Override
        public String filter(String text, long fromUserId) {
            return text;
        }

        @Override
        public void teleport(Instance player, long placeId, Object data) {
        }

        private final Map<String, Double> board = new HashMap<String, Double>();
        private static final Comparator<Object[]> BY_VALUE_DESC = new Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                return Double.compare(((Double) b[2]).doubleValue(), ((Double) a[2]).doubleValue());
            }
        };

        @Override
        public List<Object[]> leaderboard(String stat, String period, int count) {
            List<Object[]> out = new ArrayList<Object[]>();
            String pre = stat + "/";
            for (Map.Entry<String, Double> en : board.entrySet()) {
                if (en.getKey().startsWith(pre)) {
                    String uid = en.getKey().substring(pre.length());
                    out.add(new Object[] {Long.valueOf(uid), "Player" + uid, en.getValue()});
                }
            }
            Collections.sort(out, BY_VALUE_DESC);
            return out.size() > count ? new ArrayList<Object[]>(out.subList(0, count)) : out;
        }

        @Override
        public int level(long userId) {
            return 1;
        }

        @Override
        public void invite(Instance player, String message) {
            Map<String, Object> f = new HashMap<String, Object>();
            f.put("userId", player.prop("UserId"));
            analytics("invite.prompt", f);
        }

        @Override
        public boolean canChat(long userId) {
            return true;
        }

        @Override
        public boolean isMinor(long userId) {
            return false;
        }

        @Override
        public void leaderstat(long userId, String stat, double value) {
            board.put(stat + "/" + userId, Double.valueOf(value));
        }
    }
}
