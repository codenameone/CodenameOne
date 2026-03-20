import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TimeEdgeApp {
    private static String two(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static String localDateTimeString(LocalDateTime value) {
        return value.getYear() + "-" + two(value.getMonthValue()) + "-" + two(value.getDayOfMonth())
                + "T" + two(value.getHour()) + ":" + two(value.getMinute()) + ":" + two(value.getSecond());
    }

    private static String zonedString(ZonedDateTime value) {
        return localDateTimeString(value.toLocalDateTime()) + value.getOffset().getId() + "[" + value.getZone().getId() + "]";
    }

    private static String calculate() {
        LocalDate leap2000 = LocalDate.of(2000, 2, 29);
        LocalDate century1900 = LocalDate.of(1900, 2, 28).plusDays(1);
        LocalDate century2100 = LocalDate.of(2100, 2, 28).plusDays(1);
        LocalDate leap2004 = LocalDate.of(2004, 2, 29).plusYears(1);
        LocalDate rollover = LocalDate.of(2020, 1, 31).plusMonths(1);

        Instant baseInstant = Instant.parse("2020-03-08T06:30:00Z");
        ZonedDateTime nyBeforeGap = ZonedDateTime.ofInstant(baseInstant, ZoneId.of("America/New_York"));
        ZonedDateTime nyAfterGap = ZonedDateTime.ofInstant(Instant.parse("2020-03-08T07:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime nyOverlapEarly = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T05:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime nyOverlapLate = ZonedDateTime.ofInstant(Instant.parse("2020-11-01T06:30:00Z"), ZoneId.of("America/New_York"));
        ZonedDateTime berlinSummer = ZonedDateTime.ofInstant(Instant.parse("2020-06-01T10:15:30Z"), ZoneId.of("Europe/Berlin"));

        Duration duration = Duration.ofMillis(90061).plus(Duration.ofSeconds(5));
        Period period = Period.of(1, 1, 1);
        LocalDate periodTarget = LocalDate.of(2019, 1, 31).plusYears(period.getYears()).plusMonths(period.getMonths()).plusDays(period.getDays());
        LocalDateTime utcLocal = LocalDateTime.ofInstant(baseInstant, ZoneOffset.UTC);

        StringBuilder result = new StringBuilder();
        result.append(leap2000).append('|');
        result.append(century1900).append('|');
        result.append(century2100).append('|');
        result.append(leap2004).append('|');
        result.append(rollover).append('|');
        result.append(zonedString(nyBeforeGap)).append('|');
        result.append(zonedString(nyAfterGap)).append('|');
        result.append(zonedString(nyOverlapEarly)).append('|');
        result.append(zonedString(nyOverlapLate)).append('|');
        result.append(zonedString(berlinSummer)).append('|');
        result.append(duration.toMillis()).append('|');
        result.append(periodTarget).append('|');
        result.append(LocalTime.of(23, 59, 59).plusSeconds(2)).append('|');
        result.append(localDateTimeString(utcLocal));
        return result.toString();
    }

    public static void main(String[] args) {
        System.out.println("RESULT=" + calculate());
    }
}
