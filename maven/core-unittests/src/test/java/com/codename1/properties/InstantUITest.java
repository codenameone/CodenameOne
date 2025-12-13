package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import org.junit.jupiter.api.Assertions;

public class InstantUITest extends UITestBase {

    public static class MyData implements PropertyBusinessObject {
        public final Property<String, MyData> name = new Property<>("name", String.class);
        public final Property<Boolean, MyData> active = new Property<>("active", Boolean.class);
        public final PropertyIndex idx = new PropertyIndex(this, "MyData", name, active);

        @Override
        public PropertyIndex getPropertyIndex() {
            return idx;
        }
    }

    @FormTest
    public void testCreateEditUI() {
        MyData data = new MyData();
        data.name.set("Test");
        data.active.set(true);

        InstantUI iui = new InstantUI();
        Container ui = iui.createEditUI(data, true);

        Assertions.assertNotNull(ui);
        Assertions.assertTrue(ui.getComponentCount() > 0);

        UiBinding.Binding binding = iui.getBindings(ui);
        Assertions.assertNotNull(binding);
    }
}
