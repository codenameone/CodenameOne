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
package com.codename1.impl.javase;

import com.codename1.contacts.Address;
import com.codename1.contacts.Contact;
import com.codename1.contacts.ContactPicker;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Stands in for the platform's contact picker inside the simulator.
 *
 * <p>The simulator has no address book to protect, so this exists for one
 * reason: a developer moving an app off {@code READ_CONTACTS} has to be able
 * to see what their picker flow does, and see it in the simulator rather than
 * only on a device. It behaves the way the real ones do in the ways that
 * matter to calling code -- it honours single versus multiple selection and
 * the selection limit, it drops contacts that do not satisfy
 * {@code requireAllRequestedFields}, and it hands back contacts carrying
 * <em>only</em> the requested fields, so an app reading a field it forgot to
 * ask for fails here rather than in the store.</p>
 *
 * <p>It draws Codename One components rather than Swing, so the picker
 * appears inside the simulated device, which is where a device picker
 * appears.</p>
 */
final class ContactPickerSimulation {

    private ContactPickerSimulation() {
    }

    /**
     * Shows the simulated picker.
     *
     * @param available                 the simulated address book
     * @param requestedFields           bit set of {@link ContactPicker} constants
     * @param multiSelect               true to allow more than one contact
     * @param selectionLimit            the largest selection the user may make
     * @param requireAllRequestedFields true to offer only contacts holding
     *                                  every requested field
     * @param response                  invoked on the EDT with the selection
     */
    static void pick(Hashtable available, final int requestedFields,
            final boolean multiSelect, final int selectionLimit,
            final boolean requireAllRequestedFields,
            final ActionListener<ActionEvent> response) {
        final List<Contact> offered = offer(available, requestedFields,
                requireAllRequestedFields);
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                show(offered, requestedFields, multiSelect, selectionLimit,
                        response);
            }
        });
    }

    /**
     * The contacts the picker is willing to show.
     *
     * @param available                 the simulated address book
     * @param requestedFields           bit set of {@link ContactPicker} constants
     * @param requireAllRequestedFields true to keep only contacts holding
     *                                  every requested field
     * @return the offered contacts, sorted by display name so the simulator
     *         shows a stable list across runs
     */
    private static List<Contact> offer(Hashtable available, int requestedFields,
            boolean requireAllRequestedFields) {
        List<Contact> offered = new ArrayList<Contact>();
        if (available == null) {
            return offered;
        }
        Enumeration keys = available.keys();
        while (keys.hasMoreElements()) {
            Contact contact = (Contact) available.get(keys.nextElement());
            if (contact == null) {
                continue;
            }
            if (holds(contact, requestedFields, requireAllRequestedFields)) {
                offered.add(contact);
            }
        }
        sortByDisplayName(offered);
        return offered;
    }

    private static void sortByDisplayName(List<Contact> contacts) {
        // A tiny list out of a Hashtable, so an insertion sort beats pulling
        // in a Comparator the port would otherwise have no use for.
        for (int iter = 1; iter < contacts.size(); iter++) {
            Contact current = contacts.get(iter);
            int pos = iter;
            while (pos > 0 && name(contacts.get(pos - 1))
                    .compareTo(name(current)) > 0) {
                contacts.set(pos, contacts.get(pos - 1));
                pos--;
            }
            contacts.set(pos, current);
        }
    }

    /**
     * The contact's own name, or null when it has none.
     *
     * <p>Deliberately never {@link Contact#getDisplayName()}. That getter
     * SYNTHESIZES a display name from the primary phone, the primary email or
     * the id when the contact has none -- and caches the result back into the
     * contact. Asking it whether a contact has a name therefore always
     * answers yes, which would let a nameless contact satisfy
     * {@code requireAllRequestedFields} and would hand the caller a phone
     * number as a display name. Worse, the caching would rewrite the
     * simulated address book permanently, so the second pick would see a
     * contact the first pick invented.</p>
     *
     * @param contact the simulated contact
     * @return the declared name, or null
     */
    static String declaredName(Contact contact) {
        String first = contact.getFirstName();
        String family = contact.getFamilyName();
        if (first != null && family != null) {
            return first + " " + family;
        }
        if (first != null) {
            return first;
        }
        return family;
    }

    private static String name(Contact contact) {
        String display = declaredName(contact);
        if (display != null) {
            return display;
        }
        return contact.getId() == null ? "" : contact.getId();
    }

    /**
     * Whether a contact satisfies the field request.
     *
     * @param contact     the candidate
     * @param fields      bit set of {@link ContactPicker} constants
     * @param requireAll  true to demand every requested field, false to
     *                    demand any one of them
     * @return true if the picker should offer this contact
     */
    static boolean holds(Contact contact, int fields, boolean requireAll) {
        int present = 0;
        int asked = 0;
        for (int bit = 1; bit <= ContactPicker.WEBSITE; bit <<= 1) {
            if ((fields & bit) == 0) {
                continue;
            }
            asked++;
            if (has(contact, bit)) {
                present++;
            }
        }
        if (asked == 0) {
            return true;
        }
        return requireAll ? present == asked : present > 0;
    }

    private static boolean has(Contact contact, int field) {
        switch (field) {
            case ContactPicker.NAME:
                return declaredName(contact) != null;
            case ContactPicker.PHONE:
                return notEmpty(contact.getPhoneNumbers());
            case ContactPicker.EMAIL:
                return notEmpty(contact.getEmails());
            case ContactPicker.ADDRESS:
                return notEmpty(contact.getAddresses());
            case ContactPicker.PHOTO:
                return contact.getPhoto() != null;
            case ContactPicker.BIRTHDAY:
                return contact.getBirthday() != 0;
            case ContactPicker.WEBSITE:
                return contact.getUrls() != null
                        && contact.getUrls().length > 0;
            default:
                return false;
        }
    }

    private static boolean notEmpty(Hashtable table) {
        return table != null && !table.isEmpty();
    }

    /**
     * Copies just the requested fields out of a contact.
     *
     * <p>The copy is the point. A real picker never hands over a field the
     * app did not ask for, and a simulator that handed over the whole record
     * would let an app pass here and come back empty from the device.</p>
     *
     * @param source the simulated contact
     * @param fields bit set of {@link ContactPicker} constants
     * @return a new contact holding only the requested fields
     */
    static Contact project(Contact source, int fields) {
        Contact out = new Contact();
        out.setId(source.getId());
        if ((fields & ContactPicker.NAME) != 0) {
            // declaredName, not getDisplayName: see the note there. A contact
            // with no name of its own comes back with a null display name,
            // which is what Android does when the picker returns no
            // structured-name row.
            out.setDisplayName(declaredName(source));
            out.setFirstName(source.getFirstName());
            out.setFamilyName(source.getFamilyName());
        }
        if ((fields & ContactPicker.PHONE) != 0) {
            out.setPhoneNumbers(copy(source.getPhoneNumbers()));
            out.setPrimaryPhoneNumber(primary(source.getPrimaryPhoneNumber(),
                    source.getPhoneNumbers()));
        }
        if ((fields & ContactPicker.EMAIL) != 0) {
            out.setEmails(copy(source.getEmails()));
            out.setPrimaryEmail(primary(source.getPrimaryEmail(),
                    source.getEmails()));
        }
        if ((fields & ContactPicker.ADDRESS) != 0) {
            out.setAddresses(copyAddresses(source.getAddresses()));
        }
        if ((fields & ContactPicker.PHOTO) != 0) {
            out.setPhoto(copyPhoto(source.getPhoto()));
        }
        if ((fields & ContactPicker.BIRTHDAY) != 0) {
            out.setBirthday(source.getBirthday());
        }
        if ((fields & ContactPicker.WEBSITE) != 0) {
            // Cloned, like every other field here. Handing the array over
            // would let the caller's edits reach back into the simulated
            // address book and into every later pick -- a picked contact is
            // documented as a snapshot, and Android and iOS both build a
            // fresh array.
            String[] urls = source.getUrls();
            if (urls != null) {
                String[] copy = new String[urls.length];
                System.arraycopy(urls, 0, copy, 0, urls.length);
                out.setUrls(copy);
            }
        }
        return out;
    }

    /**
     * The primary value for a contact that has entries but names no primary.
     *
     * <p>The simulated address book fills the tables and leaves the primary
     * unset, so copying it straight through handed the caller a contact with
     * numbers and no primary number -- and the documented NAME|PHONE example,
     * which reads exactly that, showed nothing in the simulator while working
     * on the device. Android and iOS both promote the first entry they read,
     * so the simulator does too.</p>
     *
     * <p>A Hashtable has no order to take a first entry from, so the label
     * ranking below stands in for one. That keeps the answer stable across
     * runs, which "whatever the iterator yields" would not.</p>
     *
     * @param declared the source's primary, used as-is when it has one
     * @param entries  the label-to-value table, which may be null
     * @return the primary value, or null when there are no entries
     */
    static String primary(String declared, Hashtable entries) {
        if (declared != null) {
            return declared;
        }
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String[] ranked = {"mobile", "home", "work", "other"};
        for (int iter = 0; iter < ranked.length; iter++) {
            Object value = entries.get(ranked[iter]);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        // An unranked label. Lowest key wins so the answer does not depend on
        // the table's iteration order.
        String lowest = null;
        Enumeration keys = entries.keys();
        while (keys.hasMoreElements()) {
            String key = String.valueOf(keys.nextElement());
            if (lowest == null || key.compareTo(lowest) < 0) {
                lowest = key;
            }
        }
        return lowest == null ? null : String.valueOf(entries.get(lowest));
    }

    /**
     * An independent copy of a simulated contact's photo.
     *
     * <p>Sharing the instance looked harmless and is not:
     * {@link Image#getGraphics()} carries no mutability guard, and this port
     * answers it with the backing {@code BufferedImage}'s own graphics. So
     * drawing on a picked contact's photo would draw on the simulated address
     * book, changing what every later pick and every broad read returned. A
     * picked contact is documented as a snapshot, and Android and iOS both
     * build a fresh image per pick.</p>
     *
     * @param photo the address book's image, which may be null
     * @return an image nothing else holds, or null
     */
    private static Image copyPhoto(Image photo) {
        if (photo == null) {
            return null;
        }
        int width = photo.getWidth();
        int height = photo.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        int[] rgb = photo.getRGB();
        int[] copy = new int[rgb.length];
        System.arraycopy(rgb, 0, copy, 0, rgb.length);
        return Image.createImage(copy, width, height);
    }

    private static Hashtable copy(Hashtable source) {
        if (source == null) {
            return null;
        }
        Hashtable out = new Hashtable();
        Enumeration keys = source.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            out.put(key, source.get(key));
        }
        return out;
    }

    private static Hashtable copyAddresses(Hashtable source) {
        if (source == null) {
            return null;
        }
        Hashtable out = new Hashtable();
        Enumeration keys = source.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Address from = (Address) source.get(key);
            Address to = new Address();
            to.setStreetAddress(from.getStreetAddress());
            to.setLocality(from.getLocality());
            to.setRegion(from.getRegion());
            to.setPostalCode(from.getPostalCode());
            to.setCountry(from.getCountry());
            out.put(key, to);
        }
        return out;
    }

    private static void show(final List<Contact> offered, final int fields,
            boolean multiSelect, final int selectionLimit,
            final ActionListener<ActionEvent> response) {
        // The cap is in the title as well as enforced below, so a tick that
        // refuses to stay on reads as the rule it is rather than as a bug in
        // the simulator.
        final Dialog dialog = new Dialog(multiSelect && selectionLimit < offered.size()
                ? "Contacts (pick up to " + selectionLimit + ")" : "Contacts");
        dialog.setLayout(new BorderLayout());
        dialog.setDisposeWhenPointerOutOfBounds(false);
        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);

        // One cell so the listeners below, which have to be effectively final
        // to be captured, can still report what the user chose.
        final Contact[][] answer = new Contact[1][];
        answer[0] = new Contact[0];

        Container buttons = new Container(BoxLayout.x());
        Button cancel = new Button("Cancel");
        cancel.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                answer[0] = new Contact[0];
                dialog.dispose();
            }
        });
        buttons.add(cancel);

        if (offered.isEmpty()) {
            list.add(new Label("No contact carries the requested fields"));
        } else if (multiSelect) {
            final List<CheckBox> boxes = new ArrayList<CheckBox>();
            for (int iter = 0; iter < offered.size(); iter++) {
                final CheckBox box = new CheckBox(describe(offered.get(iter)));
                // The cap is enforced as the user ticks rather than when they
                // press Done. Dropping the surplus at the end would report a
                // different selection from the one on screen, and the
                // platform pickers do not do that: Android stops accepting
                // ticks at its limit and the iOS picker is presented in
                // single-select mode when the limit is one.
                box.addActionListener(new ActionListener<ActionEvent>() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        if (box.isSelected() && selected(boxes) > selectionLimit) {
                            box.setSelected(false);
                        }
                    }
                });
                boxes.add(box);
                list.add(box);
            }
            Button done = new Button("Done");
            done.addActionListener(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent ev) {
                    List<Contact> picked = new ArrayList<Contact>();
                    for (int iter = 0; iter < boxes.size(); iter++) {
                        // The bound is unreachable -- the tick handler above
                        // never lets the count past it -- and is kept so this
                        // loop cannot report more than the caller asked for
                        // however the boxes got into their state.
                        if (boxes.get(iter).isSelected()
                                && picked.size() < selectionLimit) {
                            picked.add(project(offered.get(iter), fields));
                        }
                    }
                    answer[0] = picked.toArray(new Contact[picked.size()]);
                    dialog.dispose();
                }
            });
            buttons.add(done);
        } else {
            for (int iter = 0; iter < offered.size(); iter++) {
                final Contact contact = offered.get(iter);
                Button entry = new Button(describe(contact));
                entry.addActionListener(new ActionListener<ActionEvent>() {
                    @Override
                    public void actionPerformed(ActionEvent ev) {
                        answer[0] = new Contact[]{project(contact, fields)};
                        dialog.dispose();
                    }
                });
                list.add(entry);
            }
        }

        dialog.add(BorderLayout.CENTER, list);
        dialog.add(BorderLayout.SOUTH, buttons);
        dialog.showStretched(BorderLayout.CENTER, true);
        response.actionPerformed(new ActionEvent(answer[0]));
    }

    /**
     * @param boxes the multi-select rows
     * @return how many are currently ticked
     */
    private static int selected(List<CheckBox> boxes) {
        int count = 0;
        for (int iter = 0; iter < boxes.size(); iter++) {
            if (boxes.get(iter).isSelected()) {
                count++;
            }
        }
        return count;
    }

    private static String describe(Contact contact) {
        StringBuilder out = new StringBuilder(name(contact));
        // Through the same promotion the projection uses, so the row the user
        // taps shows the number they are about to receive.
        String phone = primary(contact.getPrimaryPhoneNumber(),
                contact.getPhoneNumbers());
        String email = primary(contact.getPrimaryEmail(), contact.getEmails());
        if (phone != null) {
            out.append(" - ").append(phone);
        } else if (email != null) {
            out.append(" - ").append(email);
        }
        return out.toString();
    }
}
