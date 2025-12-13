package com.codename1.ui.list;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class FilterProxyListModelTest extends UITestBase {

    @FormTest
    void filterProxySortsFiltersAndTracksSelection() {
        DefaultListModel<String> base = new DefaultListModel<String>("banana", "apple", "cherry");
        FilterProxyListModel proxy = new FilterProxyListModel(base);
        TextField filter = new TextField();
        List<String> list = new List<String>(proxy);
        proxy.install(filter, list);

        Form form = new Form(BoxLayout.y());
        form.addAll(filter, list);
        form.show();

        proxy.sort(true);
        assertEquals("apple", proxy.getItemAt(0));

        proxy.setStartsWithMode(true);
        proxy.filter("ch");
        assertEquals(1, proxy.getSize());
        assertEquals("cherry", proxy.getItemAt(0));

        proxy.setSelectedIndex(0);
        assertEquals(0, proxy.getSelectedIndex());
        proxy.dataChanged(FilterProxyListModel.ADDED, 0);
        assertTrue(proxy.getSize() >= 1);
    }
}
