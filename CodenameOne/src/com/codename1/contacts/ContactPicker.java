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

import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/// Lets the user hand the application a small number of contacts they chose
/// themselves, without the application gaining access to the address book.
///
/// This is the privacy-minimized counterpart to `ContactsManager`. Where
/// `ContactsManager` enumerates the whole address book -- and therefore needs
/// the broad contacts permission -- this class shows the platform's own
/// contact picker. The user selects, the platform copies just the fields that
/// were asked for out of just the contacts that were selected, and the
/// application never gets to see anything else.
///
/// Prefer it whenever the application needs "a phone number the user picked"
/// rather than "the address book". Google Play requires exactly that
/// distinction: from 2027-01-27, an app targeting Android 17 (API level 37) or
/// later that carries `READ_CONTACTS` without core-functionality
/// justification has to pass a Play Console declaration, and an app that only
/// ever calls this class never asks for that permission in the first place.
///
/// #### Requesting fields
///
/// The requested fields are a bit set of the constants on this class. Only
/// those fields are populated on the returned contacts; everything else is
/// left null or zero. Asking for less is not merely polite -- on Android the
/// request also decides which contacts the picker offers, and the platform
/// refuses to return anything that was not asked for.
///
/// ```java
/// ContactPicker picker = new ContactPicker();
/// picker.setRequestedFields(ContactPicker.NAME | ContactPicker.PHONE);
/// picker.pick(new ActionListener<ActionEvent>() {
///     public void actionPerformed(ActionEvent ev) {
///         Contact[] picked = ContactPicker.getPickedContacts(ev);
///         if(picked.length == 0) {
///             // the user backed out
///             return;
///         }
///         numberField.setText(picked[0].getPrimaryPhoneNumber());
///     }
/// });
/// ```
///
/// #### The result is a snapshot
///
/// The contacts handed to the callback are plain copies. The application has
/// no continuing access to them: it cannot re-read them later, and on Android
/// the temporary grant behind them is gone by the time the callback returns.
/// Anything that has to outlive the callback must be copied out of the
/// `Contact` and stored by the application.
///
/// For the same reason `Contact#getId()` on a picked contact is only an
/// opaque platform identifier useful for telling two picked contacts apart.
/// Passing it to `ContactsManager#getContactById(String)` needs full
/// address-book access, which is the thing this class exists to avoid.
///
/// #### Availability
///
/// `#isSupported()` reports whether the platform has a picker at all. Where it
/// does not, `#pick(ActionListener)` reports an empty selection rather than
/// quietly falling back to reading the address book, because that fallback
/// would need the permission the caller was trying not to ask for.
///
/// @author Shai Almog
public class ContactPicker {

    /// Requests the contact's name, which populates `Contact#getFirstName()`,
    /// `Contact#getFamilyName()` and `Contact#getDisplayName()`.
    public static final int NAME = 1;

    /// Requests the contact's phone numbers, which populates
    /// `Contact#getPhoneNumbers()` and `Contact#getPrimaryPhoneNumber()`.
    public static final int PHONE = 2;

    /// Requests the contact's email addresses, which populates
    /// `Contact#getEmails()` and `Contact#getPrimaryEmail()`.
    public static final int EMAIL = 4;

    /// Requests the contact's postal addresses, which populates
    /// `Contact#getAddresses()`.
    public static final int ADDRESS = 8;

    /// Requests the contact's photo, which populates `Contact#getPhoto()`.
    public static final int PHOTO = 16;

    /// Requests the contact's birthday, which populates
    /// `Contact#getBirthday()`.
    public static final int BIRTHDAY = 32;

    /// Requests the contact's web sites, which populates
    /// `Contact#getUrls()`.
    public static final int WEBSITE = 64;

    /// Every field a picker can be asked for. Convenient for a one-off
    /// "import this person" flow, and the wrong choice for anything else:
    /// asking for a field the application will not read hands it data it did
    /// not need, which is what the picker exists to prevent.
    public static final int ALL_FIELDS =
            NAME | PHONE | EMAIL | ADDRESS | PHOTO | BIRTHDAY | WEBSITE;

    /// The largest value `#setSelectionLimit(int)` accepts. Android rejects a
    /// larger request outright.
    public static final int MAXIMUM_SELECTION_LIMIT = 100;

    private int requestedFields = NAME | PHONE;

    private boolean multiSelect;

    private int selectionLimit = MAXIMUM_SELECTION_LIMIT;

    private boolean requireAllRequestedFields;

    /// Returns true when the platform can show a contact picker.
    ///
    /// #### Returns
    ///
    /// true if `#pick(ActionListener)` will show a picker, false if it will
    /// report an empty selection without showing anything
    public static boolean isSupported() {
        return Display.getInstance().isContactPickerSupported();
    }

