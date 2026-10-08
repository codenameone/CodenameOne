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
package android.text;

/// Input type bits for text fields.
public interface InputType {
    int TYPE_MASK_CLASS = 0x0000000f;
    int TYPE_MASK_VARIATION = 0x00000ff0;
    int TYPE_MASK_FLAGS = 0x00fff000;
    int TYPE_NULL = 0x00000000;
    int TYPE_CLASS_TEXT = 0x00000001;
    int TYPE_TEXT_FLAG_CAP_CHARACTERS = 0x00001000;
    int TYPE_TEXT_FLAG_CAP_WORDS = 0x00002000;
    int TYPE_TEXT_FLAG_CAP_SENTENCES = 0x00004000;
    int TYPE_TEXT_FLAG_AUTO_CORRECT = 0x00008000;
    int TYPE_TEXT_FLAG_AUTO_COMPLETE = 0x00010000;
    int TYPE_TEXT_FLAG_MULTI_LINE = 0x00020000;
    int TYPE_TEXT_FLAG_IME_MULTI_LINE = 0x00040000;
    int TYPE_TEXT_FLAG_NO_SUGGESTIONS = 0x00080000;
    int TYPE_TEXT_VARIATION_NORMAL = 0x00000000;
    int TYPE_TEXT_VARIATION_URI = 0x00000010;
    int TYPE_TEXT_VARIATION_EMAIL_ADDRESS = 0x00000020;
    int TYPE_TEXT_VARIATION_EMAIL_SUBJECT = 0x00000030;
    int TYPE_TEXT_VARIATION_SHORT_MESSAGE = 0x00000040;
    int TYPE_TEXT_VARIATION_LONG_MESSAGE = 0x00000050;
    int TYPE_TEXT_VARIATION_PERSON_NAME = 0x00000060;
    int TYPE_TEXT_VARIATION_POSTAL_ADDRESS = 0x00000070;
    int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
    int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
    int TYPE_TEXT_VARIATION_WEB_EDIT_TEXT = 0x000000a0;
    int TYPE_TEXT_VARIATION_FILTER = 0x000000b0;
    int TYPE_TEXT_VARIATION_PHONETIC = 0x000000c0;
    int TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS = 0x000000d0;
    int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;
    int TYPE_CLASS_NUMBER = 0x00000002;
    int TYPE_NUMBER_FLAG_SIGNED = 0x00001000;
    int TYPE_NUMBER_FLAG_DECIMAL = 0x00002000;
    int TYPE_NUMBER_VARIATION_NORMAL = 0x00000000;
    int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
    int TYPE_CLASS_PHONE = 0x00000003;
    int TYPE_CLASS_DATETIME = 0x00000004;
    int TYPE_DATETIME_VARIATION_NORMAL = 0x00000000;
    int TYPE_DATETIME_VARIATION_DATE = 0x00000010;
    int TYPE_DATETIME_VARIATION_TIME = 0x00000020;
}
