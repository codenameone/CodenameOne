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
package com.codename1.presentation;

import com.codename1.impl.xml.XmlOut;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The `p:timing` of a slide for the PPTX writer: a main sequence with one click group per
/// on-click build, after-previous builds as timed sub-groups and with-previous builds beside
/// the build before them. Entrance and exit builds are written; emphasis has no counterpart
/// in the model's effects and is left out.
final class PptxTiming {
    private final XmlOut x;
    private int nextId = 3;

    private PptxTiming(XmlOut x) {
        this.x = x;
    }

    static void write(XmlOut x, List<Build> builds, Map<String, Integer> ids,
            Map<String, Boolean> text) throws IOException {
        if (builds == null) {
            return;
        }
        List<Build> usable = new ArrayList<Build>();
        for (Build b : builds) {
            boolean kind = Build.ENTRANCE.equals(b.kind) || Build.EXIT.equals(b.kind);
            if (kind && b.elementId != null && ids.containsKey(b.elementId)) {
                usable.add(b);
            }
        }
        if (usable.isEmpty()) {
            return;
        }
        new PptxTiming(x).sequence(usable, ids);
        List<String> listed = new ArrayList<String>();
        x.raw("<p:bldLst>");
        for (Build b : usable) {
            if (text.containsKey(b.elementId) && !listed.contains(b.elementId)) {
                listed.add(b.elementId);
                x.raw("<p:bldP").attr("spid", ids.get(b.elementId).toString())
                        .attr("grpId", "0").attr("animBg", "1").raw("/>");
            }
        }
        x.raw("</p:bldLst></p:timing>");
    }

    private void sequence(List<Build> builds, Map<String, Integer> ids) throws IOException {
        x.raw("<p:timing><p:tnLst><p:par><p:cTn id=\"1\" dur=\"indefinite\" restart=\"never\""
                + " nodeType=\"tmRoot\"><p:childTnLst><p:seq concurrent=\"1\" nextAc=\"seek\">"
                + "<p:cTn id=\"2\" dur=\"indefinite\" nodeType=\"mainSeq\"><p:childTnLst>");
        boolean clickOpen = false;
        boolean groupOpen = false;
        long groupStart = 0;
        long groupLength = 0;
        for (Build b : builds) {
            String trigger = b.trigger == null ? Build.ON_CLICK : b.trigger;
            boolean click = Build.ON_CLICK.equals(trigger) || !clickOpen;
            boolean after = Build.AFTER_PREVIOUS.equals(trigger);
            String node;
            if (click) {
                if (groupOpen) {
                    x.raw("</p:childTnLst></p:cTn></p:par>");
                }
                if (clickOpen) {
                    x.raw("</p:childTnLst></p:cTn></p:par>");
                }
                x.raw("<p:par><p:cTn").attr("id", nextId++).attr("fill", "hold")
                        .raw("><p:stCondLst><p:cond delay=\"indefinite\"/>");
                if (!Build.ON_CLICK.equals(trigger)) {
                    x.raw("<p:cond evt=\"onBegin\" delay=\"0\"><p:tn val=\"2\"/></p:cond>");
                }
                x.raw("</p:stCondLst><p:childTnLst>");
                clickOpen = true;
                groupOpen = false;
                groupStart = 0;
                groupLength = 0;
                node = Build.ON_CLICK.equals(trigger) ? "clickEffect"
                        : after ? "afterEffect" : "withEffect";
            } else if (after) {
                groupStart += groupLength;
                groupLength = 0;
                if (groupOpen) {
                    x.raw("</p:childTnLst></p:cTn></p:par>");
                    groupOpen = false;
                }
                node = "afterEffect";
            } else {
                node = "withEffect";
            }
            if (!groupOpen) {
                x.raw("<p:par><p:cTn").attr("id", nextId++).attr("fill", "hold")
                        .raw("><p:stCondLst><p:cond").attr("delay", String.valueOf(groupStart))
                        .raw("/></p:stCondLst><p:childTnLst>");
                groupOpen = true;
            }
            int delay = Math.max(0, b.delay);
            int dur = Math.max(1, b.duration);
            groupLength = Math.max(groupLength, delay + (long) dur);
            effect(b, ids.get(b.elementId).toString(), node, delay, dur);
        }
        if (groupOpen) {
            x.raw("</p:childTnLst></p:cTn></p:par>");
        }
        if (clickOpen) {
            x.raw("</p:childTnLst></p:cTn></p:par>");
        }
        x.raw("</p:childTnLst></p:cTn><p:prevCondLst><p:cond evt=\"onPrev\" delay=\"0\">"
                + "<p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst><p:nextCondLst>"
                + "<p:cond evt=\"onNext\" delay=\"0\"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond>"
                + "</p:nextCondLst></p:seq></p:childTnLst></p:cTn></p:par></p:tnLst>");
    }

