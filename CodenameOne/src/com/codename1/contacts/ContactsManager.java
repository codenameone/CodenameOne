/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/// `ContactsManager` provides access to the contacts on the device for listing, adding and deleting contacts.
///
/// The sample below demonstrates listing all the contacts within the device with their photos
///
/// ```java
/// Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
/// hi.add(new InfiniteProgress());
/// int size = Display.getInstance().convertToPixels(5, true);
/// FontImage fi = FontImage.createFixed("" + FontImage.MATERIAL_PERSON, FontImage.getMaterialDesignFont(), 0xff, size, size);
///
/// Display.getInstance().scheduleBackgroundTask(() -> {
///     Contact[] contacts = Display.getInstance().getAllContacts(true, true, false, true, false, false);
///     Display.getInstance().callSerially(() -> {
///         hi.removeAll();
///         for(Contact c : contacts) {
///             MultiButton mb = new MultiButton(c.getDisplayName());
///             mb.setIcon(fi);
///             mb.setTextLine2(c.getPrimaryPhoneNumber());
///             hi.add(mb);
///             mb.putClientProperty("id", c.getId());
///             Display.getInstance().scheduleBackgroundTask(() -> {
///                 Contact cc = ContactsManager.getContactById(c.getId(), false, true, false, false, false);
///                 Display.getInstance().callSerially(() -> {
///                     Image photo = cc.getPhoto();
///                     if(photo != null) {
///                         mb.setIcon(photo.fill(size, size));
///                         mb.revalidate();
///                     }
///                 });
///             });
///         }
///         hi.getContentPane().animateLayout(150);
///     });
/// });
/// ```
///
/// @author Chen
public class ContactsManager {

    /// This method returns all contacts IDs
    ///
    /// #### Returns
    ///
    /// array of contacts IDs
    public static String[] getAllContacts() {
        return Display.getInstance().getAllContacts(false);
    }

    /// This method returns all contacts that has a phone number
    ///
    /// #### Returns
    ///
    /// array of contacts IDs
    public static String[] getAllContactsWithNumbers() {
        return Display.getInstance().getAllContacts(true);
    }

    /// This method returns a Contact by the contact id
    ///
    /// #### Parameters
    ///
    /// - `id`: of the Contact
    ///
    /// #### Returns
    ///
    /// a Contact Object
    public static Contact getContactById(String id) {
        return Display.getInstance().getContactById(id);
    }

