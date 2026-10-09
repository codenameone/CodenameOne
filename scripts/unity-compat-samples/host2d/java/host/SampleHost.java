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
package host;

import global.Bridge;
import global.IHost;
import global.Keeper;
import com.codename1.unitycompat.unityengine.PlayerPrefs;
import com.codename1.unitycompat.unityengine.PlayerPrefsStore;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import headless.HeadlessTrace;

/// The Java half of the host2d sample: what an application's main class
/// does around a game.
///
/// It implements the interface the scripts declare, hands itself to them
/// before the first scene is built -- the scripts' `Awake` already finds
/// it -- and calls a script's method from outside the frame.
///
/// It also gives the runtime what a host with a display gives it before
/// the first scene wakes, as `UnityGameView.installServices()` does: the
/// platform, and a place for `PlayerPrefs` that already holds what an
/// earlier run left there. The scripts read both in `Awake`.
public final class SampleHost implements HeadlessTrace.Host, IHost, PlayerPrefsStore {
    /// The one key the sample keeps, as an earlier run wrote it.
    private Object best = Integer.valueOf(41);

    public void installed() {
        Bridge.Host = this;
        PlayerPrefs.$store(this);
        UnityRuntime.platform(UnityRuntime.PLATFORM_IOS);
        System.out.println("host installed, keeper=" + (Keeper.Instance != null));
    }

    public void call(String word) {
        Keeper.Instance.Add(Integer.parseInt(word));
        System.out.println("host store best=" + best);
    }

    public Object get(String key) {
        return "best".equals(key) ? best : null;
    }

    public void set(String key, Object value) {
        if ("best".equals(key)) {
            best = value;
        }
    }

    public void delete(String key) {
        if ("best".equals(key)) {
            best = null;
        }
    }

    public void deleteAll() {
        best = null;
    }

    public void save() {
    }

    public void Report(String text) {
        System.out.println("host told: " + text);
    }
}
