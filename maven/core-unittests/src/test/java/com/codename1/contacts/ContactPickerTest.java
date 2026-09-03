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
package com.codename1.contacts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.ContactPickerRequest;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract {@link ContactPicker} promises the ports.
 *
 * <p>The picker exists so an application can obtain a contact the user chose
 * without asking for broad address-book access, which Google Play makes an
 * app justify from 2027-01-27. That only holds if the request the application
 * writes down is the request the port receives, and if a cancelled pick is
 * something a listener cannot mistake for a selection.</p>
 */
class ContactPickerTest extends UITestBase {

    private Contact contact(String id, String name, String number) {
        Contact c = new Contact();
        c.setId(id);
        c.setDisplayName(name);
        c.setPrimaryPhoneNumber(number);
        return c;
    }

    private Contact[] pickAndWait(ContactPicker picker) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Contact[]> result = new AtomicReference<Contact[]>();
        picker.pick(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                result.set(ContactPicker.getPickedContacts(ev));
                latch.countDown();
            }
        });
        // The port answers through Display.callSerially, so the listener runs
        // after this test body yields the EDT. waitFor keeps it pumping.
        waitFor(latch, 5000);
        return result.get();
    }

    /** The ordinary case: request two fields, get back what the user chose. */
    @FormTest
    void aSelectionReachesTheListener() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);
        implementation.setContactPickerSelection(
                contact("a", "Alice", "111"),
                contact("b", "Bob", "222"));

        ContactPicker picker = new ContactPicker();
        picker.setRequestedFields(ContactPicker.NAME | ContactPicker.PHONE);
        Contact[] picked = pickAndWait(picker);

        assertNotNull(picked);
        assertEquals(2, picked.length);
        assertEquals("Alice", picked[0].getDisplayName());
        assertEquals("222", picked[1].getPrimaryPhoneNumber());
    }

    /**
     * A cancelled pick is an empty array, never null.
     *
     * <p>Every port funnels "the user backed out", "there is no picker" and
     * "the temporary grant expired" into the same answer, so a listener that
     * checks the length is complete.</p>
     */
    @FormTest
    void aCancelledPickReportsAnEmptySelection() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);
        implementation.setContactPickerSelection();

        Contact[] picked = pickAndWait(new ContactPicker());

        assertNotNull(picked, "a cancelled pick must not report null");
        assertEquals(0, picked.length);
    }

    /** An event that carries something else, or nothing, is also empty. */
    @FormTest
    void anEventWithoutContactsIsAnEmptySelection() {
        assertEquals(0, ContactPicker.getPickedContacts(null).length);
        assertEquals(0,
                ContactPicker.getPickedContacts(new ActionEvent("nonsense")).length);
    }

    /** The request the application configured is the request the port sees. */
    @FormTest
    void theConfiguredRequestIsWhatThePortReceives() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);

        ContactPicker picker = new ContactPicker();
        picker.setRequestedFields(ContactPicker.EMAIL | ContactPicker.PHOTO);
        picker.setMultiSelect(true);
        picker.setSelectionLimit(4);
        picker.setRequireAllRequestedFields(true);
        pickAndWait(picker);

        List<ContactPickerRequest> requests =
                implementation.getContactPickerRequests();
        assertEquals(1, requests.size());
        ContactPickerRequest request = requests.get(0);
        assertEquals(ContactPicker.EMAIL | ContactPicker.PHOTO,
                request.getRequestedFields());
        assertTrue(request.isMultiSelect());
        assertEquals(4, request.getSelectionLimit());
        assertTrue(request.isRequireAllRequestedFields());
    }

    /** Nothing is requested that the caller did not ask for. */
    @FormTest
    void theDefaultRequestIsNameAndPhoneOnly() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);

        ContactPicker picker = new ContactPicker();
        assertEquals(ContactPicker.NAME | ContactPicker.PHONE,
                picker.getRequestedFields());
        assertFalse(picker.isMultiSelect());
        assertFalse(picker.isRequireAllRequestedFields());
        assertEquals(ContactPicker.MAXIMUM_SELECTION_LIMIT,
                picker.getSelectionLimit());

        pickAndWait(picker);
        assertEquals(ContactPicker.NAME | ContactPicker.PHONE,
                implementation.getContactPickerRequests().get(0)
                        .getRequestedFields());
    }

    /** Bits outside the known fields are dropped rather than forwarded. */
    @FormTest
    void unknownFieldBitsAreDiscarded() {
        ContactPicker picker = new ContactPicker();
        picker.setRequestedFields(ContactPicker.NAME | 0x40000000);
        assertEquals(ContactPicker.NAME, picker.getRequestedFields());
    }

    /** A request naming no field at all is a mistake worth reporting. */
    @FormTest
    void requestingNoFieldIsRejected() {
        final ContactPicker picker = new ContactPicker();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                picker.setRequestedFields(0);
            }
        });
    }

    /** Android throws above 100, so the limit is checked before it gets there. */
    @FormTest
    void anOutOfRangeSelectionLimitIsRejected() {
        final ContactPicker picker = new ContactPicker();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                picker.setSelectionLimit(ContactPicker.MAXIMUM_SELECTION_LIMIT + 1);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                picker.setSelectionLimit(0);
            }
        });
    }

    /** Picking with no listener would silently lose the user's selection. */
    @FormTest
    void pickingWithoutAListenerIsRejected() {
        final ContactPicker picker = new ContactPicker();
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                picker.pick(null);
            }
        });
    }

    /**
     * A second pick while the first is still outstanding is refused.
     *
     * <p>Without this the ports' single result slot decodes the first pick
     * with the second request's fields and hands it to the second listener,
     * and the first listener is never called at all. A double tap is enough
     * to produce it, and no platform can show two pickers at once anyway.</p>
     */
    @FormTest
    void aSecondPickWhileOneIsOutstandingReportsEmpty() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);
        implementation.setContactPickerSelection(contact("a", "Alice", "111"));

        final CountDownLatch first = new CountDownLatch(1);
        final AtomicReference<Contact[]> firstResult = new AtomicReference<Contact[]>();
        new ContactPicker().pick(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                firstResult.set(ContactPicker.getPickedContacts(ev));
                first.countDown();
            }
        });

        // Still in flight: this test body IS the EDT, so the first listener
        // has not run yet.
        final CountDownLatch second = new CountDownLatch(1);
        final AtomicReference<Contact[]> secondResult = new AtomicReference<Contact[]>();
        new ContactPicker().pick(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                secondResult.set(ContactPicker.getPickedContacts(ev));
                second.countDown();
            }
        });

        waitFor(first, 5000);
        waitFor(second, 5000);
        assertEquals(1, firstResult.get().length,
                "the pick that was already running must still report its selection");
        assertEquals("Alice", firstResult.get()[0].getDisplayName());
        assertEquals(0, secondResult.get().length,
                "the refused pick must report an empty selection, not the other one's");
        assertEquals(1, implementation.getContactPickerRequests().size(),
                "the refused pick must never reach the port");
    }

    /** And the picker works again once the outstanding one has answered. */
    @FormTest
    void aPickAfterTheLastOneFinishedIsAccepted() {
        implementation.clearContacts();
        implementation.setContactPickerSupported(true);
        implementation.setContactPickerSelection(contact("a", "Alice", "111"));

        assertEquals(1, pickAndWait(new ContactPicker()).length);
        assertEquals(1, pickAndWait(new ContactPicker()).length,
                "the guard must not wedge the picker after a completed pick");
    }

    /** isSupported reports what the platform said, not a guess. */
    @FormTest
    void supportIsReportedFromThePlatform() {
        implementation.clearContacts();
        assertFalse(ContactPicker.isSupported());
        implementation.setContactPickerSupported(true);
        assertTrue(ContactPicker.isSupported());
    }
}
