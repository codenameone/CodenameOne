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
package java.util;

/// A currency, identified by its ISO 4217 code: the subset of the JDK's
/// `java.util.Currency` a device needs to format and exchange amounts.
///
/// ```java
/// Currency euro = Currency.getInstance("EUR");
/// euro.getDefaultFractionDigits();               // 2
/// Currency.getInstance(Locale.JAPAN).getCurrencyCode(); // "JPY"
/// ```
///
/// There is one instance per code, so two currencies compare with `==` as in
/// the JDK. The ISO 4217 codes, numeric codes and fraction digits, and the
/// currency each country uses, are compiled into this class; nothing is read
/// from the platform, so the answers are the same on every device and on the
/// server.
///
/// #### Differences from the JDK
///
/// - [#getSymbol(Locale)] knows two symbols per currency: the home one (`$`
///   for US dollars in the US) when the locale's country uses the currency,
///   and US English's (`US$`) otherwise. The JDK localizes further per
///   language.
/// - [#getDisplayName(Locale)] answers the English name for every locale. For
///   a name in the user's language use the platform's localization
///   (`com.codename1.l10n.L10NManager`).
/// - The table is the one Java 17 ships, fixed at build time; there is no
///   `java.util.currency.data` override, no `CurrencyNameProvider` and no
///   serialization.
public final class Currency {

