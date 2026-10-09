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
package android.view.inputmethod;

/// IME option and input type constants.
public class EditorInfo {
    public static final int IME_NULL = 0x00000000;
    public static final int IME_ACTION_UNSPECIFIED = 0x00000000;
    public static final int IME_ACTION_NONE = 0x00000001;
    public static final int IME_ACTION_GO = 0x00000002;
    public static final int IME_ACTION_SEARCH = 0x00000003;
    public static final int IME_ACTION_SEND = 0x00000004;
    public static final int IME_ACTION_NEXT = 0x00000005;
    public static final int IME_ACTION_DONE = 0x00000006;
    public static final int IME_ACTION_PREVIOUS = 0x00000007;
    public static final int IME_MASK_ACTION = 0x000000ff;
    public static final int IME_FLAG_NO_EXTRACT_UI = 0x10000000;
    public static final int IME_FLAG_NO_FULLSCREEN = 0x02000000;
    public static final int IME_FLAG_NO_ENTER_ACTION = 0x40000000;

    public int inputType;
    public int imeOptions;
    public String hintText;
}
