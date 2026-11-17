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
import com.codename1.luau.LuaTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Characters: the block rig a player spawns as, the Humanoid (health,
/// death and respawn, walking, jumping, MoveTo, seats, tools), animation
/// tracks, and a kinematic controller with touch events that runs when no
/// physics world is attached. With one attached, the world moves the root
/// part and this class only keeps the rig and the state machine.
final class Characters {
    private Characters() {
    }

    static final double GRAVITY = 196.2;

    static void install() {
        ClassDb.attach("Humanoid", HUMANOID);
        ClassDb.attach("Animator", ANIMATOR);
        ClassDb.attach("AnimationTrack", TRACK);
        ClassDb.attach("ProximityPrompt", PROMPT);
    }

    static Object[] ret(Object v) {
        return new Object[] {v};
    }

    /// The per-humanoid controller state, kept in `Humanoid.host`.
    static final class Ctl {
        double vy;
        boolean grounded;
        boolean dead;
        String state = "Running";
        Vector3 moveTarget;
        double moveTimeout;
        final Set<Instance> touching = new HashSet<Instance>();
        final List<Instance> tracks = new ArrayList<Instance>();
    }

    static Ctl ctl(Instance hum) {
        if (!(hum.host instanceof Ctl)) {
            hum.host = new Ctl();
        }
        return (Ctl) hum.host;
    }

    // ------------------------------------------------------------------ rig

    static Instance part(Engine e, String name, Vector3 size, Color3 color, Instance parent) {
        Instance p = e.create("Part");
        p.setName(name);
        p.putQuiet("Size", size);
        p.putQuiet("Color", color);
        p.putQuiet("TopSurface", Enums.item("SurfaceType", "Smooth"));
        p.putQuiet("BottomSurface", Enums.item("SurfaceType", "Smooth"));
        p.setParent(parent);
        return p;
    }

    static void motor(Engine e, String name, Instance p0, Instance p1, CFrame c0, CFrame c1) {
        Instance m = e.create("Motor6D");
        m.setName(name);
        m.putQuiet("Part0", p0);
        m.putQuiet("Part1", p1);
        m.putQuiet("C0", c0);
        m.putQuiet("C1", c1);
        m.setParent(p0);
    }

    static Color3 descColor(Instance desc, String key, Color3 def) {
        Object v = desc == null ? null : desc.prop(key);
        return v instanceof Color3 ? (Color3) v : def;
    }

    /// A block rig in the classic six-part layout, with a Humanoid and an
    /// Animator, coloured from a HumanoidDescription when one is given.
    static Instance buildRig(Engine e, String name, Instance desc) {
        Instance model = e.create("Model");
        model.setName(name);
        Color3 skin = new Color3(234 / 255.0, 184 / 255.0, 146 / 255.0);
        Instance root = part(e, "HumanoidRootPart", new Vector3(2, 2, 1), skin, model);
        root.putQuiet("Transparency", Args.n(1));
        root.putQuiet("CanCollide", Boolean.TRUE);
        Instance torso = part(e, "Torso", new Vector3(2, 2, 1), descColor(desc, "TorsoColor",
                new Color3(13 / 255.0, 105 / 255.0, 172 / 255.0)), model);
        Instance head = part(e, "Head", new Vector3(2, 1, 1), descColor(desc, "HeadColor", skin), model);
        Instance la = part(e, "Left Arm", new Vector3(1, 2, 1), descColor(desc, "LeftArmColor", skin), model);
        Instance ra = part(e, "Right Arm", new Vector3(1, 2, 1), descColor(desc, "RightArmColor", skin), model);
        Color3 legs = new Color3(40 / 255.0, 127 / 255.0, 71 / 255.0);
        Instance ll = part(e, "Left Leg", new Vector3(1, 2, 1), descColor(desc, "LeftLegColor", legs), model);
        Instance rl = part(e, "Right Leg", new Vector3(1, 2, 1), descColor(desc, "RightLegColor", legs), model);
        for (Instance p : new Instance[] {torso, head, la, ra, ll, rl}) {
            p.putQuiet("CanCollide", Boolean.FALSE);
        }
        motor(e, "RootJoint", root, torso, CFrame.IDENTITY, CFrame.IDENTITY);
        motor(e, "Neck", torso, head, CFrame.at(0, 1, 0), CFrame.at(0, -0.5, 0));
        motor(e, "Left Shoulder", torso, la, CFrame.at(-1, 0.5, 0), CFrame.at(0.5, 0.5, 0));
        motor(e, "Right Shoulder", torso, ra, CFrame.at(1, 0.5, 0), CFrame.at(-0.5, 0.5, 0));
        motor(e, "Left Hip", torso, ll, CFrame.at(-1, -1, 0), CFrame.at(-0.5, 1, 0));
        motor(e, "Right Hip", torso, rl, CFrame.at(1, -1, 0), CFrame.at(0.5, 1, 0));
        Instance face = e.create("Decal");
        face.setName("face");
        face.putQuiet("Texture", "brickyard://face/smile");
        face.putQuiet("Face", Enums.item("NormalId", "Front"));
        face.setParent(head);
        Instance att = e.create("Attachment");
        att.setName("RightGripAttachment");
        att.putQuiet("CFrame", CFrame.at(0, -1, 0));
        att.setParent(ra);
        Instance hum = e.create("Humanoid");
        hum.setName("Humanoid");
        hum.putQuiet("RootPart", root);
        hum.putQuiet("HipHeight", Args.n(0));
        hum.putQuiet("RigType", Enums.item("HumanoidRigType", "R6"));
        Instance animator = e.create("Animator");
        animator.setName("Animator");
        animator.setParent(hum);
        hum.setParent(model);
        model.putQuiet("PrimaryPart", root);
        syncRig(model);
        return model;
    }

    /// Places every Motor6D's Part1 from its Part0: `Part1 = Part0 * C0 *
    /// Transform * C1^-1`, walking out from the root.
    static void syncRig(Instance model) {
        List<Instance> ms = new ArrayList<Instance>();
        List<Instance> all = model.descendants();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance j = all.get(i);
            if ((j.isA("Motor6D") || j.isA("Weld")) && j.bool("Enabled")) {
                ms.add(j);
            }
        }
        // Motors are listed in creation order, parents before children, so
        // one pass places the chain; a second covers hand-built rigs.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < ms.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                Instance m = ms.get(i);
                Instance p0 = m.ref("Part0");
                Instance p1 = m.ref("Part1");
                if (p0 == null || p1 == null) {
                    continue;
                }
                CFrame t = m.isA("Motor6D") ? (CFrame) m.prop("Transform") : CFrame.IDENTITY;
                CFrame c = Behaviors.cf(p0).mul((CFrame) m.prop("C0")).mul(t).mul(((CFrame) m.prop("C1")).inverse());
                if (!c.equals(p1.prop("CFrame"))) {
                    p1.put("CFrame", c);
                }
            }
        }
    }

    // ------------------------------------------------------------------ spawning

    static CFrame spawnPoint(Engine e, Instance player) {
        Instance rl = player.ref("RespawnLocation");
        List<Instance> spawns = new ArrayList<Instance>();
        if (rl != null && rl.isA("SpawnLocation")) {
            spawns.add(rl);
        } else {
            List<Instance> all = e.getWorkspace().descendants();
            for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                Instance s = all.get(i);
                if (s.isA("SpawnLocation") && s.bool("Enabled")) {
                    boolean ok = s.bool("Neutral") || player.bool("Neutral")
                            || s.prop("TeamColor").equals(player.prop("TeamColor"));
                    if (ok) {
                        spawns.add(s);
                    }
                }
            }
        }
        if (spawns.isEmpty()) {
            return CFrame.at(0, 5, 0);
        }
        Instance s = spawns.get((int) (Math.random() * spawns.size()));
        CFrame c = Behaviors.cf(s);
        Vector3 size = Behaviors.size(s);
        double top = c.y + size.y / 2;
        // Anywhere on the pad, a stud in from its edges: players who all
        // spawned on the centre stood inside one another and the physics
        // stacked them into a tower.
        double ox = (Math.random() - 0.5) * Math.max(0, size.x - 2);
        double oz = (Math.random() - 0.5) * Math.max(0, size.z - 2);
        return CFrame.at(c.x + c.r00 * ox + c.r02 * oz, top + 3, c.z + c.r20 * ox + c.r22 * oz);
    }

    /// `Player:LoadCharacter()`: removes the old character, builds a rig,
    /// copies StarterCharacterScripts and the StarterPack into it, and parents
    /// it to the workspace.
    static void load(Engine e, Instance player) {
        Instance old = player.ref("Character");
        if (old != null) {
            player.fireEvent("CharacterRemoving", old);
            old.destroy();
        }
        Instance starter = e.service("StarterPlayer").findFirstChild("StarterCharacter");
        Instance model = starter != null ? starter.cloneTree() : buildRig(e, player.getName(), null);
        model.setName(player.getName());
        // The body the player chose in the avatar editor rides on the Player;
        // the character carries it so every client draws the same figure.
        Object body = player.getAttribute("AvatarBody");
        if (body instanceof String && model.getAttribute("AvatarBody") == null) {
            model.setAttribute("AvatarBody", body);
        }
        Instance hum = model.findFirstChildOfClass("Humanoid", false);
        Instance sp = e.service("StarterPlayer");
        if (hum != null) {
            hum.putQuiet("WalkSpeed", sp.prop("CharacterWalkSpeed"));
            hum.putQuiet("JumpPower", sp.prop("CharacterJumpPower"));
            hum.putQuiet("JumpHeight", sp.prop("CharacterJumpHeight"));
            hum.putQuiet("UseJumpPower", sp.prop("CharacterUseJumpPower"));
            hum.putQuiet("DisplayName", player.str("DisplayName"));
            hum.host = new Ctl();
        }
        Behaviors.pivotTo(model, spawnPoint(e, player));
        syncRig(model);
        Instance scs = sp.findFirstChild("StarterCharacterScripts");
        if (scs != null) {
            Engine.copyChildren(scs, model);
        }
        Instance backpack = player.findFirstChildOfClass("Backpack", false);
        if (backpack != null) {
            backpack.clearAllChildren();
            Engine.copyChildren(e.service("StarterPack"), backpack);
            Instance gear = player.findFirstChildOfClass("StarterGear", false);
            if (gear != null) {
                Engine.copyChildren(gear, backpack);
            }
        }
        if (e.service("StarterGui").bool("ResetPlayerGuiOnSpawn")) {
            resetGui(e, player);
        }
        player.put("Character", model);
        model.setParent(e.getWorkspace());
        Instance cam = e.getWorkspace().findFirstChildOfClass("Camera", false);
        if (cam != null && player == e.getLocalPlayer() && hum != null) { // NOPMD CompareObjectsWithEquals - reference identity
            cam.put("CameraSubject", hum);
        }
        player.fireEvent("CharacterAdded", model);
        player.fireEvent("CharacterAppearanceLoaded", model);
    }

    static void resetGui(Engine e, Instance player) {
        Instance pg = player.findFirstChildOfClass("PlayerGui", false);
        if (pg == null || player != e.getLocalPlayer()) { // NOPMD CompareObjectsWithEquals - reference identity
            return;
        }
        List<Instance> kids = new ArrayList<Instance>(pg.getChildren());
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance g = kids.get(i);
            if (g.isA("LayerCollector") && g.bool("ResetOnSpawn")) {
                g.destroy();
            }
        }
        List<Instance> starters = e.service("StarterGui").getChildren();
        for (int i = 0; i < starters.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance g = starters.get(i);
            if (!g.isA("LayerCollector") || g.bool("ResetOnSpawn") || pg.findFirstChild(g.getName()) == null) {
                Instance c = g.cloneTree();
                if (c != null) {
                    c.setParent(pg);
                }
            }
        }
    }

    // ------------------------------------------------------------------ humanoid

    static void die(final Instance hum) {
        Ctl c = ctl(hum);
        if (c.dead) {
            return;
        }
        c.dead = true;
        setState(hum, "Dead");
        hum.fireEvent("Died");
        final Instance model = hum.getParent();
        if (model != null && hum.bool("BreakJointsOnDeath")) {
            List<Instance> all = model.descendants();
            for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                if (all.get(i).isA("Motor6D")) {
                    all.get(i).put("Enabled", Boolean.FALSE);
                } else if (all.get(i).isA("BasePart")) {
                    // The body stays where it fell until the respawn: no
                    // solver owns a dead character's parts, and left loose
                    // they drifted down through the lava that killed them.
                    all.get(i).put("Anchored", Boolean.TRUE);
                }
            }
        }
        final Engine e = hum.getEngine();
        final Instance player = model == null ? null : ServiceBehaviors.playerFromCharacter(e, model);
        if (player != null && e.service("Players").bool("CharacterAutoLoads") && e.isServer()) {
            e.later(e.service("Players").num("RespawnTime"), new Runnable() {
                @Override
                public void run() {
                    if (player.getParent() != null && player.ref("Character") == model) { // NOPMD CompareObjectsWithEquals - reference identity
                        load(e, player);
                    }
                }
            });
        }
    }

    /// The humanoid's current `HumanoidStateType` name.
    static String stateOf(Instance hum) {
        return ctl(hum).state;
    }

    static void setState(Instance hum, String s) {
        Ctl c = ctl(hum);
        if (s.equals(c.state)) {
            return;
        }
        String old = c.state;
        c.state = s;
        hum.fireEvent("StateChanged", Enums.item("HumanoidStateType", old), Enums.item("HumanoidStateType", s));
        if ("Jumping".equals(s)) {
            hum.fireEvent("Jumping", Boolean.TRUE);
        } else if ("Freefall".equals(s)) {
            hum.fireEvent("FreeFalling", Boolean.TRUE);
        }
    }

    static final Behavior HUMANOID = new Behavior() {
        @Override
        public boolean set(Instance self, Interp in, String key, Object v) {
            if ("Health".equals(key)) {
                Double d = Interp.toNumber(v);
                if (d == null) {
                    throw LuaError.rt("Unable to assign property Health. number expected");
                }
                if (ctl(self).dead) {
                    return true;
                }
                double h = Math.max(0, Math.min(self.num("MaxHealth"), d.doubleValue()));
                self.put("Health", Args.n(h));
                return true;
            }
            if ("Jump".equals(key) && Boolean.TRUE.equals(v)) {
                self.putQuiet("Jump", Boolean.TRUE);
                return true;
            }
            return false;
        }

        @Override
        public void changed(Instance self, String prop) {
            if ("Health".equals(prop)) {
                double h = self.num("Health");
                self.fireEvent("HealthChanged", Args.n(h));
                if (h <= 0) {
                    die(self);
                }
            } else if ("MaxHealth".equals(prop) && self.num("Health") > self.num("MaxHealth")) {
                self.put("Health", self.prop("MaxHealth"));
            } else if ("Sit".equals(prop) && !self.bool("Sit")) {
                unsit(self);
            }
        }

        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "TakeDamage": {
                    double d = Args.num(a, 1, m);
                    Instance model = self.getParent();
                    if (model != null && model.findFirstChildOfClass("ForceField", false) != null) {
                        return null;
                    }
                    self.set(in, "Health", Args.n(self.num("Health") - d));
                    return null;
                }
                case "Move": {
                    Vector3 d = Args.vec3(a, 1, m);
                    boolean rel = Args.bool(a, 2);
                    if (rel) {
                        Instance cam = self.getEngine().getWorkspace().findFirstChildOfClass("Camera", false);
                        if (cam != null) {
                            d = flatten(((CFrame) cam.prop("CFrame")).vectorToWorld(d));
                        }
                    }
                    self.put("MoveDirection", d.magnitude() > 1 ? d.unit() : d);
                    ctl(self).moveTarget = null;
                    return null;
                }
                case "MoveTo": {
                    Vector3 p = Args.vec3(a, 1, m);
                    Object part = Args.at(a, 2);
                    Ctl c = ctl(self);
                    if (part instanceof Instance) {
                        p = Behaviors.cf((Instance) part).pointToWorld(p);
                        self.putQuiet("WalkToPart", part);
                    }
                    self.put("WalkToPoint", p);
                    c.moveTarget = p;
                    c.moveTimeout = 8;
                    return null;
                }
                case "ChangeState": {
                    Object s = Args.at(a, 1);
                    if (s instanceof Enums.EnumItem) {
                        String n = ((Enums.EnumItem) s).name;
                        if ("Jumping".equals(n)) {
                            self.putQuiet("Jump", Boolean.TRUE);
                        } else if ("Dead".equals(n)) {
                            self.put("Health", Args.n(0));
                        } else {
                            setState(self, n);
                        }
                    }
                    return null;
                }
                case "GetState":
                    return ret(Enums.item("HumanoidStateType", ctl(self).state));
                case "SetStateEnabled":
                    return null;
                case "GetStateEnabled":
                    return ret(Boolean.TRUE);
                case "LoadAnimation": {
                    Instance anim = Args.inst(a, 1, m);
                    Instance animator = self.findFirstChildOfClass("Animator", false);
                    return ret(loadAnimation(animator != null ? animator : self, anim));
                }
                case "GetPlayingAnimationTracks":
                    return ret(playing(self));
                case "EquipTool": {
                    Instance tool = Args.inst(a, 1, m);
                    unequip(self);
                    tool.setParent(self.getParent());
                    return null;
                }
                case "UnequipTools":
                    unequip(self);
                    return null;
                case "GetAppliedDescription":
                    return ret(self.getEngine().create("HumanoidDescription"));
                case "ApplyDescription":
                case "ApplyDescriptionReset": {
                    applyDescription(self, Args.inst(a, 1, m));
                    return null;
                }
                case "AddAccessory": {
                    Instance acc = Args.inst(a, 1, m);
                    acc.setParent(self.getParent());
                    return null;
                }
                case "GetAccessories": {
                    List<Instance> out = new ArrayList<Instance>();
                    Instance model = self.getParent();
                    if (model != null) {
                        List<Instance> kids = model.getChildren();
                        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                            if (kids.get(i).isA("Accessory")) {
                                out.add(kids.get(i));
                            }
                        }
                    }
                    return ret(Instance.list(out));
                }
                case "RemoveAccessories": {
                    Instance model = self.getParent();
                    if (model != null) {
                        List<Instance> kids = new ArrayList<Instance>(model.getChildren());
                        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
                            if (kids.get(i).isA("Accessory")) {
                                kids.get(i).destroy();
                            }
                        }
                    }
                    return null;
                }
                case "BuildRigFromAttachments":
                    if (self.getParent() != null) {
                        syncRig(self.getParent());
                    }
                    return null;
                default:
                    return PASS_CALL;
            }
        }
    };

    static Vector3 flatten(Vector3 v) {
        Vector3 f = new Vector3(v.x, 0, v.z);
        return f.magnitude() < 1e-9 ? Vector3.ZERO : f.unit().scale(Math.min(1, v.magnitude()));
    }

    static void unequip(Instance hum) {
        Instance model = hum.getParent();
        if (model == null) {
            return;
        }
        Instance player = ServiceBehaviors.playerFromCharacter(hum.getEngine(), model);
        Instance bp = player == null ? null : player.findFirstChildOfClass("Backpack", false);
        List<Instance> kids = new ArrayList<Instance>(model.getChildren());
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (kids.get(i).isA("Tool")) {
                kids.get(i).setParent(bp);
            }
        }
    }

    static void applyDescription(Instance hum, Instance desc) {
        Instance model = hum.getParent();
        if (model == null) {
            return;
        }
        String[][] map = {{"Head", "HeadColor"}, {"Torso", "TorsoColor"}, {"Left Arm", "LeftArmColor"},
            {"Right Arm", "RightArmColor"}, {"Left Leg", "LeftLegColor"}, {"Right Leg", "RightLegColor"}};
        for (String[] m : map) {
            Instance p = model.findFirstChild(m[0]);
            Object c = desc.prop(m[1]);
            if (p != null && c instanceof Color3) {
                p.put("Color", c);
            }
        }
    }

    /// Attaches a tool's Handle to the character's right hand.
    static void gripTool(Instance tool, Instance model) {
        Instance handle = tool.findFirstChild("Handle");
        Instance arm = model.findFirstChild("Right Arm");
        if (arm == null) {
            arm = model.findFirstChild("RightHand");
        }
        if (handle == null || arm == null || !handle.isA("BasePart")) {
            return;
        }
        Engine e = tool.getEngine();
        Instance old = arm.findFirstChild("RightGrip");
        if (old != null) {
            old.destroy();
        }
        Instance w = e.create("Weld");
        w.setName("RightGrip");
        w.putQuiet("Part0", arm);
        w.putQuiet("Part1", handle);
        w.putQuiet("C0", CFrame.at(0, -1, 0).mul(CFrame.rotX(-Math.PI / 2)));
        Object grip = tool.prop("Grip");
        w.putQuiet("C1", grip instanceof CFrame ? grip : CFrame.IDENTITY);
        w.setParent(arm);
        syncRig(model);
    }

    /// `Seat:Sit(humanoid)`.
    static void sit(Instance hum, Instance seat) {
        if (!hum.isA("Humanoid") || seat.bool("Disabled") || seat.ref("Occupant") != null) {
            return;
        }
        Instance model = hum.getParent();
        Instance root = model == null ? null : model.findFirstChild("HumanoidRootPart");
        if (root == null) {
            return;
        }
        Engine e = hum.getEngine();
        Instance w = e.create("Weld");
        w.setName("SeatWeld");
        w.putQuiet("Part0", seat);
        w.putQuiet("Part1", root);
        w.putQuiet("C0", CFrame.at(0, Behaviors.size(seat).y / 2 + 1.5, 0));
        w.setParent(seat);
        seat.put("Occupant", hum);
        hum.put("Sit", Boolean.TRUE);
        hum.putQuiet("SeatPart", seat);
        setState(hum, "Seated");
        hum.fireEvent("Seated", Boolean.TRUE, seat);
    }

    static void unsit(Instance hum) {
        Instance seat = hum.ref("SeatPart");
        if (seat == null) {
            return;
        }
        Instance w = seat.findFirstChild("SeatWeld");
        if (w != null) {
            w.destroy();
        }
        seat.put("Occupant", null);
        hum.putQuiet("SeatPart", null);
        setState(hum, "Running");
        hum.fireEvent("Seated", Boolean.FALSE, null);
    }

    // ------------------------------------------------------------------ animation

    static final Behavior ANIMATOR = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("LoadAnimation".equals(m)) {
                return ret(loadAnimation(self, Args.inst(a, 1, m)));
            }
            if ("GetPlayingAnimationTracks".equals(m)) {
                Instance hum = self.getParent();
                return ret(hum == null ? new LuaTable() : playing(hum));
            }
            return PASS_CALL;
        }
    };

    static Instance humanoidOf(Instance animatorOrHumanoid) {
        return animatorOrHumanoid.isA("Humanoid") ? animatorOrHumanoid : animatorOrHumanoid.getParent();
    }

    static Instance loadAnimation(Instance owner, Instance anim) {
        if (!anim.isA("Animation")) {
            throw LuaError.rt("LoadAnimation requires an Animation object");
        }
        Engine e = owner.getEngine();
        Instance t = e.create("AnimationTrack");
        t.setName(anim.getName());
        t.putQuiet("Animation", anim);
        Instance hum = humanoidOf(owner);
        t.host = hum;
        Instance seq = Animations.resolve(e, anim.str("AnimationId"));
        if (seq != null) {
            t.putQuiet("Length", Args.n(Animations.length(seq)));
            t.putQuiet("Looped", Boolean.valueOf(seq.bool("Loop")));
        }
        if (hum != null && hum.isA("Humanoid")) {
            ctl(hum).tracks.add(t);
        }
        return t;
    }

    static LuaTable playing(Instance hum) {
        List<Instance> out = new ArrayList<Instance>();
        List<Instance> ts = ctl(hum).tracks;
        for (int i = 0; i < ts.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            if (ts.get(i).bool("IsPlaying")) {
                out.add(ts.get(i));
            }
        }
        return Instance.list(out);
    }

    static final Behavior TRACK = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            switch (m) {
                case "Play": {
                    self.put("TimePosition", Args.n(0));
                    self.put("IsPlaying", Boolean.TRUE);
                    if (a.length > 3 && a[3] instanceof Double) {
                        self.put("Speed", a[3]);
                    }
                    self.put("WeightCurrent", self.prop("WeightTarget"));
                    Object hum = self.host;
                    if (hum instanceof Instance) {
                        ((Instance) hum).fireEvent("AnimationPlayed", self);
                    }
                    return null;
                }
                case "Stop":
                    stopTrack(self);
                    return null;
                case "AdjustSpeed":
                    self.put("Speed", Args.n(Args.optNum(a, 1, 1)));
                    return null;
                case "AdjustWeight":
                    self.put("WeightTarget", Args.n(Args.optNum(a, 1, 1)));
                    self.put("WeightCurrent", Args.n(Args.optNum(a, 1, 1)));
                    return null;
                case "GetMarkerReachedSignal":
                    return ret(self.event("Marker:" + Args.str(a, 1, m)));
                case "GetTimeOfKeyframe":
                    return ret(Args.n(0));
                default:
                    return PASS_CALL;
            }
        }
    };

    static void stopTrack(Instance t) {
        if (!t.bool("IsPlaying")) {
            return;
        }
        t.put("IsPlaying", Boolean.FALSE);
        t.fireEvent("Stopped");
        t.fireEvent("Ended");
        Object hum = t.host;
        if (hum instanceof Instance) {
            Instance model = ((Instance) hum).getParent();
            if (model != null) {
                Animations.reset(model);
            }
        }
    }

    // ------------------------------------------------------------------ proximity prompts

    static final Behavior PROMPT = new Behavior() {
        @Override
        public Object[] call(Instance self, String m, Interp in, Object[] a) {
            if ("InputHoldBegin".equals(m) || "InputHoldEnd".equals(m)) {
                if ("InputHoldEnd".equals(m)) {
                    trigger(self);
                }
                return null;
            }
            return PASS_CALL;
        }
    };

    static void trigger(Instance prompt) {
        if (!prompt.bool("Enabled")) {
            return;
        }
        Engine e = prompt.getEngine();
        Instance lp = e.getLocalPlayer();
        prompt.fireEvent("Triggered", lp);
        e.service("ProximityPromptService").fireEvent("PromptTriggered", prompt, lp);
        prompt.fireEvent("TriggerEnded", lp);
    }

    /// The prompt nearest the local character within its range, if any.
    static Instance nearestPrompt(Engine e, String key) {
        Instance lp = e.getLocalPlayer();
        Instance ch = lp == null ? null : lp.ref("Character");
        Instance root = ch == null ? null : ch.findFirstChild("HumanoidRootPart");
        if (root == null) {
            return null;
        }
        Vector3 at = Behaviors.cf(root).position();
        Instance best = null;
        double bestD = Double.MAX_VALUE;
        List<Instance> all = e.getWorkspace().descendants();
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (!p.isA("ProximityPrompt") || !p.bool("Enabled")) {
                continue;
            }
            Object k = p.prop("KeyboardKeyCode");
            String pk = k instanceof Enums.EnumItem ? ((Enums.EnumItem) k).name : "E";
            if (!pk.equals(key) && !"ButtonX".equals(key)) {
                continue;
            }
            Instance host = p.getParent();
            Vector3 hp = host == null ? null : host.isA("BasePart") ? Behaviors.cf(host).position()
                    : host.isA("Attachment") ? Behaviors.attachmentWorld(host).position()
                    : host.isA("Model") ? Behaviors.pivot(host).position() : null;
            if (hp == null) {
                continue;
            }
            double d = hp.sub(at).magnitude();
            if (d <= p.num("MaxActivationDistance") && d < bestD) {
                best = p;
                bestD = d;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ input and the controller

    static final class MoveInput {
        double x;
        double z;
        final Set<String> keys = new HashSet<String>();
    }

    static MoveInput moveInput(Engine e) {
        Object o = e.extra("moveInput");
        if (o == null) {
            o = new MoveInput();
            e.extra("moveInput", o);
        }
        return (MoveInput) o;
    }

    static void keyChanged(Engine e, String key, boolean down) {
        MoveInput mi = moveInput(e);
        if (down) {
            mi.keys.add(key);
        } else {
            mi.keys.remove(key);
        }
        if (down && ("Space".equals(key) || "ButtonA".equals(key))) {
            Instance hum = localHumanoid(e);
            if (hum != null) {
                if (hum.bool("Sit")) {
                    hum.put("Sit", Boolean.FALSE);
                }
                hum.putQuiet("Jump", Boolean.TRUE);
            }
        }
        if (down) {
            Instance p = nearestPrompt(e, key);
            if (p != null) {
                trigger(p);
            }
        }
    }

    static void setMoveInput(Engine e, double x, double z) {
        MoveInput mi = moveInput(e);
        mi.x = x;
        mi.z = z;
    }

    static Instance localHumanoid(Engine e) {
        Instance lp = e.getLocalPlayer();
        Instance ch = lp == null ? null : lp.ref("Character");
        return ch == null ? null : ch.findFirstChildOfClass("Humanoid", false);
    }

    /// Local movement intent from keys and the stick, in camera space.
    static Vector3 intent(Engine e) {
        MoveInput mi = moveInput(e);
        double x = mi.x;
        double z = mi.z;
        if (mi.keys.contains("W") || mi.keys.contains("Up")) {
            z += 1;
        }
        if (mi.keys.contains("S") || mi.keys.contains("Down")) {
            z -= 1;
        }
        if (mi.keys.contains("D") || mi.keys.contains("Right")) {
            x += 1;
        }
        if (mi.keys.contains("A") || mi.keys.contains("Left")) {
            x -= 1;
        }
        if (x == 0 && z == 0) {
            return Vector3.ZERO;
        }
        Instance cam = e.getWorkspace().findFirstChildOfClass("Camera", false);
        CFrame c = cam == null ? CFrame.IDENTITY : (CFrame) cam.prop("CFrame");
        Vector3 fwd = flatten(c.look());
        Vector3 right = flatten(c.right());
        if (fwd.magnitude() < 1e-6) {
            fwd = new Vector3(0, 0, -1);
            right = new Vector3(1, 0, 0);
        }
        Vector3 d = right.unit().scale(x).add(fwd.unit().scale(z));
        return d.magnitude() > 1 ? d.unit() : d;
    }

    /// Per-frame character work: the local player's input, every humanoid's
    /// walk, jump and gravity, touch events, the rig and animation tracks.
    static void animate(Engine e, double dt) {
        Instance local = localHumanoid(e);
        if (local != null && !ctl(local).dead && ctl(local).moveTarget == null) {
            Vector3 d = intent(e);
            if (!d.equals(local.prop("MoveDirection"))) {
                local.put("MoveDirection", d);
            }
        }
        List<Instance> humanoids = new ArrayList<Instance>();
        List<Instance> kids = e.getWorkspace().getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance h = kids.get(i).findFirstChildOfClass("Humanoid", false);
            if (h != null) {
                humanoids.add(h);
            }
        }
        List<Instance> nested = e.getWorkspace().descendants();
        for (int i = 0; i < nested.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance h = nested.get(i);
            if (h.isA("Humanoid") && !humanoids.contains(h)) {
                humanoids.add(h);
            }
        }
        for (int i = 0; i < humanoids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            step(e, humanoids.get(i), dt);
        }
    }

    /// In a networked game each player's client drives that player's
    /// character and the server drives the rest (NPCs); the other side only
    /// follows what replication tells it. Alone, everything is simulated.
    static boolean simulatedHere(Engine e, Instance model) {
        if (e.getRole() == Engine.SOLO || e.getTransport() == null) {
            return true;
        }
        Instance player = ServiceBehaviors.playerFromCharacter(e, model);
        if (e.getRole() == Engine.SERVER) {
            return player == null;
        }
        return player != null && player == e.getLocalPlayer(); // NOPMD CompareObjectsWithEquals - player identity
    }

    static void step(Engine e, Instance hum, double dt) {
        Instance model = hum.getParent();
        Instance root = hum.ref("RootPart");
        if (root == null && model != null) {
            root = model.findFirstChild("HumanoidRootPart");
        }
        if (model == null || root == null) {
            return;
        }
        Ctl c = ctl(hum);
        Animations.step(model, c.tracks, dt);
        if (!simulatedHere(e, model)) {
            syncRig(model);
            // The owning client moves a player's character, but the server
            // runs the Scripts, so the server finds what it touches: a kill
            // brick, a checkpoint, a coin. Skipping this left every Touched
            // handler in an online room dead, and a player stood on lava.
            if (e.getRole() == Engine.SERVER && !c.dead) {
                touches(e, hum, model, c);
            }
            return;
        }
        if (c.dead || root.bool("Anchored")) {
            syncRig(model);
            return;
        }
        if (hum.bool("Sit")) {
            Instance seat = hum.ref("SeatPart");
            Instance w = seat == null ? null : seat.findFirstChild("SeatWeld");
            if (w != null) {
                root.put("CFrame", Behaviors.cf(seat).mul((CFrame) w.prop("C0")));
            }
            syncRig(model);
            return;
        }
        Vector3 dir = (Vector3) hum.prop("MoveDirection");
        if (c.moveTarget != null) {
            Vector3 to = c.moveTarget.sub(Behaviors.cf(root).position());
            Vector3 flat = new Vector3(to.x, 0, to.z);
            c.moveTimeout -= dt;
            if (flat.magnitude() < 1 || c.moveTimeout <= 0) {
                boolean reached = flat.magnitude() < 1;
                c.moveTarget = null;
                hum.put("MoveDirection", Vector3.ZERO);
                dir = Vector3.ZERO;
                hum.fireEvent("MoveToFinished", Boolean.valueOf(reached));
            } else {
                dir = flat.unit();
                hum.put("MoveDirection", dir);
            }
        }
        // Characters stay kinematic with a physics world too: the world
        // simulates loose parts (RigidBodies skips humanoid models), and a
        // platformer controller that answers input exactly is what players expect.
        CFrame cf = Behaviors.cf(root);
        double speed = hum.num("WalkSpeed");
        double nx = cf.x + dir.x * speed * dt;
        double nz = cf.z + dir.z * speed * dt;
        double half = Behaviors.size(root).y / 2 + 2 + hum.num("HipHeight");
        double ground = groundBelow(e, model, nx, cf.y, nz);
        if (wall(e, model, cf.x, cf.z, nx, cf.y, nz, half)) {
            nx = cf.x;
            nz = cf.z;
            ground = groundBelow(e, model, nx, cf.y, nz);
        }
        boolean grounded = cf.y - half <= ground + 0.05 && c.vy <= 0;
        if (grounded && hum.bool("Jump")) {
            double jv = hum.bool("UseJumpPower") ? hum.num("JumpPower") : Math.sqrt(2 * GRAVITY * hum.num("JumpHeight"));
            c.vy = jv;
            grounded = false;
            setState(hum, "Jumping");
        }
        hum.putQuiet("Jump", Boolean.FALSE);
        double ny;
        if (grounded) {
            c.vy = 0;
            ny = ground + half;
            if (!c.grounded) {
                setState(hum, "Landed");
            }
            setState(hum, "Running");
        } else {
            c.vy -= GRAVITY * e.getWorkspace().num("Gravity") / GRAVITY * dt;
            ny = cf.y + c.vy * dt;
            if (ny - half < ground) {
                ny = ground + half;
                c.vy = 0;
            }
            if (c.vy < 0) {
                setState(hum, "Freefall");
            }
        }
        if (ny < e.getWorkspace().num("FallenPartsDestroyHeight")) {
            hum.put("Health", Args.n(0));
        }
        c.grounded = grounded;
        CFrame rot = cf.rotation();
        if (hum.bool("AutoRotate") && dir.magnitude() > 1e-3) {
            rot = CFrame.lookAt(Vector3.ZERO, dir, Vector3.Y_AXIS);
        }
        root.put("CFrame", rot.withPosition(nx, ny, nz));
        root.putQuiet("AssemblyLinearVelocity", new Vector3(dir.x * speed, c.vy, dir.z * speed));
        if (dir.magnitude() > 1e-3) {
            hum.fireEvent("Running", Args.n(speed));
        }
        syncRig(model);
        touches(e, hum, model, c);
    }

    static boolean inside(Instance p, double x, double y, double z, double pad) {
        CFrame c = Behaviors.cf(p);
        Vector3 hs = Behaviors.size(p).scale(0.5);
        Vector3 l = c.pointToObject(new Vector3(x, y, z));
        return Math.abs(l.x) <= hs.x + pad && Math.abs(l.y) <= hs.y + pad && Math.abs(l.z) <= hs.z + pad;
    }

    static List<Instance> solids(Engine e, Instance model) {
        List<Instance> out = new ArrayList<Instance>();
        List<Instance> all = Behaviors.parts(e.getWorkspace());
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (p.bool("CanCollide") && !p.isDescendantOf(model) && !p.isA("Terrain")) {
                out.add(p);
            }
        }
        return out;
    }

    /// Highest collidable top under (x, z) that is below the feet plus a step.
    static double groundBelow(Engine e, Instance model, double x, double y, double z) {
        double best = -Double.MAX_VALUE;
        List<Instance> ss = solids(e, model);
        for (int i = 0; i < ss.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = ss.get(i);
            CFrame c = Behaviors.cf(p);
            Vector3 hs = Behaviors.size(p).scale(0.5);
            Vector3 l = c.pointToObject(new Vector3(x, c.y, z));
            // All three axes: the point is taken at the part's own height, so
            // the vertical axis reads about zero whichever local axis it is.
            // Testing only X and Z made a part turned on its side (an upright
            // cylinder pad) endless along its local Y, and a character stood
            // on a pad 11 studs away, hovering over lava that never killed it.
            if (Math.abs(l.x) > hs.x + 0.5 || Math.abs(l.y) > hs.y + 0.5 || Math.abs(l.z) > hs.z + 0.5) {
                continue;
            }
            double top;
            if (p.isA("WedgePart")) {
                // The slope, not the bounding box: a ramp is walked up, never
                // stood on top of at its full height from the low end.
                double lz = Math.max(-hs.z, Math.min(hs.z, l.z));
                double lx = Math.max(-hs.x, Math.min(hs.x, l.x));
                top = c.y + c.r10 * lx + c.r11 * wedgeTop(hs, lz) + c.r12 * lz;
            } else {
                top = c.y + Math.abs(c.r10) * hs.x + Math.abs(c.r11) * hs.y + Math.abs(c.r12) * hs.z;
            }
            if (top <= y - 3 + Pathfinder.STEP && top > best) {
                best = top;
            }
        }
        return best;
    }

    /// Whether a collidable part blocks the torso at (x, y, z).
    /// Whether the body standing at (x, z), its root at `y` and `half` above
    /// its feet, would be inside something solid. The whole column from just
    /// over a step to the head is probed: testing the root alone, at hip
    /// height, let a character walk straight through anything lower than
    /// its hips and taller than a step -- a crate, a counter, a low wall.
    /// Whether moving from (`fromX`, `fromZ`) to (`x`, `z`) runs into a solid.
    /// A part the character already overlaps where it stands never blocks:
    /// a player spawned into another character (a crowded lobby, a respawn
    /// on a busy checkpoint) otherwise met a wall in every direction and
    /// could neither walk out nor jump, frozen until that character left.
    static boolean wall(Engine e, Instance model, double fromX, double fromZ, double x, double y, double z, double half) {
        double feet = y - half;
        double[] probes = {feet + Pathfinder.STEP + 0.1, (feet + Pathfinder.STEP + y) / 2, y, y + 1.5};
        List<Instance> ss = solids(e, model);
        for (int i = 0; i < ss.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = ss.get(i);
            if (blocks(p, x, z, probes) && !blocks(p, fromX, fromZ, probes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocks(Instance p, double x, double z, double[] probes) {
        for (int k = 0; k < probes.length; k++) {
            double py = probes[k];
            if (inside(p, x, py, z, 0.5) && (!p.isA("WedgePart") || underSlope(p, x, py, z, 0.5))) {
                return true;
            }
        }
        return false;
    }

    /// The local height of a wedge's sloped face at local `lz`: the face
    /// rises from the bottom of the front (-Z) to the top of the back (+Z),
    /// as the renderer draws it.
    static double wedgeTop(Vector3 hs, double lz) {
        return hs.z < 1e-9 ? hs.y : hs.y * lz / hs.z;
    }

    /// Whether a point inside a wedge's box is also under its slope; the
    /// space above the slope is air a character walks through.
    static boolean underSlope(Instance p, double x, double y, double z, double pad) {
        CFrame c = Behaviors.cf(p);
        Vector3 hs = Behaviors.size(p).scale(0.5);
        Vector3 l = c.pointToObject(new Vector3(x, y, z));
        return l.y <= wedgeTop(hs, Math.max(-hs.z, Math.min(hs.z, l.z))) + pad;
    }

    /// Touched / TouchEnded between the character's parts and the world.
    static void touches(Engine e, Instance hum, Instance model, Ctl c) {
        Instance root = model.findFirstChild("HumanoidRootPart");
        if (root == null) {
            return;
        }
        CFrame rc = Behaviors.cf(root);
        Set<Instance> now = new HashSet<Instance>();
        List<Instance> all = Behaviors.parts(e.getWorkspace());
        for (int i = 0; i < all.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = all.get(i);
            if (!p.bool("CanTouch") || p.isDescendantOf(model)) {
                continue;
            }
            if (inside(p, rc.x, rc.y, rc.z, 1.2) || inside(p, rc.x, rc.y - 2.5, rc.z, 0.6)) {
                now.add(p);
            }
        }
        Instance leg = model.findFirstChild("Left Leg");
        Instance toucher = leg != null ? leg : root;
        for (Instance p : now) {
            if (!c.touching.contains(p)) {
                p.fireEvent("Touched", toucher);
                toucher.fireEvent("Touched", p);
                hum.fireEvent("Touched", p, toucher);
                Instance pm = p.getParent();
                if (p.isA("Seat") && !hum.bool("Sit")) {
                    sit(hum, p);
                }
                if (p.isA("SpawnLocation") && p.bool("AllowTeamChangeOnTouch")) {
                    Instance pl = ServiceBehaviors.playerFromCharacter(e, model);
                    if (pl != null) {
                        pl.put("TeamColor", p.prop("TeamColor"));
                    }
                }
                if (pm != null && pm.isA("Tool") && p.getName().equals("Handle")) {
                    Instance pl = ServiceBehaviors.playerFromCharacter(e, model);
                    if (pl != null && pm.bool("CanBeDropped")) {
                        pm.setParent(pl.findFirstChildOfClass("Backpack", false));
                    }
                }
            }
        }
        for (Instance p : new ArrayList<Instance>(c.touching)) {
            if (!now.contains(p)) {
                if (!p.isDestroyed()) {
                    p.fireEvent("TouchEnded", toucher);
                }
                toucher.fireEvent("TouchEnded", p);
            }
        }
        c.touching.clear();
        c.touching.addAll(now);
    }
}
