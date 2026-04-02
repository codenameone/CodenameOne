package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Hashtable;

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

    @Test
    void showPopupDialogBiasTruePrefersTopWhenBothFit() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(120, 220, 60, 40);
        dialog.showPopupDialog(rect, true);

        assertTrue(dialog.getY() + dialog.getHeight() <= rect.getY(),
                "Expected popup above target when prioritizeTopOrRightPosition=true");
        dialog.dispose();
    }

    @Test
    void showPopupDialogBiasFalsePrefersBottomWhenBothFit() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(120, 220, 60, 40);
        dialog.showPopupDialog(rect, false);

        assertTrue(dialog.getY() >= rect.getY() + rect.getHeight(),
                "Expected popup below target when prioritizeTopOrRightPosition=false");
        dialog.dispose();
    }

    @Test
    void showPopupDialogFallsBackWhenPreferredTopDoesNotFit() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(120, 2, 60, 40);
        dialog.showPopupDialog(rect, true);

        assertTrue(dialog.getY() >= rect.getY() + rect.getHeight(),
                "Expected fallback below when preferred top side does not fit");
        dialog.dispose();
    }

    @Test
    void showPopupDialogFallsBackWhenPreferredBottomDoesNotFit() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        int displayHeight = implementation.getDisplayHeight();
        Rectangle rect = new Rectangle(120, Math.max(0, displayHeight - 8), 60, 6);
        dialog.showPopupDialog(rect, false);

        assertTrue(dialog.getY() + dialog.getHeight() <= rect.getY(),
                "Expected fallback above when preferred bottom side does not fit");
        dialog.dispose();
    }

    @Test
    void showPopupDialogBiasTruePrefersRightInLandscapeWhenBothFit() {
        implementation.setPortrait(false);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(120, 140, 60, 40);
        dialog.showPopupDialog(rect, true);

        assertTrue(dialog.getX() >= rect.getX() + rect.getWidth(),
                "Expected popup on right side when prioritizeTopOrRightPosition=true in landscape");
        dialog.dispose();
    }

    @Test
    void showPopupDialogBiasFalsePrefersLeftInLandscapeWhenBothFit() {
        implementation.setPortrait(false);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(120, 140, 60, 40);
        dialog.showPopupDialog(rect, false);

        assertTrue(dialog.getX() + dialog.getWidth() <= rect.getX(),
                "Expected popup on left side when prioritizeTopOrRightPosition=false in landscape");
        dialog.dispose();
    }

    @Test
    void showPopupDialogFallsBackWhenPreferredRightDoesNotFit() {
        implementation.setPortrait(false);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        int displayWidth = implementation.getDisplayWidth();
        Rectangle rect = new Rectangle(Math.max(0, displayWidth - 8), 140, 6, 40);
        dialog.showPopupDialog(rect, true);

        assertTrue(dialog.getX() + dialog.getWidth() <= rect.getX(),
                "Expected fallback to left when preferred right side does not fit");
        dialog.dispose();
    }

    @Test
    void showPopupDialogFallsBackWhenPreferredLeftDoesNotFit() {
        implementation.setPortrait(false);
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Popup"));
        dialog.setAnimateShow(false);

        Rectangle rect = new Rectangle(2, 140, 6, 40);
        dialog.showPopupDialog(rect, false);

        assertTrue(dialog.getX() >= rect.getX() + rect.getWidth(),
                "Expected fallback to right when preferred left side does not fit");
        dialog.dispose();
    }

    @Test
    void showPopupDialogSetsArrowTrackComponentForBothBiasModes() throws Exception {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        InteractionDialog topDialog = new InteractionDialog();
        topDialog.setUIID("PopupDialog");
        topDialog.setLayout(new FlowLayout());
        topDialog.add(new Label("Popup"));
        topDialog.setAnimateShow(false);
        TrackingBorder topBorder = new TrackingBorder();
        topDialog.getAllStyles().setBorder(topBorder);
        Rectangle rect = new Rectangle(120, 220, 60, 40);
        topDialog.showPopupDialog(rect, true);
        assertNotNull(topBorder.lastTrackRect, "Expected border tracking rectangle to be set");
        assertEquals(rect.getX(), topBorder.lastTrackRect.getX());
        assertEquals(rect.getY(), topBorder.lastTrackRect.getY());
        assertTrue(topDialog.getY() + topDialog.getHeight() <= rect.getY(),
                "Bias=true should place popup above in this setup");
        topDialog.dispose();

        InteractionDialog bottomDialog = new InteractionDialog();
        bottomDialog.setUIID("PopupDialog");
        bottomDialog.setLayout(new FlowLayout());
        bottomDialog.add(new Label("Popup"));
        bottomDialog.setAnimateShow(false);
        TrackingBorder bottomBorder = new TrackingBorder();
        bottomDialog.getAllStyles().setBorder(bottomBorder);
        bottomDialog.showPopupDialog(rect, false);
        assertNotNull(bottomBorder.lastTrackRect, "Expected border tracking rectangle to be set");
        assertEquals(rect.getX(), bottomBorder.lastTrackRect.getX());
        assertEquals(rect.getY(), bottomBorder.lastTrackRect.getY());
        assertTrue(bottomDialog.getY() >= rect.getY() + rect.getHeight(),
                "Bias=false should place popup below in this setup");
        bottomDialog.dispose();
    }

    private static class TrackingBorder extends Border {
        private Rectangle lastTrackRect;
        private boolean imageTileCalled;

        @Override
        public void setTrackComponent(Rectangle trackComponent) {
            lastTrackRect = trackComponent;
            super.setTrackComponent(trackComponent);
        }

        @Override
        public void setImageBorderSpecialTile(Image tileTop, Image tileBottom, Image tileLeft, Image tileRight, Rectangle trackComponent) {
            imageTileCalled = true;
            super.setImageBorderSpecialTile(tileTop, tileBottom, tileLeft, tileRight, trackComponent);
        }
    }

    @Test
    void showPopupDialogUsesTrackFallbackWhenArrowImagesMissing() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        String uiid = "UnitTestPopupNoImages";
        Hashtable<String, Object> themeProps = new Hashtable<>();
        themeProps.put(uiid + "ArrowBool", "true");
        UIManager.getInstance().addThemeProps(themeProps);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setUIID(uiid);
        dialog.setAnimateShow(false);
        TrackingBorder border = new TrackingBorder();
        dialog.getAllStyles().setBorder(border);
        dialog.add(new Label("Popup"));
        dialog.showPopupDialog(new Rectangle(120, 220, 60, 40), true);

        assertNotNull(border.lastTrackRect, "Expected tracked rectangle fallback when arrow images are missing");
        assertFalse(border.imageTileCalled, "Should not use image-border tiles when no arrow images are provided");
        dialog.dispose();
    }

    @Test
    void showPopupDialogUsesImageTilesWhenArrowImagesProvided() {
        Form form = new Form(new BorderLayout());
        implementation.setCurrentForm(form);

        String uiid = "UnitTestPopupWithImages";
        Hashtable<String, Object> themeProps = new Hashtable<>();
        themeProps.put(uiid + "ArrowBool", "true");
        Image img = Image.createImage(4, 4);
        themeProps.put(uiid + "ArrowTopImage", img);
        themeProps.put(uiid + "ArrowBottomImage", img);
        themeProps.put(uiid + "ArrowLeftImage", img);
        themeProps.put(uiid + "ArrowRightImage", img);
        UIManager.getInstance().addThemeProps(themeProps);

        InteractionDialog dialog = new InteractionDialog();
        dialog.setUIID(uiid);
        dialog.setAnimateShow(false);
        TrackingBorder border = new TrackingBorder();
        dialog.getAllStyles().setBorder(border);
        dialog.add(new Label("Popup"));
        dialog.showPopupDialog(new Rectangle(120, 220, 60, 40), true);

        assertTrue(border.imageTileCalled, "Expected image-border tile path when arrow images are provided");
        dialog.dispose();
    }

    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
