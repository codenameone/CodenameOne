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
import android.os.Bundle;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// A dynamically registered receiver takes a broadcast only when its whole
/// filter matches, as an activity's filter does for an implicit start. It used
/// to compare the action alone, so a receiver for one scheme ran for every
/// broadcast with the same action, and the package manager's other probes
/// for packages that do not exist answered this application.
public class BroadcastFilterMatchTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final class Recorder extends BroadcastReceiver {
        final List<String> seen = new ArrayList<String>();

        @Override
        public void onReceive(Context context, Intent intent) {
            seen.add(String.valueOf(intent.getData()));
        }
    }

    @Test
    public void receiversSeeOnlyBroadcastsTheirDataAndCategoriesMatch() {
        Context c = AndroidTestSupport.context();
        Recorder scheme = new Recorder();
        IntentFilter schemeFilter = new IntentFilter("com.codename1.test.PING");
        schemeFilter.addDataScheme("myapp");
        Recorder plain = new Recorder();
        Recorder category = new Recorder();
        IntentFilter categoryFilter = new IntentFilter("com.codename1.test.PING");
        categoryFilter.addCategory("com.codename1.test.CAT");
        c.registerReceiver(scheme, schemeFilter);
        c.registerReceiver(plain, new IntentFilter("com.codename1.test.PING"));
        c.registerReceiver(category, categoryFilter);
        try {
            c.sendBroadcast(new Intent("com.codename1.test.PING", Uri.parse("https://example.com")));
            c.sendBroadcast(new Intent("com.codename1.test.PING", Uri.parse("myapp://x")));
            c.sendBroadcast(new Intent("com.codename1.test.PING").addCategory("com.codename1.test.CAT"));
            c.sendBroadcast(new Intent("com.codename1.test.PING"));
            MainThreadRule.drain();
            assertEquals("the scheme filter took a broadcast for another scheme: " + scheme.seen,
                    1, scheme.seen.size());
            assertEquals("myapp://x", scheme.seen.get(0));
            assertEquals("a filter with no data or categories took broadcasts carrying them: " + plain.seen,
                    1, plain.seen.size());
            assertEquals("null", plain.seen.get(0));
            assertEquals(category.seen.toString(), 2, category.seen.size());
        } finally {
            c.unregisterReceiver(scheme);
            c.unregisterReceiver(plain);
            c.unregisterReceiver(category);
        }
    }

    /// The broadcast delivered is the one sent: changing or reusing the
    /// intent after `sendBroadcast` returns, before the queued delivery runs,
    /// used to change what the receivers matched and saw.
    @Test
    public void aReusedIntentDoesNotChangeASentBroadcast() {
        Context c = AndroidTestSupport.context();
        Recorder ping = new Recorder();
        IntentFilter filter = new IntentFilter("com.codename1.test.PING");
        filter.addDataScheme("myapp");
        c.registerReceiver(ping, filter);
        try {
            Intent reused = new Intent("com.codename1.test.PING", Uri.parse("myapp://first"));
            c.sendBroadcast(reused);
            reused.setAction("com.codename1.test.OTHER");
            reused.setData(Uri.parse("myapp://second"));
            MainThreadRule.drain();
            assertEquals(ping.seen.toString(), 1, ping.seen.size());
            assertEquals("myapp://first", ping.seen.get(0));
        } finally {
            c.unregisterReceiver(ping);
        }
    }

    @Test
    public void unregisteringBeforeDeliveryDropsTheOldRegistration() {
        Context c = AndroidTestSupport.context();
        Recorder receiver = new Recorder();
        IntentFilter filter = new IntentFilter("com.codename1.test.PING");
        c.registerReceiver(receiver, filter);
        c.sendBroadcast(new Intent("com.codename1.test.PING"));
        c.unregisterReceiver(receiver);
        // A new registration of the same object must not revive the old send.
        c.registerReceiver(receiver, filter);
        try {
            MainThreadRule.drain();
            assertEquals(0, receiver.seen.size());
            c.sendBroadcast(new Intent("com.codename1.test.PING"));
            MainThreadRule.drain();
            assertEquals(1, receiver.seen.size());
        } finally {
            c.unregisterReceiver(receiver);
        }
    }

    @Test
    public void mutableExtrasAreCapturedWhenTheBroadcastIsSent() {
        Context c = AndroidTestSupport.context();
        final int[] received = {-1, -1};
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                received[0] = intent.getIntArrayExtra("numbers")[0];
                received[1] = intent.getBundleExtra("nested").getInt("value");
            }
        };
        c.registerReceiver(receiver, new IntentFilter("com.codename1.test.EXTRA"));
        try {
            int[] numbers = {7};
            Bundle nested = new Bundle();
            nested.putInt("value", 11);
            c.sendBroadcast(new Intent("com.codename1.test.EXTRA")
                    .putExtra("numbers", numbers).putExtra("nested", nested));
            numbers[0] = 9;
            nested.putInt("value", 13);
            MainThreadRule.drain();
            assertEquals(7, received[0]);
            assertEquals(11, received[1]);
        } finally {
            c.unregisterReceiver(receiver);
        }
    }

    @Test
    public void launchIntentsExistOnlyForThisPackage() {
        Context c = AndroidTestSupport.context();
        android.content.pm.PackageManager pm = c.getPackageManager();
        assertNull("another package's launch intent relaunched this application",
                pm.getLaunchIntentForPackage("com.vendor.companion"));
        assertEquals(Intent.ACTION_MAIN, pm.getLaunchIntentForPackage(c.getPackageName()).getAction());
    }
}
