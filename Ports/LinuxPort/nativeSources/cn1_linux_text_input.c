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
/* GTK owns the IM context. Java owns the document; queued edits carry session and
 * sequence numbers, so neither a late callback nor a stale state push can edit
 * a successor. Surrounding context is withheld until Java acknowledges all
 * queued edits instead of handing an IME stale text. */
#include "cn1_linux_gfx.h"
#include <limits.h>
#include <string.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT text);
extern JAVA_OBJECT newStringFromUtf8Len(CODENAME_ONE_THREAD_STATE, const char* text, int length);

typedef struct {
    int session, sequence, slot, focused, surroundingValid, caretBytes, sensitive;
    GtkIMContext* im;
    GtkWidget *window, *canvas;
    char* surrounding;
} CN1TextInput;
static CN1TextInput cn1TextInput;
static GQueue cn1TextEvents = G_QUEUE_INIT;
static GMutex cn1TextEventsMutex;

static void cn1TextQueue(int kind, int value, int modifiers, const char* text) {
    CN1TextInput* input = &cn1TextInput;
    char* event;
    if (!input->session || input->sequence == INT_MAX) {
        return;
    }
    input->surroundingValid = 0;
    event = g_strdup_printf("%d;%d;%d;%d;%d;%s", input->session, ++input->sequence,
            kind, value, modifiers, text ? text : "");
    g_mutex_lock(&cn1TextEventsMutex);
    g_queue_push_tail(&cn1TextEvents, event);
    g_mutex_unlock(&cn1TextEventsMutex);
}

static void cn1TextCommit(GtkIMContext* im, const gchar* text, gpointer unused) {
    (void) im; (void) unused;
    cn1TextQueue(0, 0, 0, text);
}

static void cn1TextPreedit(GtkIMContext* im, gpointer unused) {
    gchar* text = NULL;
    PangoAttrList* attributes = NULL;
    gint cursor = 0;
    (void) unused;
    gtk_im_context_get_preedit_string(im, &text, &attributes, &cursor);
    cn1TextQueue(1, cursor, 0, text);
    g_free(text);
    if (attributes) {
        pango_attr_list_unref(attributes);
    }
}

static void cn1TextPreeditEnd(GtkIMContext* im, gpointer unused) {
    (void) im; (void) unused;
    cn1TextQueue(2, 0, 0, NULL);
}

static gboolean cn1TextSurrounding(GtkIMContext* im, gpointer unused) {
    CN1TextInput* input = &cn1TextInput;
    (void) unused;
    if (input->sensitive || !input->surroundingValid || !input->surrounding) {
        return FALSE;
    }
    gtk_im_context_set_surrounding(im, input->surrounding, -1, input->caretBytes);
    return TRUE;
}

static gboolean cn1TextDelete(GtkIMContext* im, gint offset, gint count, gpointer unused) {
    (void) im; (void) unused;
    /* TextInputClient can delete immediately before/after the caret, not an
     * unrelated range in a windowed surrounding snapshot. Decline that request. */
    if (!cn1TextInput.surroundingValid || offset > 0 || count < 0
            || (long long) offset + count < 0) {
        return FALSE;
    }
    cn1TextQueue(5, offset, count, NULL);
    return TRUE;
}

static gboolean cn1TextFocus(GtkWidget* widget, GdkEventFocus* event, gpointer unused) {
    (void) widget; (void) unused;
    if (!cn1TextInput.im) {
        return FALSE;
    }
    /* A deactivated window retains its focused component. Only component
     * focus loss or destruction ends the Java input session. */
    if (event->in) {
        cn1TextInput.focused = 1;
        gtk_im_context_focus_in(cn1TextInput.im);
    } else {
        cn1TextInput.focused = 0;
        gtk_im_context_focus_out(cn1TextInput.im);
    }
    return FALSE;
}

static void cn1TextDestroyed(GtkWidget* widget, gpointer unused) {
    (void) widget; (void) unused;
    cn1TextInput.focused = 0;
    cn1TextQueue(6, 0, 0, NULL);
}

static void cn1TextClear(void) {
    CN1TextInput* input = &cn1TextInput;
    input->session = 0; /* Reset may emit callbacks: the old session is already gone. */
    if (input->im) {
        g_signal_handlers_disconnect_by_data(input->im, input);
        gtk_im_context_reset(input->im);
        gtk_im_context_focus_out(input->im);
        gtk_im_context_set_client_window(input->im, NULL);
        g_object_unref(input->im);
    }
    if (input->window) {
        g_signal_handlers_disconnect_by_data(input->window, input);
        g_object_unref(input->window);
    }
    if (input->canvas) {
        g_object_unref(input->canvas);
    }
    g_free(input->surrounding);
    memset(input, 0, sizeof(*input));
}

