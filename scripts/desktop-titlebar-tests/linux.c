/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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


/* Local GTK test of the production window code; unused VM bridges are removed
 * with --gc-sections. This is not a Java UI or a replacement application host. */
#include "cn1_linux_window.c"
#include <assert.h>

GtkWidget* cn1LinuxDesktopFixed(int slot) { (void)slot; return NULL; }
static void settle(void) {
    for (int i = 0; i < 40; i++) {
        while (gtk_events_pending()) gtk_main_iteration();
        g_usleep(5000);
    }
}
static void snapshot(const char* filename) {
    GdkWindow* window = gtk_widget_get_window(cn1Window);
    GdkPixbuf* pixels = gdk_pixbuf_get_from_window(window, 0, 0,
            gdk_window_get_width(window), gdk_window_get_height(window));
    assert(pixels);
    assert(gdk_pixbuf_save(pixels, filename, "png", NULL, NULL));
    g_object_unref(pixels);
}
static GtkWidget* closeButton(GtkWidget* widget);
static void findCloseChild(GtkWidget* child, gpointer data) {
    GtkWidget** result = (GtkWidget**)data;
    if (!*result) *result = closeButton(child);
}
static GtkWidget* closeButton(GtkWidget* widget) {
    if (GTK_IS_BUTTON(widget)) {
        const char* name = atk_object_get_name(gtk_widget_get_accessible(widget));
        gboolean close = name && !strcmp(name, "Close");
        if (close) return widget;
    }
    if (GTK_IS_CONTAINER(widget)) {
        GtkWidget* found = NULL;
        gtk_container_forall(GTK_CONTAINER(widget), findCloseChild, &found);
        return found;
    }
    return NULL;
}
int main(int argc, char** argv) {
    gtk_init(&argc, &argv);
    cn1GtkThread = pthread_self();
    cn1Window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(cn1Window), "CN1 integrated title test");
    g_signal_connect(cn1Window, "delete-event", G_CALLBACK(cn1OnDelete), NULL);
    gtk_window_set_default_size(GTK_WINDOW(cn1Window), 720, 420);
    cn1DrawingArea = gtk_drawing_area_new();
    gtk_widget_set_events(cn1DrawingArea, GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK |
            GDK_POINTER_MOTION_MASK | GDK_LEAVE_NOTIFY_MASK | GDK_SCROLL_MASK | GDK_TOUCH_MASK);
    cn1Overlay = gtk_overlay_new();
    gtk_container_add(GTK_CONTAINER(cn1Overlay), cn1DrawingArea);
    cn1Fixed = gtk_fixed_new();
    gtk_overlay_add_overlay(GTK_OVERLAY(cn1Overlay), cn1Fixed);
    gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(cn1Overlay), cn1Fixed, TRUE);
    cn1AccessibilityFixed = gtk_fixed_new();
    gtk_overlay_add_overlay(GTK_OVERLAY(cn1Overlay), cn1AccessibilityFixed);
    gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(cn1Overlay), cn1AccessibilityFixed, TRUE);
    gtk_container_add(GTK_CONTAINER(cn1Window), cn1Overlay);
    g_signal_connect(cn1DrawingArea, "draw", G_CALLBACK(cn1OnDraw), NULL);
    g_signal_connect(cn1DrawingArea, "configure-event", G_CALLBACK(cn1OnConfigure), NULL);
    g_object_set(gtk_settings_get_default(), "gtk-decoration-layout", ":minimize,maximize,close", NULL);
    cn1TitleEnable(TRUE);
    gtk_widget_show_all(cn1Window);
    settle();
    int right[5] = {0};
    cn1TitleMetrics(right);
    printf("right: %d %d %d %d %d canvas width %d\n", right[0], right[1], right[2], right[3], right[4], gtk_widget_get_allocated_width(cn1TitleCanvas));
    assert(right[1] > right[0]);
    assert(gtk_widget_get_allocated_width(cn1TitleCanvas) > 400);
    assert(gtk_widget_get_allocated_height(cn1TitleCanvas) > 20);
    assert(right[2] + right[3] < 20);
    assert(cn1DisplayHeight == gtk_widget_get_allocated_height(cn1DrawingArea) + cn1TitleHeight);
    cn1ApplyPendingResize();
    cairo_set_source_rgb(cn1WindowG.cr, 0.95, 0.95, 0.95);
    cairo_paint(cn1WindowG.cr);
    cairo_set_source_rgb(cn1WindowG.cr, 0.1, 0.2, 0.4);
    cairo_select_font_face(cn1WindowG.cr, "Sans", CAIRO_FONT_SLANT_NORMAL, CAIRO_FONT_WEIGHT_NORMAL);
    cairo_set_font_size(cn1WindowG.cr, 15);
    cairo_move_to(cn1WindowG.cr, right[0] + 12, right[2] + 22);
    cairo_show_text(cn1WindowG.cr, "Save     Undo       Document title       Search");
    cairo_move_to(cn1WindowG.cr, 20, cn1TitleHeight + 35);
    cairo_show_text(cn1WindowG.cr, "Document starts below the integrated title bar");
    gtk_widget_queue_draw(cn1Window);
    settle();
    if (argc > 1) snapshot(argv[1]);
    cn1TitleRegions = malloc(8 * sizeof(int));
    int regions[] = {0, 0, cn1DisplayWidth, cn1TitleHeight, right[0], right[2], 80, 28};
    memcpy(cn1TitleRegions, regions, sizeof(regions));
    cn1TitleRegionCount = 8;
    GdkEventButton event = {0};
    event.type = GDK_BUTTON_PRESS;
    event.button = 1;
    event.x = 10; event.y = 10;
    assert(cn1TitleButton(cn1TitleCanvas, &event, NULL)); // application control consumes
    event.x = 200;
    event.type = GDK_BUTTON_RELEASE;
    assert(cn1TitleButton(cn1TitleCanvas, &event, NULL)); // release outside still completes control gesture
    assert(!cn1TitlePointerButtons);
    event.type = GDK_BUTTON_PRESS;
    assert(!cn1TitleButton(cn1TitleCanvas, &event, NULL)); // GTK owns blank caption
    assert(cn1TitleEventY(cn1DrawingArea, 12) == cn1TitleHeight + 12);
    GtkWidget* edit = gtk_entry_new();
    cn1LinuxOverlayAdd(CN1_MAIN_WINDOW_SLOT, edit, right[0] + 5, right[2] + 5, 100, 22);
    assert(gtk_widget_get_parent(edit) == cn1TitleFixed);
    settle();
    // Use real pointer/key events: parentage alone cannot detect a pass-through host.
    gtk_window_set_focus(GTK_WINDOW(cn1Window), NULL);
    int editX, editY;
    gtk_widget_translate_coordinates(edit, cn1TitleCanvas, 10, 10, &editX, &editY);
    int canvasX, canvasY;
    gdk_window_get_origin(gtk_widget_get_window(cn1TitleCanvas), &canvasX, &canvasY);
    char input[200];
    snprintf(input, sizeof(input), "xdotool mousemove %d %d click 1", canvasX + editX, canvasY + editY);
    assert(g_spawn_command_line_sync(input, NULL, NULL, NULL, NULL));
    settle();
    assert(gtk_widget_has_focus(edit));
    assert(g_spawn_command_line_sync("xdotool type native-title-input", NULL, NULL, NULL, NULL));
    settle();
    assert(!strcmp(gtk_entry_get_text(GTK_ENTRY(edit)), "native-title-input"));
    // A blank portion of the same overlay must still reach the title canvas.
    snprintf(input, sizeof(input), "xdotool mousemove %d %d click --repeat 2 --delay 80 1",
            canvasX + 300, canvasY + 20);
    assert(g_spawn_command_line_sync(input, NULL, NULL, NULL, NULL));
    settle();
    assert(gtk_window_is_maximized(GTK_WINDOW(cn1Window)));
    gtk_window_unmaximize(GTK_WINDOW(cn1Window));
    settle();
    cn1LinuxOverlayMove(CN1_MAIN_WINDOW_SLOT, edit, 20, cn1TitleHeight + 30, 100, 22);
    assert(gtk_widget_get_parent(edit) == cn1Fixed);
    cn1LinuxOverlayRemove(CN1_MAIN_WINDOW_SLOT, edit);
    // A peer spanning the title/body boundary must retain its document portion.
    GtkWidget* spanning = gtk_drawing_area_new();
    cn1LinuxOverlayAdd(CN1_MAIN_WINDOW_SLOT, spanning, 0, 0, 300, cn1TitleHeight + 120);
    assert(gtk_widget_get_parent(spanning) == cn1Fixed);
    cn1LinuxOverlayMove(CN1_MAIN_WINDOW_SLOT, spanning, right[0], right[2], 100, 20);
    assert(gtk_widget_get_parent(spanning) == cn1TitleFixed);
    cn1LinuxOverlayMove(CN1_MAIN_WINDOW_SLOT, spanning, 0, cn1TitleHeight - 5, 300, 120);
    assert(gtk_widget_get_parent(spanning) == cn1Fixed);
    int spanningY;
    gtk_container_child_get(GTK_CONTAINER(cn1Fixed), spanning, "y", &spanningY, NULL);
    assert(spanningY == -5);
    cn1LinuxOverlayRemove(CN1_MAIN_WINDOW_SLOT, spanning);
    g_object_set(gtk_settings_get_default(), "gtk-decoration-layout", "close,minimize,maximize:", NULL);
    settle();
    int left[5] = {0};
    cn1TitleMetrics(left);
    printf("left: %d %d %d %d %d\n", left[0], left[1], left[2], left[3], left[4]);
    assert(left[0] > left[1]);
    if (argc > 2) snapshot(argv[2]);
    int originX, originY;
    gdk_window_get_origin(gtk_widget_get_window(cn1TitleCanvas), &originX, &originY);
    char click[160];
    snprintf(click, sizeof(click), "xdotool mousemove %d %d click --repeat 2 --delay 80 1",
            originX + 200, originY + 20);
    assert(g_spawn_command_line_sync(click, NULL, NULL, NULL, NULL));
    settle();
    assert(gtk_window_is_maximized(GTK_WINDOW(cn1Window)));
    gtk_window_unmaximize(GTK_WINDOW(cn1Window));
    settle();
    assert(!gtk_window_is_maximized(GTK_WINDOW(cn1Window)));
    edit = gtk_entry_new();
    cn1LinuxOverlayAdd(CN1_MAIN_WINDOW_SLOT, edit, left[0] + 5, left[2] + 5, 100, 22);
    GtkWidget* accessible = gtk_button_new_with_label("Title action");
    gtk_fixed_put(GTK_FIXED(cn1TitleAccessibilityFixed), accessible, 5, 5);
    cn1TitleEnable(FALSE);
    settle();
    assert(gtk_widget_get_parent(edit) == cn1Fixed);
    assert(gtk_widget_get_parent(accessible) == cn1AccessibilityFixed);
    cn1LinuxOverlayRemove(CN1_MAIN_WINDOW_SLOT, edit);
    assert(!gtk_window_get_titlebar(GTK_WINDOW(cn1Window)));
    assert(cn1TitleHeight == 0);
    cn1TitleEnable(TRUE);
    settle();
    assert(GTK_IS_HEADER_BAR(gtk_window_get_titlebar(GTK_WINDOW(cn1Window))));
    if (argc > 2) snapshot(argv[2]);
    GtkWidget* close = closeButton(cn1TitleHeader);
    assert(close);
    cn1WindowOpen = 1;
    gtk_button_clicked(GTK_BUTTON(close));
    settle();
    assert(!cn1WindowOpen);
    gtk_widget_destroy(cn1Window);
    puts("GTK_TITLEBAR_OK");
    return 0;
}
