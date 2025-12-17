package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
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
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCompleteTextComponentTest extends UITestBase {

    private ListModel<String> suggestionModel;
    private static final String[] BASE_SUGGESTIONS = new String[]{"alpha", "beta", "gamma"};

    @BeforeEach
    void initModel() {
        suggestionModel = new DefaultListModel<String>(BASE_SUGGESTIONS);
    }

    @FormTest
    void constructorAppliesCustomFilterAndProvidesEditorAccess() {
        Form form = new Form("AutoComplete", BoxLayout.y());

        final List<String> filtered = new ArrayList<String>();
        final AutoCompleteTextField field;
        AutoCompleteTextComponent.AutoCompleteFilter filter;
        AutoCompleteTextComponent component;
        final DefaultListModel<String> localModel = new DefaultListModel<String>();

        filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                filtered.add(text);
                DefaultListModel<String> model = localModel;
                if (model.getSize() > 0) {
                    model.removeAll();
                }
                if (text.length() == 0) {
                    return true;
                }
                ensureSuggestions(model);
                return text.length() > 0;
            }
        };
        component = new AutoCompleteTextComponent(localModel, filter);
        field = component.getAutoCompleteField();
        assertSame(field, component.getField(), "getField should expose the underlying AutoCompleteTextField");
        assertSame(field, component.getEditor(), "getEditor should return the AutoCompleteTextField instance");

        form.add(component);
        field.setMinimumLength(0);
        form.show();
        flushSerialCalls();
        implementation.tapComponent(field);
        flushSerialCalls();

        implementation.dispatchKeyPress('a');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('p');
        flushSerialCalls();

        ComponentSelector popupList = ComponentSelector.$("AutoCompleteList", form);
        assertTrue(popupList.size() > 0, "Popup should appear when filter accepts text");
        assertTrue(filtered.contains("a"));
        assertTrue(filtered.contains("al"));
        assertEquals("alp", filtered.get(filtered.size() - 1));

        implementation.dispatchKeyPress((char) 8);
        implementation.dispatchKeyPress((char) 8);
        implementation.dispatchKeyPress((char) 8);
        flushSerialCalls();
        assertEquals(BASE_SUGGESTIONS.length, localModel.getSize(), "AutoComplete should repopulate base suggestions after clearing text");
        ComponentSelector popupListAfterReject = ComponentSelector.$("AutoCompleteList", form);
        if (popupListAfterReject.size() > 0) {
            com.codename1.ui.List popup = (com.codename1.ui.List) popupListAfterReject.iterator().next();
            assertEquals(BASE_SUGGESTIONS.length, popup.getModel().getSize(), "Popup model should mirror the base suggestions when text is empty");
        }
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
        final DefaultListModel<String> colors = new DefaultListModel<String>();
        AutoCompleteTextComponent.AutoCompleteFilter filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                filteredInputs.add(text);
                if (text.trim().length() > 1) {
                    if (colors.getSize() == 0) {
                        colors.addItem("Red");
                        colors.addItem("Green");
                        colors.addItem("Blue");
                    }
                    return true;
                }
                if (colors.getSize() > 0) {
                    colors.removeAll();
                }
                return true;
            }
        };
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(colors, filter);
        final AutoCompleteTextField field = component.getAutoCompleteField();
        component.label("Color");
        component.hint("Type a color");
        form.add(component);
        field.setMinimumLength(2);
        form.revalidate();
        flushSerialCalls();
        implementation.tapComponent(field);
        flushSerialCalls();

        implementation.dispatchKeyPress('r');
        flushSerialCalls();
        assertEquals("r", field.getText());

        assertEquals(0, colors.getSize(), "Backing model should be empty when the filter rejects input");
        ComponentSelector listsAfterSingle = ComponentSelector.$("AutoCompleteList", form);
        if (listsAfterSingle.size() > 0) {
            com.codename1.ui.List firstPopup = (com.codename1.ui.List) listsAfterSingle.iterator().next();
            assertEquals(0, firstPopup.getModel().getSize(), "Popup model should be empty when the filter rejects input");
        }
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
        CountDownLatch latch = new CountDownLatch(1);
        popupList.addActionListener(e -> latch.countDown());
        Dimension cellSize = renderer.getListCellRendererComponent(popupList, firstValue, 0, true).getPreferredSize();
        int selectX = popupList.getAbsoluteX() + Math.max(1, Math.min(cellSize.getWidth(), popupList.getWidth()) / 2);
        int selectY = popupList.getAbsoluteY() + Math.max(1, Math.min(cellSize.getHeight(), popupList.getHeight()) / 2);
        implementation.dispatchPointerPressAndRelease(selectX, selectY);

        waitFor(latch, 400);

        assertEquals("Red", field.getText());
        assertEquals("Red", component.getText());
        assertTrue(filteredInputs.contains("re"));
    }

    private void ensureSuggestions(DefaultListModel<String> model) {
        if (model.getSize() == 0) {
            for (int i = 0; i < BASE_SUGGESTIONS.length; i++) {
                model.addItem(BASE_SUGGESTIONS[i]);
            }
        }
    }

    private enum AcceptAllFilter implements AutoCompleteTextComponent.AutoCompleteFilter {
        INSTANCE;

        public boolean filter(String text) {
            return true;
        }
    }
}
