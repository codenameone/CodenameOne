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
 * Windows 11 Widgets Board provider: the MSIX lowering of
 * com.codename1.surfaces. This whole file is compile-gated behind
 * CN1_WIDGETBOARD, which WindowsNativeBuilder defines only for
 * windows.msix=true builds -- the default CMake build has no Windows App SDK
 * headers and is completely unaffected (the only unguarded symbol is absent;
 * the sole call site in cn1_windows_window.cpp is equally guarded).
 *
 * How it works
 * ------------
 * The Widgets Board hosts third-party widgets rendered from Adaptive Cards
 * JSON. A packaged (MSIX) app declares:
 *   - a COM ExeServer (`app.exe -RegisterProcessAsComServer`) exposing
 *     CN1_WIDGET_PROVIDER_CLSID, and
 *   - a `com.microsoft.windows.widgets` uap3:AppExtension whose widget
 *     Definitions are generated from the project's surfaces.json.
 * When the user pins a widget, Windows activates the exe with
 * -RegisterProcessAsComServer; cn1WidgetBoardMaybeRunComServer() (called from
 * the first native of every launch, initDisplay) detects the flag, bootstraps
 * the WindowsAppRuntime, registers the class factory and serves
 * IWidgetProvider calls until the board deactivates the last widget -- the
 * Codename One app UI never starts in this mode.
 *
 * Content comes from the same persisted surface data the in-app bridge
 * (WindowsWidgetBridge) writes: %LOCALAPPDATA%\CodenameOne\cn1surfaces\<kind>\
 * timeline.json + <hash>.png. (Under MSIX the path is transparently redirected
 * into the package's AppData view; both the app and this provider run with the
 * same package identity, so they see the same files.) The descriptor is mapped
 * to an Adaptive Card, documented approximations and all:
 *
 *   col / row / box -> Container / ColumnSet / Container (AC has no overlay
 *                      stacking, so box degrades to a vertical Container)
 *   text            -> TextBlock (size/weight best-effort buckets; role colors
 *                      map to Default/Accent/isSubtle; explicit ARGB colors are
 *                      not representable in AC and fall back to Default)
 *   dyn             -> TextBlock with a statically computed value; AC has no
 *                      countdown primitive, so while any widget is active a
 *                      timer re-templates and re-pushes the card once a minute
 *                      (second-precision ticking is not possible on the board)
 *   img             -> Image with a data: URI (base64 of the persisted PNG)
 *   prog            -> two-Column ColumnSet emulating a bar: proportional
 *                      pixel widths, filled column styled "accent"
 *   spacer          -> stretch Column in a row / empty TextBlock in a column
 *
 * Actions: nodes carrying an action id become Action.Execute selectActions
 * with the action id as the verb. OnActionInvoked appends
 * "source\tactionId\tparamsJson" to cn1surfaces\pending_actions.txt and
 * launches the app exe; WindowsWidgetBridge drains that file on startup into
 * Surfaces.dispatchAction's cold-start queue.
 *
 * Runtime dependency
 * ------------------
 * The provider APIs live in the Windows App SDK (WindowsAppRuntime). Because
 * the CN1 exe is a plain win32 app (not framework-dependent at build time),
 * MddBootstrapInitialize dynamically binds the installed WindowsAppRuntime
 * package at startup; the target machine must have the matching
 * WindowsAppRuntime redistributable installed (documented in
 * WindowsNativeBuilder next to the MSIX packaging step). Building this file
 * additionally needs the Windows App SDK headers/libs + C++/WinRT projections,
 * pointed at by the CN1_WINAPPSDK_DIR environment variable at build time.
 *
 * NOTE: this translation unit cannot be compiled or exercised outside a
 * Windows host with the Windows App SDK present; it is reviewed-not-compiled
 * on other platforms and is kept intentionally self-contained.
 */

#ifdef CN1_WIDGETBOARD

/* Include the C++ standard library and SDK headers BEFORE cn1_windows.h:
 * cn1_globals.h installs macros for the bytecode runtime that otherwise break
 * the STL headers. */
#include <windows.h>
#include <shellapi.h>
#include <shlobj.h>
#include <string>
#include <vector>
#include <map>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

/* C++/WinRT + Windows App SDK (projection headers generated from the WinAppSDK
 * winmd; shipped in the SDK layout CN1_WINAPPSDK_DIR points at). */
#include <winrt/base.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Microsoft.Windows.Widgets.h>
#include <winrt/Microsoft.Windows.Widgets.Providers.h>
#include <MddBootstrap.h>

#endif /* CN1_WIDGETBOARD */

#include "cn1_windows.h"

#ifdef CN1_WIDGETBOARD

using namespace winrt::Microsoft::Windows::Widgets::Providers;
using winrt::Microsoft::Windows::Widgets::WidgetSize;

/* Must match the com:Class Id in the AppxManifest WindowsNativeBuilder
 * generates (WIDGET_PROVIDER_CLSID there). */
