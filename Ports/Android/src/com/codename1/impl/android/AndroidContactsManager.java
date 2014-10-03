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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.ContactsContract;
import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen
 */
public class AndroidContactsManager {

    private static AndroidContactsManager instance = new AndroidContactsManager();

    private AndroidContactsManager() {
    }

    public static AndroidContactsManager getInstance() {
        return instance;
    }

    public String[] getContacts(Activity context, boolean withNumbers) {
        Vector ids = new Vector();

        String selection = null;
        if (withNumbers) {
            selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1";
        }

        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            ids.add(contactId);
        }
        cursor.close();

        String[] contacts = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            String id = (String) ids.elementAt(i);
            contacts[i] = id;
        }
        return contacts;
    }
    
    public Contact getContact(Activity activity, String id) {
        return getContact(activity, id, true, true, true, true, true);
    }

    public static InputStream loadContactPhoto(ContentResolver cr, long id, long photo_id) {

        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input != null) {
            return input;
        }

        byte[] photoBytes = null;

        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photo_id);

        Cursor c = cr.query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);

        try {
            if (c.moveToFirst()) {
                photoBytes = c.getBlob(0);
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        } finally {
            c.close();
        }

        if (photoBytes != null) {
            return new ByteArrayInputStream(photoBytes);
        }

        return null;
    }

    public String createContact(final Activity activity, String firstName, String familyName, String officePhone, String homePhone, String cellPhone, String email) {
        
        String contactId = null;
        
        String displayName = firstName + " " + familyName;

        final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

        //------------------------------------------------------ Names
        if (firstName != null) {
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    firstName).build());
        }
        if (familyName != null) {
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                    familyName).build());
        }
        if (displayName != null) {
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                    displayName).build());
        }

        //------------------------------------------------------ Mobile Number                     
        if (cellPhone != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cellPhone).withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());
        }

        //------------------------------------------------------ Home Numbers
        if (homePhone != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, homePhone).withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME).build());
        }

        //------------------------------------------------------ Work Numbers
        if (officePhone != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, officePhone).withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK).build());
        }

        //------------------------------------------------------ Email
        if (email != null) {
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Email.DATA, email).withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK).build());
        }

        ContentProviderResult[] res;
        try {
            res = activity.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if (res != null && res[0] != null) {
                contactId = res[0].uri.getPath().substring(14);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contactId;
    }
    
    public boolean deleteContact(Activity activity, String id) {
        try {
            final ArrayList ops = new ArrayList();
            final ContentResolver cr = activity.getContentResolver();
            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI).withSelection(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                    + " = ?",
                    new String[]{id}).build());
            cr.applyBatch(ContactsContract.AUTHORITY, ops);
            ops.clear();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public Contact getContact(Activity activity, String id, boolean includesFullName, 
            boolean includesPicture, boolean includesNumbers, boolean includesEmail, 
            boolean includeAddress) {
        Contact retVal = new Contact();
        retVal.setId(id);

        ContentResolver cr = activity.getContentResolver();
 
        String[] projection = new String[] {ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_ID, 
                ContactsContract.Contacts.HAS_PHONE_NUMBER};
        
        Cursor result = cr.query(ContactsContract.Contacts.CONTENT_URI, projection,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{id}, null);
        if (result.moveToFirst()) {
            String name = result.getString(result.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            retVal.setDisplayName(name);
            if(includesPicture){
                String photoID = result.getString(result.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                if (photoID != null) {
                    InputStream input = loadContactPhoto(cr, Long.parseLong(id), Long.parseLong(photoID));
                    if (input != null) {
                        try {
                            retVal.setPhoto(Image.createImage(input));
                        } catch (IOException ex) {
                            Logger.getLogger(AndroidContactsManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        
        if(includesNumbers){
            Hashtable phones = new Hashtable();
            if (Integer.parseInt(result.getString(
                    result.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                //String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
                projection = new String[] {ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.NUMBER, 
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY};
                
                Cursor pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id}, null);
                while (pCur.moveToNext()) {
                    String type = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    String phone = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    boolean isPrimary = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) != 0;
                    if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME).equals(type)) {
                        type = "home";
                    } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).equals(type)) {
                        type = "mobile";
                    } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK).equals(type)) {
                        type = "work";
                    } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME).equals(type)) {
                        type = "fax";
                    } else {
                        type = "other";
                    }
                    if (isPrimary) {
                        retVal.setPrimaryPhoneNumber(phone);
                    }
                    phones.put(type, phone);
                }
                retVal.setPhoneNumbers(phones);
                pCur.close();
            }
        }
        
        if(includesEmail){
            Hashtable emails = new Hashtable();
            projection = new String[] {ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE, 
                    ContactsContract.CommonDataKinds.Email.IS_PRIMARY};
            
            Cursor emailCur = cr.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    projection,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    new String[]{id}, null);
            while (emailCur.moveToNext()) {
                // This would allow you get several email addresses
                // if the email addresses were stored in an array
                String email = emailCur.getString(
                        emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                String type = emailCur.getString(
                        emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                boolean isPrimary = emailCur.getInt(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)) != 0;

                if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME).equals(type)) {
                    type = "home";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE).equals(type)) {
                    type = "mobile";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_WORK).equals(type)) {
                    type = "work";
                } else {
                    type = "other";
                }
                if (isPrimary) {
                    retVal.setPrimaryEmail(email);
                }
                emails.put(type, email);
            }
            retVal.setEmails(emails);
            emailCur.close();
        }

        if(includesFullName){
            String birthWhere = ContactsContract.Data.CONTACT_ID + " = ? AND "
                    + ContactsContract.Data.MIMETYPE + "= ? AND "
                    + ContactsContract.CommonDataKinds.Event.TYPE + "="
                    + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
            String[] birthWhereParams = new String[]{id,
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
            };

            Cursor birthCur = cr.query(ContactsContract.Data.CONTENT_URI, null, birthWhere, birthWhereParams, null);
            if (birthCur.moveToFirst()) {
                String birth = birthCur.getString(birthCur.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE));
                Date bd = null;
                try {
                    bd = new SimpleDateFormat("yyyy-MM-dd").parse(birth);
                    retVal.setBirthday(bd.getTime());
                } catch (ParseException ex) {
                }
            }
            birthCur.close();
        
            String nameWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] nameWhereParams = new String[]{id,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
            projection = new String[] {ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, 
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME};
            
            Cursor nameCursor = cr.query(ContactsContract.Data.CONTENT_URI, projection,
                    nameWhere,
                    nameWhereParams, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);

            while (nameCursor.moveToNext()) {

                String given = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                String family = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                String display = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                retVal.setFirstName(given);
                retVal.setFamilyName(family);
                retVal.setDisplayName(display);

            }
            nameCursor.close();
            String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] noteWhereParams = new String[]{id,
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};
            projection = new String[] {ContactsContract.CommonDataKinds.Note.NOTE};
            Cursor noteCur = cr.query(ContactsContract.Data.CONTENT_URI, projection, noteWhere, noteWhereParams, null);
            if (noteCur.moveToFirst()) {
                String note = noteCur.getString(noteCur.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
                retVal.setNote(note);
            }
            noteCur.close();
        }
        
        if(includeAddress){
            Hashtable addresses = new Hashtable();
            String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
            String[] addrWhereParams = new String[]{id,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};
            projection = new String[] {ContactsContract.CommonDataKinds.StructuredPostal.POBOX, 
            ContactsContract.CommonDataKinds.StructuredPostal.STREET, 
            ContactsContract.CommonDataKinds.StructuredPostal.CITY,
            ContactsContract.CommonDataKinds.StructuredPostal.REGION,
            ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
            ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY,
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE};
            Cursor addrCur = cr.query(ContactsContract.Data.CONTENT_URI,
                    projection, addrWhere, addrWhereParams, null);
            while (addrCur.moveToNext()) {
                Address address = new Address();
                String poBox = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
                String street = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                String city = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                String state = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                String postalCode = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                String country = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                String type = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                address.setCountry(country);
                address.setLocality(city);
                address.setPostalCode(postalCode);
                address.setRegion(state);
                address.setStreetAddress(street);

                if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME).equals(type)) {
                    type = "home";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK).equals(type)) {
                    type = "work";
                } else {
                    type = "other";
                }

                addresses.put(type, address);
            }
            retVal.setAddresses(addresses);
            addrCur.close();
        }
        result.close();

        return retVal;
    }
    
    public Contact[] getAllContacts(Activity activity, boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        HashMap<String,Contact> contacts = new HashMap<String, Contact>();

        String selection = null;
        if (withNumbers) {
            selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1";
        }

        String[] projection = new String[] {ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_ID, 
                ContactsContract.Contacts.HAS_PHONE_NUMBER};

        ContentResolver contentResolver = activity.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Contact contact = new Contact();
            contact.setId(contactId);
            contact.setEmails(new Hashtable());
            Contact old = contacts.put(contactId, contact);
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            contact.setDisplayName(name);
            if(includesPicture){
                String photoID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                if (photoID != null) {
                    InputStream input = loadContactPhoto(contentResolver, Long.parseLong(contactId), Long.parseLong(photoID));
                    if (input != null) {
                        try {
                            contact.setPhoto(Image.createImage(input));
                        } catch (IOException ex) {
                            Logger.getLogger(AndroidContactsManager.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        cursor.close();

        if(includesNumbers){
            projection = new String[] {
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.NUMBER, 
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY};

            Cursor pCur = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection, null, null, null);
            while (pCur.moveToNext()) {
                String id = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                Hashtable phones = contact.getPhoneNumbers();
                if (phones == null) {
                    phones = new Hashtable();
                    contact.setPhoneNumbers(phones);
                }
                String type = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phone = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                boolean isPrimary = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) != 0;
                if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME).equals(type)) {
                    type = "home";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).equals(type)) {
                    type = "mobile";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK).equals(type)) {
                    type = "work";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME).equals(type)) {
                    type = "fax";
                } else {
                    type = "other";
                }
                if (isPrimary) {
                    contact.setPrimaryPhoneNumber(phone);
                }
                phones.put(type, phone);
            }
            pCur.close();
        }

        if(includesEmail){
            projection = new String[] {
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.DATA,
                    ContactsContract.CommonDataKinds.Email.TYPE, 
                    ContactsContract.CommonDataKinds.Email.IS_PRIMARY};

            Cursor emailCur = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    projection,
                    null, null, null);
            while (emailCur.moveToNext()) {
                String id = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                Hashtable emails = contact.getEmails();
                if (emails == null) {
                    emails = new Hashtable();
                    contact.setEmails(emails);
                }
                // This would allow you get several email addresses
                // if the email addresses were stored in an array
                String email = emailCur.getString(
                        emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                String type = emailCur.getString(
                        emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                boolean isPrimary = emailCur.getInt(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.IS_PRIMARY)) != 0;

                if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME).equals(type)) {
                    type = "home";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE).equals(type)) {
                    type = "mobile";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_WORK).equals(type)) {
                    type = "work";
                } else {
                    type = "other";
                }
                if (isPrimary) {
                    contact.setPrimaryEmail(email);
                }
                emails.put(type, email);
            }
            emailCur.close();
        }

        if(includesFullName){
            String birthWhere = ContactsContract.Data.MIMETYPE + "= ? AND "
                    + ContactsContract.CommonDataKinds.Event.TYPE + "="
                    + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY;
            String[] birthWhereParams = new String[]{ ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE };

            Cursor birthCur = contentResolver.query(ContactsContract.Data.CONTENT_URI, null, birthWhere, birthWhereParams, null);
            while (birthCur.moveToNext()) {
                String id = birthCur.getString(birthCur.getColumnIndex(ContactsContract.CommonDataKinds.Event.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                String birth = birthCur.getString(birthCur.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE));
                Date bd = null;
                try {
                    bd = new SimpleDateFormat("yyyy-MM-dd").parse(birth);
                    contact.setBirthday(bd.getTime());
                } catch (ParseException ex) {
                }
            }
            birthCur.close();

            String nameWhere = ContactsContract.Data.MIMETYPE + " = ?";
            String[] nameWhereParams = new String[]{ ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
            projection = new String[] {
                    ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID, 
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, 
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME};

            Cursor nameCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection,
                    nameWhere,
                    nameWhereParams, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);

            while (nameCursor.moveToNext()) {
                String id = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                String given = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                String family = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                String display = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                if (given != null) contact.setFirstName(given);
                if (family != null) contact.setFamilyName(family);
                if (display != null) contact.setDisplayName(display);
            }
            nameCursor.close();
            String noteWhere = ContactsContract.Data.MIMETYPE + " = ?";
            String[] noteWhereParams = new String[]{ ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE };
            projection = new String[] {ContactsContract.CommonDataKinds.Note.CONTACT_ID, ContactsContract.CommonDataKinds.Note.NOTE};
            Cursor noteCur = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, noteWhere, noteWhereParams, null);
            while (noteCur.moveToNext()) {
                String id = noteCur.getString(noteCur.getColumnIndex(ContactsContract.CommonDataKinds.Note.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                String note = noteCur.getString(noteCur.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
                contact.setNote(note);
            }
            noteCur.close();
        }

        if(includeAddress){
            String addrWhere = ContactsContract.Data.MIMETYPE + " = ?";
            String[] addrWhereParams = new String[]{ ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE };
            projection = new String[] {
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
                    ContactsContract.CommonDataKinds.StructuredPostal.POBOX, 
                    ContactsContract.CommonDataKinds.StructuredPostal.STREET, 
                    ContactsContract.CommonDataKinds.StructuredPostal.CITY,
                    ContactsContract.CommonDataKinds.StructuredPostal.REGION,
                    ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                    ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY,
                    ContactsContract.CommonDataKinds.StructuredPostal.TYPE};
            Cursor addrCur = contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, addrWhere, addrWhereParams, null);
            while (addrCur.moveToNext()) {
                String id = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID));
                Contact contact = contacts.get(id);
                if (contact == null) {
                    continue;
                }
                Hashtable addresses = contact.getAddresses();
                if (addresses == null) {
                    addresses = new Hashtable();
                    contact.setAddresses(addresses);
                }

                Address address = new Address();
//              String poBox = addrCur.getString(
//                      addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
                String street = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                String city = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                String state = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                String postalCode = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                String country = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                String type = addrCur.getString(
                        addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                address.setCountry(country);
                address.setLocality(city);
                address.setPostalCode(postalCode);
                address.setRegion(state);
                address.setStreetAddress(street);

                if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME).equals(type)) {
                    type = "home";
                } else if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK).equals(type)) {
                    type = "work";
                } else {
                    type = "other";
                }

                addresses.put(type, address);
                contact.setAddresses(addresses);
            }
            addrCur.close();
        }
        Contact[] contactsArray = new Contact[contacts.values().size()];
        contacts.values().toArray(contactsArray);
        return contactsArray;
    }
}
