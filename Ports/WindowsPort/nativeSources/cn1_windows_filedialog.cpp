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
 * Rich open/save dialog behind Display.showOpenDialog / showSaveDialog, over
 * the Vista+ COM IFileOpenDialog / IFileSaveDialog. The string contract (filters,
 * options and the answer array) is com.codename1.impl.NativeFileDialogCodec.
 *
 * Threading: the dialog is modal and must be owned by the pump thread, so the
 * EDT-facing native copies every Java string into malloc'd wide memory, yields
 * its thread state, SendMessage's WM_CN1_FILEDIALOGEX to cn1Win.hwnd and builds
 * the Java String[] only after it has resumed. Nothing on the pump thread touches
 * a Java object.
 */
#ifdef _WIN32

#include "cn1_windows.h"

#include <shobjidl.h>
#include <shlobj.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>

namespace {

/* IFileDialogCustomize ids: control i is 100 + i, its visual group 5000 + i. */
const DWORD CN1_FDX_CONTROL_BASE = 100;
const DWORD CN1_FDX_GROUP_BASE = 5000;

struct Cn1FdxSplit {
    WCHAR* buf;      /* owned copy, '\n' replaced by NUL */
    WCHAR** fields;  /* pointers into buf */
    int count;
};

struct Cn1FdxRequest {
    int save;
    int multi;
    int dirs;
    int confirmOverwrite;
    int showHidden;
    int selectedFilter;
    WCHAR* title;
    WCHAR* acceptLabel;
    WCHAR* initialDir;
    WCHAR* suggestedName;
    WCHAR* clientGuid;
    WCHAR* defaultFolder;
    int filterCount;
    Cn1FdxSplit* filters;
    int optionCount;
    Cn1FdxSplit* options;

    /* answer, written on the pump thread */
    int accepted;
    int chosenFilter;
    WCHAR** values;   /* optionCount entries, CoTaskMem or malloc (see valueIsCoTask) */
    int* valueIsCoTask;
    WCHAR** paths;    /* CoTaskMem strings */
    int pathCount;
};

WCHAR* cn1FdxDup(const WCHAR* s) {
    size_t n;
    WCHAR* out;
    if (s == NULL) {
        return NULL;
    }
    n = wcslen(s);
    out = (WCHAR*) malloc((n + 1) * sizeof(WCHAR));
    if (out != NULL) {
        memcpy(out, s, (n + 1) * sizeof(WCHAR));
    }
    return out;
}

/* Java path (possibly "/C:/dir" from a file:// URL) to a Windows path, in place. */
void cn1FdxNormalizePath(WCHAR* p) {
    size_t i;
    if (p == NULL) {
        return;
    }
    if (p[0] == L'/' && p[1] != 0 && p[2] == L':') {
        memmove(p, p + 1, wcslen(p) * sizeof(WCHAR));
    }
    for (i = 0; p[i] != 0; i++) {
        if (p[i] == L'/') {
            p[i] = L'\\';
        }
    }
}

void cn1FdxSplitInto(WCHAR* buf, Cn1FdxSplit* out) {
    int n = 1;
    int i;
    WCHAR* c;
    out->buf = buf;
    out->fields = NULL;
    out->count = 0;
    if (buf == NULL) {
        return;
    }
    for (c = buf; *c != 0; c++) {
        if (*c == L'\n') {
            n++;
        }
    }
    out->fields = (WCHAR**) calloc((size_t) n, sizeof(WCHAR*));
    if (out->fields == NULL) {
        return;
    }
    out->fields[0] = buf;
    i = 1;
    for (c = buf; *c != 0; c++) {
        if (*c == L'\n') {
            *c = 0;
            out->fields[i++] = c + 1;
        }
    }
    out->count = n;
}

/* Reads a Java String[] into split wide strings. Called before yielding. */
Cn1FdxSplit* cn1FdxReadArray(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT arr, int* count) {
    int n, i;
    JAVA_OBJECT* elems;
    Cn1FdxSplit* out;
    *count = 0;
    if (arr == JAVA_NULL) {
        return NULL;
    }
    n = (int) (*(JAVA_ARRAY) arr).length;
    if (n <= 0) {
        return NULL;
    }
    out = (Cn1FdxSplit*) calloc((size_t) n, sizeof(Cn1FdxSplit));
    if (out == NULL) {
        return NULL;
    }
    elems = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
    for (i = 0; i < n; i++) {
        WCHAR* w = elems[i] != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, elems[i], NULL) : NULL;
        cn1FdxSplitInto(w, &out[i]);
    }
    *count = n;
    return out;
}