static const CLSID CN1_WIDGET_PROVIDER_CLSID =
    { 0xc0de4a11, 0x5a2f, 0x4e7b, { 0x9c, 0x61, 0x7d, 0x1b, 0x4a, 0x0c, 0x8e, 0x52 } };

/* Windows App SDK 1.5 release series; the bootstrap binds any installed
 * runtime with this major.minor and a compatible version tag. */
#define CN1_WINAPPSDK_MAJORMINOR 0x00010005

/* ============================================================== tiny JSON */

/*
 * A minimal recursive-descent JSON reader for the surfaces wire format. The
 * descriptors are machine-generated by SurfaceSerializer (no exotic escapes,
 * no comments), so this deliberately supports exactly RFC 8259 objects,
 * arrays, strings (with the standard escapes), numbers, booleans and null.
 */
struct CN1Json {
    enum Type { JNULL, JBOOL, JNUM, JSTR, JARR, JOBJ };
    Type type = JNULL;
    bool boolean = false;
    double number = 0;
    std::string str;
    std::vector<CN1Json> arr;
    std::map<std::string, CN1Json> obj;

    bool isMap() const { return type == JOBJ; }
    bool isNum() const { return type == JNUM; }
    bool isStr() const { return type == JSTR; }
    const CN1Json* get(const char* key) const {
        if (type != JOBJ) return NULL;
        std::map<std::string, CN1Json>::const_iterator it = obj.find(key);
        return it == obj.end() ? NULL : &it->second;
    }
    std::string getStr(const char* key, const char* def) const {
        const CN1Json* v = get(key);
        return (v != NULL && v->type == JSTR) ? v->str : std::string(def);
    }
    double getNum(const char* key, double def) const {
        const CN1Json* v = get(key);
        return (v != NULL && v->type == JNUM) ? v->number : def;
    }
};

struct CN1JsonParser {
    const char* p;
    const char* end;

    CN1JsonParser(const std::string& s) : p(s.c_str()), end(s.c_str() + s.size()) {}

    void skipWs() {
        while (p < end && (*p == ' ' || *p == '\t' || *p == '\r' || *p == '\n')) p++;
    }
    bool literal(const char* lit) {
        size_t n = strlen(lit);
        if ((size_t) (end - p) >= n && strncmp(p, lit, n) == 0) { p += n; return true; }
        return false;
    }
    bool parseString(std::string& out) {
        if (p >= end || *p != '"') return false;
        p++;
        out.clear();
        while (p < end && *p != '"') {
            if (*p == '\\' && p + 1 < end) {
                p++;
                switch (*p) {
                    case 'n': out += '\n'; break;
                    case 't': out += '\t'; break;
                    case 'r': out += '\r'; break;
                    case 'b': out += '\b'; break;
                    case 'f': out += '\f'; break;
                    case 'u': {
                        /* decode \uXXXX to UTF-8 (surrogate pairs handled) */
                        if (end - p < 5) return false;
                        unsigned cp = (unsigned) strtoul(std::string(p + 1, p + 5).c_str(), NULL, 16);
                        p += 4;
                        if (cp >= 0xD800 && cp <= 0xDBFF && end - p >= 7
                                && p[1] == '\\' && p[2] == 'u') {
                            unsigned lo = (unsigned) strtoul(std::string(p + 3, p + 7).c_str(), NULL, 16);
                            if (lo >= 0xDC00 && lo <= 0xDFFF) {
                                cp = 0x10000 + ((cp - 0xD800) << 10) + (lo - 0xDC00);
                                p += 6;
                            }
                        }
                        if (cp < 0x80) {
                            out += (char) cp;
                        } else if (cp < 0x800) {
                            out += (char) (0xC0 | (cp >> 6));
                            out += (char) (0x80 | (cp & 0x3F));
                        } else if (cp < 0x10000) {
                            out += (char) (0xE0 | (cp >> 12));
                            out += (char) (0x80 | ((cp >> 6) & 0x3F));
                            out += (char) (0x80 | (cp & 0x3F));
                        } else {
                            out += (char) (0xF0 | (cp >> 18));
                            out += (char) (0x80 | ((cp >> 12) & 0x3F));
                            out += (char) (0x80 | ((cp >> 6) & 0x3F));
                            out += (char) (0x80 | (cp & 0x3F));
                        }
                        break;
                    }
                    default: out += *p; break;
                }
                p++;
            } else {
                out += *p++;
            }
        }
        if (p >= end) return false;
        p++; /* closing quote */
        return true;
    }
    bool parseValue(CN1Json& out) {
        skipWs();
        if (p >= end) return false;
        if (*p == '{') {
            p++;
            out.type = CN1Json::JOBJ;
            skipWs();
            if (p < end && *p == '}') { p++; return true; }
            for (;;) {
                skipWs();
                std::string key;
                if (!parseString(key)) return false;
                skipWs();
                if (p >= end || *p != ':') return false;
                p++;
                CN1Json value;
                if (!parseValue(value)) return false;
                out.obj[key] = value;
                skipWs();
                if (p < end && *p == ',') { p++; continue; }
                if (p < end && *p == '}') { p++; return true; }
                return false;
            }
        }
        if (*p == '[') {
            p++;
            out.type = CN1Json::JARR;
            skipWs();
            if (p < end && *p == ']') { p++; return true; }
            for (;;) {
                CN1Json value;
                if (!parseValue(value)) return false;
                out.arr.push_back(value);
                skipWs();
                if (p < end && *p == ',') { p++; continue; }
                if (p < end && *p == ']') { p++; return true; }
                return false;
            }
        }
        if (*p == '"') {
            out.type = CN1Json::JSTR;
            return parseString(out.str);
        }
        if (literal("true")) { out.type = CN1Json::JBOOL; out.boolean = true; return true; }
        if (literal("false")) { out.type = CN1Json::JBOOL; out.boolean = false; return true; }
        if (literal("null")) { out.type = CN1Json::JNULL; return true; }
        /* number */
        char* numEnd = NULL;
        double v = strtod(p, &numEnd);
        if (numEnd == p) return false;
        out.type = CN1Json::JNUM;
        out.number = v;
        p = numEnd;
        return true;
    }
};

