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
import java.util.Vector;

/**
 * This class uses as the Contacts manager of the device, it enables the possibility
 * To get the Contacts that are available on the device.
 *
 * @author Chen
 */
public class ContactsManager {
    
    /**
     * This method returns all contacts IDs 
     * @return array of contacts IDs
     */
    public static String [] getAllContacts(){
        return Display.getInstance().getAllContacts(false);
    }
    
    /**
     * This method returns all contacts that has a phone number
     * @return array of contacts IDs
     */
    public static String [] getAllContactsWithNumbers(){
        return Display.getInstance().getAllContacts(true);
    }
    
    /**
     * This method returns a Contact by the contact id
     * @param id of the Contact
     * @return a Contact Object
     */
    public static Contact getContactById(String id){
        return Display.getInstance().getContactById(id);
    }
    
    /**
     * This method returns a Contact by the contact id and fills it's data
     * according to the given flags
     * 
     * @param id of the Contact
     * @param includesFullName if true try to fetch the full name of the Contact(not just display name)
     * @param includesPicture if true try to fetch the Contact Picture if exists
     * @param includesNumbers if true try to fetch all Contact numbers
     * @param includesEmail if ture try to fetch all Contact Emails
     * @param includeAddress if ture try to fetch all Contact Addresses
     * 
     * @return a Contact Object
     */
    public static Contact getContactById(String id, boolean includesFullName, boolean includesPicture, 
            boolean includesNumbers, boolean includesEmail, boolean includeAddress){
        return Display.getInstance().getContactById(id, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);        
    }
    
    /**
     * Create a contact to the device contacts book
     * 
     * @param firstName the Contact firstName
     * @param familyName the Contact familyName
     * @param workPhone the Contact work phone or null
     * @param homePhone the Contact home phone or null
     * @param mobilePhone the Contact mobile phone or null
     * @param email the Contact email or null
     * 
     * @return the contact id if creation succeeded or null  if failed
     */ 
    public static String createContact(String firstName, String familyName, 
            String workPhone, String homePhone, String mobilePhone, 
            String email){
        return Display.getInstance().createContact(firstName, familyName, 
            workPhone, homePhone, mobilePhone, email);
    }
    
    /**
     * removed a contact from the device contacts book
     * @param id the contact id to remove
     * @return true if deletion succeeded false otherwise
     */ 
    public static boolean deleteContact(String id){
        return Display.getInstance().deleteContact(id);
    }
}
