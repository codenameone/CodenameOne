package dart.core;

/**
 * Dart's RangeError — a numeric argument was outside its valid range,
 * including list index errors.
 */
public class RangeError extends ArgumentError {

    public RangeError(String message) {
        super(message);
    }

    /**
     * Guard used by DartList index access; mirrors RangeError.checkValidIndex.
     */
    public static long checkValidIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new RangeError("RangeError (index): Invalid value: Not in inclusive range 0.." + (length - 1) + ": " + index);
        }
        return index;
    }

    public static long checkValueInInterval(long value, long minValue, long maxValue, String name) {
        if (value < minValue || value > maxValue) {
            throw new RangeError("RangeError (" + name + "): Invalid value: Not in inclusive range "
                    + minValue + ".." + maxValue + ": " + value);
        }
        return value;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
