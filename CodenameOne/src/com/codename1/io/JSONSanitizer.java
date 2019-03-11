// Copyright (C) 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.codename1.io;

/**
 * Given JSON-like content, converts it to valid JSON.
 * This can be attached at either end of a data-pipeline to help satisfy
 * Postel's principle:
 * <blockquote>
 * be conservative in what you do, be liberal in what you accept from others
 * </blockquote>
 * <p>
 * Applied to JSON-like content from others, it will produce well-formed JSON
 * that should satisfy any parser you use.
 * <p>
 * Applied to your output before you send, it will coerce minor mistakes in
 * encoding and make it easier to embed your JSON in HTML and XML.
 *
 * <h3>Input</h3>
 * The sanitizer takes JSON like content, and interprets it as JS eval would.
 * Specifically, it deals with these non-standard constructs.
 * <ul>
 * <li>{@code '...'} Single quoted strings are converted to JSON strings.
 * <li>{@code \xAB} Hex escapes are converted to JSON unicode escapes.
 * <li>{@code \012} Octal escapes are converted to JSON unicode escapes.
 * <li>{@code 0xAB} Hex integer literals are converted to JSON decimal numbers.
 * <li>{@code 012} Octal integer literals are converted to JSON decimal numbers.
 * <li>{@code +.5} Decimal numbers are coerced to JSON's stricter format.
 * <li>{@code [0,,2]} Elisions in arrays are filled with {@code null}.
 * <li>{@code [1,2,3,]} Trailing commas are removed.
 * <li><code>{foo:"bar"}</code> Unquoted property names are quoted.
 * <li><code>//comments</code> JS style line and block comments are removed.
 * <li><code>(...)</code> Grouping parentheses are removed.
 * </ul>
 *
 * The sanitizer fixes missing punctuation, end quotes, and mismatched or
 * missing close brackets. If an input contains only white-space then the valid
 * JSON string {@code null} is substituted.
 *
 * <h3>Output</h3>
 * The output is well-formed JSON as defined by
 * <a href="http://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>.
 * The output satisfies three additional properties:
 * <ol>
 * <li>The output will not contain the substring (case-insensitively)
 *   {@code "</script"} so can be embedded inside an HTML script element without
 *   further encoding.
 * <li>The output will not contain the substring {@code "]]>"} so can be
 *   embedded inside an XML CDATA section without further encoding.</li>
 * <li>The output is a valid Javascript expression, so can be parsed by
 *   Javascript's <code>eval</code> builtin (after being wrapped in parentheses)
 *   or by <code>JSON.parse</code>.
 *   Specifically, the output will not contain any string literals with embedded
 *   JS newlines (U+2028 Paragraph separator or U+2029 Line separator).
 * <li>The output contains only valid Unicode scalar values
 *   (no isolated UTF-16 surrogates) that are
 *   <a href="http://www.w3.org/TR/xml/#charsets">allowed in XML</a> unescaped.
 * </ol>
 *
 * <h3>Security</h3>
 * Since the output is well-formed JSON, passing it to <code>eval</code> will
 * have no side-effects and no free variables, so is neither a code-injection
 * vector, nor a vector for exfiltration of secrets.
 *
 * <p>This library only ensures that the JSON string &rarr; Javascript object
 * phase has no side effects and resolves no free variables, and cannot control
 * how other client side code later interprets the resulting Javascript object.
 * So if client-side code takes a part of the parsed data that is controlled by
 * an attacker and passes it back through a powerful interpreter like
 * {@code eval} or {@code innerHTML} then that client-side code might suffer
 * unintended side-effects.
 *
 * <h3>Efficiency</h3>
 * The sanitize method will return the input string without allocating a new
 * buffer when the input is already valid JSON that satisfies the properties
 * above.  Thus, if used on input that is usually well formed, it has minimal
 * memory overhead.
 * <p>The sanitize method takes O(n) time where n is the length in UTF-16
 * code-units.
 */
final class JSONSanitizer {

  /** The default for the maximumNestingDepth constructor parameter. */
  public static final int DEFAULT_NESTING_DEPTH = 64;

  /** The maximum value for the maximumNestingDepth constructor parameter. */
  public static final int MAXIMUM_NESTING_DEPTH = 4096;

  /**
   * Given JSON-like content, produces a string of JSON that is safe to embed,
   * safe to pass to JavaScript's {@code eval} operator.
   *
   * @param jsonish JSON-like content.
   * @return embeddable JSON
   */
  public static String sanitize(String jsonish) {
    return sanitize(jsonish, DEFAULT_NESTING_DEPTH);
  }

  /**
   * Same as {@link JsonSanitizer#sanitize(String)}, but allows to set a custom
   * maximum nesting depth.
   *
   * @param jsonish JSON-like content.
   * @param maximumNestingDepth maximum nesting depth.
   * @return embeddable JSON
   */
  public static String sanitize(String jsonish, int maximumNestingDepth) {
    JSONSanitizer s = new JSONSanitizer(jsonish, maximumNestingDepth);
    s.sanitize();
    return s.toString();
  }

