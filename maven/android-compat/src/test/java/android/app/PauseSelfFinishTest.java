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
package android.app;
import android.content.Context;
import android.content.Intent;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
public class PauseSelfFinishTest {
    @Rule public final MainThreadRule mainThread = new MainThreadRule();
    @Test public void finishInPauseDoesNotPauseAgainOrNotifyAfterDestroy() {
        exercise(false);
    }
    @Test public void explicitFinishStillRunsPauseBeforeDestroy() {
        exercise(true);
    }
    private void exercise(boolean explicitFinish) {
        Context c = AndroidTestSupport.context().getApplicationContext();
        c.startActivity(new Intent(c,AndroidTestSupport.FinishingActivity.class).putExtra("finishAt","pause").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        final AndroidTestSupport.FinishingActivity a = AndroidTestSupport.FinishingActivity.last;
        Application app = (Application)c;
        Application.ActivityLifecycleCallbacks cb = new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity x,android.os.Bundle b) { }
            public void onActivityStarted(Activity x) { }
            public void onActivityResumed(Activity x) { }
            public void onActivityPaused(Activity x) { if(x==a) a.events.add("pausedCallback"); }
            public void onActivityStopped(Activity x) { }
            public void onActivitySaveInstanceState(Activity x,android.os.Bundle b) { }
            public void onActivityDestroyed(Activity x) { }
        };
        app.registerActivityLifecycleCallbacks(cb);
        try {
            if (explicitFinish) {
                a.finish();
            } else {
                c.startActivity(new Intent(c,AndroidTestSupport.TestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            assertEquals(1,java.util.Collections.frequency(a.events,"pause"));
            assertEquals(1,java.util.Collections.frequency(a.events,"destroy"));
            assertFalse(a.events.toString(), a.events.subList(a.events.indexOf("destroy")+1,a.events.size()).contains("pausedCallback"));
        } finally { app.unregisterActivityLifecycleCallbacks(cb); ActivityThread.finishAllActivities(); }
    }
}
