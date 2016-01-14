/*
 * Copyright (c) 2012, Eric Coolman, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.l10n;

import java.util.Hashtable;
import java.util.TimeZone;

/**
 * @author Eric Coolman
 * 
 */
public class DateFormatSymbols implements Cloneable {
	public static final int ZONE_ID = 0;
	public static final int ZONE_LONGNAME = 1;
	public static final int ZONE_SHORTNAME = 2;
	public static final int ZONE_LONGNAME_DST = 3;
	public static final int ZONE_SHORTNAME_DST = 4;
	private static final String L10N_ZONE_LONGNAME = "ZONE_LONGNAME_";
	private static final String L10N_ZONE_SHORTNAME = "ZONE_SHORTNAME_";
	private static final String L10N_ZONE_LONGNAME_DST = "ZONE_LONGNAME_DST_";
	private static final String L10N_ZONE_SHORTNAME_DST = "ZONE_SHORTNAME_DST_";
	private static final String L10N_WEEKDAY_LONGNAME = "WEEKDAY_LONGNAME_";
	private static final String L10N_WEEKDAY_SHORTNAME = "WEEKDAY_SHORTNAME_";
	private static final String L10N_MONTH_LONGNAME = "MONTH_LONGNAME_";
	private static final String L10N_MONTH_SHORTNAME = "MONTH_SHORTNAME_";
	private static final String L10N_AMPM = "AMPM_";
	private static final String L10N_ERA = "ERA_";
	private static final String MONTHS[] = {"January", "February", "March", "April", "May", "June", "July", "August",
			"September", "October", "November", "December"};
	private static final String WEEKDAYS[] = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
			"Saturday"};
	private static final String AMPMS[] = {"AM", "PM"};
	private static final String ERAS[] = {"AD", "BC"};

	private Hashtable<String, String> resourceBundle;
	private String ampms[];
	private String months[];
	private String zoneStrings[][];
	private String shortMonths[];
	private String weekdays[];
	private String shortWeekdays[];
	private String eras[];

	public String[] getAmPmStrings() {
		synchronized (this) {
			if (ampms == null) {
				if (resourceBundle == null) {
					return AMPMS;
				}
				String newAmpms[] = new String[2];
				newAmpms[0] = getLocalizedValue(L10N_AMPM + "AM", AMPMS[0]);
				newAmpms[1] = getLocalizedValue(L10N_AMPM + "PM", AMPMS[1]);
				ampms = newAmpms;
			}
		}
		return ampms;
	}

	public void setAmPmStrings(String[] newAmpms) {
		if (newAmpms.length != 2) {
			throw new IllegalArgumentException("Expecting array size of 2");
		}
		ampms = newAmpms;
	}

	public Hashtable<String, String> getResourceBundle() {
		return resourceBundle;
	}

	public void setResourceBundle(Hashtable<String, String> newResourceBundle) {
		this.resourceBundle = newResourceBundle;
		// force rebuild
		ampms = null;
		months = null;
		zoneStrings = null;
		shortMonths = null;
		weekdays = null;
		shortWeekdays = null;
		eras = null;
	}

	String getLocalizedValue(String key, String defaultValue) {
		Hashtable<String, String> resourceBundle = getResourceBundle();
		if (resourceBundle == null || resourceBundle.containsKey(key) == false) {
			return defaultValue;
		}
		String v = (resourceBundle.get(key));
		return (v.length() > 0) ? v : defaultValue;
	}

	public String[][] getZoneStrings() {
		synchronized (this) {
			if (zoneStrings == null) {
				String ids[] = TimeZone.getAvailableIDs();
				String newZoneStrings[][] = new String[ids.length][5];
                                int ilen = ids.length;
				for (int i = 0; i < ilen; i++) {
					newZoneStrings[i][ZONE_ID] = ids[i]; // - time zone ID
					String key = ids[i].toUpperCase();
					newZoneStrings[i][ZONE_LONGNAME] = getLocalizedValue(L10N_ZONE_LONGNAME + key, ids[i]);
					newZoneStrings[i][ZONE_SHORTNAME] = getLocalizedValue(L10N_ZONE_SHORTNAME + key, ids[i]);
					newZoneStrings[i][ZONE_LONGNAME_DST] = getLocalizedValue(L10N_ZONE_LONGNAME_DST + key, ids[i]);
					newZoneStrings[i][ZONE_SHORTNAME_DST] = getLocalizedValue(L10N_ZONE_SHORTNAME_DST + key, ids[i]);
				}
				zoneStrings = newZoneStrings;
			}
		}
		return zoneStrings;
	}

	public void setZoneStrings(String[][] newZoneStrings) {
		if (newZoneStrings != null) {
			for (String zone[] : newZoneStrings) {
				if (zone.length < 5) {
					throw new IllegalArgumentException("Expecting inner array size of 5");
				}
			}
		}
		zoneStrings = newZoneStrings;
	}

	public void setShortWeekdays(String[] newShortWeekdays) {
		if (newShortWeekdays.length != 7) {
			throw new IllegalArgumentException("Expecting array size of 7");
		}
		shortWeekdays = newShortWeekdays;
	}

	String[] createShortforms(String longForms[], String l10nKey) {
		String shortForms[] = new String[longForms.length];
                int sflen = shortForms.length;
		for (int i = 0; i < sflen; i++) {
			String shortForm = getLocalizedValue(l10nKey + longForms[i].toUpperCase(), null);
			if (shortForm != null) {
				shortForms[i] = shortForm;
			} else {
				int len = longForms[i].length();
				if (len < 3) {
					shortForms[i] = longForms[i];
				} else {
					shortForms[i] = longForms[i].substring(0, 3);
				}
			}
		}
		return shortForms;
	}

	public String[] getShortWeekdays() {
		synchronized (this) {
			if (shortWeekdays == null) {
				shortWeekdays = createShortforms(getWeekdays(), L10N_WEEKDAY_SHORTNAME);
			}
		}
		return shortWeekdays;
	}

	public String[] getWeekdays() {
		synchronized (this) {
			if (weekdays == null) {
				if (resourceBundle == null) {
					return WEEKDAYS;
				}
                                int wlen = WEEKDAYS.length;
				String newWeekdays[] = new String[wlen];
				for (int i = 0; i < wlen; i++) {
					String key = WEEKDAYS[i].toUpperCase();
					newWeekdays[i] = getLocalizedValue(L10N_WEEKDAY_LONGNAME + key, WEEKDAYS[i]);
				}
				weekdays = newWeekdays;
			}
		}
		return weekdays;
	}

	public void setWeekdays(String[] newWeekdays) {
		if (newWeekdays != null && newWeekdays.length != 7) {
			throw new IllegalArgumentException("Expecting array size of 7");
		}
		weekdays = newWeekdays;
	}

	public void setShortMonths(String[] newShortMonths) {
		if (newShortMonths != null && newShortMonths.length != 12) {
			throw new IllegalArgumentException("Expecting array size of 12");
		}
		shortMonths = newShortMonths;
	}

	public String[] getShortMonths() {
		synchronized (this) {
			if (shortMonths == null) {
				shortMonths = createShortforms(MONTHS, L10N_MONTH_SHORTNAME);
			}
		}
		return shortMonths;
	}

	public void setMonths(String[] newMonths) {
		if (newMonths != null && newMonths.length != 12) {
			throw new IllegalArgumentException("Expecting array size of 12");
		}
		months = newMonths;
	}

	public String[] getMonths() {
		synchronized (this) {
			if (months == null) {
				if (resourceBundle == null) {
					return MONTHS;
				}
                                int mlen = MONTHS.length;
				String newMonths[] = new String[mlen];
				for (int i = 0; i < mlen; i++) {
					String key = MONTHS[i].toUpperCase();
					newMonths[i] = getLocalizedValue(L10N_MONTH_LONGNAME + key, MONTHS[i]);
				}
				months = newMonths;
			}
		}
		return months;
	}

	public String[] getEras() {
		synchronized (this) {
			if (eras == null) {
				if (resourceBundle == null) {
					return ERAS;
				}
				String newEras[] = new String[2];
				newEras[0] = getLocalizedValue(L10N_ERA + "BC", ERAS[0]);
				newEras[1] = getLocalizedValue(L10N_ERA + "AD", ERAS[1]);
				eras = newEras;
			}
		}
		return eras;
	}

	public void setEras(String[] newEras) {
		eras = newEras;
	}

	public Object clone() {
		DateFormatSymbols dfs = new DateFormatSymbols();
		// TODO: do a deep clone
		dfs.ampms = ampms;
		dfs.eras = eras;
		dfs.months = months;
		dfs.shortMonths = shortMonths;
		dfs.shortWeekdays = shortWeekdays;
		dfs.weekdays = weekdays;
		dfs.zoneStrings = zoneStrings;
		dfs.resourceBundle = resourceBundle;
		return dfs;
	}
}