static bool cn1JsonParse(const std::string& text, CN1Json& out) {
    CN1JsonParser parser(text);
    return parser.parseValue(out);
}

/* ======================================================== file utilities */

/* The same persistence root WindowsWidgetBridge writes:
 * %LOCALAPPDATA%\CodenameOne\cn1surfaces (redirected into the package view
 * under MSIX -- both processes share the package identity). */
static std::wstring cn1BoardSurfacesRoot() {
    WCHAR base[MAX_PATH];
    if (SHGetFolderPathW(NULL, CSIDL_LOCAL_APPDATA, NULL, SHGFP_TYPE_CURRENT, base) != S_OK) {
        return std::wstring();
    }
    std::wstring root(base);
    root += L"\\CodenameOne\\cn1surfaces";
    return root;
}

static bool cn1BoardReadFile(const std::wstring& path, std::string& out) {
    HANDLE f = CreateFileW(path.c_str(), GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE,
            NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (f == INVALID_HANDLE_VALUE) {
        return false;
    }
    out.clear();
    char buf[8192];
    DWORD n = 0;
    while (ReadFile(f, buf, sizeof(buf), &n, NULL) && n > 0) {
        out.append(buf, (size_t) n);
    }
    CloseHandle(f);
    return true;
}

static std::wstring cn1BoardUtf8ToWide(const std::string& s) {
    if (s.empty()) return std::wstring();
    int n = MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int) s.size(), NULL, 0);
    std::wstring w((size_t) n, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int) s.size(), &w[0], n);
    return w;
}

static const char CN1_B64_ALPHABET[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

static std::string cn1BoardBase64(const std::string& data) {
    std::string out;
    out.reserve(((data.size() + 2) / 3) * 4);
    size_t i = 0;
    while (i + 2 < data.size()) {
        unsigned v = ((unsigned char) data[i] << 16)
                | ((unsigned char) data[i + 1] << 8) | (unsigned char) data[i + 2];
        out += CN1_B64_ALPHABET[(v >> 18) & 63];
        out += CN1_B64_ALPHABET[(v >> 12) & 63];
        out += CN1_B64_ALPHABET[(v >> 6) & 63];
        out += CN1_B64_ALPHABET[v & 63];
        i += 3;
    }
    if (i + 1 == data.size()) {
        unsigned v = ((unsigned char) data[i]) << 16;
        out += CN1_B64_ALPHABET[(v >> 18) & 63];
        out += CN1_B64_ALPHABET[(v >> 12) & 63];
        out += "==";
    } else if (i + 2 == data.size()) {
        unsigned v = (((unsigned char) data[i]) << 16) | (((unsigned char) data[i + 1]) << 8);
        out += CN1_B64_ALPHABET[(v >> 18) & 63];
        out += CN1_B64_ALPHABET[(v >> 12) & 63];
        out += CN1_B64_ALPHABET[(v >> 6) & 63];
        out += '=';
    }
    return out;
}

/* JSON string escaping for the emitted Adaptive Card. */
static std::string cn1BoardJsonEscape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 8);
    for (size_t i = 0; i < s.size(); i++) {
        char c = s[i];
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if ((unsigned char) c < 0x20) {
                    char esc[8];
                    _snprintf(esc, sizeof(esc), "\\u%04x", (unsigned char) c);
                    out += esc;
                } else {
                    out += c;
                }
                break;
        }
    }
    return out;
}

/* ================================================== descriptor -> AC JSON */