  /**
   * Describes where we are in a state machine that consists of transitions on
   * complete values, colons, commas, and brackets.
   */
  private enum State {
    /**
     * Immediately after '[' and
     * {@link #BEFORE_ELEMENT before the first element}.
     */
    START_ARRAY,
    /** Before a JSON value in an array or at the top level. */
    BEFORE_ELEMENT,
    /**
     * After a JSON value in an array or at the top level, and before any
     * following comma or close bracket.
     */
    AFTER_ELEMENT,
    /** Immediately after '{' and {@link #BEFORE_KEY before the first key}. */
    START_MAP,
    /** Before a key in a key-value map. */
    BEFORE_KEY,
    /** After a key in a key-value map but before the required colon. */
    AFTER_KEY,
    /** Before a value in a key-value map. */
    BEFORE_VALUE,
    /**
     * After a value in a key-value map but before any following comma or
     * close bracket.
     */
    AFTER_VALUE,
    ;
  }

  /**
   * The maximum nesting depth. According to RFC4627 it is implementation-specific.
   */
  private final int maximumNestingDepth;

  private final String jsonish;

  /**
   * The number of brackets that have been entered and not subsequently exited.
   * Also, the length of the used prefix of {@link #isMap}.
   */
  private int bracketDepth;
  /**
   * {@code isMap[i]} when {@code 0 <= i && i < bracketDepth} is true iff
   * the i-th open bracket was a <code>'{'</code>, not a <code>'['</code>.
   */
  private boolean[] isMap;
  /**
   * If non-null, then contains the sanitized form of
   * {@code jsonish.substring(0, cleaned)}.
   * If {@code null}, then no unclean constructs have been found in
   * {@code jsonish} yet.
   */
  private StringBuilder sanitizedJson;
  /**
   * The length of the prefix of {@link #jsonish} that has been written onto
   * {@link #sanitizedJson}.
   */
  private int cleaned;

  private static final boolean SUPER_VERBOSE_AND_SLOW_LOGGING = false;

  JSONSanitizer(String jsonish) {
    this(jsonish, DEFAULT_NESTING_DEPTH);
  }

  JSONSanitizer(String jsonish, int maximumNestingDepth) {
    this.maximumNestingDepth = Math.min(Math.max(1, maximumNestingDepth),MAXIMUM_NESTING_DEPTH);
    if (SUPER_VERBOSE_AND_SLOW_LOGGING) {
      System.err.println("\n" + jsonish + "\n========");
    }
    this.jsonish = jsonish != null ? jsonish : "null";
  }

  int getMaximumNestingDepth() {
    return this.maximumNestingDepth;
  }

