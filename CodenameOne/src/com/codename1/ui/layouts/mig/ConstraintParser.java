package com.codename1.ui.layouts.mig;

import com.codename1.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

/**
 * Parses string constraints.
 */
public final class ConstraintParser {

    private ConstraintParser() {
    }

    /**
     * Parses the layout constraints and stores the parsed values in the
     * transient (cache) member varables.
     *
     * @param s The String to parse. Should not be <code>null</code> and <b>must
     * be lower case and trimmed</b>.
     * @throws RuntimeException if the constaint was not valid.
     * @return The parsed constraint. Never <code>null</code>.
     */
    public static LC parseLayoutConstraint(String s) {
        LC lc = new LC();
        if (s.length() == 0) {
            return lc;
        }

        String[] parts = toTrimmedTokens(s, ',');

        // First check for "ltr" or "rtl" since that will affect the interpretation of the other constraints.
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part == null) {
                continue;
            }

            int len = part.length();
            if (len == 3 || len == 11) {   // Optimization
                if (part.equals("ltr") || part.equals("rtl") || part.equals("lefttoright") || part.equals("righttoleft")) {
                    lc.setLeftToRight(part.charAt(0) == 'l' ? Boolean.TRUE : Boolean.FALSE);
                    parts[i] = null;    // So we will not try to interpret it again
                }

                if (part.equals("ttb") || part.equals("btt") || part.equals("toptobottom") || part.equals("bottomtotop")) {
                    lc.setTopToBottom(part.charAt(0) == 't');
                    parts[i] = null;    // So we will not try to interpret it again
                }
            }
        }

        for (String part : parts) {
            if (part == null || part.length() == 0) {
                continue;
            }

            try {
                int ix = -1;
                char c = part.charAt(0);

                if (c == 'w' || c == 'h') {

                    ix = startsWithLenient(part, "wrap", -1, true);
                    if (ix > -1) {
                        String num = part.substring(ix).trim();
                        lc.setWrapAfter(num.length() != 0 ? Integer.parseInt(num) : 0);
                        continue;
                    }

                    boolean isHor = c == 'w';
                    if (isHor && (part.startsWith("w ") || part.startsWith("width "))) {
                        String sz = part.substring(part.charAt(1) == ' ' ? 2 : 6).trim();
                        lc.setWidth(parseBoundSize(sz, false, true));
                        continue;
                    }

                    if (!isHor && (part.startsWith("h ") || part.startsWith("height "))) {
                        String uvStr = part.substring(part.charAt(1) == ' ' ? 2 : 7).trim();
                        lc.setHeight(parseBoundSize(uvStr, false, false));
                        continue;
                    }

                    if (part.length() > 5) {
                        String sz = part.substring(5).trim();
                        if (part.startsWith("wmin ")) {
                            lc.minWidth(sz);
                            continue;
                        } else if (part.startsWith("wmax ")) {
                            lc.maxWidth(sz);
                            continue;
                        } else if (part.startsWith("hmin ")) {
                            lc.minHeight(sz);
                            continue;
                        } else if (part.startsWith("hmax ")) {
                            lc.maxHeight(sz);
                            continue;
                        }
                    }

                    if (part.startsWith("hidemode ")) {
                        lc.setHideMode(Integer.parseInt(part.substring(9)));
                        continue;
                    }
                }

                if (c == 'g') {
                    if (part.startsWith("gapx ")) {
                        lc.setGridGapX(parseBoundSize(part.substring(5).trim(), true, true));
                        continue;
                    }

                    if (part.startsWith("gapy ")) {
                        lc.setGridGapY(parseBoundSize(part.substring(5).trim(), true, false));
                        continue;
                    }

                    if (part.startsWith("gap ")) {
                        String[] gaps = toTrimmedTokens(part.substring(4).trim(), ' ');
                        lc.setGridGapX(parseBoundSize(gaps[0], true, true));
                        lc.setGridGapY(gaps.length > 1 ? parseBoundSize(gaps[1], true, false) : lc.getGridGapX());
                        continue;
                    }
                }

                if (c == 'd') {
                    ix = startsWithLenient(part, "debug", 5, true);
                    if (ix > -1) {
                        String millis = part.substring(ix).trim();
                        lc.setDebugMillis(millis.length() > 0 ? Integer.parseInt(millis) : 1000);
                        continue;
                    }
                }

                if (c == 'n') {
                    if (part.equals("nogrid")) {
                        lc.setNoGrid(true);
                        continue;
                    }

                    if (part.equals("nocache")) {
                        lc.setNoCache(true);
                        continue;
                    }

                    if (part.equals("novisualpadding")) {
                        lc.setVisualPadding(false);
                        continue;
                    }
                }

                if (c == 'f') {
                    if (part.equals("fill") || part.equals("fillx") || part.equals("filly")) {
                        lc.setFillX(part.length() == 4 || part.charAt(4) == 'x');
                        lc.setFillY(part.length() == 4 || part.charAt(4) == 'y');
                        continue;
                    }

                    if (part.equals("flowy")) {
                        lc.setFlowX(false);
                        continue;
                    }

                    if (part.equals("flowx")) {
                        lc.setFlowX(true); // This is the default but added for consistency
                        continue;
                    }
                }

                if (c == 'i') {
                    ix = startsWithLenient(part, "insets", 3, true);
                    if (ix > -1) {
                        String insStr = part.substring(ix).trim();
                        UnitValue[] ins = parseInsets(insStr, true);
                        LayoutUtil.putCCString(ins, insStr);
                        lc.setInsets(ins);
                        continue;
                    }
                }

                if (c == 'a') {
                    ix = startsWithLenient(part, new String[]{"aligny", "ay"}, new int[]{6, 2}, true);
                    if (ix > -1) {
                        UnitValue align = parseUnitValueOrAlign(part.substring(ix).trim(), false, null);
                        if (align == UnitValue.BASELINE_IDENTITY) {
                            throw new IllegalArgumentException("'baseline' can not be used to align the whole component group.");
                        }
                        lc.setAlignY(align);
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"alignx", "ax"}, new int[]{6, 2}, true);
                    if (ix > -1) {
                        lc.setAlignX(parseUnitValueOrAlign(part.substring(ix).trim(), true, null));
                        continue;
                    }

                    ix = startsWithLenient(part, "align", 2, true);
                    if (ix > -1) {
                        String[] gaps = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        lc.setAlignX(parseUnitValueOrAlign(gaps[0], true, null));
                        if (gaps.length > 1) {
                            lc.setAlignY(parseUnitValueOrAlign(gaps[1], false, null));
                        }
                        continue;
                    }
                }

                if (c == 'p') {
                    if (part.startsWith("packalign ")) {
                        String[] packs = toTrimmedTokens(part.substring(10).trim(), ' ');
                        lc.setPackWidthAlign(packs[0].length() > 0 ? Float.parseFloat(packs[0]) : 0.5f);
                        if (packs.length > 1) {
                            lc.setPackHeightAlign(Float.parseFloat(packs[1]));
                        }
                        continue;
                    }

                    if (part.startsWith("pack ") || part.equals("pack")) {
                        String ps = part.substring(4).trim();
                        String[] packs = toTrimmedTokens(ps.length() > 0 ? ps : "pref pref", ' ');
                        lc.setPackWidth(parseBoundSize(packs[0], false, true));
                        if (packs.length > 1) {
                            lc.setPackHeight(parseBoundSize(packs[1], false, false));
                        }

                        continue;
                    }
                }

                if (lc.getAlignX() == null) {
                    UnitValue alignX = parseAlignKeywords(part, true);
                    if (alignX != null) {
                        lc.setAlignX(alignX);
                        continue;
                    }
                }

                UnitValue alignY = parseAlignKeywords(part, false);
                if (alignY != null) {
                    lc.setAlignY(alignY);
                    continue;
                }

                throw new IllegalArgumentException("Unknown Constraint: '" + part + "'\n");

            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal Constraint: '" + part + "'\n" + ex.getMessage());
            }
        }

