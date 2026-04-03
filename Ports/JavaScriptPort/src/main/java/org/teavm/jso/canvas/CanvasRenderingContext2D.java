/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.canvas;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public interface CanvasRenderingContext2D extends JSObject {
    void save();
    void restore();
    void scale(double x, double y);
    void translate(double x, double y);
    void rotate(double angle);
    void transform(double a, double b, double c, double d, double e, double f);
    void setTransform(double a, double b, double c, double d, double e, double f);
    void beginPath();
    void closePath();
    void moveTo(double x, double y);
    void lineTo(double x, double y);
    void quadraticCurveTo(double cpx, double cpy, double x, double y);
    void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y);
    void arc(double x, double y, double radius, double startAngle, double endAngle);
    void arc(double x, double y, double radius, double startAngle, double endAngle, boolean counterclockwise);
    void arcTo(double x1, double y1, double x2, double y2, double radius);
    void rect(double x, double y, double width, double height);
    void fill();
    void stroke();
    void fillRect(double x, double y, double w, double h);
    void strokeRect(double x, double y, double w, double h);
    void clearRect(double x, double y, double w, double h);
    void clip();
    void fillText(String text, double x, double y);
    void fillText(String text, double x, double y, double maxWidth);
    void strokeText(String text, double x, double y);
    void strokeText(String text, double x, double y, double maxWidth);
    TextMetrics measureText(String text);
    void drawImage(HTMLCanvasElement image, double dx, double dy);
    void drawImage(HTMLCanvasElement image, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLCanvasElement image, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLImageElement image, double dx, double dy);
    void drawImage(HTMLImageElement image, double dx, double dy, double dWidth, double dHeight);
    void drawImage(HTMLImageElement image, double sx, double sy, double sWidth, double sHeight, double dx, double dy, double dWidth, double dHeight);
    void setFillStyle(String style);
    void setFillStyle(CanvasGradient gradient);
    void setFillStyle(CanvasPattern pattern);
    String getFillStyle();
    void setStrokeStyle(String style);
    void setStrokeStyle(CanvasGradient gradient);
    void setStrokeStyle(CanvasPattern pattern);
    String getStrokeStyle();
    CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1);
    CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1);
    CanvasPattern createPattern(HTMLCanvasElement image, String repetition);
    CanvasPattern createPattern(HTMLImageElement image, String repetition);
    void setLineWidth(double width);
    double getLineWidth();
    void setLineCap(String lineCap);
    String getLineCap();
    void setLineJoin(String lineJoin);
    String getLineJoin();
    void setMiterLimit(double miterLimit);
    double getMiterLimit();
    void setGlobalAlpha(double alpha);
    double getGlobalAlpha();
    void setGlobalCompositeOperation(String operation);
    String getGlobalCompositeOperation();
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
    ImageData createImageData(int width, int height);
    ImageData createImageData(ImageData imageData);
    ImageData getImageData(double x, double y, double width, double height);
    void putImageData(ImageData imageData, double x, double y);
    void putImageData(ImageData imageData, double x, double y, double dirtyX, double dirtyY, double dirtyWidth, double dirtyHeight);
    Uint8ClampedArray getImageDataInt32();
    @JSBody(params = {"name"}, script = "return this[name]")
    Object get(String name);
}

public interface TextMetrics extends JSObject {
    double getWidth();
}

public interface ImageData extends JSObject {
    int getWidth();
    int getHeight();
    Uint8ClampedArray getData();
    Uint8ClampedArray getData();
}