/* ${key} interpolation from the entry's state map (missing keys -> empty). */
static std::string cn1BoardInterpolate(const std::string& text, const CN1Json* state) {
    if (text.find("${") == std::string::npos) {
        return text;
    }
    std::string out;
    size_t i = 0;
    while (i < text.size()) {
        size_t start = text.find("${", i);
        if (start == std::string::npos) {
            out += text.substr(i);
            break;
        }
        size_t close = text.find('}', start + 2);
        if (close == std::string::npos) {
            out += text.substr(i);
            break;
        }
        out += text.substr(i, start - i);
        std::string key = text.substr(start + 2, close - start - 2);
        if (state != NULL) {
            const CN1Json* v = state->get(key.c_str());
            if (v != NULL) {
                if (v->type == CN1Json::JSTR) {
                    out += v->str;
                } else if (v->type == CN1Json::JNUM) {
                    char buf[48];
                    if (v->number == (long long) v->number) {
                        _snprintf(buf, sizeof(buf), "%lld", (long long) v->number);
                    } else {
                        _snprintf(buf, sizeof(buf), "%g", v->number);
                    }
                    out += buf;
                } else if (v->type == CN1Json::JBOOL) {
                    out += v->boolean ? "true" : "false";
                }
            }
        }
        i = close + 1;
    }
    return out;
}

/* Static rendering of dynamic text (the SurfaceRasterizer formatting rules);
 * the provider timer re-pushes the card once a minute while widgets are
 * active, so timers advance in minute steps on the board. */
