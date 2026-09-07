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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactPicker;
import com.codename1.ui.events.ActionEvent;

/**
 * The contact-picker API on the device VM.
 *
 * <p>Showing a picker needs a human, so this asserts only the half a suite can
 * assert without one: the request the API accepts and rejects, and that a
 * result carrying no selection is an empty array on every platform rather
 * than a null the caller has to guard.</p>
 *
 * <p>Referencing {@code com.codename1.contacts.ContactPicker} at all is also
 * the point. It is what makes the iOS build turn on
 * {@code CN1_USE_CONTACT_PICKER}, link ContactsUI and compile the
 * CNContactPickerViewController delegate, and what keeps the mangled callback
 * symbol the Objective-C side calls out of the dead-code pass -- none of
 * which any other test in this suite exercises.</p>
 */
public class ContactPickerApiTest extends BaseTest {

    @Override
    public boolean runTest() {
        try {
            // Empty rather than null, however the pick ended. Every port
            // funnels "cancelled", "no picker" and "the grant expired" into
            // this one answer so a listener that checks the length is
            // complete.
            Contact[] none = ContactPicker.getPickedContacts(null);
            assertNotNull(none, "a null event must not produce a null selection");
            assertEqual(0, none.length, "a null event must produce an empty selection");

            Contact[] wrongSource =
                    ContactPicker.getPickedContacts(new ActionEvent("not a selection"));
            assertNotNull(wrongSource, "an unrelated event must not produce null");
            assertEqual(0, wrongSource.length,
                    "an unrelated event must produce an empty selection");

            // Reads the platform's answer rather than asserting one: the
            // suite runs on ports that have a picker and on ports that do
            // not, and both are correct.
            boolean supported = ContactPicker.isSupported();
            assertBool(supported || !supported, "isSupported must not throw");

            ContactPicker picker = new ContactPicker();
            assertEqual(ContactPicker.NAME | ContactPicker.PHONE,
                    picker.getRequestedFields(),
                    "the default request must be name and phone");
            assertEqual(ContactPicker.MAXIMUM_SELECTION_LIMIT,
                    picker.getSelectionLimit(),
                    "the default selection limit must be the platform maximum");
            assertBool(!picker.isMultiSelect(),
                    "a picker must default to a single selection");

            picker.setRequestedFields(ContactPicker.EMAIL | 0x40000000);
            assertEqual(ContactPicker.EMAIL, picker.getRequestedFields(),
                    "a bit outside the known fields must be dropped");

            assertRejects(picker, 0, "a request naming no field");
            assertRejects(picker, ContactPicker.MAXIMUM_SELECTION_LIMIT + 1,
                    "a selection limit above the platform maximum");

            boolean rejectedNoListener = false;
            try {
                picker.pick(null);
            } catch (IllegalArgumentException expected) {
                rejectedNoListener = true;
            }
            assertBool(rejectedNoListener,
                    "picking with no listener would lose the user's selection silently");
        } catch (Throwable t) {
            fail("contact picker API test failed: " + t);
            return false;
        }
        done();
        return true;
    }

    /**
     * @param picker the picker under test
     * @param value  a field set when it is not a legal selection limit, a
     *               selection limit otherwise
     * @param what   named in the failure message
     */
    private void assertRejects(ContactPicker picker, int value, String what) {
        boolean rejected = false;
        try {
            if (value == 0) {
                picker.setRequestedFields(value);
            } else {
                picker.setSelectionLimit(value);
            }
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertBool(rejected, what + " must be rejected");
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
