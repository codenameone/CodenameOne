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
/// ```java
/// phone.setCountry(PhoneNumberField.findCountry("IL"));
/// // user types 501234567, or 050-123-4567, or (050) 123 4567
/// phone.getE164(); // "+972501234567"
/// ```
///
/// A national trunk prefix is not something this field can strip for you: "0"
/// is a trunk prefix in Israel and part of the number in Italy, and telling
/// them apart is a per-country rule this field deliberately does not carry.
/// What it does carry is the shape of E.164 -- at most fifteen digits, and the
/// calling code separated from the rest -- so `#isValid()` is a sanity check
/// and the service that sends the message stays the authority.
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

    /// ISO code, calling code and English name for every region libphonenumber
    /// assigns a calling code to, ordered by name. Parsed once, on first use.
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
            return;
        }
        if (countries.length == 0) {
            throw new IllegalArgumentException("At least one country is required");
        }
        this.countries = new Country[countries.length];
        System.arraycopy(countries, 0, this.countries, 0, countries.length);
        boolean stillListed = false;
        for (int i = 0; i < countries.length; i++) {
            if (countries[i].equals(country)) {
                stillListed = true;
                break;
            }
        }
        if (!stillListed) {
            setCountry(countries[0]);
        }
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
        return digitsOf(number.getText());
    }

    /// The number in E.164 form -- "+", the calling code, then the national
    /// number -- or null when nothing has been typed.
    ///
    /// #### Returns
    ///
    /// the E.164 number, or null when the national part is empty
    public String getE164() {
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
            number.setText(digits);
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
        String national = getNationalNumber();
        return national.length() >= 4 && national.length() + country.getDialCode().length() <= 15;
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
        String needle = filter == null ? "" : filter.toLowerCase();
        for (int i = 0; i < offered.length; i++) {
            final Country c = offered[i];
            String label = displayName(c);
            if (needle.length() > 0
                    && label.toLowerCase().indexOf(needle) < 0
                    && c.getDialCode().indexOf(needle) < 0
                    && c.getIsoCode().toLowerCase().indexOf(needle) < 0) {
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

    /// The name shown for a country: its English name unless the theme's
    /// resource bundle translates "Country." plus its ISO code.
    private String displayName(Country c) {
        return getUIManager().localize("Country." + c.getIsoCode(), c.getName());
    }

    private Country defaultCountry() {
        Country[] list = getCountries();
        String iso = L10NManager.getInstance().getLocale();
        if (iso != null) {
            // the locale is an ISO 3166 country code, but a device that reports
            // a full locale ("en_US") still names the country in its tail
            String upper = iso.toUpperCase();
            for (int i = 0; i < list.length; i++) {
                String code = list[i].getIsoCode();
                if (upper.equals(code) || upper.endsWith("_" + code) || upper.endsWith("-" + code)) {
                    return list[i];
                }
            }
        }
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

    /// Every known country, ordered by English name.
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
        String upper = isoCode.toUpperCase();
        Country[] all = allCountries();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getIsoCode().equals(upper)) {
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
