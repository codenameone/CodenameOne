package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Image;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class ComboBoxTest extends UITestBase {

    @Test
    void testDefaultRendererUiidsAreApplied() {
        ComboBox<String> comboBox = new ComboBox<String>("A", "B");
        assertEquals("ComboBox", comboBox.getUIID());

        DefaultListCellRenderer renderer = (DefaultListCellRenderer) comboBox.getRenderer();
        assertEquals("ComboBoxItem", renderer.getUIID());
        Component focus = renderer.getListFocusComponent(comboBox);
        assertNotNull(focus);
        assertEquals("ComboBoxFocus", focus.getUIID());
    }

    @Test
    void testSetUiidPropagatesToRendererComponents() {
        ComboBox<String> comboBox = new ComboBox<String>("One", "Two");
        comboBox.setUIID("MyCombo");

        DefaultListCellRenderer renderer = (DefaultListCellRenderer) comboBox.getRenderer();
        assertEquals("MyComboItem", renderer.getUIID());
        Component focus = renderer.getListFocusComponent(comboBox);
        assertNotNull(focus);
        assertEquals("MyComboFocus", focus.getUIID());
    }

    @Test
    void testStaticDefaultsImpactNewInstances() {
        boolean originalInclude = ComboBox.isDefaultIncludeSelectCancel();
        boolean originalSpinner = ComboBox.isDefaultActAsSpinnerDialog();
        try {
            ComboBox.setDefaultIncludeSelectCancel(false);
            ComboBox.setDefaultActAsSpinnerDialog(true);

            ComboBox<String> comboBox = new ComboBox<String>();
            assertFalse(comboBox.isIncludeSelectCancel());
            assertTrue(comboBox.isActAsSpinnerDialog());
        } finally {
            ComboBox.setDefaultIncludeSelectCancel(originalInclude);
            ComboBox.setDefaultActAsSpinnerDialog(originalSpinner);
        }
    }

    @Test
    void testCreatePopupListUsesPopupUiidsWhenThemeRequests() {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@otherPopupRendererBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        ExposedComboBox<String> comboBox = new ExposedComboBox<String>(new DefaultListModel<String>("X", "Y"));
        com.codename1.ui.List<String> popupList = comboBox.createPopupListPublic();

        assertEquals("ComboBoxList", popupList.getUIID());
        DefaultListCellRenderer renderer = (DefaultListCellRenderer) popupList.getRenderer();
        assertEquals("PopupItem", renderer.getUIID());
        assertEquals("PopupFocus", renderer.getListFocusComponent(popupList).getUIID());
    }

    @Test
    void testMutableFlagsCanBeAdjustedPerInstance() {
        ComboBox<String> comboBox = new ComboBox<String>();
        comboBox.setIncludeSelectCancel(false);
        comboBox.setActAsSpinnerDialog(true);
        comboBox.setComboBoxImage(Image.createImage(5, 5));

        assertFalse(comboBox.isIncludeSelectCancel());
        assertTrue(comboBox.isActAsSpinnerDialog());
        assertNotNull(comboBox.getComboBoxImage());
    }

    private static class ExposedComboBox<T> extends ComboBox<T> {
        ExposedComboBox(ListModel<T> model) {
            super(model);
        }

        com.codename1.ui.List<T> createPopupListPublic() {
            return super.createPopupList();
        }
    }
}
