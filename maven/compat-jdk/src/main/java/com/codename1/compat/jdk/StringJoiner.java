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
package com.codename1.compat.jdk;

/// `java.util.StringJoiner` for the Codename One runtime: builds a sequence
/// of strings separated by a delimiter, optionally between a prefix and a
/// suffix.
public final class StringJoiner {

    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private StringBuilder value;
    private String emptyValue;

    public StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        if (prefix == null) {
            throw new NullPointerException("The prefix must not be null");
        }
        if (delimiter == null) {
            throw new NullPointerException("The delimiter must not be null");
        }
        if (suffix == null) {
            throw new NullPointerException("The suffix must not be null");
        }
        this.prefix = prefix.toString();
        this.delimiter = delimiter.toString();
        this.suffix = suffix.toString();
        this.emptyValue = this.prefix + this.suffix;
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue) {
        if (emptyValue == null) {
            throw new NullPointerException("The empty value must not be null");
        }
        this.emptyValue = emptyValue.toString();
        return this;
    }

    @Override
    public String toString() {
        if (value == null) {
            return emptyValue;
        }
        if (suffix.length() == 0) {
            return value.toString();
        }
        return value.toString() + suffix;
    }

    public StringJoiner add(CharSequence newElement) {
        prepareBuilder().append(String.valueOf(newElement));
        return this;
    }

    public StringJoiner merge(StringJoiner other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (other.value != null) {
            // Taken before the builder is prepared, so that merging a joiner
            // into itself appends its content once.
            String content = other.value.toString().substring(other.prefix.length());
            prepareBuilder().append(content);
        }
        return this;
    }

    private StringBuilder prepareBuilder() {
        if (value != null) {
            value.append(delimiter);
        } else {
            value = new StringBuilder().append(prefix);
        }
        return value;
    }

    public int length() {
        return value != null ? value.length() + suffix.length() : emptyValue.length();
    }
}
