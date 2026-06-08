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
 * Native HTTP(S) client for the Codename One Windows (Win32) port, implemented
 * over WinHTTP. The Java side (com.codename1.impl.windows.WindowsNative) drives
 * a connection through a long peer that is really a CN1Connection* cast to
 * JAVA_LONG. The request is built incrementally (open -> method -> headers ->
 * body) and only flushed to the wire by cn1NetEnsureSent the first time any
 * response-side accessor is invoked, so headers and a buffered request body can
 * all be supplied before the single WinHttpSendRequest / WinHttpReceiveResponse
 * pair runs.
 */

#ifdef _WIN32

#include "cn1_windows.h"
#include <winhttp.h>

/*
 * A String[] return value needs the array class of java.lang.String; the
 * translator emits this symbol for the one-dimensional String array type.
 */
extern struct clazz class_array1__java_lang_String;

typedef struct {
    HINTERNET session;
    HINTERNET connect;
    HINTERNET request;
    WCHAR* host;
    WCHAR* path;
    INTERNET_PORT port;
    BOOL https;
    BOOL post;
    BOOL sent;
    BOOL responseReceived;
    BYTE* pendingBody;
    DWORD pendingLen;
    DWORD pendingCap;
} CN1Connection;

/* ------------------------------------------------------------- internals */

/*
 * (Re)create the request handle using the current verb. WinHTTP needs the verb
 * fixed at WinHttpOpenRequest time, so toggling the method recreates it. Headers
 * are applied directly to the live handle, so this is only ever called before
 * any header has been added (httpOpen / httpSetMethod, both pre-header).
 */
static BOOL cn1NetCreateRequest(CN1Connection* conn) {
    DWORD flags = conn->https ? WINHTTP_FLAG_SECURE : 0;
    const WCHAR* verb = conn->post ? L"POST" : L"GET";
    if (conn->request != NULL) {
        WinHttpCloseHandle(conn->request);
        conn->request = NULL;
    }
    conn->request = WinHttpOpenRequest(conn->connect, verb,
                                       conn->path ? conn->path : L"/",
                                       NULL, WINHTTP_NO_REFERER,
                                       WINHTTP_DEFAULT_ACCEPT_TYPES, flags);
    return conn->request != NULL;
}

/*
 * Send the request exactly once. The buffered request body (if any) is handed
 * to WinHttpSendRequest in a single shot, then WinHttpReceiveResponse blocks for
 * the status line + headers. Subsequent calls are no-ops so every response
 * accessor can call this freely.
 */
static BOOL cn1NetEnsureSent(CN1Connection* conn) {
    BOOL ok;
    if (conn == NULL || conn->request == NULL) {
        return FALSE;
    }
    if (conn->sent) {
        return conn->responseReceived;
    }
    conn->sent = TRUE;

    ok = WinHttpSendRequest(conn->request,
                            WINHTTP_NO_ADDITIONAL_HEADERS, 0,
                            conn->pendingLen > 0 ? (LPVOID)conn->pendingBody
                                                 : WINHTTP_NO_REQUEST_DATA,
                            conn->pendingLen,
                            conn->pendingLen,
                            0);
    if (!ok) {
        cn1WindowsLog("cn1NetEnsureSent: WinHttpSendRequest failed");
        return FALSE;
    }
    ok = WinHttpReceiveResponse(conn->request, NULL);
    if (!ok) {
        cn1WindowsLog("cn1NetEnsureSent: WinHttpReceiveResponse failed");
        return FALSE;
    }
    conn->responseReceived = TRUE;
    return TRUE;
}

/*
 * Query a (wide) header value into a freshly malloc'd buffer. infoLevel and the
 * optional custom name follow WinHttpQueryHeaders semantics. Returns NULL when
 * the header is absent or on error; caller frees.
 */
static WCHAR* cn1NetQueryHeaderString(CN1Connection* conn, DWORD infoLevel, const WCHAR* name) {
    DWORD size = 0;
    WCHAR* buffer;
    if (!cn1NetEnsureSent(conn)) {
        return NULL;
    }
    WinHttpQueryHeaders(conn->request, infoLevel,
                        name ? name : WINHTTP_HEADER_NAME_BY_INDEX,
                        WINHTTP_NO_OUTPUT_BUFFER, &size, WINHTTP_NO_HEADER_INDEX);
    if (size == 0) {
        return NULL;
    }
    /* size is in bytes and does not include room beyond the terminator. */
    buffer = (WCHAR*)malloc(size + sizeof(WCHAR));
    if (buffer == NULL) {
        return NULL;
    }
    if (!WinHttpQueryHeaders(conn->request, infoLevel,
                             name ? name : WINHTTP_HEADER_NAME_BY_INDEX,
                             buffer, &size, WINHTTP_NO_HEADER_INDEX)) {
        free(buffer);
        return NULL;
    }
    buffer[size / sizeof(WCHAR)] = 0;
    return buffer;
}

