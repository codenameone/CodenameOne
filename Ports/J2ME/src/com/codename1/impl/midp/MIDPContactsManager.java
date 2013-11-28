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
package com.codename1.impl.midp;

import com.codename1.ui.Image;
import com.codename1.util.Base64;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.pim.*;

/**
 *
 * @author Chen
 */
public class MIDPContactsManager {

    private static MIDPContactsManager instance = new MIDPContactsManager();

    private MIDPContactsManager() {
    }

    public static MIDPContactsManager getInstance() {
        return instance;
    }

    public com.codename1.contacts.Contact getContactById(String id) {
        return getContactById(id, true, true, true, true, true);
    }

    public String[] getAllContacts(boolean withNumbers) {

        PIM pim = PIM.getInstance();
        PIMList clist = null;
        Enumeration contacts = null;
        try {
            clist = pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY);
            contacts = clist.items();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Vector contactsIDs = new Vector();
        while (contacts.hasMoreElements()) {
            Contact c = (Contact) contacts.nextElement();
            if (clist.isSupportedField(Contact.UID) && c.countValues(Contact.UID) > 0) {
                String uid = c.getString(Contact.UID, 0);
                if (withNumbers) {
                    if (clist.isSupportedField(Contact.TEL) && c.countValues(Contact.TEL) > 0) {
                        contactsIDs.addElement(uid);
                    }
                } else {
                    contactsIDs.addElement(uid);
                }
            }
        }
        try {
            clist.close();
        } catch (PIMException ex) {
            ex.printStackTrace();
        }

        String[] ids = new String[contactsIDs.size()];
        for (int i = 0; i < contactsIDs.size(); i++) {
            String id = (String) contactsIDs.elementAt(i);
            ids[i] = id;
        }
        return ids;

    }

