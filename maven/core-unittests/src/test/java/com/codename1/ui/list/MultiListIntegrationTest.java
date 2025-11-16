package com.codename1.ui.list;

import com.codename1.components.MultiButton;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.GenericListCellRenderer;
import com.codename1.ui.list.MultiList;
import com.codename1.testing.TestUtils;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MultiListIntegrationTest extends UITestBase {

    @FormTest
    void multiListInitializesRendererAndProperties() {
        DefaultListModel<Hashtable> model = new DefaultListModel<Hashtable>(
                row("Row A", "Details"),
                row("Row B", "More")
        );
        MultiList multi = new MultiList(model);
        multi.setName("multi");
        multi.setPreferredW(120);
        multi.setPreferredH(80);

        Form form = new Form(BoxLayout.y());
        form.add(multi);
        form.show();

        Component renderer = multi.getRenderer();
        assertTrue(renderer instanceof GenericListCellRenderer);

        Image placeholder = Image.createImage(4, 4);
        String noError = multi.setPropertyValue("placeholder", placeholder);
        assertNull(noError);
        GenericListCellRenderer glcr = (GenericListCellRenderer) multi.getRenderer();
        MultiButton unselected = (MultiButton) glcr.getListCellRendererComponent(multi, model.getItemAt(0), 0, false, false);
        assertSame(placeholder, unselected.getIcon());

        TestUtils.selectInList("multi", 1);
        assertEquals(1, multi.getSelectedIndex());
        Hashtable selected = (Hashtable) multi.getSelectedItem();
        assertEquals("Row B", selected.get("Line1"));
    }

    @FormTest
    void multiListUsesPropertyNamesAndResetsRendererAfterPropertyChange() {
        MultiList multi = new MultiList();
        multi.setPreferredW(100);
        multi.setPreferredH(60);
        multi.setName("dynamicMulti");
        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, multi);
        form.show();

        String[] propertyNames = multi.getPropertyNames();
        assertTrue(propertyNames.length > 0);
        assertEquals(Boolean.class, multi.getPropertyTypes()[15]);

        multi.setPropertyValue("name1", "Primary");
        GenericListCellRenderer renderer = (GenericListCellRenderer) multi.getRenderer();
        MultiButton rendered = (MultiButton) renderer.getListCellRendererComponent(multi, multi.getModel().getItemAt(0), 0, false, false);
        assertEquals("Primary", rendered.getTextLine1());

        multi.setPropertyValue("uiid2", "CustomLabel");
        MultiButton afterChange = (MultiButton) multi.getRenderer().getListCellRendererComponent(multi, multi.getModel().getItemAt(0), 0, false, false);
        assertEquals("CustomLabel", afterChange.getTextLine2());
    }

    @FormTest
    void containerListFiresSelectionFromPointerEvents() {
        final AtomicInteger selected = new AtomicInteger(-1);
        ContainerList containerList = new ContainerList();
        containerList.setName("containerList");
        containerList.setPreferredW(100);
        containerList.setPreferredH(60);
        containerList.setModel(new DefaultListModel<String>("One", "Two", "Three"));
        containerList.addSelectionListener(new com.codename1.ui.events.SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                selected.set(newSelected);
            }
        });

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, containerList);
        form.show();

        TestUtils.selectInList("containerList", 2);
        assertEquals(2, selected.get());
        assertEquals("Three", containerList.getModel().getItemAt(containerList.getSelectedIndex()));
    }

    private Hashtable row(String line1, String line2) {
        Hashtable data = new Hashtable();
        data.put("Line1", line1);
        data.put("Line2", line2);
        return data;
    }
}