    // One currency per line: the three letter code, the three digit numeric
    // code, the fraction digits ('-' for none, as for gold), the symbol used
    // outside the home country (US English's, and `US$` for the dollar), '|',
    // the home symbol when it differs, '|', the English name. Generated from
    // the JDK 17 tables.
    private static final String TABLE = ""
            + "ADP0200ADP||Andorran Peseta\n"
            + "AED7842AED||United Arab Emirates Dirham\n"
            + "AFA0042AFA||Afghan Afghani (1927\u20132002)\n"
            + "AFN9712AFN||Afghan Afghani\n"
            + "ALL0082ALL|Lek\u00eb|Albanian Lek\n"
            + "AMD0512AMD|\u058f|Armenian Dram\n"
            + "ANG5322ANG|NAf.|Netherlands Antillean Guilder\n"
            + "AOA9732AOA|Kz|Angolan Kwanza\n"
            + "ARS0322ARS|$|Argentine Peso\n"
            + "ATS0402ATS||Austrian Schilling\n"
            + "AUD0362A$|$|Australian Dollar\n"
            + "AWG5332AWG|Afl.|Aruban Florin\n"
            + "AYM9452AYM||AYM\n"
            + "AZM0312AZM||Azerbaijani Manat (1993\u20132006)\n"
            + "AZN9442AZN|\u20bc|Azerbaijani Manat\n"
            + "BAM9772BAM|KM|Bosnia-Herzegovina Convertible Mark\n"
            + "BBD0522BBD|$|Barbadian Dollar\n"
            + "BDT0502BDT|\u09f3|Bangladeshi Taka\n"
            + "BEF0560BEF||Belgian Franc\n"
            + "BGL1002BGL||Bulgarian Hard Lev\n"
            + "BGN9752BGN|\u043b\u0432.|Bulgarian Lev\n"
            + "BHD0483BHD|\u062f.\u0628.\u200f|Bahraini Dinar\n"
            + "BIF1080BIF|FBu|Burundian Franc\n"
            + "BMD0602BMD|$|Bermudan Dollar\n"
            + "BND0962BND|$|Brunei Dollar\n"
            + "BOB0682BOB|Bs|Bolivian Boliviano\n"
            + "BOV9842BOV||Bolivian Mvdol\n"
            + "BRL9862R$||Brazilian Real\n"
            + "BSD0442BSD|$|Bahamian Dollar\n"
            + "BTN0642BTN|Nu.|Bhutanese Ngultrum\n"
            + "BWP0722BWP|P|Botswanan Pula\n"
            + "BYB1120BYB||Belarusian Ruble (1994\u20131999)\n"
            + "BYN9332BYN|Br|Belarusian Ruble\n"
            + "BYR9740BYR||Belarusian Ruble (2000\u20132016)\n"
            + "BZD0842BZD|$|Belize Dollar\n"
            + "CAD1242CA$|$|Canadian Dollar\n"
            + "CDF9762CDF|FC|Congolese Franc\n"
            + "CHE9472CHE||WIR Euro\n"
            + "CHF7562CHF||Swiss Franc\n"
            + "CHW9482CHW||WIR Franc\n"
            + "CLF9904CLF||Chilean Unit of Account (UF)\n"
            + "CLP1520CLP|$|Chilean Peso\n"
            + "CNY1562CN\u00a5|\u00a5|Chinese Yuan\n"
            + "COP1702COP|$|Colombian Peso\n"
            + "COU9702COU||Colombian Real Value Unit\n"
            + "CRC1882CRC|\u20a1|Costa Rican Col\u00f3n\n"
            + "CSD8912CSD||Serbian Dinar (2002\u20132006)\n"
            + "CUC9312CUC||Cuban Convertible Peso\n"
            + "CUP1922CUP|$|Cuban Peso\n"
            + "CVE1322CVE|\u200b|Cape Verdean Escudo\n"
            + "CYP1962CYP||Cypriot Pound\n"
            + "CZK2032CZK|K\u010d|Czech Koruna\n"
            + "DEM2762DEM||German Mark\n"
            + "DJF2620DJF|Fdj|Djiboutian Franc\n"
            + "DKK2082DKK|kr.|Danish Krone\n"
            + "DOP2142DOP|RD$|Dominican Peso\n"
            + "DZD0122DZD|\u062f.\u062c.\u200f|Algerian Dinar\n"
            + "EEK2332EEK||Estonian Kroon\n"
            + "EGP8182EGP|\u062c.\u0645.\u200f|Egyptian Pound\n"
            + "ERN2322ERN|Nfk|Eritrean Nakfa\n"
            + "ESP7240ESP||Spanish Peseta\n"
            + "ETB2302ETB|Br|Ethiopian Birr\n"
            + "EUR9782\u20ac||Euro\n"
            + "FIM2462FIM||Finnish Markka\n"
            + "FJD2422FJD|$|Fijian Dollar\n"
            + "FKP2382FKP|\u00a3|Falkland Islands Pound\n"
            + "FRF2502FRF||French Franc\n"
            + "GBP8262\u00a3||British Pound\n"
            + "GEL9812GEL|\u20be|Georgian Lari\n"
            + "GHC2882GHC||Ghanaian Cedi (1979\u20132007)\n"
            + "GHS9362GHS|GH\u20b5|Ghanaian Cedi\n"
            + "GIP2922GIP|\u00a3|Gibraltar Pound\n"
            + "GMD2702GMD|D|Gambian Dalasi\n"
            + "GNF3240GNF||Guinean Franc\n"
            + "GRD3000GRD||Greek Drachma\n"
            + "GTQ3202GTQ|Q|Guatemalan Quetzal\n"
            + "GWP6242GWP||Guinea-Bissau Peso\n"
            + "GYD3282GYD|$|Guyanaese Dollar\n"
            + "HKD3442HK$||Hong Kong Dollar\n"
            + "HNL3402HNL|L|Honduran Lempira\n"
            + "HRK1912HRK||Croatian Kuna\n"
            + "HTG3322HTG|G|Haitian Gourde\n"
            + "HUF3482HUF|Ft|Hungarian Forint\n"
            + "IDR3602IDR|Rp|Indonesian Rupiah\n"
            + "IEP3722IEP||Irish Pound\n"
            + "ILS3762\u20aa||Israeli New Shekel\n"
            + "INR3562\u20b9||Indian Rupee\n"
            + "IQD3683IQD|\u062f.\u0639.\u200f|Iraqi Dinar\n"
            + "IRR3642IRR||Iranian Rial\n"
            + "ISK3520ISK||Icelandic Kr\u00f3na\n"
            + "ITL3800ITL||Italian Lira\n"
            + "JMD3882JMD|$|Jamaican Dollar\n"
            + "JOD4003JOD|\u062f.\u0623.\u200f|Jordanian Dinar\n"
            + "JPY3920\u00a5|\uffe5|Japanese Yen\n"
            + "KES4042KES|Ksh|Kenyan Shilling\n"
            + "KGS4172KGS|\u0441\u043e\u043c|Kyrgystani Som\n"
            + "KHR1162KHR|\u17db|Cambodian Riel\n"
            + "KMF1740KMF|CF|Comorian Franc\n"
            + "KPW4082KPW||North Korean Won\n"
            + "KRW4100\u20a9||South Korean Won\n"
            + "KWD4143KWD|\u062f.\u0643.\u200f|Kuwaiti Dinar\n"
            + "KYD1362KYD|$|Cayman Islands Dollar\n"
            + "KZT3982KZT|\u20b8|Kazakhstani Tenge\n"
            + "LAK4182LAK|\u20ad|Laotian Kip\n"
            + "LBP4222LBP|\u0644.\u0644.\u200f|Lebanese Pound\n"
            + "LKR1442LKR|Rs.|Sri Lankan Rupee\n"
            + "LRD4302LRD|$|Liberian Dollar\n"
            + "LSL4262LSL||Lesotho Loti\n"
            + "LTL4402LTL||Lithuanian Litas\n"
            + "LUF4420LUF||Luxembourgian Franc\n"
            + "LVL4282LVL||Latvian Lats\n"
            + "LYD4343LYD|\u062f.\u0644.\u200f|Libyan Dinar\n"
            + "MAD5042MAD|\u062f.\u0645.\u200f|Moroccan Dirham\n"
            + "MDL4982MDL|L|Moldovan Leu\n"
            + "MGA9692MGA|Ar|Malagasy Ariary\n"
            + "MGF4500MGF||Malagasy Franc\n"
            + "MKD8072MKD|den|Macedonian Denar\n"
            + "MMK1042MMK|K|Myanmar Kyat\n"
            + "MNT4962MNT|\u20ae|Mongolian Tugrik\n"
            + "MOP4462MOP|MOP$|Macanese Pataca\n"
            + "MRO4782MRO||Mauritanian Ouguiya (1973\u20132017)\n"
            + "MRU9292MRU|\u0623.\u0645.|Mauritanian Ouguiya\n"
            + "MTL4702MTL||Maltese Lira\n"
            + "MUR4802MUR|Rs|Mauritian Rupee\n"
            + "MVR4622MVR||Maldivian Rufiyaa\n"
            + "MWK4542MWK|MK|Malawian Kwacha\n"
            + "MXN4842MX$|$|Mexican Peso\n"
            + "MXV9792MXV||Mexican Investment Unit\n"
            + "MYR4582MYR|RM|Malaysian Ringgit\n"
            + "MZM5082MZM||Mozambican Metical (1980\u20132006)\n"
            + "MZN9432MZN|MTn|Mozambican Metical\n"
            + "NAD5162NAD|$|Namibian Dollar\n"
            + "NGN5662NGN|\u20a6|Nigerian Naira\n"
            + "NIO5582NIO|C$|Nicaraguan C\u00f3rdoba\n"
            + "NLG5282NLG||Dutch Guilder\n"
            + "NOK5782NOK|kr|Norwegian Krone\n"
            + "NPR5242NPR|\u0928\u0947\u0930\u0942|Nepalese Rupee\n"
            + "NZD5542NZ$|$|New Zealand Dollar\n"
            + "OMR5123OMR|\u0631.\u0639.\u200f|Omani Rial\n"
            + "PAB5902PAB|B/.|Panamanian Balboa\n"
            + "PEN6042PEN|S/|Peruvian Sol\n"
            + "PGK5982PGK|K|Papua New Guinean Kina\n"
            + "PHP6082\u20b1||Philippine Piso\n"
            + "PKR5862PKR|Rs|Pakistani Rupee\n"
            + "PLN9852PLN|z\u0142|Polish Zloty\n"
            + "PTE6200PTE||Portuguese Escudo\n"
            + "PYG6000PYG|Gs.|Paraguayan Guarani\n"
            + "QAR6342QAR|\u0631.\u0642.\u200f|Qatari Rial\n"
            + "ROL6420ROL||Romanian Leu (1952\u20132006)\n"
            + "RON9462RON||Romanian Leu\n"
            + "RSD9412RSD||Serbian Dinar\n"
            + "RUB6432RUB|\u20bd|Russian Ruble\n"
            + "RUR8102RUR||Russian Ruble (1991\u20131998)\n"
            + "RWF6460RWF|RF|Rwandan Franc\n"
            + "SAR6822SAR|\u0631.\u0633.\u200f|Saudi Riyal\n"
            + "SBD0902SBD|$|Solomon Islands Dollar\n"
            + "SCR6902SCR|SR|Seychellois Rupee\n"
            + "SDD7362SDD||Sudanese Dinar (1992\u20132007)\n"
            + "SDG9382SDG||Sudanese Pound\n"
            + "SEK7522SEK|kr|Swedish Krona\n"
            + "SGD7022SGD|$|Singapore Dollar\n"
            + "SHP6542SHP|\u00a3|St. Helena Pound\n"
            + "SIT7052SIT||Slovenian Tolar\n"
            + "SKK7032SKK||Slovak Koruna\n"
            + "SLE9252SLE||Sierra Leonean Leone\n"
            + "SLL6942SLL||Sierra Leonean Leone\n"
            + "SOS7062SOS|S|Somali Shilling\n"
            + "SRD9682SRD|$|Surinamese Dollar\n"
            + "SRG7402SRG||Surinamese Guilder\n"
            + "SSP7282SSP|\u00a3|South Sudanese Pound\n"
            + "STD6782STD||S\u00e3o Tom\u00e9 & Pr\u00edncipe Dobra (1977\u20132017)\n"
            + "STN9302STN|Db|S\u00e3o Tom\u00e9 & Pr\u00edncipe Dobra\n"
            + "SVC2222SVC|C|Salvadoran Col\u00f3n\n"
            + "SYP7602SYP|\u0644.\u0633.\u200f|Syrian Pound\n"
            + "SZL7482SZL|E|Swazi Lilangeni\n"
            + "THB7642THB|\u0e3f|Thai Baht\n"
            + "TJS9722TJS|\u0441\u043e\u043c.|Tajikistani Somoni\n"
            + "TMM7952TMM||Turkmenistani Manat (1993\u20132009)\n"
            + "TMT9342TMT||Turkmenistani Manat\n"
            + "TND7883TND|DT|Tunisian Dinar\n"
            + "TOP7762TOP|T$|Tongan Pa\u02bbanga\n"
            + "TPE6260TPE||Timorese Escudo\n"
            + "TRL7920TRL||Turkish Lira (1922\u20132005)\n"
            + "TRY9492TRY|\u20ba|Turkish Lira\n"
            + "TTD7802TTD|$|Trinidad & Tobago Dollar\n"
            + "TWD9012NT$|$|New Taiwan Dollar\n"
            + "TZS8342TZS|TSh|Tanzanian Shilling\n"
            + "UAH9802UAH|\u20b4|Ukrainian Hryvnia\n"
            + "UGX8000UGX|USh|Ugandan Shilling\n"
            + "USD8402US$|$|US Dollar\n"
            + "USN9972USN||US Dollar (Next day)\n"
            + "USS9982USS||US Dollar (Same day)\n"
            + "UYI9400UYI||Uruguayan Peso (Indexed Units)\n"
            + "UYU8582UYU|$|Uruguayan Peso\n"
            + "UZS8602UZS|so\u02bbm|Uzbekistani Som\n"
            + "VEB8622VEB||Venezuelan Bol\u00edvar (1871\u20132008)\n"
            + "VED9262VED||Venezuelan Bol\u00edvar Soberano\n"
            + "VEF9372VEF||Venezuelan Bol\u00edvar (2008\u20132018)\n"
            + "VES9282VES|Bs.S|Venezuelan Bol\u00edvar\n"
            + "VND7040\u20ab||Vietnamese Dong\n"
            + "VUV5480VUV|VT|Vanuatu Vatu\n"
            + "WST8822WST|WS$|Samoan Tala\n"
            + "XAF9500FCFA||Central African CFA Franc\n"
            + "XAG961-XAG||Silver\n"
            + "XAU959-XAU||Gold\n"
            + "XBA955-XBA||European Composite Unit\n"
            + "XBB956-XBB||European Monetary Unit\n"
            + "XBC957-XBC||European Unit of Account (XBC)\n"
            + "XBD958-XBD||European Unit of Account (XBD)\n"
            + "XCD9512EC$|$|East Caribbean Dollar\n"
            + "XDR960-XDR||Special Drawing Rights\n"
            + "XFO000-XFO||French Gold Franc\n"
            + "XFU000-XFU||French UIC-Franc\n"
            + "XOF9520F\u202fCFA||West African CFA Franc\n"
            + "XPD964-XPD||Palladium\n"
            + "XPF9530CFPF|FCFP|CFP Franc\n"
            + "XPT962-XPT||Platinum\n"
            + "XSU994-XSU||Sucre\n"
            + "XTS963-XTS||Testing Currency Code\n"
            + "XUA965-XUA||ADB Unit of Account\n"
            + "XXX999-\u00a4||Unknown Currency\n"
            + "YER8862YER|\u0631.\u064a.\u200f|Yemeni Rial\n"
            + "YUM8912YUM||Yugoslavian New Dinar (1994\u20132002)\n"
            + "ZAR7102ZAR|R|South African Rand\n"
            + "ZMK8942ZMK||Zambian Kwacha (1968\u20132012)\n"
            + "ZMW9672ZMW|K|Zambian Kwacha\n"
            + "ZWD7162ZWD||Zimbabwean Dollar (1980\u20132008)\n"
            + "ZWL9322ZWL||Zimbabwean Dollar (2009)\n"
            + "ZWN9422ZWN||ZWN\n"
            + "ZWR9352ZWR||Zimbabwean Dollar (2008)\n";

