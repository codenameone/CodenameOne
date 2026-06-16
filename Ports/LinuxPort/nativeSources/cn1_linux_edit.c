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
 * Native in-place text editing for the native Codename One Linux port. A GtkEntry
 * (single line) or GtkTextView-in-GtkScrolledWindow (multi line) is overlaid at
 * the field's bounds in the window's GtkFixed layer, styled to match the
 * lightweight field (font + foreground/background + alignment). The control
 * commits on Enter (single line) or focus-out; the EDT polls editIsDone and then
 * reads the text via editGetText. All widget work runs on the GTK main thread.
 */

#include "cn1_linux_gfx.h"
#include <pthread.h>
#include <string.h>
#include <stdlib.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

typedef struct {
    GtkWidget* container;   /* the widget placed in the overlay (entry or scroller) */
    GtkWidget* entry;       /* GtkEntry, or NULL for multi-line */
    GtkTextBuffer* buffer;  /* GtkTextView buffer, or NULL for single-line */
    int singleLine;
    volatile int done;
    char* text;             /* latest committed/changed text (heap) */
    pthread_mutex_t lock;
} CN1Edit;

/* Creation request marshaled to the GTK main thread. */
typedef struct {
    int x, y, w, h;
    const char* text;
    int singleLine;
    int maxSize;
    CN1Font* font;
    int fg, bg, align;
    CN1Edit* result;
} CN1EditReq;

static void cn1EditCaptureText(CN1Edit* e) {
    char* t = 0;
    if (e->singleLine && e->entry) {
        t = g_strdup(gtk_entry_get_text(GTK_ENTRY(e->entry)));
    } else if (e->buffer) {
        GtkTextIter a, b;
        gtk_text_buffer_get_bounds(e->buffer, &a, &b);
        t = gtk_text_buffer_get_text(e->buffer, &a, &b, FALSE);
    }
    pthread_mutex_lock(&e->lock);
    if (e->text) {
        g_free(e->text);
    }
    e->text = t;
    pthread_mutex_unlock(&e->lock);
}

static void cn1EditChanged(GtkWidget* w, gpointer data) {
    (void) w;
    cn1EditCaptureText((CN1Edit*) data);
}

static void cn1EditActivate(GtkWidget* w, gpointer data) {
    (void) w;
    CN1Edit* e = (CN1Edit*) data;
    cn1EditCaptureText(e);
    e->done = 1;
}

static gboolean cn1EditFocusOut(GtkWidget* w, GdkEvent* ev, gpointer data) {
    (void) w;
    (void) ev;
    CN1Edit* e = (CN1Edit*) data;
    cn1EditCaptureText(e);
    e->done = 1;
    return FALSE;
}

static void cn1EditApplyStyle(GtkWidget* w, CN1Font* font, int fg, int bg) {
    if (font && font->desc) {
        gtk_widget_override_font(w, font->desc);
    }
    if (fg != -1) {
        GdkRGBA c;
        c.red = ((fg >> 16) & 0xff) / 255.0;
        c.green = ((fg >> 8) & 0xff) / 255.0;
        c.blue = (fg & 0xff) / 255.0;
        c.alpha = 1.0;
        gtk_widget_override_color(w, GTK_STATE_FLAG_NORMAL, &c);
    }
    if (bg != -1) {
        GdkRGBA c;
        c.red = ((bg >> 16) & 0xff) / 255.0;
        c.green = ((bg >> 8) & 0xff) / 255.0;
        c.blue = (bg & 0xff) / 255.0;
        c.alpha = 1.0;
        gtk_widget_override_background_color(w, GTK_STATE_FLAG_NORMAL, &c);
    }
}

