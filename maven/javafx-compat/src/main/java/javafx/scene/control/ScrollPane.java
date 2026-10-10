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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.ui.Component;
import com.codename1.ui.Display;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// A view onto content larger than itself, moved by scrolling.
///
/// #### How it scrolls
///
/// There is no scrolling Codename One container behind this control. The
/// content stays an ordinary node of the scene graph: it is the child of
/// a viewport region that is the first child of the scroll pane, JavaFX
/// layout sizes it, and scrolling sets its `layoutX` and `layoutY` to
/// minus the scroll offset. The viewport's peer clips what it holds to
/// its bounds, and the viewport does not pick content outside them. Events
/// therefore reach the content through the usual dispatch chain, `lookup`
/// finds it, and its bounds in the scene are true at every scroll
/// position; a scrolling native container would have moved the peers
/// behind the scene graph's back and left all three wrong.
///
/// `hvalue` and `vvalue` run from `hmin` to `hmax` and `vmin` to `vmax`
/// (0 to 1 unless changed) and map linearly onto the distance the content
/// can travel. The wheel scrolls, dragging a scroll bar thumb scrolls, and
/// dragging the content pans when `pannable` is set, and always on a
/// device that is not a desktop, where there is nothing else to scroll
/// with.
///
/// #### Differences from JavaFX
///
/// The scroll bars are thin thumbs drawn over the edge of the viewport;
/// they take no room, so the viewport is always the whole inside of the
/// pane. A policy decides only whether a thumb shows. `viewportBounds`
/// holds minus the scroll offset as its minimum and the viewport size.
///
/// #### Styling
///
/// Through `cn1ApplyStyle`, besides the region names: `-fx-fit-to-width`,
/// `-fx-fit-to-height`, `-fx-pannable` (`Boolean`); `-fx-hbar-policy`,
/// `-fx-vbar-policy` (`ScrollPane.ScrollBarPolicy`, or `"never"`,
/// `"always"`, `"as-needed"`).
public class ScrollPane extends Control {

    /// When a scroll bar shows.
    public enum ScrollBarPolicy {
        /// Never.
        NEVER,
        /// Always.
        ALWAYS,
        /// Only while the content is larger than the viewport.
        AS_NEEDED
    }

    private static final int SCROLL = Dirty.USER;
    private static final int CONTENT = Dirty.USER << 1;
    private static final double BAR = 6;
    private static final double BAR_GAP = 2;
    private static final double MIN_THUMB = 20;

    private final ObjectProperty<Node> content = new FxObject<Node>(this, "content", null,
            CONTENT | Dirty.LAYOUT);
    private final ObjectProperty<ScrollBarPolicy> hbarPolicy = new FxObject<ScrollBarPolicy>(this, "hbarPolicy",
            ScrollBarPolicy.AS_NEEDED, Dirty.LAYOUT);
    private final ObjectProperty<ScrollBarPolicy> vbarPolicy = new FxObject<ScrollBarPolicy>(this, "vbarPolicy",
            ScrollBarPolicy.AS_NEEDED, Dirty.LAYOUT);
    private final BooleanProperty fitToWidth = new FxBoolean(this, "fitToWidth", false, Dirty.LAYOUT);
    private final BooleanProperty fitToHeight = new FxBoolean(this, "fitToHeight", false, Dirty.LAYOUT);
    private final BooleanProperty pannable = new FxBoolean(this, "pannable", false, Dirty.NONE);
    private final DoubleProperty hvalue = new FxDouble(this, "hvalue", 0, SCROLL);
    private final DoubleProperty vvalue = new FxDouble(this, "vvalue", 0, SCROLL);
    private final DoubleProperty hmin = new FxDouble(this, "hmin", 0, SCROLL);
    private final DoubleProperty vmin = new FxDouble(this, "vmin", 0, SCROLL);
    private final DoubleProperty hmax = new FxDouble(this, "hmax", 1, SCROLL);
    private final DoubleProperty vmax = new FxDouble(this, "vmax", 1, SCROLL);
    private final DoubleProperty prefViewportWidth = new FxDouble(this, "prefViewportWidth", 0, Dirty.LAYOUT);
    private final DoubleProperty prefViewportHeight = new FxDouble(this, "prefViewportHeight", 0, Dirty.LAYOUT);
    private final DoubleProperty minViewportWidth = new FxDouble(this, "minViewportWidth", 0, Dirty.LAYOUT);
    private final DoubleProperty minViewportHeight = new FxDouble(this, "minViewportHeight", 0, Dirty.LAYOUT);
    private final ObjectProperty<Bounds> viewportBounds = new SimpleObjectProperty<Bounds>(this, "viewportBounds",
            new BoundingBox(0, 0, 0, 0));

