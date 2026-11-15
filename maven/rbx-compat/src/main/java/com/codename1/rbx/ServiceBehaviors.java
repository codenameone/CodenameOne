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
import com.codename1.luau.LuaError;
import com.codename1.luau.LuaFunction;
import com.codename1.luau.LuaTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Behaviour of the services: players, run loop, input, tweens, collections,
/// data stores, the marketplace, badges, messaging, teleports, pathfinding,
/// collision groups, text and chat, analytics, invites and the platform's own
/// BrickyardService.
final class ServiceBehaviors {
    private ServiceBehaviors() {
    }

    static void install() {
        ClassDb.attach("Players", PLAYERS);
        ClassDb.attach("Player", PLAYER);
        ClassDb.attach("RunService", RUN);
        ClassDb.attach("UserInputService", UIS);
        ClassDb.attach("ContextActionService", CAS);
        ClassDb.attach("TweenService", TWEEN_SERVICE);
        ClassDb.attach("Debris", DEBRIS);
        ClassDb.attach("CollectionService", COLLECTION);
        ClassDb.attach("HttpService", HTTP);
        ClassDb.attach("DataStoreService", DATA_STORES);
        ClassDb.attach("GlobalDataStore", DATA_STORE);
        ClassDb.attach("DataStorePages", PAGES);
        ClassDb.attach("MarketplaceService", MARKET);
        ClassDb.attach("BadgeService", BADGES);
        ClassDb.attach("MessagingService", MESSAGING);
        ClassDb.attach("TeleportService", TELEPORT);
        ClassDb.attach("PathfindingService", PATHFINDING);
        ClassDb.attach("Path", PATH);
        ClassDb.attach("PhysicsService", PHYSICS);
        ClassDb.attach("TextService", TEXT);
        ClassDb.attach("Chat", CHAT);
        ClassDb.attach("TextChatService", TEXT_CHAT);
        ClassDb.attach("TextChannel", TEXT_CHANNEL);
        ClassDb.attach("AnalyticsService", ANALYTICS);
        ClassDb.attach("SocialService", SOCIAL);
        ClassDb.attach("PolicyService", POLICY);
        ClassDb.attach("LocalizationService", LOCALIZATION);
        ClassDb.attach("StarterGui", STARTER_GUI);
        ClassDb.attach("Teams", TEAMS);
        ClassDb.attach("Team", TEAM);
        ClassDb.attach("GuiService", GUI_SERVICE);
        ClassDb.attach("ReplicatedFirst", REPLICATED_FIRST);
        ClassDb.attach("SoundService", SOUND_SERVICE);
        ClassDb.attach("BrickyardService", BRICKYARD);
        ClassDb.attach("TextFilterResult", FILTER_RESULT);
        ClassDb.attach("Translator", TRANSLATOR);
        ClassDb.attach("FriendPages", PAGES);
    }

    static Object[] ret(Object v) {
        return new Object[] {v};
    }

    static long userId(Instance player) {
        return (long) player.num("UserId");
    }

    static Instance player(Object[] a, int i, String m) {
        Instance p = Args.inst(a, i, m);
        if (!p.isA("Player")) {
            throw Args.bad(a, i, m, "Player");
        }
        return p;
    }

    // ------------------------------------------------------------------ players

    static List<Instance> players(Engine e) {
        List<Instance> out = new ArrayList<Instance>();
        List<Instance> c = e.service("Players").getChildren();
        for (int i = 0; i < c.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (c.get(i).isA("Player")) {
                out.add(c.get(i));
            }
        }
        return out;
    }

