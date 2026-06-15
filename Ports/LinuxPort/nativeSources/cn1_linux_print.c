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
 * Printing for the native Codename One Linux port. Images are printed through a
 * GtkPrintOperation (the system print dialog, drawing the decoded pixbuf scaled
 * to the page); PDFs and other document types are spooled to CUPS via lp, which
 * renders them natively. printDocument returns 0 (handed to the spooler), 1
 * (dialog cancelled) or 2 (failure, with printLastError carrying a description).
 */

#include "cn1_linux_gfx.h"
#include <string.h>
#include <stdlib.h>

extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

static char cn1PrintError[512];

typedef struct { const char* path; const char* mime; const char* job; int result; } CN1PrintReq;

static void cn1PrintDrawPage(GtkPrintOperation* op, GtkPrintContext* ctx, gint pageNr, gpointer data) {
    GdkPixbuf* pix = (GdkPixbuf*) data;
    cairo_t* cr = gtk_print_context_get_cairo_context(ctx);
    double pageW = gtk_print_context_get_width(ctx);
    double pageH = gtk_print_context_get_height(ctx);
    int imgW = gdk_pixbuf_get_width(pix);
    int imgH = gdk_pixbuf_get_height(pix);
    double scale = pageW / imgW;
    (void) op;
    (void) pageNr;
    if (imgH * scale > pageH) {
        scale = pageH / imgH;
    }
    cairo_save(cr);
    cairo_scale(cr, scale, scale);
    gdk_cairo_set_source_pixbuf(cr, pix, 0, 0);
    cairo_paint(cr);
    cairo_restore(cr);
}

static void cn1PrintImageOnMain(void* p) {
    CN1PrintReq* req = (CN1PrintReq*) p;
    GError* err = 0;
    GdkPixbuf* pix = gdk_pixbuf_new_from_file(req->path, &err);
    GtkPrintOperation* op;
    GtkPrintOperationResult res;
    if (!pix) {
        snprintf(cn1PrintError, sizeof(cn1PrintError), "image decode failed: %s",
                err ? err->message : "?");
        if (err) g_error_free(err);
        req->result = 2;
        return;
    }
    op = gtk_print_operation_new();
    if (req->job) {
        gtk_print_operation_set_job_name(op, req->job);
    }
    gtk_print_operation_set_n_pages(op, 1);
    g_signal_connect(op, "draw-page", G_CALLBACK(cn1PrintDrawPage), pix);
    res = gtk_print_operation_run(op, GTK_PRINT_OPERATION_ACTION_PRINT_DIALOG,
            GTK_WINDOW(cn1LinuxWindowWidget()), &err);
    if (res == GTK_PRINT_OPERATION_RESULT_APPLY) {
        req->result = 0;
    } else if (res == GTK_PRINT_OPERATION_RESULT_CANCEL) {
        req->result = 1;
    } else {
        snprintf(cn1PrintError, sizeof(cn1PrintError), "print failed: %s",
                err ? err->message : "?");
        req->result = 2;
    }
    if (err) g_error_free(err);
    g_object_unref(op);
    g_object_unref(pix);
}

/* Spools a document (e.g. PDF) to CUPS via lp; CUPS renders it for the printer. */
static int cn1PrintViaLp(const char* path, const char* job) {
    char* argv[6];
    GError* err = 0;
    gint status = 0;
    int i = 0;
    argv[i++] = (char*) "lp";
    if (job) {
        argv[i++] = (char*) "-t";
        argv[i++] = (char*) job;
    }
    argv[i++] = (char*) path;
    argv[i] = 0;
    if (!g_spawn_sync(0, argv, 0, G_SPAWN_SEARCH_PATH, 0, 0, 0, 0, &status, &err)) {
        snprintf(cn1PrintError, sizeof(cn1PrintError), "lp spawn failed: %s",
                err ? err->message : "?");
        if (err) g_error_free(err);
        return 2;
    }
    if (status != 0) {
        snprintf(cn1PrintError, sizeof(cn1PrintError), "lp exited with status %d", status);
        return 2;
    }
    return 0;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_printDocument___java_lang_String_java_lang_String_java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_OBJECT mimeType, JAVA_OBJECT jobName) {
    CN1PrintReq req;
    cn1PrintError[0] = 0;
    req.path = path == JAVA_NULL ? 0 : stringToUTF8(threadStateData, path);
    req.mime = mimeType == JAVA_NULL ? "" : stringToUTF8(threadStateData, mimeType);
    req.job = jobName == JAVA_NULL ? 0 : stringToUTF8(threadStateData, jobName);
    req.result = 2;
    if (!req.path) {
        snprintf(cn1PrintError, sizeof(cn1PrintError), "null path");
        return 2;
    }
    if (strncmp(req.mime, "image", 5) == 0) {
        if (cn1LinuxWindowWidget() == 0) {
            return cn1PrintViaLp(req.path, req.job); /* headless: no dialog */
        }
        cn1LinuxRunOnMainAndWait(cn1PrintImageOnMain, &req);
        return req.result;
    }
    /* PDF and everything else: hand to CUPS, which rasterizes natively. */
    return cn1PrintViaLp(req.path, req.job);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_printLastError___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    if (cn1PrintError[0] == 0) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, cn1PrintError);
}
