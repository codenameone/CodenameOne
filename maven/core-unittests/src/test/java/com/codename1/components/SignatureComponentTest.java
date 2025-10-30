package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.events.ActionListener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SignatureComponentTest extends UITestBase {
    @FormTest
    void testPreferredSizeUsesDisplayConversions() {
        SignatureComponent component = new SignatureComponent();
        Dimension preferred = component.getPreferredSize();
        assertEquals(75, preferred.getWidth());
        assertEquals(40, preferred.getHeight());
    }

    @FormTest
    void testSetSignatureImageScalesIconAndClearsLeadText() throws Exception {
        SignatureComponent component = new SignatureComponent();
        Button lead = getLeadButton(component);
        lead.setWidth(120);
        lead.setHeight(60);
        lead.getStyle().setPadding(0, 0, 0, 0);

        StubImage original = new StubImage(200, 100);
        component.setSignatureImage(original);

        assertEquals("", lead.getText(), "Lead text should be cleared when an image is applied");
        Image icon = lead.getIcon();
        assertNotNull(icon, "Lead button should receive an icon when an image is set");
        assertEquals(120, icon.getWidth(), "Scaled icon width should match available width");
        assertEquals(60, icon.getHeight(), "Scaled icon height should match available height");
    }

    @FormTest
    void testSetSignatureImageNullRestoresLeadState() throws Exception {
        SignatureComponent component = new SignatureComponent();
        Button lead = getLeadButton(component);
        lead.setWidth(120);
        lead.setHeight(60);
        lead.getStyle().setPadding(0, 0, 0, 0);

        component.setSignatureImage(new StubImage(120, 60));
        component.setSignatureImage(null);

        assertEquals("Press to sign", lead.getText());
        assertNull(lead.getIcon());
    }

    @FormTest
    void testGetSignatureImageReturnsStoredImage() {
        SignatureComponent component = new SignatureComponent();
        StubImage image = new StubImage(50, 25);
        component.setSignatureImage(image);
        assertSame(image, component.getSignatureImage());

        component.setSignatureImage(null);
        assertNull(component.getSignatureImage());
    }

    @FormTest
    void testActionListenersFireAndCanBeRemoved() throws Exception {
        SignatureComponent component = new SignatureComponent();
        AtomicInteger counter = new AtomicInteger();
        ActionListener listener = evt -> counter.incrementAndGet();
        component.addActionListener(listener);

        invokeFireActionEvent(component);
        assertEquals(1, counter.get());

        component.removeActionListener(listener);
        invokeFireActionEvent(component);
        assertEquals(1, counter.get(), "Removed listener should no longer fire");
    }

    private void invokeFireActionEvent(SignatureComponent component) throws Exception {
        Method fire = SignatureComponent.class.getDeclaredMethod("fireActionEvent");
        fire.setAccessible(true);
        fire.invoke(component);
    }

    private Button getLeadButton(SignatureComponent component) throws Exception {
        Field leadField = SignatureComponent.class.getDeclaredField("lead");
        leadField.setAccessible(true);
        return (Button) leadField.get(component);
    }

    private static class StubImage extends Image {
        private final int width;
        private final int height;

        StubImage(int width, int height) {
            super(new Object());
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Image scaled(int width, int height) {
            return new StubImage(width, height);
        }

        @Override
        public Image scaledSmallerRatio(int width, int height) {
            return new StubImage(width, height);
        }

        @Override
        public boolean animate() {
            return false;
        }
    }
}
