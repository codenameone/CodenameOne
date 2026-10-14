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


/* Included by cn1_linux_window.c. GTK owns the header, decorations and controls.
 * The custom-title drawing area shows the Toolbar slice of the same back buffer
 * as the body. No independent Java component tree or synthetic window controls. */
static gboolean cn1TitleButton(GtkWidget* widget, GdkEventButton* event, gpointer data) {
    int x = cn1TitleEventX(widget, event->x), y = cn1TitleEventY(widget, event->y);
    gboolean drag = cn1TitleRegionCount >= 4;
    for (int i = 0; drag && i < cn1TitleRegionCount; i += 4) {
        int* r = cn1TitleRegions + i;
        gboolean inside = x >= r[0] && y >= r[1] && x - r[0] < r[2] && y - r[1] < r[3];
        if ((i == 0 && !inside) || (i > 0 && inside)) drag = FALSE;
    }
    // Propagation lets GtkWindow apply gtk-titlebar-{double,middle,right}-click
    // actions and compositor move tracking, on both X11 and Wayland.
    if (drag) return FALSE;
    if (event->type == GDK_2BUTTON_PRESS || event->type == GDK_3BUTTON_PRESS) return TRUE;
    return cn1OnButton(widget, event, data);
}
static void cn1TitleAllocated(GtkWidget* widget, GtkAllocation* allocation, gpointer data) {
    (void)widget; (void)allocation; (void)data;
    if (!cn1TitleHeader) return;
    int oldHeight = cn1TitleHeight, oldX = cn1TitleX, oldY = cn1TitleY;
    cn1TitleHeight = gtk_widget_get_allocated_height(cn1TitleHeader);
    gtk_widget_translate_coordinates(cn1TitleCanvas, cn1TitleHeader, 0, 0, &cn1TitleX, &cn1TitleY);
    if (oldHeight != cn1TitleHeight || oldX != cn1TitleX || oldY != cn1TitleY) {
        GdkEventConfigure size = {0};
        size.width = gtk_widget_get_allocated_width(cn1DrawingArea);
        size.height = gtk_widget_get_allocated_height(cn1DrawingArea);
        cn1OnConfigure(cn1DrawingArea, &size, NULL);
        // Width and total height can stay the same when the OS moves its buttons.
        cn1LinuxPushEvent(CN1_EVENT_SIZE_CHANGED, cn1DisplayWidth, cn1DisplayHeight, 0);
    }
}
static void cn1TitleEnable(gboolean enabled) {
    if (!cn1Window || (!!cn1TitleHeader == !!enabled)) return;
    gboolean visible = gtk_widget_get_visible(cn1Window);
    // GTK requires titlebar changes before mapping for some window managers.
    if (visible) gtk_widget_hide(cn1Window);
    if (gtk_widget_get_realized(cn1Window)) gtk_widget_unrealize(cn1Window);
    if (enabled) {
        cn1TitleHeader = gtk_header_bar_new();
        gtk_header_bar_set_show_close_button(GTK_HEADER_BAR(cn1TitleHeader), TRUE);
        gtk_header_bar_set_has_subtitle(GTK_HEADER_BAR(cn1TitleHeader), FALSE);
        // Do not override decoration-layout: GTK follows gtk-decoration-layout,
        // including desktops that put controls on the left or omit minimize.
        cn1TitleCanvas = gtk_drawing_area_new();
        gtk_widget_set_hexpand(cn1TitleCanvas, TRUE);
        gtk_widget_set_size_request(cn1TitleCanvas, 1, 1);
        gtk_widget_set_can_focus(cn1TitleCanvas, TRUE);
        gtk_widget_set_events(cn1TitleCanvas, gtk_widget_get_events(cn1DrawingArea));
        GtkWidget* overlay = gtk_overlay_new();
        gtk_container_add(GTK_CONTAINER(overlay), cn1TitleCanvas);
        cn1TitleFixed = gtk_fixed_new();
        gtk_overlay_add_overlay(GTK_OVERLAY(overlay), cn1TitleFixed);
        gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(overlay), cn1TitleFixed, TRUE);
        cn1TitleAccessibilityFixed = gtk_fixed_new();
        gtk_widget_set_opacity(cn1TitleAccessibilityFixed, 0.01);
        gtk_overlay_add_overlay(GTK_OVERLAY(overlay), cn1TitleAccessibilityFixed);
        gtk_overlay_set_overlay_pass_through(GTK_OVERLAY(overlay), cn1TitleAccessibilityFixed, TRUE);
        gtk_header_bar_set_custom_title(GTK_HEADER_BAR(cn1TitleHeader), overlay);
        gtk_widget_set_valign(overlay, GTK_ALIGN_FILL);
        gtk_widget_set_vexpand(overlay, TRUE);
        gtk_window_set_titlebar(GTK_WINDOW(cn1Window), cn1TitleHeader);
        g_signal_connect(cn1TitleCanvas, "draw", G_CALLBACK(cn1OnDraw), NULL);
        g_signal_connect(cn1TitleCanvas, "button-press-event", G_CALLBACK(cn1TitleButton), NULL);
        g_signal_connect(cn1TitleCanvas, "button-release-event", G_CALLBACK(cn1TitleButton), NULL);
        g_signal_connect(cn1TitleCanvas, "motion-notify-event", G_CALLBACK(cn1OnMotion), NULL);
        g_signal_connect(cn1TitleCanvas, "leave-notify-event", G_CALLBACK(cn1OnLeave), NULL);
        g_signal_connect(cn1TitleCanvas, "touch-event", G_CALLBACK(cn1OnTouch), NULL);
        g_signal_connect(cn1TitleCanvas, "scroll-event", G_CALLBACK(cn1OnScroll), NULL);
        g_signal_connect(cn1TitleCanvas, "event", G_CALLBACK(cn1OnGenericEvent), NULL);
        g_signal_connect(cn1TitleCanvas, "size-allocate", G_CALLBACK(cn1TitleAllocated), NULL);
        gtk_widget_show_all(cn1TitleHeader);
        int minimum, natural;
        gtk_widget_get_preferred_height(cn1TitleHeader, &minimum, &natural);
        cn1TitleHeight = natural;
    } else {
        // Destroying a titlebar must not destroy native peers still owned by Java.
        GList* children = gtk_container_get_children(GTK_CONTAINER(cn1TitleFixed));
        for (GList* child = children; child; child = child->next) {
            GtkWidget* peer = GTK_WIDGET(child->data);
            int x, y;
            gtk_container_child_get(GTK_CONTAINER(cn1TitleFixed), peer, "x", &x, "y", &y, NULL);
            g_object_ref(peer);
            gtk_container_remove(GTK_CONTAINER(cn1TitleFixed), peer);
            gtk_fixed_put(GTK_FIXED(cn1Fixed), peer, x + cn1TitleX, y + cn1TitleY);
            g_object_unref(peer);
        }
        g_list_free(children);
        // Accessibility entries keep widget pointers until the next tree update.
        // Keep those widgets alive too when changing the native title mode.
        children = gtk_container_get_children(GTK_CONTAINER(cn1TitleAccessibilityFixed));
        for (GList* child = children; child; child = child->next) {
            GtkWidget* peer = GTK_WIDGET(child->data);
            int x, y;
            gtk_container_child_get(GTK_CONTAINER(cn1TitleAccessibilityFixed), peer, "x", &x, "y", &y, NULL);
            g_object_ref(peer);
            gtk_container_remove(GTK_CONTAINER(cn1TitleAccessibilityFixed), peer);
            if (cn1AccessibilityFixed)
                gtk_fixed_put(GTK_FIXED(cn1AccessibilityFixed), peer, x + cn1TitleX, y + cn1TitleY);
            g_object_unref(peer);
        }
        g_list_free(children);
        cn1TitleHeight = cn1TitleX = cn1TitleY = 0;
        cn1TitleAccessibilityFixed = NULL;
        cn1TitleHeader = cn1TitleCanvas = cn1TitleFixed = NULL;
        gtk_window_set_titlebar(GTK_WINDOW(cn1Window), NULL);
    }
    if (visible) gtk_widget_show(cn1Window);
}
static void cn1TitleMetrics(void* arg) {
    int* out = (int*)arg;
    if (!cn1Window) return;
    cn1TitleEnable(TRUE);
    GtkAllocation header, content;
    gtk_widget_get_allocation(cn1TitleHeader, &header);
    gtk_widget_get_allocation(cn1TitleCanvas, &content);
    int minimum, natural;
    gtk_widget_get_preferred_height(cn1TitleHeader, &minimum, &natural);
    if (header.width > 1 && content.width > 1) {
        out[0] = MAX(0, cn1TitleX);
        out[1] = MAX(0, header.width - cn1TitleX - content.width);
        out[2] = MAX(0, cn1TitleY);
        out[3] = MAX(0, header.height - cn1TitleY - content.height);
    }
    out[4] = natural;
}
typedef struct { int count; int* regions; } CN1TitleUpdate;
static gboolean cn1TitleUpdate(gpointer data) {
    CN1TitleUpdate* update = (CN1TitleUpdate*)data;
    free(cn1TitleRegions);
    cn1TitleRegions = update->regions;
    cn1TitleRegionCount = update->count;
    cn1TitleEnable(update->count >= 4);
    if (cn1TitleHeader) gtk_widget_set_size_request(cn1TitleHeader, -1, update->regions[3]);
    free(update);
    return G_SOURCE_REMOVE;
}
JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowTitleBarInsets___int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    if (array == JAVA_NULL || ((JAVA_ARRAY)array)->length < 5) return;
    int result[5] = {0};
    cn1LinuxRunOnMainAndWait(cn1TitleMetrics, result);
    memcpy(CN1_ARRAY_DATA(array), result, sizeof(result));
}
JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowTitleBarRegions___int_1ARRAY(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT array) {
    int count = array == JAVA_NULL ? 0 : ((JAVA_ARRAY)array)->length;
    if (count % 4 != 0) return;
    CN1TitleUpdate* update = (CN1TitleUpdate*)calloc(1, sizeof(CN1TitleUpdate));
    if (!update) return;
    update->count = count;
    update->regions = count ? (int*)malloc((size_t)count * sizeof(int)) : NULL;
    if (count && !update->regions) { free(update); return; }
    if (count) memcpy(update->regions, CN1_ARRAY_DATA(array), (size_t)count * sizeof(int));
    gdk_threads_add_idle(cn1TitleUpdate, update);
}
