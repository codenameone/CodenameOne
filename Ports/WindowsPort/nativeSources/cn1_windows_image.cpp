/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * Image translation unit for the native Codename One Windows port.
 *
 * Color-format contract used throughout this file:
 *   - The Java side speaks Codename One's canonical 0xAARRGGBB ("ARGB") with a
 *     STRAIGHT (non-premultiplied) alpha. Every uint32_t stored in
 *     CN1Image.argb is in this layout, in native (little-endian) memory order.
 *   - Direct2D / WIC here use DXGI_FORMAT_B8G8R8A8 with PREMULTIPLIED alpha
 *     (GUID_WICPixelFormat32bppPBGRA / D2D1_ALPHA_MODE_PREMULTIPLIED). On a
 *     little-endian machine a PBGRA pixel laid out in memory as bytes
 *     {B,G,R,A} reads back as a uint32_t 0xAARRGGBB only after we account for
 *     byte order: memory {B,G,R,A} == uint32 (A<<24)|(R<<16)|(G<<8)|B, which is
 *     exactly 0xAARRGGBB. So PBGRA-in-memory and our ARGB-uint32 share the SAME
 *     32-bit value once alpha is un-/pre-multiplied. The only conversions we
 *     therefore perform are premultiply (ARGB -> PBGRA for upload) and
 *     unpremultiply (PBGRA -> ARGB on readback).
 */

#ifdef _WIN32

