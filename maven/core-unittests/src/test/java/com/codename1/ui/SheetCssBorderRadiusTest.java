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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// How a `border-radius` coming out of a stylesheet may pad a `Sheet`.
///
/// A rule such as
///
/// ```css
/// cntSheet { border-radius: 4mm 4mm 0mm 0mm; padding: 0mm; margin: 0mm; border: none; }
/// ```
///
/// compiles to a `RoundRectBorder` (a `CSSBorder` before the JS port native themes work),
/// and `Sheet.show` used to inset the content pane by the corner radius for every
/// `RoundRectBorder` it saw. With `padding: 0mm` in the stylesheet that inset is padding
/// the author never wrote and it renders as a band of empty space under the title, which is
/// [issue 5488](https://github.com/codenameone/CodenameOne/issues/5488). The inset stays for
/// hand written borders, which reserve twice the radius of their own.
class SheetCssBorderRadiusTest extends UITestBase {

    @FormTest
    void cssSizedBorderDoesNotPadTheContentPane() {
        Sheet sheet = showSheet(cssBorder());

        Container contentPane = sheet.getContentPane();
        assertEquals(0, contentPane.getStyle().getPaddingTop(),
                "a stylesheet radius may not add padding above the content");
        assertEquals(0, contentPane.getStyle().getPaddingBottom(),
                "a stylesheet radius may not add padding below the content");
        assertEquals(0, contentPane.getStyle().getPaddingLeftNoRTL(),
                "a stylesheet radius may not indent the content");
        assertEquals(0, contentPane.getStyle().getPaddingRightNoRTL(),
                "a stylesheet radius may not indent the content");
    }

    @FormTest
    void cssSizedBorderLeavesNoGapUnderTheTitle() {
        // The reported app lays the sheet itself out in a Y box and adds to it directly, so the
        // empty content pane sits between the title bar and the content: any padding it picks up
        // is visible as a gap.
        Sheet sheet = new Sheet(null, "Title");
        sheet.getAllStyles().setBorder(cssBorder());
        zeroBox(sheet);
        sheet.setLayout(BoxLayout.y());
        Label first = new Label("Test label 1");
        sheet.add(first);
        sheet.add(new Label("Test label 2"));
        show(sheet);

        Container contentPane = sheet.getContentPane();
        assertEquals(0, contentPane.getPreferredH(),
                "the empty content pane may not reserve height for the corner radius");
        assertEquals(0, contentPane.getHeight(),
                "the empty content pane may not take up height under the title");
        int gap = first.getY() - (contentPane.getY() + contentPane.getHeight());
        assertEquals(first.getStyle().getMarginTop(), gap,
                "the first label must follow the title bar with nothing but its own margin above it");
    }

    @FormTest
    void handWrittenBorderStillInsetsTheContentPane() {
        // Legacy sizing: the border reserves twice the radius, so the content pane is inset by the
        // radius to keep content clear of the rounded corners. That behavior is unchanged.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));

        int radius = Display.getInstance().convertToPixels(4f);
        assertEquals(radius, sheet.getContentPane().getStyle().getPaddingTop(),
                "a hand written radius keeps insetting the content pane");
        assertEquals(radius, sheet.getContentPane().getStyle().getPaddingLeftNoRTL(),
                "a hand written radius keeps insetting the content pane");
    }

    @FormTest
    void restylingToACssBorderTakesTheLegacyInsetBackOff() {
        // The inset of a hand written border is written into the style of the content pane, so a
        // sheet restyled with a CSS sized border has to have it removed rather than merely not
        // reapplied, otherwise the gap outlives the restyle.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));
        assertEquals(Display.getInstance().convertToPixels(4f),
                sheet.getContentPane().getStyle().getPaddingTop(),
                "the hand written border insets the content pane on the first show");

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(0, sheet.getContentPane().getStyle().getPaddingTop(),
                "the inset of the previous border may not survive the restyle");
        assertEquals(0, sheet.getContentPane().getStyle().getPaddingLeftNoRTL(),
                "the inset of the previous border may not survive the restyle");
    }

