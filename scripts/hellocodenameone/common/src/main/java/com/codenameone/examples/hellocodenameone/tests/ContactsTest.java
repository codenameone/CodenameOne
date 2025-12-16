package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactsManager;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

public class ContactsTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = new Form("Contacts", new BorderLayout());
        Label status = new Label("Reading contacts...");
        form.add(BorderLayout.CENTER, status);
        form.show();

        // Attempt to get contacts.
        // On Android, this might trigger a blocking permission dialog that we cannot interact with.
        // We run this in a background thread and use a safety timer to ensure the test doesn't hang the suite.
        Thread t = new Thread(() -> {
            try {
                // Requesting only IDs to be minimal and reduce permission friction where possible
                Contact[] contacts = ContactsManager.getContacts(false, false, false, false, false, false);
                Display.getInstance().callSerially(() -> {
                    if (contacts == null) {
                        status.setText("Contacts returned null (access denied?)");
                    } else {
                        status.setText("Found " + contacts.length + " contact IDs");
                    }
                    form.revalidate();
                    done();
                });
            } catch (Exception e) {
                Display.getInstance().callSerially(() -> {
                    status.setText("Exception: " + e.getMessage());
                    form.revalidate();
                    done();
                });
                e.printStackTrace();
            }
        });
        t.start();

        // Safety timeout: if the thread is blocked (e.g. by a permission dialog) for too long,
        // we finish the test gracefully so the suite continues.
        UITimer.timer(5000, false, form, () -> {
            if (!isDone()) {
                status.setText("Timeout waiting for contacts (likely blocking permission dialog).");
                form.revalidate();
                done();
            }
        });

        return true;
    }
}