    /// Extracts the selection from the event delivered to
    /// `#pick(ActionListener)`.
    ///
    /// #### Parameters
    ///
    /// - `ev`: the event handed to the listener, which may be null
    ///
    /// #### Returns
    ///
    /// the contacts the user picked, in the order the platform reported them,
    /// or a zero length array when the user cancelled or the platform has no
    /// picker. Never null.
    public static Contact[] getPickedContacts(ActionEvent ev) {
        if (ev == null) {
            return new Contact[0];
        }
        Object source = ev.getSource();
        if (source instanceof Contact[]) {
            return (Contact[]) source;
        }
        return new Contact[0];
    }

    /// The fields the picker is asked for, as a bit set of the constants on
    /// this class.
    ///
    /// #### Returns
    ///
    /// the requested fields, `NAME | PHONE` unless it was changed
    public int getRequestedFields() {
        return requestedFields;
    }

    /// Sets the fields the picker is asked for.
    ///
    /// #### Parameters
    ///
    /// - `requestedFields`: a bit set of the constants on this class, which
    /// must name at least one field
    public void setRequestedFields(int requestedFields) {
        if ((requestedFields & ALL_FIELDS) == 0) {
            throw new IllegalArgumentException(
                    "A contact picker must request at least one field");
        }
        this.requestedFields = requestedFields & ALL_FIELDS;
    }

    /// Whether the user may pick more than one contact.
    ///
    /// #### Returns
    ///
    /// true if the picker allows a multiple selection, false by default
    public boolean isMultiSelect() {
        return multiSelect;
    }

    /// Sets whether the user may pick more than one contact.
    ///
    /// A platform whose picker is single-select ignores this and returns at
    /// most one contact, so the callback must cope with a shorter selection
    /// than it allowed for. Android before version 17 is such a platform.
    ///
    /// #### Parameters
    ///
    /// - `multiSelect`: true to allow a multiple selection
    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }

    /// The largest number of contacts the user may pick.
    ///
    /// #### Returns
    ///
    /// the selection limit, `MAXIMUM_SELECTION_LIMIT` unless it was changed
    public int getSelectionLimit() {
        return selectionLimit;
    }

    /// Sets the largest number of contacts the user may pick, which only has
    /// an effect together with `#setMultiSelect(boolean)`.
    ///
    /// #### Parameters
    ///
    /// - `selectionLimit`: a count between 1 and
    /// `MAXIMUM_SELECTION_LIMIT` inclusive
    public void setSelectionLimit(int selectionLimit) {
        if (selectionLimit < 1 || selectionLimit > MAXIMUM_SELECTION_LIMIT) {
            throw new IllegalArgumentException(
                    "Selection limit must be between 1 and "
                            + MAXIMUM_SELECTION_LIMIT + ", got "
                            + selectionLimit);
        }
        this.selectionLimit = selectionLimit;
    }

    /// Whether a contact has to carry every requested field to be offered.
    ///
    /// #### Returns
    ///
    /// true to offer only contacts holding all of the requested fields, false
    /// by default, which offers a contact holding any of them
    public boolean isRequireAllRequestedFields() {
        return requireAllRequestedFields;
    }

    /// Sets whether a contact has to carry every requested field to be
    /// offered by the picker.
    ///
    /// Use it when a partial contact is useless to the application, for
    /// instance an invitation flow that needs both a name and an email
    /// address. Leave it off when any one of the requested fields will do.
    ///
    /// A platform applies it as far as its own picker can. Android 17 and
    /// later enforce it exactly; iOS enforces it over phone numbers, email
    /// addresses and postal addresses and cannot filter on the rest; Android
    /// before 17 has no picker predicate at all and ignores it. So the
    /// listener still has to cope with a contact that turned out to be
    /// missing one.
    ///
    /// #### Parameters
    ///
    /// - `requireAllRequestedFields`: true to require every requested field
    public void setRequireAllRequestedFields(boolean requireAllRequestedFields) {
        this.requireAllRequestedFields = requireAllRequestedFields;
    }

    /// Shows the platform's contact picker and reports the selection.
    ///
    /// The call returns at once; the picker runs on top of the application and
    /// the listener is invoked on the EDT when the user is done. A cancelled
    /// pick and a platform with no picker both report an empty selection, so
    /// `#getPickedContacts(ActionEvent)` is the only thing the listener has to
    /// check.
    ///
    /// #### Parameters
    ///
    /// - `response`: invoked with the selection once the user is done
    public void pick(ActionListener<ActionEvent> response) {
        if (response == null) {
            throw new IllegalArgumentException(
                    "A contact picker needs a listener to report to");
        }
        Display.getInstance().pickContacts(requestedFields, multiSelect,
                selectionLimit, requireAllRequestedFields, response);
    }
}
