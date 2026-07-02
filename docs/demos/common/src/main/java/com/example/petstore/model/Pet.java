package com.example.petstore.model;

import com.codename1.properties.Property;
import com.codename1.properties.PropertyBusinessObject;
import com.codename1.properties.PropertyIndex;

// tag::appendix-goal-generate-openapi-java-002[]
public class Pet implements PropertyBusinessObject {
    public final Property<Long, Pet> id = new Property<Long, Pet>("id");
    public final Property<String, Pet> name = new Property<String, Pet>("name");
    private final PropertyIndex index = new PropertyIndex(this, "Pet", id, name);

    @Override
    public PropertyIndex getPropertyIndex() {
        return index;
    }
}
// end::appendix-goal-generate-openapi-java-002[]