  void sanitize() {
    // Return to consistent state.
    bracketDepth = cleaned = 0;
    sanitizedJson = null;

    State state = State.START_ARRAY;
    int n = jsonish.length();

    // Walk over each token and either validate it, by just advancing i and
    // computing the next state, or manipulate cleaned&sanitizedJson so that
    // sanitizedJson contains the sanitized equivalent of
    // jsonish.substring(0, cleaned).
    token_loop:
    for (int i = 0; i < n; ++i) {
      try {
        char ch = jsonish.charAt(i);
        if (SUPER_VERBOSE_AND_SLOW_LOGGING) {
          String sanitizedJsonStr =
            (sanitizedJson == null ? "" : sanitizedJson)
            + jsonish.substring(cleaned, i);
          System.err.println("i=" + i + ", ch=" + ch + ", state=" + state
                             + ", sanitized=" + sanitizedJsonStr);
        }
        switch (ch) {
          case '\t': case '\n': case '\r': case ' ':
            break;

          case '"': case '\'':
            state = requireValueState(i, state, true);
            int strEnd = endOfQuotedString(jsonish, i);
            sanitizeString(i, strEnd);
            i = strEnd - 1;
            break;

          case '(': case ')':
            // Often JSON-like content which is meant for use by eval is
            // wrapped in parentheses so that the JS parser treats contained
            // curly brackets as part of an object constructor instead of a
            // block statement.
            // We elide these grouping parentheses to ensure valid JSON.
            elide(i, i + 1);
            break;

          case '{': case '[':
            state = requireValueState(i, state, false);
            if (isMap == null) {
              isMap = new boolean[maximumNestingDepth];
            }
            boolean map = ch == '{';
            isMap[bracketDepth] = map;
            ++bracketDepth;
            state = map ? State.START_MAP : State.START_ARRAY;
            break;

          case '}': case ']':
            if (bracketDepth == 0) {
              elide(i, jsonish.length());
              break token_loop;
            }

            // Strip trailing comma to convert {"a":0,} -> {"a":0}
            // and [1,2,3,] -> [1,2,3,]
            switch (state) {
              case BEFORE_VALUE:
                insert(i, "null");
                break;
              case BEFORE_ELEMENT: case BEFORE_KEY:
                elideTrailingComma(i);
                break;
              case AFTER_KEY:
                insert(i, ":null");
                break;
              case START_MAP: case START_ARRAY:
              case AFTER_ELEMENT: case AFTER_VALUE: break;
            }

            --bracketDepth;
            char closeBracket = isMap[bracketDepth] ? '}' : ']';
            if (ch != closeBracket) {
              replace(i, i + 1, closeBracket);
            }
            state = bracketDepth == 0 || !isMap[bracketDepth - 1]
                ? State.AFTER_ELEMENT : State.AFTER_VALUE;
            break;
          case ',':
            if (bracketDepth == 0) { throw new RuntimeException("Unbracketed comma"); }
            // Convert comma elisions like [1,,3] to [1,null,3].
            // [1,,3] in JS is an array that has no element at index 1
            // according to the "in" operator so accessing index 1 will
            // yield the special value "undefined" which is equivalent to
            // JS's "null" value according to "==".
            switch (state) {
              // Normal
              case AFTER_ELEMENT:
                state = State.BEFORE_ELEMENT;
                break;
              case AFTER_VALUE:
                state = State.BEFORE_KEY;
                break;
              // Array elision.
              case START_ARRAY: case BEFORE_ELEMENT:
                insert(i, "null");
                state = State.BEFORE_ELEMENT;
                break;
              // Ignore
              case START_MAP: case BEFORE_KEY:
              case AFTER_KEY:
                elide(i, i + 1);
                break;
              // Supply missing value.
              case BEFORE_VALUE:
                insert(i, "null");
                state = State.BEFORE_KEY;
                break;
            }
            break;

          case ':':
            if (state == State.AFTER_KEY) {
              state = State.BEFORE_VALUE;
            } else {
              elide(i, i + 1);
            }
            break;

          case '/':
            // Skip over JS-style comments since people like inserting them into
            // data files and getting huffy with Crockford when he says no to
            // versioning JSON to allow ignorable tokens.
            int end = i + 1;
            if (i + 1 < n) {
              switch (jsonish.charAt(i + 1)) {
                case '/':
                  end = n;  // Worst case.
                  for (int j = i + 2; j < n; ++j) {
                    char cch = jsonish.charAt(j);
                    if (cch == '\n' || cch == '\r'
                        || cch == '\u2028' || cch == '\u2029') {
                      end = j + 1;
                      break;
                    }
                  }
                  break;
                case '*':
                  end = n;
                  if (i + 3 < n) {
                    for (int j = i + 2;
                         (j = jsonish.indexOf('/', j + 1)) >= 0;) {
                      if (jsonish.charAt(j - 1) == '*') {
                        end = j + 1;
                        break;
                      }
                    }
                  }
                  break;
              }
            }
            elide(i, end);
            i = end - 1;
            break;

          default:
            // Three kinds of other values can occur.
            // 1. Numbers
            // 2. Keyword values ("false", "null", "true")
            // 3. Unquoted JS property names as in the JS expression
            //      ({ foo: "bar"})
            //    which is equivalent to the JSON
            //      { "foo": "bar" }
            // 4. Cruft tokens like BOMs.

            // Look for a run of '.', [0-9], [a-zA-Z_$], [+-] which subsumes
            // all the above without including any JSON special characters
            // outside keyword and number.
            int runEnd;
            for (runEnd = i; runEnd < n; ++runEnd) {
              char tch = jsonish.charAt(runEnd);
              if (('a' <= tch && tch <= 'z') || ('0' <= tch && tch <= '9')
                  || tch == '+' || tch == '-' || tch == '.'
                  || ('A' <= tch && tch <= 'Z') || tch == '_' || tch == '$') {
                continue;
              }
              break;
            }

            if (runEnd == i) {
              elide(i, i + 1);
              break;
            }

            state = requireValueState(i, state, true);

            boolean isNumber = ('0' <= ch && ch <= '9')
               || ch == '.' || ch == '+' || ch == '-';
            boolean isKeyword = !isNumber && isKeyword(i, runEnd);

            if (!(isNumber || isKeyword)) {
              // We're going to have to quote the output.  Further expand to
              // include more of an unquoted token in a string.
              for (; runEnd < n; ++runEnd) {
                if (isJsonSpecialChar(runEnd)) {
                  break;
                }
              }
              if (runEnd < n && jsonish.charAt(runEnd) == '"') {
                ++runEnd;
              }
            }

            if (state == State.AFTER_KEY) {
              // We need to quote whatever we have since it is used as a
              // property name in a map and only quoted strings can be used that
              // way in JSON.
              insert(i, '"');
              if (isNumber) {
                // By JS rules,
                //   { .5e-1: "bar" }
                // is the same as
                //   { "0.05": "bar" }
                // because a number literal is converted to its string form
                // before being used as a property name.
                canonicalizeNumber(i, runEnd);
                // We intentionally ignore the return value of canonicalize.
                // Uncanonicalizable numbers just get put straight through as
                // string values.
                insert(runEnd, '"');
              } else {
                sanitizeString(i, runEnd);
              }
            } else {
              if (isNumber) {
                // Convert hex and octal constants to decimal and ensure that
                // integer and fraction portions are not empty.
                normalizeNumber(i, runEnd);
              } else if (!isKeyword) {
                // Treat as an unquoted string literal.
                insert(i, '"');
                sanitizeString(i, runEnd);
              }
            }
            i = runEnd - 1;
        }
      } catch (@SuppressWarnings("unused") UnbracketedComma e) {
        elide(i, jsonish.length());
        break;
      }
    }

    if (state == State.START_ARRAY && bracketDepth == 0) {
      // No tokens.  Only whitespace
      insert(n, "null");
      state = State.AFTER_ELEMENT;
    }

    if (SUPER_VERBOSE_AND_SLOW_LOGGING) {
      System.err.println(
          "state=" + state + ", sanitizedJson=" + sanitizedJson
          + ", cleaned=" + cleaned + ", bracketDepth=" + bracketDepth);
    }

    if ((sanitizedJson != null && sanitizedJson.length() != 0)
        || cleaned != 0 || bracketDepth != 0) {
      if (sanitizedJson == null) {
        sanitizedJson = new StringBuilder(n + bracketDepth);
      }
      sanitizedJson.append(jsonish.substring(cleaned, n));
      cleaned = n;

      switch (state) {
        case BEFORE_ELEMENT: case BEFORE_KEY:
          elideTrailingComma(n);
          break;
        case AFTER_KEY:
          sanitizedJson.append(":null");
          break;
        case BEFORE_VALUE:
          sanitizedJson.append("null");
          break;
        default: break;
      }

      // Insert brackets to close unclosed content.
      while (bracketDepth != 0) {
        sanitizedJson.append(isMap[--bracketDepth] ? '}' : ']');
      }
    }
  }

