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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;

import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactPicker;
import com.codename1.io.Log;
import com.codename1.ui.EncodedImage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the platform's contact picker and turns its answer into
 * {@link Contact} objects, without ever asking for {@code READ_CONTACTS}.
 *
 * <p>Two mechanisms sit behind one entry point.</p>
 *
 * <p>From Android 17 (API level 37) the system owns a real contacts picker:
 * {@code android.provider.action.PICK_CONTACTS} shows it, the app names the
 * MIME types it wants, and the result is a single session URI that projects
 * rows out of {@link ContactsContract.Data} for the selected contacts only.
 * That is the path Google Play's 2027-01-27 contacts policy expects an app
 * to be on when broad address-book access is not core functionality.</p>
 *
 * <p>Before that the closest equivalent is {@link Intent#ACTION_PICK} against
 * one of the contacts content URIs. The contacts app returns a URI for the
 * single row the user chose and grants read access to just that row, so it
 * needs no permission either -- but it can only return one row, of one kind.
 * Everything the older path cannot do is dropped rather than escalated:
 * asking for a second field, or for several contacts, never turns into a
 * permission prompt behind the caller's back.</p>
 *
 * <p>The API 37 names are written out as literals because the port compiles
 * against a much older {@code android.jar} and cannot reference
 * {@code android.provider.ContactsPickerSessionContract} at all. They are
 * copied from that class in AOSP; a wrong one would surface as
 * {@link ActivityNotFoundException}, which is handled the same way as a
 * device with no contacts app.</p>
 */
class AndroidContactPicker {

    /**
     * Android 17. {@code Build.VERSION_CODES} in the android.jar this port
     * compiles against stops long before it.
     */
    static final int SDK_INT_ANDROID_17 = 37;

    /** {@code ContactsPickerSessionContract.ACTION_PICK_CONTACTS}. */
    static final String ACTION_PICK_CONTACTS =
            "android.provider.action.PICK_CONTACTS";

    /**
     * {@code ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_REQUESTED_DATA_FIELDS},
     * an {@code ArrayList<String>} of MIME types. The picker requires it: it
     * decides both which contacts are offered and which rows come back.
     */
    static final String EXTRA_REQUESTED_DATA_FIELDS =
            "android.provider.extra.PICK_CONTACTS_REQUESTED_DATA_FIELDS";

    /** {@code ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_MATCH_ALL_DATA_FIELDS}. */
    static final String EXTRA_MATCH_ALL_DATA_FIELDS =
            "android.provider.extra.PICK_CONTACTS_MATCH_ALL_DATA_FIELDS";

    /** {@code ContactsPickerSessionContract.EXTRA_PICK_CONTACTS_SELECTION_LIMIT}. */
    static final String EXTRA_SELECTION_LIMIT =
            "android.provider.extra.PICK_CONTACTS_SELECTION_LIMIT";

    /**
     * The request code handed to {@code startActivityForResult}. Picked well
     * clear of the small integers {@link IntentResultListener} hands out.
     */
    static final int PICK_CONTACTS_REQUEST = 4212;

    private AndroidContactPicker() {
    }

    /**
     * The MIME types that correspond to a set of
     * {@link ContactPicker} field constants, in a stable order.
     *
     * <p>Android rejects the whole request with an
     * {@code IllegalArgumentException} if it is handed a MIME type outside
     * the list its picker accepts, so nothing is mapped speculatively --
     * a field with no accepted MIME type is simply not requested.</p>
     *
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @return the MIME types to request, never null and never empty
     */
    static ArrayList<String> requestedMimeTypes(int requestedFields) {
        ArrayList<String> types = new ArrayList<String>();
        if ((requestedFields & ContactPicker.NAME) != 0) {
            types.add(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.PHONE) != 0) {
            types.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.EMAIL) != 0) {
            types.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.ADDRESS) != 0) {
            types.add(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.PHOTO) != 0) {
            types.add(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.BIRTHDAY) != 0) {
            types.add(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
        }
        if ((requestedFields & ContactPicker.WEBSITE) != 0) {
            types.add(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
        }
        if (types.isEmpty()) {
            // ContactPicker refuses an empty field set, so this is only
            // reachable from the implementation API. The picker still has to
            // be told something, and a name is the one field every contact
            // has.
            types.add(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        }
        return types;
    }

    /**
     * Builds the intent that shows the system picker on Android 17 and later.
     *
     * @param requestedFields          bit set of {@link ContactPicker} constants
     * @param multiSelect              true to allow more than one contact
     * @param selectionLimit           the largest selection the user may make
     * @param requireAllRequestedFields true to offer only contacts holding
     *                                 every requested field
     * @return the intent to launch
     */
    static Intent sessionPickerIntent(int requestedFields, boolean multiSelect,
            int selectionLimit, boolean requireAllRequestedFields) {
        Intent intent = new Intent(ACTION_PICK_CONTACTS);
        intent.putStringArrayListExtra(EXTRA_REQUESTED_DATA_FIELDS,
                requestedMimeTypes(requestedFields));
        intent.putExtra(EXTRA_MATCH_ALL_DATA_FIELDS, requireAllRequestedFields);
        if (multiSelect) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(EXTRA_SELECTION_LIMIT, Math.max(1,
                    Math.min(selectionLimit, ContactPicker.MAXIMUM_SELECTION_LIMIT)));
        }
        return intent;
    }

    /**
     * Builds the intent that shows the contacts app's own single-row picker
     * on Android before 17.
     *
     * <p>The data URI decides what the user is choosing and what the returned
     * row holds. Picking a phone number returns that number under one grant,
     * which is why the most specific requested field wins: a request naming
     * several of them gets whichever one this ranking puts first rather than
     * a permission prompt.</p>
     *
     * <p>A request for a photo, a birthday or a web site has no content URI
     * of its own -- {@code ACTION_PICK} offers none -- so it falls through to
     * the contact, and those fields are read afterwards through the directory
     * hanging off the granted contact URI. See
     * {@code applyContactEntity}.</p>
     *
     * <p>{@code requireAllRequestedFields} has no effect here. The pre-17
     * picker takes no predicate, so every contact is offered and the caller
     * has to cope with one that turned out to be missing a field -- which is
     * what its own documentation says a platform may do. Narrowing it any
     * further would mean reading the address book to decide, which is the
     * thing this path exists to avoid.</p>
     *
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @return the intent to launch
     */
    static Intent legacyPickerIntent(int requestedFields) {
        Uri data;
        if ((requestedFields & ContactPicker.PHONE) != 0) {
            data = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        } else if ((requestedFields & ContactPicker.EMAIL) != 0) {
            data = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
        } else if ((requestedFields & ContactPicker.ADDRESS) != 0) {
            data = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI;
        } else {
            data = ContactsContract.Contacts.CONTENT_URI;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, data);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    /**
     * Shows the picker.
     *
     * @param context         used to resolve content
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @param multiSelect     true to allow more than one contact
     * @param selectionLimit  the largest selection the user may make
     * @param requireAllRequestedFields true to offer only contacts holding
     *                        every requested field
     * @param result          invoked with the selection, on the calling
     *                        activity's result thread
     */
    static void pick(final Context context, final int requestedFields,
            boolean multiSelect, int selectionLimit,
            boolean requireAllRequestedFields, final Result result) {
        final boolean session = Build.VERSION.SDK_INT >= SDK_INT_ANDROID_17;
        Intent intent = session
                ? sessionPickerIntent(requestedFields, multiSelect,
                        selectionLimit, requireAllRequestedFields)
                : legacyPickerIntent(requestedFields);
        try {
            AndroidNativeUtil.startActivityForResult(intent,
                    PICK_CONTACTS_REQUEST, new IntentResultListener() {
                @Override
                public void onActivityResult(int requestCode, int resultCode,
                        Intent data) {
                    result.picked(read(context, resultCode, data,
                            requestedFields, session));
                }
            });
        } catch (ActivityNotFoundException err) {
            // No contacts app, or a build of Android 17 whose picker is not
            // installed. Reporting nothing is the whole contract of a
            // cancelled pick, so the caller needs no separate case for it.
            Log.e(err);
            result.picked(new Contact[0]);
        } catch (RuntimeException err) {
            Log.e(err);
            result.picked(new Contact[0]);
        }
    }

    /**
     * Turns an activity result into contacts.
     *
     * @param context         used to resolve content
     * @param resultCode      the activity result code
     * @param data            the result intent, whose data is the URI to read
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @param session         true when the Android 17 session URI was used
     * @return the selection, never null
     */
    static Contact[] read(Context context, int resultCode, Intent data,
            int requestedFields, boolean session) {
        if (resultCode != Activity.RESULT_OK || data == null
                || data.getData() == null) {
            return new Contact[0];
        }
        Cursor cursor = null;
        Uri picked = data.getData();
        try {
            ContentResolver resolver = context.getContentResolver();
            // A null projection rather than a chosen one. The session
            // provider decides its own column set and throws on a column it
            // does not recognise, and the legacy path is reading three
            // different content URIs whose columns differ; asking for
            // whatever each has and tolerating a missing column is the only
            // shape that works for both.
            cursor = resolver.query(picked, null, null, null, null);
            if (cursor == null) {
                return new Contact[0];
            }
            return session
                    ? readSession(cursor, requestedFields)
                    : readLegacyRow(cursor, resolver, picked, requestedFields);
        } catch (SecurityException err) {
            // The temporary grant is gone, which is what happens when the
            // result is handled after the process was rebuilt.
            Log.e(err);
            return new Contact[0];
        } catch (RuntimeException err) {
            Log.e(err);
            return new Contact[0];
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Reads the Android 17 session cursor, which holds one row per data item
     * across every selected contact.
     *
     * @param cursor          the session cursor
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @return one contact per distinct selected person, in the order the
     *         picker reported them
     */
    private static Contact[] readSession(Cursor cursor, int requestedFields) {
        // Insertion ordered so the selection keeps the order the picker
        // reported, which is the order the user sees.
        Map<String, Contact> byPerson = new LinkedHashMap<String, Contact>();
        int mimeColumn = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
        int lookupColumn = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        int idColumn = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);
        int nameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        while (cursor.moveToNext()) {
            String key = column(cursor, lookupColumn);
            if (key == null) {
                key = column(cursor, idColumn);
            }
            if (key == null) {
                // Without something to group by, every row would look like a
                // separate person. One synthetic key per row is wrong in the
                // other direction but at least loses nothing.
                key = "row" + cursor.getPosition();
            }
            Contact contact = byPerson.get(key);
            if (contact == null) {
                contact = new Contact();
                contact.setId(key);
                // Gated on NAME. The session cursor joins the owner's display
                // name onto every row, so a phone-only request would come
                // back carrying the name as well -- which is data the caller
                // deliberately did not ask for and which this API promises
                // not to hand over.
                if ((requestedFields & ContactPicker.NAME) != 0) {
                    String display = column(cursor, nameColumn);
                    if (display != null) {
                        contact.setDisplayName(display);
                    }
                }
                byPerson.put(key, contact);
            }
            applyRow(contact, cursor, column(cursor, mimeColumn),
                    requestedFields);
        }
        return byPerson.values().toArray(new Contact[byPerson.size()]);
    }

    /**
     * Reads the single row the pre-17 contacts app returns.
     *
     * <p>Which content URI was picked from decides what the row is: a phone,
     * email or postal data row, or a contact row when only a name was asked
     * for. The data rows carry the owner's display name as a joined column,
     * so a name always comes back even though no name row was read.</p>
     *
     * @param cursor          the cursor over the returned URI
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @return the single picked contact, or an empty array
     */
    private static Contact[] readLegacyRow(Cursor cursor, ContentResolver resolver,
            Uri picked, int requestedFields) {
        if (!cursor.moveToFirst()) {
            return new Contact[0];
        }
        Contact contact = new Contact();
        String id = column(cursor,
                cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        if (id == null) {
            id = column(cursor, cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
        }
        if (id == null) {
            id = column(cursor, cursor.getColumnIndex(ContactsContract.Contacts._ID));
        }
        contact.setId(id);
        // Gated on NAME for the same reason as the session path: a data row
        // carries the owner's display name as a joined column, so an
        // ungated read would hand a phone-only caller the name too.
        if ((requestedFields & ContactPicker.NAME) != 0) {
            String display = column(cursor,
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            if (display != null) {
                contact.setDisplayName(display);
            }
        }
        int mimeColumn = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE);
        String mime = column(cursor, mimeColumn);
        if (mimeColumn < 0) {
            // No MIMETYPE column at all, so this is the contact row rather
            // than a data row -- what the ranking picks when the request
            // wants something no ACTION_PICK content URI offers. The row
            // itself holds only the name, so the data rows are read through
            // the directory hanging off the same granted URI.
            applyContactEntity(contact, resolver, picked, requestedFields);
            return new Contact[]{contact};
        }
        if (mime == null) {
            // The column exists but this row left it empty, which is a
            // contacts app answering from an older view of the same table.
            // Reading it as the kind the request asked for costs a few null
            // column lookups and is the difference between returning the
            // number the user picked and returning only their name.
            mime = legacyMimeType(requestedFields);
        }
        applyRow(contact, cursor, mime, requestedFields);
        return new Contact[]{contact};
    }

    /**
     * Reads the picked contact's data rows through the directory that hangs
     * off the granted contact URI.
     *
     * <p>This is the only way the pre-17 path can answer a request for a
     * photo, a birthday or a web site: no {@code ACTION_PICK} content URI
     * offers those kinds, so the ranking falls through to the contact itself,
     * and the contact row carries nothing but the name.</p>
     *
     * <p>Whether the URI grant reaches the directory is a property of the
     * device's contacts provider rather than something the API can promise,
     * so a refusal is caught and the caller simply gets the fields that were
     * readable. That is the same outcome as before this existed -- it can
     * only add fields, never lose one -- and it never falls back to asking
     * for {@code READ_CONTACTS}, which is the whole point of the picker.</p>
     *
     * @param contact         the contact being assembled
     * @param resolver        used to query the directory
     * @param contactUri      the URI the contacts app returned and granted
     * @param requestedFields bit set of {@link ContactPicker} constants
     */
    private static void applyContactEntity(Contact contact, ContentResolver resolver,
            Uri contactUri, int requestedFields) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // ContactsContract.Contacts.Entity arrived in API 11, and merely
            // naming it on an older device is a NoClassDefFoundError rather
            // than something the catch below could see.
            return;
        }
        Cursor rows = null;
        try {
            rows = resolver.query(Uri.withAppendedPath(contactUri,
                    ContactsContract.Contacts.Entity.CONTENT_DIRECTORY),
                    null, null, null, null);
            if (rows == null) {
                return;
            }
            int mimeColumn = rows.getColumnIndex(
                    ContactsContract.Contacts.Entity.MIMETYPE);
            while (rows.moveToNext()) {
                applyRow(contact, rows, column(rows, mimeColumn), requestedFields);
            }
        } catch (SecurityException err) {
            Log.p("Contact picker: the grant does not reach this contact's "
                    + "data rows, returning the fields that were readable");
        } catch (RuntimeException err) {
            Log.e(err);
        } finally {
            if (rows != null) {
                rows.close();
            }
        }
    }

    /**
     * The MIME type of the row {@link #legacyPickerIntent(int)} asked for.
     *
     * <p>Same ranking, deliberately: the two have to agree about what the
     * single returned row is, or the row is parsed as something it is not.</p>
     *
     * @param requestedFields bit set of {@link ContactPicker} constants
     * @return the MIME type of the picked row, never null
     */
    static String legacyMimeType(int requestedFields) {
        if ((requestedFields & ContactPicker.PHONE) != 0) {
            return ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
        }
        if ((requestedFields & ContactPicker.EMAIL) != 0) {
            return ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
        }
        if ((requestedFields & ContactPicker.ADDRESS) != 0) {
            return ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE;
        }
        return ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE;
    }

    /**
     * Copies one data row onto the contact it belongs to.
     *
     * @param contact         the contact being assembled
     * @param cursor          positioned on the row
     * @param mime            the row's MIME type, which may be null
     * @param requestedFields bit set of {@link ContactPicker} constants, used
     *                        so a row the platform volunteered but the caller
     *                        did not ask for is dropped rather than stored
     */
    private static void applyRow(Contact contact, Cursor cursor, String mime,
            int requestedFields) {
        if (mime == null) {
            return;
        }
        if (mime.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.NAME) != 0) {
                applyName(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.PHONE) != 0) {
                applyPhone(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.EMAIL) != 0) {
                applyEmail(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.ADDRESS) != 0) {
                applyAddress(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.PHOTO) != 0) {
                applyPhoto(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.BIRTHDAY) != 0) {
                applyBirthday(contact, cursor);
            }
        } else if (mime.equals(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)) {
            if ((requestedFields & ContactPicker.WEBSITE) != 0) {
                applyWebsite(contact, cursor);
            }
        }
    }

    private static void applyName(Contact contact, Cursor cursor) {
        String given = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
        String family = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
        String display = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
        if (given != null) {
            contact.setFirstName(given);
        }
        if (family != null) {
            contact.setFamilyName(family);
        }
        if (display != null) {
            contact.setDisplayName(display);
        }
    }

    private static void applyPhone(Contact contact, Cursor cursor) {
        String number = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER));
        if (number == null) {
            return;
        }
        Hashtable numbers = contact.getPhoneNumbers();
        if (numbers == null) {
            numbers = new Hashtable();
            contact.setPhoneNumbers(numbers);
        }
        numbers.put(phoneLabel(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.TYPE))), number);
        if (contact.getPrimaryPhoneNumber() == null
                || isPrimary(cursor, ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) {
            contact.setPrimaryPhoneNumber(number);
        }
    }

    private static void applyEmail(Contact contact, Cursor cursor) {
        String address = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Email.DATA));
        if (address == null) {
            return;
        }
        Hashtable emails = contact.getEmails();
        if (emails == null) {
            emails = new Hashtable();
            contact.setEmails(emails);
        }
        emails.put(emailLabel(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Email.TYPE))), address);
        if (contact.getPrimaryEmail() == null
                || isPrimary(cursor, ContactsContract.CommonDataKinds.Email.IS_PRIMARY)) {
            contact.setPrimaryEmail(address);
        }
    }

    private static void applyAddress(Contact contact, Cursor cursor) {
        Address address = new Address();
        address.setStreetAddress(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.STREET)));
        address.setLocality(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.CITY)));
        address.setRegion(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.REGION)));
        address.setPostalCode(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)));
        address.setCountry(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)));
        Hashtable addresses = contact.getAddresses();
        if (addresses == null) {
            addresses = new Hashtable();
            contact.setAddresses(addresses);
        }
        addresses.put(addressLabel(column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE))), address);
    }

    private static void applyPhoto(Contact contact, Cursor cursor) {
        int photoColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Photo.PHOTO);
        if (photoColumn < 0 || cursor.isNull(photoColumn)) {
            return;
        }
        byte[] bytes = cursor.getBlob(photoColumn);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        // The blob is the thumbnail the contacts database stores, already
        // JPEG or PNG. EncodedImage keeps it that way and decodes on demand,
        // which matters when a hundred of them come back from one pick.
        contact.setPhoto(EncodedImage.create(bytes));
    }

    private static void applyBirthday(Contact contact, Cursor cursor) {
        int typeColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Event.TYPE);
        if (typeColumn >= 0 && cursor.getInt(typeColumn)
                != ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY) {
            // Anniversaries and custom events ride the same MIME type; only
            // the birthday has a home on Contact.
            return;
        }
        String start = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Event.START_DATE));
        if (start == null) {
            return;
        }
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(start);
            contact.setBirthday(parsed.getTime());
        } catch (ParseException err) {
            // A contact whose birthday has no year is stored as --MM-dd,
            // which no yyyy-MM-dd parse accepts. The rest of the contact is
            // still worth returning.
            Log.p("Unparsable contact birthday: " + start);
        }
    }

    private static void applyWebsite(Contact contact, Cursor cursor) {
        String url = column(cursor, cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Website.URL));
        if (url == null) {
            return;
        }
        String[] existing = contact.getUrls();
        List<String> urls = new ArrayList<String>();
        if (existing != null) {
            for (int iter = 0; iter < existing.length; iter++) {
                urls.add(existing[iter]);
            }
        }
        urls.add(url);
        contact.setUrls(urls.toArray(new String[urls.size()]));
    }

    /**
     * Reads a column, treating an absent column exactly like a null value.
     *
     * <p>Every read here goes through this. The three legacy content URIs and
     * the session provider each expose a different column set, and
     * {@link Cursor#getString(int)} on -1 throws rather than returning
     * null.</p>
     *
     * @param cursor positioned on a row
     * @param index  a column index, which may be -1
     * @return the value, or null when the column is absent or empty
     */
    private static String column(Cursor cursor, int index) {
        if (index < 0 || cursor.isNull(index)) {
            return null;
        }
        String value = cursor.getString(index);
        if (value == null || value.length() == 0) {
            return null;
        }
        return value;
    }

    private static boolean isPrimary(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 && !cursor.isNull(index) && cursor.getInt(index) != 0;
    }

    /**
     * Maps a phone type to the label the rest of the contacts API uses.
     *
     * <p>The strings match {@link AndroidContactsManager} so an application
     * that switched a lookup over to the picker keeps reading the same keys
     * out of {@link Contact#getPhoneNumbers()}.</p>
     *
     * @param type the raw {@code Phone.TYPE} value as text, may be null
     * @return the label, never null
     */
    static String phoneLabel(String type) {
        if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME).equals(type)) {
            return "home";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).equals(type)) {
            return "mobile";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK).equals(type)) {
            return "work";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME).equals(type)) {
            return "fax";
        }
        return "other";
    }

    /**
     * Maps an email type to the label {@link AndroidContactsManager} uses.
     *
     * @param type the raw {@code Email.TYPE} value as text, may be null
     * @return the label, never null
     */
    static String emailLabel(String type) {
        if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME).equals(type)) {
            return "home";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE).equals(type)) {
            return "mobile";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_WORK).equals(type)) {
            return "work";
        }
        return "other";
    }

    /**
     * Maps a postal type to the label {@link AndroidContactsManager} uses.
     *
     * @param type the raw {@code StructuredPostal.TYPE} value as text, may be
     *             null
     * @return the label, never null
     */
    static String addressLabel(String type) {
        if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME).equals(type)) {
            return "home";
        }
        if (String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK).equals(type)) {
            return "work";
        }
        return "other";
    }

    /** Receives the selection once the picker activity has answered. */
    interface Result {

        /**
         * @param picked the contacts the user selected, empty when they
         *               cancelled
         */
        void picked(Contact[] picked);
    }
}
