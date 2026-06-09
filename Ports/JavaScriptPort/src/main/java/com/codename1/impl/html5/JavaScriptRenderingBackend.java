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
    void drawLoadedImage(CanvasRenderingContext2D context, HTMLImageElement image, int x, int y, int width, int height);
    CanvasPattern createLoadedImagePattern(CanvasRenderingContext2D context, HTMLImageElement image);
    Blob toImageBlob(HTMLCanvasElement canvas, String mimeType, float quality) throws IOException;
    void repaintCurrentForm();
}
