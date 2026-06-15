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
        if (c >= 'a' && c <= 'z') return true;
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= '0' && c <= '9') return true;
        return c == '.' || c == '_' || c == '+' || c == '-';
    }

    private static boolean isEmailDomainChar(char c) {
        if (c >= 'a' && c <= 'z') return true;
        if (c >= 'A' && c <= 'Z') return true;
        if (c >= '0' && c <= '9') return true;
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
                    if (d < '0' || d > '9') break;
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
