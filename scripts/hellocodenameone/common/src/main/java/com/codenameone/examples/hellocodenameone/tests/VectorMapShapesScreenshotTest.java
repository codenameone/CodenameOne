package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.Circle;
import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.Polygon;
import com.codename1.maps.Polyline;
import com.codename1.maps.vector.DemoTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Exercises polyline, polygon and circle overlays on the vector map.
public class VectorMapShapesScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        if (com.codename1.ui.CN.isWatch()) {
            // The watch form factor has no committed map goldens; the map
            // coverage runs on phone/tablet form factors instead.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapShapes status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Shapes", new BorderLayout(), "VectorMapShapes");
        MapView map = new MapView(new DemoTileSource(), MapStyle.light());
        map.moveCamera(new LatLng(0, 0), 5);

        Polyline line = new Polyline();
        line.addPoint(new LatLng(1.0, -1.5)).addPoint(new LatLng(0.0, 0.0)).addPoint(new LatLng(-1.0, 1.5));
        line.setStrokeColor(0xff5722).setStrokeWidth(6);
        map.addPolyline(line);

        Polygon poly = new Polygon();
        poly.addPoint(new LatLng(0.6, 0.4)).addPoint(new LatLng(0.6, 1.2))
                .addPoint(new LatLng(0.1, 1.2)).addPoint(new LatLng(0.1, 0.4));
        poly.setFillColor(0x803f51b5).setStrokeColor(0x3f51b5).setStrokeWidth(3);
        map.addPolygon(poly);

        Circle circle = new Circle(new LatLng(-0.6, -0.6), 60000);
        circle.setFillColor(0x804caf50).setStrokeColor(0x4caf50).setStrokeWidth(3);
        map.addCircle(circle);

        form.add(BorderLayout.CENTER, map);
        form.show();
        return true;
    }
}
