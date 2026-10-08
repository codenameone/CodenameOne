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
package android.content;

import android.net.Uri;
import android.os.PatternMatcher;

import java.util.ArrayList;
import java.util.Set;

/// The actions, categories and data an activity or receiver accepts.
///
/// [#match(String, String, String, Uri, Set, String)] applies Android's
/// rules: the action must be listed; every category of the intent must be
/// listed; a filter that names schemes only matches data with one of them
/// (and, when it names hosts and paths, with a matching host and path); a
/// filter that names no MIME type only matches an intent that has none. The
/// manifest's activity filters are built from these calls at build time, so
/// an `ACTION_VIEW` filter restricted to a custom scheme never captures a
/// browser intent.
public class IntentFilter {

    public static final int MATCH_CATEGORY_MASK = 0xfff0000;
    public static final int MATCH_ADJUSTMENT_MASK = 0x000ffff;
    public static final int MATCH_ADJUSTMENT_NORMAL = 0x8000;
    public static final int MATCH_CATEGORY_EMPTY = 0x0100000;
    public static final int MATCH_CATEGORY_SCHEME = 0x0200000;
    public static final int MATCH_CATEGORY_HOST = 0x0300000;
    public static final int MATCH_CATEGORY_PORT = 0x0400000;
    public static final int MATCH_CATEGORY_PATH = 0x0500000;
    public static final int MATCH_CATEGORY_SCHEME_SPECIFIC_PART = 0x0580000;
    public static final int MATCH_CATEGORY_TYPE = 0x0600000;
    public static final int NO_MATCH_TYPE = -1;
    public static final int NO_MATCH_DATA = -2;
    public static final int NO_MATCH_ACTION = -3;
    public static final int NO_MATCH_CATEGORY = -4;

    private final ArrayList<String> actions = new ArrayList<String>();
    private final ArrayList<String> categories = new ArrayList<String>();
    private ArrayList<String> schemes;
    private ArrayList<PatternMatcher> schemeSpecificParts;
    /// Host and port pairs: `{host, port}` with a null port for any port.
    private ArrayList<String[]> authorities;
    private ArrayList<PatternMatcher> paths;
    /// Full types, and the base of each `base/*` partial type.
    private ArrayList<String> types;
    private boolean hasPartialTypes;

    public IntentFilter() {
    }

    public IntentFilter(String action) {
        addAction(action);
    }

    public IntentFilter(String action, String dataType) {
        addAction(action);
        addDataType(dataType);
    }

    public final void addAction(String action) {
        if (!actions.contains(action)) {
            actions.add(action);
        }
    }

    public final void addCategory(String category) {
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    public final void addDataScheme(String scheme) {
        if (schemes == null) {
            schemes = new ArrayList<String>();
        }
        if (!schemes.contains(scheme)) {
            schemes.add(scheme);
        }
    }

    public final void addDataSchemeSpecificPart(String ssp, int type) {
        if (schemeSpecificParts == null) {
            schemeSpecificParts = new ArrayList<PatternMatcher>();
        }
        schemeSpecificParts.add(new PatternMatcher(ssp, type));
    }

    /// A host (a leading `*` matches any prefix, so `*.example.com` takes
    /// every subdomain) and a port, or null for any port.
    public final void addDataAuthority(String host, String port) {
        if (authorities == null) {
            authorities = new ArrayList<String[]>();
        }
        authorities.add(new String[] {host, port});
    }

    public final void addDataPath(String path, int type) {
        if (paths == null) {
            paths = new ArrayList<PatternMatcher>();
        }
        paths.add(new PatternMatcher(path, type));
    }

    /// A MIME type, `base/*`, or `*` / `*/*` for any type. Android throws
    /// `MalformedMimeTypeException` for a type without a slash; here such a
    /// type matches only itself.
    public final void addDataType(String type) {
        if (types == null) {
            types = new ArrayList<String>();
        }
        int slash = type.indexOf('/');
        String t = type;
        if (slash > 0 && type.length() == slash + 2 && type.charAt(slash + 1) == '*') {
            t = type.substring(0, slash);
            hasPartialTypes = true;
        }
        if (!types.contains(t)) {
            types.add(t);
        }
    }

    public final void setPriority(int priority) {
    }

    public final int countActions() {
        return actions.size();
    }

    public final String getAction(int index) {
        return actions.get(index);
    }

    public final boolean hasAction(String action) {
        return actions.contains(action);
    }

    public final boolean matchAction(String action) {
        return action != null && actions.contains(action);
    }

    public final int countCategories() {
        return categories.size();
    }

    public final String getCategory(int index) {
        return categories.get(index);
    }

    public final boolean hasCategory(String category) {
        return categories.contains(category);
    }

    public final int countDataSchemes() {
        return schemes == null ? 0 : schemes.size();
    }

    public final boolean hasDataScheme(String scheme) {
        return schemes != null && schemes.contains(scheme);
    }

    public final int countDataTypes() {
        return types == null ? 0 : types.size();
    }

    /// The first intent category the filter does not list, or null when it
    /// lists them all.
    public final String matchCategories(Set<String> intentCategories) {
        if (intentCategories == null) {
            return null;
        }
        for (String c : intentCategories) {
            if (!categories.contains(c)) {
                return c;
            }
        }
        return null;
    }

    /// Matches an intent's action, type, data and categories; a negative
    /// `NO_MATCH_*` code, or a `MATCH_CATEGORY_*` quality (higher is more
    /// specific) plus `MATCH_ADJUSTMENT_NORMAL`. As on Android, a null action
    /// skips the action test, even against a filter that lists none; the
    /// runtime's own resolvers never start or deliver an actionless intent.
    public final int match(String action, String type, String scheme, Uri data, Set<String> intentCategories,
                           String logTag) {
        if (action != null && !actions.contains(action)) {
            return NO_MATCH_ACTION;
        }
        int dataMatch = matchData(type, scheme, data);
        if (dataMatch < 0) {
            return dataMatch;
        }
        if (matchCategories(intentCategories) != null) {
            return NO_MATCH_CATEGORY;
        }
        return dataMatch;
    }

    public final int match(ContentResolver resolver, Intent intent, boolean resolve, String logTag) {
        return match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(),
                intent.getCategories(), logTag);
    }

