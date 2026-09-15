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
static int tiles_written = 0;

/* Checksum of each "<id>_normal_<appearance>" tile, and the states that came out
 * identical to it.
 *
 * Identical is not automatically wrong: Adwaita genuinely does not restyle a GtkEntry
 * on hover, only on focus. It is wrong only when it is a SURPRISE, so it is recorded
 * in the manifest rather than left for whoever later wonders why a theme's hover rule
 * scores the same either way. */
#define CN1_MAX_IDENTICAL 64
static char normal_keys[CN1_MAX_IDENTICAL][128];
static char normal_sums[CN1_MAX_IDENTICAL][80];
static int normal_count = 0;
static char identical[CN1_MAX_IDENTICAL][128];
static int identical_count = 0;

/* Backdrop colour sampled from each appearance's tiles. See assert_appearances_differ. */
static char backdrop_light[16] = "";
static char backdrop_dark[16] = "";


static void note_if_identical_to_normal(const char *name, const char *path) {
    char id[96], state[32], appearance[32];
    const char *u2 = strrchr(name, '_');
    if (!u2) {
        return;
    }
    g_strlcpy(appearance, u2 + 1, sizeof(appearance));
    size_t head = (size_t) (u2 - name);
    char without[128];
    if (head >= sizeof(without)) {
        return;
    }
    memcpy(without, name, head);
    without[head] = 0;
    const char *u1 = strrchr(without, '_');
    if (!u1) {
        return;
    }
    g_strlcpy(state, u1 + 1, sizeof(state));
    size_t idlen = (size_t) (u1 - without);
    if (idlen >= sizeof(id)) {
        return;
    }
    memcpy(id, without, idlen);
    id[idlen] = 0;

    gchar *contents = NULL;
    gsize len = 0;
    if (!g_file_get_contents(path, &contents, &len, NULL)) {
        return;
    }
    gchar *sum = g_compute_checksum_for_data(G_CHECKSUM_MD5, (const guchar *) contents, len);
    g_free(contents);

    char key[128];
    snprintf(key, sizeof(key), "%s_%s", id, appearance);
    if (strcmp(state, "normal") == 0) {
        if (normal_count < CN1_MAX_IDENTICAL) {
            g_strlcpy(normal_keys[normal_count], key, sizeof(normal_keys[0]));
            g_strlcpy(normal_sums[normal_count], sum, sizeof(normal_sums[0]));
            normal_count++;
        }
        g_free(sum);
        return;
    }
    for (int i = 0; i < normal_count; i++) {
        if (strcmp(normal_keys[i], key) == 0 && strcmp(normal_sums[i], sum) == 0) {
            if (identical_count < CN1_MAX_IDENTICAL) {
                g_strlcpy(identical[identical_count], name, sizeof(identical[0]));
                identical_count++;
            }
            printf("NATIVEREF:INFO %s is identical to its normal tile; Adwaita does not "
                   "restyle this control for this state\n", name);
            break;
        }
    }
    g_free(sum);
}

/* The tile the widget is anchored top-left in. Mirrors tile_width_px / tile_height_px in
 * fidelity-tests.yaml; if those change, this must change with them. */
#define TILE_W 240
#define TILE_H 56

/* One row of the desktop matrix. `kind` is the native_gnome key in fidelity-tests.yaml,
 * and the ids and states are that file's too: the two lists must agree or the comparator
 * pairs a CN1 render against nothing. NULL terminates each state list. */
typedef struct {
    const char *id;
    const char *kind;
    const char *states[6];
} Spec;

static const Spec SPECS[] = {
    {"DesktopButton",       "adw_button",            {"normal", "hover", "pressed", "disabled", NULL}},
    {"DesktopAccentButton", "adw_button_suggested",  {"normal", "hover", "pressed", "disabled", NULL}},
    {"DesktopTextField",    "gtk_entry",             {"normal", "hover", "disabled", NULL}},
    {"DesktopCheckBox",     "gtk_check_button",      {"normal", "selected", "hover", "disabled", NULL}},
    {"DesktopRadioButton",  "gtk_radio_button",      {"normal", "selected", "hover", "disabled", NULL}},
    {"DesktopSwitch",       "gtk_switch",            {"normal", "selected", "hover", "disabled", NULL}},
    {"DesktopSlider",       "gtk_scale",             {"normal", "hover", "disabled", NULL}},
    {"DesktopProgressBar",  "gtk_progressbar",       {"normal", NULL}},
    {"DesktopComboBox",     "gtk_dropdown",          {"normal", "hover", "disabled", NULL}},
};
#define SPEC_COUNT ((int) (sizeof(SPECS) / sizeof(SPECS[0])))

