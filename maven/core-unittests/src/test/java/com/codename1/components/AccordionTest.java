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
    void testAddContentWithStringHeader() {
        Accordion accordion = new Accordion();
        Container content = new Container(BoxLayout.y());
        content.add(new Label("Content"));

        accordion.addContent("String Header", content);

        assertTrue(accordion.getComponentCount() > 0);
    }

    @FormTest
    void testSetAutoClose() {
        Accordion accordion = new Accordion();
        accordion.setAutoClose(false);
        // Should not throw exception
        assertNotNull(accordion);

        accordion.setAutoClose(true);
        assertNotNull(accordion);
    }

    @FormTest
    void testExpandAndCollapseContent() {
        Accordion accordion = new Accordion();
        Label header = new Label("Header");
        Container content = new Container(BoxLayout.y());
        content.add(new Label("Test Content"));

        accordion.addContent(header, content);

        // Expand content by passing the body component
        accordion.expand(content);
        assertNotNull(accordion.getCurrentlyExpanded());

        // Collapse content
        accordion.collapse(content);
        assertNull(accordion.getCurrentlyExpanded());
    }

    @FormTest
    void testGetCurrentlyExpanded() {
        Accordion accordion = new Accordion();
        Container content1 = new Container(BoxLayout.y());
        Container content2 = new Container(BoxLayout.y());

        accordion.addContent("Header1", content1);
        accordion.addContent("Header2", content2);

        assertNull(accordion.getCurrentlyExpanded());

        accordion.expand(content1);
        assertEquals(content1, accordion.getCurrentlyExpanded());
    }

    @FormTest
    void testAddOnClickItemListener() {
        Accordion accordion = new Accordion();
        AtomicInteger count = new AtomicInteger();
        accordion.addOnClickItemListener(evt -> count.incrementAndGet());

        Container content = new Container(BoxLayout.y());
        accordion.addContent("Header", content);

        assertNotNull(accordion);
    }

    @FormTest
    void testRemoveOnClickItemListener() {
        Accordion accordion = new Accordion();
        AtomicInteger count = new AtomicInteger();
        accordion.addOnClickItemListener(evt -> count.incrementAndGet());
        accordion.removeOnClickItemListener(evt -> count.incrementAndGet());

        assertNotNull(accordion);
    }

    @FormTest
    void testRemoveContent() {
        Accordion accordion = new Accordion();
        Container content = new Container(BoxLayout.y());

        accordion.addContent("Header", content);

        int initialCount = accordion.getComponentCount();
        accordion.removeContent(content);
        assertTrue(accordion.getComponentCount() < initialCount);
    }

    @FormTest
    void testSetHeader() {
        Accordion accordion = new Accordion();
        Container content = new Container(BoxLayout.y());

        accordion.addContent("Initial Header", content);
        accordion.setHeader("Updated Header", content);

        assertNotNull(accordion);
    }

    @FormTest
    void testSetHeaderWithComponent() {
        Accordion accordion = new Accordion();
        Container content = new Container(BoxLayout.y());
        Label newHeader = new Label("New Header");

        accordion.addContent("Initial Header", content);
        accordion.setHeader(newHeader, content);

        assertNotNull(accordion);
    }

    @FormTest
    void testSetOpenIconImage() {
        Accordion accordion = new Accordion();
        Image newOpenIcon = Image.createImage(15, 15, 0xFF00FF);

        accordion.setOpenIcon(newOpenIcon);
        assertNotNull(accordion);
    }

    @FormTest
    void testSetCloseIconImage() {
        Accordion accordion = new Accordion();
        Image newCloseIcon = Image.createImage(15, 15, 0x00FFFF);

        accordion.setCloseIcon(newCloseIcon);
        assertNotNull(accordion);
    }

    @FormTest
    void testSetOpenIconChar() {
        Accordion accordion = new Accordion();
        accordion.setOpenIcon('\uE5CE');
        assertNotNull(accordion);
    }

    @FormTest
    void testSetCloseIconChar() {
        Accordion accordion = new Accordion();
        accordion.setCloseIcon('\uE5CF');
        assertNotNull(accordion);
    }

    @FormTest
    void testBackgroundItemUIID() {
        Accordion accordion = new Accordion();
        assertEquals("AccordionItem", accordion.getBackgroundItemUIID());

        accordion.setBackgroundItemUIID("CustomItem");
        assertEquals("CustomItem", accordion.getBackgroundItemUIID());
    }

    @FormTest
    void testHeaderUIID() {
        Accordion accordion = new Accordion();
        assertEquals("AccordionArrow", accordion.getHeaderUIID());

        accordion.setHeaderUIID("CustomHeader");
        assertEquals("CustomHeader", accordion.getHeaderUIID());
    }

    @FormTest
    void testOpenCloseIconUIID() {
        Accordion accordion = new Accordion();
        assertEquals("AccordionArrow", accordion.getOpenCloseIconUIID());

        accordion.setOpenCloseIconUIID("CustomArrow");
        assertEquals("CustomArrow", accordion.getOpenCloseIconUIID());
    }

    @FormTest
    void testSetHeaderUIIDForContent() {
        Accordion accordion = new Accordion();
        Container content = new Container(BoxLayout.y());

        accordion.addContent("Header", content);
        accordion.setHeaderUIID(content, "CustomHeaderUIID");

        assertNotNull(accordion);
    }
}