    // The currency of each ISO 3166 country: two letters of country, three of
    // currency, back to back. A country absent here uses no currency.
    private static final String COUNTRIES = ""
            + "ADEURAEAEDAFAFNAGXCDAIXCDALALLAMAMDAOAOAARARSASUSD"
            + "ATEURAUAUDAWAWGAXEURAZAZNBABAMBBBBDBDBDTBEEURBFXOF"
            + "BGBGNBHBHDBIBIFBJXOFBLEURBMBMDBNBNDBOBOBBQUSDBRBRL"
            + "BSBSDBTBTNBVNOKBWBWPBYBYNBZBZDCACADCCAUDCDCDFCFXAF"
            + "CGXAFCHCHFCIXOFCKNZDCLCLPCMXAFCNCNYCOCOPCRCRCCUCUP"
            + "CVCVECWANGCXAUDCYEURCZCZKDEEURDJDJFDKDKKDMXCDDODOP"
            + "DZDZDECUSDEEEUREGEGPEHMADERERNESEURETETBFIEURFJFJD"
            + "FKFKPFMUSDFODKKFREURGAXAFGBGBPGDXCDGEGELGFEURGGGBP"
            + "GHGHSGIGIPGLDKKGMGMDGNGNFGPEURGQXAFGREURGSGBPGTGTQ"
            + "GUUSDGWXOFGYGYDHKHKDHMAUDHNHNLHREURHTHTGHUHUFIDIDR"
            + "IEEURILILSIMGBPININRIOUSDIQIQDIRIRRISISKITEURJEGBP"
            + "JMJMDJOJODJPJPYKEKESKGKGSKHKHRKIAUDKMKMFKNXCDKPKPW"
            + "KRKRWKWKWDKYKYDKZKZTLALAKLBLBPLCXCDLICHFLKLKRLRLRD"
            + "LSLSLLTEURLUEURLVEURLYLYDMAMADMCEURMDMDLMEEURMFEUR"
            + "MGMGAMHUSDMKMKDMLXOFMMMMKMNMNTMOMOPMPUSDMQEURMRMRU"
            + "MSXCDMTEURMUMURMVMVRMWMWKMXMXNMYMYRMZMZNNANADNCXPF"
            + "NEXOFNFAUDNGNGNNINIONLEURNONOKNPNPRNRAUDNUNZDNZNZD"
            + "OMOMRPAPABPEPENPFXPFPGPGKPHPHPPKPKRPLPLNPMEURPNNZD"
            + "PRUSDPSILSPTEURPWUSDPYPYGQAQARREEURRORONRSRSDRURUB"
            + "RWRWFSASARSBSBDSCSCRSDSDGSESEKSGSGDSHSHPSIEURSJNOK"
            + "SKEURSLSLESMEURSNXOFSOSOSSRSRDSSSSPSTSTNSVSVCSXANG"
            + "SYSYPSZSZLTCUSDTDXAFTFEURTGXOFTHTHBTJTJSTKNZDTLUSD"
            + "TMTMTTNTNDTOTOPTRTRYTTTTDTVAUDTWTWDTZTZSUAUAHUGUGX"
            + "UMUSDUSUSDUYUYUUZUZSVAEURVCXCDVEVESVGUSDVIUSDVNVND"
            + "VUVUVWFXPFWSWSTYEYERYTEURZAZARZMZMWZWZWL"
            ;