  /**
   * Ensures that the output corresponding to {@code jsonish[start:end]} is a
   * valid JSON string that has the same meaning when parsed by Javascript
   * {@code eval}.
   * <ul>
   *   <li>Making sure that it is fully quoted with double-quotes.
   *   <li>Escaping any Javascript newlines : CR, LF, U+2028, U+2029
   *   <li>Escaping HTML special characters to allow it to be safely embedded
   *       in HTML {@code <script>} elements and XML {@code <!CDATA[...]]>}
   *       sections.
   *   <li>Rewrite hex, octal, and other escapes that are valid in Javascript
   *       but not in JSON.
   * </ul>
   * @param start inclusive
   * @param end   exclusive
   */
  private void sanitizeString(int start, int end) {
    boolean closed = false;
    for (int i = start; i < end; ++i) {
      char ch = jsonish.charAt(i);
      switch (ch) {
        // Fixup newlines.
        case '\n': replace(i, i + 1, "\\n"); break;
        case '\r': replace(i, i + 1, "\\r"); break;
        // Not newlines in JSON but unparseable by JS eval.
        case '\u2028': replace(i, i + 1, "\\u2028"); break;
        case '\u2029': replace(i, i + 1, "\\u2029"); break;
        // String delimiting quotes that need to be converted : 'foo' -> "foo"
        // or internal quotes that might need to be escaped : f"o -> f\"o.
        case '"': case '\'':
          if (i == start) {
            if (ch == '\'') { replace(i, i + 1, '"'); }
          } else {
            if (i + 1 == end) {
              char startDelim = jsonish.charAt(start);
              if (startDelim != '\'') {
                // If we're sanitizing a string whose start was inferred, then
                // treat '"' as closing regardless.
                startDelim = '"';
              }
              closed = startDelim == ch;
            }
            if (closed) {
              if (ch == '\'') { replace(i, i + 1, '"'); }
            } else if (ch == '"') {
              insert(i, '\\');
            }
          }
          break;
        // Embedding.  Disallow </script and ]]> in string literals so that
        // the output can be embedded in HTML script elements and in XML CDATA
        // sections.
        case '/':
          // Don't over escape.  Many JSON bodies contain innocuous HTML
          // that can be safely embedded.
          if (i > start && i + 2 < end && '<' == jsonish.charAt(i - 1)
              && 's' == (jsonish.charAt(i + 1) | 32)
              && 'c' == (jsonish.charAt(i + 2) | 32)) {
            insert(i, '\\');
          }
          break;
        case ']':
          if (i + 2 < end && ']' == jsonish.charAt(i + 1)
              && '>' == jsonish.charAt(i + 2)) {
            replace(i, i + 1, "\\u005d");
          }
          break;
        // Normalize escape sequences.
        case '\\':
          if (i + 1 == end) {
            elide(i, i + 1);
            break;
          }
          char sch = jsonish.charAt(i + 1);
          switch (sch) {
            case 'b': case 'f': case 'n': case 'r': case 't': case '\\':
            case '/': case '"':
              ++i;
              break;
            case 'v':  // Recognized by JS but not by JSON.
              replace(i, i + 2, "\\u0008");
              ++i;
              break;
            case 'x':
              if (i + 4 < end && isHexAt(i+2) && isHexAt(i+3)) {
                replace(i, i + 2, "\\u00");  // \xab -> \u00ab
                i += 3;
                break;
              }
              elide(i, i + 1);
              break;
            case 'u':
              if (i + 6 < end && isHexAt(i + 2) && isHexAt(i + 3)
                  && isHexAt(i + 4) && isHexAt(i + 5)) {
                i += 5;
                break;
              }
              elide(i, i + 1);
              break;
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':
              int octalEnd = i + 1;
              if (octalEnd + 1 < end && isOctAt(octalEnd + 1)) {
                ++octalEnd;
                if (ch <= '3' && octalEnd + 1 < end && isOctAt(octalEnd + 1)) {
                  ++octalEnd;
                }
                int value = 0;
                for (int j = i; j < octalEnd; ++j) {
                  value = (value << 3) | (jsonish.charAt(j) - '0');
                }
                replace(i + 1, octalEnd, "u00");
                appendHex(value, 2);
              }
              i = octalEnd - 1;
              break;
            default:
              // Literal char that is recognized by JS but not by JSON.
              // "\-" is valid JS but not valid JSON.
              elide(i, i + 1);
              break;
          }
          break;
        default:
          // Escape all control code-points and isolated surrogates which are
          // not embeddable in XML.
          // http://www.w3.org/TR/xml/#charsets says
          //     Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD]
          //            | [#x10000-#x10FFFF]
          if (ch < 0x20) {
            if (ch == 9 || ch == 0xa || ch == 0xd) { continue; }
          } else if (ch < 0xd800) {  // Not a surrogate.
            continue;
          } else if (ch < 0xe000) {  // A surrogate
            if (Character.isHighSurrogate(ch) && i+1 < end
                && Character.isLowSurrogate(jsonish.charAt(i+1))) {
              ++i;  // Skip over low surrogate since we have already vetted it.
              continue;
            }
          } else if (ch <= 0xfffd) {  // Not one of the 0xff.. controls.
            continue;
          }
          replace(i, i + 1, "\\u");
          for (int j = 4; --j >= 0;) {
            sanitizedJson.append(HEX_DIGITS[(ch >>> (j << 2)) & 0xf]);
          }
          break;
      }
    }
    if (!closed) { insert(end, '"'); }
  }

