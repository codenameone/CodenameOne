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
 * Offscreen rendering + PNG capture for the Windows port. A C++ unit (Direct2D
 * and WIC), exposing the WindowsNative bridge for headless rendering and the
 * screenshot tests: createOffscreenGraphics returns a normal CN1Graphics backed
 * by a WIC bitmap render target, so every drawing op flows through the same
 * graphics/text bridge as on-screen; saveGraphicsToPng flushes it and WIC-
 * encodes the bitmap to a file. The Direct2D / WIC factories are created lazily
 * so this works without a window (no initDisplay required).
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <objbase.h>
#include <stdlib.h>
#include <string.h>

extern "C" {

static ID2D1Factory* cn1ShotD2DFactory(void) {
    if (cn1Win.d2dFactory == NULL) {
        D2D1CreateFactory(D2D1_FACTORY_TYPE_MULTI_THREADED, IID_ID2D1Factory, NULL,
                (void**) &cn1Win.d2dFactory);
    }
    return cn1Win.d2dFactory;
}

static IWICImagingFactory* cn1ShotWicFactory(void) {
    if (cn1Win.wicFactory == NULL) {
        CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
        CoCreateInstance(CLSID_WICImagingFactory, NULL, CLSCTX_INPROC_SERVER,
                IID_IWICImagingFactory, (void**) &cn1Win.wicFactory);
    }
    return cn1Win.wicFactory;
}

/* Creates a CN1Graphics backed by a WIC bitmap render target. Shared by the
 * offscreen bridge and the headless display path (cn1_windows_window.cpp). */
CN1Graphics* cn1WinCreateOffscreenGraphics(int width, int height) {
    ID2D1Factory* d2d = cn1ShotD2DFactory();
    IWICImagingFactory* wic = cn1ShotWicFactory();
    if (d2d == NULL || wic == NULL) {
        cn1WindowsLog("createOffscreenGraphics: factory NULL");
        return NULL;
    }
    IWICBitmap* bitmap = NULL;
    if (FAILED(wic->CreateBitmap((UINT) width, (UINT) height,
            GUID_WICPixelFormat32bppPBGRA, WICBitmapCacheOnLoad, &bitmap))) {
        cn1WindowsLog("createOffscreenGraphics: CreateBitmap failed");
        return NULL;
    }
    D2D1_RENDER_TARGET_PROPERTIES props;
    ZeroMemory(&props, sizeof(props));
    props.type = D2D1_RENDER_TARGET_TYPE_DEFAULT;
    props.pixelFormat.format = DXGI_FORMAT_B8G8R8A8_UNORM;
    props.pixelFormat.alphaMode = D2D1_ALPHA_MODE_PREMULTIPLIED;
    ID2D1RenderTarget* rt = NULL;
    if (FAILED(d2d->CreateWicBitmapRenderTarget(bitmap, &props, &rt))) {
        bitmap->Release();
        return NULL;
    }
    CN1Graphics* g = cn1WinCreateGraphics(rt);
    if (g == NULL) {
        rt->Release();
        bitmap->Release();
        return NULL;
    }
    g->wicBitmap = bitmap; /* owned by the graphics for later encode/cleanup */
    return g;
}

/* WIC-encodes a WIC-backed graphics to a PNG file. */
JAVA_BOOLEAN cn1WinEncodeGraphicsToPng(CN1Graphics* g, const WCHAR* path) {
    if (g == NULL || g->wicBitmap == NULL || path == NULL) {
        return JAVA_FALSE;
    }
    /* Flush the batched draws into the WIC bitmap. */
    cn1WinEndFrame(g);

    IWICImagingFactory* wic = cn1ShotWicFactory();
    IWICBitmap* bitmap = (IWICBitmap*) g->wicBitmap;
    if (wic == NULL) {
        return JAVA_FALSE;
    }

    JAVA_BOOLEAN ok = JAVA_FALSE;
    IWICStream* stream = NULL;
    IWICBitmapEncoder* encoder = NULL;
    IWICBitmapFrameEncode* frame = NULL;
    if (SUCCEEDED(wic->CreateStream(&stream)) &&
            SUCCEEDED(stream->InitializeFromFilename(path, GENERIC_WRITE)) &&
            SUCCEEDED(wic->CreateEncoder(GUID_ContainerFormatPng, NULL, &encoder)) &&
            SUCCEEDED(encoder->Initialize(stream, WICBitmapEncoderNoCache)) &&
            SUCCEEDED(encoder->CreateNewFrame(&frame, NULL)) &&
            SUCCEEDED(frame->Initialize(NULL)) &&
            SUCCEEDED(frame->WriteSource((IWICBitmapSource*) bitmap, NULL)) &&
            SUCCEEDED(frame->Commit()) &&
            SUCCEEDED(encoder->Commit())) {
        ok = JAVA_TRUE;
    }
    if (frame != NULL) { frame->Release(); }
    if (encoder != NULL) { encoder->Release(); }
    if (stream != NULL) { stream->Release(); }
    return ok;
}

JAVA_LONG com_codename1_impl_windows_WindowsNative_createOffscreenGraphics___int_int_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_INT __cn1Arg1, JAVA_INT __cn1Arg2) {
    return (JAVA_LONG) (intptr_t) cn1WinCreateOffscreenGraphics(__cn1Arg1, __cn1Arg2);
}