void cn1FdxFreeArray(Cn1FdxSplit* a, int n) {
    int i;
    if (a == NULL) {
        return;
    }
    for (i = 0; i < n; i++) {
        free(a[i].fields);
        free(a[i].buf);
    }
    free(a);
}

WCHAR* cn1FdxReadString(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT s) {
    return s != JAVA_NULL ? cn1WinJavaStringToWide(threadStateData, s, NULL) : NULL;
}

IShellItem* cn1FdxItem(const WCHAR* path) {
    IShellItem* item = NULL;
    if (path == NULL || path[0] == 0) {
        return NULL;
    }
    if (FAILED(SHCreateItemFromParsingName(path, NULL, IID_IShellItem, (void**) &item))) {
        return NULL;
    }
    return item;
}

/* Builds the options panel through IFileDialogCustomize. */
/* Keeps a save dialog's default extension on the selected filter. The shell
 * appends SetDefaultExtension's value to a bare name, so without this a user
 * who switches from PDF to PNG and types "report" gets report.pdf, which the
 * Java side then turns into report.pdf.png. */
class Cn1FdxEvents : public IFileDialogEvents {
public:
    Cn1FdxEvents(Cn1FdxRequest* r) : refs(1), req(r) {}
    virtual ~Cn1FdxEvents() {}
    IFACEMETHODIMP QueryInterface(REFIID riid, void** ppv) {
        if (riid == IID_IUnknown || riid == IID_IFileDialogEvents) {
            *ppv = static_cast<IFileDialogEvents*>(this);
            AddRef();
            return S_OK;
        }
        *ppv = NULL;
        return E_NOINTERFACE;
    }
    IFACEMETHODIMP_(ULONG) AddRef() { return (ULONG) InterlockedIncrement(&refs); }
    IFACEMETHODIMP_(ULONG) Release() {
        LONG n = InterlockedDecrement(&refs);
        if (n == 0) {
            delete this;
        }
        return (ULONG) n;
    }
    IFACEMETHODIMP OnTypeChange(IFileDialog* dlg) {
        UINT index = 0;
        if (SUCCEEDED(dlg->GetFileTypeIndex(&index)) && index >= 1 && (int) index <= req->filterCount) {
            Cn1FdxSplit* f = &req->filters[index - 1];
            dlg->SetDefaultExtension(f->count > 1 && wcscmp(f->fields[1], L"*") != 0 ? f->fields[1] : L"");
        }
        return S_OK;
    }
    IFACEMETHODIMP OnFileOk(IFileDialog*) { return S_OK; }
    IFACEMETHODIMP OnFolderChanging(IFileDialog*, IShellItem*) { return S_OK; }
    IFACEMETHODIMP OnFolderChange(IFileDialog*) { return S_OK; }
    IFACEMETHODIMP OnSelectionChange(IFileDialog*) { return S_OK; }
    /* Only the type change matters here; both conflicts keep the shell's own
     * handling, which needs the response set, not left indeterminate. */
    IFACEMETHODIMP OnShareViolation(IFileDialog*, IShellItem*, FDE_SHAREVIOLATION_RESPONSE* response) {
        *response = FDESVR_DEFAULT;
        return S_OK;
    }
    IFACEMETHODIMP OnOverwrite(IFileDialog*, IShellItem*, FDE_OVERWRITE_RESPONSE* response) {
        *response = FDEOR_DEFAULT;
        return S_OK;
    }
private:
    LONG refs;
    Cn1FdxRequest* req;
};

