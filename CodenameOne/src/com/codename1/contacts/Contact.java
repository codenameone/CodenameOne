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

import com.codename1.ui.Image;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents a Contact information from the device Address Book
 * 
 * @author Chen
 */
public class Contact {
    
    private String id;
    
    private String name;
    
    private String displayName;
    
    private Hashtable phoneNumbers;
    
    private String primaryPhoneNumber;
    
    private Hashtable emails;
    
    private String primaryEmail;
    
    private Hashtable addresses;
    
    private long birthday;
    
    private String note;
    
    private Image photo;
    
    private String [] urls;
    
    /**
     * Empty Constructor
     */
    public Contact() {
    }

    /**
     * Gets the Contact Addresses, the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the Address, types can
     * be: "home", "work", "other" the value is an Address Object.
     * @return Hashtable of available addresses
     */
    public Hashtable getAddresses() {
        return addresses;
    }

    /**
     * Gets the Contact birthday
     * @return birth time
     */
    public long getBirthday() {
        return birthday;
    }

    /**
     * Gets the Contact Display Name
     * @return Display Name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the Contact Emails, the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the Email, types can
     * be: "home", "mobile", "work", "other" the value is String of the email.
     * @return Hashtable of available emails
     */
    public Hashtable getEmails() {
        return emails;
    }

    /**
     * Gets the Contact unique id
     * @return Contact unique id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets Contact Name
     * @return Contact Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets Contact Note
     * @return Contact Note
     */
    public String getNote() {
        return note;
    }

    /**
     * Gets the Contact phone numbers, the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the phone number, types can
     * be: "home", "mobile", "work", "fax", "other" the value is String of the 
     * phone number.
     * @return Hashtable of available phone numbers
     */
    public Hashtable getPhoneNumbers() {
        return phoneNumbers;
    }

    /**
     * Gets the Contact photo
     * @return the Contact Photo or null if not available
     */
    public Image getPhoto() {
        return photo;
    }

    /**
     * Gets the primary email of this Contact, notice this can be null even though
     * the Contact has emails declared
     * @return the Contact primary email or null if not declared
     */
    public String getPrimaryEmail() {
        return primaryEmail;
    }

    /**
     * Gets the primary phone number of this Contact, notice this can be null 
     * even though the Contact has phone numbers declared
     * @return the Contact primary phone number or null if not declared
     */
    public String getPrimaryPhoneNumber() {
        return primaryPhoneNumber;
    }

    /**
     * Sets the Contact Addresses
     * @param addresses the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the Address, types can
     * be: "home", "work", "other" the value is an Address Object.
     */
    public void setAddresses(Hashtable addresses) {
        this.addresses = addresses;
    }

    /**
     * Sets the Contact birthday date
     * @param birthday 
     */
    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    /**
     * Sets the Contact display name
     * @param displayName 
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the Contact emails
     * @param emails the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the Email, types can
     * be: "home", "mobile", "work", "other" the value is String of the email.
     */
    public void setEmails(Hashtable emails) {
        this.emails = emails;
    }

    /**
     * Sets the Contact unique id
     * @param id 
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets Contact name
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets Contact note
     * @param note 
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Sets Contact phone numbers
     * @param phoneNumbers the Hashtable contains key/value pairs where
     * the key is a String which represents the type of the phone number, types can
     * be: "home", "mobile", "work", "fax", "other" the value is String of the 
     * phone number.
     */
    public void setPhoneNumbers(Hashtable phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    /**
     * Sets Contact photo
     * @param photo 
     */
    public void setPhoto(Image photo) {
        this.photo = photo;
    }

    /**
     * Sets Contact primary email
     * @param primaryEmail 
     */
    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    /**
     * Sets Contact primary phone number
     * @param primaryPhoneNumber 
     */
    public void setPrimaryPhoneNumber(String primaryPhoneNumber) {
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    /**
     * Gets Contact urls
     * @return 
     */
    public String[] getUrls() {
        return urls;
    }

    /**
     * Sets Contact urls
     * @param urls 
     */
    public void setUrls(String[] urls) {
        this.urls = urls;
    }
    
}
