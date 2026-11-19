/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

/// Port of `ImageAsset`: an image of the file. Its bytes are decoded into a
/// `com.codename1.ui.Image` the first time something draws it, never while
/// the file loads, and the result is kept.
public class ImageAsset extends ImageAssetBase {
    private Object renderImage;
    private boolean decodeFailed;

    @Override
    public void decode(byte[] bytes) {
        super.decode(bytes);
        renderImage = null;
        decodeFailed = false;
    }

    /// Supplies an already decoded image (an application-provided asset).
    public void setRenderImage(Object image) {
        renderImage = image;
        decodeFailed = false;
    }

    /// The decoded image, decoding the embedded bytes on first use; null
    /// when there are none or they are not an image the platform reads.
    public Object getRenderImage() {
        if (renderImage == null && !decodeFailed && contents != null && contents.length > 0) {
            try {
                renderImage = com.codename1.ui.Image.createImage(contents, 0, contents.length);
            } catch (RuntimeException e) {
                renderImage = null;
            }
            if (renderImage == null) {
                decodeFailed = true;
            }
        }
        return renderImage;
    }

    /// Width of the decoded image, 0 when there is none.
    public float getImageWidth() {
        Object img = getRenderImage();
        return img instanceof com.codename1.ui.Image ? ((com.codename1.ui.Image) img).getWidth() : 0f;
    }

    /// Height of the decoded image, 0 when there is none.
    public float getImageHeight() {
        Object img = getRenderImage();
        return img instanceof com.codename1.ui.Image ? ((com.codename1.ui.Image) img).getHeight() : 0f;
    }
}