    @FormTest
    void restylingRestoresThePaddingTheDeveloperSet() {
        // Restoring must not zero the content pane, it puts back whatever was there before the
        // border inset it, in the units it was written in.
        Sheet sheet = new Sheet(null, "Title");
        sheet.getContentPane().getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS);
        sheet.getContentPane().getAllStyles().setPadding(1f, 1f, 2f, 2f);
        sheet.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(4f));
        zeroBox(sheet);
        sheet.getContentPane().add(new Label("Test label 1"));
        show(sheet);
        assertEquals(Display.getInstance().convertToPixels(4f),
                sheet.getContentPane().getStyle().getPaddingTop(),
                "the hand written border insets the content pane on the first show");

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        Style contentStyle = sheet.getContentPane().getStyle();
        assertEquals(Display.getInstance().convertToPixels(1f), contentStyle.getPaddingTop(),
                "the padding of the developer comes back, in the unit it was written in");
        assertEquals(Display.getInstance().convertToPixels(2f), contentStyle.getPaddingLeftNoRTL(),
                "the padding of the developer comes back, in the unit it was written in");
        assertEquals(Style.UNIT_TYPE_DIPS, contentStyle.getPaddingUnit()[Component.TOP],
                "the padding unit of the developer comes back too");
    }

    @FormTest
    void restoringLeavesTheOtherStylesOfTheContentPaneAlone() {
        // The inset pads the current style of the content pane, not all of its styles, so restoring
        // may not write the padding of the current style over the selected, pressed and disabled
        // styles the inset never touched.
        Sheet sheet = new Sheet(null, "Title");
        sheet.getContentPane().getSelectedStyle().setPadding(7, 7, 7, 7);
        sheet.getContentPane().getPressedStyle().setPadding(9, 9, 9, 9);
        sheet.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(4f));
        zeroBox(sheet);
        sheet.getContentPane().add(new Label("Test label 1"));
        show(sheet);

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(7, sheet.getContentPane().getSelectedStyle().getPaddingTop(),
                "the selected style of the content pane is not part of the inset");
        assertEquals(9, sheet.getContentPane().getPressedStyle().getPaddingTop(),
                "the pressed style of the content pane is not part of the inset");
    }

    @FormTest
    void aThemeRefreshBetweenShowsDropsTheSnapshot() {
        // Replacing the style of the content pane, which is what a theme refresh does, takes the
        // inset with it. The snapshot then describes a style nobody is using any more and writing
        // it into the fresh style would undo the theme.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));

        Style fresh = new Style(sheet.getContentPane().getStyle());
        fresh.setPadding(3, 3, 3, 3);
        fresh.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        sheet.getContentPane().setUnselectedStyle(fresh);

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(3, sheet.getContentPane().getStyle().getPaddingTop(),
                "the padding of the new style survives, the stale snapshot is dropped");
    }

    @FormTest
    void paddingChangedBetweenShowsIsNotOverwritten() {
        // The developer padding the content pane after the inset went on is saying what they want
        // it to be. Restoring may not put the pre-inset padding back over that.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));

        sheet.getContentPane().getStyle().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        sheet.getContentPane().getStyle().setPadding(5, 5, 5, 5);

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(5, sheet.getContentPane().getStyle().getPaddingTop(),
                "padding set after the inset wins over the snapshot");
    }

    @FormTest
    void disablingTheContentPaneBetweenShowsStillTakesTheInsetOff() {
        // The style a component presents follows its state, so the pane being disabled by the time
        // the sheet is restyled must not strand the inset in the unselected style it went into.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));
        assertEquals(Display.getInstance().convertToPixels(4f),
                sheet.getContentPane().getUnselectedStyle().getPaddingTop(),
                "the inset goes into the unselected style while the pane is enabled");

        sheet.getContentPane().setEnabled(false);
        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(0, sheet.getContentPane().getUnselectedStyle().getPaddingTop(),
                "the inset comes off the style it went into, not the style presented now");
    }

    @FormTest
    void everyStyleThatWasInsetIsRestoredHoweverManyThereAre() {
        // Insets pile up one per style, and none of them may be evicted to make room: a style being
        // the oldest recorded does not say it is gone, so evicting it would strand its inset.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));
        Style first = sheet.getContentPane().getStyle();
        assertEquals(Display.getInstance().convertToPixels(4f), first.getPaddingTop(),
                "the first style is inset by the hand written border");

        for (int iter = 0; iter < 4; iter++) {
            Style fresh = new Style(first);
            fresh.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
            fresh.setPadding(0, 0, 0, 0);
            sheet.getContentPane().setUnselectedStyle(fresh);
            show(sheet);
        }

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(0, first.getPaddingTop(),
                "the style inset first is restored however many were recorded after it");
    }

    @FormTest
    void changingOneSideAfterTheInsetLeavesTheOtherThreeRestorable() {
        // Each side stands on its own: padding one side after the inset says what that side should
        // be, and says nothing about the three the inset is still sitting on.
        Sheet sheet = showSheet(RoundRectBorder.create().cornerRadius(4f));
        Style contentStyle = sheet.getContentPane().getStyle();

        contentStyle.setPaddingUnitTop(Style.UNIT_TYPE_PIXELS);
        contentStyle.setPadding(Component.TOP, 5f);

        sheet.getAllStyles().setBorder(cssBorder());
        show(sheet);

        assertEquals(5, contentStyle.getPaddingTop(),
                "the side padded after the inset keeps what it was given");
        assertEquals(0, contentStyle.getPaddingBottom(),
                "the sides still holding the inset are restored");
        assertEquals(0, contentStyle.getPaddingLeftNoRTL(),
                "the sides still holding the inset are restored");
        assertEquals(0, contentStyle.getPaddingRightNoRTL(),
                "the sides still holding the inset are restored");
    }

    private RoundRectBorder cssBorder() {
        // What the CSS compiler emits for border-radius: 4mm 4mm 0mm 0mm
        return RoundRectBorder.create()
                .cornerRadius(4f)
                .topLeftMode(true)
                .topRightMode(true)
                .bottomLeftMode(false)
                .bottomRightMode(false)
                .cssBoxModel(true);
    }

    private Sheet showSheet(RoundRectBorder border) {
        Sheet sheet = new Sheet(null, "Title");
        sheet.getAllStyles().setBorder(border);
        zeroBox(sheet);
        sheet.getContentPane().add(new Label("Test label 1"));
        sheet.getContentPane().add(new Label("Test label 2"));
        show(sheet);
        return sheet;
    }

    private void zeroBox(Sheet sheet) {
        // padding: 0mm; margin: 0mm from the reported stylesheet
        sheet.getAllStyles().setPadding(0, 0, 0, 0);
        sheet.getAllStyles().setMargin(0, 0, 0, 0);
    }

    private void show(Sheet sheet) {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());
        sheet.show(0);
        form.getAnimationManager().flush();
        flushSerialCalls();
        form.revalidate();
    }
}