    private final String currencyCode;
    private final int numericCode;
    private final int fractionDigits;
    private final String symbol;
    private final String homeSymbol;
    private final String displayName;

    private Currency(String currencyCode, int numericCode, int fractionDigits, String symbol,
            String homeSymbol, String displayName) {
        this.currencyCode = currencyCode;
        this.numericCode = numericCode;
        this.fractionDigits = fractionDigits;
        this.symbol = symbol;
        this.homeSymbol = homeSymbol;
        this.displayName = displayName;
    }

    /// The currency for an ISO 4217 code such as `"EUR"`.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `currencyCode` is null
    /// - `IllegalArgumentException`: if it is not a known ISO 4217 code
    public static Currency getInstance(String currencyCode) {
        if (currencyCode == null) {
            throw new NullPointerException();
        }
        Currency c = (Currency) Table.BY_CODE.get(currencyCode);
        if (c == null) {
            throw new IllegalArgumentException("Unknown currency code: " + currencyCode);
        }
        return c;
    }

    /// The currency the country of `locale` uses, or null for a country
    /// that uses none (Antarctica).
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `locale` is null
    /// - `IllegalArgumentException`: if the locale has no country, or one
    ///   that is not two ASCII letters
    public static Currency getInstance(Locale locale) {
        String country = locale.getCountry();
        if (country == null || country.length() != 2 || !isUpperLetter(country.charAt(0))
                || !isUpperLetter(country.charAt(1))) {
            throw new IllegalArgumentException("No country in locale: " + locale);
        }
        String code = homeCurrencyCode(country);
        return code == null ? null : getInstance(code);
    }