    /// This method returns a Contact by the contact id and fills it's data
    /// according to the given flags
    ///
    /// #### Parameters
    ///
    /// - `id`: of the Contact
    ///
    /// - `includesFullName`: if true try to fetch the full name of the Contact(not just display name)
    ///
    /// - `includesPicture`: if true try to fetch the Contact Picture if exists
    ///
    /// - `includesNumbers`: if true try to fetch all Contact numbers
    ///
    /// - `includesEmail`: if ture try to fetch all Contact Emails
    ///
    /// - `includeAddress`: if ture try to fetch all Contact Addresses
    ///
    /// #### Returns
    ///
    /// a Contact Object
    public static Contact getContactById(String id, boolean includesFullName, boolean includesPicture,
                                         boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        return Display.getInstance().getContactById(id, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
    }

    /// Create a contact to the device contacts book
    ///
    /// #### Parameters
    ///
    /// - `firstName`: the Contact firstName
    ///
    /// - `familyName`: the Contact familyName
    ///
    /// - `workPhone`: the Contact work phone or null
    ///
    /// - `homePhone`: the Contact home phone or null
    ///
    /// - `mobilePhone`: the Contact mobile phone or null
    ///
    /// - `email`: the Contact email or null
    ///
    /// #### Returns
    ///
    /// the contact id if creation succeeded or null  if failed
    public static String createContact(String firstName, String familyName,
                                       String workPhone, String homePhone, String mobilePhone,
                                       String email) {
        return Display.getInstance().createContact(firstName, familyName,
                workPhone, homePhone, mobilePhone, email);
    }

    /// removed a contact from the device contacts book
    ///
    /// #### Parameters
    ///
    /// - `id`: the contact id to remove
    ///
    /// #### Returns
    ///
    /// true if deletion succeeded false otherwise
    public static boolean deleteContact(String id) {
        return Display.getInstance().deleteContact(id);
    }

    /// Notice: this method might be very slow and should be invoked on a separate thread!
    /// It might have platform specific optimizations over getAllContacts followed by looping
    /// over individual contacts but that isn't guaranteed. See isGetAllContactsFast for
    /// information.
    ///
    /// #### Parameters
    ///
    /// - `withNumbers`: if true returns only contacts that has a number
    ///
    /// - `includesFullName`: if true try to fetch the full name of the Contact(not just display name)
    ///
    /// - `includesPicture`: if true try to fetch the Contact Picture if exists
    ///
    /// - `includesNumbers`: if true try to fetch all Contact numbers
    ///
    /// - `includesEmail`: if true try to fetch all Contact Emails
    ///
    /// - `includeAddress`: if true try to fetch all Contact Addresses
    ///
    /// #### Returns
    ///
    /// array of the contacts
    public static Contact[] getContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        return Display.getInstance().getAllContacts(withNumbers, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
    }

    /// Indicates if the getAllContacts is platform optimized, notice that the method
    /// might still take seconds or more to run so you should still use a separate thread!
    ///
    /// #### Returns
    ///
    /// true if getAllContacts will perform faster that just getting each contact
    public static boolean isAllContactsFast() {
        return Display.getInstance().isGetAllContactsFast();
    }

    /// Clears the contacts cache to that they will be loaded from the system the next time `boolean, boolean, boolean, boolean, boolean)`
    /// is called.
    ///
    /// This is only necessary on platforms that use a transactional address book, if you want to reload contact changes
    /// that have occurred outside the app.  At time of writing, the only platform that does this is iOS.  This method will have no effect on other platforms.
    public static void refresh() {
        Display.getInstance().refreshContacts();
    }

    /// Notice: this method might be very slow and should be invoked on a separate thread!
    /// It might have platform specific optimizations over getAllContacts followed by looping
    /// over individual contacts but that isn't guaranteed. See isGetAllContactsFast for
    /// information.
    ///
    /// #### Parameters
    ///
    /// - `withNumbers`: if true returns only contacts that has a number
    ///
    /// - `includesFullName`: if true try to fetch the full name of the Contact(not just display name)
    ///
    /// - `includesPicture`: if true try to fetch the Contact Picture if exists
    ///
    /// - `includesNumbers`: if true try to fetch all Contact numbers
    ///
    /// - `includesEmail`: if true try to fetch all Contact Emails
    ///
    /// - `includeAddress`: if true try to fetch all Contact Addresses
    ///
    /// #### Returns
    ///
    /// array of the contacts
    ///
    /// #### Deprecated
    ///
    /// this method was incorrectly introduced use getContacts instead
    public Contact[] getAllContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        return Display.getInstance().getAllContacts(withNumbers, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
    }

    /// Indicates if the getAllContacts is platform optimized, notice that the method
    /// might still take seconds or more to run so you should still use a separate thread!
    ///
    /// #### Returns
    ///
    /// true if getAllContacts will perform faster that just getting each contact
    ///
    /// #### Deprecated
    ///
    /// this method was incorrectly introduced and isn't static use isAllContactsFast instead
    public boolean isGetAllContactsFast() {
        return Display.getInstance().isGetAllContactsFast();
    }

    /// Gets all of the contacts that are linked to this contact.  Some platforms, like iOS, allow for multiple distinct contact records to be "linked" to indicate that they refer to the same person.
    ///
    /// #### Parameters
    ///
    /// - `c`: The contact whose "linked" contacts are to be retrieved.
    ///
    /// #### Returns
    ///
    /// Array of Contacts.  Should never be null, but may be a zero-sized array.
    //public static Contact[] getLinkedContacts(Contact c) {
    //    return Display.getInstance().getLinkedContacts(c);
    //}

}