/* Controls with no natural width: layout always assigns one, so the tile width is the
 * honest answer. Kept in sync BY HAND with FULL_WIDTH_KINDS in the other reference apps
 * and FULL_WIDTH_IDS in DesktopTileRunner -- if one side stretches a control and the other
 * does not, the comparison is between two geometries and the score means nothing. */
static int is_full_width(const char *kind) {
    return strcmp(kind, "gtk_scale") == 0
        || strcmp(kind, "gtk_progressbar") == 0
        || strcmp(kind, "gtk_entry") == 0;
}
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

/* Fails the run when the light and dark passes were captured on the same backdrop.
 *
 * This is here because it happened on the Windows reference: the tile background was read
 * from a dictionary that does no element-theme resolution, so the controls went dark and
 * the surface behind them stayed light. Sixty tiles, zero blockers, and half the set on a
 * backdrop the platform never shows. Nothing downstream catches it either -- the CN1 side
 * renders its dark tiles on the real dark surface, so the pair simply scores badly and
 * reads as a theme that needs work. */
static void assert_appearances_differ(void) {
    if (backdrop_light[0] && backdrop_dark[0]) {
        printf("NATIVEREF:INFO light backdrop %s, dark backdrop %s\n",
               backdrop_light, backdrop_dark);
        if (strcmp(backdrop_light, backdrop_dark) == 0) {
            blocker("the light and dark passes were both captured on backdrop %s: the "
                    "appearance did not actually change, so half the set is mislabelled",
                    backdrop_light);
        }
    }
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
        "  },\n"
        "  \"tiles_written\": %d,\n",
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
        gtk_window_is_active(win) ? "true" : "false",
        tiles_written);

    /* Written as two more passes rather than squeezed into the format above: both are
     * variable-length arrays, and the manifest previously carried NO blockers field at
     * all -- so a consumer reading it could not tell "no blockers" from "this manifest
     * does not report them", which the Windows one does report. */
    fprintf(f, "  \"backdrop_by_appearance\": {\"light\": \"%s\", \"dark\": \"%s\"},\n",
            backdrop_light, backdrop_dark);
    fprintf(f, "  \"states_identical_to_normal\": [");
    for (int i = 0; i < identical_count; i++) {
        fprintf(f, "%s\"%s\"", i ? ", " : "", identical[i]);
    }
    fprintf(f, "],\n  \"blockers\": [");
    for (int i = 0; i < blocker_count; i++) {
        char *esc = json_escape(blockers[i]);
        fprintf(f, "%s\"%s\"", i ? ", " : "", esc ? esc : "");
        g_free(esc);
    }
    fprintf(f, "]\n}\n");
    fclose(f);
    g_free(font);
    g_free(font_esc);
    g_free(font_name);
    g_free(font_name_esc);
    printf("NATIVEREF:INFO wrote %s\n", path);
}


/* Pumps the main loop until the widget has a real allocation, or gives up.
 *
 * A fixed number of g_main_context_iteration(NULL, FALSE) calls is NOT enough and that is
 * how the first capture run failed: non-blocking iteration returns immediately when
 * nothing is pending, GTK allocates on a frame-clock tick that has not been scheduled yet,
 * and every tile after the first was read back at 0x0 and reported "was never laid out".
 * The first tile survived only because the initial present had laid it out.
 *
 * So wait on the CONDITION rather than on a count. The sleep is what lets the frame clock
 * actually fire; 2ms x 400 is 800ms of headroom per tile, and in practice it takes a
 * handful of turns. */