/* Convert a NUL-terminated wide string into a Java String. */
static JAVA_OBJECT cn1NetWideToJavaString(CODENAME_ONE_THREAD_STATE, const WCHAR* wide) {
    int utf8Len;
    char* utf8;
    JAVA_OBJECT result;
    if (wide == NULL) {
        return JAVA_NULL;
    }
    utf8Len = WideCharToMultiByte(CP_UTF8, 0, wide, -1, NULL, 0, NULL, NULL);
    if (utf8Len <= 0) {
        return JAVA_NULL;
    }
    utf8 = (char*)malloc(utf8Len);
    if (utf8 == NULL) {
        return JAVA_NULL;
    }
    WideCharToMultiByte(CP_UTF8, 0, wide, -1, utf8, utf8Len, NULL, NULL);
    result = newStringFromCString(threadStateData, utf8);
    free(utf8);
    return result;
}

/* ------------------------------------------------------------- bridges */

JAVA_LONG com_codename1_impl_windows_WindowsNative_httpOpen___java_lang_String_boolean_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1Arg1, JAVA_BOOLEAN __cn1Arg2, JAVA_BOOLEAN __cn1Arg3) {
    WCHAR* url;
    UINT32 urlLen = 0;
    URL_COMPONENTS comps;
    WCHAR hostName[256];
    WCHAR urlPath[2048];
    CN1Connection* conn;

    url = cn1WinJavaStringToWide(threadStateData, __cn1Arg1, &urlLen);
    if (url == NULL) {
        return 0;
    }

    ZeroMemory(&comps, sizeof(comps));
    comps.dwStructSize = sizeof(comps);
    comps.lpszHostName = hostName;
    comps.dwHostNameLength = sizeof(hostName) / sizeof(WCHAR);
    comps.lpszUrlPath = urlPath;
    comps.dwUrlPathLength = sizeof(urlPath) / sizeof(WCHAR);

    if (!WinHttpCrackUrl(url, 0, 0, &comps)) {
        cn1WindowsLog("httpOpen: WinHttpCrackUrl failed");
        free(url);
        return 0;
    }
    free(url);

    conn = (CN1Connection*)malloc(sizeof(CN1Connection));
    if (conn == NULL) {
        return 0;
    }
    ZeroMemory(conn, sizeof(CN1Connection));
    conn->port = comps.nPort;
    conn->https = (comps.nScheme == INTERNET_SCHEME_HTTPS);
    conn->post = FALSE;

    /* hostName is bounded above and NUL-terminated by WinHttpCrackUrl when a
     * caller buffer is supplied; copy host and path into owned buffers. */
    {
        size_t hostChars = comps.dwHostNameLength;
        size_t pathChars = comps.dwUrlPathLength;
        conn->host = (WCHAR*)malloc((hostChars + 1) * sizeof(WCHAR));
        conn->path = (WCHAR*)malloc((pathChars + 1) * sizeof(WCHAR));
        if (conn->host == NULL || conn->path == NULL) {
            free(conn->host);
            free(conn->path);
            free(conn);
            return 0;
        }
        memcpy(conn->host, hostName, hostChars * sizeof(WCHAR));
        conn->host[hostChars] = 0;
        memcpy(conn->path, urlPath, pathChars * sizeof(WCHAR));
        conn->path[pathChars] = 0;
        if (pathChars == 0) {
            conn->path[0] = L'/';
            conn->path[1] = 0;
        }
    }

    conn->session = WinHttpOpen(L"CodenameOne",
                                WINHTTP_ACCESS_TYPE_AUTOMATIC_PROXY,
                                WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (conn->session == NULL) {
        cn1WindowsLog("httpOpen: WinHttpOpen failed");
        free(conn->host);
        free(conn->path);
        free(conn);
        return 0;
    }

    conn->connect = WinHttpConnect(conn->session, conn->host, conn->port, 0);
    if (conn->connect == NULL) {
        cn1WindowsLog("httpOpen: WinHttpConnect failed");
        WinHttpCloseHandle(conn->session);
        free(conn->host);
        free(conn->path);
        free(conn);
        return 0;
    }

    if (!cn1NetCreateRequest(conn)) {
        cn1WindowsLog("httpOpen: WinHttpOpenRequest failed");
        WinHttpCloseHandle(conn->connect);
        WinHttpCloseHandle(conn->session);
        free(conn->host);
        free(conn->path);
        free(conn);
        return 0;
    }

    return (JAVA_LONG)(intptr_t)conn;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_httpSetMethod___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_BOOLEAN __cn1Arg2) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    BOOL post = __cn1Arg2 ? TRUE : FALSE;
    if (conn == NULL || conn->sent) {
        return;
    }
    if (conn->post != post) {
        conn->post = post;
        /* Recreate the request handle with the new verb. */
        cn1NetCreateRequest(conn);
    }
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_httpSetHeader___long_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_OBJECT __cn1Arg3) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    WCHAR* key;
    WCHAR* value;
    UINT32 keyLen = 0;
    UINT32 valueLen = 0;
    WCHAR* line;
    size_t lineChars;

    if (conn == NULL || conn->request == NULL || conn->sent) {
        return;
    }
    key = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &keyLen);
    value = cn1WinJavaStringToWide(threadStateData, __cn1Arg3, &valueLen);
    if (key == NULL || value == NULL) {
        free(key);
        free(value);
        return;
    }

    /* "key: value" plus terminator. */
    lineChars = keyLen + 2 + valueLen + 1;
    line = (WCHAR*)malloc(lineChars * sizeof(WCHAR));
    if (line != NULL) {
        memcpy(line, key, keyLen * sizeof(WCHAR));
        line[keyLen] = L':';
        line[keyLen + 1] = L' ';
        memcpy(line + keyLen + 2, value, valueLen * sizeof(WCHAR));
        line[keyLen + 2 + valueLen] = 0;
        WinHttpAddRequestHeaders(conn->request, line, (DWORD)-1L,
                                 WINHTTP_ADDREQ_FLAG_ADD | WINHTTP_ADDREQ_FLAG_REPLACE);
        free(line);
    }
    free(key);
    free(value);
}