    private final Viewport viewport = new Viewport();
    private final Region hbar = thumb();
    private final Region vbar = thumb();
    private double contentWidth;
    private double contentHeight;
    private double pressX;
    private double pressY;
    private double pressH;
    private double pressV;
    private boolean panning;

    /// Creates a scroll pane without content.
    public ScrollPane() {
        init();
    }

    /// Creates a scroll pane showing a node.
    public ScrollPane(Node content) {
        init();
        setContent(content);
    }

    private static Region thumb() {
        Region r = new Region();
        r.setManaged(false);
        r.setVisible(false);
        r.setBackground(new Background(new BackgroundFill(Color.gray(0.35, 0.6), new CornerRadii(BAR / 2),
                Insets.EMPTY)));
        return r;
    }

    private void init() {
        getStyleClass().add("scroll-pane");
        setFocusTraversable(false);
        cn1Children().addAll(viewport, vbar, hbar);
        addEventHandler(ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (scrollBy(-event.getDeltaX(), -event.getDeltaY())) {
                    event.consume();
                }
            }
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                rememberPress(event);
                panning = isPannable() || (Display.isInitialized() && !Display.getInstance().isDesktop());
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (panning && panTo(pressX - event.getSceneX(), pressY - event.getSceneY())) {
                    event.consume();
                }
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                panning = false;
            }
        });
        dragThumb(hbar, true);
        dragThumb(vbar, false);
    }

    private void rememberPress(MouseEvent event) {
        pressX = event.getSceneX();
        pressY = event.getSceneY();
        pressH = getHvalue();
        pressV = getVvalue();
    }

    private void dragThumb(final Region bar, final boolean horizontal) {
        bar.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                rememberPress(event);
                panning = false;
                event.consume();
            }
        });
        bar.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (horizontal) {
                    double track = viewport.getWidth() - 2 * BAR_GAP - bar.getWidth();
                    if (track > 0) {
                        setHvalue(bounded(pressH + (event.getSceneX() - pressX) / track * (getHmax() - getHmin()),
                                getHmin(), getHmax()));
                    }
                } else {
                    double track = viewport.getHeight() - 2 * BAR_GAP - bar.getHeight();
                    if (track > 0) {
                        setVvalue(bounded(pressV + (event.getSceneY() - pressY) / track * (getVmax() - getVmin()),
                                getVmin(), getVmax()));
                    }
                }
                event.consume();
            }
        });
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & CONTENT) != 0) {
            viewport.show(getContent());
        }
        if ((what & SCROLL) != 0) {
            placeContent();
        }
        super.cn1Invalidated(what);
    }

    // ------------------------------------------------------------ scrolling

    private static double bounded(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static double offset(double value, double min, double max, double extent) {
        double range = max - min;
        if (!(extent > 0) || !(range > 0)) {
            return 0;
        }
        return bounded((value - min) / range, 0, 1) * extent;
    }

    private static double valueAt(double offset, double min, double max, double extent) {
        if (!(extent > 0)) {
            return min;
        }
        return min + bounded(offset / extent, 0, 1) * (max - min);
    }

    /// Scrolls by a distance in logical pixels; answers whether anything
    /// moved.
    private boolean scrollBy(double dx, double dy) {
        double extentX = contentWidth - viewport.getWidth();
        double extentY = contentHeight - viewport.getHeight();
        double oldH = getHvalue();
        double oldV = getVvalue();
        if (extentX > 0 && dx != 0 && !hvalue.isBound()) {
            setHvalue(valueAt(offset(oldH, getHmin(), getHmax(), extentX) + dx, getHmin(), getHmax(), extentX));
        }
        if (extentY > 0 && dy != 0 && !vvalue.isBound()) {
            setVvalue(valueAt(offset(oldV, getVmin(), getVmax(), extentY) + dy, getVmin(), getVmax(), extentY));
        }
        return Double.compare(oldH, getHvalue()) != 0 || Double.compare(oldV, getVvalue()) != 0;
    }

    /// Scrolls to where the content was at the press, moved by a
    /// distance; answers whether the content can move at all.
    private boolean panTo(double dx, double dy) {
        double extentX = contentWidth - viewport.getWidth();
        double extentY = contentHeight - viewport.getHeight();
        if (extentX > 0 && !hvalue.isBound()) {
            setHvalue(valueAt(offset(pressH, getHmin(), getHmax(), extentX) + dx, getHmin(), getHmax(), extentX));
        }
        if (extentY > 0 && !vvalue.isBound()) {
            setVvalue(valueAt(offset(pressV, getVmin(), getVmax(), extentY) + dy, getVmin(), getVmax(), extentY));
        }
        return extentX > 0 || extentY > 0;
    }

    private void placeContent() {
        double vw = viewport.getWidth();
        double vh = viewport.getHeight();
        double extentX = contentWidth - vw;
        double extentY = contentHeight - vh;
        double ox = snapPositionX(offset(getHvalue(), getHmin(), getHmax(), extentX));
        double oy = snapPositionY(offset(getVvalue(), getVmin(), getVmax(), extentY));
        Node c = getContent();
        if (c != null && c.getParent() == viewport) {
            Bounds lb = c.getLayoutBounds();
            c.setLayoutX(-ox - lb.getMinX());
            c.setLayoutY(-oy - lb.getMinY());
        }
        Bounds old = viewportBounds.get();
        if (old == null || old.getMinX() != -ox || old.getMinY() != -oy || old.getWidth() != vw
                || old.getHeight() != vh) {
            viewportBounds.set(new BoundingBox(-ox, -oy, vw, vh));
        }
        placeThumb(hbar, true, getHbarPolicy(), extentX, vw, vh, contentWidth, ox);
        placeThumb(vbar, false, getVbarPolicy(), extentY, vh, vw, contentHeight, oy);
    }

    private void placeThumb(Region bar, boolean horizontal, ScrollBarPolicy policy, double extent, double along,
            double across, double contentLength, double scrolled) {
        boolean shown = policy == ScrollBarPolicy.ALWAYS || (policy != ScrollBarPolicy.NEVER && extent > 0.5);
        double track = along - 2 * BAR_GAP;
        if (!shown || !(track > 0)) {
            bar.setVisible(false);
            return;
        }
        double length = track;
        double at = 0;
        if (extent > 0 && contentLength > 0) {
            length = Math.min(track, Math.max(MIN_THUMB, track * along / contentLength));
            at = (track - length) * scrolled / extent;
        }
        double x = viewport.getLayoutX();
        double y = viewport.getLayoutY();
        if (horizontal) {
            bar.resizeRelocate(x + BAR_GAP + at, y + across - BAR - BAR_GAP, length, BAR);
        } else {
            bar.resizeRelocate(x + across - BAR - BAR_GAP, y + BAR_GAP + at, BAR, length);
        }
        bar.setVisible(true);
    }

    // --------------------------------------------------------------- layout

    private static double size(double available, double min, double pref, double max, boolean fit) {
        double wanted = fit ? available : pref;
        return Math.max(min, Math.min(wanted, max));
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        viewport.resizeRelocate(in.getLeft(), in.getTop(), w, h);
        Node c = getContent();
        contentWidth = 0;
        contentHeight = 0;
        if (c != null && c.getParent() == viewport) {
            if (c.isResizable()) {
                double cw;
                double ch;
                if (c.getContentBias() == Orientation.VERTICAL) {
                    ch = size(h, c.minHeight(-1), c.prefHeight(-1), c.maxHeight(-1), isFitToHeight());
                    cw = size(w, c.minWidth(ch), c.prefWidth(ch), c.maxWidth(ch), isFitToWidth());
                } else {
                    cw = size(w, c.minWidth(-1), c.prefWidth(-1), c.maxWidth(-1), isFitToWidth());
                    double bias = c.getContentBias() == Orientation.HORIZONTAL ? cw : -1;
                    ch = size(h, c.minHeight(bias), c.prefHeight(bias), c.maxHeight(bias), isFitToHeight());
                }
                c.resize(snapSizeX(cw), snapSizeY(ch));
            }
            Bounds lb = c.getLayoutBounds();
            contentWidth = lb.getWidth();
            contentHeight = lb.getHeight();
        }
        placeContent();
    }

    private double contentPref(boolean width) {
        Node c = getContent();
        if (c == null) {
            return 0;
        }
        if (!c.isResizable()) {
            Bounds lb = c.getLayoutBounds();
            return width ? lb.getWidth() : lb.getHeight();
        }
        return width ? c.prefWidth(-1) : c.prefHeight(-1);
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        double inner = getPrefViewportWidth() > 0 ? getPrefViewportWidth() : contentPref(true);
        return in.getLeft() + Math.max(inner, getMinViewportWidth()) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double inner = getPrefViewportHeight() > 0 ? getPrefViewportHeight() : contentPref(false);
        return in.getTop() + Math.max(inner, getMinViewportHeight()) + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + Math.max(0, getMinViewportWidth()) + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + Math.max(0, getMinViewportHeight()) + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /// The region that holds the content and clips it.
    private static final class Viewport extends Region {

        void show(Node node) {
            if (node == null) {
                cn1Children().clear();
            } else if (cn1Children().size() != 1 || cn1Children().get(0) != node) {
                cn1Children().setAll(node);
            }
        }

        @Override
        protected void layoutChildren() {
            // The scroll pane sizes and places the content.
        }

        @Override
        protected Node cn1PickLocal(double x, double y) {
            return contains(x, y) ? super.cn1PickLocal(x, y) : null;
        }
    }

    // ----------------------------------------------------------- properties

    /// Returns the node being scrolled, or `null`.
    public final Node getContent() {
        return content.get();
    }

    /// Sets the node being scrolled.
    public final void setContent(Node value) {
        content.set(value);
    }

    /// The node being scrolled.
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    /// Returns when the horizontal scroll bar shows.
    public final ScrollBarPolicy getHbarPolicy() {
        ScrollBarPolicy p = hbarPolicy.get();
        return p == null ? ScrollBarPolicy.AS_NEEDED : p;
    }

    /// Sets when the horizontal scroll bar shows.
    public final void setHbarPolicy(ScrollBarPolicy value) {
        hbarPolicy.set(value);
    }

    /// When the horizontal scroll bar shows.
    public final ObjectProperty<ScrollBarPolicy> hbarPolicyProperty() {
        return hbarPolicy;
    }

    /// Returns when the vertical scroll bar shows.
    public final ScrollBarPolicy getVbarPolicy() {
        ScrollBarPolicy p = vbarPolicy.get();
        return p == null ? ScrollBarPolicy.AS_NEEDED : p;
    }

    /// Sets when the vertical scroll bar shows.
    public final void setVbarPolicy(ScrollBarPolicy value) {
        vbarPolicy.set(value);
    }

    /// When the vertical scroll bar shows.
    public final ObjectProperty<ScrollBarPolicy> vbarPolicyProperty() {
        return vbarPolicy;
    }

    /// Returns whether resizable content is made as wide as the viewport.
    public final boolean isFitToWidth() {
        return fitToWidth.get();
    }

    /// Sets whether resizable content is made as wide as the viewport.
    public final void setFitToWidth(boolean value) {
        fitToWidth.set(value);
    }

    /// Whether resizable content is made as wide as the viewport.
    public final BooleanProperty fitToWidthProperty() {
        return fitToWidth;
    }

    /// Returns whether resizable content is made as tall as the viewport.
    public final boolean isFitToHeight() {
        return fitToHeight.get();
    }

    /// Sets whether resizable content is made as tall as the viewport.
    public final void setFitToHeight(boolean value) {
        fitToHeight.set(value);
    }

    /// Whether resizable content is made as tall as the viewport.
    public final BooleanProperty fitToHeightProperty() {
        return fitToHeight;
    }

    /// Returns whether dragging the content with the pointer scrolls.
    public final boolean isPannable() {
        return pannable.get();
    }

    /// Sets whether dragging the content with the pointer scrolls.
    public final void setPannable(boolean value) {
        pannable.set(value);
    }

    /// Whether dragging the content with the pointer scrolls.
    public final BooleanProperty pannableProperty() {
        return pannable;
    }

    /// Returns the horizontal scroll position.
    public final double getHvalue() {
        return hvalue.get();
    }

    /// Sets the horizontal scroll position.
    public final void setHvalue(double value) {
        hvalue.set(value);
    }

    /// The horizontal scroll position, from `hmin` to `hmax`.
    public final DoubleProperty hvalueProperty() {
        return hvalue;
    }

    /// Returns the vertical scroll position.
    public final double getVvalue() {
        return vvalue.get();
    }

    /// Sets the vertical scroll position.
    public final void setVvalue(double value) {
        vvalue.set(value);
    }

    /// The vertical scroll position, from `vmin` to `vmax`.
    public final DoubleProperty vvalueProperty() {
        return vvalue;
    }

    /// Returns the `hvalue` of the content's left edge.
    public final double getHmin() {
        return hmin.get();
    }

    /// Sets the `hvalue` of the content's left edge.
    public final void setHmin(double value) {
        hmin.set(value);
    }

    /// The `hvalue` of the content's left edge.
    public final DoubleProperty hminProperty() {
        return hmin;
    }

    /// Returns the `vvalue` of the content's top edge.
    public final double getVmin() {
        return vmin.get();
    }

    /// Sets the `vvalue` of the content's top edge.
    public final void setVmin(double value) {
        vmin.set(value);
    }

    /// The `vvalue` of the content's top edge.
    public final DoubleProperty vminProperty() {
        return vmin;
    }

    /// Returns the `hvalue` of the content's right edge.
    public final double getHmax() {
        return hmax.get();
    }

    /// Sets the `hvalue` of the content's right edge.
    public final void setHmax(double value) {
        hmax.set(value);
    }

    /// The `hvalue` of the content's right edge.
    public final DoubleProperty hmaxProperty() {
        return hmax;
    }

    /// Returns the `vvalue` of the content's bottom edge.
    public final double getVmax() {
        return vmax.get();
    }

    /// Sets the `vvalue` of the content's bottom edge.
    public final void setVmax(double value) {
        vmax.set(value);
    }

    /// The `vvalue` of the content's bottom edge.
    public final DoubleProperty vmaxProperty() {
        return vmax;
    }

    /// Returns the preferred width of the viewport; 0 for the content's.
    public final double getPrefViewportWidth() {
        return prefViewportWidth.get();
    }

    /// Sets the preferred width of the viewport.
    public final void setPrefViewportWidth(double value) {
        prefViewportWidth.set(value);
    }

    /// The preferred width of the viewport.
    public final DoubleProperty prefViewportWidthProperty() {
        return prefViewportWidth;
    }

    /// Returns the preferred height of the viewport; 0 for the content's.
    public final double getPrefViewportHeight() {
        return prefViewportHeight.get();
    }

    /// Sets the preferred height of the viewport.
    public final void setPrefViewportHeight(double value) {
        prefViewportHeight.set(value);
    }

    /// The preferred height of the viewport.
    public final DoubleProperty prefViewportHeightProperty() {
        return prefViewportHeight;
    }

    /// Returns the minimum width of the viewport.
    public final double getMinViewportWidth() {
        return minViewportWidth.get();
    }

    /// Sets the minimum width of the viewport.
    public final void setMinViewportWidth(double value) {
        minViewportWidth.set(value);
    }

    /// The minimum width of the viewport.
    public final DoubleProperty minViewportWidthProperty() {
        return minViewportWidth;
    }

    /// Returns the minimum height of the viewport.
    public final double getMinViewportHeight() {
        return minViewportHeight.get();
    }

    /// Sets the minimum height of the viewport.
    public final void setMinViewportHeight(double value) {
        minViewportHeight.set(value);
    }

    /// The minimum height of the viewport.
    public final DoubleProperty minViewportHeightProperty() {
        return minViewportHeight;
    }

    /// Returns the bounds of the viewport: its minimum is minus the
    /// scroll offset, its size the visible area.
    public final Bounds getViewportBounds() {
        return viewportBounds.get();
    }

    /// Sets the bounds of the viewport. Layout overwrites the value.
    public final void setViewportBounds(Bounds value) {
        viewportBounds.set(value);
    }

    /// The bounds of the viewport.
    public final ObjectProperty<Bounds> viewportBoundsProperty() {
        return viewportBounds;
    }

    // -------------------------------------------------------------- styling

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-fit-to-width".equals(property)) {
            return Boolean.valueOf(isFitToWidth());
        } else if ("-fx-fit-to-height".equals(property)) {
            return Boolean.valueOf(isFitToHeight());
        } else if ("-fx-pannable".equals(property)) {
            return Boolean.valueOf(isPannable());
        } else if ("-fx-hbar-policy".equals(property)) {
            return getHbarPolicy();
        } else if ("-fx-vbar-policy".equals(property)) {
            return getVbarPolicy();
        }
        return super.cn1StyleValue(property);
    }

    private static ScrollBarPolicy policy(Object value) {
        if (value instanceof ScrollBarPolicy) {
            return (ScrollBarPolicy) value;
        } else if (value instanceof String) {
            String wanted = ((String) value).trim().replace('-', '_');
            ScrollBarPolicy[] all = ScrollBarPolicy.values();
            for (int i = 0; i < all.length; i++) {
                if (all[i].name().equalsIgnoreCase(wanted)) {
                    return all[i];
                }
            }
        }
        return null;
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-hbar-policy".equals(property) || "-fx-vbar-policy".equals(property)) {
            ScrollBarPolicy p = policy(value);
            if (p == null) {
                return false;
            }
            if ("-fx-hbar-policy".equals(property)) {
                setHbarPolicy(p);
            } else {
                setVbarPolicy(p);
            }
            return true;
        }
        boolean fitWidth = "-fx-fit-to-width".equals(property);
        boolean fitHeight = "-fx-fit-to-height".equals(property);
        if (fitWidth || fitHeight || "-fx-pannable".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            boolean on = ((Boolean) value).booleanValue();
            if (fitWidth) {
                setFitToWidth(on);
            } else if (fitHeight) {
                setFitToHeight(on);
            } else {
                setPannable(on);
            }
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