JAVA_BOOLEAN com_codename1_impl_windows_WindowsNative_saveGraphicsToPng___long_java_lang_String_R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    CN1Graphics* g = (CN1Graphics*) (intptr_t) __cn1Arg1;
    WCHAR* path = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, NULL);
    JAVA_BOOLEAN ok = cn1WinEncodeGraphicsToPng(g, path);
    free(path);
    return ok;
}

/*
 * Encodes a width*height block of straight-alpha ARGB ints (the CN1 getRGB
 * layout, 0xAARRGGBB) to an in-memory PNG and returns it as a Java byte[].
 * A little-endian ARGB int is B,G,R,A in memory, i.e. WIC's 32bppBGRA, so the
 * pixel buffer is handed to WIC as-is. Backs the port's ImageIO.
 */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_encodeArgbToPng___int_1ARRAY_int_int_R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    if (__cn1Arg1 == JAVA_NULL || __cn1Arg2 <= 0 || __cn1Arg3 <= 0) {
        return JAVA_NULL;
    }
    IWICImagingFactory* wic = cn1ShotWicFactory();
    if (wic == NULL) {
        return JAVA_NULL;
    }
    UINT w = (UINT) __cn1Arg2;
    UINT h = (UINT) __cn1Arg3;
    JAVA_ARRAY_INT* px = (JAVA_ARRAY_INT*) (*(JAVA_ARRAY) __cn1Arg1).data;
    UINT stride = w * 4;
    UINT bufSize = stride * h;

    IWICBitmap* bitmap = NULL;
    if (FAILED(wic->CreateBitmapFromMemory(w, h, GUID_WICPixelFormat32bppBGRA,
            stride, bufSize, (BYTE*) px, &bitmap))) {
        return JAVA_NULL;
    }

    JAVA_OBJECT result = JAVA_NULL;
    IStream* stream = NULL;
    IWICBitmapEncoder* encoder = NULL;
    IWICBitmapFrameEncode* frame = NULL;
    if (SUCCEEDED(CreateStreamOnHGlobal(NULL, TRUE, &stream)) &&
            SUCCEEDED(wic->CreateEncoder(GUID_ContainerFormatPng, NULL, &encoder)) &&
            SUCCEEDED(encoder->Initialize(stream, WICBitmapEncoderNoCache)) &&
            SUCCEEDED(encoder->CreateNewFrame(&frame, NULL)) &&
            SUCCEEDED(frame->Initialize(NULL)) &&
            SUCCEEDED(frame->WriteSource((IWICBitmapSource*) bitmap, NULL)) &&
            SUCCEEDED(frame->Commit()) &&
            SUCCEEDED(encoder->Commit())) {
        STATSTG stat;
        ZeroMemory(&stat, sizeof(stat));
        HGLOBAL hg = NULL;
        if (SUCCEEDED(stream->Stat(&stat, STATFLAG_NONAME)) &&
                SUCCEEDED(GetHGlobalFromStream(stream, &hg)) && hg != NULL) {
            SIZE_T size = (SIZE_T) stat.cbSize.QuadPart;
            void* mem = GlobalLock(hg);
            if (mem != NULL && size > 0) {
                result = allocArray(threadStateData, (int) size,
                        &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
                if (result != JAVA_NULL) {
                    memcpy((*(JAVA_ARRAY) result).data, mem, size);
                }
            }
            if (mem != NULL) {
                GlobalUnlock(hg);
            }
        }
    }
    if (frame != NULL) { frame->Release(); }
    if (encoder != NULL) { encoder->Release(); }
    if (stream != NULL) { stream->Release(); }
    bitmap->Release();
    return result;
}

