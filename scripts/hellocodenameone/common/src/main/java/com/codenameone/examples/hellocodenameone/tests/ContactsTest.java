package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactsManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

public class ContactsTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        Form form = createForm("Contacts", new BorderLayout(), "Contacts");
        Label status = new Label("Reading contacts...");
        form.add(BorderLayout.CENTER, status);
        form.show();

        new Thread(() -> {
            try {
                // Requesting only IDs to be minimal and reduce permission friction where possible
                Contact[] contacts = ContactsManager.getContacts(false, false, false, false, false, false);
                Display.getInstance().callSerially(() -> {
                    if (contacts == null) {
                        status.setText("Contacts access denied or returned null");
                    } else {
                        status.setText("Found " + contacts.length + " contact IDs");
                    }
                    form.revalidate();
                });
            } catch (Exception e) {
                Display.getInstance().callSerially(() -> {
                    status.setText("Exception: " + e.getMessage());
                    form.revalidate();
                });
                e.printStackTrace();
            }
        }).start();

        return true;
    }
}