void cn1FdxAddOptions(IFileDialog* dlg, Cn1FdxRequest* r) {
    IFileDialogCustomize* cust = NULL;
    int i, k;
    if (r->optionCount <= 0
            || FAILED(dlg->QueryInterface(IID_IFileDialogCustomize, (void**) &cust))) {
        return;
    }
    for (i = 0; i < r->optionCount; i++) {
        Cn1FdxSplit* o = &r->options[i];
        DWORD id = CN1_FDX_CONTROL_BASE + (DWORD) i;
        DWORD group = CN1_FDX_GROUP_BASE + (DWORD) i;
        if (o->count < 4) {
            continue;
        }
        if (o->fields[0][0] == L'1') {
            cust->AddCheckButton(id, o->fields[2], _wcsicmp(o->fields[3], L"true") == 0 ? TRUE : FALSE);
        } else if (o->fields[0][0] == L'2') {
            cust->StartVisualGroup(group, o->fields[2]);
            cust->AddComboBox(id);
            for (k = 4; k < o->count; k++) {
                cust->AddControlItem(id, (DWORD) (k - 4), o->fields[k]);
                if (wcscmp(o->fields[k], o->fields[3]) == 0) {
                    cust->SetSelectedControlItem(id, (DWORD) (k - 4));
                }
            }
            cust->EndVisualGroup();
        } else {
            cust->StartVisualGroup(group, o->fields[2]);
            cust->AddEditBox(id, o->fields[3]);
            cust->EndVisualGroup();
        }
    }
    cust->Release();
}

void cn1FdxReadOptions(IFileDialog* dlg, Cn1FdxRequest* r) {
    IFileDialogCustomize* cust = NULL;
    int i;
    if (r->optionCount <= 0) {
        return;
    }
    r->values = (WCHAR**) calloc((size_t) r->optionCount, sizeof(WCHAR*));
    r->valueIsCoTask = (int*) calloc((size_t) r->optionCount, sizeof(int));
    if (r->values == NULL || r->valueIsCoTask == NULL
            || FAILED(dlg->QueryInterface(IID_IFileDialogCustomize, (void**) &cust))) {
        return;
    }
    for (i = 0; i < r->optionCount; i++) {
        Cn1FdxSplit* o = &r->options[i];
        DWORD id = CN1_FDX_CONTROL_BASE + (DWORD) i;
        if (o->count < 4) {
            continue;
        }
        if (o->fields[0][0] == L'1') {
            BOOL checked = FALSE;
            if (SUCCEEDED(cust->GetCheckButtonState(id, &checked))) {
                r->values[i] = cn1FdxDup(checked ? L"true" : L"false");
            }
        } else if (o->fields[0][0] == L'2') {
            DWORD sel = 0;
            if (SUCCEEDED(cust->GetSelectedControlItem(id, &sel)) && (int) sel + 4 < o->count) {
                r->values[i] = cn1FdxDup(o->fields[sel + 4]);
            }
        } else {
            WCHAR* text = NULL;
            if (SUCCEEDED(cust->GetEditBoxText(id, &text)) && text != NULL) {
                r->values[i] = text;
                r->valueIsCoTask[i] = 1;
            }
        }
    }
    cust->Release();
}

void cn1FdxAddPath(Cn1FdxRequest* r, IShellItem* item) {
    WCHAR* path = NULL;
    WCHAR** grown;
    if (item == NULL || FAILED(item->GetDisplayName(SIGDN_FILESYSPATH, &path)) || path == NULL) {
        return;
    }
    grown = (WCHAR**) realloc(r->paths, (size_t) (r->pathCount + 1) * sizeof(WCHAR*));
    if (grown == NULL) {
        CoTaskMemFree(path);
        return;
    }
    r->paths = grown;
    r->paths[r->pathCount++] = path;
}