  private State requireValueState(int pos, State state, boolean canBeKey)
    throws UnbracketedComma {
    switch (state) {
      case START_MAP: case BEFORE_KEY:
        if (canBeKey) {
          return State.AFTER_KEY;
        } else {
          insert(pos, "\"\":");
          return State.AFTER_VALUE;
        }
      case AFTER_KEY:
        insert(pos, ':');
        return State.AFTER_VALUE;
      case BEFORE_VALUE:
        return State.AFTER_VALUE;
      case AFTER_VALUE:
        if (canBeKey) {
          insert(pos, ',');
          return State.AFTER_KEY;
        } else {
          insert(pos, ",\"\":");
          return State.AFTER_VALUE;
        }
      case START_ARRAY: case BEFORE_ELEMENT:
        return State.AFTER_ELEMENT;
      case AFTER_ELEMENT:
        if (bracketDepth == 0) { throw new RuntimeException("Unbracketed comma"); }
        insert(pos, ',');
        return State.AFTER_ELEMENT;
    }
    throw new AssertionError();
  }

  private void insert(int pos, char ch) {
    replace(pos, pos, ch);
  }

  private void insert(int pos, String s) {
    replace(pos, pos, s);
  }

  private void elide(int start, int end) {
    if (sanitizedJson == null) {
      sanitizedJson = new StringBuilder(jsonish.length() + 16);
    }
    sanitizedJson.append(jsonish.substring(cleaned, start));
    cleaned = end;
  }

  private void replace(int start, int end, char ch) {
    elide(start, end);
    sanitizedJson.append(ch);
  }

  private void replace(int start, int end, String s) {
    elide(start, end);
    sanitizedJson.append(s);
  }

  /**
   * The position past the last character within the quotes of the quoted
   * string starting at {@code s.charAt(start)}.  Does not assume that the
   * quoted string is properly closed.
   */
  private static int endOfQuotedString(String s, int start) {
    char quote = s.charAt(start);
    for (int i = start; (i = s.indexOf(quote, i + 1)) >= 0;) {
      // If there are an even number of preceding backslashes then this is
      // the end of the string.
      int slashRunStart = i;
      while (slashRunStart > start && s.charAt(slashRunStart - 1) == '\\') {
        --slashRunStart;
      }
      if (((i - slashRunStart) & 1) == 0) {
        return i + 1;
      }
    }
    return s.length();
  }

