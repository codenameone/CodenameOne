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

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaError;
import com.codenameone.examples.brickyard.luau.LuaFunction;
import com.codenameone.examples.brickyard.luau.LuaTable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Input from the host: keys, pointers, touches and gamepads, delivered to
/// UserInputService, ContextActionService, the GUI and the player's mouse.
public final class Input {
    private Input() {
    }

    /// One input event, as scripts see it.
    public static final class InputObject extends RbxType {
        final Enums.EnumItem type;
        final Enums.EnumItem state;
        final Enums.EnumItem keyCode;
        Vector3 position = Vector3.ZERO;
        Vector3 delta = Vector3.ZERO;

        InputObject(Enums.EnumItem type, Enums.EnumItem state, Enums.EnumItem keyCode) {
            this.type = type;
            this.state = state;
            this.keyCode = keyCode;
        }

        @Override
        public String typeName() {
            return "InputObject";
        }

        @Override
        public Object get(Interp in, String k) {
            switch (k) {
                case "UserInputType":
                    return type;
                case "UserInputState":
                    return state;
                case "KeyCode":
                    return keyCode;
                case "Position":
                    return position;
                case "Delta":
                    return delta;
                case "ClassName":
                    return "InputObject";
                case "IsModifierKeyDown":
                    return Instance.method("IsModifierKeyDown");
                default:
                    throw LuaError.rt(k + " is not a valid member of InputObject");
            }
        }

        @Override
        public String str() {
            return "InputObject";
        }
    }

    static final class State {
        final Set<String> keys = new HashSet<String>();
        final Set<String> buttons = new HashSet<String>();
        Vector2 mouse = new Vector2(0, 0);
        Vector2 mouseDelta = new Vector2(0, 0);
        Enums.EnumItem lastType = Enums.item("UserInputType", "None");
        final List<Object[]> actions = new ArrayList<Object[]>();
        Instance focused;
    }

    static State state(Engine e) {
        Object o = e.extra("input");
        if (o == null) {
            o = new State();
            e.extra("input", o);
        }
        return (State) o;
    }

    static InputObject make(String type, String st, String key) {
        return new InputObject(Enums.item("UserInputType", type), Enums.item("UserInputState", st),
                Enums.item("KeyCode", key == null ? "Unknown" : key));
    }

    /// A key went down or up. `key` is a `KeyCode` item name ("W", "Space").
    public static void key(Engine e, String key, boolean down) {
        State s = state(e);
        if (down) {
            s.keys.add(key);
        } else {
            s.keys.remove(key);
        }
        InputObject io = make("Keyboard", down ? "Begin" : "End", key);
        deliver(e, io, false);
        Characters.keyChanged(e, key, down);
        if (down && "Space".equals(key)) {
            e.service("UserInputService").fireEvent("JumpRequest");
        }
    }

    /// A gamepad button went down or up (`ButtonA`, `DPadUp`).
    public static void gamepad(Engine e, String button, boolean down) {
        InputObject io = make("Gamepad1", down ? "Begin" : "End", button);
        deliver(e, io, false);
        Characters.keyChanged(e, button, down);
        if (down && "ButtonA".equals(button)) {
            e.service("UserInputService").fireEvent("JumpRequest");
        }
    }

    /// The host's move vector (virtual joystick, thumbstick), each axis -1..1
    /// in camera space: x right, z forward.
    public static void move(Engine e, double x, double z) {
        Characters.setMoveInput(e, x, z);
    }

    /// A pointer (mouse or touch) event at screen coordinates. `phase` is 0
    /// down, 1 move, 2 up.
    public static void pointer(Engine e, double x, double y, int phase, boolean touch) {
        State s = state(e);
        s.mouseDelta = new Vector2(x - s.mouse.x, y - s.mouse.y);
        s.mouse = new Vector2(x, y);
        String type = touch ? "Touch" : phase == 1 ? "MouseMovement" : "MouseButton1";
        String st = phase == 0 ? "Begin" : phase == 1 ? "Change" : "End";
        InputObject io = make(type, st, null);
        io.position = new Vector3(x, y, 0);
        io.delta = new Vector3(s.mouseDelta.x, s.mouseDelta.y, 0);
        if (phase == 0) {
            s.buttons.add(type);
        } else if (phase == 2) {
            s.buttons.remove(type);
        }
        Instance gui = guiAt(e, x, y);
        boolean processed = gui != null && gui.bool("Active");
        if (gui != null) {
            if (phase == 0) {
                gui.fireEvent("InputBegan", io);
            } else if (phase == 2) {
                gui.fireEvent("InputEnded", io);
                if (button(gui)) {
                    gui.fireEvent("Activated", io, Args.n(1));
                    gui.fireEvent(touch ? "TouchTap" : "MouseButton1Click");
                    if (touch) {
                        gui.fireEvent("MouseButton1Click");
                    }
                }
            }
            if (phase == 0 && button(gui)) {
                gui.fireEvent("MouseButton1Down", Args.n(x), Args.n(y));
            } else if (phase == 2 && button(gui)) {
                gui.fireEvent("MouseButton1Up", Args.n(x), Args.n(y));
            }
            if (phase == 2 && gui.isA("TextBox")) {
                e.service("UserInputService").fireEvent("TextBoxFocused", gui);
                s.focused = gui;
                gui.callMethod("CaptureFocus", e.getInterp(), new Object[] {gui});
            }
        }
        deliver(e, io, processed);
        ServiceBehaviors.mouseInput(e, x, y, phase, gui == null);
    }

