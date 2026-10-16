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
/* Local GTK input-method integration. Java ABI hooks are stubbed; GTK is real. */
#include <gtk/gtk.h>
#include <assert.h>
#include <stdint.h>
#define CN1_LINUX_GFX_H
#define CN1_MAIN_WINDOW_SLOT (-1)
#define CODENAME_ONE_THREAD_STATE void* threadStateData
typedef void* JAVA_OBJECT;
typedef int JAVA_INT;
typedef int JAVA_BOOLEAN;
#define JAVA_VOID void
#define JAVA_NULL NULL
#define JAVA_TRUE 1
#define JAVA_FALSE 0
static GtkWidget *testWindow, *testCanvas;
GtkWidget* cn1LinuxWindowWidget(void) { return testWindow; }
GtkWidget* cn1LinuxDesktopWidget(int slot) { return slot == 4 ? testWindow : NULL; }
GtkWidget* cn1LinuxTextCanvas(int slot) { return slot == -1 || slot == 4 ? testCanvas : NULL; }
void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* argument) { fn(argument); }
const char* stringToUTF8(void* state, JAVA_OBJECT text) { return text; }
JAVA_OBJECT newStringFromUtf8Len(void* state, const char* text, int length) { return g_strndup(text, length); }
#include "../../../../../Ports/LinuxPort/nativeSources/cn1_linux_text_input.c"
#define START com_codename1_impl_linux_LinuxTextInput_startNative___int_int_int_boolean_boolean_R_boolean
#define UPDATE com_codename1_impl_linux_LinuxTextInput_updateNative___int_int_java_lang_String_int_int_int_int_int
#define STOP com_codename1_impl_linux_LinuxTextInput_stopNative___int
#define POLL com_codename1_impl_linux_LinuxTextInput_pollNative___R_java_lang_String
static void clearEvents(void) { char* event; while ((event = POLL(NULL))) { g_free(event); } }
static void key(int slot, guint value, GdkModifierType state, GdkEventType type) {
    GdkEventKey event;
    memset(&event, 0, sizeof(event));
    event.type = type;
    event.window = gtk_widget_get_window(testCanvas);
    event.keyval = value;
    event.state = state;
    event.send_event = TRUE;
    cn1LinuxTextInputFilter(slot, &event);
}
static char* expectKind(int kind, const char* suffix) {
    char* event = POLL(NULL);
    int id, seq, actual, value, modifiers, consumed = 0;
    assert(event);
    assert(sscanf(event, "%d;%d;%d;%d;%d;%n", &id, &seq, &actual, &value, &modifiers, &consumed) == 5);
    assert(actual == kind);
    if (suffix) { assert(strcmp(event + consumed, suffix) == 0); }
    return event;
}
int main(int argc, char** argv) {
    char* event;
    GdkEventFocus focus;
    gtk_init(&argc, &argv);
    testWindow = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    testCanvas = gtk_drawing_area_new();
    gtk_container_add(GTK_CONTAINER(testWindow), testCanvas);
    /* Focus starts before Window.show() maps its peer. */
    assert(!gtk_widget_get_realized(testCanvas));
    assert(START(NULL, 1, -1, 0, 1, 1));
    assert(gtk_widget_get_realized(testCanvas));
    assert(!gtk_widget_get_visible(testWindow));
    STOP(NULL, 1);
    clearEvents();
    gtk_widget_show_all(testWindow);
    while (gtk_events_pending()) { gtk_main_iteration(); }
    assert(START(NULL, 1, -1, 0, 1, 1));
    UPDATE(NULL, 1, 0, "A😀B", 3, 10, 20, 2, 18);
    assert(cn1TextInput.caretBytes == 5);
    assert(cn1TextSurrounding(cn1TextInput.im, NULL));
    key(-1, GDK_KEY_a, 0, GDK_KEY_PRESS);
    event = expectKind(0, "a"); g_free(event);
    assert(!cn1TextSurrounding(cn1TextInput.im, NULL));
    UPDATE(NULL, 1, 0, "stale", 0, 0, 0, 1, 1);
    assert(!cn1TextInput.surroundingValid);
    UPDATE(NULL, 1, cn1TextInput.sequence, "A😀aB", 4, 10, 20, 2, 18);
    assert(cn1TextInput.surroundingValid && cn1TextInput.caretBytes == 6);
    assert(!cn1TextDelete(cn1TextInput.im, 1, 1, NULL));
    assert(cn1TextDelete(cn1TextInput.im, -1, 1, NULL));
    event = expectKind(5, ""); assert(strstr(event, ";5;-1;1;")); g_free(event);
    key(-1, GDK_KEY_z, GDK_CONTROL_MASK | GDK_SHIFT_MASK, GDK_KEY_PRESS);
    event = expectKind(3, ""); assert(strstr(event, ";3;17;3;")); g_free(event);
    key(-1, GDK_KEY_Left, GDK_SHIFT_MASK, GDK_KEY_PRESS);
    event = expectKind(3, ""); assert(strstr(event, ";3;1;1;")); g_free(event);
    key(-1, GDK_KEY_Tab, 0, GDK_KEY_PRESS);
    event = expectKind(3, ""); assert(strstr(event, ";3;18;0;")); g_free(event);
    key(-1, GDK_KEY_Return, 0, GDK_KEY_PRESS);
    event = expectKind(4, ""); g_free(event);
    /* The real GtkIMContextSimple dead-key path must yield a composed commit. */
    key(-1, GDK_KEY_dead_acute, 0, GDK_KEY_PRESS);
    key(-1, GDK_KEY_e, 0, GDK_KEY_PRESS);
    int composed = 0, preedit = 0;
    while ((event = POLL(NULL))) {
        int id, seq, kind, value, mods, consumed = 0;
        assert(sscanf(event, "%d;%d;%d;%d;%d;%n", &id, &seq, &kind, &value, &mods, &consumed) == 5);
        if (kind == 1) { preedit++; }
        if (kind == 0 && strcmp(event + consumed, "é") == 0) { composed++; }
        g_free(event);
    }
    assert(composed == 1 && preedit > 0);
    assert(START(NULL, 2, 4, 0x10000 | 0x80000, 1, 1));
    GtkInputHints hints;
    GtkInputPurpose purpose;
    g_object_get(cn1TextInput.im, "input-hints", &hints, "input-purpose", &purpose, NULL);
    assert((hints & GTK_INPUT_HINT_NO_SPELLCHECK) && purpose == GTK_INPUT_PURPOSE_PASSWORD);
    UPDATE(NULL, 2, 0, "secret", 6, 0, 0, 1, 1);
    assert(!cn1TextSurrounding(cn1TextInput.im, NULL));
    STOP(NULL, 1); assert(cn1TextInput.session == 2);
    key(-1, GDK_KEY_x, 0, GDK_KEY_PRESS); assert(!POLL(NULL));
    key(4, GDK_KEY_x, 0, GDK_KEY_PRESS); event = expectKind(0, "x"); g_free(event);
    memset(&focus, 0, sizeof(focus)); focus.in = FALSE;
    cn1TextFocus(testWindow, &focus, NULL);
    event = expectKind(7, ""); g_free(event);
    key(4, GDK_KEY_x, 0, GDK_KEY_PRESS); assert(!POLL(NULL));
    focus.in = TRUE;
    cn1TextFocus(testWindow, &focus, NULL);
    event = expectKind(8, ""); g_free(event);
    key(4, GDK_KEY_y, 0, GDK_KEY_PRESS); event = expectKind(0, "y"); g_free(event);
    for (int i = 3; i < 103; i++) {
        assert(START(NULL, i, -1, 0, 1, 1));
        STOP(NULL, i); STOP(NULL, i);
        assert(!cn1TextInput.im && !cn1TextInput.session);
    }
    clearEvents(); gtk_widget_destroy(testWindow);
    puts("PASS: real GTK commit/dead-key composition, UTF-16 caret, stale-state rejection, delete-surrounding, navigation/shortcuts, owner-window routing, privacy hints, focus loss, 100 session replacements/stops");
    return 0;
}
