package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.layouts.BoxLayout;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCompleteTextComponentTest extends UITestBase {

    private ListModel<String> suggestionModel;

    @BeforeEach
    void initModel() {
        suggestionModel = new DefaultListModel<String>(new String[]{"alpha", "beta", "gamma"});
    }

    @FormTest
    void constructorAppliesCustomFilterAndProvidesEditorAccess() {
        Form form = new Form("AutoComplete", BoxLayout.y());

        final List<String> filtered = new ArrayList<String>();
        AutoCompleteTextComponent.AutoCompleteFilter filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                filtered.add(text);
                return text.length() > 0;
            }
        };
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, filter);
        AutoCompleteTextField field = component.getAutoCompleteField();
        assertSame(field, component.getField(), "getField should expose the underlying AutoCompleteTextField");
        assertSame(field, component.getEditor(), "getEditor should return the AutoCompleteTextField instance");

        form.add(component);
        form.show();
        flushSerialCalls();

        field.setMinimumLength(1);
        field.setText("alp");
        flushSerialCalls();
        ComponentSelector popupList = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(1, popupList.size(), "Popup should appear when filter accepts text");
        assertEquals("alp", filtered.get(0));

        field.setText("");
        flushSerialCalls();
        ComponentSelector popupListAfterReject = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(0, popupListAfterReject.size(), "Filter returning false should prevent popup visibility");
        assertTrue(filtered.contains(""));
    }

    @FormTest
    void focusAnimationFollowsThemeAndManualOverrides() {
        Hashtable theme = new Hashtable();
        theme.put("@textComponentAnimBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, AcceptAllFilter.INSTANCE);
        assertTrue(component.isFocusAnimation(), "Theme constant should enable focus animation by default");

        component.focusAnimation(false);
        assertFalse(component.isFocusAnimation(), "Explicit false should override theme");

        component.focusAnimation(true);
        assertTrue(component.isFocusAnimation(), "Explicit true should override theme");
    }

    @FormTest
    void fluentSettersUpdateUnderlyingField() {
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, AcceptAllFilter.INSTANCE);
        Image hintIcon = Image.createImage(2, 2);

        component
                .text("username")
                .hint("Enter username")
                .hint(hintIcon)
                .multiline(true)
                .columns(12)
                .rows(3)
                .constraint(TextArea.EMAILADDR);

        AutoCompleteTextField field = component.getAutoCompleteField();
        assertEquals("username", field.getText());
        assertEquals("Enter username", field.getHint());
        assertFalse(field.isSingleLineTextArea(), "Multiline true should mark the field as multi-line");
        assertEquals(12, field.getColumns());
        assertEquals(3, field.getRows());
        assertEquals(TextArea.EMAILADDR, field.getConstraint());
        assertSame(hintIcon, field.getHintIcon());
    }

    @FormTest
    void propertyMetadataAndValuesReflectFieldState() {
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, AcceptAllFilter.INSTANCE);
        component
                .text("initial")
                .label("Label")
                .hint("Hint")
                .multiline(true)
                .columns(7)
                .rows(4)
                .constraint(TextArea.NUMERIC);

        assertArrayEquals(new String[]{"text", "label", "hint", "multiline", "columns", "rows", "constraint"}, component.getPropertyNames());
        assertArrayEquals(new Class[]{String.class, String.class, String.class, Boolean.class, Integer.class, Integer.class, Integer.class}, component.getPropertyTypes());
        assertArrayEquals(new String[]{"String", "String", "String", "Boolean", "Integer", "Integer", "Integer"}, component.getPropertyTypeNames());

        assertEquals("initial", component.getPropertyValue("text"));
        assertEquals("Hint", component.getPropertyValue("hint"));
        assertEquals(Boolean.TRUE, component.getPropertyValue("multiline"));
        assertEquals(Integer.valueOf(7), component.getPropertyValue("columns"));
        assertEquals(Integer.valueOf(4), component.getPropertyValue("rows"));
        assertEquals(Integer.valueOf(TextArea.NUMERIC), component.getPropertyValue("constraint"));

        component.setPropertyValue("text", "updated");
        component.setPropertyValue("hint", "Another");
        component.setPropertyValue("multiline", Boolean.FALSE);
        component.setPropertyValue("columns", Integer.valueOf(5));
        component.setPropertyValue("rows", Integer.valueOf(2));
        component.setPropertyValue("constraint", Integer.valueOf(TextArea.PHONENUMBER));

        AutoCompleteTextField field = component.getAutoCompleteField();
        assertEquals("updated", field.getText());
        assertEquals("Another", field.getHint());
        assertTrue(field.isSingleLineTextArea(), "Setting multiline false should restore single line mode");
        assertEquals(5, field.getColumns());
        assertEquals(2, field.getRows());
        assertEquals(TextArea.PHONENUMBER, field.getConstraint());
    }

    @FormTest
    void autoCompleteComponentRequiresMultipleCharactersForPopup() {
        Form form = new Form("AutoComplete", BoxLayout.y());
        form.show();
        flushSerialCalls();

        final List<String> filteredInputs = new ArrayList<String>();
        AutoCompleteTextComponent.AutoCompleteFilter filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                filteredInputs.add(text);
                return text.trim().length() > 1;
            }
        };

        ListModel<String> colors = new DefaultListModel<String>(new String[]{"Red", "Green", "Blue"});
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(colors, filter);
        component.label("Color");
        component.hint("Type a color");
        form.add(component);
        form.revalidate();
        flushSerialCalls();

        AutoCompleteTextField field = component.getAutoCompleteField();
        field.setMinimumLength(2);
        implementation.tapComponent(field);
        flushSerialCalls();

        implementation.dispatchKeyPress('r');
        flushSerialCalls();
        assertEquals("r", field.getText());

        ComponentSelector listsAfterSingle = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(0, listsAfterSingle.size(), "Popup should remain hidden when the filter rejects input");
        assertTrue(filteredInputs.contains("r"));

        implementation.dispatchKeyPress('e');
        form.getAnimationManager().flush();
        flushSerialCalls();

        ComponentSelector listsAfterDouble = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(1, listsAfterDouble.size(), "Popup should appear after entering enough text");

        com.codename1.ui.List popupList = (com.codename1.ui.List) listsAfterDouble.iterator().next();
        assertTrue(popupList.isVisible());
        assertTrue(popupList.getModel().getSize() > 0);

        Object firstValue = popupList.getModel().getItemAt(0);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ListCellRenderer renderer = (ListCellRenderer) popupList.getRenderer();
        Dimension cellSize = renderer.getListCellRendererComponent(popupList, firstValue, 0, true).getPreferredSize();
        int selectX = popupList.getAbsoluteX() + Math.max(1, Math.min(cellSize.getWidth(), popupList.getWidth()) / 2);
        int selectY = popupList.getAbsoluteY() + Math.max(1, Math.min(cellSize.getHeight(), popupList.getHeight()) / 2);
        implementation.dispatchPointerPressAndRelease(selectX, selectY);
        flushSerialCalls();

        if (!"Red".equals(field.getText())) {
            popupList.setSelectedIndex(0);
            popupList.fireActionEvent();
            flushSerialCalls();
        }

        assertEquals("Red", field.getText());
        assertEquals("Red", component.getText());
        assertTrue(filteredInputs.contains("re"));
    }

    private enum AcceptAllFilter implements AutoCompleteTextComponent.AutoCompleteFilter {
        INSTANCE;

        public boolean filter(String text) {
            return true;
        }
    }
}
