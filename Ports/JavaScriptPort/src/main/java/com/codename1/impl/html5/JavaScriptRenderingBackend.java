/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

import com.codename1.teavm.jso.io.Blob;
import java.io.IOException;
import com.codename1.html5.js.canvas.CanvasPattern;
import com.codename1.html5.js.canvas.CanvasRenderingContext2D;
import com.codename1.html5.js.canvas.ImageData;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLImageElement;

public interface JavaScriptRenderingBackend {
    HTMLCanvasElement createCanvas(int width, int height);
    HTMLImageElement createImageElement();
    HTMLImageElement createCrossOriginImageElement(String sourceUrl);
    HTMLImageElement createBlobImageElement(Blob blob);
    HTML5Graphics createGraphics(HTML5Implementation implementation, HTMLCanvasElement canvas);
    CanvasRenderingContext2D getContext(HTMLCanvasElement canvas);
    void drawLoadedImage(CanvasRenderingContext2D context, HTMLImageElement image, int x, int y, int width, int height);
    void drawMutableSurface(CanvasRenderingContext2D context, HTMLCanvasElement canvas, int x, int y, int width, int height);
    CanvasPattern createLoadedImagePattern(CanvasRenderingContext2D context, HTMLImageElement image);
    CanvasPattern createMutableSurfacePattern(CanvasRenderingContext2D context, HTMLCanvasElement canvas);
    ImageData readLoadedImageData(HTMLImageElement image, int x, int y, int width, int height);
    ImageData readMutableSurfaceData(HTMLCanvasElement canvas, int x, int y, int width, int height);
    void writeImageData(HTMLCanvasElement canvas, ImageData imageData, int width, int height);
    void scaleLoadedImageToCanvas(HTMLCanvasElement canvas, HTMLImageElement image, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight);
    void scaleMutableSurfaceToCanvas(HTMLCanvasElement canvas, HTMLCanvasElement sourceCanvas, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight);
    Blob toImageBlob(HTMLCanvasElement canvas, String mimeType, float quality) throws IOException;
    void repaintCurrentForm();
}
