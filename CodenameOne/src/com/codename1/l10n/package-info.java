/// The Localization API allows developers to adapt their applications to different geographic locales and
///     conventions.
///
/// Portable Localization
///
///     Most of the classes within this package are clones of Java SE classes such as `com.codename1.l10n.SimpleDateFormat`
///     vs. `java.text.SimpleDateFormat`. The main motivation of placing these classes here and using the
///     cloned version is portability.
///
///     If we would use `java.text.SimpleDateFormat` its behavior would be slightly different on Android
///     or in the simulator vs. its behavior on iOS. That is because the implementation would be radically different.
///     When you use `com.codename1.l10n.SimpleDateFormat` the implementation might be missing
///     some pieces but it would be more consistent with the implementation you would get on the device which
///     is always preferable.
///
/// L10NManager
///
///     The localization manager allows adapting values for display in different locales thru parsing and formatting
///     capabilities (similar to JavaSE's DateFormat/NumberFormat). It also includes language/locale/currency
///     related API's similar to Locale/currency API's from JavaSE.
///
///     The sample code below just lists the various capabilities of the API:
///
/// ```java
/// Form hi = new Form("L10N", new TableLayout(16, 2));
/// L10NManager l10n = L10NManager.getInstance();
/// hi.add("format(double)").add(l10n.format(11.11)).
///     add("format(int)").add(l10n.format(33)).
///     add("formatCurrency").add(l10n.formatCurrency(53.267)).
///     add("formatDateLongStyle").add(l10n.formatDateLongStyle(new Date())).
///     add("formatDateShortStyle").add(l10n.formatDateShortStyle(new Date())).
///     add("formatDateTime").add(l10n.formatDateTime(new Date())).
///     add("formatDateTimeMedium").add(l10n.formatDateTimeMedium(new Date())).
///     add("formatDateTimeShort").add(l10n.formatDateTimeShort(new Date())).
///     add("getCurrencySymbol").add(l10n.getCurrencySymbol()).
///     add("getLanguage").add(l10n.getLanguage()).
///     add("getLocale").add(l10n.getLocale()).
///     add("isRTLLocale").add("" + l10n.isRTLLocale()).
///     add("parseCurrency").add(l10n.formatCurrency(l10n.parseCurrency("33.77$"))).
///     add("parseDouble").add(l10n.format(l10n.parseDouble("34.35"))).
///     add("parseInt").add(l10n.format(l10n.parseInt("56"))).
///     add("parseLong").add("" + l10n.parseLong("4444444"));
/// hi.show();
/// ```
package com.codename1.l10n;
