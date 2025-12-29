package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import org.junit.jupiter.api.Assertions;

public class RSSReaderCoverageTest extends UITestBase {

    @FormTest
    public void testBackCommand() {
        Form previousForm = new Form("Previous");
        previousForm.show();

        Form currentForm = new Form("Current");
        currentForm.show();

        Assertions.assertEquals("Current", Display.getInstance().getCurrent().getTitle());

        RSSReader reader = new RSSReader();
        RSSReader.BackCommand backCmd = new RSSReader.BackCommand(previousForm);

        backCmd.actionPerformed(new ActionEvent(backCmd));

        // showBack() animation might take time or be instant in test
        // But usually setCurrent should reflect the change immediately in non-animated context or after
        Assertions.assertEquals("Previous", Display.getInstance().getCurrent().getTitle());
    }

    @FormTest
    public void testListener() {
        RSSReader reader = new RSSReader();
        String testUrl = "https://www.example.com";
        RSSReader.Listener listener = new RSSReader.Listener(testUrl);

        listener.actionPerformed(new ActionEvent(reader));

        Assertions.assertEquals(testUrl, TestCodenameOneImplementation.getInstance().getExecuteURL());
    }

    @FormTest
    public void testEventHandler() {
        class TestRSSReader extends RSSReader {
            public void fireEvent(ActionEvent evt) {
                super.fireActionEvent(evt);
            }
        }

        TestRSSReader reader = new TestRSSReader();
        reader.setURL("http://example.com/rss");

        reader.initComponent();

        com.codename1.io.services.RSSService service = new com.codename1.io.services.RSSService("http://example.com/rss");

        java.util.Hashtable<String, String> item = new java.util.Hashtable<String, String>();
        item.put("title", "Test Title");
        item.put("description", "Desc");
        item.put("link", "http://example.com/article");

        java.util.Vector<java.util.Hashtable<String, String>> metaData = new java.util.Vector<java.util.Hashtable<String, String>>();
        metaData.add(item);

        com.codename1.io.NetworkEvent ne = new com.codename1.io.NetworkEvent(null, service);
        // NetworkEvent stores metaData in a private field, set by constructor (request, metaData)
        // We use that constructor.
        ne = new com.codename1.io.NetworkEvent(service, (Object)metaData);

        reader.fireEvent(ne);

        Assertions.assertTrue(reader.getModel().getSize() > 0);

        reader.setSelectedIndex(0);
        reader.fireEvent(new ActionEvent(reader));

        // Use TestUtils wait if available or just check immediately since everything is on same thread
        // waitForFormTitle("Test Title");
    }
}
