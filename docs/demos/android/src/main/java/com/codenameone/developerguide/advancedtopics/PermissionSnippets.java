package com.codenameone.developerguide.advancedtopics;

import com.codename1.components.InfiniteProgress;
import com.codename1.contacts.Contact;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.MultiButton;
import com.codename1.impl.android.AndroidNativeUtil;

/**
 * Snippets related to Android runtime permissions.
 */
public class PermissionSnippets {

    public void showContactsWithPermission() {
        // tag::contactsPermission[]
        Form f = new Form("Contacts", BoxLayout.y());
        f.add(new InfiniteProgress());
        Display.getInstance().invokeAndBlock(() -> {
            Contact[] ct = Display.getInstance().getAllContacts(true, true, false, true, true, false);
            Display.getInstance().callSerially(() -> {
                f.removeAll();
                for (Contact c : ct) {
                    MultiButton mb = new MultiButton(c.getDisplayName());
                    mb.setTextLine2(c.getPrimaryPhoneNumber());
                    f.add(mb);
                }
                f.revalidate();
            });
        });

        f.show();
        // end::contactsPermission[]
    }

    public void customizePermissionPrompt() {
        // tag::permissionPrompt[]
        Display.getInstance().setProperty(
                "android.permission.READ_CONTACTS",
                "MyCoolChatApp needs access to your contacts so we can show you which of your friends already have MyCoolChatApp installed");
        // end::permissionPrompt[]
    }

    public void checkForPermission() {
        // tag::androidCheckForPermission[]
        if (!AndroidNativeUtil.checkForPermission(
                android.Manifest.permission.READ_PHONE_STATE,
                "This should be the description shown to the user...")) {
            // you didn't get the permission, you might want to return here
        }
        // you have the permission, do what you need
        // end::androidCheckForPermission[]
    }
}
