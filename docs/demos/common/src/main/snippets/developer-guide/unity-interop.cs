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
// Developer guide snippets for the Unity compatibility chapter.

// tag::unity-interop-csharp-001[]
using System;
using System.Collections.Generic;
using UnityEngine;

public class Board : MonoBehaviour
{
    public GameObject tilePrefab;

    private int[,] cells;
    private readonly Dictionary<GameObject, Vector2> tiles = new Dictionary<GameObject, Vector2>();

    void Start()
    {
        TextAsset level = Resources.Load<TextAsset>("level1");
        string[] rows = level.text.Split(new[] { '\n' }, StringSplitOptions.RemoveEmptyEntries);
        cells = new int[rows.Length, rows[0].Split(',').Length];
        for (int r = 0; r < cells.GetLength(0); r++)
        {
            string[] columns = rows[r].Split(',');
            for (int c = 0; c < cells.GetLength(1); c++)
            {
                int.TryParse(columns[c], out cells[r, c]);
                if (cells[r, c] != 0)
                {
                    GameObject tile = Instantiate(tilePrefab, new Vector3(c, -r, 0), Quaternion.identity);
                    tiles[tile] = new Vector2(c, r);
                }
            }
        }
        PlayerPrefs.SetInt("lastLevel", 1);
    }
}
// end::unity-interop-csharp-001[]

// tag::unity-interop-csharp-002[]
// What the game asks of the application around it. The game compiles
// against this interface alone; the application supplies the object.
public interface IPlatformServices
{
    void SignIn();
    void Share(string text);
    void ShowLeaderboard(int score);
}

public static class Platform
{
    public static IPlatformServices Services;
}
// end::unity-interop-csharp-002[]

// tag::unity-interop-csharp-003[]
public class Score : MonoBehaviour
{
    public int points;

    public int Best { get; private set; }

    public void Add(int amount)
    {
        points += amount;
        if (points > Best)
        {
            Best = points;
        }
    }

    public Vector2 Where()
    {
        return transform.position;
    }

    private void Start()
    {
        Platform.Services?.SignIn();
    }

    private void OnApplicationPause(bool paused)
    {
        Debug.Log("score paused=" + paused);
    }

    private void Update()
    {
        if (Platform.Services == null)
        {
            return;
        }
        if (Input.GetKeyDown(KeyCode.L))
        {
            Platform.Services.ShowLeaderboard(Best);
        }
        if (Input.GetKeyDown(KeyCode.S))
        {
            Platform.Services.Share("I scored " + Best);
        }
    }
}
// end::unity-interop-csharp-003[]
