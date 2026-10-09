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
package com.codename1.fxcompat.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.codename1.fxcompat.runtime.css.CssDeclaration;
import com.codename1.fxcompat.runtime.css.CssDeclarations;
import com.codename1.fxcompat.runtime.css.CssProperties;
import com.codename1.fxcompat.runtime.css.CssSelector;
import com.codename1.fxcompat.runtime.css.CssSheetData;
import com.codename1.fxcompat.runtime.css.CssValue;
import com.codename1.fxcompat.runtime.css.CssValueParser;
import com.codename1.io.Log;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

/// The style engine that is installed by default: it reads the compiled
/// form of the style sheets a scene and its parents name, matches their
/// rules against each node, and hands the winning values to
/// [StyleTarget#cn1ApplyStyle(String, Object)].
///
/// #### Where a style sheet comes from
///
/// Nothing on a device parses a `.css` file. The build compiles every style
/// sheet of the application into a table (`CssSheetData`) that ships at the
/// root of the bundle, and a location in `getStylesheets()` is only a name
/// for one of them: a `cn1res:` address names it by its path, and any other
/// address -- the `file:` or `jar:` URL a desktop `getResource` answers --
/// by the longest tail of its path that a table exists for. A location no
/// table exists for is reported once and then ignored. The one thing parsed
/// at run time is the inline `style` of a node, by the value parser the
/// build uses.
///
/// #### The cascade
///
/// The style sheets of the scene come first, then those of each ancestor
/// parent from the root down, then the node's own; an import comes before
/// the sheet that imports it. Among the declarations of one property the
/// winner is, in this order: an `!important` one, an inline one, the one
/// with the more specific selector, the one from the later sheet, the one
/// written later.
///
/// #### What a child inherits
///
/// Two things, as in JavaFX: the font -- `-fx-font` and the four names it
/// is short for -- and the colours a rule defines under a name of its own
/// (`-fx-base: #336699`) for other rules to use. An `em` length is relative
/// to the font size styled this way, or to the default font's when none
/// was.
///
/// #### Cost
///
/// A rule is indexed by what its rightmost compound requires, so a node
/// tries only the rules that name its id, one of its classes or its type.
/// The cascade runs in arrays the engine owns and reuses; a node keeps one
/// small object between restyles, which holds what was applied so that an
/// unchanged value is not applied again and a withdrawn one is restored.
/// A node whose styles changed restyles its descendants only when a
/// selector, the font or a colour definition below could tell the
/// difference.
///
/// Like the scene graph, the engine belongs to the JavaFX application
/// thread and keeps no lock.
public final class CssEngine extends StyleEngine {

    private static final long IMPORTANT = 1L << 62;
    private static final long INLINE = 1L << 61;

    private static final HashMap<String, Integer> PROPERTY_IDS = new HashMap<String, Integer>();
    private static final ArrayList<String> PROPERTY_NAMES = new ArrayList<String>();
    private static final HashSet<String> WARNED = new HashSet<String>();
    private static final HashMap<String, Pos> POSITIONS = new HashMap<String, Pos>();

    private static final int FONT = propertyId("-fx-font");
    private static final int FONT_FAMILY = propertyId("-fx-font-family");
    private static final int FONT_SIZE = propertyId("-fx-font-size");
    private static final int FONT_WEIGHT = propertyId("-fx-font-weight");
    private static final int FONT_STYLE = propertyId("-fx-font-style");

    private static final Object MISSING = new Object();
    private static final CssSheet.Decl[] NO_DECLS = new CssSheet.Decl[0];

    static {
        Pos[] all = Pos.values();
        for (int i = 0; i < all.length; i++) {
            POSITIONS.put(CssProperties.lower(all[i].name()).replace('_', '-'), all[i]);
        }
    }

