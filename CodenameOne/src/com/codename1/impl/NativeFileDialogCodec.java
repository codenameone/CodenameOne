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

package com.codename1.impl;

import com.codename1.io.filechooser.FileDialogOption;
import com.codename1.io.filechooser.FileDialogRequest;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.ui.Display;
import com.codename1.util.SuccessCallback;

import java.util.HashMap;
import java.util.Map;

/// The flat string form in which the desktop ports (Linux, Windows, macOS)
/// pass a file dialog request to their natives and read the answer back.
///
/// Natives take plain strings and string arrays, so a filter travels as
/// `"label\next1\next2..."` and an option as
/// `"type\nid\nlabel\ndefault\nchoice1\nchoice2..."`, where type is the
/// `FileDialogOption` type constant. The native answers `null` on cancel, else
/// `[selected filter index, one value per option, path...]`.
public final class NativeFileDialogCodec {
    private NativeFileDialogCodec() {
    }

    /// Encodes the request's filters.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request
    ///
    /// #### Returns
    ///
    /// one string per filter
    public static String[] encodeFilters(FileDialogRequest<?> request) {
        return encodeFilters(request, false);
    }

    /// Encodes the request's filters, optionally followed by each filter's
    /// uniform type identifiers. Only the macOS panels take those: their
    /// `allowedFileTypes` accepts extensions and UTIs alike, so a filter given
    /// only as `public.png` still restricts the panel. GTK and the Windows
    /// shell would read a UTI as an extension.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request
    ///
    /// - `includeUtTypes`: true to append the filters' UTIs after the extensions
    ///
    /// #### Returns
    ///
    /// one string per filter
    public static String[] encodeFilters(FileDialogRequest<?> request, boolean includeUtTypes) {
        FileTypeFilter[] filters = request.getFilters();
        String[] out = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            StringBuilder sb = new StringBuilder(clean(filters[i].getLabel()));
            for (String e : filters[i].getExtensions()) {
                sb.append('\n').append(clean(e));
            }
            if (includeUtTypes) {
                for (String u : filters[i].getUtTypes()) {
                    sb.append('\n').append(clean(u));
                }
            }
            out[i] = sb.toString();
        }
        return out;
    }

    /// Encodes the request's options.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request
    ///
    /// #### Returns
    ///
    /// one string per option
    public static String[] encodeOptions(FileDialogRequest<?> request) {
        FileDialogOption[] options = request.getOptions();
        String[] out = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            FileDialogOption o = options[i];
            StringBuilder sb = new StringBuilder();
            sb.append(o.getType()).append('\n').append(clean(o.getId())).append('\n')
                    .append(clean(o.getLabel())).append('\n').append(clean(o.getDefaultValue()));
            for (String c : o.getChoices()) {
                sb.append('\n').append(clean(c));
            }
            out[i] = sb.toString();
        }
        return out;
    }

    /// Turns a `FileSystemStorage` path into a native one.
    ///
    /// #### Parameters
    ///
    /// - `path`: a path, possibly a `file://` URL
    ///
    /// #### Returns
    ///
    /// the native path, or null
    public static String toNativePath(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("file://")) {
            return path.substring(7);
        }
        return path;
    }

    /// Decodes the native answer.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request being answered
    /// - `answer`: what the native returned
    ///
    /// #### Returns
    ///
    /// the result; cancelled when the answer is null or has no path
    public static FileDialogResult decode(FileDialogRequest<?> request, String[] answer) {
        FileDialogOption[] options = request.getOptions();
        int first = 1 + options.length;
        if (answer == null || answer.length <= first) {
            return FileDialogResult.cancelled(request);
        }
        int filter = -1;
        try {
            filter = Integer.parseInt(answer[0]);
        } catch (NumberFormatException ignored) {
            // the native always sends a number; -1 falls back to the request's choice
        }
        if (filter >= request.getFilters().length) {
            filter = -1;
        }
        Map<String, String> values = new HashMap<String, String>();
        for (int i = 0; i < options.length; i++) {
            if (answer[1 + i] != null) {
                values.put(options[i].getId(), answer[1 + i]);
            }
        }
        String[] paths = new String[answer.length - first];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = "file://" + toUrlPath(answer[first + i]);
        }
        if (request instanceof com.codename1.io.filechooser.SaveFileRequest && filter >= 0) {
            FileTypeFilter f = request.getFilters()[filter];
            paths[0] = FileDialogSupport.withExtension(paths[0], f);
        }
        return new FileDialogResult(request, paths, filter, values);
    }

    /// Converts a native path to the slash form a `file://` URL carries.
    /// Only a Windows path (`C:\dir` or a `\\server\share` UNC path) has
    /// backslash separators; on macOS and Linux a backslash is an ordinary
    /// file name character, and turning it into `/` would name another file.
    static String toUrlPath(String path) {
        boolean windows = (path.length() > 2 && path.charAt(1) == ':'
                && (path.charAt(2) == '\\' || path.charAt(2) == '/'))
                || path.startsWith("\\\\");
        return windows ? path.replace('\\', '/') : path;
    }

    /// Delivers a result through `Display.callSerially()`, so the callback runs
    /// on the EDT even when the dialog was requested from another thread.
    ///
    /// #### Parameters
    ///
    /// - `callback`: the dialog's callback
    ///
    /// - `result`: the result to deliver
    public static void deliver(final SuccessCallback<FileDialogResult> callback, final FileDialogResult result) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                callback.onSucess(result);
            }
        });
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\n', ' ');
    }
}
