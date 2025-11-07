package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.xml.Element;

import static org.junit.jupiter.api.Assertions.*;

class UIFragmentTest extends UITestBase {

    @FormTest
    void testParseXmlAndInjectParameters() {
        String xml = "<border uiid='Root' id='root'>"
                + "<y constraint='center'>"
                + "<label id='greeting'>Hello</label>"
                + "<$action constraint='south'/>"
                + "</y>"
                + "</border>";
        UIFragment fragment = UIFragment.parseXML(xml);
        Button action = new Button("Tap");
        fragment.set("action", action);

        Container view = fragment.getView();
        assertTrue(view.getLayout() instanceof BorderLayout);
        Component greeting = fragment.findById("greeting");
        assertNotNull(greeting);
        assertTrue(greeting instanceof Label);
        assertEquals("Hello", ((Label) greeting).getText());
        assertEquals("action", action.getName());

        Container firstView = view;
        Button newAction = new Button("New");
        fragment.set("action", newAction);
        Container secondView = fragment.getView();
        assertNotSame(firstView, secondView);
        assertEquals("action", newAction.getName());
    }

    @FormTest
    void testParseJsonCreatesLayout() {
        String json = "{id:'root', c:$center, s:{x:['One','Two']}}";
        UIFragment fragment = UIFragment.parseJSON(json);
        Label center = new Label("Center");
        fragment.set("center", center);

        Container view = fragment.getView();
        assertTrue(view.getLayout() instanceof BorderLayout);
        assertSame(center.getParent(), view);
        Component southContainer = view.getComponentAt(0);
        if (southContainer == center) {
            southContainer = view.getComponentAt(1);
        }
        assertTrue(southContainer instanceof Container);
        assertTrue(((Container) southContainer).getLayout() instanceof BoxLayout);
    }

    @FormTest
    void testCustomFactoryDecoration() {
        String xml = "<y id='root'><label id='first'>Hi</label></y>";
        UIFragment fragment = UIFragment.parseXML(xml);
        fragment.setFactory(new UIFragment.DefaultComponentFactory() {
            @Override
            public Component newComponent(Element el) {
                Component cmp = super.newComponent(el);
                cmp.setName(el.getTagName() + "Created");
                return cmp;
            }

            @Override
            public Object newConstraint(Container parent, Element parentEl, Component child, Element childEl) {
                return super.newConstraint(parent, parentEl, child, childEl);
            }
        });

        Container view = fragment.getView();
        assertEquals("yCreated", view.getName());
        Component child = fragment.findById("first");
        assertNotNull(child);
        assertEquals("labelCreated", child.getName());
    }
}
