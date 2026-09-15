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
 * GNOME (GTK4 + libadwaita) native reference app for the Codename One fidelity suite.
 *
 * Desktop counterpart to ios-native-ref/NativeRef.swift and android-native-ref/. It
 * renders real Adwaita widgets and writes one PNG per tile, plus a capture-manifest.json
 * describing the environment that produced them, so a later run can prove it is
 * comparable.
 *
 * Two modes, selected by NATIVEREF_MODE:
 *
 *   probe   -- answer the environment questions only, and say so loudly. Does the window
 *              actually activate under Xvfb? Which font did fontconfig really resolve?
 *              Which GSK renderer is live? This mode exists because every one of those
 *              silently degrades rather than failing, and a degraded reference is worse
 *              than no reference: it bakes a wrong design into the theme and the fidelity
 *              metric cannot tell you it happened.
 *   capture -- the same, plus the reference tiles.
 *
 * Built by scripts/build-gnome-native-ref.sh with a single cc line -- no meson, no
 * autotools -- which is the spiritual equivalent of NativeRef.swift having no xcodeproj.
 */
#include <adwaita.h>
#include <gtk/gtk.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char *out_dir = NULL;
static int is_probe = 1;
static int exit_code = 0;

/* Findings that must fail the run rather than produce a quietly wrong reference. */
static char blockers[8][256];
static int blocker_count = 0;

static void blocker(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    if (blocker_count < 8) {
        vsnprintf(blockers[blocker_count], sizeof(blockers[0]), fmt, ap);
        blocker_count++;
    }
    va_end(ap);
    exit_code = 20;
}

static char *json_escape(const char *s) {
    GString *o = g_string_new("");
    for (; s && *s; s++) {
        if (*s == '"' || *s == '\\') g_string_append_printf(o, "\\%c", *s);
        else if (*s == '\n') g_string_append(o, "\\n");
        else g_string_append_c(o, *s);
    }
    return g_string_free(o, FALSE);
}

/*
 * The resolved font is load-bearing and is the thing most likely to be wrong without
 * anyone noticing. Adwaita's default is Cantarell (GNOME 46 and earlier) or Adwaita Sans
 * (47+); with neither package installed fontconfig silently substitutes DejaVu Sans and
 * every text metric in the reference is then measuring font availability rather than
 * theme fidelity.
 */
static char *resolved_font(GtkWidget *w) {
    PangoContext *pc = gtk_widget_get_pango_context(w);
    const PangoFontDescription *d = pango_context_get_font_description(pc);
    return pango_font_description_to_string((PangoFontDescription *) d);
}

