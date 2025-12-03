package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.DisplayTest;

import static org.junit.jupiter.api.Assertions.*;

class BadgesSampleTest extends UITestBase {

    @FormTest
    void badgeTextIsInitializedWithPadding() {
        BadgesHarness harness = new BadgesHarness();
        Form form = harness.createForm();
        try {
            form.show();
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            Label label = harness.getBadgeLabel();
            Style style = label.getStyle();

            assertEquals("1", label.getBadgeText());
            assertEquals(Style.UNIT_TYPE_PIXELS, style.getPaddingUnit()[Component.RIGHT]);
            assertEquals(CN.convertToPixels(1.5f), style.getPaddingRight(false));
        } finally {
            harness.cleanup();
        }
    }

    @FormTest
    void messageEventsIncrementBadgeThroughDisplay() {
        BadgesHarness harness = new BadgesHarness();
        Form form = harness.createForm();
        try {
            form.show();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            MessageEvent firstEvent = implementation.fireMessageEvent(this, BadgesHarness.NEW_MESSAGE, 2);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            assertSame(firstEvent, harness.getLastMessageEvent());
            assertTrue(firstEvent.isConsumed());
            assertEquals("3", harness.getBadgeText());

            MessageEvent secondEvent = implementation.fireMessageEvent(this, BadgesHarness.NEW_MESSAGE, 4);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            assertSame(secondEvent, harness.getLastMessageEvent());
            assertTrue(secondEvent.isConsumed());
            assertEquals("7", harness.getBadgeText());
        } finally {
            harness.cleanup();
        }
    }

    private static class BadgesHarness {
        private static final String NEW_MESSAGE = "badge:new";

        private final Label badgeLabel = new Label("Hi World");
        private final ActionListener<MessageEvent> messageListener;
        private MessageEvent lastMessageEvent;
        private int badgeCount = 1;

        BadgesHarness() {
            messageListener = new ActionListener<MessageEvent>() {
                public void actionPerformed(MessageEvent evt) {
                    if (!NEW_MESSAGE.equals(evt.getMessage())) {
                        return;
                    }
                    lastMessageEvent = evt;
                    evt.consume();
                    int delta = evt.getCode();
                    if (delta <= 0) {
                        delta = 1;
                    }
                    badgeCount += delta;
                    badgeLabel.setBadgeText(String.valueOf(badgeCount));
                }
            };
        }

        Form createForm() {
            Form form = new Form("Hi World", BoxLayout.y());
            Style style = badgeLabel.getStyle();
            style.setPaddingUnitRight(Style.UNIT_TYPE_PIXELS);
            style.setPaddingRight(CN.convertToPixels(1.5f));
            badgeLabel.setBadgeText(String.valueOf(badgeCount));
            CN.addMessageListener(messageListener);
            form.add(FlowLayout.encloseIn(badgeLabel));
            return form;
        }

        Label getBadgeLabel() {
            return badgeLabel;
        }

        String getBadgeText() {
            return badgeLabel.getBadgeText();
        }

        MessageEvent getLastMessageEvent() {
            return lastMessageEvent;
        }

        void cleanup() {
            CN.removeMessageListener(messageListener);
        }
    }
}