static std::string cn1BoardFormatDynamic(const std::string& style, long long dateMillis,
        long long nowMillis) {
    char buf[64];
    if (style == "time" || style == "date") {
        time_t t = (time_t) (dateMillis / 1000);
        struct tm tmv;
        localtime_s(&tmv, &t);
        if (style == "time") {
            int hour = tmv.tm_hour % 12;
            if (hour == 0) hour = 12;
            _snprintf(buf, sizeof(buf), "%d:%02d %s", hour, tmv.tm_min,
                    tmv.tm_hour < 12 ? "AM" : "PM");
        } else {
            static const char* months[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            _snprintf(buf, sizeof(buf), "%s %d", months[tmv.tm_mon], tmv.tm_mday);
        }
        return buf;
    }
    if (style == "relative") {
        long long diff = dateMillis - nowMillis;
        bool future = diff >= 0;
        long long minutes = (diff < 0 ? -diff : diff) / 60000;
        if (minutes < 1) return "now";
        if (minutes < 60) {
            _snprintf(buf, sizeof(buf), future ? "in %lld min" : "%lld min ago", minutes);
        } else if (minutes < 24 * 60) {
            _snprintf(buf, sizeof(buf), future ? "in %lld hr" : "%lld hr ago", minutes / 60);
        } else {
            long long days = minutes / (24 * 60);
            _snprintf(buf, sizeof(buf), future ? "in %lld day%s" : "%lld day%s ago", days,
                    days == 1 ? "" : "s");
        }
        return buf;
    }
    /* timerDown (default) / timerUp */
    long long total = (style == "timerUp" ? nowMillis - dateMillis : dateMillis - nowMillis) / 1000;
    if (total < 0) total = 0;
    long long hours = total / 3600;
    long long minutes = (total % 3600) / 60;
    long long seconds = total % 60;
    if (hours > 0) {
        _snprintf(buf, sizeof(buf), "%lld:%02lld:%02lld", hours, minutes, seconds);
    } else {
        _snprintf(buf, sizeof(buf), "%lld:%02lld", minutes, seconds);
    }
    return buf;
}

/* Best-effort TextBlock attributes from the node's size/weight/color. */
static void cn1BoardTextAttributes(const CN1Json& node, std::string& out) {
    double size = node.getNum("size", 0);
    if (size > 0) {
        if (size <= 12) out += ",\"size\":\"Small\"";
        else if (size <= 16) out += "";                     /* Default */
        else if (size <= 20) out += ",\"size\":\"Medium\"";
        else if (size <= 28) out += ",\"size\":\"Large\"";
        else out += ",\"size\":\"ExtraLarge\"";
    }
    std::string weight = node.getStr("fw", "regular");
    if (weight == "semibold" || weight == "bold") {
        out += ",\"weight\":\"Bolder\"";
    } else if (weight == "light") {
        out += ",\"weight\":\"Lighter\"";
    }
    const CN1Json* color = node.get("color");
    if (color != NULL && color->isMap()) {
        std::string role = color->getStr("role", "");
        if (role == "accent") {
            out += ",\"color\":\"Accent\"";
        } else if (role == "secondaryLabel") {
            out += ",\"isSubtle\":true";
        }
        /* explicit {l,d} ARGB pairs are not representable in Adaptive Cards;
         * they render with the theme's Default color */
    }
}

/* Appends the node's action as an AC selectAction (Action.Execute with the
 * action id as the verb); also records the payload so OnActionInvoked can
 * relay it to the app. */
static void cn1BoardAppendAction(const CN1Json& node, std::string& out) {
    const CN1Json* action = node.get("action");
    if (action == NULL || !action->isMap()) {
        return;
    }
    std::string id = action->getStr("id", "");
    if (id.empty()) {
        return;
    }
    out += ",\"selectAction\":{\"type\":\"Action.Execute\",\"verb\":\""
            + cn1BoardJsonEscape(id) + "\"}";
}

static std::string cn1BoardNodeToElement(const CN1Json& node, const CN1Json* state,
        const std::wstring& kindDir, long long nowMillis, int depth);

/* Children of col/box stack vertically inside a Container. */
static std::string cn1BoardChildrenToItems(const CN1Json& node, const CN1Json* state,
        const std::wstring& kindDir, long long nowMillis, int depth) {
    std::string out = "[";
    const CN1Json* ch = node.get("ch");
    bool first = true;
    if (ch != NULL && ch->type == CN1Json::JARR) {
        for (size_t i = 0; i < ch->arr.size(); i++) {
            if (!ch->arr[i].isMap()) continue;
            std::string element = cn1BoardNodeToElement(ch->arr[i], state, kindDir,
                    nowMillis, depth + 1);
            if (element.empty()) continue;
            if (!first) out += ",";
            out += element;
            first = false;
        }
    }
    out += "]";
    return out;
}

static std::string cn1BoardNodeToElement(const CN1Json& node, const CN1Json* state,
        const std::wstring& kindDir, long long nowMillis, int depth) {
    if (depth > 8) {
        return std::string(); /* same nesting cap as the wire format */
    }
    std::string t = node.getStr("t", "box");

    if (t == "text") {
        std::string text = cn1BoardInterpolate(node.getStr("text", ""), state);
        std::string out = "{\"type\":\"TextBlock\",\"wrap\":true,\"text\":\""
                + cn1BoardJsonEscape(text) + "\"";
        cn1BoardTextAttributes(node, out);
        cn1BoardAppendAction(node, out);
        out += "}";
        return out;
    }

    if (t == "dyn") {
        long long date = 0;
        std::string dateKey = node.getStr("dateKey", "");
        if (!dateKey.empty()) {
            const CN1Json* v = state == NULL ? NULL : state->get(dateKey.c_str());
            if (v == NULL || !v->isNum()) return std::string();
            date = (long long) v->number;
        } else {
            const CN1Json* v = node.get("date");
            if (v == NULL || !v->isNum()) return std::string();
            date = (long long) v->number;
        }
        std::string text = cn1BoardFormatDynamic(node.getStr("style", "timerDown"),
                date, nowMillis);
        std::string out = "{\"type\":\"TextBlock\",\"text\":\""
                + cn1BoardJsonEscape(text) + "\"";
        cn1BoardTextAttributes(node, out);
        cn1BoardAppendAction(node, out);
        out += "}";
        return out;
    }

    if (t == "img") {
        std::string name = node.getStr("name", "");
        if (name.empty()) return std::string();
        std::string png;
        if (!cn1BoardReadFile(kindDir + L"\\" + cn1BoardUtf8ToWide(name) + L".png", png)) {
            return std::string();
        }
        std::string out = "{\"type\":\"Image\",\"url\":\"data:image/png;base64,"
                + cn1BoardBase64(png) + "\"";
        if (node.getStr("scale", "fit") == "fill") {
            out += ",\"size\":\"Stretch\"";
        }
        cn1BoardAppendAction(node, out);
        out += "}";
        return out;
    }

    if (t == "prog") {
        /* Pre-1.6 Adaptive Cards have no ProgressBar: emulate a determinate
         * bar with a two-column ColumnSet whose widths are proportional; the
         * filled column is styled "accent". Circular degrades to the same bar. */
        double fraction = 0;
        std::string valueKey = node.getStr("valueKey", "");
        if (!valueKey.empty()) {
            const CN1Json* v = state == NULL ? NULL : state->get(valueKey.c_str());
            if (v != NULL && v->isNum()) fraction = v->number;
        } else if (node.get("start") != NULL && node.get("start")->isNum()
                && node.get("end") != NULL && node.get("end")->isNum()) {
            double start = node.get("start")->number;
            double endv = node.get("end")->number;
            if (endv > start) fraction = (nowMillis - start) / (endv - start);
        } else if (node.get("value") != NULL && node.get("value")->isNum()) {
            fraction = node.get("value")->number;
        }
        if (fraction < 0) fraction = 0;
        if (fraction > 1) fraction = 1;
        int filled = (int) (fraction * 100 + 0.5);
        if (filled < 1) filled = 1;       /* AC rejects zero-weight columns */
        int rest = 100 - filled;
        if (rest < 1) rest = 1;
        char buf[512];
        _snprintf(buf, sizeof(buf),
                "{\"type\":\"ColumnSet\",\"spacing\":\"Small\",\"columns\":["
                "{\"type\":\"Column\",\"width\":%d,\"style\":\"accent\",\"items\":"
                "[{\"type\":\"TextBlock\",\"text\":\" \",\"size\":\"Small\"}]},"
                "{\"type\":\"Column\",\"width\":%d,\"style\":\"emphasis\",\"items\":"
                "[{\"type\":\"TextBlock\",\"text\":\" \",\"size\":\"Small\"}]}]}",
                filled, rest);
        return buf;
    }

    if (t == "spacer") {
        /* meaningful only inside a row (handled there as a stretch column); in
         * a column it degrades to a small vertical gap */
        return "{\"type\":\"TextBlock\",\"text\":\" \",\"spacing\":\"Medium\"}";
    }

    if (t == "row") {
        std::string out = "{\"type\":\"ColumnSet\",\"columns\":[";
        const CN1Json* ch = node.get("ch");
        bool first = true;
        if (ch != NULL && ch->type == CN1Json::JARR) {
            for (size_t i = 0; i < ch->arr.size(); i++) {
                if (!ch->arr[i].isMap()) continue;
                const CN1Json& child = ch->arr[i];
                std::string childType = child.getStr("t", "box");
                bool stretch = child.getNum("weight", 0) > 0 || childType == "spacer";
                std::string inner = childType == "spacer" ? std::string()
                        : cn1BoardNodeToElement(child, state, kindDir, nowMillis, depth + 1);
                if (!first) out += ",";
                out += "{\"type\":\"Column\",\"width\":\"";
                out += stretch ? "stretch" : "auto";
                out += "\",\"items\":[";
                if (!inner.empty()) out += inner;
                out += "]}";
                first = false;
            }
        }
        out += "]";
        cn1BoardAppendAction(node, out);
        out += "}";
        return out;
    }

    /* col / box (and unknown types) -> Container; AC has no overlay stacking,
     * so a box's children stack vertically like a column */
    std::string out = "{\"type\":\"Container\",\"items\":"
            + cn1BoardChildrenToItems(node, state, kindDir, nowMillis, depth);
    const CN1Json* bg = node.get("bg");
    if (bg != NULL && bg->isMap() && bg->getStr("role", "") == "accent") {
        out += ",\"style\":\"accent\"";
    }
    cn1BoardAppendAction(node, out);
    out += "}";
    return out;
}

/* Maps a widget-board size name to the wire-format layout size name. */
static std::string cn1BoardLayoutSizeName(const std::string& boardSize) {
    if (boardSize == "medium") return "medium";
    if (boardSize == "large") return "large";
    return "small";
}

/*
 * Builds the full Adaptive Card for a kind + board size from the persisted
 * timeline: picks the per-size (or default) layout, resolves the active entry
 * (latest date <= now) and maps the tree. Returns an empty string when there
 * is no published content yet (the board then keeps the default loading card).
 */
static std::string cn1BoardBuildCard(const std::string& kindId, const std::string& boardSize) {
    std::wstring root = cn1BoardSurfacesRoot();
    if (root.empty()) {
        return std::string();
    }
    std::wstring kindDir = root + L"\\" + cn1BoardUtf8ToWide(kindId);
    std::string timelineText;
    if (!cn1BoardReadFile(kindDir + L"\\timeline.json", timelineText)) {
        return std::string();
    }
    CN1Json doc;
    if (!cn1JsonParse(timelineText, doc) || !doc.isMap()) {
        return std::string();
    }
    /* layout for size, falling back to "default" */
    const CN1Json* layouts = doc.get("layouts");
    if (layouts == NULL || !layouts->isMap()) {
        return std::string();
    }
    const CN1Json* layout = layouts->get(cn1BoardLayoutSizeName(boardSize).c_str());
    if (layout == NULL || !layout->isMap()) {
        layout = layouts->get("default");
    }
    if (layout == NULL || !layout->isMap()) {
        return std::string();
    }
    /* active entry: the entry whose date most recently passed */
    long long nowMillis = (long long) time(NULL) * 1000LL;
    const CN1Json* state = NULL;
    const CN1Json* entries = doc.get("entries");
    if (entries != NULL && entries->type == CN1Json::JARR) {
        const CN1Json* current = NULL;
        for (size_t i = 0; i < entries->arr.size(); i++) {
            const CN1Json& e = entries->arr[i];
            if (e.isMap() && e.getNum("date", 0) <= (double) nowMillis) {
                current = &e;
            }
        }
        if (current == NULL && !entries->arr.empty() && entries->arr[0].isMap()) {
            current = &entries->arr[0];
        }
        if (current != NULL) {
            state = current->get("state");
        }
    }
    std::string body = cn1BoardNodeToElement(*layout, state, kindDir, nowMillis, 0);
    if (body.empty()) {
        return std::string();
    }
    return "{\"type\":\"AdaptiveCard\",\"$schema\":\"http://adaptivecards.io/schemas/"
            "adaptive-card.json\",\"version\":\"1.5\",\"body\":[" + body + "]}";
}

/* ==================================================== the widget provider */

/* Tracked live widget instances (widgetId -> kind/size), so the minute timer
 * can re-push time-driven content and Deactivate can stop when none remain. */
struct CN1BoardWidget {
    std::string kindId;
    std::string size;
};

static CRITICAL_SECTION g_boardLock;
static std::map<std::wstring, CN1BoardWidget> g_boardWidgets;
static HANDLE g_boardShutdownEvent = NULL;
static HANDLE g_boardTimer = NULL;

static void cn1BoardPushWidget(const winrt::hstring& widgetId, const CN1BoardWidget& info) {
    std::string card = cn1BoardBuildCard(info.kindId, info.size);
    if (card.empty()) {
        return;
    }
    WidgetUpdateRequestOptions options(widgetId);
    options.Template(winrt::hstring(cn1BoardUtf8ToWide(card)));
    options.Data(L"{}");
    WidgetManager::GetDefault().UpdateWidget(options);
}

static void cn1BoardPushAll() {
    std::map<std::wstring, CN1BoardWidget> snapshot;
    EnterCriticalSection(&g_boardLock);
    snapshot = g_boardWidgets;
    LeaveCriticalSection(&g_boardLock);
    for (std::map<std::wstring, CN1BoardWidget>::const_iterator it = snapshot.begin();
            it != snapshot.end(); ++it) {
        cn1BoardPushWidget(winrt::hstring(it->first), it->second);
    }
}

/* Minute cadence: Adaptive Cards cannot tick a countdown natively, so while
 * any widget is active the provider re-templates + re-pushes every card once
 * a minute (a deliberate budget-respecting floor; the in-app floating windows
 * tick at full second precision instead). */
static VOID CALLBACK cn1BoardTimerProc(PVOID, BOOLEAN) {
    cn1BoardPushAll();
}

static std::string cn1BoardWideToUtf8(const std::wstring& w) {
    if (w.empty()) return std::string();
    int n = WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int) w.size(), NULL, 0, NULL, NULL);
    std::string s((size_t) n, '\0');
    WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int) w.size(), &s[0], n, NULL, NULL);
    return s;
}

