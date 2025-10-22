package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoCompleteTextComponentTest extends UITestBase {

    private List<String> filtered;
    private AutoCompleteTextComponent component;

    @BeforeEach
    void createComponent() {
        ListModel<String> model = new DefaultListModel<String>("one", "two", "three");
        filtered = new ArrayList<String>();
        component = new AutoCompleteTextComponent(model, new AutoCompleteTextComponent.AutoCompleteFilter() {
            @Override
            public boolean filter(String text) {
                filtered.add(text);
                return text != null && text.length() > 0;
            }
        });
    }

    @Test
    void customFilterReceivesText() throws Exception {
        AutoCompleteTextField field = component.getAutoCompleteField();
        Method filterMethod = AutoCompleteTextField.class.getDeclaredMethod("filter", String.class);
        filterMethod.setAccessible(true);
        Boolean result = (Boolean) filterMethod.invoke(field, "hello");
        assertTrue(result.booleanValue());
        assertEquals(Arrays.asList("hello"), filtered);
    }

    @Test
    void propertyMetadataMatchesComponentContract() {
        assertArrayEquals(new String[]{"text", "label", "hint", "multiline", "columns", "rows", "constraint"}, component.getPropertyNames());
        assertArrayEquals(new Class[]{String.class, String.class, String.class, Boolean.class, Integer.class, Integer.class, Integer.class}, component.getPropertyTypes());
        assertArrayEquals(new String[]{"String", "String", "String", "Boolean", "Integer", "Integer", "Integer"}, component.getPropertyTypeNames());
    }

    @Test
    void settersUpdateUnderlyingField() {
        component.text("value").label("Label").hint("Hint").multiline(true).columns(5).rows(3).constraint(TextArea.EMAILADDR);
        AutoCompleteTextField field = component.getAutoCompleteField();
        assertEquals("value", field.getText());
        assertEquals("Label", component.getLabel().getText());
        assertEquals("Hint", field.getHint());
        assertFalse(field.isSingleLineTextArea());
        assertEquals(5, field.getColumns());
        assertEquals(3, field.getRows());
        assertEquals(TextArea.EMAILADDR, field.getConstraint());
    }

    @Test
    void setPropertyValueDelegatesToField() {
        component.setPropertyValue("text", "abc");
        component.setPropertyValue("hint", "def");
        component.setPropertyValue("multiline", Boolean.TRUE);
        component.setPropertyValue("columns", Integer.valueOf(7));
        component.setPropertyValue("rows", Integer.valueOf(4));
        component.setPropertyValue("constraint", Integer.valueOf(TextArea.NUMERIC));

        AutoCompleteTextField field = component.getAutoCompleteField();
        assertEquals("abc", component.getText());
        assertEquals("def", field.getHint());
        assertFalse(field.isSingleLineTextArea());
        assertEquals(7, field.getColumns());
        assertEquals(4, field.getRows());
        assertEquals(TextArea.NUMERIC, field.getConstraint());

        assertEquals("abc", component.getPropertyValue("text"));
        assertEquals("def", component.getPropertyValue("hint"));
        assertEquals(Boolean.TRUE, component.getPropertyValue("multiline"));
        assertEquals(Integer.valueOf(7), component.getPropertyValue("columns"));
        assertEquals(Integer.valueOf(4), component.getPropertyValue("rows"));
        assertEquals(Integer.valueOf(TextArea.NUMERIC), component.getPropertyValue("constraint"));
    }

    @Test
    void constructUICreatesAnimationLayerWhenFocusAnimationEnabled() throws Exception {
        component.onTopMode(true).focusAnimation(true).label("Animated");
        component.constructUI();

        assertEquals(2, component.getComponentCount());
        assertEquals("Animated", component.getLabel().getText());
        assertFalse(component.getLabel().isVisible());
        assertEquals("Animated", component.getField().getHint());

        java.lang.reflect.Field animationLayerField = AutoCompleteTextComponent.class.getDeclaredField("animationLayer");
        animationLayerField.setAccessible(true);
        Object animationLayer = animationLayerField.get(component);
        assertNotNull(animationLayer);
    }
}