void cn1FdxRun(Cn1FdxRequest* r) {
    IFileDialog* dlg = NULL;
    COMDLG_FILTERSPEC* specs = NULL;
    WCHAR** specText = NULL;
    FILEOPENDIALOGOPTIONS opts = 0;
    IShellItem* item;
    int i, j;
    HRESULT hr;

    hr = CoCreateInstance(r->save ? CLSID_FileSaveDialog : CLSID_FileOpenDialog, NULL,
            CLSCTX_INPROC_SERVER, r->save ? IID_IFileSaveDialog : IID_IFileOpenDialog, (void**) &dlg);
    if (FAILED(hr) || dlg == NULL) {
        return;
    }
    if (r->clientGuid != NULL && r->clientGuid[0] != 0) {
        /* CLSIDFromString wants the braced form; accept a bare GUID too. */
        WCHAR braced[64];
        GUID guid;
        if (r->clientGuid[0] == L'{') {
            _snwprintf(braced, 63, L"%ls", r->clientGuid);
        } else {
            _snwprintf(braced, 63, L"{%ls}", r->clientGuid);
        }
        braced[63] = 0;
        if (SUCCEEDED(CLSIDFromString(braced, &guid))) {
            dlg->SetClientGuid(guid);
        }
    }
    dlg->GetOptions(&opts);
    opts |= FOS_FORCEFILESYSTEM | FOS_NOCHANGEDIR;
    if (r->showHidden) {
        opts |= FOS_FORCESHOWHIDDEN;
    }
    if (r->save) {
        if (r->confirmOverwrite) {
            opts |= FOS_OVERWRITEPROMPT;
        } else {
            opts &= ~FOS_OVERWRITEPROMPT;
        }
    } else {
        opts |= FOS_PATHMUSTEXIST;
        if (r->dirs) {
            opts |= FOS_PICKFOLDERS;
        } else {
            opts |= FOS_FILEMUSTEXIST;
        }
        if (r->multi) {
            opts |= FOS_ALLOWMULTISELECT;
        }
    }
    dlg->SetOptions(opts);
    if (r->title != NULL) {
        dlg->SetTitle(r->title);
    }
    if (r->acceptLabel != NULL && r->acceptLabel[0] != 0) {
        dlg->SetOkButtonLabel(r->acceptLabel);
    }
    if (r->filterCount > 0 && !r->dirs) {
        specs = (COMDLG_FILTERSPEC*) calloc((size_t) r->filterCount, sizeof(COMDLG_FILTERSPEC));
        specText = (WCHAR**) calloc((size_t) r->filterCount, sizeof(WCHAR*));
        if (specs != NULL && specText != NULL) {
            for (i = 0; i < r->filterCount; i++) {
                Cn1FdxSplit* f = &r->filters[i];
                size_t len = 4;
                for (j = 1; j < f->count; j++) {
                    len += wcslen(f->fields[j]) + 3;
                }
                specText[i] = (WCHAR*) calloc(len, sizeof(WCHAR));
                if (specText[i] == NULL) {
                    continue;
                }
                if (f->count <= 1) {
                    wcscpy(specText[i], L"*.*");
                } else {
                    for (j = 1; j < f->count; j++) {
                        if (j > 1) {
                            wcscat(specText[i], L";");
                        }
                        wcscat(specText[i], L"*.");
                        wcscat(specText[i], f->fields[j]);
                    }
                }
                specs[i].pszName = f->count > 0 ? f->fields[0] : L"";
                specs[i].pszSpec = specText[i];
            }
            dlg->SetFileTypes((UINT) r->filterCount, specs);
            if (r->selectedFilter >= 0 && r->selectedFilter < r->filterCount) {
                Cn1FdxSplit* f = &r->filters[r->selectedFilter];
                dlg->SetFileTypeIndex((UINT) r->selectedFilter + 1); /* 1-based */
                if (r->save && f->count > 1) {
                    dlg->SetDefaultExtension(f->fields[1]);
                }
            }
        }
    }
    item = cn1FdxItem(r->defaultFolder);
    if (item != NULL) {
        dlg->SetDefaultFolder(item);
        item->Release();
    }
    item = cn1FdxItem(r->initialDir);
    if (item != NULL) {
        dlg->SetFolder(item);
        item->Release();
    }
    if (r->suggestedName != NULL && r->suggestedName[0] != 0) {
        dlg->SetFileName(r->suggestedName);
    }
    cn1FdxAddOptions(dlg, r);

    DWORD eventsCookie = 0;
    Cn1FdxEvents* events = NULL;
    if (r->save && r->filterCount > 1) {
        events = new Cn1FdxEvents(r);
        if (FAILED(dlg->Advise(events, &eventsCookie))) {
            eventsCookie = 0;
        }
    }
    HRESULT shown = dlg->Show(cn1Win.hwnd);
    if (events != NULL) {
        if (eventsCookie != 0) {
            dlg->Unadvise(eventsCookie);
        }
        events->Release();
    }
    if (SUCCEEDED(shown)) {
        UINT typeIndex = 0;
        r->accepted = 1;
        r->chosenFilter = -1;
        if (r->filterCount > 0 && !r->dirs && SUCCEEDED(dlg->GetFileTypeIndex(&typeIndex))
                && typeIndex >= 1 && (int) typeIndex <= r->filterCount) {
            r->chosenFilter = (int) typeIndex - 1;
        }
        cn1FdxReadOptions(dlg, r);
        if (r->save) {
            item = NULL;
            if (SUCCEEDED(dlg->GetResult(&item)) && item != NULL) {
                cn1FdxAddPath(r, item);
                item->Release();
            }
        } else {
            IShellItemArray* items = NULL;
            if (SUCCEEDED(((IFileOpenDialog*) dlg)->GetResults(&items)) && items != NULL) {
                DWORD n = 0, k;
                items->GetCount(&n);
                for (k = 0; k < n; k++) {
                    item = NULL;
                    if (SUCCEEDED(items->GetItemAt(k, &item)) && item != NULL) {
                        cn1FdxAddPath(r, item);
                        item->Release();
                    }
                }
                items->Release();
            }
        }
    }
    dlg->Release();
    if (specText != NULL) {
        for (i = 0; i < r->filterCount; i++) {
            free(specText[i]);
        }
        free(specText);
    }
    free(specs);
}