    private static int preset(String effect) {
        if (Build.FADE.equals(effect)) {
            return 10;
        }
        if (Build.FLY.equals(effect)) {
            return 2;
        }
        if (Build.ZOOM.equals(effect)) {
            return 53;
        }
        return Build.WIPE.equals(effect) ? 22 : 1;
    }

    private static int subtype(Build b) {
        if (!Build.FLY.equals(b.effect) && !Build.WIPE.equals(b.effect)) {
            return 0;
        }
        if (Transition.FROM_LEFT.equals(b.direction)) {
            return 8;
        }
        if (Transition.FROM_RIGHT.equals(b.direction)) {
            return 2;
        }
        return Transition.FROM_TOP.equals(b.direction) ? 1 : 4;
    }

    private void target(String spid) throws IOException {
        x.raw("<p:tgtEl><p:spTgt").attr("spid", spid).raw("/></p:tgtEl>");
    }

    private void effect(Build b, String spid, String node, int delay, int dur) throws IOException {
        boolean exit = Build.EXIT.equals(b.kind);
        x.raw("<p:par><p:cTn").attr("id", nextId++).attr("presetID", preset(b.effect))
                .attr("presetClass", exit ? "exit" : "entr").attr("presetSubtype", subtype(b))
                .attr("fill", "hold").attr("grpId", "0").attr("nodeType", node)
                .raw("><p:stCondLst><p:cond").attr("delay", delay).raw("/></p:stCondLst>"
                        + "<p:childTnLst>");
        if (!exit) {
            visibility(spid, "visible", 0);
        }
        if (Build.FADE.equals(b.effect) || Build.ZOOM.equals(b.effect)) {
            animEffect(spid, exit, "fade", dur);
        } else if (Build.WIPE.equals(b.effect)) {
            animEffect(spid, exit, "wipe(" + wipe(subtype(b)) + ")", dur);
        } else if (Build.FLY.equals(b.effect)) {
            fly(spid, exit, subtype(b), dur);
        }
        if (exit) {
            visibility(spid, "hidden", Build.APPEAR.equals(b.effect) ? 0 : dur - 1);
        }
        x.raw("</p:childTnLst></p:cTn></p:par>");
    }

    private static String wipe(int subtype) {
        if (subtype == 8) {
            return "left";
        }
        if (subtype == 2) {
            return "right";
        }
        return subtype == 1 ? "up" : "down";
    }

    private void visibility(String spid, String value, int at) throws IOException {
        x.raw("<p:set><p:cBhvr><p:cTn").attr("id", nextId++).attr("dur", "1")
                .attr("fill", "hold").raw("><p:stCondLst><p:cond")
                .attr("delay", Math.max(0, at)).raw("/></p:stCondLst></p:cTn>");
        target(spid);
        x.raw("<p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst>"
                + "</p:cBhvr><p:to><p:strVal").attr("val", value).raw("/></p:to></p:set>");
    }

    private void animEffect(String spid, boolean exit, String filter, int dur)
            throws IOException {
        x.raw("<p:animEffect").attr("transition", exit ? "out" : "in").attr("filter", filter)
                .raw("><p:cBhvr><p:cTn").attr("id", nextId++).attr("dur", dur).raw("/>");
        target(spid);
        x.raw("</p:cBhvr></p:animEffect>");
    }

    private void fly(String spid, boolean exit, int subtype, int dur) throws IOException {
        boolean horizontal = subtype == 8 || subtype == 2;
        String attr = horizontal ? "ppt_x" : "ppt_y";
        String off;
        if (subtype == 8) {
            off = "0-#ppt_w/2";
        } else if (subtype == 2) {
            off = "1+#ppt_w/2";
        } else if (subtype == 1) {
            off = "0-#ppt_h/2";
        } else {
            off = "1+#ppt_h/2";
        }
        String home = "#" + attr;
        x.raw("<p:anim calcmode=\"lin\" valueType=\"num\"><p:cBhvr additive=\"base\"><p:cTn")
                .attr("id", nextId++).attr("dur", dur).attr("fill", "hold").raw("/>");
        target(spid);
        x.raw("<p:attrNameLst><p:attrName>").raw(attr).raw("</p:attrName></p:attrNameLst>"
                + "</p:cBhvr><p:tavLst><p:tav tm=\"0\"><p:val><p:strVal")
                .attr("val", exit ? home : off)
                .raw("/></p:val></p:tav><p:tav tm=\"100000\"><p:val><p:strVal")
                .attr("val", exit ? off : home).raw("/></p:val></p:tav></p:tavLst></p:anim>");
    }
}