/* Encodes a WIC bitmap source to PNG and returns the bytes as a Java byte[]. */
static JAVA_OBJECT cn1WinWicSourceToPngBytes(CODENAME_ONE_THREAD_STATE, IWICBitmapSource* src) {
    IWICImagingFactory* wic = cn1ShotWicFactory();
    if (wic == NULL || src == NULL) {
        return JAVA_NULL;
    }
    JAVA_OBJECT result = JAVA_NULL;
    IStream* stream = NULL;
    IWICBitmapEncoder* encoder = NULL;
    IWICBitmapFrameEncode* frame = NULL;
    if (SUCCEEDED(CreateStreamOnHGlobal(NULL, TRUE, &stream)) &&
            SUCCEEDED(wic->CreateEncoder(GUID_ContainerFormatPng, NULL, &encoder)) &&
            SUCCEEDED(encoder->Initialize(stream, WICBitmapEncoderNoCache)) &&
            SUCCEEDED(encoder->CreateNewFrame(&frame, NULL)) &&
            SUCCEEDED(frame->Initialize(NULL)) &&
            SUCCEEDED(frame->WriteSource(src, NULL)) &&
            SUCCEEDED(frame->Commit()) &&
            SUCCEEDED(encoder->Commit())) {
        STATSTG stat;
        ZeroMemory(&stat, sizeof(stat));
        HGLOBAL hg = NULL;
        if (SUCCEEDED(stream->Stat(&stat, STATFLAG_NONAME)) &&
                SUCCEEDED(GetHGlobalFromStream(stream, &hg)) && hg != NULL) {
            SIZE_T size = (SIZE_T) stat.cbSize.QuadPart;
            void* mem = GlobalLock(hg);
            if (mem != NULL && size > 0) {
                result = allocArray(threadStateData, (int) size,
                        &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
                if (result != JAVA_NULL) {
                    memcpy((*(JAVA_ARRAY) result).data, mem, size);
                }
            }
            if (mem != NULL) {
                GlobalUnlock(hg);
            }
        }
    }
    if (frame != NULL) { frame->Release(); }
    if (encoder != NULL) { encoder->Release(); }
    if (stream != NULL) { stream->Release(); }
    return result;
}

/*
 * Encodes the current window's render target to PNG bytes. In headless /
 * offscreen mode the window target is a WIC bitmap (drawn into by the EDT's
 * normal paint), so this is the proven render path the headless capture uses --
 * a deterministic snapshot of the rendered UI for the cn1ss WebSocket sink,
 * sidestepping the mutable-image GetRGB path.
 */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_captureWindowToPngBytes___R_byte_1ARRAY(
        CODENAME_ONE_THREAD_STATE) {
    CN1Graphics* g = cn1Win.windowGraphics;
    if (g == NULL || g->wicBitmap == NULL) {
        cn1WindowsLog("captureWindowToPngBytes: window target is not WIC-backed");
        return JAVA_NULL;
    }
    cn1WinEndFrame(g);
    return cn1WinWicSourceToPngBytes(threadStateData, (IWICBitmapSource*) g->wicBitmap);
}

/* ----------------------------------------------- headless screenshot mode */

/*
 * Enables headless screenshot capture: the next initDisplay renders into an
 * offscreen WIC bitmap of the given size, and once the UI has painted the
 * bitmap is written to path and the process exits. Lets CI capture a
 * deterministic PNG of the rendered UI with no window / display.
 */
JAVA_VOID com_codename1_impl_windows_WindowsNative_enableHeadlessScreenshot___java_lang_String_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_INT __cn1Arg2, JAVA_INT __cn1Arg3) {
    free(cn1Win.shotPath);
    cn1Win.shotPath = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, NULL);
    cn1Win.shotW = __cn1Arg2;
    cn1Win.shotH = __cn1Arg3;
    cn1Win.headless = 1;
}

} /* extern "C" */

#endif /* _WIN32 */