  private void elideTrailingComma(int closeBracketPos) {
    // The content before closeBracketPos is stored in two places.
    // 1. sanitizedJson
    // 2. jsonish.substring(cleaned, closeBracketPos)
    // We walk over whitespace characters in both right-to-left looking for a
    // comma.
    for (int i = closeBracketPos; --i >= cleaned;) {
      switch (jsonish.charAt(i)) {
        case '\t': case '\n': case '\r': case ' ':
          continue;
        case ',':
          elide(i, i+1);
          return;
        default: throw new AssertionError("" + jsonish.charAt(i));
      }
    }
    assert sanitizedJson != null;
    for (int i = sanitizedJson.length(); --i >= 0;) {
      switch (sanitizedJson.charAt(i)) {
        case '\t': case '\n': case '\r': case ' ':
          continue;
        case ',':
          sanitizedJson.setLength(i);
          return;
        default: throw new AssertionError("" + sanitizedJson.charAt(i));
      }
    }
    throw new AssertionError(
        "Trailing comma not found in " + jsonish + " or " + sanitizedJson);
  }

  /**
   * Ensures that the given run of characters is a valid JSON number.  This is
   * less aggressive than {@link #canonicalizeNumber} since it can be called
   * on inputs that are valid JSON so is on the fast path.
   *
   * JS numbers differ from JSON numbers in several ways:<ul>
   * <li>They can have '+' as a sign prefix:  +1
   * <li>They allow a 0x... hexadecimal form: 0xA4
   * <li>They allow a 0... octal form:        012
   * <li>The integer part can be empty:       .5
   * <li>The fraction part can be empty:      1.
   * </ul>
   */
  private void normalizeNumber(int start, int end) {
    int pos = start;
    // Sign
    if (pos < end) {
      switch (jsonish.charAt(pos)) {
        case '+':
          elide(pos, pos + 1);
          ++pos;
          break;
        case '-':
          ++pos;
          break;
        default:
          break;
      }
    }

    // Integer part
    int intEnd = endOfDigitRun(pos, end);
    if (pos == intEnd) {  // No empty integer parts allowed in JSON.
      insert(pos, '0');
    } else if ('0' == jsonish.charAt(pos)) {
      boolean reencoded = false;
      long value = 0;
      if (intEnd - pos == 1 && intEnd < end
          && 'x' == (jsonish.charAt(intEnd) | 32)) {  // Recode hex.
        for (intEnd = intEnd + 1; intEnd < end; ++intEnd) {
          char ch = jsonish.charAt(intEnd);
          int digVal;
          if ('0' <= ch && ch <= '9') {
            digVal = ch - '0';
          } else {
            ch |= 32;
            if ('a' <= ch && ch <= 'f') {
              digVal = ch - ('a' - 10);
            } else {
              break;
            }
          }
          value = (value << 4) | digVal;
        }
        reencoded = true;
      } else if (intEnd - pos > 1) {  // Recode octal.
        for (int i = pos; i < intEnd; ++i) {
          int digVal = jsonish.charAt(i) - '0';
          if (digVal < 0) {
            break;
          }
          value = (value << 3) | digVal;
        }
        reencoded = true;
      }
      if (reencoded) {
        elide(pos, intEnd);
        if (value < 0) {
          // Underflow.
          // Avoid multiple signs.
          // Putting out the underflowed value is the least bad option.
          //
          // We could use BigInteger, but that won't help many clients,
          // and there is a valid use case for underflow: hex-encoded uint64s.
          //
          // First, consume any sign so that we don't put out strings like
          // --1
          int lastIndex = sanitizedJson.length() - 1;
          if (lastIndex >= 0) {
            char last = sanitizedJson.charAt(lastIndex);
            if (last == '-' || last == '+') {
              elide(lastIndex, lastIndex + 1);
              if (last == '-') {
                value = -value;
              }
            }
          }
        }
        sanitizedJson.append(value);
      }
    }
    pos = intEnd;

    // Optional fraction.
    if (pos < end && jsonish.charAt(pos) == '.') {
      ++pos;
      int fractionEnd = endOfDigitRun(pos, end);
      if (fractionEnd == pos) {
        insert(pos, '0');
      }
      // JS eval will discard digits after 24(?) but will not treat them as a
      // syntax error, and JSON allows arbitrary length fractions.
      pos = fractionEnd;
    }

    // Optional exponent.
    if (pos < end && 'e' == (jsonish.charAt(pos) | 32)) {
      ++pos;
      if (pos < end) {
        switch (jsonish.charAt(pos)) {
          // JSON allows explicit + in exponent but not for number as a whole.
          case '+': case '-': ++pos; break;
          default: break;
        }
      }
      // JSON allows leading zeros on exponent part.
      int expEnd = endOfDigitRun(pos, end);
      if (expEnd == pos) {
        insert(pos, '0');
      }
      pos = expEnd;
    }
    if (pos != end) {
      elide(pos, end);
    }
  }

