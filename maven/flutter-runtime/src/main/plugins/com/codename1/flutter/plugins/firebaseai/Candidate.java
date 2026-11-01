/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebaseai;

/** firebase_ai's {@code Candidate}: one answer the model proposed. */
public class Candidate {
    private final Content content;
    private final FinishReason finishReason;
    private final String finishMessage;

    Candidate(Content content, FinishReason finishReason, String finishMessage) {
        this.content = content;
        this.finishReason = finishReason;
        this.finishMessage = finishMessage;
    }

    public Content content() {
        return content;
    }

    public FinishReason finishReason() {
        return finishReason;
    }

    public String finishMessage() {
        return finishMessage;
    }

    /** The text parts of the content joined, or null when there are none. */
    public String text() {
        StringBuilder sb = null;
        for (Part p : content.parts()) {
            if (p instanceof TextPart) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(((TextPart) p).text());
            }
        }
        return sb == null ? null : sb.toString();
    }
}
