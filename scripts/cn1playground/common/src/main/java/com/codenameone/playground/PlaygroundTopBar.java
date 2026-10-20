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

package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.Resources;

final class PlaygroundTopBar extends Container {
    interface Actions {
        void onModeChanged(String key);
        void onShare();
        void onDownload();
    }

    static final String MODE_CODE = "code";
    static final String MODE_CSS = "css";

    private final Actions actions;
    private final Label appIcon;
    private final Container iconColumn;
    private final Label wordmark;
    private final PlaygroundSegmented modeToggle;
    private final PlaygroundStatusPill statusPill;
    private final Button shareButton;
    private final Button downloadButton;
    private final Container rightActions;
    private Button resetButton;
    private boolean darkMode;
    private boolean compact;
    private boolean mobile;

    PlaygroundTopBar(String initialMode, boolean darkMode, Actions actions) {
        super(new BorderLayout());
        this.actions = actions;
        this.darkMode = darkMode;
        setUIID(darkMode ? "PlaygroundTopBarDark" : "PlaygroundTopBar");

        appIcon = new Label();
        applyAppIconStyle(appIcon, darkMode);
        appIcon.getAllStyles().setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
        appIcon.getAllStyles().setMargin(0, 0, 0, 0);

        wordmark = new Label("Playground");
        wordmark.setUIID(darkMode ? "PlaygroundWordmarkDark" : "PlaygroundWordmark");

        PlaygroundSegmented.Option[] modeOptions = new PlaygroundSegmented.Option[]{
                new PlaygroundSegmented.Option(MODE_CODE, "Code", FontImage.MATERIAL_CODE),
                // CN1's MATERIAL_* constants ship only the filled Material Icons.
                // MATERIAL_BRUSH is the closest wireframe-style alternative for the
                // "styling" action; use it in place of the filled MATERIAL_PALETTE.
                new PlaygroundSegmented.Option(MODE_CSS, "CSS", FontImage.MATERIAL_BRUSH)
        };
        modeToggle = new PlaygroundSegmented(modeOptions, initialMode == null ? MODE_CODE : initialMode,
                darkMode, key -> {
                    if (actions != null) {
                        actions.onModeChanged(key);
                    }
                });

        statusPill = new PlaygroundStatusPill(darkMode);

        shareButton = new Button("Share");
        shareButton.setUIID(darkMode ? "PlaygroundShareButtonDark" : "PlaygroundShareButton");
        FontImage.setMaterialIcon(shareButton, FontImage.MATERIAL_IOS_SHARE, 3f);
        shareButton.setTextPosition(Component.RIGHT);
        shareButton.setGap(Display.getInstance().convertToPixels(1.3f));
        shareButton.addActionListener(e -> {
            if (actions != null) {
                actions.onShare();
            }
        });

        downloadButton = new Button("Download");
        downloadButton.setUIID(darkMode ? "PlaygroundDownloadButtonDark" : "PlaygroundDownloadButton");
        FontImage.setMaterialIcon(downloadButton, FontImage.MATERIAL_DOWNLOAD, 3f);
        downloadButton.setTextPosition(Component.RIGHT);
        downloadButton.setGap(Display.getInstance().convertToPixels(1.3f));
        downloadButton.addActionListener(e -> {
            if (actions != null) {
                actions.onDownload();
            }
        });

        // Wrap the app icon in a fixed-width column that matches the activity
        // bar so the top-bar icon sits directly above the activity bar icons
        // and the wordmark lines up with whatever panel title is open.
        iconColumn = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        iconColumn.getAllStyles().setBgTransparency(0);
        iconColumn.setPreferredW(Display.getInstance().convertToPixels(11f));
        iconColumn.add(appIcon);

        Container left = new Container(BoxLayout.x());
        left.getAllStyles().setBgTransparency(0);
        left.add(iconColumn);
        left.add(wordmark);
        left.add(modeToggle);
        left.add(statusPill);

        rightActions = new Container(new FlowLayout(Component.RIGHT, Component.CENTER));
        rightActions.getAllStyles().setBgTransparency(0);
        rightActions.add(shareButton);
        rightActions.add(downloadButton);

        add(BorderLayout.WEST, left);
        add(BorderLayout.EAST, rightActions);
    }

