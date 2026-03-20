package java.time;

public final class Period {
    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days) {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public static Period of(int years, int months, int days) {
        return new Period(years, months, days);
    }

    public static Period ofDays(int days) {
        return new Period(0, 0, days);
    }

    public static Period ofMonths(int months) {
        return new Period(0, months, 0);
    }

    public static Period ofYears(int years) {
        return new Period(years, 0, 0);
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public LocalDate addTo(LocalDate date) {
        return date.plusYears(years).plusMonths(months).plusDays(days);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("P");
        if (years != 0) {
            sb.append(years).append('Y');
        }
        if (months != 0) {
            sb.append(months).append('M');
        }
        if (days != 0 || (years == 0 && months == 0)) {
            sb.append(days).append('D');
        }
        return sb.toString();
    }
}