    /// What a node keeps between two restyles.
    private static final class State {
        /// The properties applied to the node and the value each was given.
        int[] ids = new int[4];
        Object[] values = new Object[4];
        int count;
        /// The font in force here, the node's own or inherited; `null` and
        /// `NaN` for a part nothing styled.
        String family;
        double size = Double.NaN;
        Object weight;
        String posture;
        /// The colours this node defines.
        String[] lookupNames;
        CssValue[] lookupValues;
        /// Whether this node or one above it defines a colour.
        boolean lookupsAbove;
        /// What of this node a selector for a descendant could ask about.
        long signature;
        /// The inline style last parsed, and what it parsed to.
        String inlineText = "";
        CssSheet.Decl[] inline = NO_DECLS;

        boolean inherits() {
            return lookupsAbove || family != null || weight != null || posture != null || size == size;
        }

        int indexOf(int id) {
            for (int i = 0; i < count; i++) {
                if (ids[i] == id) {
                    return i;
                }
            }
            return -1;
        }

        void add(int id, Object value) {
            if (count == ids.length) {
                int[] i = new int[count * 2];
                Object[] v = new Object[count * 2];
                System.arraycopy(ids, 0, i, 0, count);
                System.arraycopy(values, 0, v, 0, count);
                ids = i;
                values = v;
            }
            ids[count] = id;
            values[count++] = value;
        }

        void remove(int index) {
            count--;
            ids[index] = ids[count];
            values[index] = values[count];
            values[count] = null;
        }
    }

    private final HashMap<String, Object> sheets = new HashMap<String, Object>();
    private final HashMap<Class<?>, String> typeNames = new HashMap<Class<?>, String>();
    private final CssValueParser parser = new CssValueParser();
    private final ArrayList<CssDeclaration> parsed = new ArrayList<CssDeclaration>();

    // The cascade of the node being styled; reused for every node.
    private final ArrayList<CssSheet> scope = new ArrayList<CssSheet>();
    private final ArrayList<Parent> chain = new ArrayList<Parent>();
    private CssSheet.Decl[] winner = new CssSheet.Decl[64];
    private long[] priority = new long[64];
    private int[] stamp = new int[64];
    private Object[] wanted = new Object[64];
    private int[] touched = new int[64];
    private int touchedCount;
    private int generation;
    private boolean parentSheets;

    private boolean busy;
    private final ArrayList<Node> pending = new ArrayList<Node>();

    /// Creates an engine with no style sheet read yet.
    public CssEngine() {
        // Sheets are read when a scene first names them.
    }

    /// The number the engine knows a property by; names are numbered as
    /// they are first seen.
    static int propertyId(String name) {
        Integer id = PROPERTY_IDS.get(name);
        if (id == null) {
            id = Integer.valueOf(PROPERTY_NAMES.size());
            PROPERTY_NAMES.add(name);
            PROPERTY_IDS.put(name, id);
        }
        return id.intValue();
    }

    /// Logs a message the first time it is given, and never again: a style
    /// sheet that is not there is asked for at every restyle.
    static void warnOnce(String message) {
        if (WARNED.add(message)) {
            Log.p("[css] " + message);
        }
    }

    /// Gives the engine the compiled form of the style sheet a location
    /// names, in place of the table it would read from the bundle. For
    /// tests and tools; `null` makes the engine forget the location.
    public void define(String location, CssSheetData data) {
        if (data == null) {
            sheets.remove(location);
        } else {
            sheets.put(location, CssSheet.of(data));
        }
    }

    // ------------------------------------------------------------- sheets

    private CssSheet sheet(String location) {
        if (location == null) {
            return null;
        }
        Object known = sheets.get(location);
        if (known == null) {
            known = read(location);
            sheets.put(location, known == null ? MISSING : known);
            if (known == null) {
                warnOnce("No compiled style sheet for " + location
                        + "; style sheets are compiled by the build from the application's desktop resources");
            }
        }
        return known instanceof CssSheet ? (CssSheet) known : null;
    }

