/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

/// A phone number entry field: a country selector holding the calling code and
/// a field for the rest of the number, producing one E.164 string.
///
/// #### Example
///
/// ```java
/// PhoneNumberField phone = new PhoneNumberField();
/// form.add(phone);
/// sendButton.addActionListener(e -> requestCode(phone.getE164()));
/// ```
///
/// The selector starts on the country the device is in and opens a searchable
/// list of every calling code. The number field carries
/// `TextArea#PHONENUMBER`, so it gets the phone keypad and the platform offers
/// the device's own number where it knows it.
///
/// #### The value
///
/// `#getE164()` returns the number in the one format a server can act on --
/// a leading "+", the calling code, then the national number, digits only:
///
/// Everything that is not a digit is dropped, so the separators a user reaches
/// for make no difference:
///
/// ```java
/// phone.setCountry(PhoneNumberField.findCountry("IL"));
/// // user types 50-123-4567, or 50 123 4567, or (50) 1234567
/// phone.getE164(); // "+972501234567"
/// ```
///
/// A number that already carries its own calling code is used as it stands, and
/// the selector is not applied to it. Pasting is one way that happens; platform
/// autofill is the other, since it offers the device's own number in exactly
/// that form:
///
/// ```java
/// // Israel selected, the user pastes +1 415 555 0100
/// phone.getE164(); // "+14155550100", not the selection with that appended
/// ```
///
/// A national trunk prefix is a digit, and it is kept:
///
/// ```java
/// // the same user typing the number the way they say it out loud
/// // user types 050-123-4567
/// phone.getE164(); // "+9720501234567" -- the leading 0 is still there
/// ```
///
/// That is not an oversight, and it is the one thing to handle before sending.
/// "0" is a trunk prefix in Israel and part of the number in Italy, and telling
/// them apart is a per-country rule this field does not carry, so stripping one
/// here would corrupt numbers in the countries where it belongs. Normalizing is
/// left to the service that sends the message, which has the rules and can
/// refuse what it cannot make sense of.
///
/// What this field does carry is the shape of E.164 -- at most fifteen digits,
/// and the calling code separated from the rest -- so `#isValid()` is a sanity
/// check rather than a verdict.
///
/// #### Country names
///
/// Names are English, and each is looked up first as "Country." plus the ISO
/// code in the theme's resource bundle, so an application that ships
/// translations gets them without replacing the list. An application with its
/// own list entirely passes it to `#setCountries(Country[])`.
///
/// #### Styling
///
/// The field uses the UIID "PhoneNumberField", the country selector
/// "PhoneNumberCountry" and the number field "PhoneNumberText".
public class PhoneNumberField extends Container {

    /// A country and its E.164 calling code.
    public static final class Country {

        private final String isoCode;
        private final String dialCode;
        private final String name;

        /// Builds a country entry.
        ///
        /// #### Parameters
        ///
        /// - `isoCode`: the two letter ISO 3166 code, e.g. "IL"
        ///
        /// - `dialCode`: the calling code without the "+", e.g. "972"
        ///
        /// - `name`: the display name
        public Country(String isoCode, String dialCode, String name) {
            this.isoCode = isoCode;
            this.dialCode = dialCode;
            this.name = name;
        }

        /// The two letter ISO 3166 code.
        public String getIsoCode() {
            return isoCode;
        }

        /// The calling code, digits only, without the "+".
        public String getDialCode() {
            return dialCode;
        }

