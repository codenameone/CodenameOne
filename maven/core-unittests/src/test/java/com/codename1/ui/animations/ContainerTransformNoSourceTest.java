package com.codename1.ui.animations;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContainerTransformNoSourceTest extends UITestBase {

    @Test
    void aTransitionWithNoSourceFormDoesNotThrow() {
        // The first Form shown has nothing to transition from; the Transition contract
        // allows a null source, and dereferencing it made that first show throw.
        Form destination = new Form();
        destination.setWidth(200);
        destination.setHeight(300);
        final ContainerTransformTransition t = ContainerTransformTransition.create("anything", 200);
        t.init(null, destination);
        assertDoesNotThrow(t::initTransition);
        assertFalse(t.animate(), "with nothing to transform from there is no animation");
    }
}