JAVA_INT com_codename1_impl_windows_WindowsNative_httpResponseCode___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    DWORD statusCode = 0;
    DWORD size = sizeof(statusCode);
    if (!cn1NetEnsureSent(conn)) {
        return -1;
    }
    if (!WinHttpQueryHeaders(conn->request,
                             WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                             WINHTTP_HEADER_NAME_BY_INDEX, &statusCode, &size,
                             WINHTTP_NO_HEADER_INDEX)) {
        return -1;
    }
    return (JAVA_INT)statusCode;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_httpResponseMessage___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    WCHAR* text;
    JAVA_OBJECT result;
    if (!cn1NetEnsureSent(conn)) {
        return JAVA_NULL;
    }
    text = cn1NetQueryHeaderString(conn, WINHTTP_QUERY_STATUS_TEXT, NULL);
    if (text == NULL) {
        return JAVA_NULL;
    }
    result = cn1NetWideToJavaString(threadStateData, text);
    free(text);
    return result;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_httpContentLength___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    DWORD contentLength = 0;
    DWORD size = sizeof(contentLength);
    if (!cn1NetEnsureSent(conn)) {
        return -1;
    }
    if (!WinHttpQueryHeaders(conn->request,
                             WINHTTP_QUERY_CONTENT_LENGTH | WINHTTP_QUERY_FLAG_NUMBER,
                             WINHTTP_HEADER_NAME_BY_INDEX, &contentLength, &size,
                             WINHTTP_NO_HEADER_INDEX)) {
        return -1;
    }
    return (JAVA_INT)contentLength;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_httpHeaderField___long_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    WCHAR* name;
    WCHAR* value;
    JAVA_OBJECT result;
    UINT32 nameLen = 0;
    if (!cn1NetEnsureSent(conn)) {
        return JAVA_NULL;
    }
    name = cn1WinJavaStringToWide(threadStateData, __cn1Arg2, &nameLen);
    if (name == NULL) {
        return JAVA_NULL;
    }
    /* The name buffer is reused by WinHTTP as the [in] header name with the
     * CUSTOM info level. */
    value = cn1NetQueryHeaderString(conn, WINHTTP_QUERY_CUSTOM, name);
    free(name);
    if (value == NULL) {
        return JAVA_NULL;
    }
    result = cn1NetWideToJavaString(threadStateData, value);
    free(value);
    return result;
}

