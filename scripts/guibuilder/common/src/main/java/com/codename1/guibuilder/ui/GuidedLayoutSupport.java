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

package com.codename1.guibuilder.ui;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.xml.Element;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the persistent, name-based constraints used by the Guided Layout designer.
 * LayeredLayout's native reference serialization is index based; names remain stable when
 * components are reordered, so the builder stores names and resolves them at preview/runtime
 * generation time.
 */
public final class GuidedLayoutSupport {
    public static final String PREFERRED = "preferred";
    public static final String FIXED = "fixed";
    public static final String FILL = "fill";
    public static final String MATCH = "match";

    private GuidedLayoutSupport() { }

    public static void apply(Element parentElement, Container parent) {
        if (!(parent.getLayout() instanceof LayeredLayout)) return;
        LayeredLayout layered = (LayeredLayout) parent.getLayout();
        List<Element> elements = componentChildren(parentElement);
        for (int i = 0; i < elements.size() && i < parent.getComponentCount(); i++) {
            Element element = elements.get(i);
            Component component = parent.getComponentAt(i);
            layered.setInsets(component, value(element, "layeredInsets", "auto auto auto auto"));
            String references = value(element, "guidedReferences", "-|-|-|-");
            String[] names = four(references, "-");
            Component[] refs = new Component[4];
            for (int side = 0; side < refs.length; side++) refs[side] = namedChild(parent, names[side]);
            try {
                layered.setReferenceComponents(component, refs);
                layered.setReferencePositions(component, value(element, "guidedReferencePositions", "0 0 0 0"));
                layered.setPercentInsetAnchorHorizontal(component, decimal(element, "guidedHorizontalAnchor", 0f));
                layered.setPercentInsetAnchorVertical(component, decimal(element, "guidedVerticalAnchor", 0f));
            } catch (IllegalArgumentException circularReference) {
                // A malformed hand-edited file must remain designable.  Ignore only its references;
                // the inspector can then be used to repair the relationship.
                layered.setReferenceComponents(component, new Component[]{null, null, null, null});
            }
        }
    }

    public static String horizontalPolicy(Element element) {
        String explicit = element.getAttribute("guidedHorizontalSize");
        if (validPolicy(explicit)) return explicit;
        String[] insets = insetValues(element);
        return !"auto".equals(insets[1]) && !"auto".equals(insets[3]) ? FIXED : PREFERRED;
    }

    public static String verticalPolicy(Element element) {
        String explicit = element.getAttribute("guidedVerticalSize");
        if (validPolicy(explicit)) return explicit;
        String[] insets = insetValues(element);
        return !"auto".equals(insets[0]) && !"auto".equals(insets[2]) ? FIXED : PREFERRED;
    }

    public static String[] insetValues(Element element) {
        return cssFour(value(element, "layeredInsets", "auto auto auto auto"), "auto");
    }

    public static String[] referenceNames(Element element) {
        return four(value(element, "guidedReferences", "-|-|-|-"), "-");
    }

    public static String[] referencePositions(Element element) {
        return cssFour(value(element, "guidedReferencePositions", "0 0 0 0"), "0");
    }

    public static String joinInsets(String top, String right, String bottom, String left) {
        return top + " " + right + " " + bottom + " " + left;
    }

    public static String joinReferences(String top, String right, String bottom, String left) {
        return cleanReference(top) + "|" + cleanReference(right) + "|"
                + cleanReference(bottom) + "|" + cleanReference(left);
    }

    public static String joinPositions(String top, String right, String bottom, String left) {
        return top + " " + right + " " + bottom + " " + left;
    }

    private static Component namedChild(Container parent, String name) {
        if (name == null || name.length() == 0 || "-".equals(name)) return null;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component child = parent.getComponentAt(i);
            Object value = child.getClientProperty("gui.element");
            if (value instanceof Element && name.equals(((Element) value).getAttribute("name"))) return child;
        }
        return null;
    }

    private static List<Element> componentChildren(Element parent) {
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < parent.getNumChildren(); i++) {
            Object child = parent.getChildAt(i);
            if (child instanceof Element && "component".equals(((Element) child).getTagName())) out.add(((Element) child));
        }
        return out;
    }

    private static String[] four(String value, String fallback) {
        String[] split = value == null ? new String[0] : value.split("\\|", -1);
        String[] out = {fallback, fallback, fallback, fallback};
        for (int i = 0; i < out.length && i < split.length; i++) {
            if (split[i] != null && split[i].trim().length() > 0) out[i] = split[i].trim();
        }
        return out;
    }

    private static String[] cssFour(String value, String fallback) {
        String[] split = value == null ? new String[0] : value.trim().split("\\s+");
        if (split.length == 1) return new String[]{split[0], split[0], split[0], split[0]};
        if (split.length == 2) return new String[]{split[0], split[1], split[0], split[1]};
        if (split.length == 3) return new String[]{split[0], split[1], split[2], split[1]};
        if (split.length >= 4) return new String[]{split[0], split[1], split[2], split[3]};
        return new String[]{fallback, fallback, fallback, fallback};
    }

    private static boolean validPolicy(String value) {
        return PREFERRED.equals(value) || FIXED.equals(value) || FILL.equals(value) || MATCH.equals(value);
    }

    private static String cleanReference(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value.trim();
    }

    private static String value(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value == null || value.length() == 0 ? fallback : value;
    }

    private static float decimal(Element element, String name, float fallback) {
        try { return Float.parseFloat(value(element, name, String.valueOf(fallback))); }
        catch (NumberFormatException ex) { return fallback; }
    }
}