#include "cn1_windows.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* C++ unit (Direct2D is C++-only); keep C linkage for the bridge + helpers. */
extern "C" {

/* ------------------------------------------------------------ color helpers */

/*
 * Convert one straight-alpha 0xAARRGGBB pixel to a premultiplied uint32 whose
 * little-endian memory bytes are {B,G,R,A} as Direct2D/WIC PBGRA expects.
 */
static uint32_t cn1WinArgbToPbgra(uint32_t argb) {
    uint32_t a = (argb >> 24) & 0xFF;
    uint32_t r = (argb >> 16) & 0xFF;
    uint32_t g = (argb >> 8) & 0xFF;
    uint32_t b = argb & 0xFF;
    if (a == 0) {
        return 0;
    }
    if (a != 255) {
        r = (r * a + 127) / 255;
        g = (g * a + 127) / 255;
        b = (b * a + 127) / 255;
    }
    /* memory bytes {B,G,R,A} on little-endian == this uint32 value */
    return (a << 24) | (r << 16) | (g << 8) | b;
}

/*
 * Inverse of the above: a premultiplied PBGRA uint32 (memory {B,G,R,A}) back to
 * a straight-alpha 0xAARRGGBB pixel.
 */
static uint32_t cn1WinPbgraToArgb(uint32_t pbgra) {
    uint32_t a = (pbgra >> 24) & 0xFF;
    uint32_t r = (pbgra >> 16) & 0xFF;
    uint32_t g = (pbgra >> 8) & 0xFF;
    uint32_t b = pbgra & 0xFF;
    if (a == 0) {
        return 0;
    }
    if (a != 255) {
        r = (r * 255 + (a / 2)) / a;
        g = (g * 255 + (a / 2)) / a;
        b = (b * 255 + (a / 2)) / a;
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
    }
    return (a << 24) | (r << 16) | (g << 8) | b;
}

/* ---------------------------------------------------------- internal API */

/*
 * Wrap an in-memory 0xAARRGGBB pixel buffer in a CN1Image. We keep our own
 * malloc'd CPU copy in ->argb (used by getRGB and scaleImage); the GPU
 * ID2D1Bitmap is created lazily by cn1WinEnsureBitmap against whatever render
 * target is current when the image is first drawn.
 */
CN1Image* cn1WinImageFromArgb(const uint32_t* argb, int width, int height) {
    CN1Image* img;
    size_t pixels;
    if (width <= 0 || height <= 0) {
        return NULL;
    }
    img = (CN1Image*)malloc(sizeof(CN1Image));
    if (img == NULL) {
        return NULL;
    }
    img->bitmap = NULL;
    img->width = width;
    img->height = height;
    img->mutableGraphics = NULL;
    pixels = (size_t)width * (size_t)height;
    img->argb = (uint32_t*)malloc(pixels * sizeof(uint32_t));
    if (img->argb == NULL) {
        free(img);
        return NULL;
    }
    memcpy(img->argb, argb, pixels * sizeof(uint32_t));
    return img;
}

/*
 * Decode an encoded image (PNG/JPEG/...) via WIC into a 32bpp PBGRA frame, then
 * read its pixels into a straight-alpha 0xAARRGGBB CPU buffer stored in
 * ->argb. The GPU bitmap is created lazily later. Returns NULL on any failure.
 */
CN1Image* cn1WinDecodeImage(const BYTE* data, UINT32 len) {
    IWICStream* stream = NULL;
    IWICBitmapDecoder* decoder = NULL;
    IWICBitmapFrameDecode* frame = NULL;
    IWICFormatConverter* converter = NULL;
    IWICBitmapSource* source = NULL;
    HRESULT hr;
    UINT w = 0, h = 0;
    uint32_t* buffer = NULL;
    CN1Image* img = NULL;
    size_t pixels;
    UINT i;

    if (cn1Win.wicFactory == NULL || data == NULL || len == 0) {
        return NULL;
    }

    hr = IWICImagingFactory_CreateStream(cn1Win.wicFactory, &stream);
    if (FAILED(hr)) {
        goto cleanup;
    }
    /* WIC keeps a reference to the supplied buffer for the lifetime of the
     * stream, so we must finish reading pixels before freeing it. The caller's
     * buffer outlives this function, so InitializeFromMemory over it is safe. */
    hr = IWICStream_InitializeFromMemory(stream, (BYTE*)data, len);
    if (FAILED(hr)) {
        goto cleanup;
    }
    hr = IWICImagingFactory_CreateDecoderFromStream(cn1Win.wicFactory, (IStream*)stream,
                                                    NULL, WICDecodeMetadataCacheOnDemand,
                                                    &decoder);
    if (FAILED(hr)) {
        goto cleanup;
    }
    hr = IWICBitmapDecoder_GetFrame(decoder, 0, &frame);
    if (FAILED(hr)) {
        goto cleanup;
    }
    hr = IWICImagingFactory_CreateFormatConverter(cn1Win.wicFactory, &converter);
    if (FAILED(hr)) {
        goto cleanup;
    }
    hr = IWICFormatConverter_Initialize(converter, (IWICBitmapSource*)frame,
                                        GUID_WICPixelFormat32bppPBGRA,
                                        WICBitmapDitherTypeNone, NULL, 0.0,
                                        WICBitmapPaletteTypeMedianCut);
    if (FAILED(hr)) {
        goto cleanup;
    }
    source = (IWICBitmapSource*)converter;
    hr = IWICBitmapSource_GetSize(source, &w, &h);
    if (FAILED(hr) || w == 0 || h == 0) {
        goto cleanup;
    }
    pixels = (size_t)w * (size_t)h;
    buffer = (uint32_t*)malloc(pixels * sizeof(uint32_t));
    if (buffer == NULL) {
        goto cleanup;
    }
    /* CopyPixels writes PBGRA: memory bytes {B,G,R,A} == premultiplied uint32. */
    hr = IWICBitmapSource_CopyPixels(source, NULL, w * 4, (UINT)(pixels * 4),
                                     (BYTE*)buffer);
    if (FAILED(hr)) {
        free(buffer);
        buffer = NULL;
        goto cleanup;
    }
    /* Un-premultiply into canonical straight-alpha 0xAARRGGBB. */
    for (i = 0; i < pixels; i++) {
        buffer[i] = cn1WinPbgraToArgb(buffer[i]);
    }

    img = (CN1Image*)malloc(sizeof(CN1Image));
    if (img == NULL) {
        free(buffer);
        buffer = NULL;
        goto cleanup;
    }
    img->bitmap = NULL;
    img->width = (JAVA_INT)w;
    img->height = (JAVA_INT)h;
    img->argb = buffer;
    img->mutableGraphics = NULL;

cleanup:
    if (converter != NULL) {
        IWICFormatConverter_Release(converter);
    }
    if (frame != NULL) {
        IWICBitmapFrameDecode_Release(frame);
    }
    if (decoder != NULL) {
        IWICBitmapDecoder_Release(decoder);
    }
    if (stream != NULL) {
        IWICStream_Release(stream);
    }
    return img;
}

/*
 * Return (creating if necessary) the GPU bitmap for img bound to target.
 *
 * For a mutable image the backing store is an ID2D1BitmapRenderTarget; its
 * content bitmap (ID2D1BitmapRenderTarget_GetBitmap) is what we draw FROM, so
 * we always hand that back regardless of the passed target.
 *
 * For a regular image we lazily create an ID2D1Bitmap on the supplied target
 * from the CPU argb copy. The cached bitmap belongs to the target it was made
 * on; if the target changes between frames the bitmap stays valid as long as
 * the device is not lost (device-loss recovery is handled at the graphics
 * layer, which clears img->bitmap when it recreates targets).
 */
ID2D1Bitmap* cn1WinEnsureBitmap(CN1Image* img, ID2D1RenderTarget* target) {
    HRESULT hr;
    D2D1_SIZE_U size;
    D2D1_BITMAP_PROPERTIES props;
    uint32_t* pbgra;
    size_t pixels;
    size_t i;

    if (img == NULL) {
        return NULL;
    }
    /* Mutable image: draw from the bitmap render target's content bitmap. */
    if (img->mutableGraphics != NULL && img->mutableGraphics->target != NULL) {
        if (img->bitmap == NULL) {
            ID2D1BitmapRenderTarget_GetBitmap(
                (ID2D1BitmapRenderTarget*)img->mutableGraphics->target,
                &img->bitmap);
        }
        return img->bitmap;
    }
    if (img->bitmap != NULL) {
        return img->bitmap;
    }
    if (img->argb == NULL || target == NULL || img->width <= 0 || img->height <= 0) {
        return NULL;
    }

    pixels = (size_t)img->width * (size_t)img->height;
    pbgra = (uint32_t*)malloc(pixels * sizeof(uint32_t));
    if (pbgra == NULL) {
        return NULL;
    }
    /* Upload wants premultiplied PBGRA (memory {B,G,R,A}). */
    for (i = 0; i < pixels; i++) {
        pbgra[i] = cn1WinArgbToPbgra(img->argb[i]);
    }

    size.width = (UINT32)img->width;
    size.height = (UINT32)img->height;
    props.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    props.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;
    props.dpiX = 96.0f;
    props.dpiY = 96.0f;

    hr = ID2D1RenderTarget_CreateBitmap(target, size, NULL,
                                        (UINT32)(img->width * 4), &props,
                                        &img->bitmap);
    if (FAILED(hr) || img->bitmap == NULL) {
        img->bitmap = NULL;
        free(pbgra);
        return NULL;
    }
    {
        D2D1_RECT_U dst;
        dst.left = 0;
        dst.top = 0;
        dst.right = (UINT32)img->width;
        dst.bottom = (UINT32)img->height;
        hr = ID2D1Bitmap_CopyFromMemory(img->bitmap, &dst, pbgra,
                                        (UINT32)(img->width * 4));
        if (FAILED(hr)) {
            ID2D1Bitmap_Release(img->bitmap);
            img->bitmap = NULL;
        }
    }
    free(pbgra);
    return img->bitmap;
}

/* ----------------------------------------------------------- Java bridges */

JAVA_LONG com_codename1_impl_windows_WindowsNative_createImageFromARGB___int_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    JAVA_INT width = __cn1Arg2;
    JAVA_INT height = __cn1Arg3;
    JAVA_ARRAY_INT* src;
    uint32_t* buffer;
    size_t pixels;
    size_t i;
    CN1Image* img;

    if (__cn1Arg1 == JAVA_NULL || width <= 0 || height <= 0) {
        return 0;
    }
    src = (JAVA_ARRAY_INT*)(*(JAVA_ARRAY)__cn1Arg1).data;
    pixels = (size_t)width * (size_t)height;
    buffer = (uint32_t*)malloc(pixels * sizeof(uint32_t));
    if (buffer == NULL) {
        return 0;
    }
    /* The int[] is already 0xAARRGGBB; reinterpret each signed int as uint32. */
    for (i = 0; i < pixels; i++) {
        buffer[i] = (uint32_t)src[i];
    }
    img = cn1WinImageFromArgb(buffer, width, height);
    free(buffer);
    return (JAVA_LONG)(intptr_t)img;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_createImageFromFile___java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1) {
    WCHAR* widePath;
    UINT32 wlen = 0;
    FILE* fp;
    long size;
    BYTE* data;
    size_t readCount;
    CN1Image* img;

    if (__cn1Arg1 == JAVA_NULL) {
        return 0;
    }
    widePath = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &wlen);
    if (widePath == NULL) {
        return 0;
    }
    fp = _wfopen(widePath, L"rb");
    free(widePath);
    if (fp == NULL) {
        return 0;
    }
    if (fseek(fp, 0, SEEK_END) != 0) {
        fclose(fp);
        return 0;
    }
    size = ftell(fp);
    if (size <= 0) {
        fclose(fp);
        return 0;
    }
    if (fseek(fp, 0, SEEK_SET) != 0) {
        fclose(fp);
        return 0;
    }
    data = (BYTE*)malloc((size_t)size);
    if (data == NULL) {
        fclose(fp);
        return 0;
    }
    readCount = fread(data, 1, (size_t)size, fp);
    fclose(fp);
    if (readCount != (size_t)size) {
        free(data);
        return 0;
    }
    img = cn1WinDecodeImage(data, (UINT32)size);
    free(data);
    return (JAVA_LONG)(intptr_t)img;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_createImageFromBytes___byte_1ARRAY_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    JAVA_INT offset = __cn1Arg2;
    JAVA_INT length = __cn1Arg3;
    JAVA_ARRAY_BYTE* bytes;
    CN1Image* img;

    if (__cn1Arg1 == JAVA_NULL || length <= 0 || offset < 0) {
        return 0;
    }
    bytes = (JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)__cn1Arg1).data;
    /* JAVA_ARRAY_BYTE is signed char; reinterpret the slice as raw bytes. */
    img = cn1WinDecodeImage((const BYTE*)(bytes + offset), (UINT32)length);
    return (JAVA_LONG)(intptr_t)img;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_createMutableImage___int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    JAVA_INT width = __cn1Arg1;
    JAVA_INT height = __cn1Arg2;
    JAVA_INT fillColor = __cn1Arg3;
    CN1Image* img;
    ID2D1BitmapRenderTarget* brt = NULL;
    ID2D1RenderTarget* windowTarget;
    HRESULT hr;
    D2D1_SIZE_F desiredSize;
    D2D1_COLOR_F clearColor;
    uint32_t a, r, g, b;

    if (width <= 0 || height <= 0) {
        return 0;
    }
    if (cn1Win.windowGraphics == NULL || cn1Win.windowGraphics->target == NULL) {
        return 0;
    }
    windowTarget = cn1Win.windowGraphics->target;

    desiredSize.width = (float)width;
    desiredSize.height = (float)height;
    /* A compatible bitmap render target can be both drawn INTO (it is an
     * ID2D1RenderTarget) and drawn FROM (its content is fetched with
     * ID2D1BitmapRenderTarget_GetBitmap, done lazily in cn1WinEnsureBitmap). */
    hr = ID2D1RenderTarget_CreateCompatibleRenderTarget(windowTarget, &desiredSize,
                                                        NULL, NULL,
                                                        D2D1_COMPATIBLE_RENDER_TARGET_OPTIONS_NONE,
                                                        &brt);
    if (FAILED(hr) || brt == NULL) {
        return 0;
    }

    img = (CN1Image*)malloc(sizeof(CN1Image));
    if (img == NULL) {
        ID2D1BitmapRenderTarget_Release(brt);
        return 0;
    }
    img->bitmap = NULL;
    img->width = width;
    img->height = height;
    img->argb = NULL; /* mutable image lives on the GPU; getRGB reads back */
    img->mutableGraphics = cn1WinCreateGraphics((ID2D1RenderTarget*)brt);
    if (img->mutableGraphics == NULL) {
        ID2D1BitmapRenderTarget_Release(brt);
        free(img);
        return 0;
    }

    /* Clear the new surface to fillColor (0xAARRGGBB straight alpha). Direct2D
     * Clear takes a straight-alpha D2D1_COLOR_F and premultiplies internally. */
    a = (uint32_t)((fillColor >> 24) & 0xFF);
    r = (uint32_t)((fillColor >> 16) & 0xFF);
    g = (uint32_t)((fillColor >> 8) & 0xFF);
    b = (uint32_t)(fillColor & 0xFF);
    clearColor.r = r / 255.0f;
    clearColor.g = g / 255.0f;
    clearColor.b = b / 255.0f;
    clearColor.a = a / 255.0f;

    ID2D1RenderTarget_BeginDraw((ID2D1RenderTarget*)brt);
    ID2D1RenderTarget_Clear((ID2D1RenderTarget*)brt, &clearColor);
    ID2D1RenderTarget_EndDraw((ID2D1RenderTarget*)brt, NULL, NULL);

    return (JAVA_LONG)(intptr_t)img;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_imageWidth___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Image* img = (CN1Image*)(intptr_t)__cn1Arg1;
    if (img == NULL) {
        return 0;
    }
    return img->width;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_imageHeight___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Image* img = (CN1Image*)(intptr_t)__cn1Arg1;
    if (img == NULL) {
        return 0;
    }
    return img->height;
}