        /// The English display name.
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + " +" + dialCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Country)) {
                return false;
            }
            return isoCode.equals(((Country) o).isoCode);
        }

        @Override
        public int hashCode() {
            return isoCode.hashCode();
        }
    }

    /// ISO code, calling code and English name for every region with a numbering
    /// plan of its own, ordered by name. Parsed once, on first use.
    ///
    /// Generated from libphonenumber's region metadata rather than written out by
    /// hand, and that is the point: two hundred and forty-five calling codes typed
    /// from memory would contain mistakes nobody would find until somebody in that
    /// country could not sign in.
    ///
    /// It follows that eight ISO 3166 codes are absent -- AN, AQ, BV, GS, HM, PN, TF
    /// and UM -- and their absence is correct rather than an oversight. One of them
    /// stopped being a country in 2010; most of the rest have no permanent
    /// population; and none of them has a numbering plan of its own, which is why
    /// the metadata carries none. Pitcairn's numbers, for instance, are reachable
    /// through New Zealand's +64 rather than through anything assigned to PN.
    ///
    /// Do not add one by hand. A calling code invented for a territory is exactly
    /// the error this table is generated to avoid, and adding the one that gets
    /// noticed while leaving the seven that do not is worse than either. An
    /// application that serves such a place passes its own list to
    /// `#setCountries(Country[])`, which is what that method is for.
    private static final String COUNTRY_TABLE =
            "AF|93|Afghanistan;AL|355|Albania;DZ|213|Algeria;AS|1|American Samoa;AD|376|Andorra;"
            + "AO|244|Angola;AI|1|Anguilla;AG|1|Antigua & Barbuda;AR|54|Argentina;AM|374|Armenia;AW|297|Aruba;"
            + "AC|247|Ascension Island;AU|61|Australia;AT|43|Austria;AZ|994|Azerbaijan;BS|1|Bahamas;"
            + "BH|973|Bahrain;BD|880|Bangladesh;BB|1|Barbados;BY|375|Belarus;BE|32|Belgium;BZ|501|Belize;"
            + "BJ|229|Benin;BM|1|Bermuda;BT|975|Bhutan;BO|591|Bolivia;BA|387|Bosnia & Herzegovina;"
            + "BW|267|Botswana;BR|55|Brazil;IO|246|British Indian Ocean Territory;VG|1|British Virgin Islands;"
            + "BN|673|Brunei;BG|359|Bulgaria;BF|226|Burkina Faso;BI|257|Burundi;KH|855|Cambodia;"
            + "CM|237|Cameroon;CA|1|Canada;CV|238|Cape Verde;BQ|599|Caribbean Netherlands;KY|1|Cayman Islands;"
            + "CF|236|Central African Republic;TD|235|Chad;CL|56|Chile;CN|86|China;CX|61|Christmas Island;"
            + "CC|61|Cocos (Keeling) Islands;CO|57|Colombia;KM|269|Comoros;CG|242|Congo - Brazzaville;"
            + "CD|243|Congo - Kinshasa;CK|682|Cook Islands;CR|506|Costa Rica;HR|385|Croatia;CU|53|Cuba;"
            + "CW|599|Cura\u00e7ao;CY|357|Cyprus;CZ|420|Czechia;CI|225|C\u00f4te d'Ivoire;DK|45|Denmark;"
            + "DJ|253|Djibouti;DM|1|Dominica;DO|1|Dominican Republic;EC|593|Ecuador;EG|20|Egypt;"
            + "SV|503|El Salvador;GQ|240|Equatorial Guinea;ER|291|Eritrea;EE|372|Estonia;SZ|268|Eswatini;"
            + "ET|251|Ethiopia;FK|500|Falkland Islands;FO|298|Faroe Islands;FJ|679|Fiji;FI|358|Finland;"
            + "FR|33|France;GF|594|French Guiana;PF|689|French Polynesia;GA|241|Gabon;GM|220|Gambia;"
            + "GE|995|Georgia;DE|49|Germany;GH|233|Ghana;GI|350|Gibraltar;GR|30|Greece;GL|299|Greenland;"
            + "GD|1|Grenada;GP|590|Guadeloupe;GU|1|Guam;GT|502|Guatemala;GG|44|Guernsey;GN|224|Guinea;"
            + "GW|245|Guinea-Bissau;GY|592|Guyana;HT|509|Haiti;HN|504|Honduras;HK|852|Hong Kong SAR China;"
            + "HU|36|Hungary;IS|354|Iceland;IN|91|India;ID|62|Indonesia;IR|98|Iran;IQ|964|Iraq;IE|353|Ireland;"
            + "IM|44|Isle of Man;IL|972|Israel;IT|39|Italy;JM|1|Jamaica;JP|81|Japan;JE|44|Jersey;"
            + "JO|962|Jordan;KZ|7|Kazakhstan;KE|254|Kenya;KI|686|Kiribati;XK|383|Kosovo;KW|965|Kuwait;"
            + "KG|996|Kyrgyzstan;LA|856|Laos;LV|371|Latvia;LB|961|Lebanon;LS|266|Lesotho;LR|231|Liberia;"
            + "LY|218|Libya;LI|423|Liechtenstein;LT|370|Lithuania;LU|352|Luxembourg;MO|853|Macao SAR China;"
            + "MG|261|Madagascar;MW|265|Malawi;MY|60|Malaysia;MV|960|Maldives;ML|223|Mali;MT|356|Malta;"
            + "MH|692|Marshall Islands;MQ|596|Martinique;MR|222|Mauritania;MU|230|Mauritius;YT|262|Mayotte;"
            + "MX|52|Mexico;FM|691|Micronesia;MD|373|Moldova;MC|377|Monaco;MN|976|Mongolia;ME|382|Montenegro;"
            + "MS|1|Montserrat;MA|212|Morocco;MZ|258|Mozambique;MM|95|Myanmar (Burma);NA|264|Namibia;"
            + "NR|674|Nauru;NP|977|Nepal;NL|31|Netherlands;NC|687|New Caledonia;NZ|64|New Zealand;"
            + "NI|505|Nicaragua;NE|227|Niger;NG|234|Nigeria;NU|683|Niue;NF|672|Norfolk Island;"
            + "KP|850|North Korea;MK|389|North Macedonia;MP|1|Northern Mariana Islands;NO|47|Norway;"
            + "OM|968|Oman;PK|92|Pakistan;PW|680|Palau;PS|970|Palestinian Territories;PA|507|Panama;"
            + "PG|675|Papua New Guinea;PY|595|Paraguay;PE|51|Peru;PH|63|Philippines;PL|48|Poland;"
            + "PT|351|Portugal;PR|1|Puerto Rico;QA|974|Qatar;RO|40|Romania;RU|7|Russia;RW|250|Rwanda;"
            + "RE|262|R\u00e9union;WS|685|Samoa;SM|378|San Marino;SA|966|Saudi Arabia;SN|221|Senegal;"
            + "RS|381|Serbia;SC|248|Seychelles;SL|232|Sierra Leone;SG|65|Singapore;SX|1|Sint Maarten;"
            + "SK|421|Slovakia;SI|386|Slovenia;SB|677|Solomon Islands;SO|252|Somalia;ZA|27|South Africa;"
            + "KR|82|South Korea;SS|211|South Sudan;ES|34|Spain;LK|94|Sri Lanka;BL|590|St. Barth\u00e9lemy;"
            + "SH|290|St. Helena;KN|1|St. Kitts & Nevis;LC|1|St. Lucia;MF|590|St. Martin;"
            + "PM|508|St. Pierre & Miquelon;VC|1|St. Vincent & Grenadines;SD|249|Sudan;SR|597|Suriname;"
            + "SJ|47|Svalbard & Jan Mayen;SE|46|Sweden;CH|41|Switzerland;SY|963|Syria;"
            + "ST|239|S\u00e3o Tom\u00e9 & Pr\u00edncipe;TW|886|Taiwan;TJ|992|Tajikistan;TZ|255|Tanzania;"
            + "TH|66|Thailand;TL|670|Timor-Leste;TG|228|Togo;TK|690|Tokelau;TO|676|Tonga;"
            + "TT|1|Trinidad & Tobago;TA|290|Tristan da Cunha;TN|216|Tunisia;TR|90|Turkey;TM|993|Turkmenistan;"
            + "TC|1|Turks & Caicos Islands;TV|688|Tuvalu;VI|1|U.S. Virgin Islands;UG|256|Uganda;"
            + "UA|380|Ukraine;AE|971|United Arab Emirates;GB|44|United Kingdom;US|1|United States;"
            + "UY|598|Uruguay;UZ|998|Uzbekistan;VU|678|Vanuatu;VA|39|Vatican City;VE|58|Venezuela;"
            + "VN|84|Vietnam;WF|681|Wallis & Futuna;EH|212|Western Sahara;YE|967|Yemen;ZM|260|Zambia;"
            + "ZW|263|Zimbabwe;AX|358|\u00c5land Islands";

    private static Country[] allCountries;

    private final Button countryButton = new Button();
    private final TextField number = new TextField();
    private Country[] countries;
    private Country country;

    /// Builds a field defaulting to the country the device reports, falling
    /// back to the first entry when the device reports one that is not in the
    /// list.
    public PhoneNumberField() {
        super(new BorderLayout());
        setUIID("PhoneNumberField");
        countryButton.setUIID("PhoneNumberCountry");
        number.setUIID("PhoneNumberText");
        number.setConstraint(TextArea.PHONENUMBER);
        number.setHint(getUIManager().localize("PhoneNumberField.Hint", "Phone number"));
        countryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showCountryPicker();
            }
        });
        add(BorderLayout.WEST, countryButton);
        add(BorderLayout.CENTER, number);
        setCountry(defaultCountry());
    }

    /// The list this field offers, defaulting to every known country.
    ///
    /// #### Returns
    ///
    /// the countries offered by the selector
    public Country[] getCountries() {
        // a copy either way: the full list is a single shared table, and handing
        // it out is handing out every field's list at once
        Country[] source = countries == null ? allCountries() : countries;
        Country[] copy = new Country[source.length];
        System.arraycopy(source, 0, copy, 0, source.length);
        return copy;
    }

    /// Narrows or replaces the list this field offers. An application serving
    /// three countries has no reason to show two hundred.
    ///
    /// #### Parameters
    ///
    /// - `countries`: the countries to offer, or null to restore the full list
    public void setCountries(Country[] countries) {
        if (countries == null) {
            this.countries = null;
            // The selection has to come from the list on offer, and a country that was
            // only in a replaced list is not in this one. Not by taking the first entry
            // of the full table, which is Afghanistan and has nothing to do with anyone:
            // silently moving a user from +1 to +93 is worse than the inconsistency it
            // tidies. The same code is preferred where the full list has it, and the
            // device's own country is the fallback -- which is where the field started.
            Country listed = findCountry(country.getIsoCode());
            setCountry(listed != null ? listed : defaultCountry());
            return;
        }
        if (countries.length == 0) {
            throw new IllegalArgumentException("At least one country is required");
        }
        this.countries = new Country[countries.length];
        System.arraycopy(countries, 0, this.countries, 0, countries.length);
        // The entry from the new list, not merely the knowledge that one matches.
        // Countries are equal when their ISO codes are, so a list can carry a different
        // object for the same country -- the built-in United States beside an
        // application's own -- and keeping the old one would leave the field dialling a
        // code the selector no longer offers. Where nothing matches, the first entry of
        // the list the application supplied is a defensible default in a way the first
        // entry of the full table is not.
        Country listed = null;
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].equals(country)) {
                listed = countries[i];
                break;
            }
        }
        setCountry(listed != null ? listed : countries[0]);
    }

    /// The selected country, never null.
    public Country getCountry() {
        return country;
    }

    /// Selects a country, which changes the calling code the value is built
    /// from without touching the number that was typed.
    ///
    /// #### Parameters
    ///
    /// - `c`: the country; ignored when null
    public void setCountry(Country c) {
        if (c == null) {
            return;
        }
        country = c;
        countryButton.setText("+" + c.getDialCode());
    }

    /// The national part as typed, digits only.
    public String getNationalNumber() {
        String digits = digitsOf(number.getText());
        if (!isInternational()) {
            return digits;
        }
        // The value already says which country it is for, so the national part is what
        // follows that country's calling code rather than the whole thing. Resolved
        // against the full table rather than the offered list: the number means what it
        // means whichever countries this field happens to be offering.
        Country match = longestMatch(allCountries(), digits);
        return match == null ? digits : digits.substring(match.getDialCode().length());
    }

    /// True when what has been typed is an international number rather than a national
    /// one -- pasted, or handed over by the platform, which offers the device's own
    /// number in exactly that form.
    private boolean isInternational() {
        String raw = number.getText();
        if (raw == null) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '+') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return false;
    }

    /// The country in `list` whose calling code the digits start with, longest first,
    /// or null when none does.
    private static Country longestMatch(Country[] list, String digits) {
        Country match = null;
        int matchLength = 0;
        for (int i = 0; i < list.length; i++) {
            String dial = list[i].getDialCode();
            if (digits.length() > dial.length() && digits.startsWith(dial)
                    && dial.length() > matchLength) {
                match = list[i];
                matchLength = dial.length();
            }
        }
        return match;
    }

    /// The number in E.164 form -- "+", the calling code, then the national
    /// number -- or null when nothing has been typed.
    ///
    /// #### Returns
    ///
    /// the E.164 number, or null when the national part is empty
    public String getE164() {
        if (isInternational()) {
            // Used as typed. A value that already carries a calling code is a whole
            // number, and prepending the selector's code to it would produce one that is
            // neither the one that was pasted nor the one the selector shows -- pasting
            // +972501234567 with Israel selected once produced +972972501234567.
            String digits = digitsOf(number.getText());
            return digits.length() == 0 ? null : "+" + digits;
        }
        String national = getNationalNumber();
        if (national.length() == 0) {
            return null;
        }
        return "+" + country.getDialCode() + national;
    }

    /// Sets the field from an E.164 number, selecting the country whose calling
    /// code the number starts with and putting the rest in the number field.
    ///
    /// Several countries share a calling code (+1 covers the United States,
    /// Canada and much of the Caribbean, which the North American area code
    /// tells apart and this field does not), and the number alone does not say
    /// which. The currently selected country is kept when its code matches, and
    /// otherwise the first country listed for that code is selected.
    ///
    /// #### Parameters
    ///
    /// - `e164`: the number, with or without the leading "+"; null clears the
    ///   field
    public void setE164(String e164) {
        if (e164 == null) {
            number.setText("");
            return;
        }
        String digits = digitsOf(e164);
        Country[] list = getCountries();
        Country match = null;
        int matchLength = 0;
        // Longest calling code wins. Assigned country codes are prefix-free, so
        // at most one of them can match and the length never decides anything --
        // but a list an application supplies is under no such discipline, and a
        // shorter code would otherwise swallow a longer one's numbers.
        for (int i = 0; i < list.length; i++) {
            String dial = list[i].getDialCode();
            if (digits.length() > dial.length() && digits.startsWith(dial)) {
                if (dial.length() > matchLength || (dial.length() == matchLength && list[i].equals(country))) {
                    match = list[i];
                    matchLength = dial.length();
                }
            }
        }
        if (match == null) {
            // No country here can express it -- a narrowed list, or a calling code the
            // table does not carry. Kept in international form so it survives unchanged
            // rather than being read back as the selected country's code followed by all
            // of it, which is a different number and a plausible looking one.
            number.setText("+" + digits);
            return;
        }
        setCountry(match);
        number.setText(digits.substring(matchLength));
    }

    /// A sanity check on the shape of the number: a national part that is
    /// present and short enough to leave the whole number inside E.164's
    /// fifteen digit limit. It is not a check that the number exists, which
    /// only the service that sends the message can answer.
    ///
    /// #### Returns
    ///
    /// true when the number could be an E.164 number
    public boolean isValid() {
        String e164 = getE164();
        if (e164 == null) {
            return false;
        }
        // measured against what getE164 would actually send, which is not the selector's
        // code plus the field when the field holds a whole number of its own
        return getNationalNumber().length() >= 4 && e164.length() - 1 <= 15;
    }

    /// The field holding the national part, exposed for theming and for
    /// listening to what is typed.
    public TextField getNumberField() {
        return number;
    }

    /// The button that opens the country list, exposed for theming.
    public Button getCountryButton() {
        return countryButton;
    }

    /// Adds a listener notified as the number is typed.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addDataChangedListener(DataChangedListener l) {
        number.addDataChangedListener(l);
    }

    /// Removes a previously-registered listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeDataChangedListener(DataChangedListener l) {
        number.removeDataChangedListener(l);
    }

    private void showCountryPicker() {
        final Dialog dlg = new Dialog(getUIManager().localize("PhoneNumberField.CountryTitle", "Country"));
        dlg.setLayout(new BorderLayout());
        final Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        final Country[] offered = getCountries();
        final TextField search = new TextField("",
                getUIManager().localize("PhoneNumberField.Search", "Search"), 20, TextArea.ANY);
        search.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                fillCountryList(list, offered, search.getText(), dlg);
                list.animateLayout(100);
            }
        });
        fillCountryList(list, offered, "", dlg);
        dlg.add(BorderLayout.NORTH, search);
        dlg.add(BorderLayout.CENTER, list);
        dlg.show();
    }

    private void fillCountryList(Container list, Country[] offered, String filter, final Dialog dlg) {
        list.removeAll();
        String needle = foldCase(filter);
        for (int i = 0; i < offered.length; i++) {
            final Country c = offered[i];
            String label = displayName(c);
            if (!matchesSearch(label, c, needle)) {
                continue;
            }
            MultiButton entry = new MultiButton(label);
            entry.setTextLine2("+" + c.getDialCode());
            entry.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    setCountry(c);
                    dlg.dispose();
                }
            });
            list.add(entry);
        }
    }

    /// True when a country should be offered for what has been typed into the
    /// search field. The needle must already be folded.
    ///
    /// #### Parameters
    ///
    /// - `label`: the name being shown for the country
    ///
    /// - `c`: the country
    ///
    /// - `needle`: the folded search text, empty to match everything
    ///
    /// #### Returns
    ///
    /// true when the country matches
    static boolean matchesSearch(String label, Country c, String needle) {
        if (needle.length() == 0) {
            return true;
        }
        return foldCase(label).indexOf(needle) >= 0
                || c.getDialCode().indexOf(needle) >= 0
                || foldCase(c.getIsoCode()).indexOf(needle) >= 0;
    }

    /// Lower cases without asking the device what that means.
    ///
    /// `String.toLowerCase` folds with the default locale, and the two sides of a
    /// search do not survive that: on a Turkish device the capital I of "Israel"
    /// becomes a dotless i while the i the user typed stays dotted, so the country
    /// cannot be found by typing its first letter. Folding character by character
    /// uses the Unicode mapping instead, which is the same everywhere -- and unlike
    /// an ASCII-only fold it still matches an accented name by its accented letter.
    ///
    /// #### Parameters
    ///
    /// - `s`: the text to fold, or null
    ///
    /// #### Returns
    ///
    /// the folded text, empty for null
    static String foldCase(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            b.append(Character.toLowerCase(s.charAt(i)));
        }
        return b.toString();
    }

    /// The name shown for a country: its English name unless the theme's
    /// resource bundle translates "Country." plus its ISO code.
    private String displayName(Country c) {
        return getUIManager().localize("Country." + c.getIsoCode(), c.getName());
    }

    private Country defaultCountry() {
        Country[] list = getCountries();
        String iso = L10NManager.getInstance().getLocale();
        if (iso != null) {
            // The locale is an ISO 3166 country code, but a device that reports
            // a full locale ("en_US") still names the country in its tail.
            // Compared without folding either side, for the reason findCountry gives.
            String tail = iso;
            int separator = Math.max(iso.lastIndexOf('_'), iso.lastIndexOf('-'));
            if (separator >= 0) {
                tail = iso.substring(separator + 1);
            }
            for (int i = 0; i < list.length; i++) {
                if (list[i].getIsoCode().equalsIgnoreCase(tail)) {
                    return list[i];
                }
            }
        }
        // A device reporting a region the list does not carry -- one of the eight
        // without a numbering plan, or a country the application has narrowed away --
        // gets the first entry. Any choice here is arbitrary; this one is at least
        // stable, and the user's own first act on this screen is to pick a country.
        return list[0];
    }

    private static String digitsOf(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                b.append(c);
            }
        }
        return b.toString();
    }

    /// Every country with a numbering plan of its own, ordered by English name.
    ///
    /// A handful of ISO 3166 regions are deliberately absent -- see the note on the
    /// table itself -- because they have no calling code assigned to them. An
    /// application that needs one supplies its own list.
    public static Country[] getAllCountries() {
        Country[] all = allCountries();
        Country[] copy = new Country[all.length];
        System.arraycopy(all, 0, copy, 0, all.length);
        return copy;
    }

    /// Looks a country up by its two letter ISO 3166 code.
    ///
    /// #### Parameters
    ///
    /// - `isoCode`: the code, case insensitive
    ///
    /// #### Returns
    ///
    /// the country, or null when the code is not one this list carries
    public static Country findCountry(String isoCode) {
        if (isoCode == null) {
            return null;
        }
        Country[] all = allCountries();
        for (int i = 0; i < all.length; i++) {
            // equalsIgnoreCase rather than folding the argument: String.toUpperCase folds
            // with the device's locale, and a Turkish device turns "il" into a dotted
            // capital I that matches no ISO 3166 code -- so this documented case
            // insensitive lookup would find nothing at all there. Character-wise case
            // comparison carries no locale.
            if (all[i].getIsoCode().equalsIgnoreCase(isoCode)) {
                return all[i];
            }
        }
        return null;
    }

    private static synchronized Country[] allCountries() {
        if (allCountries == null) {
            List<Country> parsed = new ArrayList<Country>(256);
            int start = 0;
            while (start < COUNTRY_TABLE.length()) {
                int end = COUNTRY_TABLE.indexOf(';', start);
                if (end < 0) {
                    end = COUNTRY_TABLE.length();
                }
                String record = COUNTRY_TABLE.substring(start, end);
                int a = record.indexOf('|');
                int b = record.indexOf('|', a + 1);
                parsed.add(new Country(record.substring(0, a),
                        record.substring(a + 1, b), record.substring(b + 1)));
                start = end + 1;
            }
            allCountries = parsed.toArray(new Country[parsed.size()]);
        }
        return allCountries;
    }
}
