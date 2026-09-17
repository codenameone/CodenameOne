/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.maps;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.maps.vector.MapStyle;
import com.codename1.maps.vector.TileCallback;
import com.codename1.maps.vector.TileSource;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Stroke;
import com.codename1.ui.geom.Shape;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MapViewMarkerLabelTest extends UITestBase {
    private TestCodenameOneImplementation drawing;
    @FormTest
    void persistentLabelsAreDrawnAfterTheRouteAndCustomPins() {
        MapView map = map();
        EncodedImage icon = icon();
        map.addPolyline(new Polyline().addPoint(new LatLng(0, -1)).addPoint(new LatLng(0, 1)));
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon).label("Museum"));
        Graphics graphics = graphics();
        map.paintBackground(graphics);
        InOrder order = inOrder(drawing);
        order.verify(drawing).drawShape(any(), any(Shape.class), any(Stroke.class));
        order.verify(drawing).drawString(any(), eq("Museum"), anyInt(), anyInt());
    }

    @FormTest
    void hidingRemovingAndPanningAwayAlsoHideTheLabel() {
        MapView map = map();
        Marker marker = map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon()).label("Home"));
        marker.setVisible(false);
        Graphics graphics = graphics();
        map.paintBackground(graphics);
        verify(drawing, never()).drawString(any(), eq("Home"), anyInt(), anyInt());
        marker.setVisible(true);
        map.moveCamera(new LatLng(0, 150), 10);
        graphics = graphics();
        map.paintBackground(graphics);
        verify(drawing, never()).drawString(any(), eq("Home"), anyInt(), anyInt());
        map.moveCamera(new LatLng(0, 0), 10);
        graphics = graphics();
        map.paintBackground(graphics);
        verify(drawing).drawString(any(), eq("Home"), anyInt(), anyInt());
        map.removeMarker(marker);
        graphics = graphics();
        map.paintBackground(graphics);
        verify(drawing, never()).drawString(any(), eq("Home"), anyInt(), anyInt());
    }

    @FormTest
    void longLabelsStayInsideTheViewportWithoutChangingTheName() {
        MapView map = map();
        String name = "A very long place name that cannot fit inside this small map viewport";
        Marker marker = map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon()).label(name));
        Graphics graphics = graphics();
        map.paintBackground(graphics);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> x = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> y = ArgumentCaptor.forClass(Integer.class);
        verify(drawing).drawString(any(), text.capture(), x.capture(), y.capture());
        assertTrue(text.getValue().endsWith("..."));
        assertTrue(x.getValue() >= 0);
        assertTrue(x.getValue() + Font.getDefaultFont().stringWidth(text.getValue()) <= map.getWidth());
        assertTrue(y.getValue() + Font.getDefaultFont().getHeight() <= map.getHeight());
        assertEquals(name, marker.getLabel());
    }

    @FormTest
    void roundTripEndpointsDoNotPaintTheirLabelsOnTopOfEachOther() {
        MapView map = map();
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon()).label("Start"));
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon()).label("Destination"));
        map.paintBackground(graphics());
        ArgumentCaptor<Integer> startY = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> destinationY = ArgumentCaptor.forClass(Integer.class);
        verify(drawing).drawString(any(), eq("Start"), anyInt(), startY.capture());
        verify(drawing).drawString(any(), eq("Destination"), anyInt(), destinationY.capture());
        assertTrue(Math.abs(startY.getValue() - destinationY.getValue()) >= Font.getDefaultFont().getHeight());
    }

    @FormTest
    void defaultPinLabelFlipsAboveTheBottomEdge() {
        MapView map = map();
        map.addMarker(new MarkerOptions(map.screenToLatLng(190, 155)).label("Destination"));
        Graphics graphics = graphics();
        map.paintBackground(graphics);
        ArgumentCaptor<Integer> x = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> y = ArgumentCaptor.forClass(Integer.class);
        verify(drawing).drawString(any(), eq("Destination"), x.capture(), y.capture());
        assertTrue(x.getValue() >= 0);
        assertTrue(x.getValue() + Font.getDefaultFont().stringWidth("Destination") <= map.getWidth());
        assertTrue(y.getValue() >= 0);
        assertTrue(y.getValue() + Font.getDefaultFont().getHeight() < 155);
    }

    @FormTest
    void infoWindowTitlesAndBlankLabelsDoNotBecomePersistentLabels() {
        MapView map = map();
        map.addMarker(new MarkerOptions(new LatLng(0, 0)).icon(icon()).title("Title only"));
        map.addMarker(new MarkerOptions(new LatLng(0, 1)).icon(icon()).label(" "));
        Graphics graphics = graphics();
        map.paintBackground(graphics);
        verify(drawing, never()).drawString(any(), anyString(), anyInt(), anyInt());
    }

    private Graphics graphics() {
        Graphics graphics = Image.createImage(200, 160).getGraphics();
        drawing = spy(implementation);
        drawing.setShapeSupported(true);
        try {
            java.lang.reflect.Field field = Graphics.class.getDeclaredField("impl");
            field.setAccessible(true);
            field.set(graphics, drawing);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return graphics;
    }

    private EncodedImage icon() {
        return EncodedImage.create(new byte[]{1, 2, 3}, 12, 16, true);
    }

    private MapView map() {
        TileSource source = new TileSource() {
            public boolean isVector() { return true; }
            public int getTileSize() { return 256; }
            public int getMinZoom() { return 0; }
            public int getMaxZoom() { return 18; }
            public String getAttribution() { return ""; }
            public void fetchTile(int z, int x, int y, TileCallback callback) {
                callback.tileFailed(z, x, y);
            }
        };
        MapView map = new MapView(source, new MapStyle("empty", 0xffeeeeee));
        map.setWidth(200);
        map.setHeight(160);
        map.moveCamera(new LatLng(0, 0), 5);
        return map;
    }
}
