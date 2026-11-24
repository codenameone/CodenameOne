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

/// Port of `Image`: draws an [ImageAsset] at its world transform, offset by
/// its origin, or deformed by a child [Mesh].
public class Image extends ImageBase {
    private final Mat2D originMatrix = new Mat2D();
    private ImageAsset asset;
    private Mesh mesh;
    private float slotWidth = Float.NaN;
    private float slotHeight = Float.NaN;
    private float layoutScaleX = 1;
    private float layoutScaleY = 1;
    private float layoutOffsetX;
    private float layoutOffsetY;

    void setMesh(Mesh m) {
        mesh = m;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public ImageAsset getImageAsset() {
        return asset;
    }

    @Override
    public int onAddedDirty(Artboard context) {
        int code = super.onAddedDirty(context);
        if (code != STATUS_OK) {
            return code;
        }
        resolveAsset(context);
        return STATUS_OK;
    }

    private void resolveAsset(Artboard context) {
        asset = null;
        RiveFileContext file = context != null ? context.getFile() : null;
        if (file != null) {
            FileAsset a = file.getAsset(mAssetId);
            if (a instanceof ImageAsset) {
                asset = (ImageAsset) a;
            }
        }
    }

    @Override
    protected void assetIdChanged() {
        if (artboard != null) {
            resolveAsset(artboard);
            addDirt(DIRT_PAINT);
        }
    }

    @Override
    protected void originXChanged() {
        addDirt(DIRT_PAINT);
    }

    @Override
    protected void originYChanged() {
        addDirt(DIRT_PAINT);
    }

    /// Port of `Image::willDraw`.
    public boolean willDraw() {
        return !isHidden() && renderOpacity != 0f && asset != null;
    }

    @Override
    public void draw(RiveRenderer renderer) {
        if (!willDraw()) {
            return;
        }
        Object img = asset.getRenderImage();
        if (img == null) {
            return;
        }
        boolean clipped = clip(renderer);
        renderer.save();
        if (mesh != null && mesh.hasTriangles()) {
            mesh.draw(renderer, img, mBlendModeValue, renderOpacity);
        } else {
            renderer.transform(worldTransform);
            originMatrix.set(1, 0, 0, 1, -asset.getImageWidth() * mOriginX, -asset.getImageHeight() * mOriginY);
            renderer.transform(originMatrix);
            renderer.drawImage(img, mBlendModeValue, renderOpacity);
        }
        renderer.restore();
        if (clipped) {
            renderer.restore();
        }
    }

    /// Port of `Image::measureLayout`: hugs the image's own size; only an
    /// exact constraint overrides it.
    void measureLayout(float width, int widthMode, float height, int heightMode, float[] out) {
        out[0] = widthMode == FlexNode.MEASURE_EXACTLY ? width : asset == null ? 0 : asset.getImageWidth();
        out[1] = heightMode == FlexNode.MEASURE_EXACTLY ? height : asset == null ? 0 : asset.getImageHeight();
    }

    /// Port of `Image::controlSize`.
    void controlSize(float width, float height) {
        if (Float.floatToIntBits(width) != Float.floatToIntBits(slotWidth)
                || Float.floatToIntBits(height) != Float.floatToIntBits(slotHeight)) {
            slotWidth = width;
            slotHeight = height;
            updateImageScale();
        }
    }

    /// Port of `Image::updateImageScale` (the separate fit scale of 7.2+
    /// files): fit and alignment of the image inside the layout's box.
    private void updateImageScale() {
        float ox = 0;
        float oy = 0;
        float sx = layoutScaleX;
        float sy = layoutScaleY;
        if (asset != null && asset.getImageWidth() > 0 && asset.getImageHeight() > 0
                && !Float.isNaN(slotWidth) && !Float.isNaN(slotHeight)) {
            float iw = asset.getImageWidth();
            float ih = asset.getImageHeight();
            switch (mFit) {
                case 1:
                    sx = Math.min(slotWidth / iw, slotHeight / ih);
                    sy = sx;
                    break;
                case 2:
                    sx = Math.max(slotWidth / iw, slotHeight / ih);
                    sy = sx;
                    break;
                case 3:
                    sx = slotWidth / iw;
                    sy = sx;
                    break;
                case 4:
                    sx = slotHeight / ih;
                    sy = sx;
                    break;
                case 5:
                    sx = 1;
                    sy = 1;
                    break;
                case 6:
                    sx = Math.min(1, Math.min(slotWidth / iw, slotHeight / ih));
                    sy = sx;
                    break;
                default:
                    sx = slotWidth / iw;
                    sy = slotHeight / ih;
                    break;
            }
            if (mFit != 0) {
                float left = -iw * mOriginX;
                float top = -ih * mOriginY;
                if (mesh != null) {
                    left = -iw * 0.5f;
                    top = -ih * 0.5f;
                }
                ox = -left * sx + (slotWidth - iw * sx) * (mAlignmentX + 1) * 0.5f;
                oy = -top * sy + (slotHeight - ih * sy) * (mAlignmentY + 1) * 0.5f;
            }
        }
        if (Float.floatToIntBits(sx) != Float.floatToIntBits(layoutScaleX)
                || Float.floatToIntBits(sy) != Float.floatToIntBits(layoutScaleY)
                || Float.floatToIntBits(ox) != Float.floatToIntBits(layoutOffsetX)
                || Float.floatToIntBits(oy) != Float.floatToIntBits(layoutOffsetY)) {
            layoutScaleX = sx;
            layoutScaleY = sy;
            layoutOffsetX = ox;
            layoutOffsetY = oy;
            markTransformDirty();
        }
    }

    /// Port of `Image::updateTransform`: the fit scale composes innermost,
    /// the fit offset outermost.
    @Override
    public void updateTransform() {
        super.updateTransform();
        float[] m = transform.v;
        m[0] *= layoutScaleX;
        m[1] *= layoutScaleX;
        m[2] *= layoutScaleY;
        m[3] *= layoutScaleY;
        m[4] += layoutOffsetX;
        m[5] += layoutOffsetY;
    }
}
