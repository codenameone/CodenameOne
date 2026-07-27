/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the encoded polyline codec and the Polyline factory built on it. */
class PolylineCodecTest {

    /**
     * The worked example from Google's encoded polyline specification:
     * (38.5, -120.2), (40.7, -120.95), (43.252, -126.453).
     */
    private static final String GOOGLE_SAMPLE = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

    @Test
    void decodesTheSpecificationSample() {
        List points = PolylineCodec.decode(GOOGLE_SAMPLE);
        assertEquals(3, points.size());
        assertLatLng(38.5, -120.2, points.get(0));
        assertLatLng(40.7, -120.95, points.get(1));
        assertLatLng(43.252, -126.453, points.get(2));
    }

    @Test
    void encodesTheSpecificationSample() {
        List points = new ArrayList();
        points.add(new LatLng(38.5, -120.2));
        points.add(new LatLng(40.7, -120.95));
        points.add(new LatLng(43.252, -126.453));
        assertEquals(GOOGLE_SAMPLE, PolylineCodec.encode(points));
    }

    @Test
    void roundTripsAtBothPrecisions() {
        List points = new ArrayList();
        points.add(new LatLng(38.897700, -77.036500));
        points.add(new LatLng(38.889400, -77.035200));
        points.add(new LatLng(-33.866000, 151.195000));

        List five = PolylineCodec.decode(PolylineCodec.encode(points, 5), 5);
        List six = PolylineCodec.decode(PolylineCodec.encode(points, 6), 6);
        assertEquals(points.size(), five.size());
        assertEquals(points.size(), six.size());
        for (int i = 0; i < points.size(); i++) {
            LatLng expected = (LatLng) points.get(i);
            assertLatLng(expected.getLatitude(), expected.getLongitude(), five.get(i));
            assertLatLng(expected.getLatitude(), expected.getLongitude(), six.get(i));
        }
    }

    @Test
    void precisionSixIsTenTimesFinerThanPrecisionFive() {
        // Decoding a polyline6 geometry as precision 5 is the classic mistake;
        // it must move the coordinate by exactly a factor of ten, not throw.
        List points = new ArrayList();
        points.add(new LatLng(4.5, 5.5));
        String encoded = PolylineCodec.encode(points, 6);
        assertLatLng(45.0, 55.0, PolylineCodec.decode(encoded, 5).get(0));
    }

    @Test
    void toleratesNullEmptyAndTruncatedInput() {
        assertTrue(PolylineCodec.decode(null).isEmpty());
        assertTrue(PolylineCodec.decode("").isEmpty());
        assertEquals("", PolylineCodec.encode(null));

        // A geometry cut in half mid-response yields the complete coordinates
        // it did contain rather than blowing up.
        List truncated = PolylineCodec.decode("_p~iF~ps|U_ulL");
        assertEquals(1, truncated.size());
        assertLatLng(38.5, -120.2, truncated.get(0));
    }

    @Test
    void dropsAValueCutInHalfRatherThanEmittingABogusPoint() {
        // Losing the last byte truncates the final longitude mid-value. The
        // accumulated bits are a fraction of the real delta, so emitting them
        // would draw a spurious segment and widen the route bounds; the point
        // has to be dropped instead.
        String cut = GOOGLE_SAMPLE.substring(0, GOOGLE_SAMPLE.length() - 1);
        List points = PolylineCodec.decode(cut);
        assertEquals(2, points.size());
        assertLatLng(38.5, -120.2, points.get(0));
        assertLatLng(40.7, -120.95, points.get(1));
    }

    @Test
    void rejectsAPrecisionThatCannotScaleCoordinates() {
        // Precision 0 or less leaves the scale at 1 and inflates every
        // coordinate; a huge one overflows the scale and collapses them to
        // zero. Both used to decode silently into a map of wrong places.
        assertThrows(IllegalArgumentException.class, () -> PolylineCodec.decode(GOOGLE_SAMPLE, 0));
        assertThrows(IllegalArgumentException.class, () -> PolylineCodec.decode(GOOGLE_SAMPLE, -5));
        assertThrows(IllegalArgumentException.class, () -> PolylineCodec.decode(GOOGLE_SAMPLE, 400));
        assertThrows(IllegalArgumentException.class,
                () -> PolylineCodec.encode(new ArrayList(), 0));
        assertThrows(IllegalArgumentException.class, () -> Polyline.fromEncoded(GOOGLE_SAMPLE, 0));

        // The check runs before the empty-input short-circuit, so a bad
        // argument fails the same way whatever the payload.
        assertThrows(IllegalArgumentException.class, () -> PolylineCodec.decode(null, 0));
        assertThrows(IllegalArgumentException.class, () -> PolylineCodec.encode(null, 0));

        // The precisions services actually emit stay accepted.
        assertEquals(3, PolylineCodec.decode(GOOGLE_SAMPLE, 5).size());
        assertEquals(3, PolylineCodec.decode(GOOGLE_SAMPLE, 6).size());
        assertEquals(3, PolylineCodec.decode(GOOGLE_SAMPLE, 7).size());
    }

    @Test
    void polylineFactoryDecodesGeometry() {
        Polyline pl = Polyline.fromEncoded(GOOGLE_SAMPLE);
        assertEquals(3, pl.getPoints().size());
        assertLatLng(38.5, -120.2, pl.getPoints().get(0));

        assertTrue(Polyline.fromEncoded(null).getPoints().isEmpty());
    }

    private static void assertLatLng(double lat, double lon, Object actual) {
        LatLng p = (LatLng) actual;
        assertEquals(lat, p.getLatitude(), 1e-6);
        assertEquals(lon, p.getLongitude(), 1e-6);
    }
}
