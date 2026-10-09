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
using System;
using System.Collections;
using System.Linq;
using UnityEngine;
using UnityEngine.Assertions;
using UnityEngine.EventSystems;
using UnityEngine.UI;

// A title panel over a paused scene. The button's click list, set up as the
// editor's inspector writes it, starts the game, passes an int and hides the
// panel; the script adds a listener of its own beside those. What the ball
// reports fills a bar and is kept as the best in PlayerPrefs.
public class Menu : MonoBehaviour
{
    public enum Level
    {
        Easy, Normal, Hard
    }

    public Text label;
    public Text best;
    public Image bar;
    public GameObject panel;
    public Button play;
    public Transform floor;
    public Ball ball;
    public Level level;

    private int clicks;
    private readonly KeyCode[] skipKeys = { KeyCode.Space, KeyCode.Return };

    private void Awake()
    {
        Assert.IsNotNull(ball);
        Assert.IsTrue(Enum.IsDefined(typeof(Level), level), "the level is not one of " + nameof(Level));
    }

    private void Start()
    {
        best.text = "Best: " + PlayerPrefs.GetInt("best");
        Time.timeScale = 0;
        play.onClick.AddListener(() => clicks++);
        ball.Bounced += (_, hit) => OnBounce(hit.level, hit.count);
        StartCoroutine(Tick());
        Debug.Log("menu level=" + level + " hard defined=" + Enum.IsDefined(typeof(Level), 2) + " nine defined="
            + Enum.IsDefined(typeof(Level), 9) + " floor=" + floor.name + " at y=" + Cm(floor.position.y) + " wide="
            + Cm(floor.localScale.x) + " listed=" + play.onClick.GetPersistentEventCount() + " interactable="
            + play.interactable + " has key=" + PlayerPrefs.HasKey("best"));
    }

    private void Update()
    {
        if (skipKeys.Any(Input.GetKeyDown))
        {
            Debug.Log("key frame " + Time.frameCount + " over interface=" + EventSystem.current.IsPointerOverGameObject()
                + " panel=" + panel.activeSelf + " clicks=" + clicks);
        }
    }

    // The three the button lists, in order.
    public void Play()
    {
        Time.timeScale = 1;
        Debug.Log("play, frame " + Time.frameCount + " clicks=" + clicks);
    }

    public void SetLevel(int value)
    {
        level = (Level)value;
        label.text = level switch
        {
            Level.Easy => "easy",
            Level.Hard => "hard",
            _ => "normal"
        };
    }

    private void OnBounce(Level at, int count)
    {
        bar.fillAmount = Mathf.Min(1f, count / 4f);
        if (count > PlayerPrefs.GetInt("best"))
        {
            PlayerPrefs.SetInt("best", count);
            best.text = "Best: " + count + " " + at;
        }
    }

    private IEnumerator Tick()
    {
        while (true)
        {
            yield return new WaitForSeconds(0.5f);
            Debug.Log("tick, frame " + Time.frameCount + " clicks=" + clicks + " selected="
                + (EventSystem.current.currentSelectedGameObject != null));
        }
    }

    public static int Cm(float value)
    {
        return Mathf.RoundToInt(value * 100f);
    }
}