/* The board's WidgetSize enum -> the wire-format layout size name. */
static std::string cn1BoardSizeName(WidgetSize size) {
    switch (size) {
        case WidgetSize::Small: return "small";
        case WidgetSize::Large: return "large";
        default: return "medium";
    }
}

struct CN1WidgetProvider : winrt::implements<CN1WidgetProvider, IWidgetProvider> {
    void CreateWidget(WidgetContext const& context) {
        CN1BoardWidget info;
        info.kindId = cn1BoardWideToUtf8(std::wstring(context.DefinitionId()));
        info.size = cn1BoardSizeName(context.Size());
        EnterCriticalSection(&g_boardLock);
        g_boardWidgets[std::wstring(context.Id())] = info;
        LeaveCriticalSection(&g_boardLock);
        cn1BoardPushWidget(context.Id(), info);
    }

    void DeleteWidget(winrt::hstring const& widgetId, winrt::hstring const&) {
        bool empty;
        EnterCriticalSection(&g_boardLock);
        g_boardWidgets.erase(std::wstring(widgetId));
        empty = g_boardWidgets.empty();
        LeaveCriticalSection(&g_boardLock);
        if (empty && g_boardShutdownEvent != NULL) {
            SetEvent(g_boardShutdownEvent);
        }
    }