    static Instance playerFromCharacter(Engine e, Instance model) {
        List<Instance> ps = players(e);
        for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (ps.get(i).ref("Character") == model) { // NOPMD CompareObjectsWithEquals - reference identity
                return ps.get(i);
            }
        }
        return null;
    }

    static final Behavior PLAYERS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "GetPlayers":
                    return ret(Instance.list(players(e)));
                case "GetPlayerFromCharacter": {
                    Object c = Args.at(a, 1);
                    return ret(c instanceof Instance ? playerFromCharacter(e, (Instance) c) : null);
                }
                case "GetPlayerByUserId": {
                    double id = Args.num(a, 1, m);
                    List<Instance> ps = players(e);
                    for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        if (ps.get(i).num("UserId") == id) {
                            return ret(ps.get(i));
                        }
                    }
                    return ret(null);
                }
                case "GetNameFromUserIdAsync": {
                    double id = Args.num(a, 1, m);
                    List<Instance> ps = players(e);
                    for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        if (ps.get(i).num("UserId") == id) {
                            return ret(ps.get(i).getName());
                        }
                    }
                    return ret("Player" + (long) id);
                }
                case "GetUserIdFromNameAsync": {
                    String n = Args.str(a, 1, m);
                    List<Instance> ps = players(e);
                    for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        if (ps.get(i).getName().equals(n)) {
                            return ret(ps.get(i).prop("UserId"));
                        }
                    }
                    throw LuaError.rt("Players:GetUserIdFromNameAsync() failed: Unknown user");
                }
                case "GetUserThumbnailAsync":
                    return new Object[] {"brickyard://avatar/" + (long) Args.num(a, 1, m), Boolean.TRUE};
                case "GetFriendsAsync": {
                    Instance pages = e.create("FriendPages");
                    pages.host = new ArrayList<LuaTable>();
                    return ret(pages);
                }
                case "GetHumanoidDescriptionFromUserId":
                    return ret(e.create("HumanoidDescription"));
                case "CreateHumanoidModelFromDescription":
                    return ret(Characters.buildRig(e, "Character", Args.inst(a, 1, m)));
                case "BanAsync":
                case "UnbanAsync": {
                    Map<String, Object> f = new LinkedHashMap<String, Object>();
                    f.put("config", Json.encode(Args.at(a, 1)));
                    e.getHost().analytics("admin." + m, f);
                    return null;
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior PLAYER = new Behavior() {
        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("leaderstats".equals(key)) {
                Instance l = self.findFirstChild("leaderstats");
                return l == null ? PASS : l;
            }
            return PASS;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "LoadCharacter":
                    Characters.load(e, self);
                    return null;
                case "Kick": {
                    String msg = Args.optStr(a, 1, "");
                    Map<String, Object> f = new LinkedHashMap<String, Object>();
                    f.put("message", msg);
                    f.put("userId", self.prop("UserId"));
                    e.getHost().analytics("player.kick", f);
                    e.removePlayer(self);
                    return null;
                }
                case "GetMouse":
                    return ret(mouse(e));
                case "IsFriendsWith":
                case "IsInGroup":
                    return ret(Boolean.FALSE);
                case "GetRankInGroup":
                    return ret(Args.n(0));
                case "GetRoleInGroup":
                    return ret("Guest");
                case "GetJoinData": {
                    LuaTable t = new LuaTable();
                    Object d = self.getAttribute("__teleportData");
                    if (d != null) {
                        t.rawset("TeleportData", d);
                    }
                    return ret(t);
                }
                case "HasAppearanceLoaded":
                    return ret(Boolean.valueOf(self.ref("Character") != null));
                case "DistanceFromCharacter": {
                    Instance c = self.ref("Character");
                    Instance root = c == null ? null : c.findFirstChild("HumanoidRootPart");
                    if (root == null) {
                        return ret(Args.n(0));
                    }
                    return ret(Args.n(Behaviors.cf(root).position().sub(Args.vec3(a, 1, m)).magnitude()));
                }
                case "ClearCharacterAppearance":
                    return null;
                case "GetFriendsOnline":
                    return ret(new LuaTable());
                case "RequestStreamAroundAsync":
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    /// The local player's Mouse, created on first use.
    static Instance mouse(Engine e) {
        Object m = e.extra("mouse");
        if (m == null) {
            Instance mi = e.create("Mouse");
            mi.setName("Mouse");
            e.extra("mouse", mi);
            m = mi;
        }
        return (Instance) m;
    }

    /// Updates the Mouse from a pointer event; `world` is true when no GUI
    /// object took it, so the 3D hit is computed.
    static void mouseInput(Engine e, double x, double y, int phase, boolean world) {
        Object mo = e.extra("mouse");
        if (!(mo instanceof Instance)) {
            return;
        }
        Instance m = (Instance) mo;
        m.putQuiet("X", Args.n(x));
        m.putQuiet("Y", Args.n(y));
        Instance cam = e.getWorkspace().findFirstChildOfClass("Camera", false);
        if (cam != null && world) {
            Object[] r = cam.callMethod("ScreenPointToRay", e.getInterp(), new Object[] {cam, Args.n(x), Args.n(y)});
            Ray ray = (Ray) r[0];
            m.putQuiet("UnitRay", ray);
            Object[] hit = null;
            if (e.getPhysics() != null) {
                final Instance filter = m.ref("TargetFilter");
                hit = e.getPhysics().raycast(e, ray.origin, ray.direction.scale(1000), filter == null ? Behaviors.ALL
                        : Behaviors.notIn(filter));
            }
            if (hit != null) {
                Vector3 p = (Vector3) hit[1];
                m.putQuiet("Hit", CFrame.at(p.x, p.y, p.z));
                m.putQuiet("Target", hit[0]);
            } else {
                Vector3 p = ray.origin.add(ray.direction.scale(1000));
                m.putQuiet("Hit", CFrame.at(p.x, p.y, p.z));
                m.putQuiet("Target", null);
            }
        }
        if (phase == 0) {
            m.fireEvent("Button1Down");
        } else if (phase == 2) {
            m.fireEvent("Button1Up");
            Instance t = m.ref("Target");
            if (t != null && world) {
                clickDetector(e, t);
            }
        } else {
            m.fireEvent("Move");
        }
    }

    /// Fires the ClickDetector of a clicked part or its model.
    static void clickDetector(Engine e, Instance part) {
        for (Instance p = part; p != null && p != e.getWorkspace(); p = p.getParent()) { // NOPMD CompareObjectsWithEquals - tree walk
            Instance cd = p.findFirstChildOfClass("ClickDetector", false);
            if (cd != null) {
                Instance lp = e.getLocalPlayer();
                cd.fireEvent("MouseClick", lp);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ run service

    static final Behavior RUN = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "IsClient":
                    return ret(Boolean.valueOf(e.isClient()));
                case "IsServer":
                    return ret(Boolean.valueOf(e.isServer()));
                case "IsStudio":
                    return ret(Boolean.valueOf(e.isStudio()));
                case "IsRunning":
                case "IsRunMode":
                    return ret(Boolean.TRUE);
                case "IsEdit":
                    return ret(Boolean.FALSE);
                case "BindToRenderStep": {
                    String name = Args.str(a, 1, m);
                    double prio = Args.num(a, 2, m);
                    LuaFunction fn = Args.fn(a, 3, m);
                    List<Object[]> b = bindings(e);
                    unbind(b, name);
                    b.add(new Object[] {name, Args.n(prio), fn});
                    Collections.sort(b, new Comparator<Object[]>() {
                        @Override
                        public int compare(Object[] x, Object[] y) {
                            return Double.compare(((Double) x[1]).doubleValue(), ((Double) y[1]).doubleValue());
                        }
                    });
                    return null;
                }
                case "UnbindFromRenderStep":
                    unbind(bindings(e), Args.str(a, 1, m));
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    @SuppressWarnings("unchecked")
    static List<Object[]> bindings(Engine e) {
        Object o = e.extra("renderBindings");
        if (o == null) {
            o = new ArrayList<Object[]>();
            e.extra("renderBindings", o);
        }
        return (List<Object[]>) o;
    }

    static void unbind(List<Object[]> b, String name) {
        for (int i = b.size() - 1; i >= 0; i--) {
            if (name.equals(b.get(i)[0])) {
                b.remove(i);
            }
        }
    }

    static void renderBindings(Engine e, double dt) {
        List<Object[]> b = bindings(e);
        if (b.isEmpty()) {
            return;
        }
        Object[][] snap = b.toArray(new Object[0][]);
        for (Object[] x : snap) {
            e.getScheduler().spawn((LuaFunction) x[2], new Object[] {Args.n(dt)});
        }
    }

    // ------------------------------------------------------------------ input services

    static final Behavior UIS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            Input.State s = Input.state(e);
            switch (m) {
                case "IsKeyDown": {
                    Object k = Args.at(a, 1);
                    return ret(Boolean.valueOf(k instanceof Enums.EnumItem && s.keys.contains(((Enums.EnumItem) k).name)));
                }
                case "IsMouseButtonPressed": {
                    Object k = Args.at(a, 1);
                    return ret(Boolean.valueOf(k instanceof Enums.EnumItem && s.buttons.contains(((Enums.EnumItem) k).name)));
                }
                case "IsGamepadButtonDown": {
                    Object k = Args.at(a, 2);
                    return ret(Boolean.valueOf(k instanceof Enums.EnumItem && s.keys.contains(((Enums.EnumItem) k).name)));
                }
                case "GetKeysPressed": {
                    LuaTable t = new LuaTable();
                    int i = 1;
                    for (String k : s.keys) {
                        Input.InputObject io = Input.make("Keyboard", "Begin", k);
                        t.rawseti(i++, io);
                    }
                    return ret(t);
                }
                case "GetMouseLocation":
                    return ret(s.mouse);
                case "GetMouseDelta":
                    return ret(s.mouseDelta);
                case "GetLastInputType":
                    return ret(s.lastType);
                case "GetFocusedTextBox":
                    return ret(s.focused);
                case "GetConnectedGamepads":
                case "GetNavigationGamepads":
                case "GetMouseButtonsPressed":
                case "GetGamepadState":
                    return ret(new LuaTable());
                case "GetStringForKeyCode": {
                    Object k = Args.at(a, 1);
                    return ret(k instanceof Enums.EnumItem ? ((Enums.EnumItem) k).name : "");
                }
                case "GetPlatform":
                    return ret(Enums.item("Platform", e.getPlatform()));
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior CAS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "BindAction":
                case "BindActionAtPriority": {
                    String name = Args.str(a, 1, m);
                    LuaFunction fn = Args.fn(a, 2, m);
                    int first = "BindAction".equals(m) ? 4 : 5;
                    LuaTable inputs = new LuaTable();
                    for (int i = first; i < a.length; i++) {
                        inputs.rawseti(inputs.length() + 1, a[i]);
                    }
                    Input.bind(e, name, fn, inputs);
                    if (Args.bool(a, 3)) {
                        e.effect(self, "ActionButton", name);
                    }
                    return null;
                }
                case "UnbindAction":
                    Input.unbind(e, Args.str(a, 1, m));
                    e.effect(self, "ActionButtonRemoved", a.length > 1 ? a[1] : null);
                    return null;
                case "UnbindAllActions":
                    Input.state(e).actions.clear();
                    return null;
                case "GetBoundActionInfo":
                case "GetAllBoundActionInfo":
                    return ret(new LuaTable());
                case "SetTitle":
                case "SetImage":
                case "SetPosition":
                case "SetDescription":
                    e.effect(self, "Action" + m, new Object[] {Args.at(a, 1), Args.at(a, 2)});
                    return null;
                case "GetButton":
                    return ret(null);
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ tweens, debris, tags

    static final Behavior TWEEN_SERVICE = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "Create": {
                    Instance target = Args.inst(a, 1, m);
                    Object info = Args.at(a, 2);
                    if (!(info instanceof TweenInfo)) {
                        throw Args.bad(a, 2, m, "TweenInfo");
                    }
                    return ret(Tweens.create(self.getEngine(), target, (TweenInfo) info, Args.table(a, 3, m)));
                }
                case "GetValue": {
                    double t = Args.num(a, 1, m);
                    String style = Tweens.enumName(Args.at(a, 2), "Linear");
                    String dir = Tweens.enumName(Args.at(a, 3), "In");
                    return ret(Args.n(new TweenInfo(1, style, dir, 0, false, 0).ease(Math.max(0, Math.min(1, t)))));
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior DEBRIS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("AddItem".equals(m) || "addItem".equals(m)) {
                final Instance item = Args.inst(a, 1, m);
                self.getEngine().later(Args.optNum(a, 2, 10), new Runnable() {
                    @Override
                    public void run() {
                        if (!item.isDestroyed()) {
                            item.destroy();
                        }
                    }
                });
                return null;
            }
            return PASS_CALL;
        }
    };

    static final Behavior COLLECTION = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "AddTag":
                    e.addTag(Args.inst(a, 1, m), Args.str(a, 2, m));
                    return null;
                case "RemoveTag":
                    e.removeTag(Args.inst(a, 1, m), Args.str(a, 2, m));
                    return null;
                case "HasTag":
                    return ret(Boolean.valueOf(Args.inst(a, 1, m).hasTag(Args.str(a, 2, m))));
                case "GetTagged":
                    return ret(Instance.list(e.tagged(Args.str(a, 1, m))));
                case "GetTags": {
                    LuaTable t = new LuaTable();
                    List<String> tags = Args.inst(a, 1, m).tags();
                    for (int i = 0; i < tags.size(); i++) {
                        t.rawseti(i + 1, tags.get(i));
                    }
                    return ret(t);
                }
                case "GetAllTags": {
                    LuaTable t = new LuaTable();
                    int i = 1;
                    for (String s : e.allTags()) {
                        t.rawseti(i++, s);
                    }
                    return ret(t);
                }
                case "GetInstanceAddedSignal":
                    return ret(self.event("+" + Args.str(a, 1, m)));
                case "GetInstanceRemovedSignal":
                    return ret(self.event("-" + Args.str(a, 1, m)));
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ http

    static final Behavior HTTP = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "JSONEncode":
                    return ret(Json.encode(Args.at(a, 1)));
                case "JSONDecode":
                    return ret(Json.decode(Args.str(a, 1, m)));
                case "GenerateGUID": {
                    boolean braces = a.length < 2 || Args.bool(a, 1);
                    String g = guid(self.getEngine());
                    return ret(braces ? "{" + g + "}" : g);
                }
                case "UrlEncode":
                    return ret(urlEncode(Args.str(a, 1, m)));
                case "GetAsync":
                case "PostAsync":
                case "RequestAsync":
                    // Experiences run sandboxed: outbound HTTP is a platform
                    // decision, so it is refused the way a disabled place refuses it.
                    throw LuaError.rt("Http requests are not enabled. Enable via game settings");
                default:
                    return PASS_CALL;
            }
        }
    };

    static String guid(Engine e) {
        RbxRandom r = (RbxRandom) e.extra("guidRandom");
        if (r == null) {
            r = new RbxRandom(System.currentTimeMillis());
            e.extra("guidRandom", r);
        }
        String hex = "0123456789ABCDEF";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            if (i == 8 || i == 12 || i == 16 || i == 20) {
                b.append('-');
            }
            int v = (int) (r.nextDouble() * 16);
            if (i == 12) {
                v = 4;
            } else if (i == 16) {
                v = 8 | (v & 3);
            }
            b.append(hex.charAt(v));
        }
        return b.toString();
    }

    static String urlEncode(String s) {
        StringBuilder b = new StringBuilder();
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_'
                    || c == '.' || c == '~') {
                b.append(c);
            } else {
                int v = c & 0xff;
                b.append('%').append(hex.charAt(v >> 4)).append(hex.charAt(v & 15));
            }
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ data stores

    static final Behavior DATA_STORES = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "GetDataStore":
                    return ret(store(e, "DataStore", Args.str(a, 1, m), Args.optStr(a, 2, "global")));
                case "GetGlobalDataStore":
                    return ret(store(e, "DataStore", "__global", "global"));
                case "GetOrderedDataStore":
                    return ret(store(e, "OrderedDataStore", Args.str(a, 1, m), Args.optStr(a, 2, "global")));
                case "GetRequestBudgetForRequestType":
                    return ret(Args.n(60));
                default:
                    return PASS_CALL;
            }
        }
    };

    static Instance store(Engine e, String cls, String name, String scope) {
        String key = "store:" + cls + ":" + name + ":" + scope;
        Object s = e.extra(key);
        if (s == null) {
            Instance st = e.create(cls);
            st.setName(name);
            st.host = new String[] {name, scope};
            e.extra(key, st);
            s = st;
        }
        return (Instance) s;
    }

    static String[] storeId(Instance st) {
        return (String[]) st.host;
    }

    static void checkKey(String key) {
        if (key.length() == 0 || key.length() > 50) {
            throw LuaError.rt("Key name can't be empty or longer than 50 characters");
        }
    }

    static Object readValue(Engine e, Instance st, String key) {
        String[] id = storeId(st);
        String json = e.getHost().dataGet(id[0], id[1], key);
        return json == null ? null : Json.decode(json);
    }

    static void writeValue(Engine e, Instance st, String key, Object v) {
        String[] id = storeId(st);
        if (v instanceof LuaFunction || v instanceof Instance) {
            throw LuaError.rt("Cannot store " + Interp.typeName(v) + " in data store. Data stores can only accept valid UTF-8 characters.");
        }
        String json = v == null ? null : Json.encode(v);
        if (json != null && json.length() > 4_194_304) {
            throw LuaError.rt("Value exceeds the 4MB data store limit");
        }
        e.getHost().dataSet(id[0], id[1], key, json);
        Signal s = (Signal) e.extra("onUpdate:" + id[0] + ":" + id[1] + ":" + key);
        if (s != null) {
            s.fire(v);
        }
    }

    static final Behavior DATA_STORE = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            if (!e.isServer()) {
                throw LuaError.rt("DataStore can't be accessed from client");
            }
            switch (m) {
                case "GetAsync": {
                    String k = Args.str(a, 1, m);
                    checkKey(k);
                    return new Object[] {readValue(e, self, k), null};
                }
                case "SetAsync": {
                    String k = Args.str(a, 1, m);
                    checkKey(k);
                    writeValue(e, self, k, Args.at(a, 2));
                    return ret("1");
                }
                case "UpdateAsync": {
                    String k = Args.str(a, 1, m);
                    checkKey(k);
                    LuaFunction fn = Args.fn(a, 2, m);
                    Object cur = readValue(e, self, k);
                    Object[] r = e.invoke(fn, new Object[] {cur, null});
                    Object nv = r.length > 0 ? r[0] : null;
                    if (nv != null) {
                        writeValue(e, self, k, nv);
                    }
                    return new Object[] {nv == null ? cur : nv, null};
                }
                case "RemoveAsync": {
                    String k = Args.str(a, 1, m);
                    Object cur = readValue(e, self, k);
                    writeValue(e, self, k, null);
                    return new Object[] {cur, null};
                }
                case "IncrementAsync": {
                    String k = Args.str(a, 1, m);
                    checkKey(k);
                    Object cur = readValue(e, self, k);
                    double base = cur instanceof Double ? ((Double) cur).doubleValue() : 0;
                    if (cur != null && !(cur instanceof Double)) {
                        throw LuaError.rt("IncrementAsync: value is not a number");
                    }
                    Double nv = Args.n(Math.floor(base + Args.optNum(a, 2, 1)));
                    writeValue(e, self, k, nv);
                    return ret(nv);
                }
                case "OnUpdate": {
                    String[] id = storeId(self);
                    String key = "onUpdate:" + id[0] + ":" + id[1] + ":" + Args.str(a, 1, m);
                    Signal s = (Signal) e.extra(key);
                    if (s == null) {
                        s = new Signal("OnUpdate", e.getScheduler());
                        e.extra(key, s);
                    }
                    return ret(s.connect(Args.fn(a, 2, m), false));
                }
                case "GetSortedAsync": {
                    if (!self.isA("OrderedDataStore")) {
                        return PASS_CALL;
                    }
                    final boolean asc = Args.bool(a, 1);
                    int pageSize = Math.max(1, Math.min(100, (int) Args.optNum(a, 2, 50)));
                    double min = Args.at(a, 3) instanceof Double ? ((Double) a[3]).doubleValue() : -Double.MAX_VALUE;
                    double max = Args.at(a, 4) instanceof Double ? ((Double) a[4]).doubleValue() : Double.MAX_VALUE;
                    String[] id = storeId(self);
                    List<String[]> rows = e.getHost().dataList(id[0], id[1]);
                    List<Object[]> entries = new ArrayList<Object[]>();
                    for (int i = 0; i < rows.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        Object v = Json.decode(rows.get(i)[1]);
                        if (v instanceof Double) {
                            double d = ((Double) v).doubleValue();
                            if (d >= min && d <= max) {
                                entries.add(new Object[] {rows.get(i)[0], v});
                            }
                        }
                    }
                    Collections.sort(entries, new Comparator<Object[]>() {
                        @Override
                        public int compare(Object[] x, Object[] y) {
                            int c = Double.compare(((Double) x[1]).doubleValue(), ((Double) y[1]).doubleValue());
                            return asc ? c : Double.compare(((Double) y[1]).doubleValue(), ((Double) x[1]).doubleValue());
                        }
                    });
                    return ret(pages(e, entries, pageSize));
                }
                case "ListKeysAsync": {
                    String[] id = storeId(self);
                    String prefix = Args.optStr(a, 1, "");
                    List<String[]> rows = e.getHost().dataList(id[0], id[1]);
                    List<Object[]> keys = new ArrayList<Object[]>();
                    for (int i = 0; i < rows.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                        if (rows.get(i)[0].startsWith(prefix)) {
                            keys.add(new Object[] {rows.get(i)[0], null});
                        }
                    }
                    return ret(pages(e, keys, Math.max(1, (int) Args.optNum(a, 2, 50))));
                }
                case "GetVersionAsync": {
                    return new Object[] {readValue(e, self, Args.str(a, 1, m)), null};
                }
                case "ListVersionsAsync":
                    return ret(pages(e, new ArrayList<Object[]>(), 1));
                default:
                    return PASS_CALL;
            }
        }
    };

    static Instance pages(Engine e, List<Object[]> entries, int pageSize) {
        Instance p = e.create("DataStorePages");
        List<LuaTable> all = new ArrayList<LuaTable>();
        LuaTable cur = new LuaTable();
        for (int i = 0; i < entries.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (cur.length() == pageSize) {
                all.add(cur);
                cur = new LuaTable();
            }
            LuaTable row = new LuaTable();
            row.rawset("key", entries.get(i)[0]);
            if (entries.get(i)[1] != null) {
                row.rawset("value", entries.get(i)[1]);
            }
            cur.rawseti(cur.length() + 1, row);
        }
        all.add(cur);
        p.host = all;
        p.putQuiet("IsFinished", Boolean.valueOf(all.size() <= 1));
        return p;
    }

    static final Behavior PAGES = new Behavior() {
        @Override
        @SuppressWarnings("unchecked")
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            List<LuaTable> all = (List<LuaTable>) self.host;
            switch (m) {
                case "GetCurrentPage":
                    return ret(all.isEmpty() ? new LuaTable() : all.get(0));
                case "AdvanceToNextPageAsync":
                    if (!all.isEmpty()) {
                        all.remove(0);
                    }
                    self.put("IsFinished", Boolean.valueOf(all.size() <= 1));
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ marketplace

    static final Behavior MARKET = new Behavior() {
        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("ProcessReceipt".equals(key)) {
                self.putQuiet(key, v);
                return true;
            }
            return false;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "PromptPurchase":
                    e.getHost().promptPurchase(player(a, 1, m), "asset", (long) Args.num(a, 2, m));
                    return null;
                case "PromptGamePassPurchase":
                    e.getHost().promptPurchase(player(a, 1, m), "pass", (long) Args.num(a, 2, m));
                    return null;
                case "PromptProductPurchase":
                    e.getHost().promptPurchase(player(a, 1, m), "product", (long) Args.num(a, 2, m));
                    return null;
                case "PromptPremiumPurchase":
                    e.getHost().promptPurchase(player(a, 1, m), "premium", 0);
                    return null;
                case "PromptSubscriptionPurchase":
                    e.getHost().promptPurchase(player(a, 1, m), "subscription", 0);
                    return null;
                case "UserOwnsGamePassAsync":
                    return ret(Boolean.valueOf(e.getHost().ownsPass((long) Args.num(a, 1, m), (long) Args.num(a, 2, m))));
                case "PlayerOwnsAsset":
                case "PlayerOwnsBundle":
                    return ret(Boolean.valueOf(e.getHost().ownsPass(userId(player(a, 1, m)), (long) Args.num(a, 2, m))));
                case "GetProductInfo": {
                    LuaTable t = new LuaTable();
                    long id = (long) Args.num(a, 1, m);
                    t.rawset("AssetId", Args.n(id));
                    t.rawset("ProductId", Args.n(id));
                    t.rawset("Name", "Item " + id);
                    t.rawset("Description", "");
                    t.rawset("PriceInRobux", Args.n(0));
                    t.rawset("IsForSale", Boolean.TRUE);
                    return ret(t);
                }
                case "GetDeveloperProductsAsync":
                    return ret(pages(e, new ArrayList<Object[]>(), 1));
                case "GetSubscriptionProductInfoAsync":
                case "GetUserSubscriptionStatusAsync":
                    return ret(new LuaTable());
                default:
                    return PASS_CALL;
            }
        }
    };

    /// The host reports a finished purchase prompt: fires the Prompt*Finished
    /// events and, for developer products, runs ProcessReceipt until it
    /// answers PurchaseGranted.
    static void purchaseFinished(Engine e, Instance player, String kind, long id, boolean purchased) {
        Instance ms = e.service("MarketplaceService");
        Double nid = Args.n(id);
        Boolean ok = Boolean.valueOf(purchased);
        if ("pass".equals(kind)) {
            ms.fireEvent("PromptGamePassPurchaseFinished", player, nid, ok);
        } else if ("product".equals(kind)) {
            ms.fireEvent("PromptProductPurchaseFinished", player.prop("UserId"), nid, ok);
            if (purchased) {
                processReceipt(e, ms, player, id);
            }
        } else if ("premium".equals(kind)) {
            ms.fireEvent("PromptPremiumPurchaseFinished");
        } else if ("subscription".equals(kind)) {
            ms.fireEvent("PromptSubscriptionPurchaseFinished", player, nid, ok);
        } else {
            ms.fireEvent("PromptPurchaseFinished", player, nid, ok);
        }
    }

    static void processReceipt(final Engine e, final Instance ms, final Instance player, final long id) {
        Object cb = ms.prop("ProcessReceipt");
        if (!(cb instanceof LuaFunction)) {
            // Retried when the callback appears, as receipts are durable.
            e.later(1, new Runnable() {
                @Override
                public void run() {
                    if (!player.isDestroyed() && player.getParent() != null) {
                        processReceipt(e, ms, player, id);
                    }
                }
            });
            return;
        }
        LuaTable info = new LuaTable();
        info.rawset("PlayerId", player.prop("UserId"));
        info.rawset("ProductId", Args.n(id));
        info.rawset("PurchaseId", guid(e));
        info.rawset("PlaceIdWherePurchased", Args.n(0));
        info.rawset("CurrencySpent", Args.n(0));
        info.rawset("CurrencyType", Enums.item("CurrencyType", "Robux"));
        Object[] r;
        try {
            r = e.invoke((LuaFunction) cb, new Object[] {info});
        } catch (LuaError err) {
            e.getScheduler().error(ms, "ProcessReceipt: " + err.getMessage());
            r = new Object[0];
        }
        Object d = r.length > 0 ? r[0] : null;
        boolean granted = d instanceof Enums.EnumItem && "PurchaseGranted".equals(((Enums.EnumItem) d).name);
        Map<String, Object> f = new LinkedHashMap<String, Object>();
        f.put("userId", player.prop("UserId"));
        f.put("productId", Args.n(id));
        f.put("granted", Boolean.valueOf(granted));
        e.getHost().analytics("purchase.receipt", f);
        if (!granted) {
            e.later(10, new Runnable() {
                @Override
                public void run() {
                    if (!player.isDestroyed() && player.getParent() != null) {
                        processReceipt(e, ms, player, id);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ badges, messaging, teleport

    static final Behavior BADGES = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "AwardBadge":
                    return ret(Boolean.valueOf(e.getHost().awardBadge((long) Args.num(a, 1, m), (long) Args.num(a, 2, m))));
                case "UserHasBadgeAsync":
                    return ret(Boolean.valueOf(e.getHost().hasBadge((long) Args.num(a, 1, m), (long) Args.num(a, 2, m))));
                case "GetBadgeInfoAsync": {
                    LuaTable t = new LuaTable();
                    t.rawset("Name", "Badge " + (long) Args.num(a, 1, m));
                    t.rawset("Description", "");
                    t.rawset("IconImageId", Args.n(0));
                    t.rawset("IsEnabled", Boolean.TRUE);
                    return ret(t);
                }
                case "CheckUserBadgesAsync": {
                    long uid = (long) Args.num(a, 1, m);
                    LuaTable ids = Args.table(a, 2, m);
                    LuaTable out = new LuaTable();
                    for (int i = 1; i <= ids.length(); i++) {
                        Object v = ids.rawgeti(i);
                        if (v instanceof Double && e.getHost().hasBadge(uid, ((Double) v).longValue())) {
                            out.rawseti(out.length() + 1, v);
                        }
                    }
                    return ret(out);
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior MESSAGING = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "PublishAsync": {
                    String json = Json.encode(Args.at(a, 2));
                    if (json.length() > 1024) {
                        throw LuaError.rt("PublishAsync: message exceeds 1kB");
                    }
                    e.getHost().publish(Args.str(a, 1, m), json);
                    return null;
                }
                case "SubscribeAsync": {
                    String topic = Args.str(a, 1, m);
                    return ret(topic(e, topic).connect(Args.fn(a, 2, m), false));
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    static Signal topic(Engine e, String topic) {
        String k = "topic:" + topic;
        Signal s = (Signal) e.extra(k);
        if (s == null) {
            s = new Signal("Subscription", e.getScheduler());
            e.extra(k, s);
        }
        return s;
    }

    /// Delivers a MessagingService message that arrived for this server.
    static void deliverMessage(Engine e, String topic, String json) {
        Signal s = (Signal) e.extra("topic:" + topic);
        if (s == null) {
            return;
        }
        LuaTable msg = new LuaTable();
        msg.rawset("Data", Json.decode(json));
        msg.rawset("Sent", Args.n(System.currentTimeMillis() / 1000.0));
        s.fire(msg);
    }

    static final Behavior TELEPORT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "Teleport": {
                    long place = (long) Args.num(a, 1, m);
                    Object p = Args.at(a, 2);
                    Instance pl = p instanceof Instance ? (Instance) p : e.getLocalPlayer();
                    e.getHost().teleport(pl, place, Args.at(a, 3));
                    return null;
                }
                case "TeleportAsync": {
                    long place = (long) Args.num(a, 1, m);
                    LuaTable ps = Args.table(a, 2, m);
                    Object opts = Args.at(a, 3);
                    Object data = opts instanceof Instance ? ((Instance) opts).getAttribute("__teleportData") : null;
                    for (int i = 1; i <= ps.length(); i++) {
                        Object p = ps.rawgeti(i);
                        if (p instanceof Instance) {
                            e.getHost().teleport((Instance) p, place, data);
                        }
                    }
                    Instance result = e.create("Instance");
                    return ret(result);
                }
                case "TeleportToPlaceInstance":
                case "TeleportToPrivateServer": {
                    long place = (long) Args.num(a, 1, m);
                    Object p = Args.at(a, 3);
                    if (p instanceof Instance) {
                        e.getHost().teleport((Instance) p, place, Args.at(a, 5));
                    } else if (p instanceof LuaTable) {
                        LuaTable ps = (LuaTable) p;
                        for (int i = 1; i <= ps.length(); i++) {
                            if (ps.rawgeti(i) instanceof Instance) {
                                e.getHost().teleport((Instance) ps.rawgeti(i), place, Args.at(a, 5));
                            }
                        }
                    }
                    return null;
                }
                case "ReserveServer":
                    return new Object[] {guid(e), guid(e)};
                case "GetLocalPlayerTeleportData": {
                    Instance lp = e.getLocalPlayer();
                    return ret(lp == null ? null : lp.getAttribute("__teleportData"));
                }
                case "SetTeleportGui":
                    return null;
                case "GetPlayerPlaceInstanceAsync":
                    return new Object[] {Boolean.FALSE, null, Args.n(0), ""};
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ pathfinding

    static final Behavior PATHFINDING = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "CreatePath": {
                    Instance p = e.create("Path");
                    Pathfinder.Agent ag = new Pathfinder.Agent();
                    Object params = Args.at(a, 1);
                    if (params instanceof LuaTable) {
                        LuaTable t = (LuaTable) params;
                        Object r = t.rawget("AgentRadius");
                        Object h = t.rawget("AgentHeight");
                        Object j = t.rawget("AgentCanJump");
                        Object s = t.rawget("WaypointSpacing");
                        if (r instanceof Double) {
                            ag.radius = ((Double) r).doubleValue();
                        }
                        if (h instanceof Double) {
                            ag.height = ((Double) h).doubleValue();
                        }
                        if (j instanceof Boolean) {
                            ag.canJump = ((Boolean) j).booleanValue();
                        }
                        if (s instanceof Double) {
                            ag.spacing = ((Double) s).doubleValue();
                        }
                    }
                    p.host = new Object[] {ag, new ArrayList<QueryParams.Waypoint>()};
                    return ret(p);
                }
                case "FindPathAsync": {
                    Instance p = e.create("Path");
                    p.host = new Object[] {new Pathfinder.Agent(), new ArrayList<QueryParams.Waypoint>()};
                    compute(p, Args.vec3(a, 1, m), Args.vec3(a, 2, m));
                    return ret(p);
                }
                default:
                    return PASS_CALL;
            }
        }
    };

    @SuppressWarnings("unchecked")
    static void compute(Instance path, Vector3 from, Vector3 to) {
        Object[] st = (Object[]) path.host;
        Pathfinder.Agent ag = (Pathfinder.Agent) st[0];
        List<QueryParams.Waypoint> wps = (List<QueryParams.Waypoint>) st[1];
        wps.clear();
        String status = Pathfinder.find(path.getEngine(), ag, from, to, wps);
        path.put("Status", Enums.item("PathStatus", status));
    }

    static final Behavior PATH = new Behavior() {
        @Override
        @SuppressWarnings("unchecked")
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "ComputeAsync":
                    compute(self, Args.vec3(a, 1, m), Args.vec3(a, 2, m));
                    return null;
                case "GetWaypoints": {
                    List<QueryParams.Waypoint> wps = (List<QueryParams.Waypoint>) ((Object[]) self.host)[1];
                    LuaTable t = new LuaTable();
                    for (int i = 0; i < wps.size(); i++) {
                        t.rawseti(i + 1, wps.get(i));
                    }
                    return ret(t);
                }
                case "CheckOcclusionAsync":
                    return ret(Args.n(-1));
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ collision groups

    @SuppressWarnings("unchecked")
    static Map<String, Boolean> groupPairs(Engine e) {
        Object o = e.extra("collisionPairs");
        if (o == null) {
            o = new HashMap<String, Boolean>();
            e.extra("collisionPairs", o);
        }
        return (Map<String, Boolean>) o;
    }

    @SuppressWarnings("unchecked")
    static List<String> groups(Engine e) {
        Object o = e.extra("collisionGroups");
        if (o == null) {
            List<String> l = new ArrayList<String>();
            l.add("Default");
            e.extra("collisionGroups", l);
            o = l;
        }
        return (List<String>) o;
    }

    static String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "\n" + b : b + "\n" + a;
    }

    /// Whether parts in two collision groups collide.
    static boolean groupsCollide(Engine e, String a, String b) {
        Boolean v = groupPairs(e).get(pairKey(a == null ? "Default" : a, b == null ? "Default" : b));
        return v == null || v.booleanValue();
    }

    static final Behavior PHYSICS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            List<String> gs = groups(e);
            switch (m) {
                case "RegisterCollisionGroup":
                case "CreateCollisionGroup": {
                    String n = Args.str(a, 1, m);
                    if (!gs.contains(n)) {
                        if (gs.size() >= 32) {
                            throw LuaError.rt("Too many collision groups (max 32)");
                        }
                        gs.add(n);
                    }
                    return null;
                }
                case "UnregisterCollisionGroup":
                    gs.remove(Args.str(a, 1, m));
                    return null;
                case "IsCollisionGroupRegistered":
                    return ret(Boolean.valueOf(gs.contains(Args.str(a, 1, m))));
                case "RenameCollisionGroup": {
                    int i = gs.indexOf(Args.str(a, 1, m));
                    if (i >= 0) {
                        gs.set(i, Args.str(a, 2, m));
                    }
                    return null;
                }
                case "CollisionGroupSetCollidable": {
                    String x = Args.str(a, 1, m);
                    String y = Args.str(a, 2, m);
                    if (!gs.contains(x) || !gs.contains(y)) {
                        throw LuaError.rt("Collision group does not exist");
                    }
                    groupPairs(e).put(pairKey(x, y), Boolean.valueOf(Args.bool(a, 3)));
                    return null;
                }
                case "CollisionGroupsAreCollidable":
                    return ret(Boolean.valueOf(groupsCollide(e, Args.str(a, 1, m), Args.str(a, 2, m))));
                case "GetRegisteredCollisionGroups": {
                    LuaTable t = new LuaTable();
                    for (int i = 0; i < gs.size(); i++) {
                        LuaTable g = new LuaTable();
                        g.rawset("name", gs.get(i));
                        g.rawset("id", Args.n(i));
                        g.rawset("mask", Args.n(-1));
                        t.rawseti(i + 1, g);
                    }
                    return ret(t);
                }
                case "SetPartCollisionGroup":
                    Args.inst(a, 1, m).put("CollisionGroup", Args.str(a, 2, m));
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ text and chat

    /// A filtered text result, as TextService:FilterStringAsync answers.
    static Instance filtered(Engine e, String text, long from) {
        Instance r = e.create("TextFilterResult");
        r.host = e.getHost().filter(text, from);
        return r;
    }

    static final Behavior FILTER_RESULT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if (m.startsWith("Get")) {
                return ret(self.host);
            }
            return PASS_CALL;
        }
    };

    static final Behavior TRANSLATOR = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("Translate".equals(m)) {
                return ret(Args.str(a, 2, m));
            }
            if ("FormatByKey".equals(m)) {
                return ret(Args.str(a, 1, m));
            }
            return PASS_CALL;
        }
    };

    static final Behavior TEXT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "GetTextSize": {
                    String s = Args.str(a, 1, m);
                    double size = Args.num(a, 2, m);
                    Object frame = Args.at(a, 4);
                    double maxW = frame instanceof Vector2 ? ((Vector2) frame).x : 1e9;
                    double w = s.length() * size * 0.5;
                    double lines = Math.max(1, Math.ceil(w / Math.max(1, maxW)));
                    return ret(new Vector2(Math.min(w, maxW), lines * size));
                }
                case "GetTextBoundsAsync":
                    return ret(new Vector2(0, 0));
                case "FilterStringAsync":
                    return ret(filtered(e, Args.str(a, 1, m), (long) Args.num(a, 2, m)));
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior CHAT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "Chat": {
                    Instance part = Args.inst(a, 1, m);
                    String msg = e.getHost().filter(Args.str(a, 2, m), 0);
                    e.effect(part, "Bubble", msg);
                    self.fireEvent("Chatted", part, msg);
                    return null;
                }
                case "FilterStringAsync":
                case "FilterStringForBroadcast":
                    return ret(e.getHost().filter(Args.str(a, 1, m), userId(player(a, 2, m))));
                case "CanUserChatAsync":
                    return ret(Boolean.valueOf(e.getHost().canChat((long) Args.num(a, 1, m))));
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior TEXT_CHAT = new Behavior() {
        @Override
        public void created(Instance self) {
            // Default channels appear on demand in childAdded below.
        }

        @Override
        public Object get(Instance self, Interp in, String key) {
            if ("TextChannels".equals(key)) {
                return textChannels(self);
            }
            return PASS;
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "CanUserChatAsync":
                    return ret(Boolean.valueOf(e.getHost().canChat((long) Args.num(a, 1, m))));
                case "CanUsersChatAsync":
                    return ret(Boolean.valueOf(e.getHost().canChat((long) Args.num(a, 1, m))
                            && e.getHost().canChat((long) Args.num(a, 2, m))));
                case "DisplayBubble":
                    e.effect(Args.inst(a, 1, m), "Bubble", Args.str(a, 2, m));
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    /// `TextChatService.TextChannels` with the default RBXGeneral and
    /// RBXSystem channels.
    static Instance textChannels(Instance tcs) {
        Instance f = tcs.findFirstChild("TextChannels");
        if (f == null) {
            Engine e = tcs.getEngine();
            f = e.create("Folder");
            f.setName("TextChannels");
            for (String n : new String[] {"RBXGeneral", "RBXSystem"}) {
                Instance c = e.create("TextChannel");
                c.setName(n);
                c.setParent(f);
            }
            f.setParent(tcs);
        }
        return f;
    }

    static final Behavior TEXT_CHANNEL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "SendAsync": {
                    Instance lp = e.getLocalPlayer();
                    long uid = lp == null ? 0 : userId(lp);
                    String text = e.getHost().filter(Args.str(a, 1, m), uid);
                    LuaTable msg = chatMessage(e, text, lp);
                    Instance tcs = e.service("TextChatService");
                    tcs.fireEvent("SendingMessage", msg);
                    self.fireEvent("MessageReceived", msg);
                    tcs.fireEvent("MessageReceived", msg);
                    e.chat(lp, text, self.getName());
                    if (lp != null) {
                        lp.fireEvent("Chatted", text);
                    }
                    return ret(msg);
                }
                case "DisplaySystemMessage": {
                    LuaTable msg = chatMessage(e, Args.str(a, 1, m), null);
                    self.fireEvent("MessageReceived", msg);
                    e.chat(null, Args.str(a, 1, m), self.getName());
                    return ret(msg);
                }
                case "AddUserAsync":
                    return new Object[] {null, Boolean.TRUE};
                default:
                    return PASS_CALL;
            }
        }
    };

    static LuaTable chatMessage(Engine e, String text, Instance from) {
        LuaTable msg = new LuaTable();
        msg.rawset("Text", text);
        msg.rawset("MessageId", guid(e));
        msg.rawset("Timestamp", new QueryParams.DateTime(System.currentTimeMillis()));
        msg.rawset("Status", Enums.item("TextChatMessageStatus", "Success"));
        if (from != null) {
            LuaTable src = new LuaTable();
            src.rawset("UserId", from.prop("UserId"));
            src.rawset("Name", from.getName());
            msg.rawset("TextSource", src);
            msg.rawset("PrefixText", from.str("DisplayName"));
        }
        return msg;
    }

    // ------------------------------------------------------------------ analytics, social, policy

    static final Behavior ANALYTICS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            if (!m.startsWith("Log") && !m.startsWith("Fire")) {
                return PASS_CALL;
            }
            Map<String, Object> f = new LinkedHashMap<String, Object>();
            Object p = Args.at(a, 1);
            if (p instanceof Instance && ((Instance) p).isA("Player")) {
                f.put("userId", ((Instance) p).prop("UserId"));
            }
            for (int i = 2; i < a.length; i++) {
                Object v = a[i];
                if (v instanceof LuaTable) {
                    f.put("arg" + i, Json.encode(v));
                } else if (v instanceof Enums.EnumItem) {
                    f.put("arg" + i, ((Enums.EnumItem) v).name);
                } else if (v instanceof String || v instanceof Double || v instanceof Boolean) {
                    f.put("arg" + i, v);
                }
            }
            e.getHost().analytics("experience." + m, f);
            return null;
        }
    };

    static final Behavior SOCIAL = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "PromptGameInvite": {
                    Instance p = player(a, 1, m);
                    e.getHost().invite(p, null);
                    return null;
                }
                case "CanSendGameInviteAsync":
                    return ret(Boolean.TRUE);
                case "PromptPhoneBook":
                    e.getHost().invite(player(a, 1, m), Args.optStr(a, 2, null));
                    return null;
                case "CanSendCallInviteAsync":
                    return ret(Boolean.FALSE);
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior POLICY = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetPolicyInfoForPlayerAsync".equals(m)) {
                Instance p = player(a, 1, m);
                boolean minor = self.getEngine().getHost().isMinor(userId(p));
                LuaTable t = new LuaTable();
                t.rawset("ArePaidRandomItemsRestricted", Boolean.valueOf(minor));
                t.rawset("IsPaidItemTradingAllowed", Boolean.valueOf(!minor));
                t.rawset("IsSubjectToChinaPolicies", Boolean.FALSE);
                t.rawset("AllowedExternalLinkReferences", new LuaTable());
                t.rawset("IsContentSharingAllowed", Boolean.valueOf(!minor));
                return ret(t);
            }
            return PASS_CALL;
        }
    };

    static final Behavior LOCALIZATION = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetTranslatorForPlayer":
                case "GetTranslatorForPlayerAsync":
                    return ret(self.getEngine().create("Translator"));
                case "GetCountryRegionForPlayerAsync":
                    return ret("US");
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior STARTER_GUI = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            switch (m) {
                case "SetCoreGuiEnabled":
                    e.effect(self, "CoreGui", new Object[] {Args.at(a, 1), Boolean.valueOf(Args.bool(a, 2))});
                    return null;
                case "GetCoreGuiEnabled":
                    return ret(Boolean.TRUE);
                case "SetCore":
                    e.effect(self, "SetCore", new Object[] {Args.str(a, 1, m), Args.at(a, 2)});
                    return null;
                case "GetCore":
                    return ret(null);
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior TEAMS = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetTeams".equals(m)) {
                List<Instance> out = new ArrayList<Instance>();
                List<Instance> c = self.getChildren();
                for (int i = 0; i < c.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                    if (c.get(i).isA("Team")) {
                        out.add(c.get(i));
                    }
                }
                return ret(Instance.list(out));
            }
            return PASS_CALL;
        }
    };

    static final Behavior TEAM = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("GetPlayers".equals(m)) {
                List<Instance> out = new ArrayList<Instance>();
                List<Instance> ps = players(self.getEngine());
                for (int i = 0; i < ps.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                    if (ps.get(i).ref("Team") == self) { // NOPMD CompareObjectsWithEquals - reference identity
                        out.add(ps.get(i));
                    }
                }
                return ret(Instance.list(out));
            }
            return PASS_CALL;
        }
    };

    static final Behavior GUI_SERVICE = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "GetGuiInset":
                    return new Object[] {new Vector2(0, GuiLayout.TOP_INSET), new Vector2(0, 0)};
                case "IsTenFootInterface":
                    return ret(Boolean.FALSE);
                case "OpenBrowserWindow":
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    static final Behavior REPLICATED_FIRST = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("RemoveDefaultLoadingScreen".equals(m)) {
                self.getEngine().effect(self, "RemoveLoadingScreen", null);
                return null;
            }
            if ("IsFinishedReplicating".equals(m)) {
                return ret(Boolean.TRUE);
            }
            return PASS_CALL;
        }
    };

    static final Behavior SOUND_SERVICE = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "PlayLocalSound":
                    self.getEngine().effect(Args.inst(a, 1, m), "PlayLocal", null);
                    return null;
                case "SetListener":
                    return null;
                case "GetListener":
                    return new Object[] {Enums.item("ListenerType", "Camera"), null};
                default:
                    return PASS_CALL;
            }
        }
    };

    // ------------------------------------------------------------------ the platform's own service

    static final Behavior BRICKYARD = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            Engine e = self.getEngine();
            Engine.Host h = e.getHost();
            switch (m) {
                case "SetLeaderstat": {
                    Instance p = player(a, 1, m);
                    String stat = Args.str(a, 2, m);
                    double v = Args.num(a, 3, m);
                    h.leaderstat(userId(p), stat, v);
                    self.fireEvent("LeaderboardChanged", stat);
                    return null;
                }
                case "GetLeaderboardAsync": {
                    String stat = Args.str(a, 1, m);
                    String period = Args.optStr(a, 2, "alltime");
                    int count = (int) Args.optNum(a, 3, 10);
                    List<Object[]> rows = h.leaderboard(stat, period, count);
                    LuaTable t = new LuaTable();
                    for (int i = 0; i < rows.size(); i++) {
                        LuaTable r = new LuaTable();
                        r.rawset("UserId", Args.n(((Number) rows.get(i)[0]).doubleValue()));
                        r.rawset("Name", rows.get(i)[1]);
                        r.rawset("Value", Args.n(((Number) rows.get(i)[2]).doubleValue()));
                        r.rawset("Rank", Args.n(i + 1));
                        t.rawseti(i + 1, r);
                    }
                    return ret(t);
                }
                case "AwardXP": {
                    Instance p = player(a, 1, m);
                    double xp = Args.num(a, 2, m);
                    if (xp < 0 || xp > 10000) {
                        throw LuaError.rt("AwardXP: amount must be between 0 and 10000");
                    }
                    Map<String, Object> f = new LinkedHashMap<String, Object>();
                    f.put("userId", p.prop("UserId"));
                    f.put("xp", Args.n(xp));
                    f.put("reason", Args.optStr(a, 3, ""));
                    h.analytics("platform.xp", f);
                    return null;
                }
                case "CompleteQuest": {
                    Instance p = player(a, 1, m);
                    Map<String, Object> f = new LinkedHashMap<String, Object>();
                    f.put("userId", p.prop("UserId"));
                    f.put("quest", Args.str(a, 2, m));
                    h.analytics("platform.quest", f);
                    return null;
                }
                case "GetPlayerLevel":
                    return ret(Args.n(h.level(userId(player(a, 1, m)))));
                case "PromptInvite":
                    h.invite(player(a, 1, m), Args.optStr(a, 2, null));
                    return null;
                case "LogFunnel": {
                    Instance p = player(a, 1, m);
                    Map<String, Object> f = new LinkedHashMap<String, Object>();
                    f.put("userId", p.prop("UserId"));
                    f.put("funnel", Args.str(a, 2, m));
                    f.put("step", Args.n(Args.num(a, 3, m)));
                    f.put("stepName", Args.optStr(a, 4, ""));
                    h.analytics("experience.funnel", f);
                    return null;
                }
                default:
                    return PASS_CALL;
            }
        }
    };
}
