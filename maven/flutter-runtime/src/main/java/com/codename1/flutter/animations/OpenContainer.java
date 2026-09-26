/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.animations;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.SizedBox;

import dart.core.Duration;

/**
 * A container that expands (a "container transform") from a closed state to a
 * full page — the {@code animations} package's {@code OpenContainer}. The
 * {@code closedBuilder} paints the resting state and {@code openBuilder} the
 * opened page; each is a closure {@code (context, action)} where {@code action}
 * opens/closes the container. This pass renders the closed state via the
 * closedBuilder (falling back to an empty box); the expand transition is
 * deferred.
 */
public class OpenContainer<T> extends StatelessWidget {

    private Object onClosed;
    private CloseContainerBuilder closedBuilder;
    private CloseContainerBuilder openBuilder;
    private boolean tappable = true;
    private Duration transitionDuration;
    private Color closedColor;
    private Color openColor;
    private Color middleColor;
    private Double closedElevation;
    private Double openElevation;
    private Object closedShape;
    private Object openShape;

    public void onClosed(Object v) {
        this.onClosed = v;
    }

    public void closedBuilder(CloseContainerBuilder v) {
        this.closedBuilder = v;
    }

    public void openBuilder(CloseContainerBuilder v) {
        this.openBuilder = v;
    }

    public void tappable(boolean v) {
        this.tappable = v;
    }

    public void transitionDuration(Duration v) {
        this.transitionDuration = v;
    }

    public void transitionType(Object v) {
    }

    public void closedColor(Color v) {
        this.closedColor = v;
    }

    public void openColor(Color v) {
        this.openColor = v;
    }

    public void middleColor(Color v) {
        this.middleColor = v;
    }

    public void closedElevation(double v) {
        this.closedElevation = v;
    }

    public void openElevation(double v) {
        this.openElevation = v;
    }

    public void closedShape(Object v) {
        this.closedShape = v;
    }

    public void openShape(Object v) {
        this.openShape = v;
    }

    public void routeSettings(String v) {
    }

    public void useRootNavigator(boolean v) {
    }

    public Object getClosedBuilder() {
        return closedBuilder;
    }

    /**
     * The closed state: the {@code closedBuilder}'s widget on a Material surface, tappable
     * to open.
     *
     * <p>It rendered NOTHING before — an empty box — which took the whole motion demo with
     * it, eighteen containers' worth of it. The transform itself (the closed card growing
     * into the page) is a compositing effect we do not have; opening pushes the built page
     * as a route instead, so the demo is navigable and shows both of its states even though
     * the growth between them is a cut rather than a morph.</p>
     */
    @Override
    public Widget build(final BuildContext context) {
        if (closedBuilder == null) {
            return new SizedBox();
        }
        Widget closed = closedBuilder.call(context, new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                open(context);
            }
        });
        com.codename1.flutter.material.Material surface =
                new com.codename1.flutter.material.Material();
        if (closedColor != null) {
            surface.color(closedColor);
        }
        if (closedElevation != null) {
            surface.elevation(closedElevation.doubleValue());
        }
        if (closedShape != null) {
            surface.shape(closedShape);
        }
        surface.clipBehavior(com.codename1.flutter.Clip.antiAlias);
        if (!tappable) {
            surface.child(closed);
            return surface;
        }
        com.codename1.flutter.material.InkWell tap =
                new com.codename1.flutter.material.InkWell();
        tap.child(closed);
        tap.onTap(new dart.runtime.Funcs.VoidFunc0() {
            @Override
            public void call() {
                open(context);
            }
        });
        surface.child(tap);
        return surface;
    }

    /// Distinguishes one open container's surface from every other one on screen.
    private static int surfaceSerial;

    /**
     * Gives this container's closed surface a name the transition can find it by, and
     * returns that name (null when it has no component of its own to grow from).
     */
    private static String nameClosedSurface(BuildContext context) {
        if (!(context instanceof com.codename1.flutter.Element)) {
            return null;
        }
        com.codename1.flutter.RenderElement r = com.codename1.flutter.RenderElement
                .findRenderElement((com.codename1.flutter.Element) context);
        com.codename1.ui.Component c = r == null ? null : r.component();
        if (c == null) {
            return null;
        }
        String name = "cn1-open-container-" + (++surfaceSerial);
        c.setName(name);
        return name;
    }

    /** Pushes the opened page; closing it pops back and reports through {@code onClosed}. */
    private void open(BuildContext context) {
        if (openBuilder == null) {
            return;
        }
        // The route carries the container transform's identity and duration, so the
        // Navigator animates it as an expanding surface rather than as a page push.
        // Named at TAP time, not at build time. The name has to identify the one surface
        // the user actually touched -- a mail list is a column of these -- and a widget is
        // rebuilt often enough that a name assigned during build belongs to whichever
        // instance built last. The element under this context is stable and is the one in
        // front of the user right now.
        final String source = nameClosedSurface(context);
        com.codename1.flutter.navigation.MaterialPageRoute<T> route =
                new com.codename1.flutter.navigation.MaterialPageRoute<T>() {
                    @Override
                    public boolean isContainerTransform() {
                        return true;
                    }

                    @Override
                    public String containerTransformSource() {
                        return source;
                    }

                    @Override
                    public int transitionMillis() {
                        return transitionDuration == null
                                ? -1 : (int) transitionDuration.inMilliseconds();
                    }
                };
        route.builder(new dart.runtime.Funcs.Func1<BuildContext, Widget>() {
            @Override
            public Widget call(BuildContext routeContext) {
                Widget page = openBuilder.call(routeContext, new dart.runtime.Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        close(routeContext);
                    }
                });
                if (openColor == null) {
                    return page;
                }
                com.codename1.flutter.material.Material surface =
                        new com.codename1.flutter.material.Material();
                surface.color(openColor);
                if (openElevation != null) {
                    surface.elevation(openElevation.doubleValue());
                }
                surface.child(page);
                return surface;
            }
        });
        com.codename1.flutter.navigation.Navigator.push(context, route);
    }

    @SuppressWarnings("unchecked")
    private void close(BuildContext context) {
        com.codename1.flutter.navigation.Navigator.pop(context);
        if (onClosed instanceof dart.runtime.Funcs.VoidFunc1) {
            ((dart.runtime.Funcs.VoidFunc1<Object>) onClosed).call(null);
        } else if (onClosed instanceof dart.runtime.Funcs.VoidFunc0) {
            ((dart.runtime.Funcs.VoidFunc0) onClosed).call();
        }
    }
}