typedef struct { int session, slot, constraint, correct, capitalize, result; } CN1TextStart;
static void cn1TextStartOnMain(void* data) {
    CN1TextStart* request = data;
    CN1TextInput* input = &cn1TextInput;
    GtkWidget* window = request->slot == CN1_MAIN_WINDOW_SLOT
            ? cn1LinuxWindowWidget() : cn1LinuxDesktopWidget(request->slot);
    GtkWidget* canvas = cn1LinuxTextCanvas(request->slot);
    GtkInputPurpose purpose = GTK_INPUT_PURPOSE_FREE_FORM;
    GtkInputHints hints = request->correct ? GTK_INPUT_HINT_SPELLCHECK : GTK_INPUT_HINT_NO_SPELLCHECK;
    cn1TextClear();
    if (!window || !canvas) {
        return;
    }
    /* Window.show() establishes component focus before mapping the GTK window.
     * Realize the anchored canvas now so the initially focused editor receives
     * an input context even before the first map. This does not show the window. */
    gtk_widget_realize(canvas);
    if (!gtk_widget_get_realized(canvas)) {
        return;
    }
    switch (request->constraint & 0xffff) {
        case 1: purpose = GTK_INPUT_PURPOSE_EMAIL; break;
        case 2: purpose = GTK_INPUT_PURPOSE_DIGITS; break;
        case 3: purpose = GTK_INPUT_PURPOSE_PHONE; break;
        case 4: purpose = GTK_INPUT_PURPOSE_URL; break;
        case 5: purpose = GTK_INPUT_PURPOSE_NUMBER; break;
        default: break;
    }
    if (request->capitalize) {
        hints |= GTK_INPUT_HINT_UPPERCASE_SENTENCES;
    }
    if (request->constraint & 0x80000) {
        hints = GTK_INPUT_HINT_NO_SPELLCHECK;
    }
    if (request->constraint & (0x10000 | 0x40000)) {
        hints = GTK_INPUT_HINT_NO_SPELLCHECK;
        /* GTK 3 has no PRIVATE hint. Do not request spelling/completion or
         * supply surrounding text; retain the ordinary IME purpose. */
    }
    if (request->constraint & 0x10000) {
        purpose = GTK_INPUT_PURPOSE_PASSWORD;
    }
    input->session = request->session;
    input->slot = request->slot;
    input->sensitive = (request->constraint & (0x10000 | 0x40000)) != 0;
    input->window = g_object_ref(window);
    input->canvas = g_object_ref(canvas);
    input->im = gtk_im_multicontext_new();
    g_object_set(input->im, "input-purpose", purpose, "input-hints", hints, NULL);
    gtk_im_context_set_client_window(input->im, gtk_widget_get_window(canvas));
    g_signal_connect(input->im, "commit", G_CALLBACK(cn1TextCommit), input);
    g_signal_connect(input->im, "preedit-changed", G_CALLBACK(cn1TextPreedit), input);
    g_signal_connect(input->im, "preedit-end", G_CALLBACK(cn1TextPreeditEnd), input);
    g_signal_connect(input->im, "retrieve-surrounding", G_CALLBACK(cn1TextSurrounding), input);
    g_signal_connect(input->im, "delete-surrounding", G_CALLBACK(cn1TextDelete), input);
    g_signal_connect(window, "focus-in-event", G_CALLBACK(cn1TextFocus), input);
    g_signal_connect(window, "focus-out-event", G_CALLBACK(cn1TextFocus), input);
    g_signal_connect(window, "destroy", G_CALLBACK(cn1TextDestroyed), input);
    gtk_widget_set_can_focus(canvas, TRUE);
    gtk_widget_grab_focus(canvas);
    input->focused = 1;
    gtk_im_context_focus_in(input->im);
    request->result = 1;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxTextInput_startNative___int_int_int_boolean_boolean_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT session, JAVA_INT slot, JAVA_INT constraint, JAVA_BOOLEAN correct, JAVA_BOOLEAN capitalize) {
    CN1TextStart request = {session, slot, constraint, correct, capitalize, 0};
    cn1LinuxRunOnMainAndWait(cn1TextStartOnMain, &request);
    return request.result ? JAVA_TRUE : JAVA_FALSE;
}

static void cn1TextStopOnMain(void* data) {
    if (cn1TextInput.session == *(int*) data) {
        cn1TextClear();
    }
}
JAVA_VOID com_codename1_impl_linux_LinuxTextInput_stopNative___int(CODENAME_ONE_THREAD_STATE, JAVA_INT session) {
    int id = session;
    cn1LinuxRunOnMainAndWait(cn1TextStopOnMain, &id);
}

