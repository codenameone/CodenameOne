package dart.core;

import dart.runtime.DartRuntime;

/**
 * Dart's {@code dart:core} {@code StringBuffer} — a mutable sequence of
 * characters used to build strings efficiently. Backed by a
 * {@link java.lang.StringBuilder}.
 *
 * <p>Dart's {@code write}/{@code writeln} accept any {@code Object?} and append
 * its Dart string representation (via {@link DartRuntime#str(Object)} so a Dart
 * object's {@code toString()} semantics are honoured, and {@code null} renders
 * as {@code "null"}). {@code writeCharCode} appends the UTF-16 code unit.</p>
 */
public final class StringBuffer {

    private final StringBuilder sb = new StringBuilder();

    public StringBuffer() {
    }

    /** {@code StringBuffer([Object content = ""])} — seeds with the content's string. */
    public StringBuffer(Object content) {
        sb.append(DartRuntime.str(content));
    }

    /** Dart's {@code StringBuffer.length} getter — number of UTF-16 code units. */
    public long length() {
        return sb.length();
    }

    /** Dart's {@code StringBuffer.isEmpty} getter. */
    public boolean isEmpty() {
        return sb.length() == 0;
    }

    /** Dart's {@code StringBuffer.isNotEmpty} getter. */
    public boolean isNotEmpty() {
        return sb.length() != 0;
    }

    /** Dart's {@code StringBuffer.write(Object? object)}. */
    public void write(Object object) {
        sb.append(DartRuntime.str(object));
    }

    /** Dart's {@code StringBuffer.writeln([Object? object = ""])}. */
    public void writeln() {
        sb.append('\n');
    }

    public void writeln(Object object) {
        sb.append(DartRuntime.str(object));
        sb.append('\n');
    }

    /** Dart's {@code StringBuffer.writeCharCode(int charCode)}. */
    public void writeCharCode(long charCode) {
        sb.append((char) charCode);
    }

    /** Dart's {@code StringBuffer.writeAll(Iterable objects, [String separator = ""])}. */
    public void writeAll(Iterable<?> objects) {
        writeAll(objects, "");
    }

    public void writeAll(Iterable<?> objects, String separator) {
        boolean first = true;
        for (Object o : objects) {
            if (!first && separator != null) {
                sb.append(separator);
            }
            sb.append(DartRuntime.str(o));
            first = false;
        }
    }

    /** Dart's {@code StringBuffer.clear()}. */
    public void clear() {
        sb.setLength(0);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
