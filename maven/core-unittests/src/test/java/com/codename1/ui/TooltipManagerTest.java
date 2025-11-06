package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

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

        RecordingTooltipManager manager = new RecordingTooltipManager();
        TooltipManager.enableTooltips(manager);
        assertSame(manager, TooltipManager.getInstance(), "Custom manager should be stored as singleton");
    }

    @FormTest
    void showTooltipCreatesDialogWithUiids() {
        implementation.setBuiltinSoundsEnabled(false);
        RecordingTooltipManager manager = new RecordingTooltipManager();
        manager.setDialogUIID("CustomDialog");
        manager.setTextUIID("CustomText");
        TooltipManager.enableTooltips(manager);

        Form form = Display.getInstance().getCurrent();
        Label target = new Label("Hover");
        form.add(target);
        form.revalidate();

        manager.showTooltip("Hello", target);

        assertNotNull(manager.lastDialog, "Tooltip dialog should have been created");
        assertEquals("CustomDialog", manager.lastDialog.getUIID(), "Dialog UIID should match custom setting");
        assertNotNull(manager.lastText, "Tooltip text area should be created");
        assertEquals("CustomText", manager.lastText.getUIID(), "Tooltip text UIID should match custom setting");
    }

    @FormTest
    void clearTooltipDisposesDialogAndCancelsTimer() {
        implementation.setBuiltinSoundsEnabled(false);
        RecordingTooltipManager manager = new RecordingTooltipManager();
        TooltipManager.enableTooltips(manager);

        Form form = Display.getInstance().getCurrent();
        Label target = new Label("Hover");
        form.add(target);
        form.revalidate();

        manager.showTooltip("Hello", target);
        InteractionDialog dialog = manager.lastDialog;
        assertNotNull(dialog, "Tooltip dialog should exist before clearing");

        manager.clearTooltip();
        form.getAnimationManager().flush();

        assertFalse(dialog.isShowing(), "Tooltip dialog should be hidden after clearTooltip");
        Container layered = form.getLayeredPane(InteractionDialog.class, false);
        List<Component> components = layered.getChildrenAsList(true);
        boolean found = false;
        for (Component cmp : components) {
            if (cmp == dialog) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Tooltip dialog should be removed from layered pane after clearing");
    }

    private static class RecordingTooltipManager extends TooltipManager {
        InteractionDialog lastDialog;
        TextArea lastText;

        @Override
        protected void showTooltip(String tip, Component cmp) {
            super.showTooltip(tip, cmp);
            Form form = cmp.getComponentForm();
            if (form != null) {
                Container layered = form.getLayeredPane(InteractionDialog.class, false);
                List<Component> components = layered.getChildrenAsList(true);
                for (Component child : components) {
                    if (child instanceof InteractionDialog) {
                        lastDialog = (InteractionDialog) child;
                    }
                }
                if (lastDialog != null) {
                    Component center = lastDialog.getContentPane().getComponentAt(0);
                    if (center instanceof TextArea) {
                        lastText = (TextArea) center;
                    }
                }
            }
        }
    }
}
