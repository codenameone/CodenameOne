/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.components.PhoneNumberField.Country;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.TextArea;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link PhoneNumberField}: the country table, the E.164 value it
 * builds, and the parse that splits a number back into a country and the rest.
 */
class PhoneNumberFieldTest extends UITestBase {

    // ---- the country table ------------------------------------------

    @FormTest
    void tableCarriesEveryCountryWithACallingCode() {
        Country[] all = PhoneNumberField.getAllCountries();
        assertTrue(all.length > 200, "expected the full E.164 region list, got " + all.length);
    }

    @FormTest
    void wellKnownCallingCodesAreCorrect() {
        assertEquals("1", PhoneNumberField.findCountry("US").getDialCode());
        assertEquals("1", PhoneNumberField.findCountry("CA").getDialCode());
        assertEquals("44", PhoneNumberField.findCountry("GB").getDialCode());
        assertEquals("972", PhoneNumberField.findCountry("IL").getDialCode());
        assertEquals("91", PhoneNumberField.findCountry("IN").getDialCode());
        // the Caribbean NANP members share +1; 242 is the Bahamas' AREA code,
        // which is part of the national number rather than the calling code
        assertEquals("1", PhoneNumberField.findCountry("BS").getDialCode());
    }

    @FormTest
    void countryLookupIsCaseInsensitiveAndMissesCleanly() {
        assertEquals("IL", PhoneNumberField.findCountry("il").getIsoCode());
        assertNull(PhoneNumberField.findCountry("ZZ"));
        assertNull(PhoneNumberField.findCountry(null));
    }

    @FormTest
    void everyEntryIsWellFormedAndUnique() {
        Set<String> seen = new HashSet<String>();
        for (Country c : PhoneNumberField.getAllCountries()) {
            assertEquals(2, c.getIsoCode().length(), "bad ISO code: " + c.getIsoCode());
            assertTrue(seen.add(c.getIsoCode()), "duplicate ISO code: " + c.getIsoCode());
            assertTrue(c.getDialCode().length() >= 1 && c.getDialCode().length() <= 4,
                    "bad calling code for " + c.getIsoCode() + ": " + c.getDialCode());
            for (int i = 0; i < c.getDialCode().length(); i++) {
                char ch = c.getDialCode().charAt(i);
                assertTrue(ch >= '0' && ch <= '9', "non-digit calling code: " + c.getDialCode());
            }
            assertTrue(c.getName().length() > 0, "unnamed country: " + c.getIsoCode());
        }
    }

    @FormTest
    void theTableIsNotCopiedOutForCallersToMutate() {
        Country[] first = PhoneNumberField.getAllCountries();
        Country original = first[0];
        first[0] = null;
        assertSame(original, PhoneNumberField.getAllCountries()[0]);
    }

    // ---- the value ----------------------------------------------------

    @FormTest
    void emptyFieldHasNoNumber() {
        PhoneNumberField f = new PhoneNumberField();
        assertNull(f.getE164());
        assertFalse(f.isValid());
        assertNotNull(f.getCountry(), "a country is always selected");
    }

