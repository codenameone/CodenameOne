package com.codename1.ui.list;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenericListCellRendererTest extends UITestBase {

    @FormTest
    void rendererAppliesMapValuesAndSelectionListener() {
        Label selected = new Label();
        selected.setName("Line1");
        Label unselected = new Label();
        unselected.setName("Line1");
        GenericListCellRenderer<Map> renderer = new GenericListCellRenderer<Map>(selected, unselected);
        renderer.setSelectionListener(true);
        renderer.setFisheye(true);

        Map first = new HashMap();
        first.put("Line1", "First Row");
        Map second = new HashMap();
        second.put("Line1", "Second Row");

        DefaultListModel<Map> model = new DefaultListModel<Map>(first, second);
        List<Map> list = new List<Map>(model);
        list.setRenderer(renderer);

        Form form = new Form(new BorderLayout());
        form.add(BorderLayout.CENTER, list);
        form.show();

        Component unSel = renderer.getCellRendererComponent(list, model, first, 0, false);
        assertSame(unselected, unSel);
        assertEquals("First Row", ((Label) unSel).getText());

        list.setSelectedIndex(1);
        Component sel = renderer.getCellRendererComponent(list, model, second, 1, true);
        assertSame(selected, sel);
        assertEquals("Second Row", ((Label) sel).getText());

        renderer.updateIconPlaceholders();
        Component focusCmp = renderer.getFocusComponent(selected);
        assertNotNull(focusCmp);
        renderer.deinitialize(list);
    }
}
