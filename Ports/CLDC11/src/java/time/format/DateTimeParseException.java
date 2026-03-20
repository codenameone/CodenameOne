package java.time.format;

public class DateTimeParseException extends RuntimeException {
    private final String parsedData;
    private final int errorIndex;

    public DateTimeParseException(String message, CharSequence parsedData, int errorIndex) {
        super(message);
        this.parsedData = parsedData == null ? null : parsedData.toString();
        this.errorIndex = errorIndex;
    }

    public String getParsedString() {
        return parsedData;
    }

    public int getErrorIndex() {
        return errorIndex;
    }
}