    void OnActionInvoked(WidgetActionInvokedArgs const& args) {
        /* Relay the click to the app: append it to the pending-actions file
         * (drained by WindowsWidgetBridge into Surfaces.dispatchAction's
         * cold-start queue) and launch the app exe. */
        std::string kindId;
        EnterCriticalSection(&g_boardLock);
        std::map<std::wstring, CN1BoardWidget>::const_iterator it =
                g_boardWidgets.find(std::wstring(args.WidgetContext().Id()));
        if (it != g_boardWidgets.end()) {
            kindId = it->second.kindId;
        }
        LeaveCriticalSection(&g_boardLock);
        std::string verb = cn1BoardWideToUtf8(std::wstring(args.Verb()));
        if (kindId.empty() || verb.empty()) {
            return;
        }
        std::wstring path = cn1BoardSurfacesRoot() + L"\\pending_actions.txt";
        HANDLE f = CreateFileW(path.c_str(), FILE_APPEND_DATA, FILE_SHARE_READ, NULL,
                OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
        if (f != INVALID_HANDLE_VALUE) {
            std::string line = kindId + "\t" + verb + "\n";
            DWORD written = 0;
            WriteFile(f, line.c_str(), (DWORD) line.size(), &written, NULL);
            CloseHandle(f);
        }
        WCHAR exe[MAX_PATH];
        if (GetModuleFileNameW(NULL, exe, MAX_PATH) > 0) {
            ShellExecuteW(NULL, L"open", exe, NULL, NULL, SW_SHOWNORMAL);
        }
    }

    void OnWidgetContextChanged(WidgetContextChangedArgs const& args) {
        /* size / pin-context change: track the new size and re-push */
        WidgetContext ctx = args.WidgetContext();
        CN1BoardWidget info;
        bool found = false;
        EnterCriticalSection(&g_boardLock);
        std::map<std::wstring, CN1BoardWidget>::iterator it =
                g_boardWidgets.find(std::wstring(ctx.Id()));
        if (it != g_boardWidgets.end()) {
            it->second.size = cn1BoardSizeName(ctx.Size());
            found = true;
            info = it->second;
        }
        LeaveCriticalSection(&g_boardLock);
        if (found) {
            cn1BoardPushWidget(ctx.Id(), info);
        }
    }

    void Activate(WidgetContext const& context) {
        /* the widget became visible: refresh immediately */
        CN1BoardWidget info;
        bool found = false;
        EnterCriticalSection(&g_boardLock);
        std::map<std::wstring, CN1BoardWidget>::iterator it =
                g_boardWidgets.find(std::wstring(context.Id()));
        if (it != g_boardWidgets.end()) {
            found = true;
            info = it->second;
        }
        LeaveCriticalSection(&g_boardLock);
        if (found) {
            cn1BoardPushWidget(context.Id(), info);
        }
    }

    void Deactivate(winrt::hstring const&) {
        /* the widget is off-screen; the minute timer keeps state fresh enough */
    }
};

struct CN1WidgetProviderFactory : winrt::implements<CN1WidgetProviderFactory, IClassFactory> {
    HRESULT __stdcall CreateInstance(IUnknown* outer, GUID const& iid, void** result) noexcept {
        if (result == NULL) {
            return E_POINTER;
        }
        *result = NULL;
        if (outer != NULL) {
            return CLASS_E_NOAGGREGATION;
        }
        return winrt::make<CN1WidgetProvider>().as(iid, result);
    }

