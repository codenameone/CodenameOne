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
package com.codename1.androidcompat.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.PatternMatcher;
import android.util.AttributeSet;
import android.view.View;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The manifest's activity filters used to be flattened to their actions, so
/// an `ACTION_VIEW` filter for a custom scheme captured every browser URL the
/// application opened. Filters now keep their data, types and categories and
/// match the way Android's do.
public class IntentFilterMatchTest {

    public static final class DeepLink extends Activity {
    }

    public static final class Share extends Activity {
    }

    public static final class Docs extends Activity {
    }

    public static final class NoDefault extends Activity {
    }

    /// What the build generates for a manifest declaring these filters.
    private static final class App extends AndroidApp {
        App() {
            super(null, "com.x");
            activity(DeepLink.class, DeepLink.class.getName(), 0, 0, null, null, null, false);
            activity(Share.class, Share.class.getName(), 0, 0, null, null, null, false);
            activity(Docs.class, Docs.class.getName(), 0, 0, null, null, null, false);
            activity(NoDefault.class, NoDefault.class.getName(), 0, 0, null, null, null, false);
            {
                IntentFilter f = intentFilter(DeepLink.class);
                f.addAction(Intent.ACTION_VIEW);
                f.addCategory(Intent.CATEGORY_DEFAULT);
                f.addCategory(Intent.CATEGORY_BROWSABLE);
                f.addDataScheme("myapp");
                f.addDataAuthority("*.example.com", null);
                f.addDataPath("/items", PatternMatcher.PATTERN_PREFIX);
            }
            {
                IntentFilter f = intentFilter(Share.class);
                f.addAction(Intent.ACTION_SEND);
                f.addCategory(Intent.CATEGORY_DEFAULT);
                f.addDataType("image/*");
            }
            {
                IntentFilter f = intentFilter(Docs.class);
                f.addAction(Intent.ACTION_VIEW);
                f.addCategory(Intent.CATEGORY_DEFAULT);
                f.addDataScheme("file");
                f.addDataAuthority("*", null);
                f.addDataPath(".*\\.pdf", PatternMatcher.PATTERN_SIMPLE_GLOB);
            }
            {
                IntentFilter f = intentFilter(NoDefault.class);
                f.addAction("com.x.PRIVATE");
            }
        }

        @Override
        public String[] viewTags() {
            return new String[0];
        }

        @Override
        public View createView(int index, Context context, AttributeSet attrs) {
            return null;
        }

        @Override
        public Activity createActivity(Class<?> type) {
            return null;
        }

        @Override
        public Application createApplication() {
            return null;
        }
    }

    private static Class<?> target(Intent i) {
        AndroidApp.ActivityInfo a = new App().activityForIntent(i);
        return a == null ? null : a.type;
    }

    @Test
    public void aCustomSchemeFilterDoesNotCaptureForeignUrls() {
        assertNull("a browser URL was routed into the deep-link activity",
                target(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.codenameone.com/"))));
        assertNull(target(new Intent(Intent.ACTION_VIEW, Uri.parse("tel:5551234"))));
        assertNull("the host must match", target(new Intent(Intent.ACTION_VIEW, Uri.parse("myapp://other.org/items/1"))));
        assertNull("the path must match", target(new Intent(Intent.ACTION_VIEW, Uri.parse("myapp://a.example.com/x"))));
        assertNull("data constraints need data", target(new Intent(Intent.ACTION_VIEW)));
        assertSame(DeepLink.class, target(new Intent(Intent.ACTION_VIEW, Uri.parse("myapp://a.example.com/items/7"))));
    }

    @Test
    public void typesAndCategoriesMustMatch() {
        Intent text = new Intent(Intent.ACTION_SEND);
        text.setType("text/plain");
        assertNull("a share of text reached an image-only filter", target(text));
        Intent image = new Intent(Intent.ACTION_SEND);
        image.setType("image/png");
        assertSame(Share.class, target(image));
        image.addCategory("com.x.UNLISTED");
        assertNull("an intent category the filter does not list", target(image));
        assertNull("a filter without DEFAULT takes no implicit start", target(new Intent("com.x.PRIVATE")));
    }

    @Test
    public void globPathsMatchAsOnAndroid() {
        assertSame(Docs.class, target(new Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/report.pdf"))));
        assertNull(target(new Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/report.txt"))));
        assertTrue(new PatternMatcher("a*b", PatternMatcher.PATTERN_SIMPLE_GLOB).match("aaab"));
        assertTrue(new PatternMatcher("x.y", PatternMatcher.PATTERN_SIMPLE_GLOB).match("xzy"));
        assertFalse(new PatternMatcher("x.y", PatternMatcher.PATTERN_SIMPLE_GLOB).match("xy"));
        assertFalse("an escaped star is literal", new PatternMatcher("a\\*", PatternMatcher.PATTERN_SIMPLE_GLOB)
                .match("aa"));
        assertFalse("advanced globs are not implemented and never match",
                new PatternMatcher("/.*", PatternMatcher.PATTERN_ADVANCED_GLOB).match("/a"));
    }

    @Test
    public void matchReportsAndroidsCodes() {
        IntentFilter f = new IntentFilter(Intent.ACTION_VIEW);
        f.addDataScheme("myapp");
        assertEquals(IntentFilter.NO_MATCH_ACTION, f.match(Intent.ACTION_SEND, null, "myapp", Uri.parse("myapp:x"),
                null, null));
        assertEquals(IntentFilter.NO_MATCH_DATA, f.match(Intent.ACTION_VIEW, null, "https",
                Uri.parse("https://a"), null, null));
        assertEquals(IntentFilter.MATCH_CATEGORY_SCHEME + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
                f.match(Intent.ACTION_VIEW, null, "myapp", Uri.parse("myapp:x"), null, null));
        assertEquals(IntentFilter.NO_MATCH_TYPE, f.match(Intent.ACTION_VIEW, "text/plain", "myapp",
                Uri.parse("myapp:x"), null, null));
    }

    @Test
    public void anSspMissCanStillMatchTheAuthorityAndPath() {
        IntentFilter f = new IntentFilter(Intent.ACTION_VIEW);
        f.addDataScheme("myapp");
        f.addDataSchemeSpecificPart("item:42", PatternMatcher.PATTERN_LITERAL);
        f.addDataAuthority("example.com", null);
        f.addDataPath("/items", PatternMatcher.PATTERN_PREFIX);
        assertEquals(IntentFilter.MATCH_CATEGORY_PATH + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
                f.matchData(null, "myapp", Uri.parse("myapp://example.com/items/42")));
        assertEquals(IntentFilter.MATCH_CATEGORY_SCHEME_SPECIFIC_PART + IntentFilter.MATCH_ADJUSTMENT_NORMAL,
                f.matchData(null, "myapp", Uri.parse("myapp:item:42")));
        assertEquals(IntentFilter.NO_MATCH_DATA,
                f.matchData(null, "myapp", Uri.parse("myapp://other.example/items/42")));
    }
}
