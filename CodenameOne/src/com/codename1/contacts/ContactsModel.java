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
import com.codename1.ui.Image;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This Contacts model is responsible for querying Contacts from the device
 * and to cache the data for faster usage
 * 
 * @author Chen
 */
public class ContactsModel extends DefaultListModel {

    private Hashtable contactsCache = new Hashtable();

    private Image placeHolder;
    
    /**
     * Constructor with contacts ids
     * @param ids the contact ids we would like this model to handle
     */
    public ContactsModel(String[] ids) {
        super(ids);
    }
    
    /**
     * Sets the Contacts place holder image.
     * 
     * @param placeHolder image place holder for the contacts
     */
    public void setPlaceHolderImage(Image placeHolder){
        this.placeHolder = placeHolder;
    }
    
    /**
     * @inheritDoc
     */
    public Object getItemAt(final int index) {
        final String id = (String) super.getItemAt(index);
        Hashtable contact = (Hashtable) contactsCache.get(id);
        if (contact == null) {
            Hashtable cnt = getContactAsHashtable(null);
            contactsCache.put(id, cnt);
            Display.getInstance().scheduleBackgroundTask(new Runnable() {                
                public void run() {
                    Contact c = ContactsManager.getContactById(id);
                    Hashtable contact = getContactAsHashtable(c);
                    contactsCache.put(id, contact);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            setItem(index, id);
                        }
                    });
                }
            });
            return cnt;
        }
        return contact;
    }

    /**
     * @inheritDoc
     */
    public void addItem(Object item) {
        if (item instanceof String) {
            super.addItem(item);
        } else if (item instanceof Contact) {
            super.addItem(((Contact) item).getId());
            contactsCache.put(((Contact) item).getId(), getContactAsHashtable((Contact) item));

        } else if (item instanceof Hashtable) {
            super.addItem(((Hashtable) item).get("id"));
            contactsCache.put(((Hashtable) item).get("id"), item);

        }
    }

    /**
     * @inheritDoc
     */
    public void removeItem(int index) {
        String id = (String) super.getItemAt(index);
        super.removeItem(index);
        contactsCache.remove(id);
    }

    private Hashtable getContactAsHashtable(Contact c) {
        Hashtable table = new Hashtable();
        String id = null;
        String fname;
        String lname;
        String displayName;
        Image image;
        String phone;
        String email;
        
        if(c == null){
            fname = "Loading...";
            lname = "Loading...";
            displayName = "Loading...";
            image = placeHolder;
            phone = "Loading...";
            email = "Loading...";            
        }else{
            id = c.getId();
            fname = c.getFirstName();
            lname = c.getFamilyName();
            displayName = c.getDisplayName();
            image = c.getPhoto();
            if(image == null){
                image = placeHolder;
            }
            phone = getContactPhoneNumber(c);
            email = getContactEmail(c);                    
        }
        addAttribute(table, "id", id);
        addAttribute(table, "fname", fname);
        addAttribute(table, "lname", lname);
        addAttribute(table, "displayName", displayName);
        addAttribute(table, "icon", image);
        addAttribute(table, "phone", phone);
        addAttribute(table, "email", email);
        
        //the keys in the Hashtable should be enough for most use-cases, in case
        //something is missing there is the ability to get the Contact.
        if(c != null){
            table.put("contact", c);
        }
        return table;
    }
    
    private void addAttribute(Hashtable table, String key, Object value) {
        if(value == null || key == null){
            return;
        }
        table.put(key, value);
    }

    
    private String getContactPhoneNumber(Contact contact) {
        if (contact.getPrimaryPhoneNumber() != null) {
            return contact.getPrimaryPhoneNumber();
        }
        Hashtable phones = contact.getPhoneNumbers();
        if (phones != null && phones.size() > 0) {
            String first = (String) phones.keys().nextElement();
            return (String) phones.get(first);

        }
        return null;
    }

    private String getContactEmail(Contact contact) {
        if (contact.getPrimaryEmail() != null) {
            return contact.getPrimaryEmail();
        }
        Hashtable emails = contact.getEmails();
        if (emails != null && emails.size() > 0) {
            String first = (String) emails.keys().nextElement();
            return (String) emails.get(first);
        }
        return null;

    }
}
