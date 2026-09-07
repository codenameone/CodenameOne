/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which contacts permissions an application's class references earn it.
 *
 * <p>The case that forced this apart from a one-line package check is
 * {@link #aPickerOnlyApplicationAsksForNoContactsPermission()}: from
 * 2027-01-27 an Android app carrying {@code READ_CONTACTS} without
 * core-functionality justification needs a Play Console declaration, and an
 * app that only ever shows the contact picker should not be in that
 * position. The rest of the cases exist because the naive fix -- stop
 * treating the package as broad access -- silently drops the permission from
 * apps that do read the address book.</p>
 */
class ContactsPermissionScanTest {

    private static final String DISPLAY = "com/codename1/ui/Display";
    private static final String CONTACT = "com/codename1/contacts/Contact";
    private static final String PICKER = "com/codename1/contacts/ContactPicker";
    private static final String MANAGER =
            "com/codename1/contacts/ContactsManager";

    /**
     * The whole point. Picking references the picker and the value object it
     * returns, and neither reads an address book.
     */
    @Test
    void aPickerOnlyApplicationAsksForNoContactsPermission() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass(PICKER);
        scan.usesClass(CONTACT);
        scan.usesClass("com/codename1/contacts/Address");
        scan.usesClassMethod(PICKER, "pick");
        scan.usesClassMethod(DISPLAY, "pickContacts");
        scan.usesClassMethod(DISPLAY, "isContactPickerSupported");

        assertFalse(scan.readPermissionRequired(),
                "a picker-only app must not ship asking to read contacts");
        assertFalse(scan.writePermissionRequired());
    }

    /** An anonymous listener inside a pick call is still the picker. */
    @Test
    void aNestedClassOfAPermissionFreeClassIsPermissionFree() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass(PICKER + "$1");
        scan.usesClass(CONTACT + "$Builder");

        assertFalse(scan.readPermissionRequired());
    }

    /** Enumerating the address book still earns the permission. */
    @Test
    void theContactsManagerStillEarnsReadPermission() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass(MANAGER);

        assertTrue(scan.readPermissionRequired());
        assertFalse(scan.writePermissionRequired());
    }

    /** So does the list model built on top of it. */
    @Test
    void theContactsModelStillEarnsReadPermission() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass("com/codename1/contacts/ContactsModel");

        assertTrue(scan.readPermissionRequired());
    }

    /**
     * A Display call that reads has to be named by method now.
     *
     * <p>Before the picker existed, every one of these was caught by the
     * {@code Contact} it returns. That reference no longer implies anything,
     * so an unnamed reader would ship without its permission and come back
     * empty on the device.</p>
     */
    @Test
    void everyBroadDisplayReaderEarnsReadPermission() {
        String[] readers = {
            "getAllContacts",
            "getContactById",
            "getLinkedContactIds",
            "refreshContacts",
            "isGetAllContactsFast",
            "isContactsPermissionGranted",
        };
        for (int iter = 0; iter < readers.length; iter++) {
            ContactsPermissionScan scan = new ContactsPermissionScan();
            // Exactly what a Display-only app looks like: the value object
            // and the call, nothing else.
            scan.usesClass(CONTACT);
            scan.usesClassMethod(DISPLAY, readers[iter]);
            assertTrue(scan.readPermissionRequired(),
                    "Display." + readers[iter] + " reads the address book");
        }
    }

    /** The same methods on the ContactsManager facade. */
    @Test
    void theContactsManagerReadersEarnReadPermission() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClassMethod(MANAGER, "getContactById");

        assertTrue(scan.readPermissionRequired());
    }

    /** Writing is a separate permission and does not imply reading. */
    @Test
    void creatingAndDeletingEarnOnlyWritePermission() {
        String[] writers = {"createContact", "deleteContact"};
        String[] owners = {DISPLAY, MANAGER};
        for (int owner = 0; owner < owners.length; owner++) {
            for (int iter = 0; iter < writers.length; iter++) {
                ContactsPermissionScan scan = new ContactsPermissionScan();
                scan.usesClassMethod(owners[owner], writers[iter]);
                assertTrue(scan.writePermissionRequired(),
                        owners[owner] + "." + writers[iter]);
                assertFalse(scan.readPermissionRequired(),
                        owners[owner] + "." + writers[iter]
                                + " writes, it does not read");
            }
        }
    }

    /**
     * An unrecognised class in the package asks for the permission.
     *
     * <p>Stated this way round on purpose. A future class that reads the
     * address book gets its permission without anyone remembering to come
     * back here; the cost of being wrong is an unused permission rather than
     * a feature that fails on the device.</p>
     */
    @Test
    void anUnknownClassInThePackageIsTreatedAsBroadAccess() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass("com/codename1/contacts/SomethingAddedLater");

        assertTrue(scan.readPermissionRequired());
    }

    /** And so does a subpackage nobody has vetted. */
    @Test
    void aSubpackageOfContactsIsTreatedAsBroadAccess() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass("com/codename1/contacts/sync/ContactSyncAdapter");

        assertTrue(scan.readPermissionRequired());
    }

    /**
     * A package whose name merely starts the same way is not the contacts
     * package.
     *
     * <p>The rule this replaced used {@code indexOf(...) > -1}, which would
     * have said yes to any of these.</p>
     */
    @Test
    void aSimilarlyNamedPackageIsNotTheContactsPackage() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass("com/codename1/contactsync/Sync");
        scan.usesClass("com/example/com/codename1/contacts/Fake");
        scan.usesClass("com/codename1/ui/Display");

        assertFalse(scan.readPermissionRequired());
        assertFalse(scan.writePermissionRequired());
    }

    /** An unrelated class and an unrelated method change nothing. */
    @Test
    void anApplicationThatNeverTouchesContactsAsksForNothing() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClass("com/codename1/ui/Form");
        scan.usesClassMethod(DISPLAY, "getDisplayWidth");
        scan.usesClassMethod(null, null);
        scan.usesClass(null);

        assertFalse(scan.readPermissionRequired());
        assertFalse(scan.writePermissionRequired());
    }

    /**
     * The methods are only read on the classes that declare them.
     *
     * <p>{@code usesClassMethod} reports the declared owner of the call, so
     * an application class of its own with a {@code getContactById} method
     * must not drag the permission in.</p>
     */
    @Test
    void aMethodOnAnUnrelatedOwnerIsIgnored() {
        ContactsPermissionScan scan = new ContactsPermissionScan();
        scan.usesClassMethod("app/MyCache", "getContactById");
        scan.usesClassMethod("app/MyCache", "createContact");

        assertFalse(scan.readPermissionRequired());
        assertFalse(scan.writePermissionRequired());
    }
}
