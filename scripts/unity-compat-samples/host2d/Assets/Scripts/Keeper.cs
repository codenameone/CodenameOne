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
using System.Collections;
using UnityEngine;
using UnityEngine.SceneManagement;

// The object a game keeps for its whole run: the first one marks itself with
// DontDestroyOnLoad and every later one, built when its scene is loaded
// again, removes itself. Its coroutine and its total cross each load. The
// host adds to the total from outside the frame, and is told of it.
public class Keeper : MonoBehaviour
{
    public static Keeper Instance;

    // The scene points this at a slider, which the runtime does not have.
    public Behaviour gauge;
    public int total;

    private int loads;

    private void Awake()
    {
        if (Instance != null)
        {
            Debug.Log("keeper: a second one in " + SceneManager.GetActiveScene().name + " removes itself");
            Destroy(gameObject);
            return;
        }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        Debug.Log("keeper awake host=" + (Bridge.Host != null) + " gauge=" + (gauge != null));
        // What a host with a display has in place before the first Awake: the
        // platform, and the preferences an earlier run left. The write is read
        // back by the host, from its own store.
        Debug.Log("keeper platform=" + Application.platform + " mobile=" + Application.isMobilePlatform
            + " iphone=" + (Application.platform == RuntimePlatform.IPhonePlayer) + " editor="
            + Application.isEditor + " best=" + PlayerPrefs.GetInt("best", -1));
        PlayerPrefs.SetInt("best", PlayerPrefs.GetInt("best", -1) + 1);
        if (Bridge.Host != null)
        {
            Bridge.Host.Report("awake");
        }
        StartCoroutine(Tick());
    }

    private IEnumerator Tick()
    {
        for (int n = 1; ; n++)
        {
            yield return new WaitForSeconds(0.5f);
            // The scene's clock, in thousandths: it restarted with each load,
            // though this object and its coroutine came through.
            Debug.Log("keeper tick " + n + " frame " + Time.frameCount + " in " + SceneManager.GetActiveScene().name
                + " since load " + Mathf.RoundToInt(Time.timeSinceLevelLoad * 1000f) + " of "
                + Mathf.RoundToInt(Time.time * 1000f));
        }
    }

    private void Update()
    {
        if (Input.GetKeyDown(KeyCode.N))
        {
            loads++;
            SceneManager.LoadScene(loads % 2 == 1 ? "Second" : "First");
        }
    }

    public void Add(int points)
    {
        total += points;
        Debug.Log("keeper add " + points + " total=" + total + " frame " + Time.frameCount + " in "
            + SceneManager.GetActiveScene().name);
        Bridge.Host.Report("total " + total);
    }

    private void OnApplicationPause(bool paused)
    {
        Debug.Log("keeper pause=" + paused + " frame " + Time.frameCount);
    }

    private void OnApplicationFocus(bool focus)
    {
        Debug.Log("keeper focus=" + focus + " frame " + Time.frameCount);
    }

    // A message the runtime does not send: the build says so.
    private void OnBecameInvisible()
    {
        Debug.Log("never printed");
    }
}
