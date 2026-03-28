import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class JsLocaleTimeZoneApp {
    public static int result;

    public static void main(String[] args) {
        int score = 0;

        Locale locale = Locale.getDefault();
        if (locale != null) {
            score |= 1;
        }
        if (locale != null && locale.getLanguage() != null && locale.getLanguage().length() > 0) {
            score |= 2;
        }
        if (locale != null && locale.getCountry() != null && locale.getCountry().length() > 0) {
            score |= 4;
        }

        TimeZone timeZone = TimeZone.getDefault();
        if (timeZone != null && timeZone.getID() != null && timeZone.getID().length() > 0) {
            score |= 8;
        }
        String[] ids = TimeZone.getAvailableIDs();
        if (ids != null && ids.length >= 1) {
            score |= 16;
        }

        int rawOffset = timeZone.getRawOffset();
        if (rawOffset >= -43200000 && rawOffset <= 50400000) {
            score |= 32;
        }

        int offset = timeZone.getOffset(1, 2024, 0, 15, 2, 12 * 60 * 60 * 1000);
        if (offset >= -43200000 && offset <= 50400000) {
            score |= 64;
        }

        Date sample = new Date(1704067200000L);
        String formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(sample);
        if (formatted != null && formatted.length() > 0) {
            score |= 128;
        }

        String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(sample);
        if (formattedDate != null && formattedDate.length() > 0) {
            score |= 256;
        }

        result = score;
    }
}