static void write_manifest(GtkWindow *win, GtkWidget *probe_widget) {
    char path[1024];
    snprintf(path, sizeof(path), "%s/capture-manifest.json", out_dir);
    FILE *f = fopen(path, "w");
    if (!f) {
        fprintf(stderr, "NATIVEREF:ERR cannot write %s\n", path);
        exit_code = 21;
        return;
    }

    char *font = resolved_font(probe_widget);
    char *font_esc = json_escape(font);
    const char *renderer = g_getenv("GSK_RENDERER");
    GtkSettings *settings = gtk_settings_get_default();
    char *font_name = NULL;
    gboolean animations = TRUE;
    g_object_get(settings, "gtk-font-name", &font_name, "gtk-enable-animations", &animations, NULL);
    char *font_name_esc = json_escape(font_name ? font_name : "");
    AdwStyleManager *sm = adw_style_manager_get_default();
    int scale = gtk_widget_get_scale_factor(GTK_WIDGET(win));

    fprintf(f,
        "{\n"
        "  \"schema\": 1,\n"
        "  \"platform\": \"gnome\",\n"
        "  \"golden_set\": \"%s\",\n"
        "  \"mode\": \"%s\",\n"
        "  \"toolkit\": {\n"
        "    \"name\": \"GTK4\",\n"
        "    \"gtk\": \"%d.%d.%d\",\n"
        "    \"libadwaita\": \"%d.%d.%d\",\n"
        "    \"gsk_renderer\": \"%s\"\n"
        "  },\n"
        "  \"display\": {\n"
        "    \"scale_factor\": %d,\n"
        "    \"gdk_scale\": \"%s\",\n"
        "    \"gdk_dpi_scale\": \"%s\"\n"
        "  },\n"
        "  \"appearance\": {\n"
        "    \"color_scheme\": \"%s\",\n"
        "    \"dark\": %s,\n"
        "    \"high_contrast\": %s,\n"
        "    \"animations_enabled\": %s\n"
        "  },\n"
        "  \"fonts\": {\n"
        "    \"gtk_font_name\": \"%s\",\n"
        "    \"resolved\": \"%s\"\n"
        "  },\n"
        "  \"window\": {\n"
        "    \"active\": %s\n"
        "  }\n"
        "}\n",
        g_getenv("CN1SS_FIDELITY_GOLDEN_SET") ? g_getenv("CN1SS_FIDELITY_GOLDEN_SET") : "gnome-adwaita",
        is_probe ? "probe" : "capture",
        gtk_get_major_version(), gtk_get_minor_version(), gtk_get_micro_version(),
        ADW_MAJOR_VERSION, ADW_MINOR_VERSION, ADW_MICRO_VERSION,
        renderer ? renderer : "(default)",
        scale,
        g_getenv("GDK_SCALE") ? g_getenv("GDK_SCALE") : "(unset)",
        g_getenv("GDK_DPI_SCALE") ? g_getenv("GDK_DPI_SCALE") : "(unset)",
        adw_style_manager_get_dark(sm) ? "dark" : "light",
        adw_style_manager_get_dark(sm) ? "true" : "false",
        adw_style_manager_get_high_contrast(sm) ? "true" : "false",
        animations ? "true" : "false",
        font_name_esc,
        font_esc,
        gtk_window_is_active(win) ? "true" : "false");
    fclose(f);
    g_free(font);
    g_free(font_esc);
    g_free(font_name);
    g_free(font_name_esc);
    printf("NATIVEREF:INFO wrote %s\n", path);
}

/* Renders a widget through the window's OWN GskRenderer -- the same renderer that painted
 * it on screen -- rather than grabbing X11 pixels. The widget is realized, allocated and
 * state-flagged inside a real mapped window, so measurement and CSS state resolution are
 * genuinely live; only the final read-back avoids the grab, which removes cursor-in-shot,
 * root-window bleed and compositor flakiness in one go. */
static void capture_widget(GtkWindow *win, GtkWidget *w, const char *name) {
    int width = gtk_widget_get_width(w);
    int height = gtk_widget_get_height(w);
    if (width <= 0 || height <= 0) {
        blocker("%s has no allocation (%dx%d) -- it was never laid out", name, width, height);
        return;
    }
    GdkPaintable *p = gtk_widget_paintable_new(w);
    GtkSnapshot *snap = gtk_snapshot_new();
    gdk_paintable_snapshot(p, GDK_SNAPSHOT(snap), (double) width, (double) height);
    GskRenderNode *node = gtk_snapshot_free_to_node(snap);
    if (!node) {
        blocker("%s produced an empty render node", name);
        g_object_unref(p);
        return;
    }
    GskRenderer *r = gtk_native_get_renderer(GTK_NATIVE(win));
    GdkTexture *tex = gsk_renderer_render_texture(r, node, NULL);
    char path[1024];
    snprintf(path, sizeof(path), "%s/%s.png", out_dir, name);
    if (!gdk_texture_save_to_png(tex, path)) {
        blocker("%s could not be written to %s", name, path);
    } else {
        printf("NATIVEREF:wrote %s %dx%d\n", name, width, height);
    }
    g_object_unref(tex);
    gsk_render_node_unref(node);
    g_object_unref(p);
}

