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

import com.codename1.io.Util;
import java.util.Calendar;
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
    
    /**
     * Allows turning localization on/off defaults to localization
     */
    private boolean localized = true;

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
        if(localized) {
            Hashtable<String, String> resourceBundle = getResourceBundle();
            if (resourceBundle == null || resourceBundle.containsKey(key) == false) {
                return defaultValue;
            }
            String v = (resourceBundle.get(key));
            return (v.length() > 0) ? v : defaultValue;
        }
        return defaultValue;
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
        
        /**
         * Adds a timezone mapping so that SimpleDateFormat can recognize abbreviated timezones.
         * @param zoneId The TimeZone ID.  E.g. America/New_York
         * @param longName The long name of the mapping.  E.g. Eastern Standard Time
         * @param longNameDST The long name of the mapping in daylight saving time.  E.g. Eastern Daylight Time
         * @param shortName The short name of the mapping.  E.g. EST
         * @param shortNameDST The short name of the mapping in daylight saving time. E.g. EDT
         */
        public void addZoneMapping(String zoneId, String longName, String longNameDST, String shortName, String shortNameDST) {
            Hashtable<String,String> h = getResourceBundle();
            if (h == null) {
                h = new Hashtable<String,String>();
                setResourceBundle(h);
            }
            zoneId = zoneId.toUpperCase();
            h.put(L10N_ZONE_LONGNAME + zoneId, longName);
            h.put(L10N_ZONE_LONGNAME_DST + zoneId, longNameDST);
            h.put(L10N_ZONE_SHORTNAME + zoneId, shortName);
            h.put(L10N_ZONE_SHORTNAME_DST + zoneId, shortNameDST);
        }
        
        /**
         * Gets the short name of a given timezone.
         * @param zoneId The timezone ID.  E.g. America/Vancouver
         * @param defaultValue A default value if no mapping is found.
         * @return The short name of the timezone.  E.g. PST
         */
        public String getZoneShortName(String zoneId, String defaultValue) {
            zoneId = zoneId.toUpperCase();
            return getLocalizedValue(L10N_ZONE_SHORTNAME + zoneId, defaultValue);
        }
        
        /**
         * Gets the short name of a given timezone in daylight saving time.
         * @param zoneId The timezone ID.  E.g. America/Vancouver
         * @param defaultValue A default value if no mapping is found.
         * @return The short name of the timezone in daylight saving time.  E.g. PDT
         */
        public String getZoneShortNameDST(String zoneId, String defaultValue) {
            zoneId = zoneId.toUpperCase();
            return getLocalizedValue(L10N_ZONE_SHORTNAME_DST + zoneId, defaultValue);
        }
        
        /**
         * Gets the long name of a given timezone.
         * @param zoneId The timezone ID.  E.g. America/Vancouver
         * @param defaultValue A default value if no mapping is found.
         * @return The short name of the timezone.  E.g. Pacific Standard Time
         */
        public String getZoneLongName(String zoneId, String defaultValue) {
            zoneId = zoneId.toUpperCase();
            return getLocalizedValue(L10N_ZONE_LONGNAME + zoneId, defaultValue);
        }
        
        /**
         * Gets the long name of a given timezone in daylight saving time.
         * @param zoneId The timezone ID.  E.g. America/Vancouver
         * @param defaultValue A default value if no mapping is found.
         * @return The short name of the timezone.  E.g. Pacific Daylight Time
         */
        public String getZoneLongNameDST(String zoneId, String defaultValue) {
            zoneId = zoneId.toUpperCase();
            return getLocalizedValue(L10N_ZONE_LONGNAME_DST + zoneId, defaultValue);
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
            String defaultVal = longForms == MONTHS ? getPlatformLocalizedShortMonths()[i] : null;
			String shortForm = getLocalizedValue(l10nKey + longForms[i].toUpperCase(), defaultVal);
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

        private String[] platformLocalizedMonths;
        private String[] getPlatformLocalizedMonths() {
            if(!localized) {
                return MONTHS;
            }
            if (platformLocalizedMonths == null) {
                int len = MONTHS.length;
                platformLocalizedMonths = new String[len];
                L10NManager l10n = L10NManager.getInstance();
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 15);
                for (int i=0; i<len; i++) {
                    cal.set(Calendar.MONTH, i);
                    String fmt = l10n.formatDateLongStyle(cal.getTime());
                    try {
                        platformLocalizedMonths[i] = extractMonthName(fmt);
                    } catch (ParseException ex) {
                        platformLocalizedMonths[i] = MONTHS[i];
                    }
                }
            }
            return platformLocalizedMonths;
        }
        
        private String[] platformLocalizedShortMonths;
        private String[] getPlatformLocalizedShortMonths() {
            if(!localized) {
                return MONTHS;
            }
            if (platformLocalizedShortMonths == null) {
                int len = MONTHS.length;
                platformLocalizedShortMonths = new String[len];
                L10NManager l10n = L10NManager.getInstance();
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 15);
                for (int i=0; i<len; i++) {
                    cal.set(Calendar.MONTH, i);
                    String fmt = l10n.formatDateLongStyle(cal.getTime());
                    try {
                        platformLocalizedShortMonths[i] = extractMonthName(fmt).substring(0, 3);
                    } catch (ParseException ex) {
                        platformLocalizedShortMonths[i] = MONTHS[i].substring(0, 3);
                    }
                }
            }
            return platformLocalizedShortMonths;
        }
        
        private String extractMonthName(String dateStr) throws ParseException {
            String[] parts = Util.split(dateStr, " ");
            for (String part : parts) {
                if (part.length() == 0) {
                    continue;
                }
                String firstChar = part.substring(0, 1);
                if (!firstChar.toLowerCase().equals(firstChar.toUpperCase())) {
                    return part;
                }
            }
            throw new ParseException("Cannot extract month from string", 0);
            
        }
        
	public String[] getMonths() {
		synchronized (this) {
			if (months == null) {
				if (resourceBundle == null) {
					return getPlatformLocalizedMonths();
				}
                                int mlen = MONTHS.length;
				String newMonths[] = new String[mlen];
				for (int i = 0; i < mlen; i++) {
					String key = MONTHS[i].toUpperCase();
					newMonths[i] = getLocalizedValue(L10N_MONTH_LONGNAME + key, getPlatformLocalizedMonths()[i]);
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

    /**
     * Allows turning localization on/off defaults to localization
     * @return the localized
     */
    public boolean isLocalized() {
        return localized;
    }

    /**
     * Allows turning localization on/off defaults to localization
     * @param localized the localized to set
     */
    public void setLocalized(boolean localized) {
        this.localized = localized;
    }
}
