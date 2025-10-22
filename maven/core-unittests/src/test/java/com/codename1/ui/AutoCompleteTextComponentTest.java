package com.codename1.ui;

import com.codename1.components.ComponentTestBase;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoCompleteTextComponentTest extends ComponentTestBase {

    private ListModel<String> suggestionModel;

    @BeforeEach
    void initModel() {
        suggestionModel = new DefaultListModel<String>(new String[]{"alpha", "beta", "gamma"});
    }

    @Test
    void constructorAppliesCustomFilterAndProvidesEditorAccess() throws Exception {
        final List<String> filtered = new ArrayList<String>();
        AutoCompleteTextComponent.AutoCompleteFilter filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                filtered.add(text);
                return text.length() > 0;
            }
        };
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, filter);
        AutoCompleteTextField field = component.getAutoCompleteField();
        Method filterMethod = AutoCompleteTextField.class.getDeclaredMethod("filter", String.class);
        filterMethod.setAccessible(true);
        Boolean result = (Boolean) filterMethod.invoke(field, "alp");
        assertTrue(result.booleanValue(), "Custom filter should return its result");
        assertEquals(1, filtered.size(), "Filter should be invoked with provided text");
        assertEquals("alp", filtered.get(0));
        assertSame(field, component.getField(), "getField should expose the underlying AutoCompleteTextField");
        assertSame(field, component.getEditor(), "getEditor should return the AutoCompleteTextField instance");
    }

    @Test
    void focusAnimationFollowsThemeAndManualOverrides() {
        Hashtable theme = new Hashtable();
        theme.put("textComponentAnimBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        AutoCompleteTextComponent component = new AutoCompleteTextComponent(suggestionModel, AcceptAllFilter.INSTANCE);
        assertTrue(component.isFocusAnimation(), "Theme constant should enable focus animation by default");

        component.focusAnimation(false);
        assertFalse(component.isFocusAnimation(), "Explicit false should override theme");

        component.focusAnimation(true);
        assertTrue(component.isFocusAnimation(), "Explicit true should override theme");
    }

    @Test
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

    @Test
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

    private enum AcceptAllFilter implements AutoCompleteTextComponent.AutoCompleteFilter {
        INSTANCE;

        public boolean filter(String text) {
            return true;
        }
    }
}
