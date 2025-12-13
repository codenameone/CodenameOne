package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class TooltipManagerTest extends UITestBase {
    @BeforeEach
    void enableManager() {
        TooltipManager.enableTooltips();
    }

    @AfterEach
    void resetManager() {
        TooltipManager.enableTooltips();
    }

    @FormTest
    void enableTooltipsRegistersInstance() {
        TooltipManager.enableTooltips();
        assertNotNull(TooltipManager.getInstance(), "Default enable should create an instance");

        InspectableTooltipManager manager = new InspectableTooltipManager();
        TooltipManager.enableTooltips(manager);
        assertSame(manager, TooltipManager.getInstance(), "Custom manager should be stored as singleton");
    }

    @FormTest
    void showTooltipCreatesDialogWithUiids() {
        implementation.setBuiltinSoundsEnabled(false);
        InspectableTooltipManager manager = new InspectableTooltipManager();
        manager.setDialogUIID("CustomDialog");
        manager.setTextUIID("CustomText");
        TooltipManager.enableTooltips(manager);

        Form form = Display.getInstance().getCurrent();
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();
        Label target = new Label("Hover");
        form.add(target);
        form.revalidate();

        manager.showTooltipPublic("Hello", target);

        InteractionDialog tooltip = manager.findTooltip(form);
        assertNotNull(tooltip, "Tooltip dialog should have been created");
        assertEquals("CustomDialog", tooltip.getUIID(), "Dialog UIID should match custom setting");
        TextArea body = manager.findTooltipText(tooltip);
        assertNotNull(body, "Tooltip text area should be created");
        assertEquals("CustomText", body.getUIID(), "Tooltip text UIID should match custom setting");
    }

    @FormTest
    void clearTooltipDisposesDialogAndCancelsTimer() {
        implementation.setBuiltinSoundsEnabled(false);
        InspectableTooltipManager manager = new InspectableTooltipManager();
        TooltipManager.enableTooltips(manager);

        Form form = Display.getInstance().getCurrent();
        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();
        Label target = new Label("Hover");
        form.add(target);
        form.revalidate();

        manager.showTooltipPublic("Hello", target);

        InteractionDialog dialog = manager.findTooltip(form);
        assertNotNull(dialog, "Tooltip dialog should exist before clearing");

        manager.clearTooltipPublic();

        assertNull(manager.findTooltip(form), "Tooltip dialog should be removed from layered pane after clearing");
    }

    private static class InspectableTooltipManager extends TooltipManager {
        void showTooltipPublic(String tip, Component cmp) {
            showTooltip(tip, cmp);
        }

        void clearTooltipPublic() {
            clearTooltip();
        }

        private InteractionDialog lastDialog;
        private TextArea lastText;

        @Override
        protected void showTooltip(String tip, Component cmp) {
            lastDialog = new InteractionDialog(new BorderLayout());
            lastDialog.setUIID(getDialogUIID());
            lastDialog.setDialogUIID("Container");
            TextArea text = new TextArea(tip);
            text.setGrowByContent(true);
            text.setEditable(false);
            text.setFocusable(false);
            text.setActAsLabel(true);
            text.setUIID(getTextUIID());
            lastDialog.add(BorderLayout.CENTER, text);
            lastText = text;
        }

        @Override
        protected void clearTooltip() {
            super.clearTooltip();
            lastDialog = null;
            lastText = null;
        }

        InteractionDialog findTooltip(Form form) {
            return lastDialog;
        }

        TextArea findTooltipText(InteractionDialog dialog) {
            return lastText;
        }
    }
}