    public String createContact(String firstName, String familyName, String officePhone, String homePhone, String cellPhone, String email) {
        String contactId = null;
        try {
            PIM pim = PIM.getInstance();
            ContactList contacts = (ContactList) pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_WRITE);
            Contact contact = ((ContactList) contacts).createContact();
            String[] name = new String[contacts.stringArraySize(Contact.NAME)];
            String displayName = "";
            if (firstName != null) {
                displayName += firstName;
            }
            if (familyName != null) {
                displayName += " " + familyName;
            }

            if (contacts.isSupportedField(Contact.FORMATTED_NAME)) {
                contact.addString(Contact.FORMATTED_NAME, PIMItem.ATTR_NONE, displayName);
            }
            if (familyName != null && contacts.isSupportedArrayElement(Contact.NAME, Contact.NAME_FAMILY)) {
                name[Contact.NAME_FAMILY] = familyName;
            }
            if (firstName != null && contacts.isSupportedArrayElement(Contact.NAME, Contact.NAME_GIVEN)) {
                name[Contact.NAME_GIVEN] = firstName;
            }
            contact.addStringArray(Contact.NAME, PIMItem.ATTR_NONE, name);

            if (homePhone != null && contacts.isSupportedField(Contact.TEL)) {
                contact.addString(Contact.TEL, Contact.ATTR_HOME, homePhone);
            }
            if (cellPhone != null && contacts.isSupportedField(Contact.TEL)) {
                contact.addString(Contact.TEL, Contact.ATTR_MOBILE, cellPhone);
            }
            if (officePhone != null && contacts.isSupportedField(Contact.TEL)) {
                contact.addString(Contact.TEL, Contact.ATTR_WORK, officePhone);
            }

            if (email != null && contacts.isSupportedField(Contact.EMAIL)) {
                contact.addString(Contact.EMAIL, Contact.ATTR_HOME | Contact.ATTR_PREFERRED, email);
            }
            contact.commit();
            contactId = contact.getString(Contact.UID, 0);
            contacts.close();

        } catch (PIMException ex) {
            ex.printStackTrace();
        }
        return contactId;
    }

    boolean deleteContact(String id) {
        ContactList clist = null;
        try {
            PIM pim = PIM.getInstance();
            clist = (ContactList) pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_WRITE);
            Enumeration contacts = clist.items();
            while (contacts.hasMoreElements()) {
                Contact c = (Contact) contacts.nextElement();

                if (clist.isSupportedField(Contact.UID) && c.countValues(Contact.UID) > 0) {
                    String uid = c.getString(Contact.UID, 0);
                    if (uid.equals(id)) {
                        clist.removeContact(c);
                        return true;
                    }
                }
            }
        } catch (PIMException ex) {
            ex.printStackTrace();
        } finally {
            if (clist != null) {
                try {
                    clist.close();
                } catch (PIMException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return false;
    }

    public com.codename1.contacts.Contact getContactById(String id, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        com.codename1.contacts.Contact contact = new com.codename1.contacts.Contact();
        PIM pim = PIM.getInstance();
        PIMList clist = null;
        Enumeration contacts = null;
        try {
            clist = pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY);
            contacts = clist.items();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (contacts.hasMoreElements()) {
            Contact c = (Contact) contacts.nextElement();

            if (clist.isSupportedField(Contact.UID) && c.countValues(Contact.UID) > 0) {
                String uid = c.getString(Contact.UID, 0);
                if (uid.equals(id)) {
                    String name = null;
                    String firstName = null;
                    String familyName = null;

                    String notes = "";
                    String url = "";
                    long bdate = 0;
                    try {
                        if (clist.isSupportedField(Contact.FORMATTED_NAME) && c.countValues(Contact.FORMATTED_NAME) > 0) {
                            name = c.getString(Contact.FORMATTED_NAME, 0);
                        }
                    } catch (Exception e) {
                    }
                    if (includesFullName) {
                        try {
                            if (clist.isSupportedField(Contact.NAME) && c.countValues(Contact.NAME) > 0) {
                                name = "";
                                String[] newname = c.getStringArray(Contact.NAME, 0);
                                if (newname[Contact.NAME_GIVEN] != null) {
                                    firstName = newname[Contact.NAME_GIVEN];
                                }
                                if (newname[Contact.NAME_FAMILY] != null) {
                                    familyName = newname[Contact.NAME_FAMILY];
                                }
                                if (name == null) {
                                    if (firstName != null) {
                                        name = firstName;
                                    }
                                    if (familyName != null) {
                                        name += " " + familyName;
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                        if (clist.isSupportedField(Contact.BIRTHDAY) && c.countValues(Contact.BIRTHDAY) > 0) {
                            bdate = c.getDate(Contact.BIRTHDAY, 0);
                        }
                        if (clist.isSupportedField(Contact.NOTE) && c.countValues(Contact.NOTE) > 0) {
                            if (c.getString(Contact.NOTE, 0) != null) {
                                notes = c.getString(Contact.NOTE, 0);
                            }
                        }
                        if (clist.isSupportedField(Contact.URL) && c.countValues(Contact.URL) > 0) {
                            if (c.getString(Contact.URL, 0) != null) {
                                url = c.getString(Contact.URL, 0);
                            }
                        }

                    }
                    if (includesNumbers) {
                        Hashtable phones = new Hashtable();

                        int phoneNumbers = c.countValues(Contact.TEL);
                        for (int i = 0; i < phoneNumbers; i++) {
                            String key = "other";
                            String val = c.getString(Contact.TEL, i);
                            int attributes = c.getAttributes(Contact.TEL, i);
                            if (attributes != 0) {
                                if ((attributes & Contact.ATTR_HOME) != 0) {
                                    key = "home";
                                } else if ((attributes & Contact.ATTR_MOBILE) != 0) {
                                    key = "mobile";
                                } else if ((attributes & Contact.ATTR_FAX) != 0) {
                                    key = "fax";
                                } else if ((attributes & Contact.ATTR_WORK) != 0) {
                                    key = "work";
                                }
                                //set the preffered
                                if ((attributes & Contact.ATTR_PREFERRED) != 0) {
                                    contact.setPrimaryPhoneNumber(val);
                                }
                            }
                            phones.put(key, val);
                        }
                        contact.setPhoneNumbers(phones);
                    }

                    if (includesEmail) {
                        Hashtable mails = new Hashtable();
                        int emails = c.countValues(Contact.EMAIL);
                        for (int i = 0; i < emails; i++) {
                            String key = "other";
                            String val = c.getString(Contact.EMAIL, i);
                            int attributes = c.getAttributes(Contact.EMAIL, i);
                            if (attributes != 0) {

                                if ((attributes & Contact.ATTR_HOME) != 0) {
                                    key = "home";
                                } else if ((attributes & Contact.ATTR_WORK) != 0) {
                                    key = "work";
                                }

                                //set the preffered
                                if ((attributes & Contact.ATTR_PREFERRED) != 0) {
                                    contact.setPrimaryEmail(val);
                                }
                            }
                            mails.put(key, val);
                        }
                        contact.setEmails(mails);
                    }

                    if (includeAddress) {
                        if (clist.isSupportedField(Contact.ADDR)) {
                            Hashtable address = new Hashtable();
                            int addressNumbers = c.countValues(Contact.ADDR);
                            for (int i = 0; i < addressNumbers; i++) {
                                com.codename1.contacts.Address ad = new com.codename1.contacts.Address();
                                int attributes = c.getAttributes(Contact.ADDR, i);
                                String[] addr = c.getStringArray(Contact.ADDR, i);
                                ad.setCountry(addr[Contact.ADDR_COUNTRY]);
                                ad.setLocality(addr[Contact.ADDR_LOCALITY]);
                                ad.setPostalCode(addr[Contact.ADDR_POSTALCODE]);
                                ad.setStreetAddress(addr[Contact.ADDR_STREET]);
                                ad.setRegion(addr[Contact.ADDR_REGION]);

                                if ((attributes != 0) & Contact.ATTR_HOME != 0) {
                                    address.put("home", ad);
                                } else if ((attributes != 0) & Contact.ATTR_WORK != 0) {
                                    address.put("work", ad);
                                } else {
                                    address.put("other", ad);
                                }
                            }
                            contact.setAddresses(address);
                        }
                    }

                    if (includesPicture) {
                        if (clist.isSupportedField(Contact.PHOTO) && c.countValues(Contact.PHOTO) > 0) {
                            byte[] photo = c.getBinary(Contact.PHOTO, 0);
                            photo = Base64.decode(photo);
                            contact.setPhoto(Image.createImage(photo, 0, photo.length));
                        }
                    }
                    contact.setId(uid);
                    contact.setFirstName(firstName);
                    contact.setFamilyName(familyName);
                    contact.setDisplayName(name);
                    contact.setNote(notes);
                    contact.setBirthday(bdate);
                    contact.setUrls(new String[]{url});
                    try {
                        clist.close();
                    } catch (PIMException ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        }

        return contact;
    }
}
