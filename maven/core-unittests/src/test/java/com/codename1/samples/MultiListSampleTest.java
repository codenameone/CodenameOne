package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.list.MultiList;
import com.codename1.ui.list.DefaultListModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import static com.codename1.ui.CN.*;

public class MultiListSampleTest extends UITestBase {

    @FormTest
    public void testMultiList() {
        Form current = new MyForm();
        current.show();
        // Just checking that it renders without error
    }

    class MyForm extends Form {
        MultiList cmp = new MultiList();

        MyForm() {
            Map<String, Object> entry = new HashMap<String, Object>();

            entry.put("Line1", "somedata");
            entry.put("Line2", "somedata");
            entry.put("Line3", "somedata");
            entry.put("Line4", "somedata");
            ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

            data.add(entry);


            cmp.setModel(new DefaultListModel(data));
            setLayout(new BorderLayout());
            add(BorderLayout.CENTER, cmp);
        }
    }
}
