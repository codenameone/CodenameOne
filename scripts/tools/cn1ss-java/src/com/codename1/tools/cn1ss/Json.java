package com.codename1.tools.cn1ss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {
    }

    static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        write(value, builder);
        return builder.toString();
    }

    private static void write(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String) {
            builder.append('"').append(escape((String) value)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value.toString());
        } else if (value instanceof Map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                write(entry.getKey().toString(), builder);
                builder.append(':');
                write(entry.getValue(), builder);
            }
            builder.append('}');
        } else if (value instanceof Iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                write(item, builder);
            }
            builder.append(']');
        } else {
            write(value.toString(), builder);
        }
    }

    private static String escape(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        return builder.toString();
    }

    static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("Unexpected trailing data in JSON");
        }
        return value;
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
            this.index = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            switch (ch) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    if (ch == '-' || Character.isDigit(ch)) {
                        return parseNumber();
                    }
                    throw new IllegalArgumentException("Unexpected character in JSON: " + ch);
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++; // skip {
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++; // skip [
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() {
            if (!peek('"')) {
                throw new IllegalArgumentException("Expected string");
            }
            index++; // skip opening quote
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    break;
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw new IllegalArgumentException("Incomplete escape sequence");
                    }
                    char esc = text.charAt(index++);
                    switch (esc) {
                        case '"':
                        case '\\':
                        case '/':
                            builder.append(esc);
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > text.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape");
                            }
                            String hex = text.substring(index, index + 4);
                            index += 4;
                            builder.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape sequence: \\" + esc);
                    }
                } else {
                    builder.append(ch);
                }
            }
            return builder.toString();
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (peek('.')) {
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String slice = text.substring(start, index);
            if (slice.indexOf('.') >= 0 || slice.indexOf('e') >= 0 || slice.indexOf('E') >= 0) {
                return Double.valueOf(slice);
            }
            try {
                return Long.valueOf(slice);
            } catch (NumberFormatException ex) {
                return Double.valueOf(slice);
            }
        }

        private void expect(char ch) {
            skipWhitespace();
            if (isEnd() || text.charAt(index) != ch) {
                throw new IllegalArgumentException("Expected '" + ch + "'");
            }
            index++;
        }

        private void expect(String literal) {
            skipWhitespace();
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected '" + literal + "'");
            }
            index += literal.length();
        }

        void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (!Character.isWhitespace(ch)) {
                    break;
                }
                index++;
            }
        }

        boolean isEnd() {
            return index >= text.length();
        }

        private boolean peek(char ch) {
            return index < text.length() && text.charAt(index) == ch;
        }
    }
}
