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
package com.codename1.builders;

/**
 * Decides whether an application needs the broad contacts permissions.
 *
 * <p>It used to be one line: anything under {@code com.codename1.contacts}
 * meant {@code READ_CONTACTS}. That was right while the only way to reach a
 * contact was to enumerate the address book. It stopped being right when
 * {@code ContactPicker} arrived, because the picker's whole purpose is to
 * hand over a user-selected contact <em>without</em> that permission, and it
 * necessarily traffics in {@code Contact} objects from the same package. An
 * app that only ever picks would still have shipped asking to read the
 * address book -- which from 2027-01-27 is exactly what Google Play makes an
 * app justify in a Play Console declaration.</p>
 *
 * <p>So the package is split by what a class actually does rather than by
 * where it lives. {@code Contact}, {@code Address} and {@code ContactPicker}
 * read nothing on their own; everything else in the package does. The split
 * is deliberately stated the safe way round: a class this file has never
 * heard of still asks for the permission, so adding one to the package
 * cannot silently strip a permission an app depends on. Only a class named
 * here can be permission-free, and each is named because it was checked.</p>
 *
 * <p>Two things follow from the scan being over the app's own classes rather
 * than the framework's. It sees a {@code Contact} reference the app makes and
 * not one the framework makes internally, so a picking app never trips the
 * broad rule. And it cannot see through a facade: a {@code Display} call that
 * reads the address book has to be named here by method, because after this
 * change its {@code Contact} return type no longer speaks for it.</p>
 *
 * <p><b>Keep this file in sync with
 * {@code com.codename1.build.daemon.ContactsPermissionScan}.</b></p>
 */
final class ContactsPermissionScan {

    /** Where the contacts API lives, with the trailing separator. */
    private static final String CONTACTS_PACKAGE = "com/codename1/contacts/";

    /**
     * Classes under {@link #CONTACTS_PACKAGE} that reach no address book.
     *
     * <p>{@code Contact} and {@code Address} are value objects with no
     * behaviour at all. {@code ContactPicker} launches the platform's picker,
     * which grants the app temporary access to the contacts the user chose
     * and nothing else.</p>
     */
    private static final String[] PERMISSION_FREE_CLASSES = {
        "Contact",
        "Address",
        "ContactPicker",
    };

    // Exempting Contact is safe even when the code that READ it lives in a
    // submitted library, which review has asked about three times. A cn1lib is
    // entirely client-side: CN1BuildMojo.mergeJars merges every
    // compile-classpath element except codenameone-core and java-runtime into
    // the -jar-with-dependencies.jar that becomes the dist.jar the client
    // uploads, so the library's own ContactsManager and Display references
    // are walked by the same scan of loose classes that sees the
    // application's.
    //
    // Measured rather than argued, on a real built artifact: the sample app's
    // dist.jar carries com/codename1/ads/mock/*.class -- classes belonging to
    // its cn1-ads-mock DEPENDENCY -- as loose entries stamped with that
    // library's own build date, and contains no nested jar or aar at all.
    // Nothing routed to the libraries directory holds Java classes, so
    // folding that tree in would add no signal and would risk the opposite
    // failure: a scanner over a directory that also receives framework
    // payloads reports usage for every application ever built.

    private static final String DISPLAY = "com/codename1/ui/Display";

    private static final String CONTACTS_MANAGER =
            "com/codename1/contacts/ContactsManager";

    /**
     * {@code Display} methods that read the address book.
     *
     * <p>Every one of these used to be caught indirectly, by the
     * {@code Contact} it returns or takes. Nothing catches them now, so they
     * are listed. {@code pickContacts} and {@code isContactPickerSupported}
     * are pointedly absent: they are the picker.</p>
     */
    private static final String[] DISPLAY_READ_METHODS = {
        "getAllContacts",
        "getContactById",
        "getLinkedContactIds",
        "refreshContacts",
        "isGetAllContactsFast",
        "isContactsPermissionGranted",
    };

    /** Methods that add to or remove from the address book. */
    private static final String[] WRITE_METHODS = {
        "createContact",
        "deleteContact",
    };

    private boolean read;

    private boolean write;

    /**
     * Reports a class the application references.
     *
     * @param cls internal name, slash separated
     */
    void usesClass(String cls) {
        if (cls == null || !cls.startsWith(CONTACTS_PACKAGE)) {
            return;
        }
        if (!permissionFree(cls.substring(CONTACTS_PACKAGE.length()))) {
            read = true;
        }
    }

    /**
     * Reports a method the application calls.
     *
     * @param cls    internal name of the declared owner, slash separated
     * @param method the method name
     */
    void usesClassMethod(String cls, String method) {
        if (cls == null || method == null) {
            return;
        }
        boolean contactsApi = DISPLAY.equals(cls)
                || CONTACTS_MANAGER.equals(cls);
        if (!contactsApi) {
            return;
        }
        for (int iter = 0; iter < WRITE_METHODS.length; iter++) {
            if (method.indexOf(WRITE_METHODS[iter]) > -1) {
                write = true;
                // Creating a contact reads nothing, so this deliberately does
                // not also set the read flag.
                return;
            }
        }
        for (int iter = 0; iter < DISPLAY_READ_METHODS.length; iter++) {
            if (method.indexOf(DISPLAY_READ_METHODS[iter]) > -1) {
                read = true;
                return;
            }
        }
    }

    /**
     * @return true if the generated manifest must request
     *         {@code READ_CONTACTS}
     */
    boolean readPermissionRequired() {
        return read;
    }

    /**
     * @return true if the generated manifest must request
     *         {@code WRITE_CONTACTS}
     */
    boolean writePermissionRequired() {
        return write;
    }

    /**
     * Whether a class in the contacts package reaches no address book.
     *
     * <p>A nested class counts as its outer class, so
     * {@code ContactPicker$1} -- which is what an anonymous listener inside a
     * picker call compiles to -- is judged as {@code ContactPicker} rather
     * than falling through to the permission-requiring default.</p>
     *
     * @param simpleName the part of the internal name after the package
     * @return true if referencing it implies no permission
     */
    private static boolean permissionFree(String simpleName) {
        String outer = simpleName;
        int nested = outer.indexOf('$');
        if (nested > 0) {
            outer = outer.substring(0, nested);
        }
        if (outer.indexOf('/') > -1) {
            // A subpackage of com.codename1.contacts. None exists today and
            // the safe answer for one that appears later is the permission.
            return false;
        }
        for (int iter = 0; iter < PERMISSION_FREE_CLASSES.length; iter++) {
            if (PERMISSION_FREE_CLASSES[iter].equals(outer)) {
                return true;
            }
        }
        return false;
    }
}
