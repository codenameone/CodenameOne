package com.codename1.ui.list;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import static com.codename1.testing.TestUtils.*;

public class GenericListCellRendererCoverageTest extends UITestBase {

    @FormTest
    public void testMonitor() {
        Button b = new Button("Test");
        b.setName("myBtn");
        Label l = new Label("Test");
        GenericListCellRenderer rend = new GenericListCellRenderer(b, l);

        List lst = new List();
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("myBtn", "Value");

        lst.setRenderer(rend);

        Form f = new Form("List", new BorderLayout());
        f.add(BorderLayout.CENTER, lst);
        f.show();

        b.pointerPressed(0, 0);
        b.pointerReleased(0, 0);

        // Check if last clicked component is set
        assertEqual(b, rend.extractLastClickedComponent());
    }
}
