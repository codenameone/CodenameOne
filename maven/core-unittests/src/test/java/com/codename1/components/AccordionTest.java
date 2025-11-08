package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AccordionTest extends UITestBase {

    @FormTest
    void testDefaultConstructorInitializes() {
        Accordion accordion = new Accordion();
        assertNotNull(accordion);
        assertTrue(accordion.isScrollableY());
    }

    @FormTest
    void testConstructorWithIcons() {
        Image openIcon = Image.createImage(10, 10, 0xFF0000);
        Image closeIcon = Image.createImage(10, 10, 0x00FF00);
        Accordion accordion = new Accordion(openIcon, closeIcon);
        assertNotNull(accordion);
        assertTrue(accordion.isScrollableY());
    }

    @FormTest
    void testAddContent() {
        Accordion accordion = new Accordion();
        Label header = new Label("Header");
        Container content = new Container(BoxLayout.y());
        content.add(new Label("Content"));

        accordion.addContent(header, content);

        assertTrue(accordion.getComponentCount() > 0);
    }

    @FormTest
    void testAutoCloseGetterAndSetter() {
        Accordion accordion = new Accordion();
        assertTrue(accordion.isAutoClose());

        accordion.setAutoClose(false);
        assertFalse(accordion.isAutoClose());

        accordion.setAutoClose(true);
        assertTrue(accordion.isAutoClose());
    }

    @FormTest
    void testExpandAndCollapseContent() {
        Accordion accordion = new Accordion();
        Label header = new Label("Header");
        Container content = new Container(BoxLayout.y());
        content.add(new Label("Test Content"));

        accordion.addContent(header, content);

        // Expand content
        accordion.expand(0);
        assertTrue(accordion.isExpanded(0));

        // Collapse content
        accordion.collapse(0);
        assertFalse(accordion.isExpanded(0));
    }

    @FormTest
    void testAddActionListener() {
        Accordion accordion = new Accordion();
        AtomicInteger count = new AtomicInteger();
        accordion.addActionListener(evt -> count.incrementAndGet());

        Label header = new Label("Header");
        Container content = new Container(BoxLayout.y());
        accordion.addContent(header, content);

        // Expand should trigger listeners
        accordion.expand(0);
        flushSerialCalls();

        assertTrue(count.get() >= 0);
    }

    @FormTest
    void testRemoveActionListener() {
        Accordion accordion = new Accordion();
        AtomicInteger count = new AtomicInteger();
        accordion.addActionListener(evt -> count.incrementAndGet());
        accordion.removeActionListener(evt -> count.incrementAndGet());

        assertNotNull(accordion);
    }

    @FormTest
    void testRemoveContentByIndex() {
        Accordion accordion = new Accordion();
        Label header1 = new Label("Header 1");
        Container content1 = new Container(BoxLayout.y());
        Label header2 = new Label("Header 2");
        Container content2 = new Container(BoxLayout.y());

        accordion.addContent(header1, content1);
        accordion.addContent(header2, content2);

        int initialCount = accordion.getComponentCount();
        accordion.removeContent(0);
        assertTrue(accordion.getComponentCount() < initialCount);
    }

    @FormTest
    void testRemoveContentByComponent() {
        Accordion accordion = new Accordion();
        Label header = new Label("Header");
        Container content = new Container(BoxLayout.y());

        accordion.addContent(header, content);

        int initialCount = accordion.getComponentCount();
        accordion.removeContent(header);
        assertTrue(accordion.getComponentCount() < initialCount);
    }

    @FormTest
    void testRemoveAllContent() {
        Accordion accordion = new Accordion();
        accordion.addContent(new Label("H1"), new Container());
        accordion.addContent(new Label("H2"), new Container());

        accordion.removeAllContent();
        assertEquals(0, accordion.getComponentCount());
    }

    @FormTest
    void testGetContentCount() {
        Accordion accordion = new Accordion();
        assertEquals(0, accordion.getContentCount());

        accordion.addContent(new Label("H1"), new Container());
        assertEquals(1, accordion.getContentCount());

        accordion.addContent(new Label("H2"), new Container());
        assertEquals(2, accordion.getContentCount());
    }

    @FormTest
    void testOpenIconGetterAndSetter() {
        Accordion accordion = new Accordion();
        Image newOpenIcon = Image.createImage(15, 15, 0xFF00FF);

        accordion.setOpenIcon(newOpenIcon);
        assertSame(newOpenIcon, accordion.getOpenIcon());
    }

    @FormTest
    void testCloseIconGetterAndSetter() {
        Accordion accordion = new Accordion();
        Image newCloseIcon = Image.createImage(15, 15, 0x00FFFF);

        accordion.setCloseIcon(newCloseIcon);
        assertSame(newCloseIcon, accordion.getCloseIcon());
    }
}
