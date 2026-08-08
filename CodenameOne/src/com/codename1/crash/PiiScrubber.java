/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crash;

/// Default PII scrubber for {@link CrashProtection} uploads. Designed to
/// be subclassed: override {@link #scrubMessage(String)} or
/// {@link #scrubFrame(String, String)} to extend the behaviour, then
/// register the subclass with {@link CrashProtection#setScrubber(PiiScrubber)}.
///
/// Default behaviour applied to exception message strings only:
///
/// 1. Emails partially redacted: the local part is truncated to its first
///    three characters followed by `***`, the domain is preserved.
///    Example: `johndoe@example.com` becomes `joh***@example.com`.
/// 2. Runs of six or more consecutive digits are replaced with `[num]`,
///    catching phone numbers, long IDs, etc.
/// 3. URLs are NOT scrubbed (they routinely carry useful debugging
///    context; if a particular app embeds tokens in URLs it can opt-in
///    to URL scrubbing by overriding this class).
///
/// Stack frames are not scrubbed by default. Class and method names do
/// not carry PII; subclasses that emit synthetic frames containing user
/// data may override {@link #scrubFrame(String, String)}.
public class PiiScrubber {

    /// Scrubs PII from a free-form message, typically an exception message.
    /// The default implementation applies email partial redaction and
    /// long-digit-run masking.
    ///
    /// #### Parameters
    ///
    /// - `message`: original message; may be `null`.
    ///
    /// #### Returns
    ///
    /// scrubbed message, or `null` if `message` is `null`.
    public String scrubMessage(String message) {
        if (message == null) {
            return null;
        }
        String result = scrubEmails(message);
        result = scrubDigitRuns(result);
        return result;
    }

    /// Scrubs PII from a single stack frame. Default implementation
    /// returns the original method name unchanged.
    ///
    /// #### Parameters
    ///
    /// - `className`: fully-qualified class name of the frame.
    /// - `methodName`: method name of the frame.
    ///
    /// #### Returns
    ///
    /// the (possibly modified) method name to upload.
    public String scrubFrame(String className, String methodName) {
        return methodName;
    }