  /**
   * Converts a run of characters that form a JS number to its canonical form
   * which happens to also be a valid JSON number.
   * @return true when the number could be canonicalized.
   */
  private boolean canonicalizeNumber(int start, int end) {
    elide(start, start);
    int sanStart = sanitizedJson.length();

    normalizeNumber(start, end);

    // Ensure that the number is on the output buffer.  Since this method is
    // only called when we are quoting a number that appears where a property
    // name is expected, we can force the sanitized form to contain it without
    // affecting the fast-track for already valid inputs.
    elide(end, end);
    int sanEnd = sanitizedJson.length();

    return canonicalizeNumber(sanitizedJson, sanStart, sanEnd);
  }

  /**
   * @param sanStart the start (inclusive) of the number on sanitizedJson.
   * @param sanEnd the end (exclusive) of the number on sanitizedJson.
   * @return true when the number could be canonicalized.
   */
  private static boolean canonicalizeNumber(
      StringBuilder sanitizedJson, int sanStart, int sanEnd) {
    // Now we perform several steps.
    // 1. Convert from scientific notation to regular or vice-versa based on
    //    normalized exponent.
    // 2. Remove trailing zeroes from the fraction and truncate it to 24 digits.
    // 3. Elide the fraction entirely if it is ".0".
    // 4. Convert any 'E' that separates the exponent to lower-case.
    // 5. Elide any minus sign on a zero value.
    // to convert the number to its canonical JS string form.

    // Figure out where the parts of the number start and end.
    int intStart, intEnd, fractionStart, fractionEnd, expStart, expEnd;
    intStart = sanStart + (sanitizedJson.charAt(sanStart) == '-' ? 1 : 0);
    for (intEnd = intStart; intEnd < sanEnd; ++intEnd) {
      char ch = sanitizedJson.charAt(intEnd);
      if (!('0' <= ch && ch <= '9')) { break; }
    }
    if (intEnd == sanEnd || '.' != sanitizedJson.charAt(intEnd)) {
      fractionStart = fractionEnd = intEnd;
    } else {
      fractionStart = intEnd + 1;
      for (fractionEnd = fractionStart; fractionEnd < sanEnd; ++fractionEnd) {
        char ch = sanitizedJson.charAt(fractionEnd);
        if (!('0' <= ch && ch <= '9')) { break; }
      }
    }
    if (fractionEnd == sanEnd) {
      expStart = expEnd = sanEnd;
    } else {
      assert 'e' == (sanitizedJson.charAt(fractionEnd) | 32);
      expStart = fractionEnd + 1;
      if (sanitizedJson.charAt(expStart) == '+') { ++expStart; }
      expEnd = sanEnd;
    }

    assert
         intStart      <= intEnd
      && intEnd        <= fractionStart
      && fractionStart <= fractionEnd
      && fractionEnd   <= expStart
      && expStart      <= expEnd;

    int exp;
    if (expEnd == expStart) {
      exp = 0;
    } else {
      try {
        exp = Integer.parseInt(sanitizedJson.toString().substring(expStart, expEnd), 10);
      } catch (NumberFormatException ex) {
        // The exponent is out of the range of representable ints.
        // JSON does not place limits on the range of representable numbers but
        // nor does it allow bare numbers as keys.
        return false;
      }
    }

    // Numbered Comments below come from the EcmaScript 5 language specification
    // section 9.8.1 : ToString Applied to the Number Type
    // http://es5.github.com/#x9.8.1

    // 5. let n, k, and s be integers such that k >= 1, 10k-1 <= s < 10k, the
    // Number value for s * 10n-k is m, and k is as small as possible.
    // Note that k is the number of digits in the decimal representation of s,
    // that s is not divisible by 10, and that the least significant digit of s
    // is not necessarily uniquely determined by these criteria.
    int n = exp;  // Exponent

    // s, the string of decimal digits in the representation of m are stored in
    // sanitizedJson.substring(intStart).
    // k, the number of digits in s is computed later.

    // Leave only the number representation on the output buffer after intStart.
    // This leaves any sign on the digit per
    // 3. If m is less than zero, return the String concatenation of the
    //    String "-" and ToString(-m).
    boolean sawDecimal = false;
    boolean zero = true;
    int digitOutPos = intStart;
    for (int i = intStart, nZeroesPending = 0; i < fractionEnd; ++i) {
      char ch = sanitizedJson.charAt(i);
      if (ch == '.') {
        sawDecimal = true;
        if (zero) { nZeroesPending = 0; }
        continue;
      }

      char digit = ch;
      if ((!zero || digit != '0') && !sawDecimal) { ++n; }

      if (digit == '0') {
        // Keep track of runs of zeros so that we can take them into account
        // if we later see a non-zero digit.
        ++nZeroesPending;
      } else {
        if (zero) {  // First non-zero digit.
          // Discard runs of zeroes at the front of the integer part, but
          // any after the decimal point factor into the exponent, n.
          if (sawDecimal) {
            n -= nZeroesPending;
          }
          nZeroesPending = 0;
        }
        zero = false;
        while (nZeroesPending != 0 || digit != 0) {
          char vdigit;
          if (nZeroesPending == 0) {
            vdigit = digit;
            digit = (char) 0;
          } else {
            vdigit = '0';
            --nZeroesPending;
          }

          // TODO: limit s to 21 digits?
          sanitizedJson.setCharAt(digitOutPos++, vdigit);
        }
      }
    }
    sanitizedJson.setLength(digitOutPos);
    // Number of digits in decimal representation of s.
    int k = digitOutPos - intStart;

    // Now we have computed n, k, and s as defined above.  Time to add decimal
    // points, exponents, and leading zeroes per the rest of the JS number
    // formatting specification.

    if (zero) {  // There are no non-zero decimal digits.
      // 2. If m is +0 or -0, return the String "0".
      sanitizedJson.setLength(sanStart);  // Elide any sign.
      sanitizedJson.append('0');
      return true;
    }

    // 6. If k <= n <= 21, return the String consisting of the k digits of the
    // decimal representation of s (in order, with no leading zeroes),
    // followed by n-k occurrences of the character '0'.
    if (k <= n && n <= 21) {
      for (int i = k; i < n; ++i) {
        sanitizedJson.append('0');
      }

    // 7. If 0 < n <= 21, return the String consisting of the most significant n
    // digits of the decimal representation of s, followed by a decimal point
    // '.', followed by the remaining k-n digits of the decimal representation
    // of s.
    } else if (0 < n && n <= 21) {
      sanitizedJson.insert(intStart + n, '.');

    // 8. If -6 < n <= 0, return the String consisting of the character '0',
    // followed by a decimal point '.', followed by -n occurrences of the
    // character '0', followed by the k digits of the decimal representation of
    // s.
    } else if (-6 < n && n <= 0) {
      sanitizedJson.insert(intStart, "0.000000".substring(0, 2 - n));

    } else {

      // 9. Otherwise, if k = 1, return the String consisting of the single
      // digit of s, followed by lowercase character 'e', followed by a plus
      // sign '+' or minus sign '-' according to whether n-1 is positive or
      // negative, followed by the decimal representation of the integer
      // abs(n-1) (with no leading zeros).
      if (k == 1) {
        // Sole digit already on sanitizedJson.

      // 10. Return the String consisting of the most significant digit of the
      // decimal representation of s, followed by a decimal point '.', followed
      // by the remaining k-1 digits of the decimal representation of s,
      // followed by the lowercase character 'e', followed by a plus sign '+'
      // or minus sign '-' according to whether n-1 is positive or negative,
      // followed by the decimal representation of the integer abs(n-1) (with
      // no leading zeros).
      } else {
        sanitizedJson.insert(intStart + 1, '.');
      }
      int nLess1 = n-1;
      sanitizedJson.append('e').append(nLess1 < 0 ? '-' : '+')
          .append(Math.abs(nLess1));
    }
    return true;
  }