/*
 * Scale to a new CN1Image. We take the CPU path: nearest-neighbour resample the
 * source argb copy into a new buffer and wrap it with cn1WinImageFromArgb. This
 * keeps a CPU copy on the result (needed for further getRGB / scaleImage) and
 * avoids depending on a live render target here. Mutable / decode-only images
 * always carry an argb copy except for mutable images, which have none; for a
 * mutable source we read it back into a temporary argb buffer via imageGetRGB's
 * helper path before resampling.
 */
static uint32_t* cn1WinReadbackMutable(CN1Image* img);

JAVA_LONG com_codename1_impl_windows_WindowsNative_scaleImage___long_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    CN1Image* img = (CN1Image*)(intptr_t)__cn1Arg1;
    JAVA_INT dstW = __cn1Arg2;
    JAVA_INT dstH = __cn1Arg3;
    uint32_t* srcArgb;
    int ownSrc = 0;
    uint32_t* dst;
    CN1Image* result;
    int sx, sy;
    int dx, dy;

    if (img == NULL || dstW <= 0 || dstH <= 0 || img->width <= 0 || img->height <= 0) {
        return 0;
    }

    srcArgb = img->argb;
    if (srcArgb == NULL) {
        /* Mutable image: pull the pixels back from the GPU first. */
        srcArgb = cn1WinReadbackMutable(img);
        if (srcArgb == NULL) {
            return 0;
        }
        ownSrc = 1;
    }

    dst = (uint32_t*)malloc((size_t)dstW * (size_t)dstH * sizeof(uint32_t));
    if (dst == NULL) {
        if (ownSrc) {
            free(srcArgb);
        }
        return 0;
    }
    /* Nearest-neighbour: map each destination pixel back to the source grid. */
    for (dy = 0; dy < dstH; dy++) {
        sy = (int)(((long long)dy * img->height) / dstH);
        if (sy >= img->height) {
            sy = img->height - 1;
        }
        for (dx = 0; dx < dstW; dx++) {
            sx = (int)(((long long)dx * img->width) / dstW);
            if (sx >= img->width) {
                sx = img->width - 1;
            }
            dst[(size_t)dy * dstW + dx] = srcArgb[(size_t)sy * img->width + sx];
        }
    }

    result = cn1WinImageFromArgb(dst, dstW, dstH);
    free(dst);
    if (ownSrc) {
        free(srcArgb);
    }
    return (JAVA_LONG)(intptr_t)result;
}

