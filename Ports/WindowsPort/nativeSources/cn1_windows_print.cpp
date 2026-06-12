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
 * Printing translation unit for the native Codename One Windows port: backs
 * com.codename1.printing (Printer / PrintResult) with the real Win32 print
 * pipeline.
 *
 * Flow: the CN1 printing worker thread calls printDocument, which hands the
 * modal system print dialog (PrintDlgW) to the window-owning pump thread with a
 * blocking SendMessage(WM_CN1_PRINTDLG) -- the same cross-thread handover the
 * file dialog uses (cn1_windows_io.c). The dialog yields the printer HDC; the
 * worker thread then rasterizes the document and spools it through GDI
 * (StartDocW / StartPage / StretchDIBits / EndPage / EndDoc):
 *
 *   - images (image/png, image/jpeg, ...): decoded with the shared WIC helper
 *     (cn1WinDecodeImage in cn1_windows_image.cpp) and printed as a single
 *     page scaled to fit the printable area, preserving aspect ratio.
 *   - application/pdf: each page is rasterized by the WinRT Windows.Data.Pdf
 *     renderer at the printer's DPI into an in-memory PNG stream, decoded with
 *     the same WIC helper and printed like an image page. WinRT is consumed
 *     through the raw WRL ABI projection exactly like cn1_windows_winrt.cpp --
 *     no C++/WinRT, no C++ STL (the cross-compile toolchain may predate the
 *     MSVC STL's clang requirement), manual COM lifetime management.
 *
 * Outcomes are honest: 0 only after EndDoc handed the job to the spooler, 1
 * when the user dismissed the dialog, 2 with a short message (printLastError)
 * for everything else. Nothing is fabricated in headless mode -- with no host
 * window there is no dialog, so the request fails.
 */

#ifdef _WIN32

#include "cn1_windows.h"

#include <cderr.h>
#include <commdlg.h>
#include <roapi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wrl.h>
#include <wrl/event.h>
#include <wrl/wrappers/corewrappers.h>
#include <windows.foundation.h>
#include <windows.data.pdf.h>
#include <windows.storage.h>
#include <windows.storage.streams.h>

using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::Data::Pdf;
using namespace ABI::Windows::Storage;
using namespace ABI::Windows::Storage::Streams;
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;

/* Largest page-raster dimension for PDF rendering. Bounds the per-page pixel
 * buffer (4096^2 * 4 bytes = 64MB) while still exceeding 400 DPI on letter /
 * A4 paper -- visually lossless for a desktop print. */
#define CN1_PRINT_MAX_RENDER_DIM 4096

/* Last failure description for the Java side (printLastError). Printing is
 * serialized by the modal dialog -- one job at a time -- so a single static
 * buffer is sufficient. ASCII/UTF-8. */
static char g_printError[512];

static void cn1PrintSetError(const char* msg) {
    snprintf(g_printError, sizeof(g_printError), "%s", msg != NULL ? msg : "");
}

static void cn1PrintSetErrorHr(const char* what, HRESULT hr) {
    snprintf(g_printError, sizeof(g_printError), "%s (HRESULT 0x%08lX)",
            what, (unsigned long) hr);
}

/* Blocks the calling (CN1 worker) thread until a WinRT IAsyncOperation<T>
 * completes, then fetches its result -- same shape as the helper in
 * cn1_windows_winrt.cpp (which is private to that unit). TOp is the operation's
 * logical result type; TResultPtr the GetResults out-pointer type. */
template <typename TOp, typename TResultPtr>
static HRESULT cn1PrintAwaitOp(IAsyncOperation<TOp>* op, TResultPtr result) {
    Event done(CreateEventExW(nullptr, nullptr, 0, EVENT_ALL_ACCESS));
    if (!done.IsValid()) {
        return E_FAIL;
    }
    HRESULT hr = op->put_Completed(Callback<IAsyncOperationCompletedHandler<TOp>>(
            [&done](IAsyncOperation<TOp>*, AsyncStatus) {
                SetEvent(done.Get());
                return S_OK;
            }).Get());
    if (FAILED(hr)) {
        return hr;
    }
    WaitForSingleObject(done.Get(), 120000);
    return op->GetResults(result);
}

/* IAsyncAction variant of the above (RenderWithOptionsToStreamAsync returns an
 * action, not an operation). */
static HRESULT cn1PrintAwaitAction(IAsyncAction* action) {
    Event done(CreateEventExW(nullptr, nullptr, 0, EVENT_ALL_ACCESS));
    if (!done.IsValid()) {
        return E_FAIL;
    }
    HRESULT hr = action->put_Completed(Callback<IAsyncActionCompletedHandler>(
            [&done](IAsyncAction*, AsyncStatus) {
                SetEvent(done.Get());
                return S_OK;
            }).Get());
    if (FAILED(hr)) {
        return hr;
    }
    WaitForSingleObject(done.Get(), 120000);
    return action->GetResults();
}

/* Frees a CN1Image produced by cn1WinDecodeImage. Decode-only images carry just
 * the CPU pixel copy -- no GPU bitmap (lazy) and no mutable graphics. */
static void cn1PrintFreeImage(CN1Image* img) {
    if (img == NULL) {
        return;
    }
    if (img->argb != NULL) {
        free(img->argb);
    }
    free(img);
}

/*
 * Spools one page of straight-alpha 0xAARRGGBB pixels: composites over white
 * (paper), scales to fit the printable area preserving aspect ratio, centers,
 * and emits it between StartPage/EndPage with StretchDIBits. Returns 1 on
 * success, 0 on failure with g_printError set.
 */
static int cn1PrintArgbPage(HDC hdc, const uint32_t* argb, int w, int h) {
    int printW = GetDeviceCaps(hdc, HORZRES);
    int printH = GetDeviceCaps(hdc, VERTRES);
    long long destW, destH;
    int destX, destY;
    uint32_t* dib;
    size_t pixels;
    size_t i;
    BITMAPINFO bmi;
    int scanned;
    int ok = 1;

    if (argb == NULL || w <= 0 || h <= 0) {
        cn1PrintSetError("Nothing to print on this page");
        return 0;
    }
    if (printW <= 0 || printH <= 0) {
        cn1PrintSetError("The printer reports an empty printable area");
        return 0;
    }

    /* Fit within the printable area, preserving aspect ratio; center within it.
     * (0,0) of a printer DC is the top-left of the printable area -- the
     * physical margin offsets (PHYSICALOFFSETX/Y) are already outside it. */
    destW = printW;
    destH = ((long long) h * printW) / w;
    if (destH > printH) {
        destH = printH;
        destW = ((long long) w * printH) / h;
    }
    if (destW < 1) {
        destW = 1;
    }
    if (destH < 1) {
        destH = 1;
    }
    destX = (int) ((printW - destW) / 2);
    destY = (int) ((printH - destH) / 2);

    /* Composite over white into a 32bpp BI_RGB top-down DIB. A DIB pixel's
     * little-endian memory bytes are {B,G,R,x}, i.e. the uint32 0x00RRGGBB --
     * the same layout as our ARGB once alpha has been blended away. */
    pixels = (size_t) w * (size_t) h;
    dib = (uint32_t*) malloc(pixels * sizeof(uint32_t));
    if (dib == NULL) {
        cn1PrintSetError("Out of memory preparing the print page");
        return 0;
    }
    for (i = 0; i < pixels; i++) {
        uint32_t p = argb[i];
        uint32_t a = (p >> 24) & 0xFF;
        uint32_t r = (p >> 16) & 0xFF;
        uint32_t g = (p >> 8) & 0xFF;
        uint32_t b = p & 0xFF;
        if (a != 255) {
            uint32_t inv = 255 - a;
            r = (r * a + 255 * inv + 127) / 255;
            g = (g * a + 255 * inv + 127) / 255;
            b = (b * a + 255 * inv + 127) / 255;
        }
        dib[i] = (r << 16) | (g << 8) | b;
    }

    ZeroMemory(&bmi, sizeof(bmi));
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = w;
    bmi.bmiHeader.biHeight = -h; /* negative = top-down rows */
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    if (StartPage(hdc) <= 0) {
        cn1PrintSetError("StartPage failed");
        free(dib);
        return 0;
    }
    /* HALFTONE averages source pixels when shrinking -- the quality mode for
     * scaling a raster onto a high-DPI printer page. */
    SetStretchBltMode(hdc, HALFTONE);
    SetBrushOrgEx(hdc, 0, 0, NULL);
    /* StretchDIBits returns the scanned line count, 0 / GDI_ERROR (-1 as int)
     * on failure. */
    scanned = StretchDIBits(hdc, destX, destY, (int) destW, (int) destH,
            0, 0, w, h, dib, &bmi, DIB_RGB_COLORS, SRCCOPY);
    if (scanned <= 0) {
        cn1PrintSetError("StretchDIBits failed to spool the page");
        ok = 0;
    }
    if (EndPage(hdc) <= 0 && ok) {
        cn1PrintSetError("EndPage failed");
        ok = 0;
    }
    free(dib);
    return ok;
}

/*
 * Prints an image file (PNG/JPEG/... -- anything the shared WIC decoder
 * handles) as a single-page document. Returns 1 on success, 0 on failure with
 * g_printError set. The document (StartDocW/EndDoc) is managed by the caller.
 */
static int cn1PrintImagePages(HDC hdc, const WCHAR* path) {
    FILE* fp;
    long size;
    BYTE* data;
    size_t readCount;
    CN1Image* img;
    int ok;

    fp = _wfopen(path, L"rb");
    if (fp == NULL) {
        cn1PrintSetError("Could not open the image file");
        return 0;
    }
    if (fseek(fp, 0, SEEK_END) != 0 || (size = ftell(fp)) <= 0
            || fseek(fp, 0, SEEK_SET) != 0) {
        fclose(fp);
        cn1PrintSetError("Could not read the image file");
        return 0;
    }
    data = (BYTE*) malloc((size_t) size);
    if (data == NULL) {
        fclose(fp);
        cn1PrintSetError("Out of memory reading the image file");
        return 0;
    }
    readCount = fread(data, 1, (size_t) size, fp);
    fclose(fp);
    if (readCount != (size_t) size) {
        free(data);
        cn1PrintSetError("Could not read the image file");
        return 0;
    }
    img = cn1WinDecodeImage(data, (UINT32) size);
    free(data);
    if (img == NULL || img->argb == NULL) {
        cn1PrintFreeImage(img);
        cn1PrintSetError("Could not decode the image");
        return 0;
    }
    ok = cn1PrintArgbPage(hdc, img->argb, img->width, img->height);
    cn1PrintFreeImage(img);
    return ok;
}

/*
 * Prints a PDF file: loads it with the WinRT Windows.Data.Pdf renderer and
 * rasterizes each page at the printer's DPI (capped at
 * CN1_PRINT_MAX_RENDER_DIM) into an in-memory PNG, which is then decoded with
 * the shared WIC helper and spooled like an image page. Returns 1 on success,
 * 0 on failure with g_printError set. The document (StartDocW/EndDoc) is
 * managed by the caller.
 */
static int cn1PrintPdfPages(HDC hdc, const WCHAR* path) {
    HRESULT hr;
    UINT32 pageCount = 0;
    UINT32 pageIndex;
    int dpiX = GetDeviceCaps(hdc, LOGPIXELSX);
    int dpiY = GetDeviceCaps(hdc, LOGPIXELSY);

    if (dpiX <= 0) {
        dpiX = 600;
    }
    if (dpiY <= 0) {
        dpiY = 600;
    }

    RoInitialize(RO_INIT_MULTITHREADED);

    /* StorageFile from the Win32 path (the unpackaged-app entry point into
     * Windows.Storage; the caller already normalized to backslashes). */
    ComPtr<IStorageFileStatics> fileStatics;
    hr = RoGetActivationFactory(
            HStringReference(RuntimeClass_Windows_Storage_StorageFile).Get(),
            IID_PPV_ARGS(&fileStatics));
    if (FAILED(hr)) {
        cn1PrintSetErrorHr("The PDF renderer is unavailable", hr);
        return 0;
    }
    ComPtr<IAsyncOperation<StorageFile*>> fileOp;
    hr = fileStatics->GetFileFromPathAsync(
            HStringReference(path).Get(), &fileOp);
    if (FAILED(hr)) {
        cn1PrintSetErrorHr("Could not open the PDF file", hr);
        return 0;
    }
    ComPtr<IStorageFile> file;
    hr = cn1PrintAwaitOp(fileOp.Get(), file.GetAddressOf());
    if (FAILED(hr) || !file) {
        cn1PrintSetErrorHr("Could not open the PDF file", hr);
        return 0;
    }

    ComPtr<IPdfDocumentStatics> pdfStatics;
    hr = RoGetActivationFactory(
            HStringReference(RuntimeClass_Windows_Data_Pdf_PdfDocument).Get(),
            IID_PPV_ARGS(&pdfStatics));
    if (FAILED(hr)) {
        cn1PrintSetErrorHr("The PDF renderer is unavailable", hr);
        return 0;
    }
    ComPtr<IAsyncOperation<PdfDocument*>> docOp;
    hr = pdfStatics->LoadFromFileAsync(file.Get(), &docOp);
    if (FAILED(hr)) {
        cn1PrintSetErrorHr("Could not load the PDF document", hr);
        return 0;
    }
    ComPtr<IPdfDocument> doc;
    hr = cn1PrintAwaitOp(docOp.Get(), doc.GetAddressOf());
    if (FAILED(hr) || !doc) {
        /* A password-protected or corrupt PDF lands here. */
        cn1PrintSetErrorHr("Could not load the PDF document", hr);
        return 0;
    }
    doc->get_PageCount(&pageCount);
    if (pageCount == 0) {
        cn1PrintSetError("The PDF document has no pages");
        return 0;
    }

    for (pageIndex = 0; pageIndex < pageCount; pageIndex++) {
        ComPtr<IPdfPage> page;
        hr = doc->GetPage(pageIndex, &page);
        if (FAILED(hr) || !page) {
            cn1PrintSetErrorHr("Could not read a PDF page", hr);
            return 0;
        }
        /* Page size is in DIPs (1/96"); scale uniformly to the printer DPI,
         * bounded so one rasterized page cannot exhaust memory. */
        Size pageSize;
        pageSize.Width = 0;
        pageSize.Height = 0;
        page->get_Size(&pageSize);
        if (pageSize.Width <= 0 || pageSize.Height <= 0) {
            cn1PrintSetError("A PDF page reports an empty size");
            return 0;
        }
        double scale = dpiX / 96.0;
        double scaleY = dpiY / 96.0;
        if (scaleY < scale) {
            scale = scaleY;
        }
        if (pageSize.Width * scale > CN1_PRINT_MAX_RENDER_DIM) {
            scale = CN1_PRINT_MAX_RENDER_DIM / pageSize.Width;
        }
        if (pageSize.Height * scale > CN1_PRINT_MAX_RENDER_DIM) {
            scale = CN1_PRINT_MAX_RENDER_DIM / pageSize.Height;
        }
        UINT32 destW = (UINT32) (pageSize.Width * scale + 0.5);
        UINT32 destH = (UINT32) (pageSize.Height * scale + 0.5);
        if (destW < 1) {
            destW = 1;
        }
        if (destH < 1) {
            destH = 1;
        }

        ComPtr<IInspectable> streamInsp;
        hr = RoActivateInstance(
                HStringReference(RuntimeClass_Windows_Storage_Streams_InMemoryRandomAccessStream).Get(),
                &streamInsp);
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not create the page render stream", hr);
            return 0;
        }
        ComPtr<IRandomAccessStream> stream;
        if (FAILED(streamInsp.As(&stream)) || !stream) {
            cn1PrintSetError("Could not create the page render stream");
            return 0;
        }
        ComPtr<IInspectable> optionsInsp;
        hr = RoActivateInstance(
                HStringReference(RuntimeClass_Windows_Data_Pdf_PdfPageRenderOptions).Get(),
                &optionsInsp);
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not create the page render options", hr);
            return 0;
        }
        ComPtr<IPdfPageRenderOptions> options;
        if (FAILED(optionsInsp.As(&options)) || !options) {
            cn1PrintSetError("Could not create the page render options");
            return 0;
        }
        options->put_DestinationWidth(destW);
        options->put_DestinationHeight(destH);

        /* Rasterize the page (PNG, the renderer's default encoder). */
        ComPtr<IAsyncAction> renderAction;
        hr = page->RenderWithOptionsToStreamAsync(stream.Get(), options.Get(), &renderAction);
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not render a PDF page", hr);
            return 0;
        }
        hr = cn1PrintAwaitAction(renderAction.Get());
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not render a PDF page", hr);
            return 0;
        }

        /* Pull the encoded bytes back out of the in-memory stream through a
         * DataReader -- pure ABI, no shcore IStream bridge needed. */
        UINT64 streamSize = 0;
        stream->get_Size(&streamSize);
        if (streamSize == 0 || streamSize > 0x7FFFFFFF) {
            cn1PrintSetError("A rendered PDF page is empty or too large");
            return 0;
        }
        ComPtr<IInputStream> input;
        hr = stream->GetInputStreamAt(0, &input);
        if (FAILED(hr) || !input) {
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }
        ComPtr<IDataReaderFactory> readerFactory;
        hr = RoGetActivationFactory(
                HStringReference(RuntimeClass_Windows_Storage_Streams_DataReader).Get(),
                IID_PPV_ARGS(&readerFactory));
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }
        ComPtr<IDataReader> reader;
        hr = readerFactory->CreateDataReader(input.Get(), &reader);
        if (FAILED(hr) || !reader) {
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }
        ComPtr<IAsyncOperation<UINT32>> loadOp;
        hr = reader->LoadAsync((UINT32) streamSize, &loadOp);
        if (FAILED(hr)) {
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }
        UINT32 loaded = 0;
        hr = cn1PrintAwaitOp(loadOp.Get(), &loaded);
        if (FAILED(hr) || loaded == 0) {
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }
        BYTE* png = (BYTE*) malloc(loaded);
        if (png == NULL) {
            cn1PrintSetError("Out of memory reading a rendered PDF page");
            return 0;
        }
        hr = reader->ReadBytes(loaded, png);
        if (FAILED(hr)) {
            free(png);
            cn1PrintSetErrorHr("Could not read back a rendered PDF page", hr);
            return 0;
        }

        CN1Image* img = cn1WinDecodeImage(png, loaded);
        free(png);
        if (img == NULL || img->argb == NULL) {
            cn1PrintFreeImage(img);
            cn1PrintSetError("Could not decode a rendered PDF page");
            return 0;
        }
        int ok = cn1PrintArgbPage(hdc, img->argb, img->width, img->height);
        cn1PrintFreeImage(img);
        if (!ok) {
            return 0;
        }
    }
    return 1;
}