    /// Every currency this class knows, in a new set the caller may change.
    public static Set<Currency> getAvailableCurrencies() {
        return new HashSet<Currency>(Table.BY_CODE.values());
    }

    /// The ISO 4217 code, such as `"EUR"`.
    public String getCurrencyCode() {
        return currencyCode;
    }

    /// The ISO 4217 numeric code, such as 978 for the euro.
    public int getNumericCode() {
        return numericCode;
    }

    /// The numeric code as three digits, zero padded: `"036"` for the
    /// Australian dollar.
    public String getNumericCodeAsString() {
        if (numericCode < 10) {
            return "00" + numericCode;
        }
        if (numericCode < 100) {
            return "0" + numericCode;
        }
        return String.valueOf(numericCode);
    }

    /// The number of digits after the decimal point an amount normally has:
    /// 2 for the euro, 0 for the yen, 3 for the Bahraini dinar, and -1 for a
    /// pseudo currency such as gold (`XAU`).
    public int getDefaultFractionDigits() {
        return fractionDigits;
    }

    /// The symbol in the default locale; see [#getSymbol(Locale)].
    public String getSymbol() {
        return getSymbol(Locale.getDefault());
    }

    /// The symbol in `locale`: the home symbol (`$` for the US dollar in the
    /// US) when the locale's country uses this currency, else the symbol US
    /// English writes for it (`CA$`, and `US$` for the dollar itself), which
    /// is the code for most currencies.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `locale` is null
    public String getSymbol(Locale locale) {
        String country = locale.getCountry();
        if (homeSymbol != null && country != null && country.length() == 2
                && currencyCode.equals(homeCurrencyCode(country))) {
            return homeSymbol;
        }
        return symbol;
    }

