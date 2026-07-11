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
package com.codename1.surfaces;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers the pure logic of {@link SurfaceRasterizer} that runs without a platform Display:
 * timeline entry selection and flip scheduling, per-size layout lookup, dynamic-text formatting,
 * placeholder interpolation and wire color resolution. Pixel output is exercised manually through
 * the simulator's Widgets preview window.
 */
class SurfaceRasterizerTest {

    // numbers deliberately mix Long and Double, matching what JSONParser may hand the rasterizer
    private static Map<String, Object> timelineDoc() {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        List<Object> entries = new ArrayList<Object>();
        entries.add(entry(Long.valueOf(1000L), "first"));
        entries.add(entry(Double.valueOf(2000d), "second"));
        entries.add(entry(Long.valueOf(3000L), "third"));
        doc.put("entries", entries);
        return doc;
    }

    private static Map<String, Object> entry(Object date, String marker) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("date", date);
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("marker", marker);
        e.put("state", state);
        return e;
    }

    @SuppressWarnings("unchecked")
    private static String markerOf(Map<String, Object> entry) {
        return (String) ((Map<String, Object>) entry.get("state")).get("marker");
    }

    // --- currentEntry / nextEntryFlip -------------------------------------------

    @Test
    void currentEntryPicksTheMostRecentlyPassedEntry() {
        Map<String, Object> doc = timelineDoc();
        assertEquals("first", markerOf(SurfaceRasterizer.currentEntry(doc, 1500)));
        assertEquals("second", markerOf(SurfaceRasterizer.currentEntry(doc, 2500)));
        assertEquals("third", markerOf(SurfaceRasterizer.currentEntry(doc, 99999)));
    }

    @Test
    void currentEntryFallsBackToTheFirstEntryBeforeAnyDatePassed() {
        assertEquals("first", markerOf(SurfaceRasterizer.currentEntry(timelineDoc(), 500)));
    }

    @Test
    void currentEntryReturnsNullWithoutEntries() {
        assertNull(SurfaceRasterizer.currentEntry(new LinkedHashMap<String, Object>(), 500));
        assertNull(SurfaceRasterizer.currentEntry(null, 500));
    }

    @Test
    void nextEntryFlipReturnsTheEarliestFutureEntry() {
        Map<String, Object> doc = timelineDoc();
        assertEquals(1000, SurfaceRasterizer.nextEntryFlip(doc, 500));
        assertEquals(2000, SurfaceRasterizer.nextEntryFlip(doc, 1500));
        assertEquals(3000, SurfaceRasterizer.nextEntryFlip(doc, 2000));
    }

    @Test
    void nextEntryFlipReturnsZeroWhenNoEntryIsAhead() {
        assertEquals(0, SurfaceRasterizer.nextEntryFlip(timelineDoc(), 3000));
        assertEquals(0, SurfaceRasterizer.nextEntryFlip(null, 0));
    }

    // --- layoutForSize -------------------------------------------------------------

    @Test
    void layoutForSizePrefersTheExplicitSizeAndFallsBackToDefault() {
        Map<String, Object> defaultLayout = new LinkedHashMap<String, Object>();
        defaultLayout.put("t", "col");
        Map<String, Object> smallLayout = new LinkedHashMap<String, Object>();
        smallLayout.put("t", "row");
        Map<String, Object> layouts = new LinkedHashMap<String, Object>();
        layouts.put("default", defaultLayout);
        layouts.put("small", smallLayout);
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("layouts", layouts);

        assertSame(smallLayout, SurfaceRasterizer.layoutForSize(doc, "small"));
        assertSame(defaultLayout, SurfaceRasterizer.layoutForSize(doc, "medium"));
        assertSame(defaultLayout, SurfaceRasterizer.layoutForSize(doc, null));
    }

    @Test
    void layoutForSizeReturnsNullWhenAbsent() {
        assertNull(SurfaceRasterizer.layoutForSize(null, "small"));
        assertNull(SurfaceRasterizer.layoutForSize(new LinkedHashMap<String, Object>(), "small"));
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("layouts", new LinkedHashMap<String, Object>());
        assertNull(SurfaceRasterizer.layoutForSize(doc, "small"));
    }

    // --- dynamic text ------------------------------------------------------------------

    @Test
    void timerDownFormatsMinutesSecondsAndClampsAtZero() {
        long now = 1000000L;
        assertEquals("1:05", SurfaceRasterizer.formatDynamicText("timerDown", now + 65000, now));
        assertEquals("0:00", SurfaceRasterizer.formatDynamicText("timerDown", now - 5000, now));
        assertEquals("1:01:01",
                SurfaceRasterizer.formatDynamicText("timerDown", now + 3661000, now));
    }

    @Test
    void timerUpCountsElapsedTime() {
        long now = 1000000L;
        assertEquals("1:05", SurfaceRasterizer.formatDynamicText("timerUp", now - 65000, now));
        assertEquals("0:00", SurfaceRasterizer.formatDynamicText("timerUp", now + 5000, now));
    }

    @Test
    void timeStyleRendersTwelveHourClock() {
        assertEquals("9:41 AM", SurfaceRasterizer.formatDynamicText("time", at(9, 41), 0));
        assertEquals("12:05 AM", SurfaceRasterizer.formatDynamicText("time", at(0, 5), 0));
        assertEquals("3:07 PM", SurfaceRasterizer.formatDynamicText("time", at(15, 7), 0));
    }

    @Test
    void dateStyleRendersMonthAndDay() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.JUNE, 3, 12, 0, 0);
        assertEquals("Jun 3",
                SurfaceRasterizer.formatDynamicText("date", c.getTimeInMillis(), 0));
    }

    @Test
    void relativeStyleUsesMinuteHourDayGranularity() {
        long now = 1000000000L;
        assertEquals("in 5 min",
                SurfaceRasterizer.formatDynamicText("relative", now + 5 * 60000L, now));
        assertEquals("5 min ago",
                SurfaceRasterizer.formatDynamicText("relative", now - 5 * 60000L, now));
        assertEquals("in 2 hr",
                SurfaceRasterizer.formatDynamicText("relative", now + 2 * 3600000L, now));
        assertEquals("in 1 day",
                SurfaceRasterizer.formatDynamicText("relative", now + 25 * 3600000L, now));
        assertEquals("3 days ago",
                SurfaceRasterizer.formatDynamicText("relative", now - 72 * 3600000L, now));
        assertEquals("now", SurfaceRasterizer.formatDynamicText("relative", now + 30000, now));
    }

    private static long at(int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.JANUARY, 15, hour, minute, 0);
        return c.getTimeInMillis();
    }

    // --- interpolation --------------------------------------------------------------------

    @Test
    void interpolateReplacesPlaceholdersFromState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("status", "Out for delivery");
        state.put("stops", Integer.valueOf(3));
        assertEquals("Out for delivery - 3 stops",
                SurfaceRasterizer.interpolate("${status} - ${stops} stops", state));
    }

    @Test
    void interpolateRendersMissingKeysAsEmpty() {
        assertEquals("Order ", SurfaceRasterizer.interpolate("Order ${missing}",
                new HashMap<String, Object>()));
        assertEquals("Order ", SurfaceRasterizer.interpolate("Order ${missing}", null));
    }

    @Test
    void interpolatePassesPlainTextThrough() {
        assertEquals("plain", SurfaceRasterizer.interpolate("plain", null));
        assertEquals("open ${", SurfaceRasterizer.interpolate("open ${", null));
    }

    // --- vector angle conversion and transforms -------------------------------------------

    /** A point at clock angle d sits at (cos(clockRadians(d)), sin(clockRadians(d))). */
    private static double[] clockUnitVector(double deg) {
        double rad = SurfaceRasterizer.clockRadians(deg);
        return new double[] {Math.cos(rad), Math.sin(rad)};
    }

    @Test
    void clockAngleZeroPointsUpAndAdvancesClockwise() {
        double[] up = clockUnitVector(0);
        assertEquals(0.0, up[0], 1e-9);
        assertEquals(-1.0, up[1], 1e-9);
        double[] right = clockUnitVector(90);
        assertEquals(1.0, right[0], 1e-9);
        assertEquals(0.0, right[1], 1e-9);
        double[] down = clockUnitVector(180);
        assertEquals(0.0, down[0], 1e-9);
        assertEquals(1.0, down[1], 1e-9);
        double[] left = clockUnitVector(270);
        assertEquals(-1.0, left[0], 1e-9);
        assertEquals(0.0, left[1], 1e-9);
    }

    @Test
    void clockAnglesConvertToCn1ArcConvention() {
        // CN1 Graphics arcs: 0 = 3 o'clock, counterclockwise positive
        assertEquals(90, SurfaceRasterizer.cn1ArcStartDegrees(0));
        assertEquals(0, SurfaceRasterizer.cn1ArcStartDegrees(90));
        assertEquals(-90, SurfaceRasterizer.cn1ArcStartDegrees(180));
        assertEquals(-180, SurfaceRasterizer.cn1ArcStartDegrees(270));
        // a clockwise sweep is a negative CN1 sweep
        assertEquals(-90, SurfaceRasterizer.cn1ArcSweepDegrees(90));
        assertEquals(360, SurfaceRasterizer.cn1ArcSweepDegrees(-360));
    }

    @Test
    void composedRotationSpinsPointsClockwiseAroundThePivot() {
        double[] identity = new double[] {1, 0, 0, 1, 0, 0};
        // 90 degrees clockwise around (100, 100): the 12 o'clock point moves to 3 o'clock
        double[] rot = SurfaceRasterizer.composeRotation(identity, 90, 100, 100);
        double[] p = SurfaceRasterizer.transformPoint(rot, 100, 40);
        assertEquals(160.0, p[0], 1e-6);
        assertEquals(100.0, p[1], 1e-6);
        // the pivot itself is a fixed point
        double[] pivot = SurfaceRasterizer.transformPoint(rot, 100, 100);
        assertEquals(100.0, pivot[0], 1e-6);
        assertEquals(100.0, pivot[1], 1e-6);
        // nested rotations compose: two 90s make a 180
        double[] twice = SurfaceRasterizer.composeRotation(rot, 90, 100, 100);
        double[] p2 = SurfaceRasterizer.transformPoint(twice, 100, 40);
        assertEquals(100.0, p2[0], 1e-6);
        assertEquals(160.0, p2[1], 1e-6);
    }

    @Test
    void rotationComposesUnderScaleAndOffset() {
        // the view-box mapping of a 200x200 view box into a 100x100 target at offset (10, 20)
        double[] base = new double[] {0.5, 0, 0, 0.5, 10, 20};
        double[] rot = SurfaceRasterizer.composeRotation(base, 90, 100, 100);
        // pivot maps to the same pixel with or without rotation
        double[] pivotPlain = SurfaceRasterizer.transformPoint(base, 100, 100);
        double[] pivotRot = SurfaceRasterizer.transformPoint(rot, 100, 100);
        assertEquals(pivotPlain[0], pivotRot[0], 1e-6);
        assertEquals(pivotPlain[1], pivotRot[1], 1e-6);
        // a hand pointing up rotates to point right, scaled and offset
        double[] p = SurfaceRasterizer.transformPoint(rot, 100, 40);
        assertEquals(10 + 160 * 0.5, p[0], 1e-6);
        assertEquals(20 + 100 * 0.5, p[1], 1e-6);
    }

    // --- colors --------------------------------------------------------------------------

    @Test
    void resolveColorPicksLightOrDarkArm() {
        Map<String, Object> color = new LinkedHashMap<String, Object>();
        // JSONParser hands numbers back as Double/Long; both arms must resolve
        color.put("l", Double.valueOf(0xff112233L));
        color.put("d", Long.valueOf(0xff445566L));
        assertEquals(0xff112233, SurfaceRasterizer.resolveColor(color, false, 0));
        assertEquals(0xff445566, SurfaceRasterizer.resolveColor(color, true, 0));
    }

    @Test
    void resolveColorResolvesSemanticRoles() {
        Map<String, Object> label = new LinkedHashMap<String, Object>();
        label.put("role", "label");
        assertEquals(0xff1c1c1e, SurfaceRasterizer.resolveColor(label, false, 0));
        assertEquals(0xffffffff, SurfaceRasterizer.resolveColor(label, true, 0));
        Map<String, Object> accent = new LinkedHashMap<String, Object>();
        accent.put("role", "accent");
        assertEquals(0xff007aff, SurfaceRasterizer.resolveColor(accent, false, 0));
        assertEquals(0xff007aff, SurfaceRasterizer.resolveColor(accent, true, 0));
    }

    @Test
    void resolveColorFallsBackForMissingInput() {
        assertEquals(0xffabcdef, SurfaceRasterizer.resolveColor(null, false, 0xffabcdef));
        assertEquals(0xffabcdef, SurfaceRasterizer.resolveColor("nonsense", true, 0xffabcdef));
    }
}
