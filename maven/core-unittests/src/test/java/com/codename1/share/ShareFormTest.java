package com.codename1.share;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ShareFormTest extends UITestBase {

    @BeforeEach
    void setUpImageIO() {
        TestCodenameOneImplementation.getInstance().setImageIO(null);
    }

    @FormTest
    void testConstructorWithoutRecipientAddsMessageOnly() throws Exception {
        AtomicInteger postClicks = new AtomicInteger();
        Form contacts = new Form();
        ShareForm form = new ShareForm(contacts, "Share", null, "Hello", new RecordingListener(postClicks));

        assertEquals("Share", form.getTitle());
        assertEquals("Hello", form.getMessage());
        assertEquals("", form.getTo());

        TextField toField = getField(form, "to", TextField.class);
        assertFalse(form.getContentPane().contains(toField));

        TextArea messageField = getField(form, "message", TextArea.class);
        assertSame(form.getContentPane(), messageField.getParent());

        Button postButton = getField(form, "post", Button.class);
        triggerButton(postButton);
        assertEquals(1, postClicks.get());

        assertEquals(BorderLayout.class, form.getContentPane().getLayout().getClass());
    }

    @FormTest
    void testRecipientFieldIsAddedWhenProvided() throws Exception {
        Form contacts = new Form();
        ShareForm form = new ShareForm(contacts, "Share", "friend@example.com", "Message", new RecordingListener(new AtomicInteger()));

        assertEquals("friend@example.com", form.getTo());
        TextField toField = getField(form, "to", TextField.class);
        assertSame(form.getContentPane(), toField.getParent());
    }

    @FormTest
    void testImageFallbackDisplaysPathTextWhenScalingUnavailable() throws Exception {
        Form contacts = new Form();
        String imagePath = "missing-image.jpg";
        ShareForm form = new ShareForm(contacts, "Share", "friend@example.com", "Caption", imagePath, new RecordingListener(new AtomicInteger()));

        TextArea messageField = getField(form, "message", TextArea.class);
        Container body = (Container) messageField.getParent();
        assertNotNull(body);
        assertEquals(BorderLayout.class, body.getLayout().getClass());

        Label imageLabel = findFirstLabel(body);
        assertNotNull(imageLabel);
        assertEquals(imagePath, imageLabel.getText());
        assertSame(body, imageLabel.getParent());
    }

    @FormTest
    void testBackCommandInvokesContactsShowBack() {
        final AtomicInteger backCalls = new AtomicInteger();
        Form contacts = new Form() {
            @Override
            public void showBack() {
                backCalls.incrementAndGet();
            }
        };
        ShareForm form = new ShareForm(contacts, "Share", null, "Body", new RecordingListener(new AtomicInteger()));

        Command back = form.getBackCommand();
        assertNotNull(back);
        back.actionPerformed(null);
        assertEquals(1, backCalls.get());
    }

    private void triggerButton(Button button) throws Exception {
        Method fire = Button.class.getDeclaredMethod("fireActionEvent", int.class, int.class);
        fire.setAccessible(true);
        fire.invoke(button, 0, 0);
    }

    private Label findFirstLabel(Container root) {
        List<Component> children = root.getChildrenAsList(true);
        for (Component child : children) {
            if (child instanceof Label) {
                return (Label) child;
            }
            if (child instanceof Container) {
                Label nested = findFirstLabel((Container) child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static class RecordingListener implements ActionListener {
        private final AtomicInteger counter;

        RecordingListener(AtomicInteger counter) {
            this.counter = counter;
        }

        public void actionPerformed(ActionEvent evt) {
            counter.incrementAndGet();
        }
    }
}