/*
 * Read a mutable image's pixels back into a freshly malloc'd straight-alpha
 * 0xAARRGGBB buffer of width*height. We render the bitmap render target's
 * content bitmap into a WIC bitmap via a WIC render target, then CopyPixels.
 * Returns NULL on failure (the caller must free a non-NULL result).
 */
static uint32_t* cn1WinReadbackMutable(CN1Image* img) {
    IWICBitmap* wicBitmap = NULL;
    ID2D1RenderTarget* wicRT = NULL;
    ID2D1Bitmap* contentBitmap = NULL;
    IWICBitmapLock* lock = NULL;
    HRESULT hr;
    D2D1_RENDER_TARGET_PROPERTIES rtProps;
    D2D1_RECT_F destRect;
    WICRect lockRect;
    UINT bufSize = 0;
    BYTE* locked = NULL;
    UINT stride = 0;
    uint32_t* out = NULL;
    int y;
    size_t pixels;
    size_t i;

    if (img == NULL || cn1Win.wicFactory == NULL || cn1Win.d2dFactory == NULL) {
        return NULL;
    }
    if (img->mutableGraphics == NULL || img->mutableGraphics->target == NULL) {
        return NULL;
    }
    if (img->width <= 0 || img->height <= 0) {
        return NULL;
    }

    /* Commit any pending draws: the mutable image's compatible render target is
     * left mid-frame (BeginDraw) after paintComponent -- nothing flushes it the
     * way flushGraphics flushes the window target -- so its bitmap is only valid
     * once EndDraw has run. Without this the readback returns the cleared
     * surface (a blank image), e.g. for Display.screenshot. */
    cn1WinEndFrame(img->mutableGraphics);

    contentBitmap = cn1WinEnsureBitmap(img, img->mutableGraphics->target);
    if (contentBitmap == NULL) {
        return NULL;
    }

    hr = IWICImagingFactory_CreateBitmap(cn1Win.wicFactory, (UINT)img->width,
                                         (UINT)img->height,
                                         GUID_WICPixelFormat32bppPBGRA,
                                         WICBitmapCacheOnLoad, &wicBitmap);
    if (FAILED(hr)) {
        return NULL;
    }

    rtProps.type = D2D1_RENDER_TARGET_TYPE_DEFAULT;
    rtProps.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    rtProps.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;
    rtProps.dpiX = 96.0f;
    rtProps.dpiY = 96.0f;
    rtProps.usage = D2D1_RENDER_TARGET_USAGE_NONE;
    rtProps.minLevel = D2D1_FEATURE_LEVEL_DEFAULT;

    hr = ID2D1Factory_CreateWicBitmapRenderTarget(cn1Win.d2dFactory, wicBitmap,
                                                  &rtProps, &wicRT);
    if (FAILED(hr) || wicRT == NULL) {
        IWICBitmap_Release(wicBitmap);
        return NULL;
    }

    destRect.left = 0.0f;
    destRect.top = 0.0f;
    destRect.right = (float)img->width;
    destRect.bottom = (float)img->height;

    ID2D1RenderTarget_BeginDraw(wicRT);
    ID2D1RenderTarget_DrawBitmap(wicRT, contentBitmap, &destRect, 1.0f,
                                 D2D1_BITMAP_INTERPOLATION_MODE_NEAREST_NEIGHBOR,
                                 NULL);
    hr = ID2D1RenderTarget_EndDraw(wicRT, NULL, NULL);
    ID2D1RenderTarget_Release(wicRT);
    if (FAILED(hr)) {
        IWICBitmap_Release(wicBitmap);
        return NULL;
    }

    lockRect.X = 0;
    lockRect.Y = 0;
    lockRect.Width = img->width;
    lockRect.Height = img->height;
    hr = IWICBitmap_Lock(wicBitmap, &lockRect, WICBitmapLockRead, &lock);
    if (FAILED(hr)) {
        IWICBitmap_Release(wicBitmap);
        return NULL;
    }
    hr = IWICBitmapLock_GetStride(lock, &stride);
    if (SUCCEEDED(hr)) {
        hr = IWICBitmapLock_GetDataPointer(lock, &bufSize, &locked);
    }
    if (FAILED(hr) || locked == NULL) {
        IWICBitmapLock_Release(lock);
        IWICBitmap_Release(wicBitmap);
        return NULL;
    }

    pixels = (size_t)img->width * (size_t)img->height;
    out = (uint32_t*)malloc(pixels * sizeof(uint32_t));
    if (out == NULL) {
        IWICBitmapLock_Release(lock);
        IWICBitmap_Release(wicBitmap);
        return NULL;
    }
    /* Locked data is PBGRA with arbitrary stride; copy row by row and
     * un-premultiply into straight-alpha 0xAARRGGBB. */
    for (y = 0; y < img->height; y++) {
        uint32_t* srcRow = (uint32_t*)(locked + (size_t)y * stride);
        uint32_t* dstRow = out + (size_t)y * img->width;
        int x;
        for (x = 0; x < img->width; x++) {
            dstRow[x] = srcRow[x];
        }
    }
    for (i = 0; i < pixels; i++) {
        out[i] = cn1WinPbgraToArgb(out[i]);
    }

    IWICBitmapLock_Release(lock);
    IWICBitmap_Release(wicBitmap);
    return out;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_imageGetRGB___long_int_1ARRAY_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4, JAVA_INT __cn1Arg5, JAVA_INT __cn1Arg6, JAVA_INT __cn1Arg7) {
    CN1Image* img = (CN1Image*)(intptr_t)__cn1Arg1;
    JAVA_INT offset = __cn1Arg3;
    JAVA_INT x = __cn1Arg4;
    JAVA_INT y = __cn1Arg5;
    JAVA_INT width = __cn1Arg6;
    JAVA_INT height = __cn1Arg7;
    JAVA_ARRAY_INT* out;
    uint32_t* srcArgb;
    int ownSrc = 0;
    int row, col;

    if (img == NULL || __cn1Arg2 == JAVA_NULL || width <= 0 || height <= 0) {
        return;
    }
    if (x < 0 || y < 0 || x + width > img->width || y + height > img->height) {
        return;
    }

    /* getRGB reads from the CPU argb copy. Mutable images keep no CPU copy, so
     * pull the pixels back from the GPU into a temporary buffer first. */
    srcArgb = img->argb;
    if (srcArgb == NULL) {
        srcArgb = cn1WinReadbackMutable(img);
        if (srcArgb == NULL) {
            return;
        }
        ownSrc = 1;
    }

    out = (JAVA_ARRAY_INT*)(*(JAVA_ARRAY)__cn1Arg2).data;
    /* Destination scanline stride equals the requested width (CN1 contract). */
    for (row = 0; row < height; row++) {
        uint32_t* srcRow = srcArgb + (size_t)(y + row) * img->width + x;
        JAVA_ARRAY_INT* dstRow = out + offset + (size_t)row * width;
        for (col = 0; col < width; col++) {
            dstRow[col] = (JAVA_ARRAY_INT)srcRow[col];
        }
    }

    if (ownSrc) {
        free(srcArgb);
    }
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_getImageGraphics___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Image* img = (CN1Image*)(intptr_t)__cn1Arg1;
    if (img == NULL) {
        return 0;
    }
    return (JAVA_LONG)(intptr_t)img->mutableGraphics;
}

} /* extern "C" */

#endif /* _WIN32 */