extern "C" {

/* ----------------------------------------------------- system print dialog
 *
 * Cross-thread request for the modal print dialog. The printing worker fills
 * it, sends it to the pump thread (which owns the window) via WM_CN1_PRINTDLG,
 * and reads the results back once SendMessage returns. The struct lives on the
 * worker's stack, valid for the whole blocking call. */
typedef struct CN1PrintDlgReq {
    HDC hdc;        /* out: printer DC on success; the worker owns it (DeleteDC) */
    int cancelled;  /* out: 1 when the user dismissed the dialog                 */
    DWORD dlgError; /* out: CommDlgExtendedError code when the dialog failed     */
} CN1PrintDlgReq;

/* Runs on the pump thread (dispatched from cn1WinWndProc on WM_CN1_PRINTDLG).
 * Shows the modal system print dialog owned by the main window and hands the
 * chosen printer's DC back to the worker. The driver handles copies/collation
 * (PD_USEDEVMODECOPIESANDCOLLATE), so the worker spools each page exactly once. */
LRESULT cn1WinPrintDialogHandleMessage(WPARAM wp) {
    CN1PrintDlgReq* req = (CN1PrintDlgReq*) wp;
    PRINTDLGW pd;
    if (req == NULL) {
        return 0;
    }
    req->hdc = NULL;
    req->cancelled = 0;
    req->dlgError = 0;
    ZeroMemory(&pd, sizeof(pd));
    pd.lStructSize = sizeof(pd);
    pd.hwndOwner = cn1Win.hwnd;
    pd.Flags = PD_RETURNDC | PD_USEDEVMODECOPIESANDCOLLATE | PD_NOPAGENUMS
            | PD_NOSELECTION | PD_HIDEPRINTTOFILE;
    pd.nCopies = 1;
    if (PrintDlgW(&pd)) {
        req->hdc = pd.hDC;
    } else {
        DWORD err = CommDlgExtendedError();
        if (err == 0) {
            req->cancelled = 1; /* user dismissed the dialog */
        } else {
            req->dlgError = err;
        }
    }
    /* PrintDlgW allocates these global handles even when the caller passed
     * NULL; they are not needed once the DC exists. */
    if (pd.hDevMode != NULL) {
        GlobalFree(pd.hDevMode);
    }
    if (pd.hDevNames != NULL) {
        GlobalFree(pd.hDevNames);
    }
    return 0;
}

/* --------------------------------------------------------------- Java bridge
 *
 * Prints the document at `path` (already stripped of the file:// prefix by the
 * Java side). Returns 0 once EndDoc handed the job to the print spooler, 1 when
 * the user cancelled the print dialog, 2 on failure (printLastError carries the
 * description). Blocks for the whole flow -- the Java side calls it from a
 * dedicated worker thread, and the thread state is yielded around the blocking
 * native section so the GC is never held up (same discipline as fileDialog). */
JAVA_INT com_codename1_impl_windows_WindowsNative_printDocument___java_lang_String_java_lang_String_java_lang_String_R_int(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT pathObj, JAVA_OBJECT mimeObj, JAVA_OBJECT jobNameObj) {
    WCHAR* path;
    WCHAR* mime;
    WCHAR* jobName;
    WCHAR* p;
    CN1PrintDlgReq req;
    DOCINFOW di;
    int isPdf;
    int isImage;
    int ok;
    JAVA_INT status = 2;

    g_printError[0] = 0;
    /* Headless screenshot mode has no window to own the print dialog. */
    if (cn1Win.hwnd == NULL) {
        cn1PrintSetError("Printing requires a display window");
        return 2;
    }
    if (pathObj == JAVA_NULL) {
        cn1PrintSetError("Print file path is null");
        return 2;
    }
    path = cn1WinJavaStringToWide(threadStateData, pathObj, NULL);
    mime = mimeObj != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, mimeObj, NULL) : NULL;
    jobName = jobNameObj != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, jobNameObj, NULL) : NULL;
    if (path == NULL) {
        if (mime != NULL) {
            free(mime);
        }
        if (jobName != NULL) {
            free(jobName);
        }
        cn1PrintSetError("Print file path is invalid");
        return 2;
    }
    /* WinRT's GetFileFromPathAsync insists on backslashes; CN1 paths arrive
     * with forward slashes (the Win32 file APIs accept both). */
    for (p = path; *p != 0; p++) {
        if (*p == L'/') {
            *p = L'\\';
        }
    }
    isPdf = mime != NULL && wcscmp(mime, L"application/pdf") == 0;
    isImage = mime != NULL && wcsncmp(mime, L"image/", 6) == 0;

    if (!isPdf && !isImage) {
        cn1PrintSetError("Unsupported print document type");
        free(path);
        if (mime != NULL) {
            free(mime);
        }
        if (jobName != NULL) {
            free(jobName);
        }
        return 2;
    }

    /* Everything from the modal dialog to the spooler hand-off blocks with no
     * Java object access, so yield the thread state for the duration. */
    CN1_YIELD_THREAD;

    req.hdc = NULL;
    req.cancelled = 0;
    req.dlgError = 0;
    SendMessageW(cn1Win.hwnd, WM_CN1_PRINTDLG, (WPARAM) &req, 0);

    if (req.cancelled) {
        status = 1;
    } else if (req.hdc == NULL) {
        if (req.dlgError == PDERR_NODEFAULTPRN) {
            cn1PrintSetError("No printer is installed");
        } else {
            cn1PrintSetError("The print dialog could not be shown");
        }
        status = 2;
    } else {
        ZeroMemory(&di, sizeof(di));
        di.cbSize = sizeof(di);
        di.lpszDocName = (jobName != NULL && jobName[0] != 0) ? jobName : L"Codename One";
        if (StartDocW(req.hdc, &di) <= 0) {
            cn1PrintSetError("The print job could not be started");
            status = 2;
        } else {
            ok = isPdf ? cn1PrintPdfPages(req.hdc, path)
                       : cn1PrintImagePages(req.hdc, path);
            if (ok) {
                if (EndDoc(req.hdc) > 0) {
                    status = 0;
                } else {
                    cn1PrintSetError("The print job could not be handed to the spooler");
                    status = 2;
                }
            } else {
                /* g_printError was set by the page renderer; discard the job. */
                AbortDoc(req.hdc);
                status = 2;
            }
        }
        DeleteDC(req.hdc);
    }

    CN1_RESUME_THREAD;

    free(path);
    if (mime != NULL) {
        free(mime);
    }
    if (jobName != NULL) {
        free(jobName);
    }
    return status;
}

/* Short description of the most recent printDocument failure, or null when the
 * last request did not fail. */
JAVA_OBJECT com_codename1_impl_windows_WindowsNative_printLastError___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    if (g_printError[0] == 0) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, g_printError);
}

} /* extern "C" */

#endif /* _WIN32 */