    private static CssSheet read(String location) {
        String path = location;
        int bang = path.lastIndexOf("!/");
        if (bang >= 0) {
            path = path.substring(bang + 2);
        } else {
            int colon = path.indexOf(':');
            int slash = path.indexOf('/');
            if (colon > 0 && (slash < 0 || colon < slash)) {
                path = path.substring(colon + 1);
            }
        }
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        try {
            // The whole path first, then each shorter tail of it: a desktop
            // URL starts with directories the bundle knows nothing about.
            while (true) {
                int start = 0;
                while (start < path.length() && path.charAt(start) == '/') {
                    start++;
                }
                path = path.substring(start);
                if (path.length() == 0) {
                    return null;
                }
                CssSheet found = CssSheet.load(path);
                if (found != null) {
                    return found;
                }
                int next = path.indexOf('/');
                if (next < 0) {
                    return null;
                }
                path = path.substring(next);
            }
        } catch (IOException e) {
            warnOnce("The compiled style sheet for " + location + " cannot be read: " + e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------- the calls

    @Override
    public void stylesheetsChanged(Parent parent) {
        parentSheets = true;
        super.stylesheetsChanged(parent);
    }

    @Override
    public void restyle(Node node) {
        if (node == null) {
            return;
        }
        if (busy) {
            // Something a styled value set asked for another node; it waits
            // for the cascade arrays to be free.
            pending.add(node);
            return;
        }
        process(node, false);
        while (!pending.isEmpty()) {
            Node next = pending.remove(pending.size() - 1);
            if (next.getScene() != null) {
                process(next, false);
            }
        }
    }

    private static State stateOf(Node node) {
        Object s = node.cn1StyleState();
        return s instanceof State ? (State) s : null;
    }

    private void process(Node node, boolean force) {
        boolean deep = force;
        busy = true;
        try {
            deep |= style(node);
        } finally {
            busy = false;
        }
        if (deep && node instanceof Parent) {
            ObservableList<Node> children = ((Parent) node).getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                process(children.get(i), true);
            }
        }
    }

    /// Styles one node; answers whether its descendants could be styled
    /// differently for it.
    private boolean style(Node node) {
        State state = stateOf(node);
        Parent parent = node.getParent();
        State above = parent == null ? null : stateOf(parent);
        collectScope(node);
        String inlineText = node.getStyle();
        boolean fresh = state == null;
        if (fresh) {
            if (scope.isEmpty() && inlineText.length() == 0 && (above == null || !above.inherits())) {
                return false;
            }
            state = new State();
            node.cn1SetStyleState(state);
        }

        // 1. The cascade.
        generation++;
        touchedCount = 0;
        String id = node.getId();
        String type = typeName(node);
        ObservableList<String> classes = node.getStyleClass();
        long signature = 0;
        for (int s = 0; s < scope.size(); s++) {
            CssSheet sheet = scope.get(s);
            if (id != null) {
                consider(sheet.byId.get(id), node, s);
                if (sheet.ancestorIds.contains(id)) {
                    signature = signature * 31 + id.hashCode();
                }
            }
            for (int c = 0; c < classes.size(); c++) {
                String cls = classes.get(c);
                consider(sheet.byClass.get(cls), node, s);
                if (sheet.ancestorClasses.contains(cls)) {
                    signature = signature * 31 + cls.hashCode();
                }
            }
            consider(sheet.byType.get(type), node, s);
            consider(sheet.universal, node, s);
            for (int p = 0; p < sheet.ancestorPseudos.length; p++) {
                if (node.cn1HasPseudoClass(sheet.ancestorPseudos[p])) {
                    signature = signature * 31 + (p + 1) * 7919L + s;
                }
            }
        }
        scope.clear();
        if (!inlineText.equals(state.inlineText)) {
            state.inlineText = inlineText;
            state.inline = parseInline(inlineText);
        }
        for (int i = 0; i < state.inline.length; i++) {
            CssSheet.Decl d = state.inline[i];
            offer(d, d.important ? INLINE | IMPORTANT | i : INLINE | i);
        }

        // 2. The colours this node defines, which its own values may use.
        boolean changed = updateLookups(state, above);

        // 3. The font, which an em length below is relative to.
        changed |= updateFont(state, above);

        if (signature != state.signature) {
            state.signature = signature;
            changed = true;
        }

        // 4. The values.
        for (int i = 0; i < touchedCount; i++) {
            int property = touched[i];
            CssSheet.Decl d = winner[property];
            winner[property] = null;
            if (d == null || d.kind == CssProperties.LOOKUP_DEFINITION || CssProperties.isFont(d.name)) {
                wanted[property] = null;
            } else {
                wanted[property] = decode(d, node, state);
            }
        }
        want(FONT_FAMILY, state.family);
        want(FONT_SIZE, state.size == state.size ? Double.valueOf(state.size) : null);
        want(FONT_WEIGHT, state.weight);
        want(FONT_STYLE, state.posture);
        apply(node, state);
        return changed && !fresh;
    }

    private void want(int property, Object value) {
        if (stamp[property] != generation) {
            if (value == null) {
                return;
            }
            stamp[property] = generation;
            touched[touchedCount++] = property;
        }
        wanted[property] = value;
    }

    private void apply(Node node, State state) {
        // Withdrawing one font name restores the whole font, so when one
        // goes they all go and the ones still wanted are applied again.
        boolean fontGoes = false;
        for (int i = 0; i < state.count; i++) {
            int property = state.ids[i];
            if (isFontPart(property) && (stamp[property] != generation || wanted[property] == null)) {
                fontGoes = true;
            }
        }
        for (int i = state.count - 1; i >= 0; i--) {
            int property = state.ids[i];
            boolean keep = stamp[property] == generation && wanted[property] != null;
            if (!keep || (fontGoes && isFontPart(property))) {
                node.cn1ApplyStyle(PROPERTY_NAMES.get(property), null);
                state.remove(i);
            }
        }
        for (int i = 0; i < touchedCount; i++) {
            int property = touched[i];
            Object value = wanted[property];
            wanted[property] = null;
            if (value == null) {
                continue;
            }
            int at = state.indexOf(property);
            if (at < 0) {
                node.cn1ApplyStyle(PROPERTY_NAMES.get(property), value);
                state.add(property, value);
            } else if (!value.equals(state.values[at])) {
                node.cn1ApplyStyle(PROPERTY_NAMES.get(property), value);
                state.values[at] = value;
            }
        }
        touchedCount = 0;
    }

    private static boolean isFontPart(int property) {
        return property == FONT_FAMILY || property == FONT_SIZE || property == FONT_WEIGHT || property == FONT_STYLE;
    }

    // -------------------------------------------------------- the cascade

    private void collectScope(Node node) {
        scope.clear();
        Scene scene = node.getScene();
        if (scene != null) {
            ObservableList<String> list = scene.getStylesheets();
            for (int i = 0; i < list.size(); i++) {
                CssSheet s = sheet(list.get(i));
                if (s != null) {
                    scope.add(s);
                }
            }
        }
        if (!parentSheets) {
            return;
        }
        chain.clear();
        Parent p = node instanceof Parent ? (Parent) node : node.getParent();
        while (p != null) {
            if (p.cn1HasStylesheets()) {
                chain.add(p);
            }
            p = p.getParent();
        }
        for (int c = chain.size() - 1; c >= 0; c--) {
            ObservableList<String> list = chain.get(c).getStylesheets();
            for (int i = 0; i < list.size(); i++) {
                CssSheet s = sheet(list.get(i));
                if (s != null) {
                    scope.add(s);
                }
            }
        }
        chain.clear();
    }

    private String typeName(Node node) {
        Class<?> c = node.getClass();
        String name = typeNames.get(c);
        if (name == null) {
            name = node.getTypeSelector();
            typeNames.put(c, name);
        }
        return name;
    }

    private void consider(CssSheet.Rule[] rules, Node node, int sheetIndex) {
        if (rules == null) {
            return;
        }
        for (int r = 0; r < rules.length; r++) {
            CssSheet.Rule rule = rules[r];
            if (!compound(rule, 0, node) || !above(rule, 1, node)) {
                continue;
            }
            long base = ((long) rule.selector.specificity() << 32) | ((long) (sheetIndex & 0xfff) << 20)
                    | (rule.order & 0xfffff);
            for (int i = 0; i < rule.decls.length; i++) {
                CssSheet.Decl d = rule.decls[i];
                offer(d, d.important ? base | IMPORTANT : base);
            }
        }
    }

    private void offer(CssSheet.Decl d, long p) {
        int property = d.id;
        if (property >= stamp.length) {
            int size = Math.max(property + 1, stamp.length * 2);
            CssSheet.Decl[] w = new CssSheet.Decl[size];
            long[] pr = new long[size];
            int[] st = new int[size];
            Object[] wa = new Object[size];
            int[] to = new int[size];
            System.arraycopy(winner, 0, w, 0, winner.length);
            System.arraycopy(priority, 0, pr, 0, priority.length);
            System.arraycopy(stamp, 0, st, 0, stamp.length);
            System.arraycopy(wanted, 0, wa, 0, wanted.length);
            System.arraycopy(touched, 0, to, 0, touched.length);
            winner = w;
            priority = pr;
            stamp = st;
            wanted = wa;
            touched = to;
        }
        if (stamp[property] != generation) {
            stamp[property] = generation;
            touched[touchedCount++] = property;
            winner[property] = d;
            priority[property] = p;
        } else if (winner[property] == null || p >= priority[property]) {
            winner[property] = d;
            priority[property] = p;
        }
    }

    /// Whether `node` is what compound `i` of the rule's selector asks for.
    private boolean compound(CssSheet.Rule rule, int i, Node node) {
        CssSelector s = rule.selector;
        String type = s.type(i);
        if (type != null && !type.equals(typeName(node))) {
            return false;
        }
        String id = s.id(i);
        if (id != null && !id.equals(node.getId())) {
            return false;
        }
        int n = s.classCount(i);
        if (n > 0) {
            ObservableList<String> classes = node.getStyleClass();
            int have = classes.size();
            for (int j = 0; j < n; j++) {
                String wantedClass = s.styleClass(i, j);
                boolean found = false;
                for (int k = 0; k < have && !found; k++) {
                    found = wantedClass.equals(classes.get(k));
                }
                if (!found) {
                    return false;
                }
            }
        }
        javafx.css.PseudoClass[] states = rule.pseudo[i];
        for (int j = 0; j < states.length; j++) {
            if (!node.cn1HasPseudoClass(states[j])) {
                return false;
            }
        }
        return true;
    }

    /// Whether the compounds from `i` on are satisfied by what is above
    /// `node`.
    private boolean above(CssSheet.Rule rule, int i, Node node) {
        if (i >= rule.selector.size()) {
            return true;
        }
        Parent p = node.getParent();
        if (rule.selector.combinator(i - 1) == CssSelector.CHILD) {
            return p != null && compound(rule, i, p) && above(rule, i + 1, p);
        }
        while (p != null) {
            if (compound(rule, i, p) && above(rule, i + 1, p)) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /// The inline style being parsed, for the message of a warning.
    private String inlineText;

    /// One reporter for every inline style, rather than one per parse.
    private final CssDeclarations.Reporter inlineReporter = new CssDeclarations.Reporter() {
        @Override
        public void warning(int offset, String message) {
            warnOnce("In the inline style \"" + inlineText + "\": " + message);
        }
    };

    private CssSheet.Decl[] parseInline(String text) {
        if (text.trim().length() == 0) {
            return NO_DECLS;
        }
        parsed.clear();
        String clean = CssDeclarations.stripComments(text);
        inlineText = text;
        CssDeclarations.parse(clean, 0, clean.length(), parser, inlineReporter, parsed);
        inlineText = null;
        CssSheet.Decl[] out = new CssSheet.Decl[parsed.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = new CssSheet.Decl(parsed.get(i));
        }
        parsed.clear();
        return out;
    }

    // ------------------------------------------------- inherited: colours

    private boolean updateLookups(State state, State above) {
        int n = 0;
        boolean same = true;
        for (int i = 0; i < touchedCount; i++) {
            CssSheet.Decl d = winner[touched[i]];
            if (d != null && d.kind == CssProperties.LOOKUP_DEFINITION) {
                n++;
                int at = -1;
                for (int j = 0; state.lookupNames != null && j < state.lookupNames.length && at < 0; j++) {
                    if (state.lookupNames[j].equals(d.name)) {
                        at = j;
                    }
                }
                same &= at >= 0 && state.lookupValues[at].equals(d.value);
            }
        }
        same &= n == (state.lookupNames == null ? 0 : state.lookupNames.length);
        if (!same) {
            if (n == 0) {
                state.lookupNames = null;
                state.lookupValues = null;
            } else {
                state.lookupNames = new String[n];
                state.lookupValues = new CssValue[n];
                int at = 0;
                for (int i = 0; i < touchedCount; i++) {
                    CssSheet.Decl d = winner[touched[i]];
                    if (d != null && d.kind == CssProperties.LOOKUP_DEFINITION) {
                        state.lookupNames[at] = d.name;
                        state.lookupValues[at++] = d.value;
                    }
                }
            }
        }
        boolean chainNow = state.lookupNames != null || (above != null && above.lookupsAbove);
        boolean changed = !same || chainNow != state.lookupsAbove;
        state.lookupsAbove = chainNow;
        return changed;
    }

    /// The value a name is defined as at `node` or the nearest ancestor
    /// that defines it.
    private static CssValue lookup(String name, Node node) {
        Node at = node;
        while (at != null) {
            State s = stateOf(at);
            if (s == null || !s.lookupsAbove) {
                return null;
            }
            if (s.lookupNames != null) {
                for (int i = 0; i < s.lookupNames.length; i++) {
                    if (s.lookupNames[i].equals(name)) {
                        return s.lookupValues[i];
                    }
                }
            }
            at = at.getParent();
        }
        return null;
    }

    // ---------------------------------------------------- inherited: font

    private CssSheet.Decl won(int property) {
        return property < stamp.length && stamp[property] == generation ? winner[property] : null;
    }

    private boolean updateFont(State state, State above) {
        String family = above == null ? null : above.family;
        double size = above == null ? Double.NaN : above.size;
        Object weight = above == null ? null : above.weight;
        String posture = above == null ? null : above.posture;
        double base = size == size ? size : Font.getDefault().getSize();

        CssSheet.Decl font = won(FONT);
        if (font != null && font.value.type() == CssValue.FONT) {
            CssValue v = font.value;
            family = v.text();
            size = fontSize(v, base);
            int w = v.flags() & 0xffff;
            weight = w == 0 ? (Object) "normal" : (Object) Integer.valueOf(w);
            posture = (v.flags() >> 16) == 2 ? "italic" : "normal";
        }
        CssSheet.Decl d = won(FONT_FAMILY);
        if (d != null && d.value.text() != null) {
            family = d.value.text();
        }
        d = won(FONT_SIZE);
        if (d != null && d.value.count() > 0) {
            size = fontSize(d.value, base);
        }
        d = won(FONT_WEIGHT);
        if (d != null) {
            if (d.value.type() == CssValue.NUMBER) {
                weight = Integer.valueOf((int) d.value.num(0));
            } else if (d.value.text() != null) {
                weight = d.value.text();
            }
        }
        d = won(FONT_STYLE);
        if (d != null && d.value.text() != null) {
            posture = d.value.text();
        }
        boolean changed = !same(family, state.family) || !same(weight, state.weight) || !same(posture, state.posture)
                || Double.doubleToLongBits(size) != Double.doubleToLongBits(state.size);
        state.family = family;
        state.size = size;
        state.weight = weight;
        state.posture = posture;
        return changed;
    }

    private static boolean same(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static double fontSize(CssValue v, double inherited) {
        double n = v.num(0);
        switch (v.unit(0)) {
            case CssValue.UNIT_EM:
                return n * inherited;
            case CssValue.UNIT_PERCENT:
                return n * inherited / 100;
            default:
                return n;
        }
    }

    // --------------------------------------------------------- the values

    /// The object the node takes for a declaration, or `null` when the
    /// declaration cannot be applied here: a colour name nothing defines.
    private Object decode(CssSheet.Decl d, Node node, State state) {
        if (d.decoded != null) {
            return d.decoded;
        }
        double em = state.size == state.size ? state.size : Font.getDefault().getSize();
        Object out = decode(d.kind, d.name, d.value, node, em);
        if (out != null && !d.value.isContextual()) {
            d.decoded = out;
        }
        return out;
    }

    private static Object decode(int kind, String name, CssValue v, Node node, double em) {
        switch (kind) {
            case CssProperties.PAINT:
                return paint(v, node, 0);
            case CssProperties.LENGTH:
                return v.count() == 0 || v.unit(0) == CssValue.UNIT_PERCENT ? null
                        : (Object) Double.valueOf(length(v, 0, em));
            case CssProperties.NUMBER:
                return v.count() == 0 ? null : (Object) Double.valueOf(v.num(0));
            case CssProperties.INSETS: {
                int n = v.count();
                if (n == 0) {
                    return null;
                }
                double top = length(v, 0, em);
                double right = n > 1 ? length(v, 1, em) : top;
                double bottom = n > 2 ? length(v, 2, em) : top;
                double left = n > 3 ? length(v, 3, em) : right;
                return new Insets(top, right, bottom, left);
            }
            case CssProperties.RADII: {
                int n = v.count();
                if (n == 0) {
                    return null;
                }
                double tl = length(v, 0, em);
                double tr = n > 1 ? length(v, 1, em) : tl;
                double br = n > 2 ? length(v, 2, em) : tl;
                double bl = n > 3 ? length(v, 3, em) : tr;
                return new CornerRadii(Math.max(0, tl), Math.max(0, tr), Math.max(0, br), Math.max(0, bl),
                        v.unit(0) == CssValue.UNIT_PERCENT);
            }
            case CssProperties.BOOLEAN:
                return v.flags() != 0 ? Boolean.TRUE : Boolean.FALSE;
            case CssProperties.KEYWORD:
            case CssProperties.TEXT:
                return v.text();
            case CssProperties.POS:
                return v.text() == null ? null : POSITIONS.get(v.text());
            case CssProperties.CURSOR:
                try {
                    return v.text() == null ? null : Cursor.cursor(v.text());
                } catch (IllegalArgumentException e) {
                    warnOnce("Unknown cursor '" + v.text() + "' for " + name);
                    return null;
                }
            default:
                return null;
        }
    }

    private static double length(CssValue v, int i, double em) {
        double n = v.num(i);
        if (v.unit(i) == CssValue.UNIT_EM) {
            return n * em;
        }
        if (v.unit(i) == CssValue.UNIT_PERCENT) {
            // A percentage radius is kept as written; the radii say so.
            return n / 100;
        }
        return n;
    }

    private static Object paint(CssValue v, Node node, int depth) {
        if (v == null || depth > 8) {
            return null;
        }
        switch (v.type()) {
            case CssValue.COLOR:
            case CssValue.LOOKUP:
            case CssValue.DERIVE:
                return color(v, node, depth);
            case CssValue.LINEAR: {
                List<Stop> stops = stops(v, 4, node, depth);
                if (stops == null) {
                    return null;
                }
                return new LinearGradient(v.num(0), v.num(1), v.num(2), v.num(3),
                        (v.flags() & CssValue.FLAG_PROPORTIONAL) != 0, cycle(v), stops);
            }
            case CssValue.RADIAL: {
                List<Stop> stops = stops(v, 5, node, depth);
                if (stops == null) {
                    return null;
                }
                return new RadialGradient(v.num(0), v.num(1), v.num(2), v.num(3), v.num(4),
                        (v.flags() & CssValue.FLAG_PROPORTIONAL) != 0, cycle(v), stops);
            }
            default:
                return null;
        }
    }

    private static CycleMethod cycle(CssValue v) {
        if ((v.flags() & CssValue.FLAG_REFLECT) != 0) {
            return CycleMethod.REFLECT;
        }
        return (v.flags() & CssValue.FLAG_REPEAT) != 0 ? CycleMethod.REPEAT : CycleMethod.NO_CYCLE;
    }

    private static List<Stop> stops(CssValue v, int from, Node node, int depth) {
        int n = v.partCount();
        if (n == 0 || v.count() < from + n) {
            return null;
        }
        double[] offsets = new double[n];
        for (int i = 0; i < n; i++) {
            offsets[i] = v.num(from + i);
        }
        // A stop without an offset sits evenly between its neighbours.
        if (offsets[0] != offsets[0]) {
            offsets[0] = 0;
        }
        if (offsets[n - 1] != offsets[n - 1]) {
            offsets[n - 1] = 1;
        }
        int known = 0;
        for (int i = 1; i < n; i++) {
            if (offsets[i] == offsets[i]) {
                int gap = i - known;
                for (int j = known + 1; j < i; j++) {
                    offsets[j] = offsets[known] + (offsets[i] - offsets[known]) * (j - known) / gap;
                }
                known = i;
            }
        }
        ArrayList<Stop> out = new ArrayList<Stop>(n);
        for (int i = 0; i < n; i++) {
            Color c = color(v.part(i), node, depth + 1);
            if (c == null) {
                return null;
            }
            out.add(new Stop(offsets[i], c));
        }
        return out;
    }

    private static Color color(CssValue v, Node node, int depth) {
        if (v == null || depth > 8) {
            return null;
        }
        switch (v.type()) {
            case CssValue.COLOR: {
                int argb = v.flags();
                return Color.rgb((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, ((argb >>> 24) & 0xff) / 255.0);
            }
            case CssValue.LOOKUP: {
                CssValue defined = lookup(v.text(), node);
                if (defined == null) {
                    warnOnce("The colour '" + v.text() + "' is used but no rule defines it");
                    return null;
                }
                Object p = paint(defined, node, depth + 1);
                return p instanceof Color ? (Color) p : null;
            }
            case CssValue.DERIVE: {
                Color c = v.partCount() == 0 ? null : color(v.part(0), node, depth + 1);
                return c == null || v.count() == 0 ? null : derive(c, v.num(0) / 100);
            }
            default:
                return null;
        }
    }

    /// JavaFX's `derive()`: a brightness change that is scaled by how
    /// bright the colour already is, so that one percentage reads the same
    /// on a light and on a dark base.
    private static Color derive(Color c, double brightness) {
        double base = 0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue();
        double by = brightness;
        if (by > 0) {
            if (base > 0.85) {
                by = by * 1.6;
            } else if (base > 0.6) {
                by = by * 1.0;
            } else if (base > 0.5) {
                by = by * 0.9;
            } else if (base > 0.4) {
                by = by * 0.8;
            } else if (base > 0.3) {
                by = by * 0.7;
            } else {
                by = by * 0.6;
            }
        } else if (base < 0.2) {
            by = by * 0.6;
        }
        by = Math.max(-1, Math.min(1, by));
        double h = c.getHue();
        double s = c.getSaturation();
        double b = c.getBrightness();
        if (by > 0) {
            s = s * (1 - by);
            b = b + (1 - b) * by;
        } else {
            b = b * (by + 1);
        }
        return Color.hsb(h, Math.max(0, Math.min(1, s)), Math.max(0, Math.min(1, b)), c.getOpacity());
    }
}