static void cn1EditCreateOnMain(void* p) {
    CN1EditReq* req = (CN1EditReq*) p;
    CN1Edit* e = (CN1Edit*) calloc(1, sizeof(CN1Edit));
    pthread_mutex_init(&e->lock, 0);
    e->singleLine = req->singleLine;

    if (req->singleLine) {
        e->entry = gtk_entry_new();
        e->container = e->entry;
        gtk_widget_set_can_focus(e->entry, TRUE);
        if (req->maxSize > 0) {
            gtk_entry_set_max_length(GTK_ENTRY(e->entry), req->maxSize);
        }
        gtk_entry_set_text(GTK_ENTRY(e->entry), req->text ? req->text : "");
        gtk_entry_set_alignment(GTK_ENTRY(e->entry),
                req->align == 1 ? 0.5f : (req->align == 2 ? 1.0f : 0.0f));
        cn1EditApplyStyle(e->entry, req->font, req->fg, req->bg);
        g_signal_connect(e->entry, "changed", G_CALLBACK(cn1EditChanged), e);
        g_signal_connect(e->entry, "activate", G_CALLBACK(cn1EditActivate), e);
        g_signal_connect(e->entry, "focus-out-event", G_CALLBACK(cn1EditFocusOut), e);
    } else {
        /* Use the GtkTextView directly as the overlay widget rather than wrapping it
         * in a GtkScrolledWindow. The wrapper meant the focus grab had to reach the
         * view via gtk_bin_get_child and the view could end up unfocused, so the
         * multi-line editor received no keystrokes and never committed (the text was
         * "lost" and the field could not be re-edited). The view scrolls its own
         * content to follow the caret, which is all the overlay editor needs. */
        GtkWidget* tv = gtk_text_view_new();
        e->container = tv;
        gtk_widget_set_can_focus(tv, TRUE);
        gtk_text_view_set_editable(GTK_TEXT_VIEW(tv), TRUE);
        gtk_text_view_set_cursor_visible(GTK_TEXT_VIEW(tv), TRUE);
        gtk_text_view_set_wrap_mode(GTK_TEXT_VIEW(tv), GTK_WRAP_WORD_CHAR);
        e->buffer = gtk_text_view_get_buffer(GTK_TEXT_VIEW(tv));
        gtk_text_buffer_set_text(e->buffer, req->text ? req->text : "", -1);
        cn1EditApplyStyle(tv, req->font, req->fg, req->bg);
        g_signal_connect(e->buffer, "changed", G_CALLBACK(cn1EditChanged), e);
        g_signal_connect(tv, "focus-out-event", G_CALLBACK(cn1EditFocusOut), e);
    }

    cn1LinuxOverlayAdd(e->container, req->x, req->y, req->w, req->h);
    gtk_widget_grab_focus(e->container);
    req->result = e;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_editStringAt___int_int_int_int_java_lang_String_boolean_int_long_int_int_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT x, JAVA_INT y, JAVA_INT w, JAVA_INT h, JAVA_OBJECT text, JAVA_BOOLEAN singleLine, JAVA_INT maxSize, JAVA_LONG fontPeer, JAVA_INT fgColor, JAVA_INT bgColor, JAVA_INT align) {
    CN1EditReq req;
    if (cn1LinuxWindowWidget() == 0) {
        return 0; /* headless: no native edit */
    }
    req.x = x;
    req.y = y;
    req.w = w;
    req.h = h;
    req.text = text == JAVA_NULL ? "" : stringToUTF8(threadStateData, text);
    req.singleLine = singleLine ? 1 : 0;
    req.maxSize = maxSize;
    req.font = (CN1Font*) (intptr_t) fontPeer;
    req.fg = fgColor;
    req.bg = bgColor;
    req.align = align;
    req.result = 0;
    cn1LinuxRunOnMainAndWait(cn1EditCreateOnMain, &req);
    return (JAVA_LONG) (intptr_t) req.result;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_editIsDone___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    return (e && e->done) ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_editGetText___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    JAVA_OBJECT result;
    if (!e) {
        return JAVA_NULL;
    }
    pthread_mutex_lock(&e->lock);
    result = newStringFromCString(threadStateData, e->text ? e->text : "");
    pthread_mutex_unlock(&e->lock);
    return result;
}

static void cn1EditCloseOnMain(void* p) {
    CN1Edit* e = (CN1Edit*) p;
    if (e->container) {
        cn1LinuxOverlayRemove(e->container);
        gtk_widget_destroy(e->container);
        e->container = 0;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_editClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG peer) {
    CN1Edit* e = (CN1Edit*) (intptr_t) peer;
    if (!e) {
        return;
    }
    cn1LinuxRunOnMainAndWait(cn1EditCloseOnMain, e);
    pthread_mutex_lock(&e->lock);
    if (e->text) {
        g_free(e->text);
    }
    pthread_mutex_unlock(&e->lock);
    pthread_mutex_destroy(&e->lock);
    free(e);
}