    @FormTest
    void numberIsTheCallingCodeFollowedByWhatWasTyped() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("IL"));
        f.getNumberField().setText("501234567");
        assertEquals("+972501234567", f.getE164());
        assertTrue(f.isValid());
    }

    @FormTest
    void separatorsTheUserTypesAreNotPartOfTheNumber() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("US"));
        f.getNumberField().setText("(555) 010-0123");
        assertEquals("+15550100123", f.getE164());
        assertEquals("5550100123", f.getNationalNumber());
    }

    @FormTest
    void changingCountryKeepsTheNumberThatWasTyped() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("US"));
        f.getNumberField().setText("5550100123");
        f.setCountry(PhoneNumberField.findCountry("GB"));
        assertEquals("+445550100123", f.getE164());
    }

    @FormTest
    void numberFieldAsksForThePhoneKeypad() {
        assertEquals(TextArea.PHONENUMBER, new PhoneNumberField().getNumberField().getConstraint());
    }

    // ---- parsing an existing number ------------------------------------

    @FormTest
    void settingAnE164NumberSelectsItsCountry() {
        PhoneNumberField f = new PhoneNumberField();
        f.setE164("+972501234567");
        assertEquals("IL", f.getCountry().getIsoCode());
        assertEquals("501234567", f.getNationalNumber());
        assertEquals("+972501234567", f.getE164());
    }

    @FormTest
    void assignedCallingCodesArePrefixFree() {
        // the parse leans on this: no assigned code is a prefix of another, so a
        // number can match at most one of them
        Country[] all = PhoneNumberField.getAllCountries();
        for (Country a : all) {
            for (Country b : all) {
                if (!a.getDialCode().equals(b.getDialCode())) {
                    assertFalse(b.getDialCode().startsWith(a.getDialCode()),
                            b.getIsoCode() + " (+" + b.getDialCode() + ") starts with "
                                    + a.getIsoCode() + " (+" + a.getDialCode() + ")");
                }
            }
        }
    }

    @FormTest
    void theLongestCallingCodeWinsInAnApplicationsOwnList() {
        // an application's list is not bound by the prefix-free rule the assigned
        // codes follow, so the longer code still has to win
        PhoneNumberField f = new PhoneNumberField();
        f.setCountries(new Country[]{
                new Country("XA", "1", "Shorter"),
                new Country("XB", "1242", "Longer")});
        f.setE164("+12425550123");
        assertEquals("XB", f.getCountry().getIsoCode());
        assertEquals("5550123", f.getNationalNumber());
    }

    @FormTest
    void aSharedCallingCodeKeepsTheCountryAlreadySelected() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("CA"));
        f.setE164("+15550100123");
        assertEquals("CA", f.getCountry().getIsoCode(),
                "+1 does not say which of its countries the number is from");
    }

    @FormTest
    void aNumberWithNoKnownCallingCodeStillLandsInTheField() {
        PhoneNumberField f = new PhoneNumberField();
        f.setE164("+9995550123");
        assertEquals("9995550123", f.getNationalNumber());
    }

    @FormTest
    void settingNullClearsTheNumber() {
        PhoneNumberField f = new PhoneNumberField();
        f.setE164("+972501234567");
        f.setE164(null);
        assertNull(f.getE164());
    }

    // ---- validity ------------------------------------------------------

    @FormTest
    void aNumberTooShortOrTooLongIsNotValid() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("IL"));
        f.getNumberField().setText("123");
        assertFalse(f.isValid());
        f.getNumberField().setText("1234");
        assertTrue(f.isValid());
        // E.164 allows fifteen digits including the calling code
        f.getNumberField().setText("123456789012");
        assertTrue(f.isValid());
        f.getNumberField().setText("1234567890123");
        assertFalse(f.isValid());
    }

    // ---- narrowing the list --------------------------------------------

    @FormTest
    void anApplicationCanOfferItsOwnCountries() {
        PhoneNumberField f = new PhoneNumberField();
        Country il = PhoneNumberField.findCountry("IL");
        f.setCountries(new Country[]{il});
        assertEquals(1, f.getCountries().length);
        assertEquals("IL", f.getCountry().getIsoCode(),
                "a selection outside the offered list is replaced");
    }

    @FormTest
    void narrowingKeepsASelectionThatIsStillOffered() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountry(PhoneNumberField.findCountry("GB"));
        f.setCountries(new Country[]{
                PhoneNumberField.findCountry("IL"),
                PhoneNumberField.findCountry("GB")});
        assertEquals("GB", f.getCountry().getIsoCode());
    }

    @FormTest
    void anEmptyListIsRefused() {
        PhoneNumberField f = new PhoneNumberField();
        assertThrows(IllegalArgumentException.class, () -> f.setCountries(new Country[0]));
    }

    @FormTest
    void nullRestoresTheFullList() {
        PhoneNumberField f = new PhoneNumberField();
        f.setCountries(new Country[]{PhoneNumberField.findCountry("IL")});
        f.setCountries(null);
        assertTrue(f.getCountries().length > 200);
    }

    @FormTest
    void countriesAreEqualByIsoCode() {
        assertEquals(PhoneNumberField.findCountry("IL"), new Country("IL", "972", "Anything"));
        assertEquals(PhoneNumberField.findCountry("IL").hashCode(),
                new Country("IL", "972", "Anything").hashCode());
        assertNotEquals(PhoneNumberField.findCountry("IL"), PhoneNumberField.findCountry("US"));
    }
}