    HRESULT __stdcall LockServer(BOOL) noexcept {
        return S_OK;
    }
};

/* ============================================================ entry point */

extern "C" {

/*
 * Called from initDisplay (the first native of every launch, before any
 * window exists). When the process was activated by COM with
 * -RegisterProcessAsComServer this bootstraps the WindowsAppRuntime, registers
 * the widget provider class factory and serves the Widgets Board until the
 * last widget is deleted (or the board disconnects), then exits the process --
 * the Codename One app UI never starts in this mode. In a normal launch the
 * flag is absent and this returns immediately.
 */
void cn1WidgetBoardMaybeRunComServer(void) {
    const WCHAR* cmd = GetCommandLineW();
    if (cmd == NULL || wcsstr(cmd, L"-RegisterProcessAsComServer") == NULL) {
        return;
    }

    InitializeCriticalSection(&g_boardLock);
    g_boardShutdownEvent = CreateEventW(NULL, TRUE, FALSE, NULL);

    winrt::init_apartment();

    /* Bind the installed WindowsAppRuntime (the exe carries no framework
     * package dependency of its own). PackageVersion{0} accepts any servicing
     * revision of the 1.5 series. Requires the WindowsAppRuntime
     * redistributable on the machine -- see the MSIX packaging notes in
     * WindowsNativeBuilder. */
    PACKAGE_VERSION minVersion{};
    HRESULT hr = MddBootstrapInitialize(CN1_WINAPPSDK_MAJORMINOR, L"", minVersion);
    if (FAILED(hr)) {
        cn1WindowsLog("widgetboard: MddBootstrapInitialize failed (is the "
                "WindowsAppRuntime installed?)");
        ExitProcess(1);
    }

    DWORD cookie = 0;
    auto factory = winrt::make<CN1WidgetProviderFactory>();
    hr = CoRegisterClassObject(CN1_WIDGET_PROVIDER_CLSID, factory.get(),
            CLSCTX_LOCAL_SERVER, REGCLS_MULTIPLEUSE, &cookie);
    if (FAILED(hr)) {
        cn1WindowsLog("widgetboard: CoRegisterClassObject failed");
        MddBootstrapShutdown();
        ExitProcess(1);
    }

    /* Recover the already-pinned widget set (this process restarts on demand). */
    try {
        auto infos = WidgetManager::GetDefault().GetWidgetInfos();
        for (auto const& winfo : infos) {
            WidgetContext ctx = winfo.WidgetContext();
            CN1BoardWidget info;
            info.kindId = cn1BoardWideToUtf8(std::wstring(ctx.DefinitionId()));
            info.size = cn1BoardSizeName(ctx.Size());
            EnterCriticalSection(&g_boardLock);
            g_boardWidgets[std::wstring(ctx.Id())] = info;
            LeaveCriticalSection(&g_boardLock);
        }
    } catch (...) {
        /* no widgets yet -- CreateWidget will populate the map */
    }
    cn1BoardPushAll();

    /* Minute re-push for dynamic text / interval progress (see the timer
     * comment above), on the default threadpool. */
    CreateTimerQueueTimer(&g_boardTimer, NULL, cn1BoardTimerProc, NULL,
            60000, 60000, WT_EXECUTEDEFAULT);

    /* Serve until the last widget is deleted; COM calls arrive on RPC threads,
     * so a plain wait suffices (no message pump needed for a non-STA server). */
    WaitForSingleObject(g_boardShutdownEvent, INFINITE);

    if (g_boardTimer != NULL) {
        DeleteTimerQueueTimer(NULL, g_boardTimer, NULL);
    }
    CoRevokeClassObject(cookie);
    MddBootstrapShutdown();
    ExitProcess(0);
}

} /* extern "C" */

#endif /* CN1_WIDGETBOARD */
