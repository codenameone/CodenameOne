/*
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 * Licensed under the PolyForm Noncommercial License 1.0.0
 */
package com.codename1.html5.js.canvas;

import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLImageElement;

/**
 * Interface for the Canvas 2D rendering context.
 * https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D
 */
public interface CanvasRenderingContext2D extends JSObject {
    HTMLCanvasElement getCanvas();
    void save();
    void restore();
    void scale(double x, double y);
    void rotate(double angle);
    void translate(double x, double y);
    void transform(double a, double b, double c, double d, double e, double f);
    void setTransform(double a, double b, double c, double d, double e, double f);
    void setGlobalAlpha(double alpha);
    double getGlobalAlpha();
    void setGlobalCompositeOperation(String operation);
    String getGlobalCompositeOperation();
    void setFillStyle(String style);
    void setFillStyle(CanvasPattern pattern);
    void setFillStyle(CanvasGradient gradient);
    String getFillStyle();
    void setStrokeStyle(String style);
    void setStrokeStyle(CanvasPattern pattern);
    String getStrokeStyle();
    void setLineWidth(double width);
    double getLineWidth();
    void setLineCap(String cap);
    String getLineCap();
    void setLineJoin(String join);
    String getLineJoin();
    void setMiterLimit(double limit);
    double getMiterLimit();
    void setFont(String font);
    String getFont();
    void setTextAlign(String align);
    String getTextAlign();
    void setTextBaseline(String baseline);
    String getTextBaseline();
    void setShadowColor(String color);
    String getShadowColor();
    void setShadowBlur(double blur);
    double getShadowBlur();
    void setShadowOffsetX(double offset);
    double getShadowOffsetX();
    void setShadowOffsetY(double offset);
    double getShadowOffsetY();
    void clearRect(double x, double y, double width, double height);
    void fillRect(double x, double y, double width, double height);
    void strokeRect(double x, double y, double width, double height);
    void beginPath();
    void closePath();
    void moveTo(double x, double y);
    void lineTo(double x, double y);
    void quadraticCurveTo(double cpx, double cpy, double x, double y);
    void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);
    void arc(double x, double y, double radius, double startAngle, double endAngle);
    void arc(double x, double y, double radius, double startAngle, double endAngle, boolean counterclockwise);
    void arcTo(double x1, double y1, double x2, double y2, double radius);
    void ellipse(double x, double y, double radiusX, double radiusY, double rotation, double startAngle, double endAngle);
    void rect(double x, double y, double width, double height);
    void fill();
    void stroke();
    void clip();
    boolean isPointInPath(double x, double y);
    void fillText(String text, double x, double y);
    void fillText(String text, double x, double y, double maxWidth);
    void strokeText(String text, double x, double y);
    void strokeText(String text, double x, double y, double maxWidth);
    TextMetrics measureText(String text);
    ImageData createImageData(double width, double height);
    ImageData getImageData(double x, double y, double width, double height);
    void putImageData(ImageData imageData, double x, double y);
    void putImageData(ImageData imageData, double dx, double dy, double dirtyX, double dirtyY, double dirtyWidth, double dirtyHeight);
    void drawImage(HTMLImageElement image, double dx, double dy);
    void drawImage(HTMLImageElement image, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLImageElement image, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLCanvasElement canvas, double dx, double dy);
    void drawImage(HTMLCanvasElement canvas, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLCanvasElement canvas, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight);
    void drawImage(CanvasImageSource image, double dx, double dy, double dWidth, double dHeight);
    void setImageData(ImageData imageData);
    CanvasPattern createPattern(Object image, String repetition);
    CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1);
    CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1);
    void setLineDash(double[] segments);
    double[] getLineDash();
    void setLineDashOffset(double offset);
    double getLineDashOffset();
}