    /// Android's data test, rule for rule.
    public final int matchData(String type, String scheme, Uri data) {
        if (types == null && schemes == null) {
            return type == null && data == null ? MATCH_CATEGORY_EMPTY + MATCH_ADJUSTMENT_NORMAL : NO_MATCH_DATA;
        }
        int match = MATCH_CATEGORY_EMPTY;
        if (schemes != null) {
            if (!schemes.contains(scheme != null ? scheme : "")) {
                return NO_MATCH_DATA;
            }
            match = MATCH_CATEGORY_SCHEME;
            if (schemeSpecificParts != null && data != null) {
                if (anyMatches(schemeSpecificParts, data.getSchemeSpecificPart())) {
                    match = MATCH_CATEGORY_SCHEME_SPECIFIC_PART;
                } else if (authorities == null) {
                    return NO_MATCH_DATA;
                }
            }
            if (match != MATCH_CATEGORY_SCHEME_SPECIFIC_PART && authorities != null) {
                int auth = matchAuthority(data);
                if (auth < 0) {
                    return NO_MATCH_DATA;
                }
                if (paths == null) {
                    match = auth;
                } else if (anyMatches(paths, data.getPath())) {
                    match = MATCH_CATEGORY_PATH;
                } else {
                    return NO_MATCH_DATA;
                }
            }
        } else if (scheme != null && !"".equals(scheme) && !"content".equals(scheme) && !"file".equals(scheme)) {
            // A filter that names only types takes data with no URI, or a
            // content: or file: URI whose type it names.
            return NO_MATCH_DATA;
        }
        if (types != null) {
            if (!findMimeType(type)) {
                return NO_MATCH_TYPE;
            }
            match = MATCH_CATEGORY_TYPE;
        } else if (type != null) {
            return NO_MATCH_TYPE;
        }
        return match + MATCH_ADJUSTMENT_NORMAL;
    }

    private int matchAuthority(Uri data) {
        if (data == null) {
            return NO_MATCH_DATA;
        }
        String host = data.getHost();
        if (host == null) {
            return NO_MATCH_DATA;
        }
        for (String[] a : authorities) {
            String want = a[0];
            String h = host;
            if (want.length() > 0 && want.charAt(0) == '*') {
                want = want.substring(1);
                if (h.length() < want.length()) {
                    continue;
                }
                h = h.substring(h.length() - want.length());
            }
            if (!h.equalsIgnoreCase(want)) {
                continue;
            }
            if (a[1] == null) {
                return MATCH_CATEGORY_HOST;
            }
            if (portOf(a[1]) == data.getPort()) {
                return MATCH_CATEGORY_PORT;
            }
        }
        return NO_MATCH_DATA;
    }

    private static int portOf(String port) {
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            // A port that is not a number matches no URI.
            return -2;
        }
    }

    private static boolean anyMatches(ArrayList<PatternMatcher> patterns, String value) {
        for (PatternMatcher p : patterns) {
            if (p.match(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean findMimeType(String type) {
        if (type == null) {
            return false;
        }
        if (types.contains(type)) {
            return true;
        }
        if (type.equals("*/*")) {
            return !types.isEmpty();
        }
        if (hasPartialTypes && types.contains("*")) {
            return true;
        }
        int slash = type.indexOf('/');
        if (slash > 0) {
            if (hasPartialTypes && types.contains(type.substring(0, slash))) {
                return true;
            }
            if (type.length() == slash + 2 && type.charAt(slash + 1) == '*') {
                // "image/*" in the intent takes any image type the filter names.
                for (String t : types) {
                    if (type.regionMatches(0, t, 0, slash + 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