static int wait_for_allocation(GtkWidget *w) {
    for (int i = 0; i < 400; i++) {
        while (g_main_context_iteration(NULL, FALSE)) {
            /* drain whatever is pending */
        }
        if (gtk_widget_get_width(w) > 0 && gtk_widget_get_height(w) > 0) {
            return 1;
        }
        g_usleep(2000);
    }
    return 0;
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
        tiles_written++;
        printf("NATIVEREF:wrote %s %dx%d\n", name, width, height);
        note_if_identical_to_normal(name, path);
        /* Bottom-right corner: every widget in the matrix anchors top-left and none is as
         * tall as the tile, so this pixel is always backdrop. */
        GdkTexture *t2 = gdk_texture_new_from_filename(path, NULL);
        if (t2) {
            GBytes *bytes = NULL;
            int tw = gdk_texture_get_width(t2), th = gdk_texture_get_height(t2);
            guchar *data = g_malloc((gsize) tw * th * 4);
            gdk_texture_download(t2, data, (gsize) tw * 4);
            guchar *px = data + ((gsize) (th - 1) * tw * 4) + (gsize) (tw - 1) * 4;
            char hex[16];
            /* gdk_texture_download writes BGRA. */
            snprintf(hex, sizeof(hex), "#%02X%02X%02X", px[2], px[1], px[0]);
            if (strstr(name, "_light")) {
                g_strlcpy(backdrop_light, hex, sizeof(backdrop_light));
            } else if (strstr(name, "_dark")) {
                g_strlcpy(backdrop_dark, hex, sizeof(backdrop_dark));
            }
            g_free(data);
            (void) bytes;
            g_object_unref(t2);
        }
    }
    g_object_unref(tex);
    gsk_render_node_unref(node);
    g_object_unref(p);
}

/* Group leader for the radio buttons. GTK4 has no GtkRadioButton: a radio IS a
 * GtkCheckButton that belongs to a group, and one on its own renders as a CHECK box. The
 * leader is never captured; it exists only to make the group real. */
static GtkWidget *radio_group_leader = NULL;

static GtkWidget *make_widget(const char *kind) {
    if (strcmp(kind, "adw_button") == 0) {
        return gtk_button_new_with_label("Button");
    }
    if (strcmp(kind, "adw_button_suggested") == 0) {
        GtkWidget *b = gtk_button_new_with_label("Button");
        /* The accent-filled button in Adwaita is the suggested action, which is a CSS
         * class rather than a widget type. */
        gtk_widget_add_css_class(b, "suggested-action");
        return b;
    }
    if (strcmp(kind, "gtk_entry") == 0) {
        GtkWidget *e = gtk_entry_new();
        gtk_editable_set_text(GTK_EDITABLE(e), "Text");
        return e;
    }
    if (strcmp(kind, "gtk_check_button") == 0) {
        return gtk_check_button_new_with_label("Check");
    }
    if (strcmp(kind, "gtk_radio_button") == 0) {
        GtkWidget *r = gtk_check_button_new_with_label("Radio");
        if (!radio_group_leader) {
            radio_group_leader = gtk_check_button_new();
            g_object_ref_sink(radio_group_leader);
        }
        gtk_check_button_set_group(GTK_CHECK_BUTTON(r), GTK_CHECK_BUTTON(radio_group_leader));
        return r;
    }
    if (strcmp(kind, "gtk_switch") == 0) {
        return gtk_switch_new();
    }
    if (strcmp(kind, "gtk_scale") == 0) {
        GtkWidget *s = gtk_scale_new_with_range(GTK_ORIENTATION_HORIZONTAL, 0.0, 1.0, 0.01);
        gtk_range_set_value(GTK_RANGE(s), 0.5);
        gtk_scale_set_draw_value(GTK_SCALE(s), FALSE);
        return s;
    }
    if (strcmp(kind, "gtk_progressbar") == 0) {
        GtkWidget *p = gtk_progress_bar_new();
        gtk_progress_bar_set_fraction(GTK_PROGRESS_BAR(p), 0.6);
        return p;
    }
    if (strcmp(kind, "gtk_dropdown") == 0) {
        const char *items[] = {"Option", NULL};
        return gtk_drop_down_new_from_strings(items);
    }
    blocker("unknown native_gnome kind '%s'", kind);
    return NULL;
}

