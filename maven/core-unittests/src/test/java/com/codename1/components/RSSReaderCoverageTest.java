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
        RSSReader.BackCommand backCmd = reader.new BackCommand(previousForm);

        backCmd.actionPerformed(new ActionEvent(backCmd));

        // showBack() animation might take time or be instant in test
        // But usually setCurrent should reflect the change immediately in non-animated context or after
        Assertions.assertEquals("Previous", Display.getInstance().getCurrent().getTitle());
    }

    @FormTest
    public void testListener() {
        RSSReader reader = new RSSReader();
        String testUrl = "https://www.example.com";
        RSSReader.Listener listener = reader.new Listener(testUrl);

        listener.actionPerformed(new ActionEvent(reader));

        Assertions.assertEquals(testUrl, TestCodenameOneImplementation.getInstance().getExecuteURL());
    }
}
