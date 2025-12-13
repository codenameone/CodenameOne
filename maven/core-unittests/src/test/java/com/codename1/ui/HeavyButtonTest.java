package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import static org.junit.jupiter.api.Assertions.*;

class HeavyButtonTest extends UITestBase {

    @FormTest
    void testHeavyButtonLifecycle() {
        implementation.setRequiresHeavyButton(true);
        implementation.resetHeavyButtonTracking();
        Form form = Display.getInstance().getCurrent();
        HeavyButton button = new HeavyButton("Copy");
        assertNotNull(button.peer);

        form.add(button);
        form.revalidate();
        TestCodenameOneImplementation.HeavyButtonPeerState state = implementation.getHeavyButtonPeerState(button.peer);
        assertNotNull(state);
        assertTrue(state.isInitCalled());

        final int[] fired = {0};
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                fired[0]++;
            }
        };
        button.addActionListener(listener);
        assertEquals(1, state.getListeners().size());

        button.setX(5);
        button.setY(6);
        button.setWidth(70);
        button.setHeight(30);
        form.revalidate();
        assertEquals(button.getAbsoluteX(), state.getX());
        assertEquals(button.getAbsoluteY(), state.getY());
        assertEquals(button.getWidth(), state.getWidth());
        assertEquals(button.getHeight(), state.getHeight());
        assertTrue(state.getUpdateCount() >= 1);

        button.removeActionListener(listener);
        assertTrue(state.getListeners().isEmpty());

        form.removeComponent(button);
        form.revalidate();
        assertTrue(state.isDeinitCalled());
    }
}
