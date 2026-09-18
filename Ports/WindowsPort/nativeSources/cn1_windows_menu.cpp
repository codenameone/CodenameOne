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
 * The native menu bar for the Windows port.
 *
 * Codename One commands reach here as one encoded row each, exactly the format
 * IOSImplementation.setNativeCommands writes for the macOS menu:
 *
 *   "<menuHint>\t<label>\t<shortcutKeyChar>\t<shortcutModifiers>\t<commandId>"
 *
 * rows separated by '\n'. The three ParparVM desktop ports share that shape on purpose --
 * a second encoding is a second thing to keep in step with Command's placement constants.
 *
 * WHY A MENU BAR AND NOT A RIBBON: this is what SetMenu gives, it is what Win32 draws for
 * free in the window's non-client area, and it is what desktopTitleBarMode=native means on
 * this platform. The Fluent design language's own command surfaces are application-drawn,
 * which is a different feature.
 *
 * The item ids handed to Win32 are NOT the Codename One command ids. Win32 menu ids are
 * 16-bit and share a space with accelerator and control notifications, so the ids here are
 * allocated from a private base and mapped back; a Codename One id is a 32-bit counter that
 * would collide with WM_COMMAND's control notifications the moment it passed 0xFFFF.
 *
 * Threading: SetMenu must run on the thread that owns the window, so the whole rebuild is
 * marshalled through WM_CN1_MENU with a blocking SendMessageW, the pattern the native edit
 * control, the file dialog and the desktop windows already use. The selection goes back the
 * other way through the ordinary event queue, so the command runs on the EDT.
 */

#include "cn1_windows.h"
#include <stdlib.h>
#include <string.h>

/* Win32 menu item ids start here. Comfortably above the control notification ids a dialog
 * would use (IDOK is 1, IDCANCEL 2, ...) and far below 0xF000, which the system reserves
 * for its own window-menu commands (SC_CLOSE and friends arrive as WM_SYSCOMMAND, but the
 * range is reserved regardless). */
#define CN1_MENU_ID_BASE 0x2000

/* The most items a menu bar will carry. A bar deeper than this is not a menu bar any
 * user can operate, and a fixed table means the WM_COMMAND lookup needs no allocation on
 * the pump thread. */
#define CN1_MENU_MAX_ITEMS 512

typedef struct {
    int commandId;    /* the Codename One id, echoed back in the event */
} CN1MenuItem;

static CN1MenuItem menuItems[CN1_MENU_MAX_ITEMS];
static int menuItemCount = 0;
static HMENU menuBar = NULL;

/* The standard top-level menus, in the order Windows applications put them. The hint
 * strings are Command.DESKTOP_MENU_* verbatim; anything else becomes a top-level menu
 * titled with the hint itself, and an empty hint lands in "Commands".
 *
 * The application menu has no equivalent here: Windows has no app menu, so About,
 * Preferences and Quit go where a Windows user looks for them -- About under Help, the
 * other two under File. That is a placement decision, not a mapping gap, and it is the
 * reason this table is not simply the macOS one. */
typedef struct {
    const char* hint;
    const char* title;
} CN1MenuHint;

static const CN1MenuHint MENU_HINTS[] = {
    {"File",        "File"},
    {"Edit",        "Edit"},
    {"View",        "View"},
    {"Window",      "Window"},
    {"Help",        "Help"},
    {"About",       "Help"},
    {"Preferences", "File"},
    {"Quit",        "File"},
    {"App",         "File"},
    {"",            "Commands"},
};
#define MENU_HINT_COUNT ((int) (sizeof(MENU_HINTS) / sizeof(MENU_HINTS[0])))

static const char* titleForHint(const char* hint) {
    for (int i = 0; i < MENU_HINT_COUNT; i++) {
        if (strcmp(MENU_HINTS[i].hint, hint) == 0) {
            return MENU_HINTS[i].title;
        }
    }
    /* An unrecognised hint is a top-level menu with that literal title, which is what
     * Command.setDesktopMenu documents. */
    return hint[0] == '\0' ? "Commands" : hint;
}

static WCHAR* widen(const char* utf8) {
    int len = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
    if (len <= 0) {
        return NULL;
    }
    WCHAR* wide = (WCHAR*) malloc((size_t) len * sizeof(WCHAR));
    if (wide != NULL) {
        MultiByteToWideChar(CP_UTF8, 0, utf8, -1, wide, len);
    }
    return wide;
}

/* Appends the accelerator to a label the way Win32 shows one: a tab, then the modifier
 * names and the key. Win32 draws nothing by itself -- unlike AppKit, the text after the
 * tab IS the accelerator display -- so a command with a shortcut that was not spelled out
 * here would show the shortcut nowhere while still responding to it. */
static void appendAccelerator(char* label, size_t cap, int keyChar, int modifiers) {
    if (keyChar == 0) {
        return;
    }
    size_t len = strlen(label);
    if (len + 16 >= cap) {
        return;
    }
    label[len++] = '\t';
    /* Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY is Control on Windows, which is the whole
     * point of the constant: the same application code produces Command on a Mac. */
    if (modifiers & 1) {
        memcpy(label + len, "Ctrl+", 5);
        len += 5;
    }
    if (modifiers & 2) {
        memcpy(label + len, "Shift+", 6);
        len += 6;
    }
    if (modifiers & 4) {
        memcpy(label + len, "Alt+", 4);
        len += 4;
    }
    label[len++] = (char) keyChar;
    label[len] = '\0';
}