    /// Text typed into the focused TextBox; `enter` commits it.
    public static void text(Engine e, String text, boolean enter) {
        State s = state(e);
        Instance tb = s.focused;
        if (tb == null) {
            return;
        }
        tb.put("Text", text);
        if (enter) {
            s.focused = null;
            tb.callMethod("ReleaseFocus", e.getInterp(), new Object[] {tb, Boolean.TRUE});
        }
    }

    static void deliver(Engine e, InputObject io, boolean processed) {
        State s = state(e);
        s.lastType = io.type;
        Instance uis = e.service("UserInputService");
        String st = io.state.name;
        String ev = "Begin".equals(st) ? "InputBegan" : "End".equals(st) ? "InputEnded" : "InputChanged";
        if (!processed && !"InputChanged".equals(ev) && actionsHandle(e, io)) {
            processed = true;
        }
        uis.fireEvent(ev, io, Boolean.valueOf(processed));
        if ("Touch".equals(io.type.name)) {
            uis.fireEvent("Begin".equals(st) ? "TouchStarted" : "End".equals(st) ? "TouchEnded" : "TouchMoved",
                    io, Boolean.valueOf(processed));
        }
    }

    /// ContextActionService bindings, newest first, until one sinks.
    static boolean actionsHandle(Engine e, InputObject io) {
        List<Object[]> acts = state(e).actions;
        for (int i = acts.size() - 1; i >= 0; i--) {
            Object[] a = acts.get(i);
            LuaTable inputs = (LuaTable) a[2];
            boolean match = false;
            for (int k = 1; k <= inputs.length(); k++) {
                Object v = inputs.rawgeti(k);
                if (v == io.keyCode || v == io.type) { // NOPMD CompareObjectsWithEquals - enum items are unique
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            Object[] r = e.invoke((LuaFunction) a[1], new Object[] {a[0], io.state, io});
            Object res = r.length > 0 ? r[0] : null;
            if (!(res instanceof Enums.EnumItem) || !"Pass".equals(((Enums.EnumItem) res).name)) {
                return true;
            }
        }
        return false;
    }

    static void bind(Engine e, String name, LuaFunction fn, LuaTable inputs) {
        unbind(e, name);
        state(e).actions.add(new Object[] {name, fn, inputs});
    }

    static void unbind(Engine e, String name) {
        List<Object[]> acts = state(e).actions;
        for (int i = acts.size() - 1; i >= 0; i--) {
            if (name.equals(acts.get(i)[0])) {
                acts.remove(i);
            }
        }
    }

    static boolean button(Instance g) {
        return g.isA("TextButton") || g.isA("ImageButton");
    }

    /// Topmost visible GUI object under a point.
    static Instance guiAt(Engine e, double x, double y) {
        Instance lp = e.getLocalPlayer();
        Instance pg = lp == null ? null : lp.findFirstChildOfClass("PlayerGui", false);
        if (pg == null) {
            return null;
        }
        List<Instance> hits = new ArrayList<Instance>();
        List<Instance> guis = pg.getChildren();
        for (int i = 0; i < guis.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance g = guis.get(i);
            if (g.isA("ScreenGui") && g.bool("Enabled")) {
                collect(g, x, y, hits);
            }
        }
        Instance best = null;
        for (int i = 0; i < hits.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance h = hits.get(i);
            if (best == null || h.num("ZIndex") >= best.num("ZIndex")) {
                best = h;
            }
        }
        return best;
    }

    static void collect(Instance parent, double x, double y, List<Instance> out) {
        List<Instance> kids = parent.getChildren();
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance c = kids.get(i);
            if (!c.isA("GuiObject") || !c.bool("Visible")) {
                continue;
            }
            Vector2 p = (Vector2) c.prop("AbsolutePosition");
            Vector2 s = (Vector2) c.prop("AbsoluteSize");
            boolean inside = x >= p.x && y >= p.y && x < p.x + s.x && y < p.y + s.y;
            if (inside && c.num("BackgroundTransparency") < 1 || inside && button(c)) {
                out.add(c);
            }
            if (inside || !c.bool("ClipsDescendants")) {
                collect(c, x, y, out);
            }
        }
    }
}
