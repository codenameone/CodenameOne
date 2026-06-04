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
#ifndef CN1_WINDOWS_COMC_H
#define CN1_WINDOWS_COMC_H

/*
 * Call macros that let the COBJMACROS-style call sites (Interface_Method(p,..))
 * compile against the C++ COM classes. Direct2D and DirectWrite have no C
 * binding, so the COM translation units of this port are C++; there each macro
 * maps to a plain C++ method call on the interface pointer. Included only under
 * C++ (the C translation units never call these interfaces). Guarded so any a
 * Windows SDK header happens to define are left alone.
 */
#ifdef _WIN32
#ifdef __cplusplus

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#ifndef ID2D1BitmapRenderTarget_GetBitmap
#define ID2D1BitmapRenderTarget_GetBitmap(This, ...) ((This)->GetBitmap(__VA_ARGS__))
#endif
#ifndef ID2D1BitmapRenderTarget_Release
#define ID2D1BitmapRenderTarget_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1Bitmap_CopyFromMemory
#define ID2D1Bitmap_CopyFromMemory(This, ...) ((This)->CopyFromMemory(__VA_ARGS__))
#endif
#ifndef ID2D1Bitmap_Release
#define ID2D1Bitmap_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1Factory_CreateHwndRenderTarget
#define ID2D1Factory_CreateHwndRenderTarget(This, ...) ((This)->CreateHwndRenderTarget(__VA_ARGS__))
#endif
#ifndef ID2D1Factory_CreatePathGeometry
#define ID2D1Factory_CreatePathGeometry(This, ...) ((This)->CreatePathGeometry(__VA_ARGS__))
#endif
#ifndef ID2D1Factory_CreateWicBitmapRenderTarget
#define ID2D1Factory_CreateWicBitmapRenderTarget(This, ...) ((This)->CreateWicBitmapRenderTarget(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_AddArc
#define ID2D1GeometrySink_AddArc(This, ...) ((This)->AddArc(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_AddLine
#define ID2D1GeometrySink_AddLine(This, ...) ((This)->AddLine(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_AddBezier
#define ID2D1GeometrySink_AddBezier(This, ...) ((This)->AddBezier(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_AddQuadraticBezier
#define ID2D1GeometrySink_AddQuadraticBezier(This, ...) ((This)->AddQuadraticBezier(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_SetFillMode
#define ID2D1GeometrySink_SetFillMode(This, ...) ((This)->SetFillMode(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_BeginFigure
#define ID2D1GeometrySink_BeginFigure(This, ...) ((This)->BeginFigure(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_Close
#define ID2D1GeometrySink_Close(This, ...) ((This)->Close(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_EndFigure
#define ID2D1GeometrySink_EndFigure(This, ...) ((This)->EndFigure(__VA_ARGS__))
#endif
#ifndef ID2D1GeometrySink_Release
#define ID2D1GeometrySink_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1HwndRenderTarget_Resize
#define ID2D1HwndRenderTarget_Resize(This, ...) ((This)->Resize(__VA_ARGS__))
#endif
#ifndef ID2D1PathGeometry_Open
#define ID2D1PathGeometry_Open(This, ...) ((This)->Open(__VA_ARGS__))
#endif
#ifndef ID2D1PathGeometry_Release
#define ID2D1PathGeometry_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_BeginDraw
#define ID2D1RenderTarget_BeginDraw(This, ...) ((This)->BeginDraw(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_Clear
#define ID2D1RenderTarget_Clear(This, ...) ((This)->Clear(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_CreateBitmap
#define ID2D1RenderTarget_CreateBitmap(This, ...) ((This)->CreateBitmap(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_CreateBitmapFromWicBitmap
#define ID2D1RenderTarget_CreateBitmapFromWicBitmap(This, ...) ((This)->CreateBitmapFromWicBitmap(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_CreateCompatibleRenderTarget
#define ID2D1RenderTarget_CreateCompatibleRenderTarget(This, ...) ((This)->CreateCompatibleRenderTarget(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_CreateSolidColorBrush
#define ID2D1RenderTarget_CreateSolidColorBrush(This, ...) ((This)->CreateSolidColorBrush(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_DrawBitmap
#define ID2D1RenderTarget_DrawBitmap(This, ...) ((This)->DrawBitmap(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_DrawGeometry
#define ID2D1RenderTarget_DrawGeometry(This, ...) ((This)->DrawGeometry(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_DrawLine
#define ID2D1RenderTarget_DrawLine(This, ...) ((This)->DrawLine(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_DrawRectangle
#define ID2D1RenderTarget_DrawRectangle(This, ...) ((This)->DrawRectangle(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_DrawRoundedRectangle
#define ID2D1RenderTarget_DrawRoundedRectangle(This, ...) ((This)->DrawRoundedRectangle(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_EndDraw
#define ID2D1RenderTarget_EndDraw(This, ...) ((This)->EndDraw(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_SetTransform
#define ID2D1RenderTarget_SetTransform(This, ...) ((This)->SetTransform(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_FillGeometry
#define ID2D1RenderTarget_FillGeometry(This, ...) ((This)->FillGeometry(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_FillRectangle
#define ID2D1RenderTarget_FillRectangle(This, ...) ((This)->FillRectangle(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_FillRoundedRectangle
#define ID2D1RenderTarget_FillRoundedRectangle(This, ...) ((This)->FillRoundedRectangle(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_GetSize
#define ID2D1RenderTarget_GetSize(This, ...) ((This)->GetSize(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_PopAxisAlignedClip
#define ID2D1RenderTarget_PopAxisAlignedClip(This, ...) ((This)->PopAxisAlignedClip(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_PushAxisAlignedClip
#define ID2D1RenderTarget_PushAxisAlignedClip(This, ...) ((This)->PushAxisAlignedClip(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_CreateLayer
#define ID2D1RenderTarget_CreateLayer(This, ...) ((This)->CreateLayer(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_PushLayer
#define ID2D1RenderTarget_PushLayer(This, ...) ((This)->PushLayer(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_PopLayer
#define ID2D1RenderTarget_PopLayer(This, ...) ((This)->PopLayer(__VA_ARGS__))
#endif
#ifndef ID2D1Layer_Release
#define ID2D1Layer_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1RenderTarget_Release
#define ID2D1RenderTarget_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef ID2D1SolidColorBrush_SetColor
#define ID2D1SolidColorBrush_SetColor(This, ...) ((This)->SetColor(__VA_ARGS__))
#endif
#ifndef IWICBitmapDecoder_GetFrame
#define IWICBitmapDecoder_GetFrame(This, ...) ((This)->GetFrame(__VA_ARGS__))
#endif
#ifndef IWICBitmapDecoder_Release
#define IWICBitmapDecoder_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef IWICBitmapFrameDecode_Release
#define IWICBitmapFrameDecode_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef IWICBitmapLock_GetDataPointer
#define IWICBitmapLock_GetDataPointer(This, ...) ((This)->GetDataPointer(__VA_ARGS__))
#endif
#ifndef IWICBitmapLock_GetStride
#define IWICBitmapLock_GetStride(This, ...) ((This)->GetStride(__VA_ARGS__))
#endif
#ifndef IWICBitmapLock_Release
#define IWICBitmapLock_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef IWICBitmapSource_CopyPixels
#define IWICBitmapSource_CopyPixels(This, ...) ((This)->CopyPixels(__VA_ARGS__))
#endif
#ifndef IWICBitmapSource_GetSize
#define IWICBitmapSource_GetSize(This, ...) ((This)->GetSize(__VA_ARGS__))
#endif
#ifndef IWICBitmap_Lock
#define IWICBitmap_Lock(This, ...) ((This)->Lock(__VA_ARGS__))
#endif
#ifndef IWICBitmap_Release
#define IWICBitmap_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef IWICFormatConverter_Initialize
#define IWICFormatConverter_Initialize(This, ...) ((This)->Initialize(__VA_ARGS__))
#endif
#ifndef IWICFormatConverter_Release
#define IWICFormatConverter_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif
#ifndef IWICImagingFactory_CreateBitmap
#define IWICImagingFactory_CreateBitmap(This, ...) ((This)->CreateBitmap(__VA_ARGS__))
#endif
#ifndef IWICImagingFactory_CreateDecoderFromStream
#define IWICImagingFactory_CreateDecoderFromStream(This, ...) ((This)->CreateDecoderFromStream(__VA_ARGS__))
#endif
#ifndef IWICImagingFactory_CreateFormatConverter
#define IWICImagingFactory_CreateFormatConverter(This, ...) ((This)->CreateFormatConverter(__VA_ARGS__))
#endif
#ifndef IWICImagingFactory_CreateStream
#define IWICImagingFactory_CreateStream(This, ...) ((This)->CreateStream(__VA_ARGS__))
#endif
#ifndef IWICStream_InitializeFromMemory
#define IWICStream_InitializeFromMemory(This, ...) ((This)->InitializeFromMemory(__VA_ARGS__))
#endif
#ifndef IWICStream_Release
#define IWICStream_Release(This, ...) ((This)->Release(__VA_ARGS__))
#endif

#endif /* __cplusplus */
#endif /* _WIN32 */
#endif /* CN1_WINDOWS_COMC_H */