/* Finds or creates the popup for a top-level title, so two commands with the same hint
 * land in one menu rather than two menus with the same name. */
static HMENU popupNamed(HMENU bar, const char* title, HMENU* popups, char titles[][64],
                        int* popupCount) {
    for (int i = 0; i < *popupCount; i++) {
        if (strcmp(titles[i], title) == 0) {
            return popups[i];
        }
    }
    if (*popupCount >= 32) {
        return NULL;
    }
    HMENU popup = CreatePopupMenu();
    if (popup == NULL) {
        return NULL;
    }
    WCHAR* wide = widen(title);
    if (wide != NULL) {
        AppendMenuW(bar, MF_POPUP, (UINT_PTR) popup, wide);
        free(wide);
    }
    popups[*popupCount] = popup;
    strncpy(titles[*popupCount], title, 63);
    titles[*popupCount][63] = '\0';
    (*popupCount)++;
    return popup;
}

/* Reads one tab-delimited field into out, advancing *cursor past the delimiter. Returns 0
 * at the end of the row. */
static int nextField(const char** cursor, char* out, size_t cap) {
    const char* p = *cursor;
    size_t n = 0;
    while (*p != '\0' && *p != '\t' && *p != '\n') {
        if (n + 1 < cap) {
            out[n++] = *p;
        }
        p++;
    }
    out[n] = '\0';
    if (*p == '\t') {
        p++;
        *cursor = p;
        return 1;
    }
    *cursor = p;
    return 0;
}

/* Builds the bar. Runs on the window's own thread; see the file header. */
static void rebuildMenu(const char* spec) {
    HMENU previous = menuBar;
    menuItemCount = 0;
    menuBar = NULL;

    if (spec != NULL && spec[0] != '\0') {
        HMENU bar = CreateMenu();
        if (bar != NULL) {
            HMENU popups[32];
            char titles[32][64];
            int popupCount = 0;
            const char* cursor = spec;
            while (*cursor != '\0' && menuItemCount < CN1_MENU_MAX_ITEMS) {
                char hint[64];
                char label[256];
                char keyField[16];
                char modField[16];
                char idField[24];
                nextField(&cursor, hint, sizeof(hint));
                nextField(&cursor, label, sizeof(label));
                nextField(&cursor, keyField, sizeof(keyField));
                nextField(&cursor, modField, sizeof(modField));
                nextField(&cursor, idField, sizeof(idField));
                if (*cursor == '\n') {
                    cursor++;
                }
                if (label[0] == '\0') {
                    continue;
                }
                appendAccelerator(label, sizeof(label), atoi(keyField), atoi(modField));
                HMENU popup = popupNamed(bar, titleForHint(hint), popups, titles, &popupCount);
                if (popup == NULL) {
                    continue;
                }
                WCHAR* wide = widen(label);
                if (wide == NULL) {
                    continue;
                }
                int slot = menuItemCount++;
                menuItems[slot].commandId = atoi(idField);
                AppendMenuW(popup, MF_STRING, (UINT_PTR) (CN1_MENU_ID_BASE + slot), wide);
                free(wide);
            }
            if (popupCount == 0) {
                /* Every row was unusable. An empty bar is a one-pixel strip the user cannot
                 * explain, so it is not installed at all. */
                DestroyMenu(bar);
            } else {
                menuBar = bar;
            }
        }
    }

    SetMenu(cn1Win.hwnd, menuBar);
    DrawMenuBar(cn1Win.hwnd);
    if (previous != NULL) {
        /* After SetMenu, never before: destroying the menu the window is still showing
         * leaves it drawing freed memory until the next paint. */
        DestroyMenu(previous);
    }
}

extern "C" {

void cn1WinMenuSetCommands(const char* spec) {
    if (cn1Win.hwnd == NULL) {
        return;
    }
    rebuildMenu(spec);
}

int cn1WinMenuHandleCommand(WPARAM wParam) {
    /* A menu selection has a zero high word; a control notification does not. Checked
     * before the id range so a notification whose control id happens to fall in the range
     * is not mistaken for a menu item. */
    if (HIWORD(wParam) != 0) {
        return 0;
    }
    int id = (int) LOWORD(wParam) - CN1_MENU_ID_BASE;
    if (id < 0 || id >= menuItemCount) {
        return 0;
    }
    cn1WinPushEvent(CN1_EVENT_MENU_COMMAND, 0, 0, menuItems[id].commandId);
    return 1;
}

/* The native the Java side calls.
 *
 * SENT, not posted, and that is load bearing twice over. The menu is in place before the
 * call returns, so a form shown immediately afterwards cannot race the rebuild -- and, more
 * sharply, stringToUTF8 hands back THIS THREAD'S scratch buffer, which the next conversion
 * on this thread overwrites. A posted message would be read after the buffer had moved on.
 * A blocking send cannot be: nothing else runs on this thread until it returns. */
JAVA_VOID com_codename1_impl_windows_WindowsNative_menuSetCommands___java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT spec) {
    if (cn1Win.hwnd == NULL) {
        return;
    }
    const char* utf8 = spec == JAVA_NULL ? "" : stringToUTF8(threadStateData, spec);
    SendMessageW(cn1Win.hwnd, WM_CN1_MENU, 0, (LPARAM) utf8);
}

}
