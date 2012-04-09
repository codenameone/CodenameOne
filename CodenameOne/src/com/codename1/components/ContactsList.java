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
package com.codename1.components;

import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactsManager;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.ListCellRenderer;
import java.util.Hashtable;

/**
 * This is a Contacts List.
 * This List brings the Contacts in an async matter, therefore provides high 
 * performance even when contacts list are very big.
 * 
 * @author Chen
 */
public class ContactsList extends List {

    private boolean displayPhone;
    
    private String [] contactIds;
    
    private static int paging = 10;
    
    /**
     * ContactsList Constructor 
     * @param contactIds the contact ids to display
     * @param displayPhone if true the item will display the Contact phone number
     * else it will display the contact email.
     */
    public ContactsList(String [] contactIds, boolean displayPhone) {
        this.displayPhone = displayPhone;
        this.contactIds = contactIds;
        setRenderer(new ContactsRenderer());
        fetchContacts(0);
    }

    /**
     * Gets the selected Contact phone number
     * @return phone number
     */
    public String getSelectedPhoneNumber() {
        Contact selected = (Contact) getSelectedItem();
        return getContactPhoneNumber(selected);
    }
    
    /**
     * Gets the selected Contact email
     * @return email
     */
    public String getSelectedEmail() {
        Contact selected = (Contact) getSelectedItem();
        return getContactEmail(selected);
    }
    
    private String getContactPhoneNumber(Contact contact) {
        if (contact.getPrimaryPhoneNumber() != null) {
            return contact.getPrimaryPhoneNumber();
        }
        Hashtable phones = contact.getPhoneNumbers();
        if (phones.size() > 0) {
            String first = (String) phones.keys().nextElement();
            return (String) phones.get(first);

        }
        return null;
    }
    
    
    private String getContactEmail(Contact contact){
        if (contact.getPrimaryEmail() != null) {
            return contact.getPrimaryEmail();
        }
        Hashtable emails = contact.getEmails();
        if (emails.size() > 0) {
            String first = (String) emails.keys().nextElement();
            return (String) emails.get(first);
        }
        return null;
    
    }
    
    
    private void fetchContacts(final int index) {
        new Thread(new Runnable() {

            public void run() {
                for (int i = index; i < index + paging && i < contactIds.length; i++) {
                    Contact c = ContactsManager.getContactById(contactIds[i]);
                    addItem(c);
                }
            }
        }).start();
    }

    class ContactsRenderer extends Container implements ListCellRenderer {

        private Label name = new Label("");
        private Label phoneOrEmail = new Label("");
        private Label pic = new Label("");
        private Label focus = new Label("");

        /**
         * Contacts Renderer has two display modes: displays the Contacts phone number
         * or displays the Contacts number.
         * 
         * @param displayPhone if true the phone number is displayed else the e-mail
         * is displayed
         */
        public ContactsRenderer() {
            setLayout(new BorderLayout());
            addComponent(BorderLayout.WEST, pic);
            Container cnt = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            name.getStyle().setBgTransparency(0);
            phoneOrEmail.getStyle().setBgTransparency(0);
            pic.getStyle().setBgTransparency(0);
            cnt.addComponent(name);
            cnt.addComponent(phoneOrEmail);
            addComponent(BorderLayout.CENTER, cnt);
            setUIID("ListRenderer");
            focus.setUIID("ListRendererFocus");
            focus.setFocus(true);
        }

        /**
         * @inheritDoc
         */
        public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {

            if ((index + 1) >= list.size()) {
                fetchContacts(index + 1);
            }
            
            Contact person = (Contact) value;
            name.setText(person.getDisplayName());
            String val;
            if (displayPhone) {
                val = getContactPhoneNumber(person);
            } else {
                val = getContactEmail(person);
            }
            
            if (val != null) {
                phoneOrEmail.setText(val);
            } else {
                phoneOrEmail.setText("");
            }

            pic.setIcon(person.getPhoto());
            return this;
        }

        /**
         * @inheritDoc
         */
        public Component getListFocusComponent(List list) {
            return focus;
        }
    }
}