    /// Inline mode: the Playground embedded under a code snippet in the developer
    /// guide or the API reference. Share and Download give way to Reset (back to
    /// the snippet as the page shows it), and the wordmark is dropped to leave the
    /// narrow frame to the code. Opening the full Playground is the host page's
    /// button, not this bar's -- see ``WebsiteThemeNative.notifyInlineResult``.
    void setInline(Runnable onReset) {
        wordmark.setVisible(false);
        wordmark.setHidden(true);
        rightActions.removeAll();
        resetButton = createActionButton("Reset", FontImage.MATERIAL_RESTORE, onReset);
        rightActions.add(resetButton);
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    private Button createActionButton(String text, char icon, Runnable action) {
        Button b = new Button(text);
        b.setUIID(darkMode ? "PlaygroundShareButtonDark" : "PlaygroundShareButton");
        FontImage.setMaterialIcon(b, icon, 3f);
        b.setTextPosition(Component.RIGHT);
        b.setGap(Display.getInstance().convertToPixels(1.3f));
        b.addActionListener(e -> action.run());
        return b;
    }

    void setMode(String mode) {
        modeToggle.setSelected(mode);
    }

    String getMode() {
        return modeToggle.getSelectedKey();
    }

    void showLive() {
        statusPill.showLive();
    }

    void showFailed() {
        statusPill.showFailed();
    }

    boolean isFailed() {
        return statusPill.isFailed();
    }

    void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        // setVisible alone leaves the wordmark's preferred width reserved,
        // which eats ~30 mm of horizontal space on mobile and pushes the
        // Live pill / Share / Download off-screen. setHidden() collapses it
        // to 0 preferred width so the remaining siblings fit.
        // Inline mode never shows the wordmark, whatever the width.
        boolean showWordmark = !compact && resetButton == null;
        wordmark.setVisible(showWordmark);
        wordmark.setHidden(!showWordmark);
        if (compact) {
            shareButton.setText("");
            downloadButton.setText("");
        } else {
            shareButton.setText("Share");
            downloadButton.setText("Download");
        }
        if (resetButton != null) {
            resetButton.setText(compact ? "" : "Reset");
        }
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    /// Mobile strips further than compact: the Code/CSS mode toggle is hidden
    /// (the tab strip under the top bar takes its role), the status pill
    /// collapses to its dot-only compact form, the icon column shrinks to 8mm
    /// so the remaining siblings have room, and the Share/Download icon sizes
    /// drop from 3mm to 2.2mm.
    void setMobile(boolean mobile) {
        if (this.mobile == mobile) {
            return;
        }
        this.mobile = mobile;
        modeToggle.setVisible(!mobile);
        modeToggle.setHidden(mobile);
        statusPill.setCompactDot(mobile);
        if (iconColumn != null) {
            iconColumn.setPreferredW(Display.getInstance().convertToPixels(mobile ? 8f : 11f));
        }
        float iconMm = mobile ? 2.2f : 3f;
        FontImage.setMaterialIcon(shareButton, FontImage.MATERIAL_IOS_SHARE, iconMm);
        FontImage.setMaterialIcon(downloadButton, FontImage.MATERIAL_DOWNLOAD, iconMm);
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        setUIID(dark ? "PlaygroundTopBarDark" : "PlaygroundTopBar");
        wordmark.setUIID(dark ? "PlaygroundWordmarkDark" : "PlaygroundWordmark");
        applyAppIconStyle(appIcon, dark);
        modeToggle.applyTheme(dark);
        statusPill.setDarkMode(dark);
        shareButton.setUIID(dark ? "PlaygroundShareButtonDark" : "PlaygroundShareButton");
        downloadButton.setUIID(dark ? "PlaygroundDownloadButtonDark" : "PlaygroundDownloadButton");
        FontImage.setMaterialIcon(shareButton, FontImage.MATERIAL_IOS_SHARE, 3f);
        FontImage.setMaterialIcon(downloadButton, FontImage.MATERIAL_DOWNLOAD, 3f);
        if (resetButton != null) {
            resetButton.setUIID(dark ? "PlaygroundShareButtonDark" : "PlaygroundShareButton");
            FontImage.setMaterialIcon(resetButton, FontImage.MATERIAL_RESTORE, 3f);
        }
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    /// In dark mode the playground's icon.png would otherwise flash a white
    /// background against the navy top bar. Render a white Material icon on a
    /// translucent rounded pill instead; in light mode keep the bundled image.
    private static void applyAppIconStyle(Label label, boolean dark) {
        label.setIcon(null);
        if (dark) {
            label.setUIID("PlaygroundAppIconDark");
            // Explicitly set white FG before setMaterialIcon bakes the glyph -
            // setMaterialIcon uses the style's current FG color at the moment
            // of the call, and UIID application can be deferred. Forcing the
            // colour inline guarantees the icon renders in white.
            label.getAllStyles().setFgColor(0xFFFFFF);
            FontImage.setMaterialIcon(label, FontImage.MATERIAL_CODE, 4.5f);
        } else {
            label.setUIID("PlaygroundAppIcon");
            label.getAllStyles().setFgColor(0x112247);
            Image icon = loadAppIcon();
            if (icon != null) {
                int px = Display.getInstance().convertToPixels(6.5f);
                label.setIcon(icon.scaled(px, px));
            } else {
                FontImage.setMaterialIcon(label, FontImage.MATERIAL_CODE, 4.5f);
            }
        }
    }

    private static Image loadAppIcon() {
        try {
            Resources r = Resources.getGlobalResources();
            if (r == null) {
                return null;
            }
            Image img = r.getImage("icon.png");
            if (img != null) {
                return img;
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }
}