static gboolean on_ready(gpointer data) {
    GtkWindow *win = GTK_WINDOW(data);
    GtkWidget *content = gtk_window_get_child(win);
    GtkWidget *button = gtk_widget_get_first_child(content);

    /* Under a bare Xvfb with no window manager nothing takes focus, so every toplevel
     * sits in GTK_STATE_FLAG_BACKDROP and Adwaita draws the whole window in its dimmed,
     * unfocused style. That is the GTK equivalent of an unfocused NSWindow greying every
     * AppKit control, and it would produce a whole reference set wrong in the same
     * direction -- which is exactly the kind of error nobody spots for a month. Assert it
     * rather than hoping openbox did its job. */
    if (!gtk_window_is_active(win)) {
        blocker("the window is not active: every widget would be captured in the "
                "dimmed backdrop state. Is a window manager running on $DISPLAY?");
    }

    char *font = resolved_font(button);
    printf("NATIVEREF:INFO resolved font = %s\n", font);
    if (font && (strstr(font, "DejaVu") || strstr(font, "Sans ") == font)) {
        blocker("fontconfig resolved '%s' -- Cantarell / Adwaita Sans is missing, so "
                "every text metric would measure font availability, not fidelity", font);
    }
    g_free(font);

    if (!is_probe) {
        capture_widget(win, button, "Button_normal_light");
    } else {
        /* Even in probe mode take one tile: a manifest that says the environment is fine
         * while the render path is broken is a check satisfiable by nothing happening. */
        capture_widget(win, button, "probe_Button_normal_light");
    }

    write_manifest(win, button);

    for (int i = 0; i < blocker_count; i++) {
        fprintf(stderr, "NATIVEREF:BLOCKER %s\n", blockers[i]);
    }
    printf("NATIVEREF:DONE exit=%d\n", exit_code);
    gtk_window_close(win);
    return G_SOURCE_REMOVE;
}

int main(int argc, char **argv) {
    out_dir = g_getenv("NATIVEREF_OUT");
    if (!out_dir) {
        fprintf(stderr, "NATIVEREF:ERR NATIVEREF_OUT is not set\n");
        return 2;
    }
    const char *mode = g_getenv("NATIVEREF_MODE");
    is_probe = !(mode && strcmp(mode, "capture") == 0);

    adw_init();

    AdwStyleManager *sm = adw_style_manager_get_default();
    /* FORCE_LIGHT rather than PREFER_LIGHT: the forcing variants override whatever the
     * desktop portal reports, so no xdg-desktop-portal needs to be running and the
     * appearance cannot drift with the runner image. */
    adw_style_manager_set_color_scheme(sm, ADW_COLOR_SCHEME_FORCE_LIGHT);

    GtkSettings *settings = gtk_settings_get_default();
    g_object_set(settings,
                 "gtk-enable-animations", FALSE,
                 "gtk-cursor-blink", FALSE,
                 /* Grayscale, not subpixel. The runner default produces coloured fringes
                  * on every glyph edge that Codename One's grayscale AA can never match,
                  * which would show up as a permanent, unfixable text residual. */
                 "gtk-xft-rgba", "none",
                 "gtk-xft-antialias", 1,
                 "gtk-xft-hinting", 1,
                 "gtk-xft-hintstyle", "hintslight",
                 "gtk-icon-theme-name", "Adwaita",
                 NULL);

    GtkWidget *win = gtk_window_new();
    gtk_window_set_title(GTK_WINDOW(win), "cn1-native-ref");
    gtk_window_set_default_size(GTK_WINDOW(win), 480, 240);
    gtk_window_set_decorated(GTK_WINDOW(win), FALSE);

    GtkWidget *box = gtk_box_new(GTK_ORIENTATION_VERTICAL, 12);
    GtkWidget *button = gtk_button_new_with_label("Default");
    gtk_widget_set_halign(button, GTK_ALIGN_START);
    gtk_widget_set_valign(button, GTK_ALIGN_START);
    gtk_box_append(GTK_BOX(box), button);
    gtk_window_set_child(GTK_WINDOW(win), box);

    gtk_window_present(GTK_WINDOW(win));

    /* Give the frame clock a couple of turns so the first frame is actually presented
     * before anything is read back; a capture taken before the first present is the
     * classic uniformly-blank tile. */
    g_timeout_add(600, on_ready, win);

    while (g_list_model_get_n_items(gtk_window_get_toplevels()) > 0) {
        g_main_context_iteration(NULL, TRUE);
    }
    return exit_code;
}
