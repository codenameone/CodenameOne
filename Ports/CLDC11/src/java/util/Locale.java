package java.util;

/**
 *
 * @author Shai Almog
 */
public class Locale {
    private static Locale defaultLocale;
    private String language;
    private String country;
    public Locale(String language, String locale) {
        this.language = language;
        this.country = locale;
    }

    public Locale() {
        language = "en";
        int pos;
        if (language != null && (pos = language.indexOf('-')) != -1) {
            country = language.substring(pos+1);
            language = language.substring(0, pos);
        }
        if (language != null && (pos = language.indexOf('_')) != -1) {
            country = language.substring(pos+1);
            language = language.substring(0, pos);
        }
        if (country == null) {
            country = "US";//getOSCountry();
        }
    }

    public static Locale getDefault() {
        if(defaultLocale == null) {
            defaultLocale = new Locale();
        }
        return defaultLocale;
    }

    public static void setDefault(Locale l) {
        defaultLocale = l;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }


}