//		lc = (LC) serializeTest(lc);
        return lc;
    }

    /**
     * Parses the column or rows constraints. They normally looks something like
     * <code>"[min:pref]rel[10px][]"</code>.
     *
     * @param s The string to parse. Not <code>null</code>.
     * @return An array of {@link DimConstraint}s that is as many are there
     * exist "[...]" sections in the string that is parsed.
     * @throws RuntimeException if the constraint was not valid.
     */
    public static AC parseRowConstraints(String s) {
        return parseAxisConstraint(s, false);
    }

    /**
     * Parses the column or rows constraints. They normally looks something like
     * <code>"[min:pref]rel[10px][]"</code>.
     *
     * @param s The string to parse. Not <code>null</code>.
     * @return An array of {@link DimConstraint}s that is as many are there
     * exist "[...]" sections in the string that is parsed.
     * @throws RuntimeException if the constraint was not valid.
     */
    public static AC parseColumnConstraints(String s) {
        return parseAxisConstraint(s, true);
    }

    /**
     * Parses the column or rows constraints. They normally looks something like
     * <code>"[min:pref]rel[10px][]"</code>.
     *
     * @param s The string to parse. Not <code>null</code>.
     * @param isCols If this for columns rather than rows.
     * @return An array of {@link DimConstraint}s that is as many are there
     * exist "[...]" sections in the string that is parsed.
     * @throws RuntimeException if the constraint was not valid.
     */
    private static AC parseAxisConstraint(String s, boolean isCols) {
        s = s.trim();

        if (s.length() == 0) {
            return new AC();    // Short circuit for performance.
        }
        s = s.toLowerCase();

        ArrayList<String> parts = getRowColAndGapsTrimmed(s);

        BoundSize[] gaps = new BoundSize[(parts.size() >> 1) + 1];
        for (int i = 0, iSz = parts.size(), gIx = 0; i < iSz; i += 2, gIx++) {
            gaps[gIx] = parseBoundSize(parts.get(i), true, isCols);
        }

        DimConstraint[] colSpecs = new DimConstraint[parts.size() >> 1];
        for (int i = 0, gIx = 0; i < colSpecs.length; i++, gIx++) {
            if (gIx >= gaps.length - 1) {
                gIx = gaps.length - 2;
            }

            colSpecs[i] = parseDimConstraint(parts.get((i << 1) + 1), gaps[gIx], gaps[gIx + 1], isCols);
        }

        AC ac = new AC();
        ac.setConstaints(colSpecs);

//		ac = (AC) serializeTest(ac);
        return ac;
    }

    /**
     * Parses a single column or row constraint.
     *
     * @param s The single constraint to parse. May look something like
     * <code>"min:pref,fill,grow"</code>. Should not be <code>null</code> and
     * <b>must be lower case and trimmed</b>.
     * @param gapBefore The default gap "before" the column/row constraint. Can
     * be overridden with a <code>"gap"</code> section within <code>s</code>.
     * @param gapAfter The default gap "after" the column/row constraint. Can be
     * overridden with a <code>"gap"</code> section within <code>s</code>.
     * @param isCols If the constraints are column constraints rather than row
     * constraints.
     * @return A single constraint. Never <code>null</code>.
     * @throws RuntimeException if the constraint was not valid.
     */
    private static DimConstraint parseDimConstraint(String s, BoundSize gapBefore, BoundSize gapAfter, boolean isCols) {
        DimConstraint dimConstraint = new DimConstraint();

        // Default values.
        dimConstraint.setGapBefore(gapBefore);
        dimConstraint.setGapAfter(gapAfter);

        String[] parts = toTrimmedTokens(s, ',');
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                if (part.length() == 0) {
                    continue;
                }

                if (part.equals("fill")) {
                    dimConstraint.setFill(true);
//					 dimConstraint.setAlign(null);   // Can not have both fill and alignment (changed for 3.5 since it can have "growy 0")
                    continue;
                }

                if (part.equals("nogrid")) {
                    dimConstraint.setNoGrid(true);
                    continue;
                }

                int ix = -1;
                char c = part.charAt(0);

                if (c == 's') {
                    ix = startsWithLenient(part, new String[]{"sizegroup", "sg"}, new int[]{5, 2}, true);
                    if (ix > -1) {
                        dimConstraint.setSizeGroup(part.substring(ix).trim());
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"shrinkprio", "shp"}, new int[]{10, 3}, true);
                    if (ix > -1) {
                        dimConstraint.setShrinkPriority(Integer.parseInt(part.substring(ix).trim()));
                        continue;
                    }

                    ix = startsWithLenient(part, "shrink", 6, true);
                    if (ix > -1) {
                        dimConstraint.setShrink(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }
                }

                if (c == 'g') {
                    ix = startsWithLenient(part, new String[]{"growpriority", "gp"}, new int[]{5, 2}, true);
                    if (ix > -1) {
                        dimConstraint.setGrowPriority(Integer.parseInt(part.substring(ix).trim()));
                        continue;
                    }

                    ix = startsWithLenient(part, "grow", 4, true);
                    if (ix > -1) {
                        dimConstraint.setGrow(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }
                }

                if (c == 'a') {
                    ix = startsWithLenient(part, "align", 2, true);
                    if (ix > -1) {
//						if (dimConstraint.isFill() == false)    // Swallow, but ignore if fill is set. (changed for 3.5 since it can have "growy 0")
                        dimConstraint.setAlign(parseUnitValueOrAlign(part.substring(ix).trim(), isCols, null));
                        continue;
                    }
                }

                UnitValue align = parseAlignKeywords(part, isCols);
                if (align != null) {
//					if (dimConstraint.isFill() == false)    // Swallow, but ignore if fill is set. (changed for 3.5 since it can have "growy 0")
                    dimConstraint.setAlign(align);
                    continue;
                }

                // Only min:pref:max still left that is ok
                dimConstraint.setSize(parseBoundSize(part, false, isCols));

            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal constraint: '" + part + "'\n" + ex.getMessage());
            }
        }
        return dimConstraint;
    }

    /**
     * Parses all component constraints and stores the parsed values in the
     * transient (cache) member variables.
     *
     * @param constrMap The constraints as <code>String</code>s. Strings <b>must
     * be lower case and trimmed</b>
     * @return The parsed constraints. Never <code>null</code>.
     */
    public static Map<ComponentWrapper, CC> parseComponentConstraints(Map<ComponentWrapper, String> constrMap) {
        HashMap<ComponentWrapper, CC> flowConstrMap = new HashMap<ComponentWrapper, CC>();

        for (ComponentWrapper c : constrMap.keySet()) {
            flowConstrMap.put(c, parseComponentConstraint(constrMap.get(c)));
        }

        return flowConstrMap;
    }

    /**
     * Parses one component constraint and returns the parsed value.
     *
     * @param s The string to parse. Should not be <code>null</code> and <b>must
     * be lower case and trimmed</b>.
     * @throws RuntimeException if the constraint was not valid.
     * @return The parsed constraint. Never <code>null</code>.
     */
    public static CC parseComponentConstraint(String s) {
        CC cc = new CC();

        if (s.length() == 0) {
            return cc;
        }

        String[] parts = toTrimmedTokens(s, ',');

        for (String part : parts) {
            try {
                if (part.length() == 0) {
                    continue;
                }

                int ix = -1;
                char c = part.charAt(0);

                if (c == 'n') {
                    if (part.equals("north")) {
                        cc.setDockSide(0);
                        continue;
                    }

                    if (part.equals("newline")) {
                        cc.setNewline(true);
                        continue;
                    }

                    if (part.startsWith("newline ")) {
                        String gapSz = part.substring(7).trim();
                        cc.setNewlineGapSize(parseBoundSize(gapSz, true, true));
                        continue;
                    }
                }

                if (c == 'f' && (part.equals("flowy") || part.equals("flowx"))) {
                    cc.setFlowX(part.charAt(4) == 'x' ? Boolean.TRUE : Boolean.FALSE);
                    continue;
                }

                if (c == 's') {
                    ix = startsWithLenient(part, "skip", 4, true);
                    if (ix > -1) {
                        String num = part.substring(ix).trim();
                        cc.setSkip(num.length() != 0 ? Integer.parseInt(num) : 1);
                        continue;
                    }

                    ix = startsWithLenient(part, "split", 5, true);
                    if (ix > -1) {
                        String split = part.substring(ix).trim();
                        cc.setSplit(split.length() > 0 ? Integer.parseInt(split) : LayoutUtil.INF);
                        continue;
                    }

                    if (part.equals("south")) {
                        cc.setDockSide(2);
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"spany", "sy"}, new int[]{5, 2}, true);
                    if (ix > -1) {
                        cc.setSpanY(parseSpan(part.substring(ix).trim()));
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"spanx", "sx"}, new int[]{5, 2}, true);
                    if (ix > -1) {
                        cc.setSpanX(parseSpan(part.substring(ix).trim()));
                        continue;
                    }

                    ix = startsWithLenient(part, "span", 4, true);
                    if (ix > -1) {
                        String[] spans = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        cc.setSpanX(spans[0].length() > 0 ? Integer.parseInt(spans[0]) : LayoutUtil.INF);
                        cc.setSpanY(spans.length > 1 ? Integer.parseInt(spans[1]) : 1);
                        continue;
                    }

                    ix = startsWithLenient(part, "shrinkx", 7, true);
                    if (ix > -1) {
                        cc.getHorizontal().setShrink(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "shrinky", 7, true);
                    if (ix > -1) {
                        cc.getVertical().setShrink(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "shrink", 6, false);
                    if (ix > -1) {
                        String[] shrinks = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        cc.getHorizontal().setShrink(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        if (shrinks.length > 1) {
                            cc.getVertical().setShrink(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        }
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"shrinkprio", "shp"}, new int[]{10, 3}, true);
                    if (ix > -1) {
                        String sp = part.substring(ix).trim();
                        if (sp.startsWith("x") || sp.startsWith("y")) { // To gandle "gpx", "gpy", "shrinkpriorityx", shrinkpriorityy"
                            (sp.startsWith("x") ? cc.getHorizontal() : cc.getVertical()).setShrinkPriority(Integer.parseInt(sp.substring(2)));
                        } else {
                            String[] shrinks = toTrimmedTokens(sp, ' ');
                            cc.getHorizontal().setShrinkPriority(Integer.parseInt(shrinks[0]));
                            if (shrinks.length > 1) {
                                cc.getVertical().setShrinkPriority(Integer.parseInt(shrinks[1]));
                            }
                        }
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"sizegroupx", "sizegroupy", "sgx", "sgy"}, new int[]{9, 9, 2, 2}, true);
                    if (ix > -1) {
                        String sg = part.substring(ix).trim();
                        char lc = part.charAt(ix - 1);
                        if (lc != 'y') {
                            cc.getHorizontal().setSizeGroup(sg);
                        }
                        if (lc != 'x') {
                            cc.getVertical().setSizeGroup(sg);
                        }
                        continue;
                    }
                }

                if (c == 'g') {
                    ix = startsWithLenient(part, "growx", 5, true);
                    if (ix > -1) {
                        cc.getHorizontal().setGrow(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "growy", 5, true);
                    if (ix > -1) {
                        cc.getVertical().setGrow(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "grow", 4, false);
                    if (ix > -1) {
                        String[] grows = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        cc.getHorizontal().setGrow(parseFloat(grows[0], ResizeConstraint.WEIGHT_100));
                        cc.getVertical().setGrow(parseFloat(grows.length > 1 ? grows[1] : "", ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"growprio", "gp"}, new int[]{8, 2}, true);
                    if (ix > -1) {
                        String gp = part.substring(ix).trim();
                        char c0 = gp.length() > 0 ? gp.charAt(0) : ' ';
                        if (c0 == 'x' || c0 == 'y') { // To gandle "gpx", "gpy", "growpriorityx", growpriorityy"
                            (c0 == 'x' ? cc.getHorizontal() : cc.getVertical()).setGrowPriority(Integer.parseInt(gp.substring(2)));
                        } else {
                            String[] grows = toTrimmedTokens(gp, ' ');
                            cc.getHorizontal().setGrowPriority(Integer.parseInt(grows[0]));
                            if (grows.length > 1) {
                                cc.getVertical().setGrowPriority(Integer.parseInt(grows[1]));
                            }
                        }
                        continue;
                    }

                    if (part.startsWith("gap")) {
                        BoundSize[] gaps = parseGaps(part); // Changes order!!
                        if (gaps[0] != null) {
                            cc.getVertical().setGapBefore(gaps[0]);
                        }
                        if (gaps[1] != null) {
                            cc.getHorizontal().setGapBefore(gaps[1]);
                        }
                        if (gaps[2] != null) {
                            cc.getVertical().setGapAfter(gaps[2]);
                        }
                        if (gaps[3] != null) {
                            cc.getHorizontal().setGapAfter(gaps[3]);
                        }
                        continue;
                    }
                }

                if (c == 'a') {
                    ix = startsWithLenient(part, new String[]{"aligny", "ay"}, new int[]{6, 2}, true);
                    if (ix > -1) {
                        cc.getVertical().setAlign(parseUnitValueOrAlign(part.substring(ix).trim(), false, null));
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"alignx", "ax"}, new int[]{6, 2}, true);
                    if (ix > -1) {
                        cc.getHorizontal().setAlign(parseUnitValueOrAlign(part.substring(ix).trim(), true, null));
                        continue;
                    }

                    ix = startsWithLenient(part, "align", 2, true);
                    if (ix > -1) {
                        String[] gaps = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        cc.getHorizontal().setAlign(parseUnitValueOrAlign(gaps[0], true, null));
                        if (gaps.length > 1) {
                            cc.getVertical().setAlign(parseUnitValueOrAlign(gaps[1], false, null));
                        }
                        continue;
                    }
                }

                if ((c == 'x' || c == 'y') && part.length() > 2) {
                    char c2 = part.charAt(1);
                    if (c2 == ' ' || (c2 == '2' && part.charAt(2) == ' ')) {
                        if (cc.getPos() == null) {
                            cc.setPos(new UnitValue[4]);
                        } else if (cc.isBoundsInGrid() == false) {
                            throw new IllegalArgumentException("Cannot combine 'position' with 'x/y/x2/y2' keywords.");
                        }

                        int edge = (c == 'x' ? 0 : 1) + (c2 == '2' ? 2 : 0);
                        UnitValue[] pos = cc.getPos();
                        pos[edge] = parseUnitValue(part.substring(2).trim(), null, c == 'x');
                        cc.setPos(pos);
                        cc.setBoundsInGrid(true);
                        continue;
                    }
                }

                if (c == 'c') {
                    ix = startsWithLenient(part, "cell", 4, true);
                    if (ix > -1) {
                        String[] grs = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        if (grs.length < 2) {
                            throw new IllegalArgumentException("At least two integers must follow " + part);
                        }
                        cc.setCellX(Integer.parseInt(grs[0]));
                        cc.setCellY(Integer.parseInt(grs[1]));
                        if (grs.length > 2) {
                            cc.setSpanX(Integer.parseInt(grs[2]));
                        }
                        if (grs.length > 3) {
                            cc.setSpanY(Integer.parseInt(grs[3]));
                        }
                        continue;
                    }
                }

                if (c == 'p') {
                    ix = startsWithLenient(part, "pos", 3, true);
                    if (ix > -1) {
                        if (cc.getPos() != null && cc.isBoundsInGrid()) {
                            throw new IllegalArgumentException("Can not combine 'pos' with 'x/y/x2/y2' keywords.");
                        }

                        String[] pos = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        UnitValue[] bounds = new UnitValue[4];
                        for (int j = 0; j < pos.length; j++) {
                            bounds[j] = parseUnitValue(pos[j], null, j % 2 == 0);
                        }

                        if (bounds[0] == null && bounds[2] == null || bounds[1] == null && bounds[3] == null) {
                            throw new IllegalArgumentException("Both x and x2 or y and y2 can not be null!");
                        }

                        cc.setPos(bounds);
                        cc.setBoundsInGrid(false);
                        continue;
                    }

                    ix = startsWithLenient(part, "pad", 3, true);
                    if (ix > -1) {
                        UnitValue[] p = parseInsets(part.substring(ix).trim(), false);
                        cc.setPadding(new UnitValue[]{
                            p[0],
                            p.length > 1 ? p[1] : null,
                            p.length > 2 ? p[2] : null,
                            p.length > 3 ? p[3] : null});
                        continue;
                    }

                    ix = startsWithLenient(part, "pushx", 5, true);
                    if (ix > -1) {
                        cc.setPushX(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "pushy", 5, true);
                    if (ix > -1) {
                        cc.setPushY(parseFloat(part.substring(ix).trim(), ResizeConstraint.WEIGHT_100));
                        continue;
                    }

                    ix = startsWithLenient(part, "push", 4, false);
                    if (ix > -1) {
                        String[] pushs = toTrimmedTokens(part.substring(ix).trim(), ' ');
                        cc.setPushX(parseFloat(pushs[0], ResizeConstraint.WEIGHT_100));
                        cc.setPushY(parseFloat(pushs.length > 1 ? pushs[1] : "", ResizeConstraint.WEIGHT_100));
                        continue;
                    }
                }

                if (c == 't') {
                    ix = startsWithLenient(part, "tag", 3, true);
                    if (ix > -1) {
                        cc.setTag(part.substring(ix).trim());
                        continue;
                    }
                }

                if (c == 'w' || c == 'h') {
                    if (part.equals("wrap")) {
                        cc.setWrap(true);
                        continue;
                    }

                    if (part.startsWith("wrap ")) {
                        String gapSz = part.substring(5).trim();
                        cc.setWrapGapSize(parseBoundSize(gapSz, true, true));
                        continue;
                    }

                    boolean isHor = c == 'w';
                    if (isHor && (part.startsWith("w ") || part.startsWith("width "))) {
                        String uvStr = part.substring(part.charAt(1) == ' ' ? 2 : 6).trim();
                        cc.getHorizontal().setSize(parseBoundSize(uvStr, false, true));
                        continue;
                    }

                    if (!isHor && (part.startsWith("h ") || part.startsWith("height "))) {
                        String uvStr = part.substring(part.charAt(1) == ' ' ? 2 : 7).trim();
                        cc.getVertical().setSize(parseBoundSize(uvStr, false, false));
                        continue;
                    }

                    if (part.startsWith("wmin ") || part.startsWith("wmax ") || part.startsWith("hmin ") || part.startsWith("hmax ")) {
                        String uvStr = part.substring(5).trim();
                        if (uvStr.length() > 0) {
                            UnitValue uv = parseUnitValue(uvStr, null, isHor);
                            boolean isMin = part.charAt(3) == 'n';
                            DimConstraint dc = isHor ? cc.getHorizontal() : cc.getVertical();
                            dc.setSize(new BoundSize(
                                    isMin ? uv : dc.getSize().getMin(),
                                    dc.getSize().getPreferred(),
                                    isMin ? (dc.getSize().getMax()) : uv,
                                    uvStr
                            ));
                            continue;
                        }
                    }

                    if (part.equals("west")) {
                        cc.setDockSide(1);
                        continue;
                    }

                    if (part.startsWith("hidemode ")) {
                        cc.setHideMode(Integer.parseInt(part.substring(9)));
                        continue;
                    }
                }

                if (c == 'i' && part.startsWith("id ")) {
                    cc.setId(part.substring(3).trim());
                    int dIx = cc.getId().indexOf('.');
                    if (dIx == 0 || dIx == cc.getId().length() - 1) {
                        throw new IllegalArgumentException("Dot must not be first or last!");
                    }

                    continue;
                }

                if (c == 'e') {
                    if (part.equals("east")) {
                        cc.setDockSide(3);
                        continue;
                    }

                    if (part.equals("external")) {
                        cc.setExternal(true);
                        continue;
                    }

                    ix = startsWithLenient(part, new String[]{"endgroupx", "endgroupy", "egx", "egy"}, new int[]{-1, -1, -1, -1}, true);
                    if (ix > -1) {
                        String sg = part.substring(ix).trim();
                        char lc = part.charAt(ix - 1);
                        DimConstraint dc = (lc == 'x' ? cc.getHorizontal() : cc.getVertical());
                        dc.setEndGroup(sg);
                        continue;
                    }
                }

                if (c == 'd') {
                    if (part.equals("dock north")) {
                        cc.setDockSide(0);
                        continue;
                    }
                    if (part.equals("dock west")) {
                        cc.setDockSide(1);
                        continue;
                    }
                    if (part.equals("dock south")) {
                        cc.setDockSide(2);
                        continue;
                    }
                    if (part.equals("dock east")) {
                        cc.setDockSide(3);
                        continue;
                    }

                    if (part.equals("dock center")) {
                        cc.getHorizontal().setGrow(new Float(100f));
                        cc.getVertical().setGrow(new Float(100f));
                        cc.setPushX(new Float(100f));
                        cc.setPushY(new Float(100f));
                        continue;
                    }
                }

                if (c == 'v') {
                    ix = startsWithLenient(part, new String[]{"visualpadding", "vp"}, new int[]{3, 2}, true);
                    if (ix > -1) {
                        UnitValue[] p = parseInsets(part.substring(ix).trim(), false);
                        cc.setVisualPadding(new UnitValue[]{
                            p[0],
                            p.length > 1 ? p[1] : null,
                            p.length > 2 ? p[2] : null,
                            p.length > 3 ? p[3] : null});
                        continue;
                    }
                }

                UnitValue horAlign = parseAlignKeywords(part, true);
                if (horAlign != null) {
                    cc.getHorizontal().setAlign(horAlign);
                    continue;
                }

                UnitValue verAlign = parseAlignKeywords(part, false);
                if (verAlign != null) {
                    cc.getVertical().setAlign(verAlign);
                    continue;
                }

                throw new IllegalArgumentException("Unknown keyword.");

            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IllegalArgumentException("Error parsing Constraint: '" + part + "'");
            }
        }

//		cc = (CC) serializeTest(cc);
        return cc;
    }

    /**
     * Parses insets which consists of 1-4 <code>UnitValue</code>s.
     *
     * @param s The string to parse. E.g. "10 10 10 10" or "20". If less than 4
     * groups the last will be used for the missing.
     * @param acceptPanel If "panel" and "dialog" should be accepted. They are
     * used to access platform defaults.
     * @return An array of length 4 with the parsed insets.
     * @throws IllegalArgumentException if the parsing could not be done.
     */
    public static UnitValue[] parseInsets(String s, boolean acceptPanel) {
        if (s.length() == 0 || s.equals("dialog") || s.equals("panel")) {
            if (acceptPanel == false) {
                throw new IllegalArgumentException("Insets now allowed: " + s + "\n");
            }

            boolean isPanel = s.startsWith("p");
            UnitValue[] ins = new UnitValue[4];
            for (int j = 0; j < 4; j++) {
                ins[j] = isPanel ? PlatformDefaults.getPanelInsets(j) : PlatformDefaults.getDialogInsets(j);
            }

            return ins;
        } else {
            String[] insS = toTrimmedTokens(s, ' ');
            UnitValue[] ins = new UnitValue[4];
            for (int j = 0; j < 4; j++) {
                UnitValue insSz = parseUnitValue(insS[j < insS.length ? j : insS.length - 1], UnitValue.ZERO, j % 2 == 1);
                ins[j] = insSz != null ? insSz : PlatformDefaults.getPanelInsets(j);
            }
            return ins;
        }
    }

    /**
     * Parses gaps.
     *
     * @param s The string that contains gap information. Should start with
     * "gap".
     * @return The gaps as specified in <code>s</code>. Indexed:
     * <code>[top,left,bottom,right][min,pref,max]</code> or
     * [before,after][min,pref,max] if <code>oneDim</code> is true.
     */
    private static BoundSize[] parseGaps(String s) {
        BoundSize[] ret = new BoundSize[4];

        int ix = startsWithLenient(s, "gaptop", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[0] = parseBoundSize(s, true, false);
            return ret;
        }

        ix = startsWithLenient(s, "gapleft", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[1] = parseBoundSize(s, true, true);
            return ret;
        }

        ix = startsWithLenient(s, "gapbottom", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[2] = parseBoundSize(s, true, false);
            return ret;
        }

        ix = startsWithLenient(s, "gapright", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[3] = parseBoundSize(s, true, true);
            return ret;
        }

        ix = startsWithLenient(s, "gapbefore", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[1] = parseBoundSize(s, true, true);
            return ret;
        }

        ix = startsWithLenient(s, "gapafter", -1, true);
        if (ix > -1) {
            s = s.substring(ix).trim();
            ret[3] = parseBoundSize(s, true, true);
            return ret;
        }

        ix = startsWithLenient(s, new String[]{"gapx", "gapy"}, null, true);
        if (ix > -1) {
            boolean x = s.charAt(3) == 'x';
            String[] gaps = toTrimmedTokens(s.substring(ix).trim(), ' ');
            ret[x ? 1 : 0] = parseBoundSize(gaps[0], true, x);
            if (gaps.length > 1) {
                ret[x ? 3 : 2] = parseBoundSize(gaps[1], true, !x);
            }
            return ret;
        }

        ix = startsWithLenient(s, "gap ", 1, true);
        if (ix > -1) {
            String[] gaps = toTrimmedTokens(s.substring(ix).trim(), ' ');

            ret[1] = parseBoundSize(gaps[0], true, true);   // left
            if (gaps.length > 1) {
                ret[3] = parseBoundSize(gaps[1], true, false);   // right
                if (gaps.length > 2) {
                    ret[0] = parseBoundSize(gaps[2], true, true);   // top
                    if (gaps.length > 3) {
                        ret[2] = parseBoundSize(gaps[3], true, false);   // bottom
                    }
                }
            }
            return ret;
        }

        throw new IllegalArgumentException("Unknown Gap part: '" + s + "'");
    }

    private static int parseSpan(String s) {
        return s.length() > 0 ? Integer.parseInt(s) : LayoutUtil.INF;
    }

    private static Float parseFloat(String s, Float nullVal) {
        return s.length() > 0 ? new Float(Float.parseFloat(s)) : nullVal;
    }

    /**
     * Parses a single "min:pref:max" value. May look something like
     * <code>"10px:20lp:30%"</code> or <code>"pref!"</code>.
     *
     * @param s The string to parse. Not <code>null</code>.
     * @param isGap If this bound size is a gap (different empty string
     * handling).
     * @param isHor If the size is for the horizontal dimension.
     * @return A bound size that may be <code>null</code> if the string was
     * "null", "n" or <code>null</code>.
     */
    public static BoundSize parseBoundSize(String s, boolean isGap, boolean isHor) {
        if (s.length() == 0 || s.equals("null") || s.equals("n")) {
            return null;
        }

        String cs = s;
        boolean push = false;
        if (s.endsWith("push")) {
            push = true;
            int l = s.length();
            s = s.substring(0, l - (s.endsWith(":push") ? 5 : 4));
            if (s.length() == 0) {
                return new BoundSize(null, null, null, true, cs);
            }
        }

        String[] sizes = toTrimmedTokens(s, ':');
        String s0 = sizes[0];

        if (sizes.length == 1) {
            boolean hasEM = s0.endsWith("!");
            if (hasEM) {
                s0 = s0.substring(0, s0.length() - 1);
            }
            UnitValue uv = parseUnitValue(s0, null, isHor);
            return new BoundSize(((isGap || hasEM) ? uv : null), uv, (hasEM ? uv : null), push, cs);

        } else if (sizes.length == 2) {
            return new BoundSize(parseUnitValue(s0, null, isHor), parseUnitValue(sizes[1], null, isHor), null, push, cs);
        } else if (sizes.length == 3) {
            return new BoundSize(parseUnitValue(s0, null, isHor), parseUnitValue(sizes[1], null, isHor), parseUnitValue(sizes[2], null, isHor), push, cs);
        } else {
            throw new IllegalArgumentException("Min:Preferred:Max size section must contain 0, 1 or 2 colons. '" + cs + "'");
        }
    }

    /**
     * Parses a single unit value that may also be an alignment as parsed by
     * {@link #parseAlignKeywords(String, boolean)}.
     *
     * @param s The string to parse. Not <code>null</code>. May look something
     * like <code>"10px"</code> or <code>"5dlu"</code>.
     * @param isHor If the value is for the horizontal dimension.
     * @param emptyReplacement A replacement if <code>s</code> is empty. May be
     * <code>null</code>.
     * @return The parsed unit value. May be <code>null</code>.
     */
    public static UnitValue parseUnitValueOrAlign(String s, boolean isHor, UnitValue emptyReplacement) {
        if (s.length() == 0) {
            return emptyReplacement;
        }

        UnitValue align = parseAlignKeywords(s, isHor);
        if (align != null) {
            return align;
        }

        return parseUnitValue(s, emptyReplacement, isHor);
    }

    /**
     * Parses a single unit value. E.g. "10px" or "5in"
     *
     * @param s The string to parse. Not <code>null</code>. May look something
     * like <code>"10px"</code> or <code>"5dlu"</code>.
     * @param isHor If the value is for the horizontal dimension.
     * @return The parsed unit value. <code>null</code> is empty string,
     */
    public static UnitValue parseUnitValue(String s, boolean isHor) {
        return parseUnitValue(s, null, isHor);
    }

    /**
     * Parses a single unit value.
     *
     * @param s The string to parse. May be <code>null</code>. May look
     * something like <code>"10px"</code> or <code>"5dlu"</code>.
     * @param emptyReplacement A replacement <code>s</code> is empty or
     * <code>null</code>. May be <code>null</code>.
     * @param isHor If the value is for the horizontal dimension.
     * @return The parsed unit value. May be <code>null</code>.
     */
    private static UnitValue parseUnitValue(String s, UnitValue emptyReplacement, boolean isHor) {
        if (s == null || s.length() == 0) {
            return emptyReplacement;
        }

        String cs = s; // Save creation string.
        char c0 = s.charAt(0);

        // Remove start and end parentheses, if there.
        if (c0 == '(' && s.charAt(s.length() - 1) == ')') {
            s = s.substring(1, s.length() - 1);
        }

        if (c0 == 'n' && (s.equals("null") || s.equals("n"))) {
            return null;
        }

        if (c0 == 'i' && s.equals("inf")) {
            return UnitValue.INF;
        }

        int oper = getOper(s);
        boolean inline = oper == UnitValue.ADD || oper == UnitValue.SUB || oper == UnitValue.MUL || oper == UnitValue.DIV;

        if (oper != UnitValue.STATIC) {  // It is a multi-value

            String[] uvs;
            if (inline == false) {   // If the format is of type "opr(xxx,yyy)" (compared to in-line "10%+15px")
                String sub = s.substring(4, s.length() - 1).trim();
                uvs = toTrimmedTokens(sub, ',');
                if (uvs.length == 1) {
                    return parseUnitValue(sub, null, isHor);
                }
            } else {
                char delim;
                if (oper == UnitValue.ADD) {
                    delim = '+';
                } else if (oper == UnitValue.SUB) {
                    delim = '-';
                } else if (oper == UnitValue.MUL) {
                    delim = '*';
                } else {    // div left
                    delim = '/';
                }
                uvs = toTrimmedTokens(s, delim);
                if (uvs.length > 2) {   // More than one +-*/.
                    String last = uvs[uvs.length - 1];
                    String first = s.substring(0, s.length() - last.length() - 1);
                    uvs = new String[]{first, last};
                }
            }

            if (uvs.length != 2) {
                throw new IllegalArgumentException("Malformed UnitValue: '" + s + "'");
            }

            UnitValue sub1 = parseUnitValue(uvs[0], null, isHor);
            UnitValue sub2 = parseUnitValue(uvs[1], null, isHor);

            if (sub1 == null || sub2 == null) {
                throw new IllegalArgumentException("Malformed UnitValue. Must be two sub-values: '" + s + "'");
            }

            return new UnitValue(isHor, oper, sub1, sub2, cs);
        } else {
            try {
                String[] numParts = getNumTextParts(s);
                float value = numParts[0].length() > 0 ? Float.parseFloat(numParts[0]) : 1;     // e.g. "related" has no number part..

                return new UnitValue(value, numParts[1], isHor, oper, cs);

            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Malformed UnitValue: '" + s + "'");
            }
        }
    }

    /**
     * Parses alignment keywords and returns the appropriate
     * <code>UnitValue</code>.
     *
     * @param s The string to parse. Not <code>null</code>.
     * @param isHor If alignments for horizontal is checked. <code>false</code>
     * means vertical.
     * @return The unit value or <code>null</code> if not recognized (no
     * exception).
     */
    static UnitValue parseAlignKeywords(String s, boolean isHor) {
        if (startsWithLenient(s, "center", 1, false) != -1) {
            return UnitValue.CENTER;
        }

        if (isHor) {
            if (startsWithLenient(s, "left", 1, false) != -1) {
                return UnitValue.LEFT;
            }

            if (startsWithLenient(s, "right", 1, false) != -1) {
                return UnitValue.RIGHT;
            }

            if (startsWithLenient(s, "leading", 4, false) != -1) {
                return UnitValue.LEADING;
            }

            if (startsWithLenient(s, "trailing", 5, false) != -1) {
                return UnitValue.TRAILING;
            }

            if (startsWithLenient(s, "label", 5, false) != -1) {
                return UnitValue.LABEL;
            }

        } else {

            if (startsWithLenient(s, "baseline", 4, false) != -1) {
                return UnitValue.BASELINE_IDENTITY;
            }

            if (startsWithLenient(s, "top", 1, false) != -1) {
                return UnitValue.TOP;
            }

            if (startsWithLenient(s, "bottom", 1, false) != -1) {
                return UnitValue.BOTTOM;
            }
        }

        return null;
    }

    /**
     * Splits a text-number combination such as "hello 10.0" into
     * <code>{"hello", "10.0"}</code>.
     *
     * @param s The string to split. Not <code>null</code>. Needs be be
     * reasonably formatted since the method only finds the first 0-9 or . and
     * cuts the string in half there.
     * @return Always length 2 and no <code>null</code> elements. Elements are
     * "" if no part found.
     */
    private static String[] getNumTextParts(String s) {
        for (int i = 0, iSz = s.length(); i < iSz; i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                throw new IllegalArgumentException("Space in UnitValue: '" + s + "'");
            }

            if ((c < '0' || c > '9') && c != '.' && c != '-') {
                return new String[]{s.substring(0, i).trim(), s.substring(i).trim()};
            }
        }
        return new String[]{s, ""};
    }

    /**
     * Returns the operation depending on the start character.
     *
     * @param s The string to check. Not <code>null</code>.
     * @return E.g. UnitValue.ADD, UnitValue.SUB or UnitValue.STATIC. Returns
     * negative value for in-line operations.
     */
    private static int getOper(String s) {
        int len = s.length();
        if (len < 3) {
            return UnitValue.STATIC;
        }

        if (len > 5 && s.charAt(3) == '(' && s.charAt(len - 1) == ')') {
            if (s.startsWith("min(")) {
                return UnitValue.MIN;
            }

            if (s.startsWith("max(")) {
                return UnitValue.MAX;
            }

            if (s.startsWith("mid(")) {
                return UnitValue.MID;
            }
        }

        // Try in-line add/sub. E.g. "pref+10px".
        for (int j = 0; j < 2; j++) {   // First +-   then */   (precedence)
            for (int i = len - 1, p = 0; i > 0; i--) {
                char c = s.charAt(i);
                if (c == ')') {
                    p++;
                } else if (c == '(') {
                    p--;
                } else if (p == 0) {
                    if (j == 0) {
                        if (c == '+') {
                            return UnitValue.ADD;
                        }
                        if (c == '-') {
                            return UnitValue.SUB;
                        }
                    } else {
                        if (c == '*') {
                            return UnitValue.MUL;
                        }
                        if (c == '/') {
                            return UnitValue.DIV;
                        }
                    }
                }
            }
        }
        return UnitValue.STATIC;
    }

    /**
     * Returns if a string shares at least a specified numbers starting
     * characters with a number of matches.
     * <p>
     * This method just exercise
     * {@link #startsWithLenient(String, String, int, boolean)} with every one
     * of <code>matches</code> and <code>minChars</code>.
     *
     * @param s The string to check. Not <code>null</code>.
     * @param matches A number of possible starts for <code>s</code>.
     * @param minChars The minimum number of characters to match for every
     * element in <code>matches</code>. Needs to be of same length as
     * <code>matches</code>. Can be <code>null</code>.
     * @param acceptTrailing If after the required number of characters are
     * matched on recognized characters that are not in one of the the
     * <code>matches</code> string should be accepted. For instance if "abczz"
     * should be matched with "abcdef" and min chars 3.
     * @return The index of the first unmatched character if
     * <code>minChars</code> was reached or <code>-1</code> if a match was not
     * found.
     */
    private static int startsWithLenient(String s, String[] matches, int[] minChars, boolean acceptTrailing) {
        for (int i = 0; i < matches.length; i++) {
            int minChar = minChars != null ? minChars[i] : -1;
            int ix = startsWithLenient(s, matches[i], minChar, acceptTrailing);
            if (ix > -1) {
                return ix;
            }
        }
        return -1;
    }

    /**
     * Returns if a string shares at least a specified numbers starting
     * characters with a match.
     *
     * @param s The string to check. Not <code>null</code> and must be trimmed.
     * @param match The possible start for <code>s</code>. Not <code>null</code>
     * and must be trimmed.
     * @param minChars The mimimum number of characters to match to
     * <code>s</code> for it this to be considered a match. -1 means the full
     * length of <code>match</code>.
     * @param acceptTrailing If after the required number of charecters are
     * matched unrecognized characters that are not in one of the the
     * <code>matches</code> string should be accepted. For instance if "abczz"
     * should be matched with "abcdef" and min chars 3.
     * @return The index of the first unmatched character if
     * <code>minChars</code> was reached or <code>-1</code> if a match was not
     * found.
     */
    private static int startsWithLenient(String s, String match, int minChars, boolean acceptTrailing) {
        if (s.charAt(0) != match.charAt(0)) // Fast sanity check.
        {
            return -1;
        }

        if (minChars == -1) {
            minChars = match.length();
        }

        int sSz = s.length();
        if (sSz < minChars) {
            return -1;
        }

        int mSz = match.length();
        int sIx = 0;
        for (int mIx = 0; mIx < mSz; sIx++, mIx++) {
            while (sIx < sSz && (s.charAt(sIx) == ' ' || s.charAt(sIx) == '_')) // Disregard spaces and _
            {
                sIx++;
            }

            if (sIx >= sSz || s.charAt(sIx) != match.charAt(mIx)) {
                return mIx >= minChars && (acceptTrailing || sIx >= sSz) && (sIx >= sSz || s.charAt(sIx - 1) == ' ') ? sIx : -1;
            }
        }
        return sIx >= sSz || acceptTrailing || s.charAt(sIx) == ' ' ? sIx : -1;
    }

    /**
     * Parses a string and returns it in those parts of the string that are
     * separated with a <code>sep</code> character.
     * <p>
     * separator characters within parentheses will not be counted or handled in
     * any way, whatever the depth.
     * <p>
     * A space separator will be a hit to one or more spaces and thus not return
     * empty strings.
     *
     * @param s The string to parse. If it starts and/or ends with a
     * <code>sep</code> the first and/or last element returned will be "". If
     * two <code>sep</code> are next to each other and empty element will be
     * "between" the periods. The <code>sep</code> themselves will never be
     * returned.
     * @param sep The separator char.
     * @return Those parts of the string that are separated with
     * <code>sep</code>. Never null and at least of size 1
     * @since 6.7.2 Changed so more than one space in a row works as one space.
     */
    private static String[] toTrimmedTokens(String s, char sep) {
        int toks = 0, sSize = s.length();
        boolean disregardDoubles = sep == ' ';

        // Count the sep:s
        int p = 0;
        for (int i = 0; i < sSize; i++) {
            char c = s.charAt(i);
            if (c == '(') {
                p++;
            } else if (c == ')') {
                p--;
            } else if (p == 0 && c == sep) {
                toks++;
                while (disregardDoubles && i < sSize - 1 && s.charAt(i + 1) == ' ') {
                    i++;
                }
            }
            if (p < 0) {
                throw new IllegalArgumentException("Unbalanced parentheses: '" + s + "'");
            }
        }
        if (p != 0) {
            throw new IllegalArgumentException("Unbalanced parentheses: '" + s + "'");
        }

        if (toks == 0) {
            return new String[]{s.trim()};
        }

        String[] retArr = new String[toks + 1];

        int st = 0, pNr = 0;
        p = 0;
        for (int i = 0; i < sSize; i++) {

            char c = s.charAt(i);
            if (c == '(') {
                p++;
            } else if (c == ')') {
                p--;
            } else if (p == 0 && c == sep) {
                retArr[pNr++] = s.substring(st, i).trim();
                st = i + 1;
                while (disregardDoubles && i < sSize - 1 && s.charAt(i + 1) == ' ') {
                    i++;
                }
            }
        }

        retArr[pNr++] = s.substring(st, sSize).trim();
        return retArr;
    }

    /**
     * Parses "AAA[BBB]CCC[DDD]EEE" into {"AAA", "BBB", "CCC", "DDD", "EEE",
     * "FFF"}. Handles empty parts. Will always start and end outside a [] block
     * so that the number of returned elemets will always be uneven and at least
     * of length 3.
     * <p>
     * "|" is interpreted as "][".
     *
     * @param s The string. Might be "" but not null. Should be trimmed.
     * @return The string divided into elements. Never <code>null</code> and at
     * least of length 3.
     * @throws IllegalArgumentException If a [] mismatch of some kind. (If not
     * same [ as ] count or if the interleave.)
     */
    private static ArrayList<String> getRowColAndGapsTrimmed(String s) {
        if (s.indexOf('|') != -1) {
            s = StringUtil.replaceAll(s, "\\|", "][");
        }

        ArrayList<String> retList = new ArrayList<String>(Math.max(s.length() >> 2 + 1, 3)); // Approx return length.
        int s0 = 0, s1 = 0; // '[' and ']' count.
        int st = 0; // Start of "next token to add".
        for (int i = 0, iSz = s.length(); i < iSz; i++) {
            char c = s.charAt(i);
            if (c == '[') {
                s0++;
            } else if (c == ']') {
                s1++;
            } else {
                continue;
            }

            if (s0 != s1 && (s0 - 1) != s1) {
                break;  // Wrong [ or ] found. Break for throw.
            }
            retList.add(s.substring(st, i).trim());
            st = i + 1;
        }
        if (s0 != s1) {
            throw new IllegalArgumentException("'[' and ']' mismatch in row/column format string: " + s);
        }

        if (s0 == 0) {
            retList.add("");
            retList.add(s);
            retList.add("");
        } else if (retList.size() % 2 == 0) {
            retList.add(s.substring(st, s.length()));
        }

        return retList;
    }

    /**
     * Makes <code>null</code> "", trims and converts to lower case.
     *
     * @param s The string
     * @return Not null.
     */
    public static String prepare(String s) {
        return s != null ? s.trim().toLowerCase() : "";
    }

//	/** Tests to serialize and deserialize the object with both XMLEncoder/Decoder and through Serializable
//	 * @param o The object to serialize
//	 * @return The same object after a tri through the process.
//	 */
//	public static final Object serializeTest(Object o)
//	{
//		try {
//			ByteArrayOutputStream barr = new ByteArrayOutputStream();
//			XMLEncoder enc = new XMLEncoder(barr);
//			enc.writeObject(o);
//			enc.close();
//
//			XMLDecoder dec = new XMLDecoder(new ByteArrayInputStream(barr.toByteArray()));
//			o = dec.readObject();
//			dec.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		try {
//			ByteArrayOutputStream barr = new ByteArrayOutputStream();
//			ObjectOutputStream oos = new ObjectOutputStream(barr);
//			oos.writeObject(o);
//			oos.close();
//
//			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(barr.toByteArray()));
//			o = ois.readObject();
//			ois.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return o;
//	}
}
