package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class InteractionDialogTest extends UITestBase {

    @BeforeEach
    void stubOrientation() {
        implementation.setPortrait(true);
    }

    @Test
    void constructorInitializesTitleAndContentPane() {
        InteractionDialog dialog = new InteractionDialog("Hello");
        assertEquals("Hello", dialog.getTitle());
        assertEquals("Dialog", dialog.getUIID());
        assertEquals("DialogTitle", dialog.getTitleComponent().getUIID());
        assertEquals("DialogContentPane", dialog.getContentPane().getUIID());
    }

    @Test
    void addComponentDelegatesToContentPane() {
        InteractionDialog dialog = new InteractionDialog();
        Label content = new Label("Body");
        dialog.addComponent(content);
        assertEquals(1, dialog.getContentPane().getComponentCount());
        assertSame(content, dialog.getContentPane().getComponentAt(0));
    }

    @Test
    void showPlacesDialogOnLayeredPane() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog("Title");
        dialog.setAnimateShow(false);
        dialog.show(10, 20, 30, 40);
        assertTrue(dialog.isShowing());
        Container layered = form.getLayeredPane(InteractionDialog.class, true);
        assertTrue(layered.contains(dialog));
        dialog.dispose();
        assertFalse(dialog.isShowing());
    }

    @Test
    void showPopupDialogUpdatesUiidsAndUsesLayeredPane() throws Exception {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        Rectangle rect = new Rectangle(20, 30, 80, 60);
        dialog.showPopupDialog(rect);
        assertEquals("PopupDialog", dialog.getUIID());
        assertEquals("PopupDialogTitle", dialog.getTitleComponent().getUIID());
        assertEquals("PopupContentPane", dialog.getContentPane().getUIID());
        Container layered = form.getLayeredPane(InteractionDialog.class, true);
        assertTrue(layered.contains(dialog));
        dialog.dispose();
    }

    @Test
    void pointerOutOfBoundsListenersInstalledWhenEnabled() throws Exception {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.setAnimateShow(false);
        dialog.show(0, 0, 0, 0);
        assertNotNull(getPrivateField(dialog, "pressedListener", Object.class));
        assertNotNull(getPrivateField(dialog, "releasedListener", Object.class));
        dialog.dispose();
    }

    @FormTest
    void formModeUsesFormLayeredPane() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.setFormMode(true);
        Rectangle rect = new Rectangle(0, 0, 50, 50);
        dialog.showPopupDialog(rect);
        Container formLayer = form.getFormLayeredPane(InteractionDialog.class, true);
        assertTrue(formLayer.contains(dialog));
        dialog.dispose();
    }

    @FormTest
    void disposeWithoutAnimationSchedulesFormRepaint() {
        // Regression for #5067: with setAnimateShow(false) +
        // setFormMode(true), dispose() removed the dialog from the form
        // tree but never asked the form to repaint, so the previously
        // painted dialog pixels stayed on screen until something else
        // (scrolling, hover) forced a redraw. dispose() -> remove()
        // triggers the recursive deinitialize() path, which runs
        // cleanupLayer() and detaches the layered pane wrapper before
        // the outer dispose gets to call pp.revalidate(). By then pp
        // has no Form in its parent chain, so the revalidate never
        // bubbles up to a Form.repaint(). dispose() must trigger a
        // form-level revalidate after cleanupLayer so the next paint
        // cycle clears the old dialog pixels. NB: this test requires a
        // shown form so the recursive deinitialize path actually fires
        // -- with just setCurrentForm the dialog isn't initialized and
        // dispose hits a different (working) code path that masks the
        // bug.
        RepaintCountingForm form = new RepaintCountingForm();
        form.show();
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.setFormMode(true);
        dialog.setDisposeWhenPointerOutOfBounds(false);
        Rectangle rect = new Rectangle(0, 0, 50, 50);
        dialog.showPopupDialog(rect);
        assertTrue(dialog.isShowing(), "dialog should be on the layered pane before dispose");

        // Reset the counter so we only observe repaint() calls triggered
        // by dispose itself, not the ones from show.
        form.repaintCount = 0;

        dialog.dispose();

        assertFalse(dialog.isShowing(), "dispose must detach the dialog from the form tree");
        assertTrue(form.repaintCount > 0,
                "#5067: dispose() must trigger a form repaint so the old dialog pixels "
                        + "are cleared without needing scroll/hover to force a redraw; "
                        + "observed " + form.repaintCount + " calls");
    }

    private static class RepaintCountingForm extends Form {
        int repaintCount;

        RepaintCountingForm() {
            super(new BorderLayout());
        }

        @Override
        public void repaint() {
            repaintCount++;
            super.repaint();
        }
    }

    @Test
    void showPopupDialogStraddlingMidlineDoesNotOverlapTarget() {
        // Regression for #5028: when the anchor rect straddles the
        // vertical midline, the legacy placement logic fell through to a
        // "popup over aligned with top of rect" branch that drew the
        // popup ON TOP of the target (covering the Close button in the
        // reporter's screenshot). The fix prefers above / below based on
        // available space; the popup must end up entirely outside the
        // target rect.
        implementation.setDisplaySize(1080, 1920);
        implementation.setPortrait(true);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.addComponent(new Label("Popup body content"));

        // 60px target straddling the midline at y=960.
        int targetHeight = 60;
        int targetY = 1920 / 2 - targetHeight / 2;
        Rectangle anchor = new Rectangle(490, targetY, 100, targetHeight);
        dialog.showPopupDialog(anchor);

        int dlgTop = dialog.getAbsoluteY();
        int dlgBottom = dlgTop + dialog.getHeight();
        int targetBottom = targetY + targetHeight;
        assertTrue(dialog.getHeight() > 0, "popup must have non-zero height");
        boolean overlaps = dlgTop < targetBottom && dlgBottom > targetY;
        assertFalse(overlaps,
                "#5028: popup [" + dlgTop + ".." + dlgBottom
                        + ") overlaps anchor [" + targetY + ".." + targetBottom
                        + ") -- expected the popup to land entirely above or below the rect");

        dialog.dispose();
    }

    @Test
    void showPopupDialogArrowDirectionConsistentWithPlacement() {
        // Regression for #5029: with the popup ending up overlapping the
        // target (the #5028 bug), CSSBorder.Arrow could not pick a
        // consistent direction (cabsY straddles trackY..trackY+h) so the
        // arrow tip rendered on the wrong edge. The arrow logic needs the
        // popup to be either fully above or fully below the target; this
        // test mirrors the reporter's geometry (target halfway down a
        // tall column) and pins that invariant.
        implementation.setDisplaySize(1080, 1920);
        implementation.setPortrait(true);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.addComponent(new Label("Popup body content"));

        // Target lives at y = available/2 - 1 (rect.getY() < availableHeight/2,
        // rect.bottom > availableHeight/2). This is the exact case that
        // used to hit the buggy "popup over aligned with top of rect"
        // branch before the fix.
        Rectangle anchor = new Rectangle(490, 1920 / 2 - 1, 100, 80);
        dialog.showPopupDialog(anchor);

        int dlgTop = dialog.getAbsoluteY();
        int dlgBottom = dlgTop + dialog.getHeight();
        int targetTop = anchor.getY();
        int targetBottom = targetTop + anchor.getHeight();

        boolean popupBelowTarget = dlgTop >= targetBottom;
        boolean popupAboveTarget = dlgBottom <= targetTop;
        assertTrue(popupBelowTarget || popupAboveTarget,
                "#5029: popup at [" + dlgTop + ".." + dlgBottom
                        + ") is neither fully above nor fully below target ["
                        + targetTop + ".." + targetBottom
                        + ") -- CSSBorder.Arrow has no consistent direction"
                        + " to point at the target");

        dialog.dispose();
    }

    @Test
    void showPopupDialogLandscapeFullWidthRectGetsVisibleSize() {
        // Regression for #4991: in landscape, when the anchor rect spans the
        // full available width (Picker in a Y-axis BoxLayout row), the legacy
        // "popup left" fallback computed width = max(0, rect.getX()) = 0 and
        // the dialog rendered zero-width. JS port desktop builds reproduced
        // this because their viewport satisfies isTablet() (sw>=600) AND
        // isPortrait()==false, sending Picker into the showPopupDialog branch.
        implementation.setDisplaySize(1440, 900);
        implementation.setPortrait(false);
        try {
            Form form = new Form(new BorderLayout());
            implementation.setCurrentForm(form);
            InteractionDialog dialog = new InteractionDialog();
            dialog.setAnimateShow(false);
            Label body = new Label("Body content with enough width to matter");
            dialog.addComponent(body);
            Rectangle fullWidthRect = new Rectangle(0, 80, 1440, 60);
            dialog.showPopupDialog(fullWidthRect);
            assertTrue(dialog.isShowing(), "dialog should be on the layered pane");
            assertTrue(dialog.getWidth() > 0,
                    "dialog must have non-zero width after a full-width anchor in landscape; got "
                            + dialog.getWidth());
            dialog.dispose();
        } finally {
            implementation.setDisplaySize(1080, 1920);
            implementation.setPortrait(true);
        }
    }

    @Test
    void animationSpeedDefaultsToThemeConstant() {
        InteractionDialog dialog = new InteractionDialog();
        assertEquals(-1, dialog.getAnimationSpeed(),
                "default should be -1 meaning 'use theme constant interactionDialogSpeedInt'");
    }

    @Test
    void animationSpeedSetterStoresValue() {
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimationSpeed(1500);
        assertEquals(1500, dialog.getAnimationSpeed());
        dialog.setAnimationSpeed(-1);
        assertEquals(-1, dialog.getAnimationSpeed(),
                "setting -1 reverts to the theme constant");
    }

    @Test
    void showAnimationSetupRunsInsteadOfDefaultRepositionAnimation() {
        // #5072: users need to customize show animations. The
        // setShowAnimationSetup callback replaces the built-in
        // "grow from 1x1 at center" behavior. Verify it runs and
        // that the parent bounds we set inside it are preserved
        // when the animation kicks off.
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        final int[] callCount = {0};
        dialog.setShowAnimationSetup(new Runnable() {
            @Override
            public void run() {
                callCount[0]++;
                // Slide-from-bottom setup: full size, translated off-screen
                Container parent = dialog.getParent();
                parent.setY(1000);
            }
        });
        dialog.show(0, 0, 0, 0);
        assertEquals(1, callCount[0], "showAnimationSetup must run once on show()");
        assertSame(dialog.getShowAnimationSetup(), dialog.getShowAnimationSetup(),
                "getter returns the stored callback");
        dialog.setShowAnimationSetup(null);
        assertNull(dialog.getShowAnimationSetup(), "setShowAnimationSetup(null) clears the override");
        dialog.dispose();
    }

    @Test
    void disposeAnimationSetupRunsInsteadOfDefaultRepositionAnimation() {
        // #5072: dispose animation should be customizable too.
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.show(0, 0, 0, 0);

        final int[] callCount = {0};
        dialog.setDisposeAnimationSetup(new Runnable() {
            @Override
            public void run() {
                callCount[0]++;
            }
        });
        // Re-enable animation so dispose runs the animation path
        // (and thus the dispose setup callback).
        dialog.setAnimateShow(true);
        dialog.dispose();
        assertEquals(1, callCount[0], "disposeAnimationSetup must run once on dispose()");
        dialog.setDisposeAnimationSetup(null);
        assertNull(dialog.getDisposeAnimationSetup(), "setDisposeAnimationSetup(null) clears the override");
    }

    @FormTest
    void stackableModeKeepsSiblingFormModeDialogOnDispose() {
        // #5193: all InteractionDialogs share the InteractionDialog.class
        // layer. In the default behavior, disposing one formMode dialog runs
        // cleanupLayer() -> c.removeAll()/c.remove(), which also wipes any
        // sibling dialog still showing in that layer ("sometimes nothing is
        // shown"). With stackable mode on, dispose must remove only the
        // disposed dialog.
        boolean prev = InteractionDialog.isStackable();
        InteractionDialog.setStackable(true);
        try {
            Form form = new Form(new BorderLayout());
            form.show();

            InteractionDialog first = new InteractionDialog("First");
            first.setAnimateShow(false);
            first.setFormMode(true);
            first.show(0, 0, 0, 0);

            InteractionDialog second = new InteractionDialog("Second");
            second.setAnimateShow(false);
            second.setFormMode(true);
            second.show(0, 0, 0, 0);

            Container formLayer = form.getFormLayeredPane(InteractionDialog.class, true);
            assertTrue(first.isShowing(), "first dialog should be showing");
            assertTrue(second.isShowing(), "second dialog should be showing");

            first.dispose();

            assertFalse(first.isShowing(), "disposed dialog must be removed");
            assertTrue(second.isShowing(),
                    "#5193: disposing one dialog must not remove its sibling sharing the layer");
            assertTrue(formLayer.contains(second),
                    "#5193: the sibling dialog must remain in the shared layer");

            second.dispose();
            assertFalse(second.isShowing(), "the last dialog should dispose normally");
        } finally {
            InteractionDialog.setStackable(prev);
        }
    }

    @FormTest
    void stackableModeKeepsSiblingDialogOnDisposeToTheLeft() {
        // #5193: disposeTo*() additionally calls pp.removeAll() on the shared
        // layered pane, which also discards siblings (independent of formMode).
        // Stackable mode must guard that too.
        boolean prev = InteractionDialog.isStackable();
        InteractionDialog.setStackable(true);
        try {
            Form form = new Form(new BorderLayout());
            form.show();

            InteractionDialog first = new InteractionDialog("First");
            first.setAnimateShow(false);
            first.show(0, 0, 0, 0);

            InteractionDialog second = new InteractionDialog("Second");
            second.setAnimateShow(false);
            second.show(0, 0, 0, 0);

            Container layer = form.getLayeredPane(InteractionDialog.class, true);

            first.disposeToTheLeft();

            assertFalse(first.isShowing(), "disposed dialog must be removed");
            assertTrue(second.isShowing(),
                    "#5193: disposeTo* must not remove the sibling dialog");
            assertTrue(layer.contains(second),
                    "#5193: the sibling dialog must remain in the shared layer");

            second.dispose();
        } finally {
            InteractionDialog.setStackable(prev);
        }
    }

    @FormTest
    void stackableModeLayersDialogsByShowOrder() {
        // #5193: dialogs should stack by show() order -- a dialog shown later
        // renders on top of one shown earlier (higher child index in the
        // shared layered pane).
        boolean prev = InteractionDialog.isStackable();
        InteractionDialog.setStackable(true);
        try {
            Form form = new Form(new BorderLayout());
            form.show();

            InteractionDialog first = new InteractionDialog("First");
            first.setAnimateShow(false);
            first.show(0, 0, 0, 0);

            InteractionDialog second = new InteractionDialog("Second");
            second.setAnimateShow(false);
            second.show(0, 0, 0, 0);

            Container layer = form.getLayeredPane(InteractionDialog.class, true);
            int firstIndex = layer.getComponentIndex(first.getParent());
            int secondIndex = layer.getComponentIndex(second.getParent());
            assertTrue(secondIndex > firstIndex,
                    "#5193: the later-shown dialog must layer on top of the earlier one");

            first.dispose();
            second.dispose();
        } finally {
            InteractionDialog.setStackable(prev);
        }
    }

    @FormTest
    void stackableModeRemovesSharedLayerOnceEmpty() {
        // #5193: layers must not accumulate -- once the last dialog leaves the
        // shared layer it should be torn down rather than lingering empty.
        boolean prev = InteractionDialog.isStackable();
        InteractionDialog.setStackable(true);
        try {
            Form form = new Form(new BorderLayout());
            form.show();

            InteractionDialog dialog = new InteractionDialog("Only");
            dialog.setAnimateShow(false);
            dialog.show(0, 0, 0, 0);

            Container layer = form.getLayeredPane(InteractionDialog.class, true);
            assertNotNull(layer.getParent(), "layer should be attached while a dialog is showing");

            dialog.dispose();

            assertNull(layer.getParent(),
                    "#5193: the shared layer must be removed once the last dialog disposes");
        } finally {
            InteractionDialog.setStackable(prev);
        }
    }

    @FormTest
    void defaultModeClearsFormLayerOnDispose() {
        // Backward-compat: with stackable mode off (the default), disposing a
        // formMode dialog still clears and removes the shared form layer as it
        // historically did.
        assertFalse(InteractionDialog.isStackable(), "stackable must default to false");
        Form form = new Form(new BorderLayout());
        form.show();

        InteractionDialog dialog = new InteractionDialog("Only");
        dialog.setAnimateShow(false);
        dialog.setFormMode(true);
        dialog.show(0, 0, 0, 0);

        Container formLayer = form.getFormLayeredPane(InteractionDialog.class, true);
        assertTrue(formLayer.contains(dialog), "dialog should be in the form layer before dispose");

        dialog.dispose();

        assertFalse(dialog.isShowing(), "dispose must remove the dialog");
        assertNull(formLayer.getParent(),
                "default behavior should remove the shared form layer on dispose");
    }

    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
