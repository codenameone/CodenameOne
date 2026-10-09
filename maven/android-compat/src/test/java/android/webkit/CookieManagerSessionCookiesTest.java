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
package android.webkit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/// Removing session cookies keeps the cookies set with `Expires` or
/// `Max-Age`; it used to clear the whole store.
public class CookieManagerSessionCookiesTest {

    @Before
    @After
    public void clear() {
        CookieManager.getInstance().removeAllCookies(null);
    }

    private static ValueCallback<Boolean> recorder(final ArrayList<Boolean> answers) {
        return new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
                answers.add(value);
            }
        };
    }

    @Test
    public void persistentCookiesSurvive() {
        CookieManager cm = CookieManager.getInstance();
        String url = "https://example.com/";
        cm.setCookie(url, "session=1; Path=/");
        cm.setCookie(url, "auth=abc; Path=/; Max-Age=3600");
        cm.setCookie(url, "pref=dark; EXPIRES=Wed, 21 Oct 2099 07:28:00 GMT");
        cm.setCookie(url, "bare=x");

        ArrayList<Boolean> answers = new ArrayList<Boolean>();
        cm.removeSessionCookies(recorder(answers));
        assertEquals("persistent cookies were removed with the session ones", "auth=abc; pref=dark",
                cm.getCookie(url));
        assertEquals("[true]", answers.toString());

        // A second pass has nothing left to remove.
        answers.clear();
        cm.removeSessionCookies(recorder(answers));
        assertEquals("[false]", answers.toString());
        assertEquals("auth=abc; pref=dark", cm.getCookie(url));
    }

    @Test
    public void aHostWithOnlySessionCookiesIsGone() {
        CookieManager cm = CookieManager.getInstance();
        cm.setCookie("https://a.example/", "s=1");
        cm.removeSessionCookies(null);
        assertNull(cm.getCookie("https://a.example/"));
        assertFalse(cm.hasCookies());
    }
}
