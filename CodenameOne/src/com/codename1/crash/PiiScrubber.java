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
    /// EVERY line is routed through {@link #scrubMessage(String)} -- the overridable method -- so an app
    /// that redacts app-specific tokens there redacts them in `rawStack` too. No line is treated as a
    /// "frame" whose coordinate is preserved: `printStackTrace` writes the exception MESSAGE verbatim, and
    /// a message can contain an embedded, indented, frame-shaped line (e.g. code that folds another stack
    /// trace into a message), which is indistinguishable from a real frame by any shape or indentation
    /// check. Preserving a "coordinate" from such a line would let a crafted `:line:column` tail bypass
    /// digit masking. So the raw stack is scrubbed uniformly; `scrubMessage` masks only 6+ digit runs, so
    /// ordinary short line numbers survive and stay readable, while a large minified-bundle column (or a
    /// long id planted as a fake column) is masked. Precise coordinates for symbolication come from the
    /// structured frames, which are real `StackTraceElement`s, not parsed text. The app's
    /// {@link #scrubFrame(String, String)} override is still applied to a `at <class>.<method>` line so a
    /// synthetic method name redacted from the structured frames does not resurface here.
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
            out.append(scrubMessage(applyFrameOverride(line)));
            if (nl < 0) {
                break;
            }
            out.append('\n');
            i = nl + 1;
        }
        return out.toString();
    }

    /// Applies {@link #scrubFrame(String, String)} to the method name of a JVM/ParparVM
    /// `at <class>.<method>(<loc>)` / `at <class>.<method>:<line>` frame, so an app that redacts a
    /// synthetic method name in its structured frames redacts it in the raw stack too. Only these
    /// dotted-identity forms carry a {@code class.method} the override addresses; other forms (a bare
    /// V8 function, a URL frame) are returned unchanged. The default {@code scrubFrame} returns the
    /// method unchanged, so a build that does not override it sees no difference.
    private String applyFrameOverride(String line) {
        int at = line.indexOf("at ");
        if (at < 0 || line.substring(0, at).trim().length() != 0) {
            return line;
        }
        String rest = line.substring(at + 3).trim();
        int paren = rest.indexOf('(');
        int idEnd;
        if (paren >= 0) {
            idEnd = paren;
        } else {
            int locStart = trailingLocationStart(rest);
            idEnd = locStart > 0 ? locStart : rest.length();
        }
        String identity = rest.substring(0, idEnd).trim();
        int lastDot = identity.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == identity.length() - 1 || !isFrameIdentity(identity)) {
            return line;
        }
        String cls = identity.substring(0, lastDot);
        String method = identity.substring(lastDot + 1);
        String scrubbed = scrubFrame(cls, method);
        if (scrubbed == null) {
            // The app removed the method name (its structured frame renders an empty method,
            // CrashReportPayload.Frame); render it empty here too rather than restoring the original.
            scrubbed = "";
        } else if (scrubbed.equals(method)) {
            return line;
        }
        return line.substring(0, at + 3) + cls + "." + scrubbed + rest.substring(idEnd);
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