  private static boolean regionMatches(String s1, int offset, String s2, int ooffset, int len) {
    for (int i=0; i<len; i++) {
        if (s1.charAt(offset+i) != s2.charAt(ooffset+i)) {
            return false;
        }
    }
    return true;
  }
  
  private boolean isKeyword(int start, int end) {
    int n = end - start;
    if (n == 5) {
      return regionMatches("false", 0, jsonish, start, n);
    } else if (n == 4) {
      return regionMatches("null", 0, jsonish, start, n)
          || regionMatches("true", 0, jsonish, start, n);
    }
    return false;
  }

  private boolean isOctAt(int i) {
    char ch = jsonish.charAt(i);
    return '0' <= ch && ch <= '7';
  }

  private boolean isHexAt(int i) {
    char ch = jsonish.charAt(i);
    if ('0' <= ch && ch <= '9') { return true; }
    ch |= 32;
    return 'a' <= ch && ch <= 'f';
  }

  private boolean isJsonSpecialChar(int i) {
    char ch = jsonish.charAt(i);
    if (ch <= ' ') { return true; }
    switch (ch) {
      case '"':
      case ',': case ':':
      case '[': case ']':
      case '{': case '}':
        return true;
      default:
        return false;
    }
  }

  private void appendHex(int n, int nDigits) {
    for (int i = 0, x = n; i < nDigits; ++i, x >>>= 4) {
      int dig = x & 0xf;
      sanitizedJson.append(dig + (dig < 10 ? '0' : (char) ('a' - 10)));
    }
  }

  /** Indicates that a comma was seen at the top level. */
  private static final class UnbracketedComma extends Exception {
    private static final long serialVersionUID = 783239978717247850L;
    // No members.  Used for nominal type.
  }

  private int endOfDigitRun(int start, int limit) {
    for (int end = start; end < limit; ++end) {
      char ch = jsonish.charAt(end);
      if (!('0' <= ch && ch <= '9')) { return end; }
    }
    return limit;
  }

 
  

  @Override
  public String toString() {
    return sanitizedJson != null ? sanitizedJson.toString() : jsonish;
  }

  private static final char[] HEX_DIGITS = new char[] {
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
  };
}