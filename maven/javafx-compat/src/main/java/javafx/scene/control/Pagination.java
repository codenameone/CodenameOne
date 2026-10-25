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

import com.codename1.ui.Component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/// Pages of content with a row of page numbers below to move among them.
///
/// The page factory is asked for the content of the current page whenever
/// that page changes; a factory that answers `null` for an index leaves
/// the page that was showing and the index it had. Below the page are a
/// button to the previous page, one button per page number -- at most
/// [#getMaxPageIndicatorCount()] of them, the group that holds the
/// current page -- a button to the next page, and `current/count`.
///
/// The page changes at once: nothing slides. The style class
/// [#STYLE_CLASS_BULLET] is accepted and the indicators stay numbered.
public class Pagination extends Control {

    /// The page count of a pagination that does not know how many pages
    /// there are.
    public static final int INDETERMINATE = Integer.MAX_VALUE;

    /// The style class that asks for bullets in place of page numbers.
    public static final String STYLE_CLASS_BULLET = "bullet";

    private final IntegerProperty pageCount = new SimpleIntegerProperty(this, "pageCount", INDETERMINATE);
    private final IntegerProperty currentPageIndex = new SimpleIntegerProperty(this, "currentPageIndex", 0);
    private final IntegerProperty maxPageIndicatorCount = new SimpleIntegerProperty(this, "maxPageIndicatorCount",
            10);
    private final ObjectProperty<Callback<Integer, Node>> pageFactory =
            new SimpleObjectProperty<Callback<Integer, Node>>(this, "pageFactory");
    private final StackPane page = new StackPane();
    private final HBox numbers = new HBox(4);
    private final Label place = new Label();
    private int shown = -1;
    private boolean moving;

    /// Creates a pagination of an unknown number of pages, at the first.
    public Pagination() {
        this(INDETERMINATE, 0);
    }

    /// Creates a pagination of a number of pages, at the first.
    public Pagination(int pageCount) {
        this(pageCount, 0);
    }

    /// Creates a pagination of a number of pages at a page.
    public Pagination(int pageCount, int pageIndex) {
        getStyleClass().add("pagination");
        setFocusTraversable(false);
        numbers.setAlignment(Pos.CENTER);
        numbers.getStyleClass().add("control-box");
        place.getStyleClass().add("page-information");
        VBox.setVgrow(page, Priority.ALWAYS);
        VBox below = new VBox(2, numbers, place);
        below.setAlignment(Pos.CENTER);
        below.setPadding(new Insets(6, 0, 6, 0));
        below.getStyleClass().add("pagination-control");
        VBox whole = new VBox(page, below);
        this.pageCount.addListener((observable, was, now) -> changed());
        this.currentPageIndex.addListener((observable, was, now) -> changed());
        this.maxPageIndicatorCount.addListener((observable, was, now) -> changed());
        this.pageFactory.addListener((observable, was, now) -> {
            shown = -1;
            changed();
        });
        setPageCount(pageCount);
        setCurrentPageIndex(pageIndex);
        cn1MadeOf(whole);
        changed();
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private void changed() {
        if (moving) {
            return;
        }
        moving = true;
        try {
            int count = Math.max(1, getPageCount());
            int at = Math.max(0, Math.min(count - 1, getCurrentPageIndex()));
            if (at != getCurrentPageIndex()) {
                currentPageIndex.set(at);
            }
            Callback<Integer, Node> factory = getPageFactory();
            if (at != shown && factory != null) {
                Node content = factory.call(Integer.valueOf(at));
                if (content == null && shown >= 0) {
                    // No such page: stay on the one that is showing.
                    at = shown;
                    currentPageIndex.set(at);
                } else {
                    shown = at;
                    if (content == null) {
                        page.getChildren().clear();
                    } else {
                        page.getChildren().setAll(content);
                    }
                }
            }
            indicators(at, count);
        } finally {
            moving = false;
        }
        requestLayout();
    }

    private void indicators(final int at, int count) {
        int per = Math.max(1, getMaxPageIndicatorCount());
        int first = at / per * per;
        int last = (int) Math.min((long) first + per, count);
        numbers.getChildren().clear();
        Button back = new Button("<");
        back.getStyleClass().add("left-arrow-button");
        back.setDisable(at <= 0);
        back.setOnAction(e -> setCurrentPageIndex(at - 1));
        numbers.getChildren().add(back);
        for (int i = first; i < last; i++) {
            final int index = i;
            ToggleButton number = new ToggleButton(String.valueOf(i + 1));
            number.getStyleClass().add("number-button");
            number.setSelected(i == at);
            number.setOnAction(e -> {
                setCurrentPageIndex(index);
                changed();
            });
            numbers.getChildren().add(number);
        }
        Button on = new Button(">");
        on.getStyleClass().add("right-arrow-button");
        on.setDisable(at >= count - 1);
        on.setOnAction(e -> setCurrentPageIndex(at + 1));
        numbers.getChildren().add(on);
        place.setText((at + 1) + "/" + (getPageCount() == INDETERMINATE ? "..." : String.valueOf(count)));
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /// Returns the number of pages, or [#INDETERMINATE].
    public final int getPageCount() {
        return pageCount.get();
    }

    /// Sets the number of pages, which must be at least one.
    public final void setPageCount(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Page count must be at least 1");
        }
        pageCount.set(value);
    }

    /// The number of pages.
    public final IntegerProperty pageCountProperty() {
        return pageCount;
    }

    /// Returns the index of the page that is showing, from zero.
    public final int getCurrentPageIndex() {
        return currentPageIndex.get();
    }

    /// Shows a page; an index outside the pages is the nearest page.
    public final void setCurrentPageIndex(int value) {
        currentPageIndex.set(value);
    }

    /// The index of the page that is showing.
    public final IntegerProperty currentPageIndexProperty() {
        return currentPageIndex;
    }

    /// Returns how many page numbers are shown at a time.
    public final int getMaxPageIndicatorCount() {
        return maxPageIndicatorCount.get();
    }

    /// Sets how many page numbers are shown at a time.
    public final void setMaxPageIndicatorCount(int value) {
        maxPageIndicatorCount.set(value);
    }

    /// How many page numbers are shown at a time.
    public final IntegerProperty maxPageIndicatorCountProperty() {
        return maxPageIndicatorCount;
    }

    /// Returns what makes the content of a page from its index.
    public final Callback<Integer, Node> getPageFactory() {
        return pageFactory.get();
    }

    /// Sets what makes the content of a page from its index.
    public final void setPageFactory(Callback<Integer, Node> value) {
        pageFactory.set(value);
    }

    /// What makes the content of a page from its index.
    public final ObjectProperty<Callback<Integer, Node>> pageFactoryProperty() {
        return pageFactory;
    }
}
