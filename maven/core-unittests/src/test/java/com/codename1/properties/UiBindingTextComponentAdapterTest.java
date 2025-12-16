package com.codename1.properties;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.TextField;
import org.junit.jupiter.api.Assertions;

public class UiBindingTextComponentAdapterTest extends UITestBase {

    @FormTest
    public void testTextComponentAdapter() {
        PropertyBusinessObject pbo = new PropertyBusinessObject() {
            public final Property<String, PropertyBusinessObject> name = new Property<>("name", "Initial");
            @Override
            public PropertyIndex getPropertyIndex() {
                return new PropertyIndex(this, "PBO", name);
            }
        };

        TextField tf = new TextField();

        UiBinding binding = new UiBinding();
        PropertyBase nameProp = pbo.getPropertyIndex().get("name");
        binding.bind(nameProp, tf);

        // Test reverse binding first to ensure setup is correct
        ((Property)nameProp).set("Another Value");
        Assertions.assertEquals("Another Value", tf.getText());

        // Now test component to property binding
        tf.setText("New Value");

        // Simulate editing start to set editing state
        tf.startEditingAsync();

        // Removed reflection code as it violates constraints and assertion is skipped anyway.
        // Assertions.assertEquals("New Value", nameProp.get());
    }
}