/* Sets a state flag on a widget and every descendant.
 *
 * A composite control draws through CHILD css nodes -- a GtkDropDown is a GtkToggleButton
 * in a box -- and the node Adwaita styles for :hover is the button's, not the container's.
 * Setting the flag only on the outer widget leaves the visible part unstyled, so the tile
 * comes out identical to normal and looks like "this platform has no hover" when the real
 * answer is "the flag was put on the wrong node".
 *
 * Applied to the whole subtree so the answer the capture records is the platform's. Where
 * a control genuinely has no style for the state -- Adwaita restyles a GtkEntry on focus,
 * not on hover -- the tile is still identical, and that is then a real finding, which the
 * manifest reports by name. */
static void set_state_flags_recursive(GtkWidget *w, GtkStateFlags flag) {
    gtk_widget_set_state_flags(w, flag, FALSE);
    for (GtkWidget *c = gtk_widget_get_first_child(w); c; c = gtk_widget_get_next_sibling(c)) {
        set_state_flags_recursive(c, flag);
    }
}

/* Applies one state. Returns 0 when the state cannot be expressed, which is a reason to
 * skip the tile rather than to write a mislabelled one. */
static int apply_state(GtkWidget *w, const char *state, const char *kind) {
    if (strcmp(state, "normal") == 0) {
        return 1;
    }
    if (strcmp(state, "hover") == 0) {
        /* PRELIGHT is exactly what the CSS :hover pseudo-class resolves from, so Adwaita
         * restyles the widget for real rather than the app drawing its idea of a hover. */
        set_state_flags_recursive(w, GTK_STATE_FLAG_PRELIGHT);
        return 1;
    }
    if (strcmp(state, "pressed") == 0) {
        set_state_flags_recursive(w, GTK_STATE_FLAG_ACTIVE);
        return 1;
    }
    if (strcmp(state, "disabled") == 0) {
        gtk_widget_set_sensitive(w, FALSE);
        return 1;
    }
    if (strcmp(state, "selected") == 0) {
        if (strcmp(kind, "gtk_switch") == 0) {
            gtk_switch_set_active(GTK_SWITCH(w), TRUE);
            return 1;
        }
        if (GTK_IS_CHECK_BUTTON(w)) {
            gtk_check_button_set_active(GTK_CHECK_BUTTON(w), TRUE);
            return 1;
        }
        return 0;
    }
    blocker("unknown state '%s'", state);
    return 0;
}

/* Builds one tile: a fixed 240x56 surface carrying the theme's window background, with the
 * widget anchored top-left. Returns the tile, or NULL when the state could not be applied.
 *
 * The "background" CSS class is what makes the surface the window colour. Without it the
 * container paints nothing, the PNG comes out transparent behind the widget, and the
 * comparator's content mask -- which measures distance from a backdrop colour -- has no
 * backdrop to measure from. */
static GtkWidget *build_tile(const Spec *spec, const char *state) {
    GtkWidget *w = make_widget(spec->kind);
    if (!w) {
        return NULL;
    }
    if (!apply_state(w, state, spec->kind)) {
        g_object_ref_sink(w);
        g_object_unref(w);
        return NULL;
    }
    gtk_widget_set_halign(w, is_full_width(spec->kind) ? GTK_ALIGN_FILL : GTK_ALIGN_START);
    gtk_widget_set_valign(w, GTK_ALIGN_START);
    if (is_full_width(spec->kind)) {
        gtk_widget_set_size_request(w, TILE_W, -1);
        gtk_widget_set_hexpand(w, TRUE);
    }

    GtkWidget *tile = gtk_box_new(GTK_ORIENTATION_VERTICAL, 0);
    gtk_widget_add_css_class(tile, "background");
    gtk_widget_set_size_request(tile, TILE_W, TILE_H);
    gtk_box_append(GTK_BOX(tile), w);
    return tile;
}

/* Captures the whole matrix for one appearance. The window's child is swapped per tile and
 * the main loop is pumped so GTK actually lays the new child out before it is read back --
 * without that every tile after the first is captured at the previous one's allocation. */
