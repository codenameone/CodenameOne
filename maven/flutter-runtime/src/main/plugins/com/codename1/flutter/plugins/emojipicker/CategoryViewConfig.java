/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.emojipicker;

/// emoji_picker_flutter's `CategoryViewConfig`: held for [Config], not yet applied by the picker.
public class CategoryViewConfig {
    private Object initCategory;
    private Object backgroundColor;
    private Object indicatorColor;
    private Object iconColor;
    private Object iconColorSelected;
    private Object backspaceColor;
    private Object tabBarHeight;
    private Object tabIndicatorAnimDuration;
    private Object showBackspaceButton;
    private Object recentTabBehavior;
    private Object categoryIcons;
    private Object dividerColor;
    private Object extraTab;
    private Object customCategoryView;

    public CategoryViewConfig() {
    }

    public void initCategory(Object v) {
        initCategory = v;
    }

    public Object getInitCategory() {
        return initCategory;
    }

    public void backgroundColor(Object v) {
        backgroundColor = v;
    }

    public Object getBackgroundColor() {
        return backgroundColor;
    }

    public void indicatorColor(Object v) {
        indicatorColor = v;
    }

    public Object getIndicatorColor() {
        return indicatorColor;
    }

    public void iconColor(Object v) {
        iconColor = v;
    }

    public Object getIconColor() {
        return iconColor;
    }

    public void iconColorSelected(Object v) {
        iconColorSelected = v;
    }

    public Object getIconColorSelected() {
        return iconColorSelected;
    }

    public void backspaceColor(Object v) {
        backspaceColor = v;
    }

    public Object getBackspaceColor() {
        return backspaceColor;
    }

    public void tabBarHeight(Object v) {
        tabBarHeight = v;
    }

    public Object getTabBarHeight() {
        return tabBarHeight;
    }

    public void tabIndicatorAnimDuration(Object v) {
        tabIndicatorAnimDuration = v;
    }

    public Object getTabIndicatorAnimDuration() {
        return tabIndicatorAnimDuration;
    }

    public void showBackspaceButton(Object v) {
        showBackspaceButton = v;
    }

    public Object getShowBackspaceButton() {
        return showBackspaceButton;
    }

    public void recentTabBehavior(Object v) {
        recentTabBehavior = v;
    }

    public Object getRecentTabBehavior() {
        return recentTabBehavior;
    }

    public void categoryIcons(Object v) {
        categoryIcons = v;
    }

    public Object getCategoryIcons() {
        return categoryIcons;
    }

    public void dividerColor(Object v) {
        dividerColor = v;
    }

    public Object getDividerColor() {
        return dividerColor;
    }

    public void extraTab(Object v) {
        extraTab = v;
    }

    public Object getExtraTab() {
        return extraTab;
    }

    public void customCategoryView(Object v) {
        customCategoryView = v;
    }

    public Object getCustomCategoryView() {
        return customCategoryView;
    }
}