JAVA_OBJECT cn1FdxToJava(CODENAME_ONE_THREAD_STATE, const WCHAR* wide) {
    int needed;
    char* utf8;
    JAVA_OBJECT result;
    if (wide == NULL) {
        return JAVA_NULL;
    }
    needed = WideCharToMultiByte(CP_UTF8, 0, wide, -1, NULL, 0, NULL, NULL);
    if (needed <= 0) {
        return JAVA_NULL;
    }
    utf8 = (char*) malloc((size_t) needed);
    if (utf8 == NULL) {
        return JAVA_NULL;
    }
    if (WideCharToMultiByte(CP_UTF8, 0, wide, -1, utf8, needed, NULL, NULL) <= 0) {
        free(utf8);
        return JAVA_NULL;
    }
    result = newStringFromCString(threadStateData, utf8);
    free(utf8);
    return result;
}

} /* namespace */

extern "C" {

extern struct clazz class_array1__java_lang_String;

/* Runs on the pump thread (dispatched from cn1WinWndProc on WM_CN1_FILEDIALOGEX). */
LRESULT cn1WinFileDialogExHandleMessage(WPARAM wp) {
    Cn1FdxRequest* r = (Cn1FdxRequest*) wp;
    HRESULT init;
    if (r == NULL) {
        return 0;
    }
    /* The pump thread is normally an STA already (WebView2, OLE drag and drop);
     * S_FALSE / RPC_E_CHANGED_MODE just mean COM is up, and only a call that
     * succeeded is balanced with CoUninitialize. */
    init = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    cn1FdxRun(r);
    if (SUCCEEDED(init)) {
        CoUninitialize();
    }
    return 0;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_fileDialogEx___boolean_java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String_1ARRAY_int_boolean_boolean_boolean_boolean_java_lang_String_1ARRAY_java_lang_String_java_lang_String_R_java_lang_String_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_BOOLEAN save, JAVA_OBJECT title, JAVA_OBJECT acceptLabel,
        JAVA_OBJECT initialDir, JAVA_OBJECT suggestedName, JAVA_OBJECT filters, JAVA_INT selectedFilter,
        JAVA_BOOLEAN multi, JAVA_BOOLEAN dirs, JAVA_BOOLEAN confirmOverwrite, JAVA_BOOLEAN showHidden,
        JAVA_OBJECT options, JAVA_OBJECT clientGuid, JAVA_OBJECT defaultFolder) {
    Cn1FdxRequest r;
    JAVA_OBJECT arr = JAVA_NULL;
    int i;
    /* Headless screenshot mode has no window to own the dialog. */
    if (cn1Win.hwnd == NULL) {
        return JAVA_NULL;
    }
    memset(&r, 0, sizeof(r));
    r.save = save ? 1 : 0;
    r.multi = multi ? 1 : 0;
    r.dirs = dirs ? 1 : 0;
    r.confirmOverwrite = confirmOverwrite ? 1 : 0;
    r.showHidden = showHidden ? 1 : 0;
    r.selectedFilter = selectedFilter;
    r.chosenFilter = -1;
    /* Every Java string is copied before the thread state is yielded. */
    r.title = cn1FdxReadString(threadStateData, title);
    r.acceptLabel = cn1FdxReadString(threadStateData, acceptLabel);
    r.initialDir = cn1FdxReadString(threadStateData, initialDir);
    cn1FdxNormalizePath(r.initialDir);
    r.suggestedName = cn1FdxReadString(threadStateData, suggestedName);
    r.clientGuid = cn1FdxReadString(threadStateData, clientGuid);
    r.defaultFolder = cn1FdxReadString(threadStateData, defaultFolder);
    cn1FdxNormalizePath(r.defaultFolder);
    r.filters = cn1FdxReadArray(threadStateData, filters, &r.filterCount);
    r.options = cn1FdxReadArray(threadStateData, options, &r.optionCount);

    CN1_YIELD_THREAD;
    SendMessageW(cn1Win.hwnd, WM_CN1_FILEDIALOGEX, (WPARAM) &r, 0);
    CN1_RESUME_THREAD;

    if (r.accepted && r.pathCount > 0) {
        char num[16];
        arr = allocArray(threadStateData, 1 + r.optionCount + r.pathCount,
                &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
        if (arr != JAVA_NULL) {
            JAVA_OBJECT s;
            _snprintf(num, sizeof(num) - 1, "%d", r.chosenFilter);
            num[sizeof(num) - 1] = 0;
            s = newStringFromCString(threadStateData, num);
            ((JAVA_OBJECT*) CN1_ARRAY_DATA(arr))[0] = s;
            CN1_WRITE_BARRIER(arr, s);
            for (i = 0; i < r.optionCount; i++) {
                s = r.values != NULL ? cn1FdxToJava(threadStateData, r.values[i]) : JAVA_NULL;
                ((JAVA_OBJECT*) CN1_ARRAY_DATA(arr))[1 + i] = s;
                CN1_WRITE_BARRIER(arr, s);
            }
            for (i = 0; i < r.pathCount; i++) {
                s = cn1FdxToJava(threadStateData, r.paths[i]);
                ((JAVA_OBJECT*) CN1_ARRAY_DATA(arr))[1 + r.optionCount + i] = s;
                CN1_WRITE_BARRIER(arr, s);
            }
        }
    }

    free(r.title);
    free(r.acceptLabel);
    free(r.initialDir);
    free(r.suggestedName);
    free(r.clientGuid);
    free(r.defaultFolder);
    cn1FdxFreeArray(r.filters, r.filterCount);
    if (r.values != NULL) {
        for (i = 0; i < r.optionCount; i++) {
            if (r.values[i] == NULL) {
                continue;
            }
            if (r.valueIsCoTask != NULL && r.valueIsCoTask[i]) {
                CoTaskMemFree(r.values[i]);
            } else {
                free(r.values[i]);
            }
        }
        free(r.values);
    }
    free(r.valueIsCoTask);
    cn1FdxFreeArray(r.options, r.optionCount);
    for (i = 0; i < r.pathCount; i++) {
        CoTaskMemFree(r.paths[i]);
    }
    free(r.paths);
    return arr;
}

} /* extern "C" */

#endif /* _WIN32 */