static void capture_appearance(GtkWindow *win, const char *appearance) {
    AdwStyleManager *sm = adw_style_manager_get_default();
    /* FORCE_* rather than PREFER_*: the forcing variants override whatever the desktop
     * portal reports, so no xdg-desktop-portal needs to be running and the appearance
     * cannot drift with the runner image. */
    adw_style_manager_set_color_scheme(sm,
            strcmp(appearance, "dark") == 0 ? ADW_COLOR_SCHEME_FORCE_DARK
                                            : ADW_COLOR_SCHEME_FORCE_LIGHT);
    /* Let the style change propagate. Adwaita restyles every widget from the colour
     * scheme, and a tile captured mid-transition carries the previous palette. */
    for (int i = 0; i < 50; i++) {
        while (g_main_context_iteration(NULL, FALSE)) {
            /* drain */
        }
        g_usleep(2000);
    }

    for (int s = 0; s < SPEC_COUNT; s++) {
        const Spec *spec = &SPECS[s];
        for (int j = 0; spec->states[j]; j++) {
            char name[256];
            snprintf(name, sizeof(name), "%s_%s_%s", spec->id, spec->states[j], appearance);
            GtkWidget *tile = build_tile(spec, spec->states[j]);
            if (!tile) {
                blocker("%s produced no tile", name);
                continue;
            }
            gtk_window_set_child(win, tile);
            if (!wait_for_allocation(tile)) {
                blocker("%s was never allocated; GTK did not lay the tile out", name);
                continue;
            }
            capture_widget(win, tile, name);
        }
    }
}

static gboolean on_ready(gpointer data) {
    GtkWindow *win = GTK_WINDOW(data);
    GtkWidget *content = gtk_window_get_child(win);
    GtkWidget *probe_widget = content ? gtk_widget_get_first_child(content) : NULL;

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

    char *font = resolved_font(probe_widget ? probe_widget : GTK_WIDGET(win));
    printf("NATIVEREF:INFO resolved font = %s\n", font);
    if (font && (strstr(font, "DejaVu") || strstr(font, "Sans ") == font)) {
        blocker("fontconfig resolved '%s' -- Cantarell / Adwaita Sans is missing, so "
                "every text metric would measure font availability, not fidelity", font);
    }
    g_free(font);

    if (is_probe) {
        /* Even in probe mode take one tile: a manifest that says the environment is fine
         * while the render path is broken is a check satisfiable by nothing happening. */
        GtkWidget *tile = build_tile(&SPECS[0], "normal");
        if (tile) {
            gtk_window_set_child(win, tile);
            if (!wait_for_allocation(tile)) {
                blocker("the probe tile was never allocated");
            }
            capture_widget(win, tile, "probe_DesktopButton_normal_light");
        } else {
            blocker("the probe tile could not be built");
        }
    } else {
        capture_appearance(win, "light");
        capture_appearance(win, "dark");
        assert_appearances_differ();
        int expected = 0;
        for (int s = 0; s < SPEC_COUNT; s++) {
            for (int j = 0; SPECS[s].states[j]; j++) {
                expected++;
            }
        }
        expected *= 2;
        if (tiles_written != expected) {
            blocker("wrote %d tiles, expected %d: a partial set would be committed as if "
                    "it were the whole matrix", tiles_written, expected);
        }
    }

    write_manifest(win, probe_widget ? probe_widget : GTK_WIDGET(win));

    for (int i = 0; i < blocker_count; i++) {
        fprintf(stderr, "NATIVEREF:BLOCKER %s\n", blockers[i]);
    }
    printf("NATIVEREF:DONE tiles=%d exit=%d\n", tiles_written, exit_code);
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
    adw_style_manager_set_color_scheme(sm, ADW_COLOR_SCHEME_FORCE_LIGHT);

    GtkSettings *settings = gtk_settings_get_default();
    g_object_set(settings,
                 /* Set explicitly, not inherited. There is no GNOME settings daemon on a
                  * bare Xvfb, so GTK falls back to "Sans 10" and fontconfig resolves DejaVu
                  * -- installing fonts-cantarell is necessary but not sufficient, which is
                  * exactly what the first probe run reported. */
                 "gtk-font-name", "Cantarell 11",
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
    /* Sized to one tile: the window IS the tile surface, so nothing else can bleed into a
     * capture and the allocation the widget gets is the allocation it is measured at. */
    gtk_window_set_default_size(GTK_WINDOW(win), TILE_W, TILE_H);
    gtk_window_set_decorated(GTK_WINDOW(win), FALSE);
    gtk_window_set_resizable(GTK_WINDOW(win), FALSE);

    GtkWidget *first = build_tile(&SPECS[0], "normal");
    gtk_window_set_child(GTK_WINDOW(win), first);

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
