package com.codename1.flutter;

/**
 * The drawing surface handed to a CustomPainter — Flutter's dart:ui
 * {@code Canvas}. This milestone provides the API surface (so painters can be
 * transpiled and their draw/transform calls resolve); the concrete backend
 * that binds these calls to a Codename One {@code Graphics} is supplied by the
 * render layer.
 */
public class Canvas {

    public void drawPath(Path path, Paint paint) {
    }

    public void drawRect(Rect rect, Paint paint) {
    }

    public void drawRRect(RRect rrect, Paint paint) {
    }

    public void drawCircle(Offset c, double radius, Paint paint) {
    }

    public void drawOval(Rect rect, Paint paint) {
    }

    public void drawLine(Offset p1, Offset p2, Paint paint) {
    }

    public void drawArc(Rect rect, double startAngle, double sweepAngle, boolean useCenter, Paint paint) {
    }

    public void drawPoints(Object pointMode, Object points, Paint paint) {
    }

    public void drawColor(Color color, Object blendMode) {
    }

    public void drawShadow(Path path, Color color, double elevation, boolean transparentOccluder) {
    }

    public void drawVertices(Object vertices, Object blendMode, Paint paint) {
    }

    public void drawImage(Object image, Offset offset, Paint paint) {
    }

    public void translate(double dx, double dy) {
    }

    public void scale(double sx, double sy) {
    }

    public void rotate(double radians) {
    }

    public void skew(double sx, double sy) {
    }

    public void save() {
    }

    public void saveLayer(Rect bounds, Paint paint) {
    }

    public void restore() {
    }

    public void clipRect(Rect rect) {
    }

    public void clipRRect(RRect rrect) {
    }

    public void clipPath(Path path) {
    }
}
