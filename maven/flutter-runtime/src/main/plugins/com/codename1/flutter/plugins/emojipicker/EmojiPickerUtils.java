/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.emojipicker;

import dart.async.Future;
import dart.core.DartList;

/** emoji_picker_flutter's EmojiPickerUtils. Search matches the keyword anywhere in the name, ignoring case. */
public class EmojiPickerUtils {
    public EmojiPickerUtils() {
    }

    public Future<DartList<Emoji>> getRecentEmojis() {
        return Future.value(EmojiData.recents());
    }

    public Future<DartList<Emoji>> searchEmoji(String keyword, DartList<CategoryEmoji> data) {
        DartList<Emoji> out = new DartList<Emoji>();
        String k = keyword == null ? "" : keyword;
        for (CategoryEmoji c : data == null ? EmojiData.all() : data) {
            for (Emoji e : c.emoji()) {
                if (contains(e.name(), k)) {
                    out.add(e);
                }
            }
        }
        return Future.value(out);
    }

    private static boolean contains(String hay, String needle) {
        for (int i = 0; i + needle.length() <= hay.length(); i++) {
            if (hay.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
    }

    public Future<Object> clearRecentEmojis() {
        EmojiData.clearRecents();
        return Future.value(null);
    }
}