typedef struct { int session, sequence, caret, x, y, width, height; char* text; } CN1TextUpdate;
static void cn1TextUpdateOnMain(void* data) {
    CN1TextUpdate* request = data;
    CN1TextInput* input = &cn1TextInput;
    const char* cursor;
    int units = 0;
    GdkRectangle rect;
    if (!input->im || input->session != request->session || input->sequence != request->sequence) {
        return;
    }
    g_free(input->surrounding);
    input->surrounding = g_strdup(request->text);
    cursor = input->surrounding;
    while (*cursor && units < request->caret) {
        int length = g_utf8_get_char(cursor) > 0xffff ? 2 : 1;
        if (units + length > request->caret) {
            break;
        }
        units += length;
        cursor = g_utf8_next_char(cursor);
    }
    input->caretBytes = (int) (cursor - input->surrounding);
    input->surroundingValid = 1;
    /* CN1 paints in drawing-area coordinates, which is also the IM client window. */
    rect.x = request->x;
    rect.y = request->y;
    rect.width = request->width;
    rect.height = request->height;
    gtk_im_context_set_cursor_location(input->im, &rect);
}
JAVA_VOID com_codename1_impl_linux_LinuxTextInput_updateNative___int_int_java_lang_String_int_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_INT session, JAVA_INT sequence, JAVA_OBJECT text, JAVA_INT caret, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1TextUpdate request = {session, sequence, caret, x, y, width, height, NULL};
    request.text = g_strdup(text == JAVA_NULL ? "" : stringToUTF8(threadStateData, text));
    cn1LinuxRunOnMainAndWait(cn1TextUpdateOnMain, &request);
    g_free(request.text);
}
JAVA_OBJECT com_codename1_impl_linux_LinuxTextInput_pollNative___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    char* event;
    JAVA_OBJECT result;
    g_mutex_lock(&cn1TextEventsMutex);
    event = g_queue_pop_head(&cn1TextEvents);
    g_mutex_unlock(&cn1TextEventsMutex);
    if (!event) {
        return JAVA_NULL;
    }
    result = newStringFromUtf8Len(threadStateData, event, (int) strlen(event));
    g_free(event);
    return result;
}

/* Invoked after each window's native-peer focus guard, before ordinary CN1 keys. */
gboolean cn1LinuxTextInputFilter(int slot, GdkEventKey* event) {
    CN1TextInput* input = &cn1TextInput;
    int command = 0, modifiers = 0;
    guint key;
    if (!input->im || !input->focused || slot != input->slot || !event) {
        return FALSE;
    }
    if (event->state & GDK_SHIFT_MASK) { modifiers |= 1; }
    if (event->state & GDK_CONTROL_MASK) { modifiers |= 2; }
    if (event->state & GDK_MOD1_MASK) { modifiers |= 4; }
    key = gdk_keyval_to_lower(event->keyval);
    if ((modifiers & 2) && !(modifiers & 4)) {
        switch (key) {
            case GDK_KEY_c: command = 12; break;
            case GDK_KEY_x: command = 13; break;
            case GDK_KEY_v: command = 14; break;
            case GDK_KEY_a: command = 15; break;
            case GDK_KEY_z: command = (modifiers & 1) ? 17 : 16; break;
            case GDK_KEY_y: command = 17; break;
            default: break;
        }
    }
    if (!command && gtk_im_context_filter_keypress(input->im, event)) {
        return TRUE;
    }
    if (!command) {
        switch (key) {
            case GDK_KEY_Left: case GDK_KEY_KP_Left: command = 1; break;
            case GDK_KEY_Right: case GDK_KEY_KP_Right: command = 2; break;
            case GDK_KEY_Up: case GDK_KEY_KP_Up: command = 3; break;
            case GDK_KEY_Down: case GDK_KEY_KP_Down: command = 4; break;
            case GDK_KEY_Home: case GDK_KEY_KP_Home: command = 5; break;
            case GDK_KEY_End: case GDK_KEY_KP_End: command = 6; break;
            case GDK_KEY_Page_Up: case GDK_KEY_KP_Page_Up: command = 7; break;
            case GDK_KEY_Page_Down: case GDK_KEY_KP_Page_Down: command = 8; break;
            case GDK_KEY_BackSpace: command = 9; break;
            case GDK_KEY_Delete: case GDK_KEY_KP_Delete: command = 10; break;
            case GDK_KEY_Escape: command = 11; break;
            case GDK_KEY_Tab: case GDK_KEY_ISO_Left_Tab: command = 18; break;
            case GDK_KEY_Return: case GDK_KEY_KP_Enter:
                if (event->type == GDK_KEY_PRESS) { cn1TextQueue(4, 0, modifiers, NULL); }
                return TRUE;
            default: break;
        }
    }
    if (command) {
        if (event->type == GDK_KEY_PRESS) {
            cn1TextQueue(3, command, modifiers, NULL);
        }
        return TRUE;
    }
    return FALSE;
}