    /// The English name, such as `"Euro"`.
    public String getDisplayName() {
        return displayName;
    }

    /// The English name for every locale; see the class description.
    ///
    /// #### Throws
    ///
    /// - `NullPointerException`: if `locale` is null
    public String getDisplayName(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return displayName;
    }

    /// The ISO 4217 code.
    @Override
    public String toString() {
        return currencyCode;
    }

    private static boolean isUpperLetter(char c) {
        return c >= 'A' && c <= 'Z';
    }

    /// The code of the currency `country` uses, or null. The table is sorted
    /// by country, five characters an entry.
    private static String homeCurrencyCode(String country) {
        int low = 0;
        int high = COUNTRIES.length() / 5 - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int at = mid * 5;
            int cmp = compareCountry(country, at);
            if (cmp == 0) {
                return COUNTRIES.substring(at + 2, at + 5);
            }
            if (cmp < 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return null;
    }

    private static int compareCountry(String country, int at) {
        int d = country.charAt(0) - COUNTRIES.charAt(at);
        return d != 0 ? d : country.charAt(1) - COUNTRIES.charAt(at + 1);
    }

    /// Holds the instances, built when a currency is first asked for. Class
    /// initialization runs once and is safe across threads, so the map needs
    /// no lock.
    private static final class Table {
        static final Map<String, Currency> BY_CODE = build();

        private Table() {
        }

        private static Map<String, Currency> build() {
            Map<String, Currency> map = new HashMap<String, Currency>();
            int start = 0;
            int length = TABLE.length();
            while (start < length) {
                int end = TABLE.indexOf('\n', start);
                int bar1 = TABLE.indexOf('|', start);
                int bar2 = TABLE.indexOf('|', bar1 + 1);
                String code = TABLE.substring(start, start + 3);
                int numeric = Integer.parseInt(TABLE.substring(start + 3, start + 6));
                char digit = TABLE.charAt(start + 6);
                int fraction = digit == '-' ? -1 : digit - '0';
                String intl = TABLE.substring(start + 7, bar1);
                String home = bar2 == bar1 + 1 ? null : TABLE.substring(bar1 + 1, bar2);
                String name = TABLE.substring(bar2 + 1, end);
                map.put(code, new Currency(code, numeric, fraction, intl, home, name));
                start = end + 1;
            }
            return map;
        }
    }
}