    /// Scrubs a pre-rendered stack string. On the ParparVM ports the whole
    /// Java trace arrives as one string rather than structured frames, and on
    /// the JavaScript port it is the engine's `Error().stack`. A stricter
    /// application can override this to redact aggressively.
    ///
    /// The default scrubs emails everywhere, but applies long-digit-run masking
    /// only to non-frame lines. A frame/location line carries no PII -- it is
    /// class, method, file and line/column text -- and its numbers are exactly
    /// what the server needs to symbolicate. In particular a minified
    /// JavaScript bundle is often one line, so a `Error().stack` frame reads
    /// `app.js:1:123456` where the six-plus-digit column would otherwise be
    /// masked to `[num]`, destroying the location. Free-form lines (the leading
    /// `ExceptionClass: message` line and any non-frame text) are still scrubbed,
    /// since a message can carry a phone number or long id.
    ///
    /// #### Parameters
    ///
    /// - `rawStack`: the pre-rendered stack string; may be `null`.
    ///
    /// #### Returns
    ///
    /// the scrubbed stack string, or `null` if `rawStack` is `null`.
    ///
    /// A free-form (non-frame) line is routed through {@link #scrubMessage(String)}
    /// -- the overridable method -- so an app that redacts app-specific tokens there
    /// redacts them in `rawStack` too, not only in the separately-scrubbed message.
    /// A frame line is scrubbed too, but only up to its terminal `:line:column`
    /// coordinate: the coordinate is preserved for symbolication while the function
    /// identity and any URL/query before it (which can carry user data, e.g.
    /// `app.js?account=123456`) still get message scrubbing.
    public String scrubRawStack(String rawStack) {
        if (rawStack == null) {
            return null;
        }
        int len = rawStack.length();
        StringBuilder out = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            int nl = rawStack.indexOf('\n', i);
            int lineEnd = nl < 0 ? len : nl;
            String line = rawStack.substring(i, lineEnd);
            out.append(isFrameLine(line) ? scrubFrameLine(line) : scrubMessage(line));
            if (nl < 0) {
                break;
            }
            out.append('\n');
            i = nl + 1;
        }
        return out.toString();
    }

    /// Scrubs a recognized frame line while preserving its terminal `:line[:column]` coordinate.
    /// Everything before the coordinate -- the function identity and any URL/query -- goes through
    /// {@link #scrubMessage(String)}, so a URL query like `?account=123456` is masked; the coordinate
    /// tail is appended verbatim so symbolication still works. A frame with no numeric coordinate
    /// (`(Native Method)`) has nothing to protect and no PII to speak of, so it gets only the email pass.
    private String scrubFrameLine(String line) {
        int loc = trailingLocationStart(line);
        if (loc <= 0) {
            // A coordinate-free frame ((Native Method)/(Unknown Source)): there is no numeric coordinate
            // to protect, so run the whole line through message scrubbing. A real such frame's identity
            // is a dotted class.method with no long digit run, so it is unchanged; a message that merely
            // mimics the shape (at account123456failed (Native Method)) has its id masked.
            return scrubMessage(line);
        }
        return scrubMessage(line.substring(0, loc)) + line.substring(loc);
    }

    /// True for a stack-trace line whose numeric tokens are source coordinates,
    /// not PII: the JVM/ParparVM `at <class>.<method>(...)` / `at <class>.<method>:<line>`
    /// form (which every V8/Chrome JavaScript frame also uses), and the
    /// Firefox/Safari `fn@url:line:column` form.
    ///
    /// A frame requires the full grammar, not just the leading token and some digits: a
    /// message can wrap onto a line that begins with `at ` (`printStackTrace` puts
    /// `at account 123456 failed:789` on its own line) and its id must still be scrubbed.
    /// A real `at ...` frame is additionally always INDENTED -- `printStackTrace` emits a
    /// leading tab, V8 four spaces -- while a wrapped message sits at column 0; requiring
    /// the indentation rejects a continuation that otherwise matches the frame grammar
    /// exactly (`at account.failed(File.java:123456)`), whose numeric tail must stay
    /// scrubbable. The `at ` form must then be a single whitespace-free identity
    /// (`<class>.<method>`, a JS function ref, or a URL) followed by a real location -- a
    /// parenthesized `(File.java:42)`/`(url:line:col)`/`(Native Method)`/`(Unknown Source)`,
    /// or a bare trailing `:<line>` (ParparVM). The `@` form (Firefox/Safari, unindented by
    /// that engine) must carry an `@`, a URL/file source, and a terminal `:<line>:<column>`.
    private static boolean isFrameLine(String line) {
        String t = line.trim();
        if (t.startsWith("at ")) {
            return startsWithWhitespace(line) && atFrame(t.substring(3).trim());
        }
        return atSignFrame(t);
    }

    /// True when a line begins with the tab or space indentation that a real `at ...` frame carries.
    private static boolean startsWithWhitespace(String line) {
        return line.length() > 0 && (line.charAt(0) == ' ' || line.charAt(0) == '\t');
    }

    /// The body of a Firefox/Safari `fn@source:line:column` frame: a whitespace-free function
    /// identity (empty for an anonymous frame), an `@`, and a URL/file source before the trailing
    /// `:<line>:<column>`. The source must actually look like a URL or file -- contain a `/`
    /// (`scheme://host/path`) or a `.` (`file.ext`). Without that check a wrapped message such as
    /// `send status@host:1:123456` matches merely by containing an `@` and ending in two numeric
    /// groups, and its six-digit tail would be preserved verbatim as a fake column instead of being
    /// scrubbed. A bare source word like `host` fails the shape test, so the message stays scrubbed.
    private static boolean atSignFrame(String t) {
        int at = t.indexOf('@');
        if (at < 0 || !endsWithLineColumn(t)) {
            return false;
        }
        // A wrapped message almost always has a space before the '@' (`send status@...`); a real frame's
        // function ref is a single token. An empty identity is allowed (an anonymous `@url:1:2` frame).
        String ident = t.substring(0, at);
        if (ident.length() > 0 && !isFrameIdentity(ident)) {
            return false;
        }
        int loc = trailingLocationStart(t);
        if (loc <= at + 1) {
            return false;
        }
        String source = t.substring(at + 1, loc);
        return source.indexOf('/') >= 0 || source.indexOf('.') >= 0;
    }

    /// The body of an `at ...` line: a whitespace-free identity plus a real location. A message
    /// continuation such as `account 123456 failed:789` or `account 123456 failed (token:789)` has
    /// spaces in its identity, so it is not a frame and its digits stay subject to scrubbing.
    private static boolean atFrame(String rest) {
        if (rest.length() == 0) {
            return false;
        }
        if (rest.endsWith(")")) {
            int open = rest.lastIndexOf('(');
            if (open < 0) {
                return false;
            }
            String inside = rest.substring(open + 1, rest.length() - 1);
            // The parenthesized location is the discriminator here (JVM `(File.java:42)`, Chrome
            // `(url:line:col)`, or the `(Native Method)`/`(Unknown Source)` literals), so a plain
            // whitespace-free identity before it is enough -- a Chrome frame's identity can be a bare
            // function name with no dot.
            return isParenLocation(inside) && isFrameIdentity(rest.substring(0, open).trim());
        }
        int start = trailingLocationStart(rest);
        if (start <= 0) {
            return false;
        }
        // The bare `IDENT:<line>` form is ParparVM (`com.foo.Bar.baz:42`) or a JS anonymous URL frame;
        // its identity is always a dotted `<class>.<method>` or a URL. A message token such as
        // `account123456failed` is neither, so its digits stay subject to scrubbing.
        return isDottedOrUrlIdentity(rest.substring(0, start));
    }

    /// True when the content of an `at ...(<loc>)` is a real location: the `(Native Method)` /
    /// `(Unknown Source)` literals, or a `file.ext:line` / `scheme://host/path:line:col` whose part
    /// before the trailing `:<digits>` names a file (has an extension dot) or a URL (has a `/`). A
    /// bare word like `attempt:123456` is not a location, so its digits stay subject to scrubbing.
    private static boolean isParenLocation(String inside) {
        if ("Native Method".equals(inside) || "Unknown Source".equals(inside)) {
            return true;
        }
        if (!endsWithColonNumber(inside)) {
            return false;
        }
        int loc = trailingLocationStart(inside);
        String head = loc > 0 ? inside.substring(0, loc) : "";
        return head.indexOf('.') >= 0 || head.indexOf('/') >= 0;
    }

    /// A frame's identity is a single token: non-empty and free of whitespace. A free-form message
    /// continuation (`account 123456 failed`) has spaces, so it is rejected.
    private static boolean isFrameIdentity(String id) {
        if (id.length() == 0) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == ' ' || c == '\t') {
                return false;
            }
        }
        return true;
    }

    /// A stricter identity for the bare `IDENT:<line>` form: a whitespace-free token that is a dotted
    /// `<class>.<method>` or a URL (has a `/`). A single word like `account123456failed` is rejected,
    /// so a wrapped message that happens to start with `at ` and end in a colon-number is still scrubbed.
    private static boolean isDottedOrUrlIdentity(String id) {
        return isFrameIdentity(id) && (id.indexOf('.') >= 0 || id.indexOf('/') >= 0);
    }

    /// Index at which a trailing `:<line>` (optionally `:<line>:<column>`) location begins, or -1
    /// when the string does not end in one. A single trailing `)` is allowed. Consumes AT MOST two
    /// numeric groups -- a real location is `:line` or `:line:column` -- so colon-delimited data before
    /// the coordinate (a URL like `host/account:123456:1:42`) stays in the scrubbable head rather than
    /// being preserved as if it were part of the coordinate.
    private static int trailingLocationStart(String t) {
        int i = t.length() - 1;
        if (i >= 0 && t.charAt(i) == ')') {
            i--;
        }
        int start = -1;
        for (int groups = 0; groups < 2; groups++) {
            int j = i;
            int digits = 0;
            while (j >= 0 && t.charAt(j) >= '0' && t.charAt(j) <= '9') {
                j--;
                digits++;
            }
            if (digits > 0 && j >= 0 && t.charAt(j) == ':') {
                start = j;
                i = j - 1;
            } else {
                break;
            }
        }
        return start;
    }

    /// True when `t` ends with a `:<digits>` run (a trailing `)` allowed): the
    /// ParparVM frame coordinate `at <fqcn>.<method>:<line>`, and also the tail of a
    /// `:<line>:<column>`. A message ending in text or a space-separated number does
    /// not match, so its digits stay subject to scrubbing.
    private static boolean endsWithColonNumber(String t) {
        int end = t.length();
        if (end > 0 && t.charAt(end - 1) == ')') {
            end--;
        }
        int i = end - 1;
        int digits = 0;
        while (i >= 0 && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i--;
            digits++;
        }
        return digits > 0 && i >= 0 && t.charAt(i) == ':';
    }

    /// True when `t` ends with a `:<line>:<column>` location: two colon-separated
    /// runs of digits, allowing a single trailing `)` (a wrapped frame). This is
    /// the JavaScript engine frame location; a free-form message ending in text
    /// (or a lone number) does not match, so its digits stay subject to scrubbing.
    private static boolean endsWithLineColumn(String t) {
        int end = t.length();
        if (end > 0 && t.charAt(end - 1) == ')') {
            end--;
        }
        int i = end - 1;
        int col = 0;
        while (i >= 0 && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i--;
            col++;
        }
        if (col == 0 || i < 0 || t.charAt(i) != ':') {
            return false;
        }
        i--;
        int lineDigits = 0;
        while (i >= 0 && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i--;
            lineDigits++;
        }
        return lineDigits > 0 && i >= 0 && t.charAt(i) == ':';
    }

    /// Replaces all occurrences of an email-like substring with the form
    /// `<first-three>***@<domain>`. Local parts shorter than three
    /// characters are not padded; the original prefix is preserved and
    /// followed by `***`. The domain (including TLD) is preserved verbatim.
    ///
    /// This implementation is character-driven rather than regex-based
    /// to stay compatible with the Java 5 source level enforced by the
    /// core framework module.
    protected static String scrubEmails(String s) {
        if (s == null || s.indexOf('@') < 0) {
            return s;
        }
        int len = s.length();
        StringBuilder out = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '@') {
                int localStart = i;
                while (localStart > 0 && isEmailLocalChar(s.charAt(localStart - 1))) {
                    localStart--;
                }
                int domainEnd = i + 1;
                while (domainEnd < len && isEmailDomainChar(s.charAt(domainEnd))) {
                    domainEnd++;
                }
                String local = s.substring(localStart, i);
                String domain = s.substring(i + 1, domainEnd);
                if (local.length() > 0 && isValidDomain(domain)) {
                    int alreadyWritten = i - localStart;
                    int outBaseLen = out.length() - alreadyWritten;
                    out.setLength(outBaseLen);
                    int keep = local.length() < 3 ? local.length() : 3;
                    out.append(local, 0, keep);
                    out.append("***@");
                    out.append(domain);
                    i = domainEnd;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isEmailLocalChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        return c == '.' || c == '_' || c == '+' || c == '-';
    }

    private static boolean isEmailDomainChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        return c == '.' || c == '-';
    }

    private static boolean isValidDomain(String domain) {
        int dot = domain.indexOf('.');
        if (dot < 1 || dot == domain.length() - 1) {
            return false;
        }
        int afterDot = domain.length() - dot - 1;
        return afterDot >= 2;
    }

    /// Replaces every run of six or more consecutive ASCII digits with
    /// the literal token `[num]`.
    protected static String scrubDigitRuns(String s) {
        if (s == null) {
            return null;
        }
        int len = s.length();
        StringBuilder out = null;
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                int j = i + 1;
                while (j < len) {
                    char d = s.charAt(j);
                    if (d < '0' || d > '9') {
                        break;
                    }
                    j++;
                }
                if (j - i >= 6) {
                    if (out == null) {
                        out = new StringBuilder(len);
                        out.append(s, 0, i);
                    }
                    out.append("[num]");
                    i = j;
                    continue;
                }
                if (out != null) {
                    out.append(s, i, j);
                }
                i = j;
                continue;
            }
            if (out != null) {
                out.append(c);
            }
            i++;
        }
        return out == null ? s : out.toString();
    }
}