JAVA_OBJECT com_codename1_impl_windows_WindowsNative_httpHeaderFieldNames___long_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    WCHAR* raw;
    JAVA_OBJECT arr;
    int count = 0;
    int index = 0;
    WCHAR* p;
    BOOL firstLine = TRUE;

    if (!cn1NetEnsureSent(conn)) {
        return JAVA_NULL;
    }
    /* WINHTTP_QUERY_RAW_HEADERS_CRLF returns the full block of headers joined by
     * CRLF, beginning with the status line and ending with a double CRLF. */
    raw = cn1NetQueryHeaderString(conn, WINHTTP_QUERY_RAW_HEADERS_CRLF, NULL);
    if (raw == NULL) {
        return JAVA_NULL;
    }

    /* Pass 1: count header lines that contain a ':' (skip the status line). */
    p = raw;
    firstLine = TRUE;
    while (*p != 0) {
        WCHAR* lineStart = p;
        WCHAR* colon = NULL;
        while (*p != 0 && !(p[0] == L'\r' && p[1] == L'\n')) {
            if (colon == NULL && *p == L':') {
                colon = p;
            }
            p++;
        }
        if (!firstLine && lineStart != p && colon != NULL) {
            count++;
        }
        firstLine = FALSE;
        if (p[0] == L'\r' && p[1] == L'\n') {
            p += 2;
        }
    }

    /* Allocate the String[] result. allocArray for an object array uses the
     * array class, sizeof(JAVA_OBJECT) element size and dimension 1; this is the
     * canonical CN1 idiom and is flagged here for first-build verification. */
    arr = allocArray(threadStateData, count, &class_array1__java_lang_String,
                     sizeof(JAVA_OBJECT), 1);
    if (arr == JAVA_NULL) {
        free(raw);
        return JAVA_NULL;
    }

    /* Pass 2: copy each header name (text before its first ':') into the array. */
    p = raw;
    firstLine = TRUE;
    while (*p != 0 && index < count) {
        WCHAR* lineStart = p;
        WCHAR* colon = NULL;
        while (*p != 0 && !(p[0] == L'\r' && p[1] == L'\n')) {
            if (colon == NULL && *p == L':') {
                colon = p;
            }
            p++;
        }
        if (!firstLine && lineStart != p && colon != NULL) {
            size_t nameChars = (size_t)(colon - lineStart);
            WCHAR* name = (WCHAR*)malloc((nameChars + 1) * sizeof(WCHAR));
            if (name != NULL) {
                memcpy(name, lineStart, nameChars * sizeof(WCHAR));
                name[nameChars] = 0;
                /* Object-array element store: write the String into the data
                 * slot, again flagged for first-build verification. */
                ((JAVA_OBJECT*)(*(JAVA_ARRAY)arr).data)[index] =
                    cn1NetWideToJavaString(threadStateData, name);
                free(name);
                index++;
            }
        }
        firstLine = FALSE;
        if (p[0] == L'\r' && p[1] == L'\n') {
            p += 2;
        }
    }

    free(raw);
    return arr;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_httpReadBody___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    DWORD bytesRead = 0;
    if (!cn1NetEnsureSent(conn)) {
        return -1;
    }
    if (__cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return 0;
    }
    data = (JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)__cn1Arg2).data;
    if (!WinHttpReadData(conn->request, (LPVOID)(data + __cn1Arg3),
                         (DWORD)__cn1Arg4, &bytesRead)) {
        cn1WindowsLog("httpReadBody: WinHttpReadData failed");
        return -1;
    }
    if (bytesRead == 0) {
        return -1; /* EOF */
    }
    return (JAVA_INT)bytesRead;
}

JAVA_INT com_codename1_impl_windows_WindowsNative_httpWriteBody___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1, JAVA_OBJECT __cn1Arg2, JAVA_INT __cn1Arg3, JAVA_INT __cn1Arg4) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    JAVA_ARRAY_BYTE* data;
    if (conn == NULL || conn->sent || __cn1Arg2 == JAVA_NULL || __cn1Arg4 <= 0) {
        return 0;
    }
    data = (JAVA_ARRAY_BYTE*)(*(JAVA_ARRAY)__cn1Arg2).data;

    /* Grow the pending body buffer as needed and append the new bytes. The
     * accumulated buffer is transmitted in a single WinHttpSendRequest. */
    if (conn->pendingLen + (DWORD)__cn1Arg4 > conn->pendingCap) {
        DWORD newCap = conn->pendingCap == 0 ? 256 : conn->pendingCap * 2;
        BYTE* grown;
        while (newCap < conn->pendingLen + (DWORD)__cn1Arg4) {
            newCap *= 2;
        }
        grown = (BYTE*)realloc(conn->pendingBody, newCap);
        if (grown == NULL) {
            return 0;
        }
        conn->pendingBody = grown;
        conn->pendingCap = newCap;
    }
    memcpy(conn->pendingBody + conn->pendingLen, data + __cn1Arg3, (size_t)__cn1Arg4);
    conn->pendingLen += (DWORD)__cn1Arg4;
    return __cn1Arg4;
}

JAVA_VOID com_codename1_impl_windows_WindowsNative_httpClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG __cn1Arg1) {
    CN1Connection* conn = (CN1Connection*)(intptr_t)__cn1Arg1;
    if (conn == NULL) {
        return;
    }
    if (conn->request != NULL) {
        WinHttpCloseHandle(conn->request);
    }
    if (conn->connect != NULL) {
        WinHttpCloseHandle(conn->connect);
    }
    if (conn->session != NULL) {
        WinHttpCloseHandle(conn->session);
    }
    free(conn->host);
    free(conn->path);
    free(conn->pendingBody);
    free(conn);
}

#endif /* _WIN32 */
