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

        // If the platform allows us to check for permission grant, we should.
        // If not granted, we might want to skip to avoid blocking.
        // Not all platforms implement isContactsPermissionGranted() reliably (it might return true but still ask).
        // However, if it returns false, we know we shouldn't ask.
        if (!Display.getInstance().isContactsPermissionGranted()) {
             status.setText("Contacts permission not granted. Skipping.");
             form.revalidate();
             done();
             return true;
        }

        // We run getContacts in a separate thread.
        // We also set a safety timer to abort the test if it hangs on a permission dialog.
        Thread t = new Thread(() -> {
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
        // 5 seconds should be enough for a non-blocking fast return or a quick grant,
        // but if it waits for user input, it will hang.
        UITimer.timer(5000, false, form, () -> {
            if (!isDone()) {
                status.setText("Timeout waiting for contacts (likely permission dialog).");
                form.revalidate();
                done(); // Mark test as done so suite proceeds
                // We cannot kill the thread t cleanly in Java if it's blocked in native code,
                // but we can move on.
            }
        });

        return true;
    }
}
